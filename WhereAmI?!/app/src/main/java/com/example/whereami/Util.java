package com.example.whereami;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Util  extends AppCompatActivity {

    static List<Sample> loadSamples(SharedPreferences sharedPreferences) {

        Gson gson = new Gson();
        String json = sharedPreferences.getString("locations",null);
        Type type = new TypeToken<ArrayList<Sample>>() {}.getType();
        List<Sample> samples = gson.fromJson(json,type);

        if(samples == null) {
            samples = new ArrayList<>();
        }

        Log.i("Total saved samples",""+samples.size());

        return samples;
    }

    static void saveSamples(SharedPreferences sharedPreferences, List<Sample> locations) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(locations);
        editor.putString("samples",json);
        editor.apply();

        Log.i("Samples saved!","");
    }

    static void resetSamples(SharedPreferences sharedPreferences) {
        try {
            List<Sample> locations = new ArrayList<>();
            saveSamples(sharedPreferences,locations);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
