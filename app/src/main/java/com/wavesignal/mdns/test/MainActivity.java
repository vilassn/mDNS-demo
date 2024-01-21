package com.wavesignal.mdns.test;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.wavesignal.mdns.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Thread thread1 = new Thread(() -> Esp32Discovery.main(null));
//        thread1.start();

//        Thread thread = new Thread(() -> MDNSServer.main(null));
//        thread.start();

//        Thread thread1 = new Thread(() -> MDNSClient.main(null));
//        thread1.start();

        Thread thread1 = new Thread(() -> ArduinoHttpClient.main(null));
        thread1.start();
    }
}