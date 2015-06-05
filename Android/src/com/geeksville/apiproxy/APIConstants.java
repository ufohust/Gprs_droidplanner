package com.geeksville.apiproxy;

public class APIConstants {
	/**
	 * The default world wide drone broker
	 */
	public static final String DEFAULT_SERVER = "api.3drobotics.com";

	public static final String URL_BASE = "https://" + DEFAULT_SERVER;

	/**
	 * If using a raw TCP link to the server, use this port number
	 */
	public static final int DEFAULT_TCP_PORT = 5555;

	public static final String ZMQ_URL = "tcp://" + DEFAULT_SERVER + ":5556";

	public static final String TLOG_MIME_TYPE = "application/vnd.mavlink.tlog";
}
