package com.example.whereami;

public class StepCounter {

    private StepListener listener;
    private int accelerationHistory = 50;
    private int accelerationDifferenceHistory = 10;
    private double previousAccelerationDifference = 0;
    private long previousTimestamp = 0;
    private CircularQueue<Double> accelerationQueueX = new CircularQueue(accelerationHistory);
    private CircularQueue<Double> accelerationQueueY = new CircularQueue(accelerationHistory);
    private CircularQueue<Double> accelerationQueueZ = new CircularQueue(accelerationHistory);
    private CircularQueue<Double> accelerationDifferenceQueue = new CircularQueue(accelerationDifferenceHistory);

    public void registerListener(StepListener listener) {
        this.listener = listener;
    }

    // Method that counts a step if it satisfies the correct conditions. This is not a real step counter
    public void count(long timestamp, float[] acceleration) {

        // Add acceleration values to queue of previous acceleration values for each axis
        accelerationQueueX.add((double) acceleration[0]);
        accelerationQueueY.add((double) acceleration[1]);
        accelerationQueueZ.add((double) acceleration[2]);

        // Determine average acceleration for each axis
        double[] averageAcceleration = new double[3];
        averageAcceleration[0] = accelerationQueueX.average(accelerationQueueX);
        averageAcceleration[1] = accelerationQueueY.average(accelerationQueueY);
        averageAcceleration[2] = accelerationQueueZ.average(accelerationQueueZ);

        // Determine magnitude of acceleration vector as gravity and normalize the average acceleration
        double gravity = Math.sqrt(averageAcceleration[0]*averageAcceleration[0]+averageAcceleration[1]*averageAcceleration[1]+averageAcceleration[2]*averageAcceleration[2]);
        double[] normalizedAcceleration = new double[3];
        normalizedAcceleration[0] = averageAcceleration[0]/gravity;
        normalizedAcceleration[1] = averageAcceleration[1]/gravity;
        normalizedAcceleration[2] = averageAcceleration[2]/gravity;

        // Determine the magnitude of the acceleration difference over all axis with gravity subtracted.
        // Add to queue and take the average acceleration difference over all previous values.
        accelerationDifferenceQueue.add((normalizedAcceleration[0]*acceleration[0]+normalizedAcceleration[1]*acceleration[1]+normalizedAcceleration[2]*acceleration[2])-gravity);
        double accelerationDifference = accelerationDifferenceQueue.average(accelerationDifferenceQueue);

        // Obtain sensitivity/threshold from preferences. Manually optimization required for best result.
        double threshold = listener.getSensitivity();
        double stepTime = listener.getStepTime();

        // Average acceleration difference must be bigger than the defined threshold and the previous average acceleration difference must be smaller than the threshold.
        // This means that the phone accelerated in upward direction (increasing acceleration). The time to the previous step also should be at least some predefined value.
        // If all these conditions satisfy a step is counted and the particles will move in the current direction.
        if(accelerationDifference > threshold && previousAccelerationDifference <= threshold && (timestamp-previousTimestamp) >= stepTime) {
            int direction = listener.getDirection();
            listener.moveParticles(direction,true);
            previousTimestamp = timestamp;
        }

        previousAccelerationDifference = accelerationDifference;
    }
}