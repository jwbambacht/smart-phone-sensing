package com.example.whereami;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Util  extends AppCompatActivity {

    static List<Sample> loadSamples(SharedPreferences sharedPreferences) {

        Gson gson = new Gson();
        String json = sharedPreferences.getString("samples",null);
        Type type = new TypeToken<ArrayList<Sample>>() {}.getType();
        List<Sample> samples = gson.fromJson(json,type);

        if(samples == null) {
            samples = new ArrayList<>();
        }

        Log.i("Total saved samples",""+samples.size());

        for(Sample s : samples) {
            HashMap<String,Integer> networks = s.getNetworks();
            Log.i("BSSID/RSSI for ","network");

            for (Map.Entry<String,Integer> entry : networks.entrySet()) {
                Log.i("", entry.getKey() + " / " + entry.getValue());
            }
        }

        return samples;
    }

    static void saveSamples(SharedPreferences sharedPreferences, List<Sample> samples) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(samples);
        editor.putString("samples",json);
        editor.apply();

        Log.i("Samples saved!","");
    }

    static void resetSamples(SharedPreferences sharedPreferences) {
        try {
            List<Sample> samples = new ArrayList<>();
            saveSamples(sharedPreferences,samples);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static int getPrecision(SharedPreferences sharedPreferences) {
        try {
            return sharedPreferences.getInt("precision",1);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String getMethod(SharedPreferences sharedPreferences) {
        try {
            return sharedPreferences.getString("method","KNN");
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void setPrecision(SharedPreferences sharedPreferences, int precision) {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("precision",precision);
            editor.commit();
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void setMethod(SharedPreferences sharedPreferences, String method) {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("method",method);
            editor.commit();
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
