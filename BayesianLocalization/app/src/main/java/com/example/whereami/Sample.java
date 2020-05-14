package com.example.whereami;

import java.util.HashMap;

public class Sample {

    public int sampleID;
    public int cellID;
    public HashMap<String,Integer> networks;

    public Sample(int sampleID, int cellID, HashMap<String, Integer> networks) {
        this.sampleID = sampleID;
        this.cellID = cellID;
        this.networks = networks;
    }

    public HashMap<String,Integer> getNetworks() {
        return this.networks;
    }

    public int getCellID() {
        return this.cellID;
    }
}
