package com.example.whereami;

import android.Manifest;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class TrainingActivity extends AppCompatActivity {

    // Receiver
    private WifiManager wifiManager;
    WifiReceiver receiverWifi;

    // Storage helpers
    SQLiteDatabase db;

    // UI Elements
    TableLayout tableSamples;
    Button trainButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = openOrCreateDatabase("database.db", MODE_PRIVATE, null);

        this.tableSamples = (TableLayout) findViewById(R.id.table_samples);
        this.tableSamples.setStretchAllColumns(true);
        this.loadTableData(this.tableSamples);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "Turning WiFi ON...", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        Spinner cellSpinner = (Spinner) findViewById(R.id.spinner_cell);
        ArrayAdapter<String> cellAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.cell_array));
        cellSpinner.setAdapter(cellAdapter);

        trainButton = (Button) findViewById(R.id.button_add_training);
        trainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int idxCell = cellSpinner.getSelectedItemPosition();

                try {
                    addToTraining(idxCell);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.loadTableData(this.tableSamples);
    }

    // Method that loads the data of the table, removes current data and inserts new data
    public void loadTableData(TableLayout table) {

        table.removeAllViews();

        int[] cellCount = new int[8];
        int totalCount = 0;

        for(int i = 0; i < cellCount.length; i++) {
            int count = Util.getTrainingCount(db,i);
            cellCount[i] = count;
            totalCount += count;
        }

        String[] allCells = getResources().getStringArray(R.array.cell_array);

        for(int i = 0; i < 9; i++) {
            String nameCell,sampleCell;
            int fontSize = 14;
            if(i < 8) {
                nameCell = allCells[i];
                sampleCell = cellCount[i] + " scans";
            }else{
                nameCell = getResources().getString(R.string.textview_label_total_samples);
                sampleCell = totalCount+" scans";
                fontSize = 16;
            }

            TableRow tableRow = new TableRow(this);
            tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));

            TextView textViewLeft = new TextView(this);
            textViewLeft.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            textViewLeft.setText(nameCell);
            textViewLeft.setTextColor(Color.WHITE);
            textViewLeft.setTextSize(TypedValue.COMPLEX_UNIT_SP,fontSize);
            tableRow.addView(textViewLeft);
            textViewLeft.setHeight(50);
            textViewLeft.setTypeface(Typeface.DEFAULT_BOLD);

            TextView textViewRight = new TextView(this);
            textViewRight.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            textViewRight.setText(sampleCell);
            textViewRight.setTextColor(Color.WHITE);
            textViewRight.setTextSize(TypedValue.COMPLEX_UNIT_SP,fontSize);

            tableRow.addView(textViewRight);
            table.addView(tableRow, new TableLayout.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
        }
    }

    public void addToTraining(int cellID) throws InterruptedException {

        if (ActivityCompat.checkSelfPermission(TrainingActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(TrainingActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

            Toast.makeText(getApplicationContext(), R.string.error_add_training_text, Toast.LENGTH_SHORT).show();
        } else {

            Toast.makeText(this,"SAMPLING STARTED FOR THIS CELL", Toast.LENGTH_SHORT).show();

            Toast toast = Toast.makeText(getApplicationContext(),"SAMPLING FINISHED FOR THIS CELL", Toast.LENGTH_SHORT);

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    int nScans = 30;
                    int timeDifference = 10;
                    int delay = timeDifference*1000;
                    int scanned = 0;
                    int scanID = Util.getMaximumScanID(db);

                    try {
                        while(scanned < nScans) {
                            scanID += 1;
                            Thread.sleep(delay);
                            Log.i("Scan ",""+scanned);
                            Util.findNetworks(wifiManager,db,cellID, false, scanID);
                            scanned++;
                        }
                        toast.show();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        receiverWifi = new WifiReceiver(wifiManager);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(receiverWifi, intentFilter);
        getWifi();
    }

    private void getWifi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(TrainingActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(TrainingActivity.this, "location turned off", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(TrainingActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            } else {
                wifiManager.startScan();
            }
        } else {
            Toast.makeText(TrainingActivity.this, "scanning", Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiverWifi);
    }
}
