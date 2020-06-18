package com.example.whereami;

import java.util.LinkedList;

public class CircularQueue<Double> extends LinkedList<Double> {
    private int capacity = 5;

    public CircularQueue(int capacity){
        this.capacity = capacity;
    }

    @Override
    public boolean add(Double element) {
        if(size() >= capacity)
            removeFirst();
        return super.add(element);
    }

    public double sum(CircularQueue<java.lang.Double> queue) {
        double sum = 0;

        for(int i = 0; i < queue.size(); i++) {
            sum += queue.get(i);
        }
        return sum;
    }

    public double average(CircularQueue<java.lang.Double> queue) {
        return queue.sum(queue)/queue.size();
    }
}
