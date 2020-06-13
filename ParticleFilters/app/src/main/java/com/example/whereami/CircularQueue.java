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
}
