package com.example.whereami;

public class Util {

    public static int maxValue(float[] array) {
        float max = 0;
        int index = 0;

        for (int i = 0; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
                index = i;
            }
        }

        return index;
    }

    public static float[] normalize(float[] array) {
        float sum = 0;
        for(int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        for(int i = 0; i < array.length; i++) {
            array[i] = array[i]/sum;
        }

        return array;
    }
}
