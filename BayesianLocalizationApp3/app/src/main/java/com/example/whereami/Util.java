package com.example.whereami;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import java.util.ArrayList;
import java.util.List;

public class Util {

    // Method that performs a network scan and inserts them into the database
    static void train(WifiManager wifiManager, SQLiteDatabase database, int cellID, int scanID) {

        wifiManager.startScan();
        List<ScanResult> scanResults = wifiManager.getScanResults();

        // Temporary AP names that should not be included in the result
        String[] filter_names = new String[]{"AndroidAP","iPhone", "Nokia", "OnePlus", "HUAWEI", "LG"};

        // Sense networks and add to database
        scanResultLoop: for (ScanResult scan : scanResults) {

            // Do not process temporary APs, based on general AP names of Android, iOS, and other vendors
            for(String name : filter_names) {
                if (scan.SSID.contains(name)) {
                    break scanResultLoop;
                }
            }

            ContentValues networkRow = new ContentValues();
            networkRow.put("BSSID", scan.BSSID);
            networkRow.put("SSID", scan.SSID);
            networkRow.put("RSSI", scan.level);
            networkRow.put("cellID", cellID);
            networkRow.put("scanID", scanID);
            networkRow.put("type", "training");

            database.insert("networks", null, networkRow);
        }
    }

    // Method that calculates the standard deviation of a list of samples
    static double standardDeviation(List<Integer> samples) {

        double mean = Util.sum(samples)/samples.size();
        double variance = 0;

        for(Integer sample : samples) {
            variance += Math.pow(mean-sample,2);
        }

        return Math.sqrt(variance/samples.size());
    }

    // Method that calculates a Gaussian Kernel at a given value x
    static double gaussianKernel(double x) {
        return 1/Math.sqrt(2*Math.PI)*Math.exp((-Math.pow(x,2))/2);
    }

    // Method that calculates the Gaussian Kernel Density Estimator, with std as the standard deviation of the sample values,
    // bandwidth is estimated to be approximately (4(std)^5/(3n))^(1/5). More info: http://faculty.washington.edu/yenchic/17Sp_403/Lec7-density.pdf
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

    // Method that obtains probabilities for a given AP and RSSI
    static double[] getProbabilities(SQLiteDatabase database, String BSSID, int RSSI) {
        double[] probs = new double[8];

        double norm = 0;

        for(int cellID = 0; cellID < 8; cellID++) {
            try{
                Cursor cellCursor = database.rawQuery("SELECT * FROM networks WHERE BSSID = ? AND cellID = ?", new String[]{BSSID,""+cellID});
                cellCursor.moveToFirst();

                List<Integer> samples = new ArrayList<>();

                while(cellCursor.moveToNext()) {
                    samples.add(Math.abs(cellCursor.getInt(cellCursor.getColumnIndex("RSSI"))));
                }

                probs[cellID] = Util.gaussianKernelProbability(RSSI, samples);

                if(Double.isNaN(probs[cellID])){
                    probs[cellID] = Double.MIN_VALUE;
                }

                norm += probs[cellID];

                cellCursor.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        for(int i = 0; i < 8; i++) {
            probs[i] = probs[i]/norm;
        }

        return probs;
    }

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

    // Method that calculates the sum of list of integers
    static double sum(List<Integer> samples) {
        double sum = 0;

        for(Integer sample : samples) {
            sum += sample;
        }

        return sum;
    }
}