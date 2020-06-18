package com.example.whereami;

public class AccessPoint implements Comparable<AccessPoint> {

    public String BSSID;
    public int RSSI;

    public AccessPoint(String BSSID, int RSSI) {
        this.BSSID = BSSID;
        this.RSSI = RSSI;
    }

    public String getBSSID() {
        return this.BSSID;
    }

    public int getRSSI() {
        return this.RSSI;
    }

    @Override
    public int compareTo(AccessPoint o) {
        return this.RSSI-o.RSSI;
    }

    @Override
    public String toString() {
        return this.BSSID+", "+this.RSSI;
    }
}
