package com.example.whereami;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class AccelerometerListener extends Activity implements SensorEventListener {
    public static final int MIN_MAX = 0;
    private static final int MEASUREMENTS_PER_SAMPLE = 200;//how many measurements to save

    /* How much of the previous measurement should account for the current measurement? The lower, the quicker sensor values are pushed to 0 */
    private static final float alpha = .4f;

    private final SensorManager mSensorManager;
    private final Sensor mAccelerometer;
    private float[] gravity = new float[3];

    Deque<float[]> measurements = new ArrayDeque<>();

    public AccelerometerListener(SensorManager mSensorManager) {
        this.mSensorManager = mSensorManager;
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        float[] linear_acceleration = new float[3];
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];
        /*Log.i("Accelerometer", String.format("[%.2f,%.2f,%.2f]",
                linear_acceleration[0], linear_acceleration[1], linear_acceleration[2]));*/

        measurements.addFirst(linear_acceleration);
        if(measurements.size() > MEASUREMENTS_PER_SAMPLE){
            measurements.removeLast();
        }
    }

    public float[] getFeature(int feature){
        float[] data = new float[3];
        switch (feature) {
            case MIN_MAX:
                float[] min = new float[3];
                float[] max = new float[3];
                min[0] = 0;
                min[1] = 0;
                min[2] = 0;
                max[0] = 0;
                max[1] = 0;
                max[2] = 0;
                Iterator it = measurements.iterator();
                while(it.hasNext()){
                    float[] measurement = (float[]) it.next();
                    //Log.i("current measurement",""+measurement[0]);
                    if(measurement[0] > max[0]){
                        max[0] = measurement[0];
                    } else if (measurement[0] < min[0]){
                        min[0] = measurement[0];
                    }
                    if(measurement[1] > max[1]){
                        max[1] = measurement[1];
                    } else if (measurement[1] < min[1]){
                        min[1] = measurement[1];
                    }
                    if(measurement[2] > max[2]){
                        max[2] = measurement[2];
                    } else if (measurement[2] < min[2]){
                        min[2] = measurement[2];
                    }
                }
                data[0] = max[0] - min[0];
                data[1] = max[1] - min[1];
                data[2] = max[2] - min[2];
                break;
        }
        return data;
    }
}
