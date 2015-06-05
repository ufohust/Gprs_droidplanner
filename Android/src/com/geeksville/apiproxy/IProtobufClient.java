package com.geeksville.apiproxy;

import java.io.IOException;

import com.geeksville.dapi.Webapi.Envelope;

/**
 * A low level pipe which can send and receive protobufs (hides differences
 * between UDP, TCP, ZMQ from the GCSHookImpl)
 * 
 * @author kevinh
 * 
 */
public interface IProtobufClient {
	/**
	 * Send a message
	 * 
	 * @param msg
	 * @throws IOException
	 */
	void send(Envelope msg, Boolean noBlock) throws IOException;

	/**
	 * Block until a message can be read
	 * 
	 * @param timeout
	 *            in msecs, or -1 for infinite
	 * @return null if we timeout
	 * @throws IOException
	 */
	Envelope receive(long timeout) throws IOException;

	void close() throws IOException;

	void flush() throws IOException;
}
