package com.example.whereami;

public class ResultScan implements Comparable<ResultScan> {

    public String BSSID;
    public int RSSI;

    public ResultScan(String BSSID, int RSSI) {
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
    public int compareTo(ResultScan o) {
        return this.RSSI-o.RSSI;
    }
}
