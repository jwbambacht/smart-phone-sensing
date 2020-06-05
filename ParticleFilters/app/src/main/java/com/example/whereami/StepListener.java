package com.example.whereami;

public interface StepListener {
    
    public float getSensitivity();

    public int getDirection();

    public void step(long timeNs);

    public void moveParticles(int direction);

}