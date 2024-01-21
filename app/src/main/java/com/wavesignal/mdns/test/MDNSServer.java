package com.wavesignal.mdns.test;

import java.io.IOException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

public class MDNSServer {

    private static final String SERVICE_TYPE = "_myservice._tcp.local.";
    private static final String SERVICE_NAME = "MyService";
    private static final int SERVICE_PORT = 12345;

    public static void main(String[] args) {
        try {
            JmDNS jmdns = JmDNS.create();

            ServiceInfo serviceInfo = ServiceInfo.create(SERVICE_TYPE, SERVICE_NAME, SERVICE_PORT, "My Server");

            jmdns.registerService(serviceInfo);

            System.out.println("mDNS Service Registered");

            // Keep the program running to continue serving mDNS requests
            boolean flag = true;
            while(flag) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            jmdns.unregisterAllServices();
            jmdns.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
