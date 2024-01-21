package com.wavesignal.mdns.service;

import android.util.Log;

import com.wavesignal.mdns.network.TCPClient;
import com.wavesignal.mdns.network.TCPServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownServiceException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;

public class NetworkService {
    /**
     * The hooks allow a gentle mechanism of stepping into the normal
     * process of setup and teardown of a JmDNS service for
     * platform-specific actions (such as acquiring and releasing a
     * WiFi multicast lock for Android)
     */
    public interface ServiceSetupHook {
        /**
         * Called before creating the JmDNS service
         */
        boolean setup();
    }

    public interface ServiceTeardownHook {
        /**
         * Called after the JmDNS service was stopped
         */
        boolean teardown();
    }

    public interface ServiceEventHandler {
        void handle(ServiceInfo si);
    }

    private static final String LOG_TAG = "NetworkService";

    /**
     * If no identity is provided, a generic one is used.
     */
    private static final String DEFAULT_HOST_ID_PREFIX = "annon-";

    /**
     * What type of serviceServer should be advertised.
     */
    private static final String SERVICE_TYPE = "_pingpong._tcp.local.";

    /**
     * What request should be sent to the serviceServer running on other peers ?
     */
    private static final String REQUEST_MESSAGE = "Ping ";

    private final TCPServer serviceServer;
    private String devId;
    private final ServiceTeardownHook teardownHook;

    /**
     * Service discovery and advertisement.
     */
    private JmDNS jmdns;
    private ServiceInfo serviceInfo;

    private final Set<ServiceInfo> discoveredPeers = new TreeSet<>(new Comparator<ServiceInfo>() {
        @Override
        public int compare(ServiceInfo si1, ServiceInfo si2) {
            return si1.getName().compareTo(si2.getName());
        }
    });
    private ServiceEventHandler onNew, onRemove;

    public void setOnNewServiceCallback(ServiceEventHandler callback) {
        onNew = callback;
    }

    public void setOnServiceRemovedCallback(ServiceEventHandler callback) {
        onRemove = callback;
    }

    public List<ServiceInfo> getPeers() {
        List<ServiceInfo> peers = new LinkedList<>();
        peers.addAll(discoveredPeers);
        return peers;
    }

    public NetworkService(String nodeId, TCPServer serviceServer,
                          ServiceSetupHook setupStub, ServiceTeardownHook teardownStub) throws UnknownServiceException {
        teardownHook = teardownStub;
        this.serviceServer = serviceServer;

        // Add a random suffix to make the device id unique
        String prefix;
        if (nodeId != null) {
            prefix = nodeId;
        } else {
            prefix = DEFAULT_HOST_ID_PREFIX;
        }
        devId = prefix + new Random().nextInt();

        // Call the setup stub
        if (!setupStub.setup()) {
            throw new UnknownServiceException("Error during NSD setup");
        }

        Log.d(LOG_TAG, "Starting jmDNS serviceServer");
        try {
            jmdns = JmDNS.create(serviceServer.listenAddress(), serviceServer.listenAddress().getHostName());
            // Define the behavior of serviceServer discovery.
            jmdns.addServiceTypeListener(new PingPongServiceTypeListener());

            // Advertise the local serviceServer in the network
            serviceInfo = ServiceInfo.create(SERVICE_TYPE, devId, serviceServer.listenPort(), "ping pong");
            jmdns.registerService(serviceInfo);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error starting jmDNS instance" + e.getMessage());
        }
    }

    public boolean stop() {
        serviceServer.kill();

        if (jmdns != null) {
            jmdns.unregisterAllServices();
            try {
                jmdns.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
                return false;
            }
            Log.i(LOG_TAG, "Services unregistered");
            jmdns = null;
        }

        teardownHook.teardown();

        return true;
    }

    public boolean changeId(String prefix) {
        String newId = prefix + new Random().nextInt();
        if (newId.equals(devId)) {
            return true;
        } else {
            jmdns.unregisterService(serviceInfo);
            devId = newId;
            serviceInfo = ServiceInfo.create(SERVICE_TYPE, devId, serviceServer.listenPort(), "ping pong");
            try {
                jmdns.registerService(serviceInfo);
                Log.d(LOG_TAG, "Identity changed and advertised");
                return true;
            } catch (IOException e) {
                Log.e(LOG_TAG, "Cannot change identity: " + e.getMessage());
                return false;
            }
        }
    }

    private class PingPongServiceTypeListener implements ServiceTypeListener {
        @Override
        public void serviceTypeAdded(ServiceEvent event) {
            // A new serviceServer provider was discovered, is it running the serviceServer I want ?
            if (event.getType().equals(SERVICE_TYPE)) {
                Log.d("LOG_TAG", "Same serviceServer discovered");

                // I am interested in receiving events about this serviceServer type.
                jmdns.addServiceListener(event.getType(), new ServiceListener() {
                    @Override
                    public void serviceAdded(ServiceEvent serviceEvent) {
                        Log.i(LOG_TAG, "Service added " + serviceEvent.getInfo().toString());
                    }

                    @Override
                    public void serviceRemoved(ServiceEvent serviceEvent) {
                        Log.i(LOG_TAG, "Service removed " + serviceEvent.getInfo().toString());
                        discoveredPeers.remove(serviceEvent.getInfo());
                        onRemove.handle(serviceEvent.getInfo());
                    }

                    @Override
                    public void serviceResolved(final ServiceEvent serviceEvent) {
                        Log.i(LOG_TAG, "Peer found " + serviceEvent.getInfo().toString());

                        // If I'm not the newly discovered peer, engage in communication
                        if (!serviceEvent.getName().equals(devId)) {
                            // Send request to other peer
                            ExecutorService executor = Executors.newSingleThreadExecutor();
                            FutureTask<ServiceInfo> futureTask = new FutureTask<>(new Callable<ServiceInfo>() {
                                @Override
                                public ServiceInfo call() {
                                    try {
                                        for (InetAddress i : serviceEvent.getInfo().getInet4Addresses()) {
                                            Log.d(LOG_TAG, "Other peer is: " + i.getHostAddress());
                                        }
                                        Log.i(LOG_TAG, "Requesting " + REQUEST_MESSAGE);
                                        final String response = TCPClient.sendTo(REQUEST_MESSAGE,
                                                serviceEvent.getInfo().getInetAddresses()[0],
                                                serviceEvent.getInfo().getPort());
                                        Log.d(LOG_TAG, response);
                                        return serviceEvent.getInfo();
                                    } catch (IOException e) {
                                        Log.e(LOG_TAG, "Error in request:" + e.getMessage());
                                        return null;
                                    }
                                }
                            });

                            executor.execute(futureTask);

                            try {
                                ServiceInfo result = futureTask.get();
                                if (result != null) {
                                    discoveredPeers.add(result);
                                    onNew.handle(result);
                                }
                            } catch (Exception e) {
                                Log.e(LOG_TAG, e.getMessage());
                            } finally {
                                executor.shutdown();
                            }
                        } else {
                            Log.d(LOG_TAG, "I found myself");
                        }
                    }
                });

                // Request information about the serviceServer.
                jmdns.requestServiceInfo(event.getType(), event.getName());
            }

            Log.i(LOG_TAG, "Service discovered: " + event.getType() + " : " + event.getName());
        }

        @Override
        public void subTypeForServiceTypeAdded(ServiceEvent ev) {
        }
    }
}
