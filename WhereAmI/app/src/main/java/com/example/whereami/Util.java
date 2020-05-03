package com.example.whereami;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Util  extends AppCompatActivity {

    // Method that retrieves all trained samples from the sharedPreferences storage
    // Samples are first converted from json to objects
    static List<Sample> loadSamples(SQLiteDatabase database) {

        Util.createDatabases(database);

        List<Sample> samples = new ArrayList<>();

        Cursor sampleCursor = database.rawQuery("SELECT sampleID, cellID, activityID FROM samples", null);

        while(sampleCursor.moveToNext()) {
            int sampleID = sampleCursor.getInt(0);
            int cellID = sampleCursor.getInt(1);
            int activityID = sampleCursor.getInt(2);

            HashMap<String,Integer> networks = new HashMap<String,Integer>();

            String[] args = {""+sampleID};
            Cursor networksCursor = database.rawQuery("SELECT sampleID, BSSID, RSSI FROM networks WHERE sampleID = ?", args);

            Log.i("Sample "+sampleID,"cellID "+cellID+", activityID "+activityID+", "+networksCursor.getCount()+" networks");
            while(networksCursor.moveToNext()) {
                String BSSID = networksCursor.getString(1);
                int RSSI = networksCursor.getInt(2);

                networks.put(BSSID,RSSI);

                Log.i("Network "+BSSID," and RSSI "+RSSI);
            }

            networksCursor.close();

            samples.add(new Sample(sampleID,cellID,activityID,networks));
        }

        sampleCursor.close();

        Log.i(" ","----------------------------------------------");
        Log.i("Total loaded samples",""+samples.size());
        Log.i(" ","----------------------------------------------");

        return samples;
    }

    // Method that saves all trained samples to the sharedPreferences
    // Before saving the objects are converted into json
    static void saveSamples(List<Sample> samples, SQLiteDatabase database) {

        Util.createDatabases(database);

        for(Sample sample : samples) {

            String[] args = {""+sample.sampleID};
            Cursor sampleCursor = database.rawQuery("SELECT * FROM samples WHERE sampleID = ?", args);

            if(sampleCursor.getCount() == 0) {
                ContentValues sampleRow = new ContentValues();
                sampleRow.put("sampleID",sample.sampleID);
                sampleRow.put("cellID", sample.getCellID());
                sampleRow.put("activityID", sample.getActivityID());

                database.insert("samples", null, sampleRow);

                Log.i("Size "+sample.getNetworks().entrySet().size()," ");

                for (Map.Entry<String, Integer> entry : sample.getNetworks().entrySet()) {
                    ContentValues networkRow = new ContentValues();
                    networkRow.put("sampleID", sample.sampleID);
                    networkRow.put("BSSID", entry.getKey());
                    networkRow.put("RSSI", entry.getValue());

                    database.insert("networks", null, networkRow);
                }
            }
            sampleCursor.close();
        }
    }

    // Method that removes all saved samples
    static void resetSamples(SQLiteDatabase database) {
        try {
            database.execSQL("DROP TABLE samples");
            database.execSQL("DROP TABLE networks");

            Util.createDatabases(database);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void createDatabases(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE IF NOT EXISTS samples (sampleID INT PRIMARY KEY, cellID INT, activityID INT)");
        database.execSQL("CREATE TABLE IF NOT EXISTS networks (sampleID INT, BSSID VARCHAR(40), RSSI INT)");
    }

    // Method that retrieves the cell precision the user has selected in settings activity
    static int getPrecision(SharedPreferences sharedPreferences) {
        try {
            return sharedPreferences.getInt("precision",4);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Method that saves the cell precision the user has selected in the settings activity
    static void setPrecision(SharedPreferences sharedPreferences, int precision) {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("precision",precision);
            editor.commit();
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Method that retrieves the method of calculation for localization the user has selected in settings activity
    static String getMethod(SharedPreferences sharedPreferences) {
        try {
            return sharedPreferences.getString("method", "KNN");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Method that saves the methof of calculation for localization the user has selected in the settings activity
    static void setMethod(SharedPreferences sharedPreferences, String method) {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("method",method);
            editor.commit();
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Method that performs a network scan
    static HashMap<String,Integer> findNetworks(WifiManager wifiManager) {
        HashMap<String, Integer> networks = new HashMap<String, Integer>();

        wifiManager.startScan();

        // Sense networks and add to hashmap
        for (ScanResult scan : wifiManager.getScanResults()) {
            networks.put(scan.BSSID, scan.level);
        }

        return networks;
    }

    // Method that determines the KNN of the sensed networks and returns the predicted cell back
    public static String KNN(HashMap<String, Integer> sensedNetworks, List<Sample> allSamples, SharedPreferences settingsSharedPreferences, String[] cells) {

        List<Result> results = new ArrayList<>();

        int cellPrecision = Util.getPrecision(settingsSharedPreferences);
        double[] cellCounts = new double[cellPrecision];
        int k = Util.calculateK(allSamples.size());

        // For each trained sample we want to compute the distance to the sensed sample
        for(Sample sample : allSamples) {
            double distance = 0;

            HashMap<String,Integer> networks = sample.getNetworks();

            // Determine the networks in a trained sample against the sensed sample
            for(Map.Entry<String,Integer> entry : networks.entrySet()) {
                String BSSID = entry.getKey();
                Integer RSSI = entry.getValue();

                // If the network is in both samples, the distance can easily be computed as the euclidean distance
                // If the network is not scanned in the sensed sample, we use a RSSI of -100 (meaning it is as weak as possible)
                if(sensedNetworks.containsKey(BSSID)) {
                    distance += Math.pow(Math.abs(RSSI-sensedNetworks.get(BSSID)),2);
                }else{
                    distance += Math.pow(Math.abs(RSSI+100),2);
                }
            }

            // Determine the networks that are scanned (only) in the sensed sample but not in the trained sample
            // We also use a RSSI value of -100 for the not scanned network
            for(Map.Entry<String,Integer> entry : sensedNetworks.entrySet()) {
                String BSSID = entry.getKey();
                Integer RSSI = entry.getValue();

                if(!networks.containsKey(BSSID)) {
                    distance += Math.pow(Math.abs(RSSI+100),2);
                }
            }

            results.add(new Result(sample, Math.sqrt(distance)));
        }

        // Sort the distances in increasing order
        Collections.sort(results, (a, b) -> a.getDistance() < b.getDistance() ? -1 : a.getDistance() == b.getDistance() ? 0 : 1);

        // Take the k nearest networks (with lowest distance)
        List<Result> kresults = results.subList(0,k);

        // Count the number of neighbors in each belonging in each cell
        for(Result res : kresults) {
            Log.i(" ",res.toString()+"");
            cellCounts[res.getCellID()]++;
        }

        // Method to give more weight to the nearest neighbor and less weight to the more distant nearest neighbors (for comparison)
        // Compute individual weight for each result
        double totalWeight = 0;
        for(Result res : kresults) {
            double weight = 1/res.getDistance();
            res.setWeight(weight);
            totalWeight += weight;
        }

        // Normalize the weight for each result
        for(Result res : kresults) {
            res.normalizeWeight(totalWeight);
        }

        // Compute how much weight contributes to each cell prediction
        double[] weightPerCell = new double[cellPrecision];

        for(Result res : kresults) {
            weightPerCell[res.getCellID()] += res.getWeight();
        }

        // Find the index of the cell with the maximum value
        List<Integer> largestIndicesCount = Util.findMaxIndicesInArray(cellCounts);
        int largestIndexCount = largestIndicesCount.get(0);
        int largestIndexWeights = Util.findMaxInArray(weightPerCell);

        // Look if two cells have an equal count, and include weight per cell as tiebreaker
        if(largestIndicesCount.size() > 1) {
            double maxValue = weightPerCell[largestIndexCount];

            for(int index : largestIndicesCount) {
                if(weightPerCell[index] > maxValue) {
                    maxValue = weightPerCell[index];
                    largestIndexCount = index;
                }
            }
        }

        // Display results of cell count and weight per cell, based on majority
        Log.i("k=",k+"");
        Log.i("Cell Count Result: "+cells[largestIndexCount]+" ",Arrays.toString(cellCounts));
        Log.i("Weight Result: "+cells[largestIndexWeights]+", ", Arrays.toString(weightPerCell));

        return cells[largestIndexCount];
    }

    public static String activity(List<Sample> allSamples, SharedPreferences settingsSharedPreferences, String[] activities) {

        Random r = new Random();
        return activities[r.nextInt((2 - 0) + 1) + 0];
    }

    // Method that calculates the k-value based on the size of the trained samples
    // It is researched and proven that the optimal value of 7 delivers best results.
    // If there aren't enough samples the value of k will be equal to the square root of the sample size.
    static int calculateK(int size) {
        int k = (int) Math.floor(Math.sqrt(size));

        if(k > 7) {
            return 7;
        }

        return k;
    }

    // Method to find the index/indices of the highest value in array
    static List<Integer> findMaxIndicesInArray(double[] arr) {
        List<Integer> indices = new ArrayList<>();
        double maxValue = 0;

        for(int i = 0; i < arr.length; i++) {
            if(arr[i] > maxValue) {
                maxValue = arr[i];
                indices = new ArrayList<>();
                indices.add(i);
            }else if(arr[i] == maxValue) {
                indices.add(i);
            }
        }
        return indices;
    }

    // Method that finds the index of the maximum value in array
    static int findMaxInArray(double[] arr) {
        double largestValue = arr[0];
        int largestIndex = 0;
        for (int i = 1; i < arr.length; i++) {
            if ( arr[i] >= largestValue ) {
                largestValue = arr[i];
                largestIndex = i;
            }
        }
        return largestIndex;
    }

    // Method that calculates the average of elements in array, excluding zero values
    static int calculateAverageInArray(int[] array, int rounds) {
        int nonZeroElements = 0;
        int sum = 0;
        for(int i = 0; i < rounds; i++) {
            if(array[i] < 0) {
                nonZeroElements++;
                sum += array[i];
            }
        }

        return (int) Math.floor(sum/nonZeroElements);
    }

    static List<Network> processNetworks(List<ScanResult> scanResults) {
        List<Network> networks = new ArrayList<>();

        for(ScanResult scan : scanResults) {
            networks.add(new Network(scan.BSSID,scan.level));
        }

        return networks;
    }

    static boolean networksEqual(List<ScanResult> networkSetOne, List<ScanResult> networkSetTwo) {

        boolean equal = true;

        if(networkSetOne.size() != networkSetTwo.size()) {
            return false;
        }

        for(int i = 0; i < networkSetOne.size(); i++) {
            if(!((networkSetOne.get(i).BSSID.equals(networkSetTwo.get(i).BSSID) && (networkSetOne.get(i).level == networkSetTwo.get(i).level)))) {
                return false;
            }
        }

        return true;
    }
}