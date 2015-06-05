package com.geeksville.apiproxy;

import java.io.IOException;
import java.io.File;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * A quick test to be easy to embed into Droidplanner.
 * 
 * @author kevinh
 * 
 */
public class TestClient extends GCSHookImpl {

	public int interfaceNum = 0;
	static int numPackets = 4;

	static String login = "test-bob";
	static String password = "sekrit";
	static String vehicleId = "550e8400-e29b-41d4-a716-446655440000";
	static File testTlog = new File("mav.tlog");

	// Do not use this key in your own applications - please register your own.
	static String apiKey = "a41df935.ef413c94e19e056091675063a9df7c53";

	public void connect() throws UnknownHostException, IOException {
		super.connect();

		String email = "test-bob@3drobotics.com";

		// Create user if necessary/possible
		if (isUsernameAvailable(login))
			createUser(login, password, email);
		else
			loginUser(login, password);

		int sysId = 1;
		setVehicleId(vehicleId, interfaceNum, sysId, false);

		startMission(false, UUID.randomUUID());
	}

	@Override
	public void close() throws IOException {
		stopMission(true);

		flush();
		super.close();
	}

	/**
	 * Do one full connection/upload session
	 * 
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	public static void runTest() throws UnknownHostException, Exception {
		TestClient webapi = new TestClient();
		try {
			webapi.connect();
			// Test splitting packet into two calls - to show bug in server
			byte[] payload1 = new byte[] { (byte) 0xfe, (byte) 0x0e,
					(byte) 0x9d, (byte) 0x01, (byte) 0x01, (byte) 0x1d,
					(byte) 0xf9, (byte) 0x46, (byte) 0x01, (byte) 0x00,
					(byte) 0x33 };
			byte[] payload2 = new byte[] { (byte) 0x03, (byte) 0x7c,
					(byte) 0x44, (byte) 0xec, (byte) 0x51, (byte) 0x1e,
					(byte) 0xbe, (byte) 0x27, (byte) 0x01, (byte) 0xca,
					(byte) 0x8f };
			for (int i = 0; i < numPackets; i++) {
				webapi.filterMavlink(webapi.interfaceNum, payload1);
				webapi.filterMavlink(webapi.interfaceNum, payload2);
				Thread.sleep(200);
			}

			System.out.println("Test successful");
		} finally {
			webapi.close();
		}
	}

	/**
	 * Test simple REST uploads
	 * 
	 * @throws Exception
	 */
	public static void runRESTTest() throws Exception {
		RESTClient.doUpload(testTlog, login, password, vehicleId, apiKey, "DEFAULT");
	}

	/**
	 * Test uploading all files in a directory
	 * 
	 * @throws Exception
	 */
	public static void runDirTest() throws Exception {
		IUploadListener callback = new IUploadListener() {
			public void onUploadStart(File f) {
				System.out.println("Upload start: " + f);
			}

			public void onUploadSuccess(File f, String viewURL) {
				System.out.println("Upload success: " + f + " url=" + viewURL);
			}

			public void onUploadFailure(File f, Exception ex) {
				System.out.println("Upload fail: " + f + " " + ex);
			}
		};

		File tmpDir = new File("/tmp");
		File srcDir = new File(tmpDir, "testsrc");
		srcDir.mkdirs();
		File destDir = new File(tmpDir, "testdest");
		DirectoryUploader up = new DirectoryUploader(srcDir, destDir, callback,
				login, password, vehicleId, apiKey, "DEFAULT");
		up.run();
	}

	public static void main(String[] args) {
		if (false) {
			System.out.println("Starting REST test");
			try {
				runRESTTest();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (false) {
			System.out.println("Starting dir upload test");
			try {
				runDirTest();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (true) {
			System.out.println("Starting Protobuf test");
			try {
				runTest();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
