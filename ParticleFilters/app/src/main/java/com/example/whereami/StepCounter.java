package com.example.whereami;

public class StepCounter {

    private StepListener listener;
    private int accelerationHistory = 50;
    private int velocityHistory = 10;
    private double previousVelocity = 0;
    private long previousTimestamp = 0;
    private CircularQueue<Double> accelerationQueueX = new CircularQueue(accelerationHistory);
    private CircularQueue<Double> accelerationQueueY = new CircularQueue(accelerationHistory);
    private CircularQueue<Double> accelerationQueueZ = new CircularQueue(accelerationHistory);
    private CircularQueue<Double> velocityQueue = new CircularQueue(velocityHistory);

    public void registerListener(StepListener listener) {
        this.listener = listener;
    }

    public void count(long timestamp, float[] acceleration) {

        // Add acceleration values to queue of previous acceleration values for each axis
        accelerationQueueX.add((double) acceleration[0]);
        accelerationQueueY.add((double) acceleration[1]);
        accelerationQueueZ.add((double) acceleration[2]);

        // Determine average acceleration for each axis
        double[] averageAcceleration = new double[3];
        averageAcceleration[0] = accelerationQueueX.sum(accelerationQueueX)/accelerationQueueX.size();
        averageAcceleration[1] = accelerationQueueY.sum(accelerationQueueY)/accelerationQueueY.size();
        averageAcceleration[2] = accelerationQueueZ.sum(accelerationQueueZ)/accelerationQueueZ.size();

        // Determine magnitude of acceleration vector as gravity and normalize the average acceleration
        double gravity = Math.sqrt(averageAcceleration[0]*averageAcceleration[0]+averageAcceleration[1]*averageAcceleration[1]+averageAcceleration[2]*averageAcceleration[2]);
        double[] normalizedAcceleration = new double[3];
        normalizedAcceleration[0] = averageAcceleration[0]/gravity;
        normalizedAcceleration[1] = averageAcceleration[1]/gravity;
        normalizedAcceleration[2] = averageAcceleration[2]/gravity;

        // Determine velocity in vertical direction, with gravity subtracted, and add to queue of velocity values
        double velocity = (normalizedAcceleration[0]*acceleration[0]+normalizedAcceleration[1]*acceleration[1]+normalizedAcceleration[2]*acceleration[2])-gravity;
        velocityQueue.add(velocity);
        double averageVelocity = velocityQueue.sum(velocityQueue);

        // Obtain sensitivity from preferences. Manually optimization required for best result.
        double threshold = listener.getSensitivity();
        double stepTime = listener.getStepTime();

        // If the average velocity is bigger than the defined threshold and the previous average velocity was smaller
        // (meaning the phone accelerated in vertical position), and the threshold is bigger than a defined time taken
        // for a step, a step is approved and the particles are moved. If not,
        if(averageVelocity > threshold && previousVelocity <= threshold && (timestamp-previousTimestamp) >= stepTime) {
            int direction = listener.getDirection();
            listener.moveParticles(direction,true);
            previousTimestamp = timestamp;
        }

        previousVelocity = averageVelocity;
    }
}
