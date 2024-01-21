package com.wavesignal.mdns.network;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class AndroidWiFiTCPServer extends TCPServer {
    private static final String LOG_TAG = "AndroidWiFiTCPServer";

    public AndroidWiFiTCPServer(InetAddress address) throws IOException {
        super(address);
    }

    public static AndroidWiFiTCPServer build(Context androidContext) {
        // We need to know our identity inside the local WiFi network.
        WifiManager wifi = (WifiManager) androidContext.getSystemService(Context.WIFI_SERVICE);
        InetAddress deviceIpAddress;

        try {
            // Get the IP the server will be bound to.
            deviceIpAddress = InetAddress.getByAddress(ByteBuffer.allocate(4)
                    .putInt(Integer.reverseBytes(wifi.getConnectionInfo().getIpAddress())).array());

            Log.i(LOG_TAG, "My address is " + deviceIpAddress.getHostAddress());

            // Start the server
            return new AndroidWiFiTCPServer(deviceIpAddress);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error starting serviceServer " + e.getMessage());
            return null;
        }
    }
}
