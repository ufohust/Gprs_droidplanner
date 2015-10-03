package org.droidplanner.core.MAVLink.connection;

import android.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Provides support for mavlink connection via TCP.
 */
public abstract class TcpConnection extends MavLinkConnection {

	private static final int CONNECTION_TIMEOUT = 20 * 1000; // 20 secs in ms

	private Socket socket;
	private BufferedOutputStream mavOut;
	private BufferedInputStream mavIn;

	private String serverIP;
	private String serverLogin;
	//private String serverPeer;
	private int serverPort;

	@Override
	public final void openConnection() throws IOException {
		getTCPStream();
	}

	@Override
	public final int readDataBlock(byte[] buffer) throws IOException {
		return mavIn.read(buffer);
	}

	@Override
	public final void sendBuffer(byte[] buffer) throws IOException {
		if (mavOut != null) {
			mavOut.write(buffer);
			mavOut.flush();
		}
	}

	@Override
	public final void loadPreferences() {
		serverIP = loadServerIP();
		serverPort = loadServerPort();
		serverLogin = loadServerLogin();
		//serverPeer = loadServerPeer();

	}

	protected abstract int loadServerPort();

	protected abstract String loadServerIP();
	protected abstract String loadServerLogin();
	//protected abstract String loadServerPeer();
	@Override
	public final void closeConnection() throws IOException {
		if (socket != null)
			socket.close();
	}

	private void getTCPStream() throws IOException {
		InetAddress serverAddr = InetAddress.getByName(serverIP);
		socket = new Socket();
		socket.connect(new InetSocketAddress(serverAddr, serverPort), CONNECTION_TIMEOUT);
		mavOut = new BufferedOutputStream((socket.getOutputStream()));
		mavIn = new BufferedInputStream(socket.getInputStream());

		//added by mike
		String login="$&@&()Glogin:"+serverLogin;

		sendBuffer(login.getBytes());
		Log.d("Login", login);

		try{

			Thread.sleep(300,0);
		}catch (InterruptedException e){
			e.printStackTrace();
		}

		final byte[] readBuffer1 = new byte[200];
		readDataBlock(readBuffer1);
		String str=new String(readBuffer1,"ISO-8859-1");

		Log.d("Login", str);
		if(str.contains("$&@&()login success")) {

		}

		//String peer = "peer:"+serverPeer;//"peer:52420,420";
		//sendBuffer(peer.getBytes());
		//Log.d("Login", peer);
		//try{

		//	Thread.sleep(300,0);
		//}catch (InterruptedException e){
		//	e.printStackTrace();
		//}

		//readDataBlock(readBuffer1);

		//String str2=new String(readBuffer1,"ISO-8859-1");
		//Log.d("Login", str2);
		//str2.contains("OK");
		//add by mike

	}

	@Override
	public final int getConnectionType() {
		return MavLinkConnectionTypes.MAVLINK_CONNECTION_TCP;
	}
}
