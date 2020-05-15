package com.example.whereami;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;

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
        database.execSQL("CREATE TABLE IF NOT EXISTS networks (ID INTEGER PRIMARY KEY AUTOINCREMENT, BSSID VARCHAR(40), SSID VARCHAR(40), RSSI INTEGER, cellID INTEGER)");
    }

    static boolean exportSamples(SQLiteDatabase database) {

        Util.createDatabases(database);

        Cursor cursor = null;

        try {
            cursor = database.rawQuery("SELECT * FROM networks", null);
            File sdCardDir = Environment.getExternalStorageDirectory();
            String filename = "database_BAK.csv";

            File saveFile = new File(sdCardDir, filename);
            FileWriter fileWriter = new FileWriter(saveFile);

            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            int rowCount = cursor.getCount();
            int colCount = cursor.getColumnCount();

            if(rowCount == 0) {
                return false;
            }

            if (rowCount > 0) {
                cursor.moveToFirst();
                for (int i = 0; i < colCount; i++) {
                    if (i != colCount - 1) {
                        bufferedWriter.write(cursor.getColumnName(i) + ",");
                    } else {
                        bufferedWriter.write(cursor.getColumnName(i));
                    }
                }
                bufferedWriter.newLine();

                for (int i = 0; i < rowCount; i++) {
                    cursor.moveToPosition(i);

                    for (int j = 0; j < colCount; j++) {
                        if (j != colCount - 1)
                            bufferedWriter.write(cursor.getString(j) + ",");
                        else
                            bufferedWriter.write(cursor.getString(j));
                    }
                    bufferedWriter.newLine();
                }
                bufferedWriter.flush();
            }
        } catch (Exception ex) {
            if (database.isOpen()) {
                database.close();
            }
        }

        return true;
    }

    // Method that performs a network scan and inserts them into the database
    static void findNetworks(WifiManager wifiManager, SQLiteDatabase database, int cellID) {

        wifiManager.startScan();

        // Sense networks and add to database
        for (ScanResult scan : wifiManager.getScanResults()) {
            ContentValues networkRow = new ContentValues();
            networkRow.put("BSSID", scan.BSSID);
            networkRow.put("SSID", scan.SSID);
            networkRow.put("RSSI",scan.level);
            networkRow.put("cellID", cellID);

            database.insert("networks", null, networkRow);

            Log.i("Network",scan.BSSID + "-"+scan.level);
        }
    }

    // Method that determines the location by use of Bayes
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