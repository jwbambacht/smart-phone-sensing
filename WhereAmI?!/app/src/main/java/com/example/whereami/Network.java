package com.example.whereami;

import java.util.List;

public class Network {

    String BSSID;
    int RSSI;

    public Network(String BSSID, int RSSI) {
        this.BSSID = BSSID;
        this.RSSI = RSSI;
    }

    public String getBSSID() {
        return this.BSSID;
    }
}
