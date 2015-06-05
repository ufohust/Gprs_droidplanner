package com.geeksville.apiproxy;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.geeksville.dapi.Webapi.Envelope;

/**
 * This class wraps some other IProtobufClient with a pair of reader/writer
 * threads.
 * 
 * This allows us to ensure that clients of this instance never inadvertently
 * block. We are also able to ensure that all read and write operations come
 * from a single thread (necessary for ZMQ on android).
 * 
 * @author kevinh
 * 
 */
public class ThreadedProtobufClient implements IProtobufClient {

	private IProtobufClient target;

	private Boolean wantClose = false;

	// How often do our worker threads wake up if there is no work to do
	private static int POLL_INTERVAL = 1000;

	// FIXME, limit capacity in a more flexible manner...
	private LinkedBlockingQueue<Envelope> toSend = new LinkedBlockingQueue<Envelope>(
			100);
	private LinkedBlockingQueue<Envelope> received = new LinkedBlockingQueue<Envelope>(
			100);

	private Thread receiverThread = new Thread(new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			receiverLoop();
		}
	}, "pb-reader");

	private Thread senderThread = new Thread(new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			senderLoop();
		}
	}, "pb-sender");

	ThreadedProtobufClient(IProtobufClient target) {
		this.target = target;

		receiverThread.setDaemon(true);
		receiverThread.start();
		senderThread.setDaemon(true);
		senderThread.start();
	}

	protected void senderLoop() {
		try {
			while (!wantClose) {
				Envelope msg = toSend
						.poll(POLL_INTERVAL, TimeUnit.MILLISECONDS);
				if (msg != null) {
					// System.out.println("sending " + msg);
					target.send(msg, true);
				}
			}
			target.close(); // Shut down the real driver
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void receiverLoop() {
		try {
			while (!wantClose) {
				Envelope msg = target.receive(POLL_INTERVAL);
				if (msg != null) {
					// System.out.println("received " + msg);
					if (!received.offer(msg, POLL_INTERVAL,
							TimeUnit.MILLISECONDS))
						throw new IOException("Receiver queue is full");
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void send(Envelope msg, Boolean noBlock) throws IOException {
		try {
			// FIXME - provide more flexibility in specifying timeout
			// System.out.println("Enqueue to send " + msg);
			if (!toSend.offer(msg, noBlock ? 0 : 30000, TimeUnit.MILLISECONDS)
					&& !noBlock)
				throw new IOException("Timeout on send");
		} catch (InterruptedException ex) {
			throw new IOException("Interrupt on send", ex);
		}
	}

	@Override
	public Envelope receive(long timeout) throws IOException {
		try {
			return received.poll(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new IOException("Interrupt on receive", e);
		}
	}

	@Override
	public void close() throws IOException {
		wantClose = true;

		// kinda yucky - we rely on the threads to exit once they have exhausted
		// all work
	}

	@Override
	public void flush() throws IOException {
		// fixme
	}
}
