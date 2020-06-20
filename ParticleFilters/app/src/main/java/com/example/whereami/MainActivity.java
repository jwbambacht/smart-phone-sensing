package com.example.whereami;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
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
    private Layout layout;

    private SharedPreferences settingsSharedPreferences;

    // General variables
    private int width, height;
    private String layoutType;
    private int stepSize = 5;
    private float sensitivity;
    private int stepSizeMultiplier;
    private double stepTime;
    private int nParticles;
    private int wallThickness = 5;
    private int numSteps = 0;
    private String[] cellNames = new String[]{"A","B","C","D","E","F","G","H"};

    // Variables for orientation and filtering
    private boolean startFiltering = false;
    private int anchor = 0;
    private long cornerDelay = 0;
    private boolean initialize = false;

    // Sensors and sensor variables
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor accelerometerSensor;
    private double azimuth;
    private StepCounter stepCounter;

    // Queue for azimuth values
    CircularQueue<Double> orientationQueue = new CircularQueue<>(2);
    CircularQueue<Double> anchorQueue = new CircularQueue<>(5);

    ExecutorService executorService = Executors.newFixedThreadPool(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_whereami_icon_white);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Obtain settings from shared preferences and display size
        getSettings();

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

        toggleButtons(false);
        up.setOnClickListener(this);
        down.setOnClickListener(this);
        left.setOnClickListener(this);
        right.setOnClickListener(this);
        reset.setOnClickListener(this);
        init.setOnClickListener(this);
        start.setOnClickListener(this);

        // TextViews
        textview_azimuth = (TextView) findViewById(R.id.textview_azimuth);
        textview_particle_count = (TextView) findViewById(R.id.textview_particle_count);
        textview_steps = (TextView) findViewById(R.id.textview_steps);
        textview_direction = (TextView) findViewById(R.id.textview_direction);

        // TextViews for convergence results
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

        // Get the size of the display
        this.getDisplaySize();

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
            orientationQueue.add(azimuth);

            // Initialize anchor values by taking 5 orientation-values and taking the average as calibrated anchor.
            if(initialize && anchorQueue.size() < 5) {
                anchorQueue.add(azimuth);
            }
            if(initialize && anchorQueue.size() == 5) {
                anchor = (int) anchorQueue.average(anchorQueue);
                initialize = false;
            }

            textview_azimuth.setText("Azimuth:\n" + (int) azimuth);
        }
    };

    // Method that determines whether the accelerometer sensor changed its state
    private SensorEventListener accelerometerEventListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        public void onSensorChanged(SensorEvent event) {

            // If a step is bigger than 60 degrees we do not count a step and set a delay on the timestamp such that the corner step is not taken. Otherwise there may be a result of over-walking, especially when making a 180 degree turn
            if(startFiltering) {
                long timeStamp = TimeUnit.SECONDS.convert(event.timestamp, TimeUnit.NANOSECONDS);

                if(Math.min((int)(((orientationQueue.getLast()-orientationQueue.sum(orientationQueue)/orientationQueue.size())%360)+360)%360,(int) (((orientationQueue.sum(orientationQueue)/orientationQueue.size()-orientationQueue.getLast())%360)+360)%360) > 60) {
                    cornerDelay = timeStamp+1;
                }else{
                    if(timeStamp >= cornerDelay) {
                        stepCounter.count(timeStamp, event.values);
                    }
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
                startFiltering = false;

                try {
                    if (executorService.awaitTermination(0, TimeUnit.SECONDS)) {
                        Intent i = new Intent(this, SettingsActivity.class);
                        this.startActivity(i);
                        return true;
                    } else {
                        executorService.shutdown();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Process the click listeners
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_up: {
                moveParticles(0, false);
                break;
            }
            case R.id.button_down: {
                moveParticles(1,false);
                break;
            }
            case R.id.button_left: {
                moveParticles(2,false);
                break;
            }
            case R.id.button_right: {
                moveParticles(3,false);
                break;
            }
            case R.id.button_reset: {
                startFiltering = false;

                try {
                    if(executorService.awaitTermination(0,TimeUnit.SECONDS)) {
                        anchorQueue = new CircularQueue<>(5);
                        anchor = 0;
                        numSteps = 0;
                        initialize = false;

                        toggleButtons(false);
                        init.setEnabled(true);

                        this.prepareCanvas();
                    }else{
                        executorService.shutdown();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                reset.setText("RESET");
                break;
            }
            case R.id.button_init: {
                initialize = true;
                toggleButtons(true);
                break;
            }
            case R.id.button_start: {
                startFiltering = true;
                toggleButtons(true);
                init.setEnabled(false);
                start.setEnabled(false);
                reset.setText("STOP");

                executorService = Executors.newFixedThreadPool(1);
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
    public void getDisplaySize() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        if(layoutType.equals("Joost")) {
            this.width = size.x * 9 / 16;
            this.height = size.y;
        }
    }

    // Method that retrieves the settings from the sharedpreferences
    public void getSettings() {
        settingsSharedPreferences = getApplicationContext().getSharedPreferences("SETTINGS", 0);

        layoutType = settingsSharedPreferences.getString("layout", "Joost");
        sensitivity = Float.parseFloat(settingsSharedPreferences.getString("sensitivity", "1.0"));
        stepSizeMultiplier = Integer.parseInt(settingsSharedPreferences.getString("stepsize", "5"));
        stepTime = Double.parseDouble(settingsSharedPreferences.getString("steptime","0.3"));
        nParticles = Integer.parseInt(settingsSharedPreferences.getString("particles", "5000"));

        this.getDisplaySize();
    }

    // Method that makes the particles move over the layout, based on the direction and stepsize
    // StepSizeMultiplier is used to optimize the number of steps to the actual walked distance. Requires manual optimization
    public void moveParticles(int direction, boolean step) {

        if(step) {
            numSteps++;
            textview_steps.setText("Steps:\n"+numSteps);
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < stepSizeMultiplier; i++) {
                    for (Particle particle : particles) {
                        particle.move(direction, stepSize);

                        if (isCollision(particle.getShape())) {
                            particle.setCollided(true);
                            resampleParticle(particle,false);
                        }
                    }

                    canvas.drawColor(ContextCompat.getColor(MainActivity.this, R.color.colorDark));

                    for (Particle particle : particles) {
                        particle.getShape().draw(canvas);
                    }

                    canvas = layout.drawSeparators();
                    canvas = layout.drawBoundaries();
                    canvas = layout.drawCellNames();

                    moveHandler.sendMessage(new Message());
                }
            }
        });
    }

    // Handler that returns the weights to the textviews
    private Handler moveHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            canvasView.invalidate();
        }
    };



    // Method that resample a collided particle. It randomly chooses another particle and takes its current location.
    // Resamples based on more random numbers. If collided with a wall, the process repeats until not collided.
    public void resampleParticle(Particle particle, boolean init) {
        while(particle.getCollided()) {
            // Get the x and y coordinates of an uncollided random particle to resample a collided particle to
            int randomParticleID = (int) (Math.random() * particles.size());
            int selectedParticleX = particles.get(randomParticleID).getX();
            int selectedParticleY = particles.get(randomParticleID).getY();

            particle.resample(selectedParticleX, selectedParticleY);

            if (!isCollision(particle.getShape())) {
                particle.setCollided(false);
                if(!init) {
                    particle.lowerWeight();
                }
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

        layout = new Layout(this.width,this.height, this.wallThickness, this.canvas, this);

        particles = createParticles();

        canvas = layout.drawSeparators();
        canvas = layout.drawBoundaries();
        canvas = layout.drawCellNames();

        for(Particle particle : particles) {
            ShapeDrawable particleShape = particle.getShape();
            if(isCollision(particleShape)) {
                particle.setCollided(true);
                resampleParticle(particle,true);
            }
        }

        textview_particle_count.setText("Particles:\n"+particles.size());
        textview_direction.setText("Direction:\n-");
        textview_steps.setText("Steps:\n0");
        textview_azimuth.setText("Azimuth:\n-");
    }

    // Method that create a number of particles randomnly spread over the layout
    public List<Particle> createParticles() {

        List<Particle> particles = new ArrayList<>();

        for(int i = 0; i < nParticles; i++) {
            int x = (int) (Math.random()*width);
            int y = (int) (Math.random()*height);
            Particle particle = new Particle(x,y,width,height,wallThickness);
            particles.add(particle);
            particle.getShape().draw(canvas);
        }

        return particles;
    }

    // Method that determines if the particle collides with a wall or furniture
    private boolean isCollision(ShapeDrawable particle) {
        for(ShapeDrawable boundary : layout.getBoundaries()) {
            if(isCollision(boundary,particle))
                return true;
        }
        return false;
    }

    // Helper method that detects collision between two shapes
    private boolean isCollision(ShapeDrawable boundary, ShapeDrawable particle) {
        Rect boundaryShape = new Rect(boundary.getBounds());
        return boundaryShape.intersect(particle.getBounds());
    }

    // Method for step interface, passing the sensitivity/threshold of step determination
    @Override
    public float getSensitivity() {
        return this.sensitivity;
    }

    // Method for step interface, passing the (average) time per step
    @Override
    public double getStepTime() {
        return this.stepTime;
    };

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

}
