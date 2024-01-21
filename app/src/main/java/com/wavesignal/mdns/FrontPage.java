package com.wavesignal.mdns;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.wavesignal.mdns.network.AndroidWiFiTCPServer;
import com.wavesignal.mdns.service.AndroidDNSSetupHooks;
import com.wavesignal.mdns.service.NetworkService;

import java.net.UnknownServiceException;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class FrontPage extends Activity {
    private static final String LOG_TAG = "FrontPage";
    private NetworkService service;
    private Button btnUpdate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnUpdate = findViewById(R.id.ok_button);
        btnUpdate.setOnClickListener(v -> {
            EditText idBox = findViewById(R.id.identity_box);
            String newDevId = idBox.getText().toString();
            changeId(newDevId);
        });

        // Start the server and advertise the service via mDNS.
        ExecutorService executor = Executors.newSingleThreadExecutor();

        FutureTask<NetworkService> futureTask = new FutureTask<>(() -> {
            AndroidDNSSetupHooks hooks = new AndroidDNSSetupHooks(FrontPage.this);
            try {
                return new NetworkService(null, AndroidWiFiTCPServer.build(FrontPage.this), hooks, hooks);
            } catch (UnknownServiceException e) {
                Log.e(LOG_TAG, e.getMessage());
                return null;
            }
        });

        executor.execute(futureTask);

        try {
            service = futureTask.get();
        } catch (InterruptedException | ExecutionException e) {
            Log.e(LOG_TAG, e.getMessage());
        } finally {
            executor.shutdown();
        }

        if (service != null) {
            final ListView peerList = findViewById(R.id.peer_list);
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_list_item_1, new LinkedList<>());
            peerList.setAdapter(adapter);

            service.setOnNewServiceCallback(si -> {
                adapter.add(si.getName());
                adapter.notifyDataSetChanged();
            });

            service.setOnServiceRemovedCallback(si -> {
                adapter.remove(si.getName());
                adapter.notifyDataSetChanged();
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        service.stop();
        service = null;
    }

    private void changeId(final String id) {
        btnUpdate.setEnabled(false);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        FutureTask<Boolean> futureTask = new FutureTask<>(() -> service.changeId(id));

        executor.execute(futureTask);

        try {
            Boolean result = futureTask.get();
            // Handle the result as needed
        } catch (InterruptedException | ExecutionException e) {
            Log.e(LOG_TAG, e.getMessage());
        } finally {
            btnUpdate.setEnabled(true);
            executor.shutdown();
        }
    }
}
