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
    int width, height, wallThickness;

    public Particle(int x, int y, int width, int height, int wallThickness) {
        this.x = x;
        this.y = y;
        this.pointRadius = 2;
        this.width = width;
        this.height = height;
        this.wallThickness = wallThickness;
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

    // Method that lowers the weight of the particle and updates the color of the particle based on that
    public void lowerWeight() {
        this.weight = this.weight/2;

        if(this.weight > 0.5) {
            this.shape.getPaint().setColor(0xff0000);
        }else if(this.weight > 0.25) {
            this.shape.getPaint().setColor(0xff4d00);
        }else if(this.weight > 0.125) {
            this.shape.getPaint().setColor(0xff7400);
        }else if(this.weight > 0.06125) {
            this.shape.getPaint().setColor(0xff9a00);
        }else if(this.weight > 0.03125) {
            this.shape.getPaint().setColor(0xffc100);
        }else {
            this.shape.getPaint().setColor(Color.GREEN);
        }
    }

    // Method that replaces the particle on the map if it within boundaries. No check here for collision with walls, that is done after resampling.
    public void resample(int x, int y) {
        boolean finished = false;

        while(!finished) {
            int offPlacement = 8;
            int randomXOffPlacement = (int) (Math.random() * offPlacement * 2) - offPlacement;
            int randomYOffPlacement = (int) (Math.random() * offPlacement * 2) - offPlacement;

            this.x = x + randomXOffPlacement;
            this.y = y + randomYOffPlacement;

            if(this.x > wallThickness && this.x < width-wallThickness && this.y > wallThickness && this.y < height-wallThickness) {
                finished = true;
            }
        }

        this.shape.setBounds(this.x-this.pointRadius, this.y-this.pointRadius, this.x+this.pointRadius, this.y+this.pointRadius);
    }

    // Method that returns the current cell of the particle
    public int getCurrentCell() {
        int cellID = 0;

        if(this.y >= 0 && this.y <= this.height/6) {
            if(this.x >= 0 && this.x <= this.width/2) {
                cellID = 1;
            }else if(this.x <= width){
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
            if(this.x >= 0 && this.x <= this.width/2) {
                cellID = 7;
            }else if(this.x <= width){
                cellID = 6;
            }
        }
        return cellID;
    }

    // Method that updates the location of the particle given the direction and stepsize
    public void move(int direction, int stepSize) {

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
}
