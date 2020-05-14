package com.example.whereami;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Util  extends AppCompatActivity {

    // Method that removes all saved samples
    static void resetSamples(SQLiteDatabase database) {
        try {
            database.execSQL("DROP TABLE IF EXISTS networks");

            Util.createDatabases(database);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void createDatabases(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE IF NOT EXISTS networks (ID INTEGER PRIMARY KEY AUTOINCREMENT, BSSID VARCHAR(40), RSSI INTEGER, cellID INTEGER)");
    }

    // Method that performs a network scan
    static void findNetworks(WifiManager wifiManager, SQLiteDatabase database, int cellID) {

        wifiManager.startScan();

        // Sense networks and add to database
        for (ScanResult scan : wifiManager.getScanResults()) {
            ContentValues networkRow = new ContentValues();
            networkRow.put("BSSID", scan.BSSID);
            networkRow.put("RSSI",scan.level);
            networkRow.put("cellID", cellID);

            database.insert("networks", null, networkRow);

            Log.i("Network",scan.BSSID + "-"+scan.level);
        }
    }

    static float[] BayesianLocalization(HashMap<String, Integer> networks, float[] cellBeliefs) {

        return cellBeliefs;
    };

    // Method to find index of maximum value in float array
    static int findMaxValue(float[] results) {
        int index = 0;
        float max = results[0];

        for(int i = 1; i < results.length; i++) {
            if(results[i] > max) {
                max = results[i];
                index = i;
            }
        }

        return index;
    }


}