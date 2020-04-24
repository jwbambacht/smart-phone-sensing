package com.example.whereami;

public class Location {

    public int cellID;
    public int activityID;
    public byte[] BSSID;
    public int[] RSSI;

    public Location(int cellID, int activityID, byte[] BSSID, int[] RSSI) {
        this.cellID = cellID;
        this.activityID = activityID;
        this.BSSID = BSSID;
        this.RSSI = RSSI;
    }
    
    @Override
    public String toString() {
        return "Location is in cell " + this.cellID;
    }
}
