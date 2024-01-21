package com.wavesignal.mdns.test;

import java.io.IOException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

public class Esp32Discovery {

    public static void main(String[] args) {
        try {
            // Create JmDNS instance
            JmDNS jmdns = JmDNS.create();

            System.out.println("ESP32 IP.....1");

            // Replace "esp32-device" with the hostname you used in the ESP32 code
            String esp32Hostname = "esp32-device";

            // Discover service info
            ServiceInfo[] serviceInfos = jmdns.list(esp32Hostname);

            // Print IP addresses
            for (ServiceInfo info : serviceInfos) {
                String ipAddress = info.getInetAddresses()[0].getHostAddress();
                System.out.println("ESP32 IP Address: " + ipAddress);
            }

            System.out.println("ESP32 IP.....2");

            // Close JmDNS
            jmdns.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

