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

    // Method that obtains probabilities for a given AP and RSSI
    static double[] getProbabilities(SQLiteDatabase database, String BSSID, int RSSI) {
        double[] probs = new double[8];

        for(int cellID = 0; cellID < 8; cellID++) {
            try{
                Cursor cellCursor = database.rawQuery("SELECT * FROM networks WHERE BSSID = ? AND cellID = ?", new String[]{BSSID,""+cellID});

                cellCursor.moveToFirst();

                List<Integer> samples = new ArrayList<>();

                while(cellCursor.moveToNext()) {
                    samples.add(Math.abs(cellCursor.getInt(cellCursor.getColumnIndex("RSSI"))));
                }

                probs[cellID] = Util.gaussianKernelProbability(RSSI, samples);
                cellCursor.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return probs;
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

    // Method that determines all BSSIDs that are included in the probabilities table
    static List<String> getNetworkNames(SQLiteDatabase database) {
        List<String> networks = new ArrayList<>();

        Cursor cursor = database.rawQuery("SELECT DISTINCT BSSID from networks",null);

        while(cursor.moveToNext()) {
            networks.add(cursor.getString(cursor.getColumnIndex("BSSID")));
        }

        return networks;
    }

    // Method that determines the location by use of Bayes
    static double[] BayesianLocalization(SQLiteDatabase database, double[] prior, WifiManager wifiManager, List<String> networkNames) {

        wifiManager.startScan();

        // Temporary AP names that should not be included in the result
        String[] filter_names = new String[]{"AndroidAP","iPhone", "Nokia", "OnePlus", "HUAWEI", "LG"};

        // Include scanned APs only if the BSSID is also in the list of processed networks
        // Use the absolute value for RSSI to be able to simply take the RSSI value as index
        List<ResultScan> resultScans = new ArrayList<>();
        for(ScanResult scanResult : wifiManager.getScanResults()) {

            // Do not process temporary APs, based on general AP names of Android, iOS, and other vendors
            boolean include = true;
            for(String name : filter_names) {
                if (scanResult.SSID.contains(name)) {
                    include = false;
                    break;
                }
            }

            if(networkNames.contains(scanResult.BSSID) && include) {
                resultScans.add(new ResultScan(scanResult.BSSID,Math.abs(scanResult.level)));
            }
        }

        // Sort scanned APs on best (lowest) positive RSSI
        Collections.sort(resultScans);

        // Create posterior
        double[] posterior = new double[8];

        // For each sense scan only include the APs that have an RSSI of 75 or lower
        int index = 0;
//        while(resultScans.get(index).getRSSI() <= 75 && index < resultScans.size()-1 && index < 4) {
//        for(ResultScan result : resultScans) {
        while(index < resultScans.size()-1) {
            ResultScan result = resultScans.get(index);
            String BSSID = result.getBSSID();

            int RSSI = result.getRSSI();
            double norm_sum = 0;

//            double[] probs = networks.get(BSSID).getProbabilitiesForRSSI(RSSI);

            System.out.println("BSSID: "+BSSID+", RSSI:" +RSSI);
            double[] probs = Util.getProbabilities(database,BSSID,RSSI);

            System.out.println(Arrays.toString(probs));

            // Normalize posterior
            for(int j = 0; j < 8; j++) {
                posterior[j] = prior[j]*probs[j];
                norm_sum += posterior[j];
            }
            for(int j = 0; j < 8; j++) {
                posterior[j] = posterior[j]/norm_sum;
            }

            prior = posterior;
            index++;
        }

        return posterior;
    };

    // Method to find index of maximum value in double array
    static int findMaxValue(double[] results) {
        int index = 0;
        double max = results[0];

        for(int i = 1; i < results.length; i++) {
            if(results[i] > max) {
                max = results[i];
                index = i;
            }
        }

        return index;
    }

    static double sum(List<Integer> samples) {
        double sum = 0;

        for(Integer sample : samples) {
            sum += sample;
        }

        return sum;
    }

    static double standardDeviation(List<Integer> samples) {

        double mean = Util.sum(samples)/samples.size();
        double variance = 0;

        for(Integer sample : samples) {
            variance += Math.pow(mean-sample,2);
        }

        return Math.sqrt(variance/samples.size());
    }

    static double gaussianKernel(double x) {
        return 1/Math.sqrt(2*Math.PI)*Math.exp((-Math.pow(x,2))/2);
    }

    static double gaussianKernelProbability(int x, List<Integer> samples) {
        int size = samples.size();
        double std = standardDeviation(samples);
        double bandwidth = Math.pow((4*Math.pow(std,5)/(3*size)),0.2);

        double sum = 0;

        for(Integer sample : samples) {
            double kernelVariable = (sample-x)/bandwidth;
            sum += gaussianKernel(kernelVariable);
        }

        return sum/(size*bandwidth);
    }




}