package com.wavesignal.mdns.network;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class TCPClient {

    private static final String LOG_TAG = "TCPClient";
    /**
     * Sends a string via a TCP socket (in UTF format), waits for a response and returns it (also UTF string).
     * @param str Request
     * @param destination IP address
     * @param port Port
     * @return Response from the other host.
     * @throws IOException gives exception
     */
    public static String sendTo(String str, InetAddress destination, int port) throws IOException {
        Socket socket = null;
        DataOutputStream writer;
        DataInputStream reader;

        try {
            socket = new Socket(destination, port);
            writer = new DataOutputStream(socket.getOutputStream());
            writer.writeUTF(str);
            // Close the output stream to signal there is no more data to be send.
            socket.shutdownOutput();
            // Read response.
            reader = new DataInputStream(socket.getInputStream());
            return reader.readUTF();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    /**
     * Convenience wrapper method, it builds an INetAddr form a string host name.
     * @param str Request
     * @param host IP address
     * @param port Port
     * @return Response from the other host.
     * @throws IOException gives exception
     */
    static String sendTo(String str, String host, int port) throws IOException {
        return sendTo(str, InetAddress.getByName(host), port);
    }
}
