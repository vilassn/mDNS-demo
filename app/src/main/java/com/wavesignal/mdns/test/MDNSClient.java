package com.wavesignal.mdns.test;

import java.io.IOException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

public class MDNSClient {

    public static void main(String[] args) {
        try {
            JmDNS jmdns = JmDNS.create();

            System.out.println("after create..................1");

            jmdns.addServiceListener("http://esp32-device.local", new ServiceListener() {
                @Override
                public void serviceAdded(ServiceEvent event) {
                    System.out.println("Service added: " + event.getInfo());
                }

                @Override
                public void serviceRemoved(ServiceEvent event) {
                    System.out.println("Service removed: " + event.getInfo());
                }

                @Override
                public void serviceResolved(ServiceEvent event) {
                    System.out.println("Service resolved: " + event.getInfo());
                }
            });

            System.out.println("after create..................2");

            // Keep the program running to continue listening for mDNS events
            boolean flag = true;
            while(flag) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            System.out.println("after create..................3");
            jmdns.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

