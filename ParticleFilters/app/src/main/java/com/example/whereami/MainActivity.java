package com.example.whereami;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class MainActivity extends AppCompatActivity implements OnClickListener, SensorEventListener, StepListener {

    private Button up, left, right, down, reset, init, start;
    private TextView textview_azimuth, textview_particle_count, textview_steps, textview_direction;
    private List<ShapeDrawable> particles;
    private Canvas canvas;
    private List<ShapeDrawable> walls;
    private int width, height;
    private SharedPreferences settingsSharedPreferences;
    private String layout;
    private int stepSize;
    private float sensitivity;
    private int stepSizeMultiplier;
    private int nParticles;
    private boolean startFiltering;
    private boolean initializeOrientation;
    private boolean initialOrientationGathered;
    private int initialOrientationCount;
    private int initialOrientation;
    private int[] initialOrientationVector;

    private StepDetector simpleStepDetector;
    private SensorManager sensorManager;
    private SensorEventListener mSensorEventListener;
    private int numSteps;

    private Sensor rotationSensor;
    private float[] mAccel = new float[3];
    // Rotation sensor vector
    private float[] mRotation = new float[3];
    // Orientation angles from accelerometer and magnetometer
    private float[] mRotationMatrixFromVector = new float[9];
    // Orientation angles from accelerometer and magnetometer
    private float[] mOrientation = new float[3];

    private double currentAzimuth, preAzimuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_whereami_icon_white);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        settingsSharedPreferences = getApplicationContext().getSharedPreferences("SETTINGS", 0);
        layout = settingsSharedPreferences.getString("layout", "Joost");
        sensitivity = Float.parseFloat(settingsSharedPreferences.getString("sensitivity", "10"));
        stepSizeMultiplier = Integer.parseInt(settingsSharedPreferences.getString("stepsize", "1"));
        nParticles = Integer.parseInt(settingsSharedPreferences.getString("particles", "5000"));

        Log.i("Layout",layout);
        Log.i("Sensitivity",""+sensitivity);
        Log.i("Step Size Multiplier",""+stepSizeMultiplier);
        Log.i("Particles",""+nParticles);

        stepSize = 5;
        numSteps = 0;
        startFiltering = false;
        initializeOrientation = false;
        initialOrientationCount = 0;
        initialOrientationGathered = false;
        initialOrientation = 0;

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sSensor= sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);
        sensorManager.registerListener(MainActivity.this, accel, SensorManager.SENSOR_DELAY_FASTEST);

        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(mySensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),SensorManager.SENSOR_DELAY_NORMAL);

        // Buttons and listeners
        up = (Button) findViewById(R.id.button_up);
        left = (Button) findViewById(R.id.button_left);
        right = (Button) findViewById(R.id.button_right);
        down = (Button) findViewById(R.id.button_down);
        reset = (Button) findViewById(R.id.button_reset);
        init = (Button) findViewById(R.id.button_init);
        start = (Button) findViewById(R.id.button_start);
        textview_azimuth = (TextView) findViewById(R.id.textview_azimuth);
        textview_particle_count = (TextView) findViewById(R.id.textview_particle_count);
        textview_steps = (TextView) findViewById(R.id.textview_steps);
        textview_direction = (TextView) findViewById(R.id.textview_direction);
        up.setOnClickListener(this);
        down.setOnClickListener(this);
        left.setOnClickListener(this);
        right.setOnClickListener(this);
        reset.setOnClickListener(this);
        init.setOnClickListener(this);
        start.setOnClickListener(this);

        // Get display size, create drawable object and walls
        width = this.getDisplaySize()[0];
        height = this.getDisplaySize()[1];

        // Initialize Canvas
        this.prepareFiltering();
    }

    private SensorEventListener mySensorEventListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            System.arraycopy(event.values, 0, mRotation, 0, 3);
            SensorManager.getRotationMatrixFromVector(mRotationMatrixFromVector, mRotation);

            currentAzimuth = (int) ( Math.toDegrees( SensorManager.getOrientation( mRotationMatrixFromVector, mOrientation )[0] ) + 360 ) % 360;

            if(initializeOrientation && !initialOrientationGathered && initialOrientationCount < 5) {
                initialOrientation += (int) currentAzimuth;
                initialOrientationCount++;

                if(initialOrientationCount == 5) {
                    initializeOrientation = false;
                    initialOrientationGathered = true;
                    initialOrientation = (int) initialOrientation/5;
                }

            }
            if(startFiltering) {
                if (Math.abs(currentAzimuth - preAzimuth) >= 30.0) {
                    preAzimuth = currentAzimuth;
                    textview_azimuth.setText("Azimuth:\n" + preAzimuth);
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        layout = settingsSharedPreferences.getString("layout", "Joost");
        sensitivity = Float.parseFloat(settingsSharedPreferences.getString("sensitivity", "10"));
        stepSizeMultiplier = Integer.parseInt(settingsSharedPreferences.getString("stepsize", "1"));
        nParticles = Integer.parseInt(settingsSharedPreferences.getString("particles", "5000"));
        width = this.getDisplaySize()[0];
        height = this.getDisplaySize()[1];
        prepareFiltering();
    }

    @Override
    protected void onPause() {
        super.onPause();
        layout = settingsSharedPreferences.getString("layout", "Joost");
        sensitivity = Float.parseFloat(settingsSharedPreferences.getString("sensitivity", "10"));
        stepSizeMultiplier = Integer.parseInt(settingsSharedPreferences.getString("stepsize", "1"));
        nParticles = Integer.parseInt(settingsSharedPreferences.getString("particles", "5000"));
        width = this.getDisplaySize()[0];
        height = this.getDisplaySize()[1];
        prepareFiltering();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                this.startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_up: {
                moveParticles(0);
                break;
            }
            case R.id.button_down: {
                moveParticles(1);
                break;
            }
            case R.id.button_left: {
                moveParticles(2);
                break;
            }
            case R.id.button_right: {
                moveParticles(3);
                break;
            }
            case R.id.button_reset: {
                startFiltering = false;
                initializeOrientation = false;
                initialOrientationGathered = false;
                initialOrientationVector = new int[5];
                initialOrientation = 0;

                this.prepareFiltering();
                break;
            }
            case R.id.button_init: {
                if(!initializeOrientation) {
                    initializeOrientation = true;
                }
            }
            case R.id.button_start: {
                if(!startFiltering) {
                    startFiltering = true;
                    Log.i("START","");
                }
            }
        }
    }

    public void moveParticles(int direction) {
        for(int i = 0; i < stepSizeMultiplier; i++) {
            for (ShapeDrawable drawable : particles) {
                Rect r = drawable.getBounds();

                // UP - DOWN - LEFT - RIGHT
                if (direction == 0) {
                    drawable.setBounds(r.left, r.top - stepSize, r.right, r.bottom - stepSize);
                } else if (direction == 1) {
                    drawable.setBounds(r.left, r.top + stepSize, r.right, r.bottom + stepSize);
                } else if (direction == 2) {
                    drawable.setBounds(r.left - stepSize, r.top, r.right - stepSize, r.bottom);
                } else if (direction == 3) {
                    drawable.setBounds(r.left + stepSize, r.top, r.right + stepSize, r.bottom);
                }
            }

            // Remove particle if there is a collision with a wall
            Iterator<ShapeDrawable> itr = particles.iterator();
            while (itr.hasNext()) {
                ShapeDrawable particle = itr.next();
                if (isCollision(particle)) {
                    itr.remove();
                }
            }
        }

        // redrawing of the object
        canvas.drawColor(ContextCompat.getColor(this, R.color.colorDark));
        for(ShapeDrawable p : particles) {
            p.draw(canvas);
        }
        for(ShapeDrawable wall : walls) {
            wall.draw(canvas);
        }

        textview_particle_count.setText("Particles:\n"+particles.size());
    }

    public int[] getDisplaySize() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int[] sizes = new int[2];

        if(layout.equals("Joost")) {
            sizes[0] = size.x * 9 / 16;
            sizes[1] = size.y;
        }else{
            sizes[0] = size.x;
            sizes[1] = size.y;
        }

        return sizes;
    }

    public void prepareFiltering() {
        // create a canvas
        ImageView canvasView = (ImageView) findViewById(R.id.canvas);
        Bitmap blankBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(blankBitmap);
        canvas.drawColor(ContextCompat.getColor(this, R.color.colorDark));
        canvasView.setImageBitmap(blankBitmap);

        particles = this.createParticles(nParticles);
        walls = this.createLayout();

        // draw the objects
        for (ShapeDrawable wall : walls) {
            wall.draw(canvas);
        }

        Iterator<ShapeDrawable> itr = particles.iterator();
        while (itr.hasNext()) {
            ShapeDrawable particle = itr.next();
            if (isCollision(particle)) {
                itr.remove();
            }
        }

        textview_particle_count.setText("Particles:\n"+particles.size());
    }

    public List<ShapeDrawable> createParticles(int N) {

        int width = this.getDisplaySize()[0];
        int height = this.getDisplaySize()[1];
        int pointRadius = 2;

        List<ShapeDrawable> particles = new ArrayList<>();

        for(int i = 0; i < N; i++) {
            ShapeDrawable particle = new ShapeDrawable(new OvalShape());
            particle.getPaint().setColor(Color.RED);
            int x = (int) (Math.random()*width);
            int y = (int) (Math.random()*height);
            particle.setBounds(x-pointRadius, y-pointRadius, x+pointRadius, y+pointRadius);
            particles.add(particle);
            particle.draw(canvas);
        }

        return particles;
    }

    private boolean isCollision(ShapeDrawable particle) {
        for(ShapeDrawable wall : walls) {
            if(isCollision(wall,particle))
                return true;
        }
        return false;
    }

    private boolean isCollision(ShapeDrawable wall, ShapeDrawable particle) {
        Rect wallShape = new Rect(wall.getBounds());
        return wallShape.intersect(particle.getBounds());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(startFiltering) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                simpleStepDetector.updateAccel(event.timestamp, event.values[0], event.values[1], event.values[2]);
            }
        }
    }

    @Override
    public void step(long timeNs) {
        numSteps++;
        textview_steps.setText("Steps:\n"+numSteps);
    }

    @Override
    public float getSensitivity() {
        return this.sensitivity;
    }

    @Override
    public int getDirection() {

        int correction = (int) this.currentAzimuth-initialOrientation;
        int direction = 0; // UP

        if(correction < 0) {
            correction = 360-Math.abs(correction);
        }

        if(correction >= 45 && correction <135) {
            direction = 3; // RIGHT
            textview_direction.setText("RIGHT");
        }else if(correction >= 135 && correction < 225) {
            direction = 1; // DOWN
            textview_direction.setText("DOWN");
        }else if(correction >= 225 && correction < 315) {
            direction = 2; // LEFT
            textview_direction.setText("LEFT");
        }else{
            textview_direction.setText("UP");
        }

        return direction;
    }

    public List<ShapeDrawable> createLayout() {
        List<ShapeDrawable> walls = new ArrayList<>();

        int wallThickness = 2;

        if(layout.equals("Joost")) {
            // Top boundary
            ShapeDrawable topBoundary = new ShapeDrawable(new RectShape());
            topBoundary.setBounds(0, 0, width, wallThickness * 2);
            topBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(topBoundary);

            // Right boundary
            ShapeDrawable rightBoundary = new ShapeDrawable(new RectShape());
            rightBoundary.setBounds(width - 2 * wallThickness, 0, width, height);
            rightBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(rightBoundary);

            // Bottom boundary
            ShapeDrawable bottomBoundary = new ShapeDrawable(new RectShape());
            bottomBoundary.setBounds(0, height - 2 * wallThickness, width, height);
            bottomBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(bottomBoundary);

            // Left boundary
            ShapeDrawable leftBoundary = new ShapeDrawable(new RectShape());
            leftBoundary.setBounds(0, 0, 2 * wallThickness, height);
            leftBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(leftBoundary);

            // Wall A-B
            ShapeDrawable abBoundary = new ShapeDrawable(new RectShape());
            abBoundary.setBounds(width / 2 - wallThickness, 0, width / 2 + wallThickness, height / 6);
            abBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(abBoundary);

            // Wall A-C
            ShapeDrawable acBoundary = new ShapeDrawable(new RectShape());
            acBoundary.setBounds(width / 2, height / 6 - wallThickness, width / 2 + 15, height / 6 + wallThickness);
            acBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(acBoundary);
            ShapeDrawable accBoundary = new ShapeDrawable(new RectShape());
            accBoundary.setBounds(width / 2 + 80, height / 6 - wallThickness, width, height / 6 + wallThickness);
            accBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(accBoundary);

            // Walls B-C
            ShapeDrawable bcBoundary = new ShapeDrawable(new RectShape());
            bcBoundary.setBounds(0, height / 6 - wallThickness, width / 2 - 105, height / 6 + wallThickness);
            bcBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(bcBoundary);
            ShapeDrawable bccBoundary = new ShapeDrawable(new RectShape());
            bccBoundary.setBounds(width / 2 - 40, height / 6 - wallThickness, width / 2, height / 6 + wallThickness);
            bccBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(bccBoundary);

            // Walls C-C
            ShapeDrawable ccBoundary = new ShapeDrawable(new RectShape());
            ccBoundary.setBounds(width / 2 - wallThickness, height / 6 + 5, width / 2 + wallThickness, height / 6 * 2 - 70);
            ccBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(ccBoundary);
            ShapeDrawable cccBoundary = new ShapeDrawable(new RectShape());
            cccBoundary.setBounds(width / 2 - wallThickness, height / 6 * 2 - 10, width / 2 + wallThickness, height / 6 * 2);
            cccBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(cccBoundary);
            ShapeDrawable cctoiletBoundary = new ShapeDrawable(new RectShape());
            cctoiletBoundary.setBounds(width / 2 + 105, height / 6, width - wallThickness, height / 6 + 75);
            cctoiletBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(cctoiletBoundary);
            ShapeDrawable ccoBoundary = new ShapeDrawable(new RectShape());
            ccoBoundary.setBounds(width / 2 + 105, height / 6+150, width - wallThickness, height / 6 * 3 + 20);
            ccoBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(ccoBoundary);
            ShapeDrawable ccooBoundary = new ShapeDrawable(new RectShape());
            ccooBoundary.setBounds(width / 2 + 175, height / 6+75, width - wallThickness, height / 6 + 150);
            ccooBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(ccooBoundary);

            // Walls C-D
            ShapeDrawable cdBoundary = new ShapeDrawable(new RectShape());
            cdBoundary.setBounds(0, height / 6 * 2 - wallThickness, width / 2, height / 6 * 2 + wallThickness);
            cdBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(cdBoundary);

            // Walls D-E
            ShapeDrawable deBoundary = new ShapeDrawable(new RectShape());
            deBoundary.setBounds(0, height / 6 * 3 - wallThickness, width / 2 - 25, height / 6 * 3 + wallThickness);
            deBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(deBoundary);

            // Walls EF-OUT
            ShapeDrawable eobBoundary = new ShapeDrawable(new RectShape());
            eobBoundary.setBounds(0, height / 6 * 3 - wallThickness, 90, height / 6 * 5 + wallThickness);
            eobBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(eobBoundary);
            ShapeDrawable eotBoundary = new ShapeDrawable(new RectShape());
            eotBoundary.setBounds(width - 65, height / 6 * 3 + 20, width - wallThickness, height / 6 * 5 + wallThickness);
            eotBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(eotBoundary);

            // Dinner Table
            ShapeDrawable tableBoundary = new ShapeDrawable(new RectShape());
            tableBoundary.setBounds(width/2-85, height / 6 * 4 - 65, width/2-25, height / 6 * 4 + 65);
            tableBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorLight));
            walls.add(tableBoundary);

            // Kitchen Island
            ShapeDrawable kitchenBoundary = new ShapeDrawable(new RectShape());
            kitchenBoundary.setBounds(width/2+40, height / 6 * 4 - 80, width/2+95, height / 6 * 4 + 80);
            kitchenBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorLight));
            walls.add(kitchenBoundary);

            // Bed
            ShapeDrawable bedBoundary = new ShapeDrawable(new RectShape());
            bedBoundary.setBounds(2*wallThickness, height / 6 * 2 - 165, width/2-90, height / 6 * 2 - 45);
            bedBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorLight));
            walls.add(bedBoundary);

            // Closet
            ShapeDrawable closetBoundary = new ShapeDrawable(new RectShape());
            closetBoundary.setBounds(width/2-45, 60, width/2-wallThickness, height / 6 - wallThickness);
            closetBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorLight));
            walls.add(closetBoundary);

            // Shower Wall
            ShapeDrawable showerWallBoundary = new ShapeDrawable(new RectShape());
            showerWallBoundary.setBounds(width/2+50, 100-wallThickness, width/2+125, 100+wallThickness);
            showerWallBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(showerWallBoundary);
            ShapeDrawable showerWall2Boundary = new ShapeDrawable(new RectShape());
            showerWall2Boundary.setBounds(width/2+125-wallThickness, 0, width/2+125+wallThickness, 100+wallThickness);
            showerWall2Boundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(showerWall2Boundary);

            // Desks
            ShapeDrawable desk1Boundary = new ShapeDrawable(new RectShape());
            desk1Boundary.setBounds(2*wallThickness, height/6*3-45, 90, height / 6*3 - wallThickness);
            desk1Boundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorLight));
            walls.add(desk1Boundary);
            ShapeDrawable desk2Boundary = new ShapeDrawable(new RectShape());
            desk2Boundary.setBounds(2*wallThickness, height/6*2+wallThickness, 90, height / 6*2 +45);
            desk2Boundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorLight));
            walls.add(desk2Boundary);

            // Couch
            ShapeDrawable couchBoundary = new ShapeDrawable(new RectShape());
            couchBoundary.setBounds(width/2+45, height/6*5+10, width/2+95, height / 6 * 5 +140);
            couchBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorLight));
            walls.add(couchBoundary);

            // Sofa Chair
            ShapeDrawable sofaBoundary = new ShapeDrawable(new RectShape());
            sofaBoundary.setBounds(width/2-90, height/6*5-65, width/2-30, height / 6 * 5);
            sofaBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorLight));
            walls.add(sofaBoundary);

            // Coffee Table
            ShapeDrawable coffeeBoundary = new ShapeDrawable(new RectShape());
            coffeeBoundary.setBounds(width/2-50, height/6*5+50, width/2, height / 6 * 5+100);
            coffeeBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorLight));
            walls.add(coffeeBoundary);
        }else{

        }

        return walls;
    }
}
