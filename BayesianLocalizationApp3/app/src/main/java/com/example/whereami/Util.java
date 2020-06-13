package com.example.whereami;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Util  extends AppCompatActivity {

    // Create Database
    static void createDatabases(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE IF NOT EXISTS networks (ID INTEGER PRIMARY KEY AUTOINCREMENT, BSSID VARCHAR(40), SSID VARCHAR(40), RSSI INTEGER, cellID INTEGER, type VARCHAR(10), scanID INTEGER)");
    }

    // Method that removes all saved samples and initiates re-creation of database
    static void resetSamples(SQLiteDatabase database) {
        try {
            database.execSQL("DROP TABLE IF EXISTS networks");
            Util.createDatabases(database);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Method that gets the maximum scanID from the networks table
    static int getMaximumScanID(SQLiteDatabase database) {
        int maxID = -1;
        Cursor cursor = null;

        try {
            cursor = database.rawQuery("SELECT MAX(scanID) from networks",null);

            if(cursor.getCount() == 0) {
                return maxID;
            }

            cursor.moveToFirst();
            maxID = cursor.getInt(0);
            cursor.close();
        }catch (Exception e) {
            e.printStackTrace();
        }

        return maxID;
    }

    // Method that converts the samples in the SQLLite database to a CSV file and writes it to the phone storage
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
                    if (j != colCount - 1) {
                        bufferedWriter.write(cursor.getString(j) + ",");
                    } else {
                        bufferedWriter.write(cursor.getString(j));
                    }
                }
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return true;
    }

    // Method that imports the processed data
    static HashMap<String,Network> readData(Context context) {
        InputStream input = context.getResources().openRawResource(R.raw.data_to_phone_app2);

        BufferedReader reader = new BufferedReader(new InputStreamReader(input, Charset.forName("UTF-8")));
        String line = "";

        HashMap<String, Network> networks = new HashMap<String,Network>();

        try {
            while ((line = reader.readLine()) != null) {
                // Split the line into different tokens (using the comma as a separator).
                String[] tokens = line.split(",");

                Network network;

                String BSSID = tokens[0];
                int cellID = Integer.parseInt(tokens[1]);
                BigDecimal[] probabilities = new BigDecimal[100];

                for(int i = 0; i < 100; i++) {
                    probabilities[i] = new BigDecimal(tokens[i+2]);
                }

                if(cellID == 0) {
                    network = new Network(BSSID);
                }else{
                    network = networks.get(BSSID);
                }

                network.setCellProbabilities(cellID,probabilities);

                networks.put(BSSID,network);

                Log.i("Added "+BSSID+" to list, cell "+cellID,"");
                Log.i("Probabilities",Arrays.toString(probabilities));
            }
        } catch (IOException e1) {
            Log.e("Error reading csv", "Error" + line, e1);
            e1.printStackTrace();
        }

        return networks;
    }

    // Method that finds the number of samples per cell in the database
    static int getTrainingCount(SQLiteDatabase database, int cellID) {
        int count = 0;
        Cursor cursor = null;

        try {
            String[] args = {""+cellID};
            cursor = database.rawQuery("SELECT * from networks WHERE cellID = ? AND type = 'training' GROUP BY scanID",args);

            count = cursor.getCount();

            cursor.close();
        }catch (Exception e) {
            e.printStackTrace();
        }

        return count;

    }

    // Method that performs a network scan and inserts them into the database
    static void findNetworks(WifiManager wifiManager, SQLiteDatabase database, int cellID, boolean testing, int scanID) {

        wifiManager.startScan();

        // Sense networks and add to database
        for (ScanResult scan : wifiManager.getScanResults()) {
            ContentValues networkRow = new ContentValues();
            networkRow.put("BSSID", scan.BSSID);
            networkRow.put("SSID", scan.SSID);
            networkRow.put("RSSI", scan.level);
            networkRow.put("cellID", cellID);
            networkRow.put("scanID", scanID);

            if(testing) {
                networkRow.put("type", "testing");
            }else{
                networkRow.put("type", "training");
            }

            database.insert("networks", null, networkRow);
        }
    }

    // Method that determines the location by use of Bayes
    static BigDecimal[] BayesianLocalization(BigDecimal[] prior, WifiManager wifiManager, HashMap<String,Network> networks) {

        wifiManager.startScan();

        // Include scanned APs only if the BSSID is also in the list of processed networks
        // Use the absolute value for RSSI to be able to simply take the RSSI value as index
        List<ResultScan> resultScans = new ArrayList<>();
        for(ScanResult scanResult : wifiManager.getScanResults()) {
            if(networks.containsKey(scanResult.BSSID)) {
                resultScans.add(new ResultScan(scanResult.BSSID,Math.abs(scanResult.level)));
            }
        }

        // Sort scanned APs on best (lowest) positive RSSI
        Collections.sort(resultScans);

        // Create posterior
        BigDecimal[] posterior = new BigDecimal[8];

        // For each sense scan only include the APs that have an RSSI of 75 or lower
        int index = 0;
        while(resultScans.get(index).getRSSI() <= 75 && index < resultScans.size()-1 && index < 4) {
            ResultScan result = resultScans.get(index);
            String BSSID = result.getBSSID();
            int RSSI = result.getRSSI();
            BigDecimal norm_sum = new BigDecimal(0);

            BigDecimal[] probs = networks.get(BSSID).getProbabilitiesForRSSI(RSSI);

            // Normalize posterior
            for(int j = 0; j < 8; j++) {
                posterior[j] = prior[j].multiply(probs[j]);
                norm_sum = norm_sum.add(posterior[j]);
            }
            for(int j = 0; j < 8; j++) {
                posterior[j] = posterior[j].divide(norm_sum, 300, RoundingMode.CEILING);
            }

            prior = posterior;
            index++;
        }

        return posterior;
    };

    // Method to find index of maximum value in BigDecimal array
    static int findMaxBigDecimal(BigDecimal[] results) {
        int index = 0;
        BigDecimal max = results[0];

        for(int i = 1; i < results.length; i++) {
            if(results[i].compareTo(max) > 0) {
                max = results[i];
                index = i;
            }
        }

        return index;
    }

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