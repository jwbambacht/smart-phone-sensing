package com.example.whereami;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class NetworkProcessed {

    String BSSID;
    List<double[]> cells = new ArrayList<>();

    public NetworkProcessed(String BSSID) {
        this.BSSID = BSSID;
        this.cells.add(new double[101]);
        this.cells.add(new double[101]);
        this.cells.add(new double[101]);
        this.cells.add(new double[101]);
        this.cells.add(new double[101]);
        this.cells.add(new double[101]);
        this.cells.add(new double[101]);
        this.cells.add(new double[101]);
    }

    public String getBSSID() {
        return this.BSSID;
    }

    public double[] getProbabilitiesForRSSI(int RSSI) {
        double[] values = new double[8];
        for(double[] cell : this.cells) {
            int index = this.cells.indexOf(cell);
            values[index] = cell[RSSI];
        }
        return values;
    }

    public void setCellProbabilities(int cellID, List<Integer> samples) {
        double[] probs = new double[101];

        for(int x = 0; x < 101; x++) {
            probs[x] = Util.gaussianKernelProbability(x, samples);
        }

        this.cells.set(cellID,probs);
    }

    public double getCellProbabilityAtRSSI(int cellID, int RSSI) {
        return this.cells.get(cellID)[RSSI];
    }
}
