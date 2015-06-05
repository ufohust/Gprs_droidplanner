package com.geeksville.apiproxy;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.geeksville.dapi.Webapi.Envelope;

/**
 * Send/receive protobufs (with length fields) over a TCP link
 * 
 * @author kevinh
 * 
 */
public class TCPProtobufClient implements IProtobufClient {

    private Socket socket;
    private OutputStream out;
    private InputStream in;

    TCPProtobufClient(String host, int port) throws UnknownHostException,
            IOException {
        socket = new Socket();
        socket.setTcpNoDelay(true); // Turn off nagle
        socket.connect(new InetSocketAddress(host, port));

        out = new BufferedOutputStream(socket.getOutputStream(), 8192);
        in = socket.getInputStream();
    }

    /**
     * Send a message
     * 
     * @param msg
     * @throws IOException
     */
    public void send(Envelope msg, Boolean noBlock) throws IOException {
        msg.writeDelimitedTo(out);
    }

    /**
     * Block until a message can be read
     * 
     * @return
     * @throws IOException
     */
    public Envelope receive(long timeout) throws IOException {
        return Envelope.parseDelimitedFrom(in);
    }

    public void close() throws IOException {
        out.close();
        in.close();
        socket.close();
    }

    public void flush() throws IOException {
        out.flush();
    }
}
