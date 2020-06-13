package com.example.whereami;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;

public class Particle {

    int x, y;
    ShapeDrawable shape;
    boolean collided;
    int pointRadius;
    double weight;

    public Particle(int x, int y) {
        this.x = x;
        this.y = y;
        this.pointRadius = 2;
        this.weight = 1;
        this.shape = new ShapeDrawable(new OvalShape());
        this.shape.getPaint().setColor(Color.RED);
        this.shape.setBounds(this.x-this.pointRadius, this.y-this.pointRadius, this.x+this.pointRadius, this.y+this.pointRadius);
    }

    public ShapeDrawable getShape() {
        return this.shape;
    }

    public boolean getCollided() {
        return this.collided;
    }

    public void setCollided(boolean bool) {
        this.collided = bool;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public void resample(int x, int y) {
        int[] offPlacement = new int[]{-4,-3,-2,-1,0,1,2,3,4};
        int randomXOffPlacement = offPlacement[(int) (Math.random() * offPlacement.length)];
        int randomYOffPlacement = offPlacement[(int) (Math.random() * offPlacement.length)];

        this.x = x+randomXOffPlacement;
        this.y = y+randomYOffPlacement;

        this.shape.setBounds(this.x-this.pointRadius, this.y-this.pointRadius, this.x+this.pointRadius, this.y+this.pointRadius);
    }

    public void updateLocation(int direction, int stepSize) {

        Rect r = this.shape.getBounds();

        // UP - DOWN - LEFT - RIGHT
        if (direction == 0) {
            this.shape.setBounds(r.left, r.top - stepSize, r.right, r.bottom - stepSize);
            this.y -= stepSize;
        } else if (direction == 1) {
            this.shape.setBounds(r.left, r.top + stepSize, r.right, r.bottom + stepSize);
            this.y += stepSize;
        } else if (direction == 2) {
            this.shape.setBounds(r.left - stepSize, r.top, r.right - stepSize, r.bottom);
            this.x -= stepSize;
        } else if (direction == 3) {
            this.shape.setBounds(r.left + stepSize, r.top, r.right + stepSize, r.bottom);
            this.x += stepSize;
        }
    }

    public double distanceToOtherParticle(Particle particle) {
        return Math.sqrt((particle.getY()-this.y) * (particle.getY()-this.y) + (particle.getX()-this.x) * (particle.getX()-this.x));
    }
}
