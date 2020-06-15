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
    float weight;
    int width, height;

    public Particle(int x, int y, int nParticles, int width, int height) {
        this.x = x;
        this.y = y;
        this.pointRadius = 2;
        this.width = width;
        this.height = height;
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

    public float getWeight() { return this.weight; }

    public void lowerWeight() { this.weight = this.weight/2; }

    public void resample(int x, int y) {
        int[] offPlacement = new int[]{-5,0,5};
        int randomXOffPlacement = (int) (Math.random() * 100) - 50;
        int randomYOffPlacement = (int) (Math.random() * 100) - 50;

        this.x = x+randomXOffPlacement;
        this.y = y+randomYOffPlacement;

        this.shape.setBounds(this.x-this.pointRadius, this.y-this.pointRadius, this.x+this.pointRadius, this.y+this.pointRadius);
    }

    public int getCurrentCell() {
        int cellID = 0;

        if(this.y <= this.height/6) {
            if(this.x <= this.width/2) {
                cellID = 1;
            }else{
                cellID = 0;
            }
        }else if(this.y <= this.height/6*2) {
            cellID = 2;
        }else if(this.y <= this.height/6*3) {
            cellID = 3;
        }else if(this.y <= this.height/6*4) {
            cellID = 4;
        }else if(this.y <= this.height/6*5) {
            cellID = 5;
        }else if(this.y <= this.height) {
            if(this.x <= this.width/2) {
                cellID = 7;
            }else{
                cellID = 6;
            }
        }
        return cellID;
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
