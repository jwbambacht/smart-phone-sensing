package com.example.whereami;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class MainActivity extends AppCompatActivity implements OnClickListener, StepListener {

    // UI Elements
    private Button up, left, right, down, reset, init, start;
    private TextView textview_azimuth, textview_particle_count, textview_steps, textview_direction;
    private List<TextView> cellResults;
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
    private double stepTime;
    private int nParticles;
    private int numSteps;
    private String[] cellNames;

    // Variables for orientation and filtering
    private boolean startFiltering;
    private boolean initializeOrientation;
    private boolean anchorGathered;
    private int anchorCount;
    private int anchor;
    private int[] anchorVector;

    // Sensors and sensor variables
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor accelerometerSensor;
    private double azimuth, lastAzimuth;
    private StepCounter stepCounter;

    CircularQueue<Double> queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_whereami_icon_white);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Obtain settings from shared preferences and display size
        getSettings();

        // Set variables
        stepSize = 7;
        numSteps = 0;
        startFiltering = false;
        initializeOrientation = false;
        anchorCount = 0;
        anchorGathered = false;
        anchor = 0;
        cellNames = new String[]{"A","B","C","D","E","F","G","H"};

        queue = new CircularQueue<>(2);

        // Sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        stepCounter = new StepCounter();
        stepCounter.registerListener(this);
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

        // TextViews
        textview_azimuth = (TextView) findViewById(R.id.textview_azimuth);
        textview_particle_count = (TextView) findViewById(R.id.textview_particle_count);
        textview_steps = (TextView) findViewById(R.id.textview_steps);
        textview_direction = (TextView) findViewById(R.id.textview_direction);

        cellResults = new ArrayList<>();
        cellResults.add((TextView) findViewById(R.id.textview_cell_A));
        cellResults.add((TextView) findViewById(R.id.textview_cell_B));
        cellResults.add((TextView) findViewById(R.id.textview_cell_C));
        cellResults.add((TextView) findViewById(R.id.textview_cell_D));
        cellResults.add((TextView) findViewById(R.id.textview_cell_E));
        cellResults.add((TextView) findViewById(R.id.textview_cell_F));
        cellResults.add((TextView) findViewById(R.id.textview_cell_G));
        cellResults.add((TextView) findViewById(R.id.textview_cell_H));

        for(int i = 0; i < cellResults.size(); i++) {
            cellResults.get(i).setText("Cell "+cellNames[i]+":\n0.1250");
        }

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
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        public void onSensorChanged(SensorEvent event) {

            float[] rotationMatrix = new float[16];

            // Convert quaternion into a rotation matrix
            // Instead of remapping the rotation matrix and obtain the orientation from that we take the arctan of two values, giving the rotation in the z-axis.
            // We convert the rotation to degrees and take the modulo of 360 to obtain degrees aranging from 0 to 360.
            // Pitch and roll could also be determined this way but we are only interested in the orientation.
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            azimuth = (double) ((float) Math.toDegrees((float) Math.atan2(rotationMatrix[1],rotationMatrix[5]))+360) % 360;

            // Add azimuth to queue to be able to detect direction changes without counting a step
            queue.add(azimuth);

            // If init button is pressed we obtain 5 values for azimuth and take the average for initial orientation
            if(initializeOrientation && !anchorGathered && anchorCount < 5) {
                anchor += (int) azimuth;
                anchorCount++;

                if(anchorCount == 5) {
                    initializeOrientation = false;
                    anchorGathered = true;
                    anchor = anchor/5;
                }
            }

            // If start button is pressed we detect a change in azimuth if the difference is big enough to compensate for sensitivity
            if(startFiltering) {
                if(Math.min((int)(((azimuth-lastAzimuth)%360)+360)%360,(int) (((lastAzimuth-azimuth)%360)+360)%360) >= 50) {
                    lastAzimuth = azimuth;
                }
            }
            textview_azimuth.setText("Azimuth:\n" + (int) azimuth);
        }
    };

    // Method that determines whether the accelerometer sensor changed its state
    private SensorEventListener accelerometerEventListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        public void onSensorChanged(SensorEvent event) {

            // To account for changes in direction (90 degrees) we only allow a step to be included if the (absolute) rotation is smaller than 30 degrees
            if(startFiltering) {
                if(Math.min((int)(((queue.getLast()-queue.sum(queue)/queue.size())%360)+360)%360,(int) (((queue.sum(queue)/queue.size()-queue.getLast())%360)+360)%360) < 60) {
                    stepCounter.count(TimeUnit.SECONDS.convert(event.timestamp, TimeUnit.NANOSECONDS), event.values);
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
                anchorGathered = false;
                anchorVector = new int[5];
                anchor = 0;
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
                start.setEnabled(false);

                currentCellThread.start();
                break;
            }
        }
    }

    // Thread for calculation of weights of all particles in each cell
    private Thread currentCellThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {

                while(startFiltering) {
                    float[] cellWeights = new float[]{0,0,0,0,0,0,0,0};

                    for(Particle particle : particles) {
                        int currentCell = particle.getCurrentCell();
                        cellWeights[currentCell] += particle.getWeight();
                    }

                    float[] weightRes = Util.normalize(cellWeights);

                    Message message = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putFloatArray("weights",weightRes);
                    message.setData(bundle);
                    currentCellHandler.sendMessage(message);
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });

    // Handler that returns the weights to the textviews
    private Handler currentCellHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            Bundle bundle = message.getData();
            float[] keys = bundle.getFloatArray("weights");

            int maxValue = Util.maxValue(keys);

            for(int i = 0; i < keys.length; i++) {
                if(i == maxValue) {
                    cellResults.get(i).setBackgroundResource(R.drawable.cell_closed);
                    cellResults.get(i).setTextColor(getResources().getColor(R.color.colorDark));
                }else{
                    cellResults.get(i).setBackgroundResource(R.drawable.cell_open);
                    cellResults.get(i).setTextColor(getResources().getColor(R.color.colorLight));
                }
                cellResults.get(i).setText("Cell "+cellNames[i]+":\n"+String.format("%.4f",keys[i]));
            }
        }
    };

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
        }

        return sizes;
    }

    // Method that retrieves the settings from the sharedpreferences
    public void getSettings() {
        settingsSharedPreferences = getApplicationContext().getSharedPreferences("SETTINGS", 0);

        layout = settingsSharedPreferences.getString("layout", "Joost");
        sensitivity = Float.parseFloat(settingsSharedPreferences.getString("sensitivity", "10"));
        stepSizeMultiplier = Integer.parseInt(settingsSharedPreferences.getString("stepsize", "1"));
        stepTime = Double.parseDouble(settingsSharedPreferences.getString("steptime","0.5"));
        nParticles = Integer.parseInt(settingsSharedPreferences.getString("particles", "5000"));

        width = this.getDisplaySize()[0];
        height = this.getDisplaySize()[1];
    }

    // Method that makes the particles move over the layout, based on the direction and stepsize
    // StepSizeMultiplier is used to optimize the number of steps to the actual walked distance. Requires manual optimization

    public void moveParticles(int direction, boolean step) {
        numSteps++;
        textview_steps.setText("Steps:\n"+numSteps);

        moveParticles(direction);
    }

    public void moveParticles(int direction) {
        for(int i = 0; i < stepSizeMultiplier; i++) {

            for (Particle particle : particles) {
                particle.updateLocation(direction, stepSize);

                if (isCollision(particle.getShape())) {
                    particle.setCollided(true);
                    resampleParticle(particle);
                }
            }

            canvas.drawColor(ContextCompat.getColor(MainActivity.this, R.color.colorDark));
            canvas.save();
            for (Particle particle : particles) {
                particle.getShape().draw(canvas);
            }
            for (ShapeDrawable wall : walls) {
                wall.draw(canvas);
            }

            canvasView.invalidate();
        }
    }

    // Method that resample a collided particle. It randomly chooses another particle and takes its current location.
    // Resamples based on more random numbers. If collided with a wall, the process repeats until not collided.
    public void resampleParticle(Particle particle) {
        while(particle.getCollided()) {
            // Get the x and y coordinates of an uncollided random particle to resample a collided particle to
            int randomParticleID = (int) (Math.random() * particles.size());
            int selectedParticleX = particles.get(randomParticleID).getX();
            int selectedParticleY = particles.get(randomParticleID).getY();

            particle.resample(selectedParticleX, selectedParticleY);

            if (!isCollision(particle.getShape())) {
                particle.setCollided(false);
                particle.lowerWeight();
            }
        }
    }

    // Method that prepares the canvas with particles and layout
    public void prepareCanvas() {
        canvasView = (ImageView) findViewById(R.id.canvas);
        blankBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(blankBitmap);
        canvas.drawColor(ContextCompat.getColor(this, R.color.colorDark));
        canvasView.setImageBitmap(blankBitmap);

        particles = this.createParticles();
        walls = this.createLayout();

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
        textview_direction.setText("Direction:\n-");
        textview_steps.setText("Steps:\n0");
        textview_azimuth.setText("Azimuth:\n-");
    }

    // Method that create a number of particles randomnly spread over the layout
    public List<Particle> createParticles() {

        int width = this.getDisplaySize()[0];
        int height = this.getDisplaySize()[1];

        List<Particle> particles = new ArrayList<>();

        for(int i = 0; i < nParticles; i++) {
            int x = (int) (Math.random()*width);
            int y = (int) (Math.random()*height);
            Particle particle = new Particle(x,y,nParticles,width,height);
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

    // Method for step interface, passing the sensitivity/threshold of step determination
    @Override
    public float getSensitivity() {
        return this.sensitivity;
    }

    // Method for step interface, passing the direction to walk
    @Override
    public int getDirection() {

        int correction = (int) this.azimuth-anchor;
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

    // Method for step interface, passing the (average) time per step
    @Override
    public double getStepTime() {
        return this.stepTime;
    };

    // Method that creates the layouts
    public List<ShapeDrawable> createLayout() {
        List<ShapeDrawable> walls = new ArrayList<>();

        int wallThickness = 5;

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
            ShapeDrawable ccoBoundary = new ShapeDrawable(new RectShape());
            ccoBoundary.setBounds(width / 2 + 95, height / 6, width - wallThickness, height / 6 * 3 + 50);
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
            eobBoundary.setBounds(0, height / 6 * 3 - wallThickness, 80, height / 6 * 5 + wallThickness);
            eobBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(eobBoundary);
            ShapeDrawable eotBoundary = new ShapeDrawable(new RectShape());
            eotBoundary.setBounds(width - 50, height / 6 * 3 + 20, width - wallThickness, height / 6 * 5 + wallThickness);
            eotBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorMuted));
            walls.add(eotBoundary);

            // Dinner Table
            ShapeDrawable tableBoundary = new ShapeDrawable(new RectShape());
            tableBoundary.setBounds(width/2-95, height / 6 * 4 - 65, width/2-45, height / 6 * 4 + 65);
            tableBoundary.getPaint().setColor(ContextCompat.getColor(this, R.color.colorLight));
            walls.add(tableBoundary);

            // Kitchen Island
            ShapeDrawable kitchenBoundary = new ShapeDrawable(new RectShape());
            kitchenBoundary.setBounds(width/2+50, height / 6 * 4 - 75, width/2+100, height / 6 * 4 + 75);
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
        }

        return walls;
    }
}
