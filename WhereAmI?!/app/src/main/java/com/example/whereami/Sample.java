package com.example.whereami;

public class Sample {

    public int cellID;
    public int activityID;
    public byte[] BSSID;
    public int[] RSSI;

    public Sample(int cellID, int activityID, byte[] BSSID, int[] RSSI) {
        this.cellID = cellID;
        this.activityID = activityID;
        this.BSSID = BSSID;
        this.RSSI = RSSI;
    }
    
    @Override
    public String toString() {
        return "Sample is in cell " + this.cellID;
    }
}
