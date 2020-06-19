package com.example.whereami;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class Layout {

    public int width;
    public int height;
    public int wallThickness;
    public List<ShapeDrawable> boundaries;
    Context context;

    public Layout(int width, int height, int wallThickness, Context context) {
        this.width = width;
        this.height = height;
        this.wallThickness = wallThickness;
        this.context = context;
        this.boundaries = new ArrayList<>();
        this.fillLayout();
    }

    public void addBoundary(int left, int top, int right, int bottom, String type) {

        int color = R.color.colorMuted;

        if(type.equals("object")) {
            color = R.color.colorLight;
        }

        ShapeDrawable shape = new ShapeDrawable(new RectShape());
        shape.setBounds(left, top, right, bottom);
        shape.getPaint().setColor(ContextCompat.getColor(this.context,color));

        this.boundaries.add(shape);
    }

    public List<ShapeDrawable> getBoundaries() {
        return this.boundaries;
    }

    public void fillLayout() {
        this.addBoundary(0,0,width,wallThickness*2, "wall");                                                                        // Top
        this.addBoundary(width - 2 * wallThickness, 0, width, height, "wall");                                                             // Right
        this.addBoundary(0, height - 2 * wallThickness, width, height, "wall");                                                            // Bottom
        this.addBoundary(0, 0, 2 * wallThickness, height, "wall");                                                                    // Left
        this.addBoundary(width / 2 - wallThickness, 0, width / 2 + wallThickness, height / 6, "wall");                        // A-B
        this.addBoundary(width / 2, height / 6 - wallThickness, width / 2 + 15, height / 6 + wallThickness, "wall");          // A-C
        this.addBoundary(width / 2 + 80, height / 6 - wallThickness, width, height / 6 + wallThickness, "wall");                   // A-C
        this.addBoundary(0, height / 6 - wallThickness, width / 2 - 105, height / 6 + wallThickness, "wall");                 // B-C
        this.addBoundary(width / 2 - 40, height / 6 - wallThickness, width / 2, height / 6 + wallThickness, "wall");          // B-C
        this.addBoundary(width / 2 - wallThickness, height / 6 + 5, width / 2 + wallThickness, height / 6 * 2 - 70, "wall");  // C-C
        this.addBoundary(width / 2 - wallThickness, height / 6 * 2 - 10, width / 2 + wallThickness, height / 6 * 2, "wall");  // C-C
        this.addBoundary(width / 2 + 95, height / 6, width - wallThickness, height / 6 * 3 + 50, "wall");                     // C-C
        this.addBoundary(width / 2 + 175, height / 6+75, width - wallThickness, height / 6 + 150, "wall");                    // C-C
        this.addBoundary(0, height / 6 * 2 - wallThickness, width / 2, height / 6 * 2 + wallThickness, "wall");               // C-D
        this.addBoundary(0, height / 6 * 3 - wallThickness, width / 2 - 25, height / 6 * 3 + wallThickness, "wall");          // D-E
        this.addBoundary(0, height / 6 * 3 - wallThickness, 80, height / 6 * 5 + wallThickness, "wall");                      // EF-OUT
        this.addBoundary(width - 50, height / 6 * 3 + 20, width - wallThickness, height / 6 * 5 + wallThickness, "wall");     // EF-OUT
        this.addBoundary(width/2-95, height / 6 * 4 - 65, width/2-45, height / 6 * 4 + 65, "object");                           // Dinner Table
        this.addBoundary(width/2+50, height / 6 * 4 - 75, width/2+100, height / 6 * 4 + 75, "object");                          // Kitchen Island
        this.addBoundary(2*wallThickness, height / 6 * 2 - 165, width/2-90, height / 6 * 2 - 45, "object");                     // Bed
        this.addBoundary(width/2-40, 60, width/2-wallThickness, height / 6 - wallThickness, "object");                          // Closet
        this.addBoundary(width/2+50, 100-wallThickness, width/2+125, 100+wallThickness, "wall");                              // Shower wall
        this.addBoundary(width/2+125-wallThickness, 0, width/2+125+wallThickness, 100+wallThickness, "wall");                 // Shower wall
        this.addBoundary(2*wallThickness, height/6*3-45, 90, height / 6*3 - wallThickness, "object");                           // Desk
        this.addBoundary(2*wallThickness, height/6*2+wallThickness, 90, height / 6*2 +45, "object");                            // Desk
        this.addBoundary(100, height/6*2 + wallThickness, width / 2, height / 6*2 +25, "object");                               // Bookcases
        this.addBoundary(100, height/6*3-25, width / 2 - 25, height / 6*3 - wallThickness, "object");                           // Piano
        this.addBoundary(width/2+45, height/6*5+20, width/2+85, height / 6 * 5 +140, "object");                                 // Couch
        this.addBoundary(width/2-90, height/6*5-65, width/2-30, height / 6 * 5, "object");                                      // Sofa Chair
    }
}
