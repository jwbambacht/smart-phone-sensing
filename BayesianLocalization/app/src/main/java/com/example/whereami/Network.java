package com.example.whereami;

public class Network  implements Comparable<Network> {

    String BSSID;
    int RSSI;

    public Network(String BSSID, int RSSI) {
        this.BSSID = BSSID;
        this.RSSI = RSSI;
    }

    public String getBSSID() {
        return this.BSSID;
    }

    @Override
    public int compareTo(Network o) {
        return this.RSSI-o.RSSI;
    }
}
