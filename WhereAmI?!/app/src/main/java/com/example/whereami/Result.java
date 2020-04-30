package com.example.whereami;

public class Result {

    public Sample sample;
    public double distance;

    public Result(Sample sample, double distance) {
        this.sample = sample;
        this.distance = distance;
    }

    public double getDistance() {
        return this.distance;
    }

    public int getCellID() {
        return this.sample.cellID;
    }

    public String toString() {
        return "Cell with id " + this.sample.cellID + " and distance " + this.distance;
    }
}
