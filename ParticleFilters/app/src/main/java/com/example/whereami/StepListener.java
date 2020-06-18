package com.example.whereami;

public interface StepListener {
    public float getSensitivity();
    public int getDirection();
    public double getStepTime();
    public void moveParticles(int direction, boolean step);
}