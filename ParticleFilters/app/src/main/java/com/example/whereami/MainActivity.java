package com.example.whereami;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.content.Intent;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class MainActivity extends AppCompatActivity implements OnClickListener, StepListener {

    // UI Elements
    private Button up, left, right, down, reset, init, start;
    private TextView textview_azimuth, textview_particle_count, textview_steps, textview_direction;
    ImageView canvasView;
    Bitmap blankBitmap;

    // Canvas and shapes
    private Canvas canvas;
    private List<Particle> particles;
    private List<ShapeDrawable> walls;

    private SharedPreferences settingsSharedPreferences;

    // General variables
    private int width, height;
    private String layout;
    private int stepSize;
    private float sensitivity;
    private int stepSizeMultiplier;
    private int nParticles;
    private int numSteps;

    int test;

    // Variables for orientation and filtering
    private boolean startFiltering;
    private boolean initializeOrientation;
    private boolean initialOrientationGathered;
    private int initialOrientationCount;
    private int initialOrientation;
    private int[] initialOrientationVector;

    // Sensors and sensor variables
    private SensorManager sensorManager;
    private StepDetector simpleStepDetector;
    private Sensor rotationSensor;
    private Sensor accelerometerSensor;
    private float[] mRotation = new float[3];                       // Rotation sensor vector
    private float[] mRotationMatrixFromVector = new float[9];       // Orientation angles from accelerometer and magnetometer
    private float[] mOrientation = new float[3];                    // Orientation angles from accelerometer and magnetometer
    private double currentAzimuth, preAzimuth;                      // Azimuth

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_whereami_icon_white);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Obtain settings from shared preferences and display size
        getSettings();


        test = 0;

        // Set variables
        stepSize = 5;
        numSteps = 0;
        startFiltering = false;
        initializeOrientation = false;
        initialOrientationCount = 0;
        initialOrientationGathered = false;
        initialOrientation = 0;

        // Sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);
        sensorManager.registerListener(accelerometerEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(rotationVectorEventListener, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);

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
        toggleButtons(false);
        up.setOnClickListener(this);
        down.setOnClickListener(this);
        left.setOnClickListener(this);
        right.setOnClickListener(this);
        reset.setOnClickListener(this);
        init.setOnClickListener(this);
        start.setOnClickListener(this);

        // Initialize Canvas
        this.prepareCanvas();
    }

    // Method that determines whether the rotation vector sensor changed its state and determines the azimuth
    private SensorEventListener rotationVectorEventListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            System.arraycopy(event.values, 0, mRotation, 0, 3);
            SensorManager.getRotationMatrixFromVector(mRotationMatrixFromVector, mRotation);

            currentAzimuth = (int) ( Math.toDegrees( SensorManager.getOrientation( mRotationMatrixFromVector, mOrientation )[0] ) + 360 ) % 360;

            // If init button is pressed we obtain 5 values for azimuth and take the average for initial orientation
            if(initializeOrientation && !initialOrientationGathered && initialOrientationCount < 5) {
                initialOrientation += (int) currentAzimuth;
                initialOrientationCount++;
                if(initialOrientationCount == 5) {
                    initializeOrientation = false;
                    initialOrientationGathered = true;
                    initialOrientation = (int) initialOrientation/5;
                }

            }

            // If start button is pressed we detect a change in azimuth if the difference is big enough to compensate for sensitivity
            if(startFiltering) {
                if (Math.abs(currentAzimuth - preAzimuth) >= 30.0) {
                    preAzimuth = currentAzimuth;
                    textview_azimuth.setText("Azimuth:\n" + preAzimuth);
                }
            }
        }
    };

    // Method that determines whether the accelerometer sensor changed its state
    private SensorEventListener accelerometerEventListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            if(startFiltering) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    simpleStepDetector.updateAccel(event.timestamp, event.values[0], event.values[1], event.values[2]);
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        getSettings();
        prepareCanvas();

        sensorManager.registerListener(accelerometerEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(rotationVectorEventListener, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getSettings();
        prepareCanvas();

        sensorManager.unregisterListener(accelerometerEventListener);
        sensorManager.unregisterListener(rotationVectorEventListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(accelerometerEventListener);
        sensorManager.unregisterListener(rotationVectorEventListener);
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

    // Process the click listeners
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
                numSteps = 0;

                toggleButtons(false);
                init.setEnabled(true);

                this.prepareCanvas();
                break;
            }
            case R.id.button_init: {
                if(!initializeOrientation) {
                    initializeOrientation = true;
                    toggleButtons(true);
                }
                break;
            }
            case R.id.button_start: {
                startFiltering = true;
                toggleButtons(true);
                init.setEnabled(false);
//                Thread thread = new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        double totalDistance = 0;
//                        boolean convergence = false;
//
//                        while(!convergence && startFiltering) {
//                            totalDistance = 0;
//                            Log.i("",particles.size()+"");
//                            for(Particle particle_one : particles) {
//                                for(Particle particle_two : particles) {
//                                    totalDistance += particle_one.distanceToOtherParticle(particle_two);
//                                }
//                            }
//                            if(totalDistance/2 < 100) {
//                                convergence = true;
//                            }
//                            Log.i("TotalDistance",totalDistance+"");
//                        }
//
//                    }
//                });
//
//                thread.start();
                break;
            }
        }
    }

    // Method that toggles the state of some of the buttons for a better user experience
    public void toggleButtons(boolean onoff) {
        up.setEnabled(onoff);
        left.setEnabled(onoff);
        right.setEnabled(onoff);
        down.setEnabled(onoff);
        start.setEnabled(onoff);
    }

    // Method that determines the screen size and adapts the size of the layout
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

    // Method that retrieves the settings from the sharedpreferences
    public void getSettings() {
        settingsSharedPreferences = getApplicationContext().getSharedPreferences("SETTINGS", 0);

        layout = settingsSharedPreferences.getString("layout", "Joost");
        sensitivity = Float.parseFloat(settingsSharedPreferences.getString("sensitivity", "10"));
        stepSizeMultiplier = Integer.parseInt(settingsSharedPreferences.getString("stepsize", "1"));
        nParticles = Integer.parseInt(settingsSharedPreferences.getString("particles", "5000"));

        width = this.getDisplaySize()[0];
        height = this.getDisplaySize()[1];
    }

    // Method that makes the particles move over the layout, based on the direction and stepsize
    public void moveParticles(int direction) {

        for(int i = 0; i < stepSizeMultiplier; i++) {
            for (Particle particle : particles) {
                particle.updateLocation(direction, stepSize);

                if(isCollision(particle.getShape())) {
                    particle.setCollided(true);
                    resampleParticle(particle);
                }
            }

            canvas.drawColor(ContextCompat.getColor(this, R.color.colorDark));
            for(Particle particle : particles) {
                particle.getShape().draw(canvas);
            }
            for(ShapeDrawable wall : walls) {
                wall.draw(canvas);
            }

            textview_particle_count.setText("Particles:\n"+particles.size());

        }

        canvasView.invalidate(); //redraw canvas

    }

    public void resampleParticle(Particle particle) {

        while(particle.getCollided()) {
            // Get the x and y coordinates of an uncollided random particle to resample a collided particle to
            int randomParticleID = (int) (Math.random() * particles.size());
            int selectedParticleX = particles.get(randomParticleID).getX();
            int selectedParticleY = particles.get(randomParticleID).getY();

            particle.resample(selectedParticleX, selectedParticleY);

            if (!isCollision(particle.getShape())) {
                particle.setCollided(false);
            }
        }
    }


    // Method that prepares the canvas with particles and layout
    public void prepareCanvas() {
        // create a canvas
        canvasView = (ImageView) findViewById(R.id.canvas);
        blankBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(blankBitmap);
        canvas.drawColor(ContextCompat.getColor(this, R.color.colorDark));
        canvasView.setImageBitmap(blankBitmap);

        particles = this.createParticles();
        walls = this.createLayout();

        // draw the objects
        for (ShapeDrawable wall : walls) {
            wall.draw(canvas);
        }

        for(Particle particle : particles) {
            ShapeDrawable particleShape = particle.getShape();
            if(isCollision(particleShape)) {
                particle.setCollided(true);
                resampleParticle(particle);
            }
        }

        textview_particle_count.setText("Particles:\n"+particles.size());
    }

    // Method that create a number of particles randomnly spread over the layout
    public List<Particle> createParticles() {

        int width = this.getDisplaySize()[0];
        int height = this.getDisplaySize()[1];

        List<Particle> particles = new ArrayList<>();

        for(int i = 0; i < nParticles; i++) {
            int x = (int) (Math.random()*width);
            int y = (int) (Math.random()*height);
            Particle particle = new Particle(x,y);

            particles.add(particle);
            particle.getShape().draw(canvas);
        }

        return particles;
    }

    // Method that determines if the particle collides with a wall or furniture
    private boolean isCollision(ShapeDrawable particle) {
        for(ShapeDrawable wall : walls) {
            if(isCollision(wall,particle))
                return true;
        }
        return false;
    }

    // Helper method that detects collision between two shapes
    private boolean isCollision(ShapeDrawable wall, ShapeDrawable particle) {
        Rect wallShape = new Rect(wall.getBounds());
        return wallShape.intersect(particle.getBounds());
    }

    // Method for step interface
    @Override
    public void step(long timeNs) {
        numSteps++;
        textview_steps.setText("Steps:\n"+numSteps);
    }

    // Method for step interface
    @Override
    public float getSensitivity() {
        return this.sensitivity;
    }

    // Method for step interface
    @Override
    public int getDirection() {

        int correction = (int) this.currentAzimuth-initialOrientation;
        int direction = 0;

        if(correction < 0) {
            correction = 360-Math.abs(correction);
        }

        if(correction >= 45 && correction < 135) { // RIGHT
            direction = 3;
            textview_direction.setText("Direction:\nRIGHT");
        }else if(correction >= 135 && correction < 225) { // DOWN
            direction = 1;
            textview_direction.setText("Direction:\nDOWN");
        }else if(correction >= 225 && correction < 315) { // LEFT
            direction = 2;
            textview_direction.setText("Direction:\nLEFT");
        }else{
            direction = 0;
            textview_direction.setText("Direction:\nUP");
        }

        return direction;
    }

    // Method that creates the layouts
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
            tableBoundary.setBounds(width/2-85, height / 6 * 4 - 65, width/2-45, height / 6 * 4 + 65);
            tableBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorLight));
            walls.add(tableBoundary);

            // Kitchen Island
            ShapeDrawable kitchenBoundary = new ShapeDrawable(new RectShape());
            kitchenBoundary.setBounds(width/2+50, height / 6 * 4 - 80, width/2+95, height / 6 * 4 + 80);
            kitchenBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorLight));
            walls.add(kitchenBoundary);

            // Bed
            ShapeDrawable bedBoundary = new ShapeDrawable(new RectShape());
            bedBoundary.setBounds(2*wallThickness, height / 6 * 2 - 165, width/2-90, height / 6 * 2 - 45);
            bedBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorLight));
            walls.add(bedBoundary);

            // Closet
            ShapeDrawable closetBoundary = new ShapeDrawable(new RectShape());
            closetBoundary.setBounds(width/2-40, 60, width/2-wallThickness, height / 6 - wallThickness);
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

            // Bookcases
            ShapeDrawable booksBoundary = new ShapeDrawable(new RectShape());
            booksBoundary.setBounds(100, height/6*2 + wallThickness, width / 2, height / 6*2 +25);
            booksBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorLight));
            walls.add(booksBoundary);

            // Piano
            ShapeDrawable pianoBoundary = new ShapeDrawable(new RectShape());
            pianoBoundary.setBounds(100, height/6*3-25, width / 2 - 25, height / 6*3 - wallThickness);
            pianoBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorLight));
            walls.add(pianoBoundary);

            // Couch
            ShapeDrawable couchBoundary = new ShapeDrawable(new RectShape());
            couchBoundary.setBounds(width/2+45, height/6*5+20, width/2+85, height / 6 * 5 +140);
            couchBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorLight));
            walls.add(couchBoundary);

            // Sofa Chair
            ShapeDrawable sofaBoundary = new ShapeDrawable(new RectShape());
            sofaBoundary.setBounds(width/2-90, height/6*5-65, width/2-30, height / 6 * 5);
            sofaBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorLight));
            walls.add(sofaBoundary);
        }else{

        }

        return walls;
    }
}
