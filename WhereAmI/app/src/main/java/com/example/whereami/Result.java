package com.example.whereami;

public class Result {

    public Sample sample;
    public double distance;
    public double weight;

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

    public int getActivityID() { return this.sample.activityID; }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getWeight() {
        return this.weight;
    }

    public void normalizeWeight(double totalWeight) {
        this.weight = this.weight/totalWeight;
    }

    public String toString() {
        return "Cell with id " + this.sample.cellID + " and distance " + this.distance;
    }

    public String activityToString() {
        return "Activity with id " + this.sample.activityID + " and distance " + this.distance;
    }
}
