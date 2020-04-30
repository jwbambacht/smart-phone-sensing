package com.example.whereami;

import java.util.HashMap;
import java.util.List;

public class Sample {

    public int cellID;
    public int activityID;
    public HashMap<String,Integer> networks;

    public Sample(int cellID, int activityID, HashMap<String, Integer> networks) {
        this.cellID = cellID;
        this.activityID = activityID;
        this.networks = networks;
    }

    public HashMap<String,Integer> getNetworks() {
        return this.networks;
    }

    public int getCellID() {
        return this.cellID;
    }

    public int getActivityID() {
        return this.activityID;
    }
}
