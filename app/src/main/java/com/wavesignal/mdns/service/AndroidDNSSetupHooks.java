package com.wavesignal.mdns.service;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

public class AndroidDNSSetupHooks implements NetworkService.ServiceSetupHook,
    NetworkService.ServiceTeardownHook {

    private static final String LOG_TAG = "AndroidDNSSetupHooks";

    private final Context ctx;
    /**
     * Required for WiFi multicast communication.
     */
    private WifiManager.MulticastLock lock;

    public AndroidDNSSetupHooks(Context androidContext) {
        ctx = androidContext;
    }

    @Override
    public boolean setup() {
        // Acquire lock for multicast communication.
        WifiManager wifi = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        lock = wifi.createMulticastLock(getClass().getName());
        lock.setReferenceCounted(true);
        lock.acquire();

        return true;
    }

    @Override
    public boolean teardown() {
        if (lock != null) {
            Log.i(LOG_TAG, "Releasing multicast lock");
            lock.release();
            lock = null;
        }
        return true;
    }
}
