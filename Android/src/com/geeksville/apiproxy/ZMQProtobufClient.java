package com.geeksville.apiproxy;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMsg;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.*;

import com.geeksville.dapi.Webapi.Envelope;

/**
 * Send/receive protobufs (with length fields) over a ZeroMQ link
 * 
 * @author kevinh
 * 
 */
public class ZMQProtobufClient implements IProtobufClient {

	private ZContext ctx;
	private Socket socket;
	private PollItem[] items;

	ZMQProtobufClient(String zurl) {
		System.out.println("Starting zmq client to " + zurl);
		ctx = new ZContext();

		// We use ROUTER rather than DEALER because we want ZMQ to drop packets
		// (rather than blocking)
		// if we are not connected to the server.
		socket = ctx.createSocket(ZMQ.ROUTER);

		socket.setHWM(20);
		socket.setLinger(200); // in msecs

		// Use the following to get better client ids (must be unique)
		String identity = UUID.randomUUID().toString();
		socket.setIdentity(identity.getBytes(ZMQ.CHARSET));

		socket.connect(zurl);

		items = new PollItem[] { new PollItem(socket, Poller.POLLIN) };
	}

	/**
	 * Send a message
	 * 
	 * @param msg
	 * @throws IOException
	 */
	public void send(Envelope msg, Boolean noBlock) throws IOException {
		// System.out.println("Sending " + msg);
		socket.sendMore("SERVER"); // destination identity
		socket.sendMore(""); // A zero delemiter before payload
		socket.send(msg.toByteArray(), noBlock ? ZMQ.NOBLOCK : 0);
	}

	/**
	 * Block until a message can be read
	 * 
	 * @return
	 * @throws IOException
	 */
	public Envelope receive(long timeout) throws IOException {

		ZMQ.poll(items, timeout);
		if (items[0].isReadable()) {
			// System.out.println("Receiving");
			// The DEALER socket gives us the address envelope and message
			ZMsg msg = ZMsg.recvMsg(socket);
			// System.out.println("Recvd " + msg);
			ZFrame content = msg.getLast();
			assert (content != null);
			// System.out.println("Content " + content);

			return Envelope.parseFrom(content.getData());
		} else
			return null; // timed out
	}

	public void close() throws IOException {
		System.out.println("Closing ZMQ link");
		// socket.close();
		// ctx.close();
		ctx.destroy();
	}

	public void flush() throws IOException {
		// Not needed (zmq hides this)
	}
}
