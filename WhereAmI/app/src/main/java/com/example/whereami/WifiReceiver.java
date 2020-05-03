package com.example.whereami;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

class WifiReceiver extends BroadcastReceiver {

    WifiManager wifiManager;

    public WifiReceiver(WifiManager wifiManager) {
        this.wifiManager = wifiManager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("WiFiReceiver","Wifi scan completed");

        // If training or sensing button is pushed again before the wifi receiver is completed the result of the scan will be the same!
        Toast.makeText(context.getApplicationContext(), R.string.confirm_wifi_receiver_completed, Toast.LENGTH_SHORT).show();
    }
}
