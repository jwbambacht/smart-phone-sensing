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

    }
}
