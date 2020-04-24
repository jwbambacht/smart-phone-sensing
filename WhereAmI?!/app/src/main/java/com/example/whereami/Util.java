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

    static List<Location> loadLocations(SharedPreferences sharedPreferences) {

        Gson gson = new Gson();
        String json = sharedPreferences.getString("locations",null);
        Type type = new TypeToken<ArrayList<Location>>() {}.getType();
        List<Location> locations = gson.fromJson(json,type);

        if(locations == null) {
            locations = new ArrayList<>();
        }

        Log.i("Total saved locations",""+locations.size());

        return locations;
    }

    static void saveLocations(SharedPreferences sharedPreferences, List<Location> locations) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(locations);
        editor.putString("locations",json);
        editor.apply();

        Log.i("Locations saved!","");
    }

    static void resetLocations(SharedPreferences sharedPreferences) {
        try {
            List<Location> locations = new ArrayList<>();
            saveLocations(sharedPreferences,locations);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
