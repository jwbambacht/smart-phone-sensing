package com.example.whereami;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Network {

    String BSSID;
    List<BigDecimal[]> cells = new ArrayList<>();

    public Network(String BSSID) {
        this.BSSID = BSSID;
        this.cells.add(new BigDecimal[100]);
        this.cells.add(new BigDecimal[100]);
        this.cells.add(new BigDecimal[100]);
        this.cells.add(new BigDecimal[100]);
        this.cells.add(new BigDecimal[100]);
        this.cells.add(new BigDecimal[100]);
        this.cells.add(new BigDecimal[100]);
        this.cells.add(new BigDecimal[100]);
    }

    public String getBSSID() {
        return this.BSSID;
    }

    public BigDecimal[] getProbabilitiesForRSSI(int RSSI) {
        BigDecimal[] values = new BigDecimal[8];
        for(BigDecimal[] cell : this.cells) {
            int index = this.cells.indexOf(cell);
            values[index] = cell[RSSI];
        }
        return values;
    }

    public void setCellProbabilities(int cellID, BigDecimal[] values) {
        this.cells.set(cellID,values);
    }
}
