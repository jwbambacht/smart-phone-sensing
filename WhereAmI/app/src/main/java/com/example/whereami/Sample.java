package com.example.whereami;

import java.util.HashMap;

public class Sample {

    public int sampleID;
    public int cellID;
    public int activityID;
    public HashMap<String,Integer> networks;
    public float[] activityFeature;

    public Sample(int sampleID, int cellID, int activityID, HashMap<String, Integer> networks, float[] activityFeature) {
        this.sampleID = sampleID;
        this.cellID = cellID;
        this.activityID = activityID;
        this.networks = networks;
        this.activityFeature = activityFeature;
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

    public float[] getActivityFeature() { return activityFeature; }
}
