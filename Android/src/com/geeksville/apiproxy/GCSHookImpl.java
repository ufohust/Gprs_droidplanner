package com.geeksville.apiproxy;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.Random;

import com.geeksville.dapi.Webapi.*;
import com.google.protobuf.ByteString;

/**
 * Create an instance of this class to be able to connect to the web API.
 * 
 * @author kevinh
 * 
 */
public class GCSHookImpl implements GCSHooks {

	private IProtobufClient weblink;

	private boolean loggedIn = false;

	/**
	 * Time in usecs
	 */
	private long startTime;

	private Random random = new Random(System.currentTimeMillis());

	// / We must receive a reply to any message within this period or we
	// consider the link dropped
	private static long receiveTimeout = 30 * 1000;

	public void connect() throws UnknownHostException, IOException {
		// weblink = new TCPProtobufClient(APIConstants.DEFAULT_SERVER,
		// APIConstants.DEFAULT_TCP_PORT);
		weblink = new ThreadedProtobufClient(new ZMQProtobufClient(
				APIConstants.ZMQ_URL));
		waitConnected(5000);

		startTime = System.currentTimeMillis() * 1000;
	}

	private void waitConnected(long timeoutMsec) throws IOException {
		int nonce = random.nextInt();
		Envelope ping = Envelope.newBuilder()
				.setPing(PingMsg.newBuilder().setNonce(nonce).build()).build();

		long timeout = 300; // Check every 100ms
		while (timeoutMsec > 0) {
			// System.out.println("Sending ping");
			weblink.send(ping, true);
			flush();

			Envelope env = weblink.receive(timeout);
			if (env != null && env.hasPingResponse()
					&& env.getPingResponse().getNonce() == nonce) {
				System.out.println("Connected");
				return;
			}

			if (env != null)
				System.out.println("Discarding " + env);

			timeoutMsec -= timeout;
		}
		throw new IOException("Protocol connection timeout");
	}

	@Override
	public void setCallback(GCSCallback cb) {
		// TODO Auto-generated method stub

	}

	@Override
	public void filterMavlink(int fromInterface, byte[] bytes)
			throws IOException {
		long deltat = (System.currentTimeMillis() * 1000) - startTime;

		MavlinkMsg mav = MavlinkMsg.newBuilder().setSrcInterface(fromInterface)
				.setDeltaT(deltat).addPacket(ByteString.copyFrom(bytes))
				.build();

		sendNoBlock(Envelope.newBuilder().setMavlink(mav).build());
	}

	@Override
	public void loginUser(String userName, String password)
			throws UnknownHostException, IOException {

		LoginMsg m = LoginMsg.newBuilder().setUsername(userName)
				.setCode(LoginRequestCode.LOGIN).setPassword(password)
				.setStartTime(startTime).build();
		Envelope msg = Envelope.newBuilder().setLogin(m).build();
		sendUnchecked(msg);
		checkLoginOkay();
	}

	// / Ask server if the specified username is available for creation
	public boolean isUsernameAvailable(String userName)
			throws UnknownHostException, IOException {
		// System.out.println("Checking if username available");
		LoginMsg m = LoginMsg.newBuilder().setUsername(userName)
				.setCode(LoginRequestCode.CHECK_USERNAME).build();
		Envelope msg = Envelope.newBuilder().setLogin(m).build();
		sendUnchecked(msg);
		LoginResponseMsg r = readLoginResponse();
		// System.out.println("username available " + r.getCode());

		return (r.getCode() == LoginResponseMsg.ResponseCode.OK);
	}

	// / Create a new user account
	@Override
	public void createUser(String userName, String password, String email)
			throws UnknownHostException, IOException {
		LoginMsg.Builder builder = LoginMsg.newBuilder().setUsername(userName)
				.setCode(LoginRequestCode.CREATE).setPassword(password)
				.setStartTime(startTime);

		if (email != null)
			builder.setEmail(email);

		Envelope msg = Envelope.newBuilder().setLogin(builder.build()).build();
		sendUnchecked(msg);
		checkLoginOkay();
	}

	private Envelope readEnvelope() throws IOException {
		return weblink.receive(receiveTimeout);
	}

	private LoginResponseMsg readLoginResponse() throws IOException {
		flush(); // Make sure any previous commands has been sent
		while (true) {
			Envelope env = readEnvelope();

			// Ignore msgs that are not login responses
			if (env.hasLoginResponse()) {
				LoginResponseMsg r = env.getLoginResponse();

				// No matter what, if the server is telling us to hang up, we
				// must bail
				// immediately
				if (r.getCode() == LoginResponseMsg.ResponseCode.CALL_LATER)
					throw new CallbackLaterException(r.getMessage(),
							r.getCallbackDelay());

				return r;
			}
			// else System.out.println("Ignoring non login resp: " + env);
		}
	}

	private void checkLoginOkay() throws IOException {
		LoginResponseMsg r = readLoginResponse();
		if (r.getCode() != LoginResponseMsg.ResponseCode.OK)
			throw new LoginFailedException(r.getMessage());

		loggedIn = true;
	}

	@Override
	public void setVehicleId(String vehicleId, int interfaceId,
			int mavlinkSysId, boolean canAcceptCommands) throws IOException {
		SenderIdMsg mav = SenderIdMsg.newBuilder().setGcsInterface(interfaceId)
				.setSysId(mavlinkSysId).setCanAcceptCommands(canAcceptCommands)
				.setVehicleUUID(vehicleId).build();

		send(Envelope.newBuilder().setSetSender(mav).build());
	}

	@Override
	public void flush() throws IOException {
		if (weblink != null)
			weblink.flush();
	}

	@Override
	public void close() throws IOException {
		if (weblink != null) {
			weblink.close();
			weblink = null;
		}
	}

	@Override
	public void send(Envelope e) throws IOException {
		if (loggedIn)
			sendUnchecked(e);
	}

	public synchronized void sendNoBlock(Envelope e) throws IOException {
		if (loggedIn && weblink != null)
			weblink.send(e, true);
	}

	/**
	 * Send without checking to see if we are logged in (this method will block
	 * if necessary)
	 * 
	 * @param e
	 * @throws IOException
	 */
	private synchronized void sendUnchecked(Envelope e) throws IOException {
		if (weblink != null)
			weblink.send(e, false);
	}

	@Override
	public void startMission(Boolean keep, UUID uuid) throws IOException {
		StartMissionMsg mav = StartMissionMsg.newBuilder().setKeep(keep)
				.setUuid(uuid.toString()).build();

		send(Envelope.newBuilder().setStartMission(mav).build());
	}

	@Override
	public void stopMission(Boolean keep) throws IOException {
		StopMissionMsg mav = StopMissionMsg.newBuilder().setKeep(keep).build();

		// Note - this method will be called from the GUI thread in the case of
		// droidplanner, therefore we can never
		// allow it to block
		sendNoBlock(Envelope.newBuilder().setStopMission(mav).build());
	}

}
