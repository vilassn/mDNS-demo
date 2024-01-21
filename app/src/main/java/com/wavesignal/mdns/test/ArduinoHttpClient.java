package com.wavesignal.mdns.test;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ArduinoHttpClient {

    private static final String ESP32_HOSTNAME = "esp32-device.local";

    public static void main(String[] args) {

        // Create a separate thread to perform the HTTP request
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Perform the HTTP request
                String response = performHttpRequest();

                // Use a handler to update the UI with the response
                Handler handler = new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(@NonNull Message msg) {
                        if (response != null) {
                            System.out.println("Response Body: " + response);
                        } else {
                            System.out.println("Error occurred during HTTP request");
                        }
                    }
                };

                // Send a message to the handler
                handler.sendEmptyMessage(0);
            }
        }).start();
    }

    private static String performHttpRequest() {
        try {
            // Build the URL for the GET request
            URL url = new URL("http://" + ESP32_HOSTNAME);

            // Open a connection to the URL
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            try {
                // Read the response
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    response.append(line);
                    response.append('\n');
                }

                return response.toString();
            } finally {
                // Disconnect the HttpURLConnection
                urlConnection.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}