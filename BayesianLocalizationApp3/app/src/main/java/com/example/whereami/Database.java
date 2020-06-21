package com.example.whereami;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class Database {

    // Create Database
    static void createDatabases(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE IF NOT EXISTS networks (ID INTEGER PRIMARY KEY AUTOINCREMENT, BSSID VARCHAR(40), SSID VARCHAR(40), RSSI INTEGER, cellID INTEGER, type VARCHAR(10), scanID INTEGER)");
    }

    // Method that removes all saved samples and initiates re-creation of database
    static void resetSamples(SQLiteDatabase database) {
        try {
            database.execSQL("DROP TABLE IF EXISTS networks");
            Database.createDatabases(database);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Method that gets the maximum scanID from the networks table
    static int getMaximumScanID(SQLiteDatabase database) {
        int maxID = -1;

        try {
            Cursor cursor = database.rawQuery("SELECT MAX(scanID) from networks",null);

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

    // Method that finds the number of samples per cell in the database
    static int getTrainingCount(SQLiteDatabase database, int cellID) {
        int count = 0;

        try {
            Cursor cursor = database.rawQuery("SELECT * from networks WHERE cellID = ? AND type = 'training' GROUP BY scanID",new String[]{""+cellID});
            count = cursor.getCount();
            cursor.close();
        }catch (Exception e) {
            e.printStackTrace();
        }

        return count;
    }

    // Method that finds the number of samples per cell in the database
    static int getSampleCount(SQLiteDatabase database, String BSSID) {
        int count = 0;

        try {
            Cursor cursor = database.rawQuery("SELECT * from networks WHERE BSSID = ? GROUP BY scanID",new String[]{BSSID});
            count = cursor.getCount();
            cursor.close();
        }catch (Exception e) {
            e.printStackTrace();
        }

        return count;
    }

    // Method that determines all BSSIDs that are included in the probabilities table
    static List<String> getNetworkBSSIDs(SQLiteDatabase database) {
        List<String> networks = new ArrayList<>();

        Cursor cursor = database.rawQuery("SELECT DISTINCT BSSID from networks",null);

        while(cursor.moveToNext()) {
            networks.add(cursor.getString(cursor.getColumnIndex("BSSID")));
        }

        return networks;
    }

    static void importData(SQLiteDatabase database, Context context) throws IOException {
        database.execSQL("DROP TABLE IF EXISTS networks");
        Database.createDatabases(database);

        InputStream input = context.getResources().openRawResource(R.raw.data_to_phone);

        BufferedReader reader = new BufferedReader(new InputStreamReader(input, Charset.forName("UTF-8")));
        String line = reader.readLine();

        try {
            while ((line = reader.readLine()) != null) {

                String[] tokens = line.split(",");
                String BSSID = tokens[1];
                String SSID = tokens[2];
                String RSSI = tokens[3];
                String cellID = tokens[4];
                String type = tokens[5];
                String scanID = tokens[6];

                ContentValues row = new ContentValues();
                row.put("BSSID", BSSID);
                row.put("SSID", SSID);
                row.put("RSSI", RSSI);
                row.put("cellID", cellID);
                row.put("type", type);
                row.put("scanID", scanID);

                database.insert("networks", null, row);

            }
        } catch (IOException e1) {
            Log.e("Error reading csv", "Error" + line, e1);
            e1.printStackTrace();
        }

    }

    // Method that converts the samples in the SQLLite database to a CSV file and writes it to the phone storage
    static boolean exportData(SQLiteDatabase database) {

        Database.createDatabases(database);

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
}
