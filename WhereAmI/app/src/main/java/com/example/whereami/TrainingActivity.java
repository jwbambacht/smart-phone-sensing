package com.example.whereami;

import android.Manifest;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.List;

public class TrainingActivity extends AppCompatActivity {

    // Receiver
    private WifiManager wifiManager;
    private final int MY_PERMISSIONS_ACCESS_COARSE_LOCATION = 1;
    WifiReceiver receiverWifi;

    //Accelerometer
    private SensorManager mSensorManager;
    private AccelerometerListener accelerometer;

    // Storage helpers
    SharedPreferences settingsSharedPreferences;
    SQLiteDatabase db;

    // UI Elements
    TableLayout tableSamples;

    // Variables
    List<Sample> currentSamples;
    int precision;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = openOrCreateDatabase("database.db", MODE_PRIVATE, null);
        this.currentSamples = Util.loadSamples(db);

        settingsSharedPreferences = getApplicationContext().getSharedPreferences("SETTINGS", 0);
        precision = Util.getPrecision(settingsSharedPreferences);

        final RadioGroup radioGroupCells = (RadioGroup) findViewById(R.id.radiogroup_cells);
        fillRadioGroup(this, radioGroupCells, R.array.cell_array,precision);

        final RadioGroup radioGroupActivity = (RadioGroup) findViewById(R.id.radiogroup_activity);
        fillRadioGroup(this, radioGroupActivity, R.array.activity_array, Integer.MAX_VALUE);

        this.tableSamples = (TableLayout) findViewById(R.id.table_samples);
        this.tableSamples.setStretchAllColumns(true);
        this.loadTableData(this.tableSamples);

        SensorManager mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = new AccelerometerListener(mSensorManager);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "Turning WiFi ON...", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        Button trainButton = (Button) findViewById(R.id.button_add_training);
        trainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int radioButtonCellID = radioGroupCells.getCheckedRadioButtonId();
                View radioButtonCell = radioGroupCells.findViewById(radioButtonCellID);
                int idxCell = radioGroupCells.indexOfChild(radioButtonCell);

                int radioButtonActivityID = radioGroupActivity.getCheckedRadioButtonId();
                View radioButtonActivity = radioGroupActivity.findViewById(radioButtonActivityID);
                int idxActivity = radioGroupActivity.indexOfChild(radioButtonActivity);

                addToTraining(idxCell, idxActivity);
            }
        });
    }

    // Method that loads the data of the table, removes current data and inserts new data
    public void loadTableData(TableLayout table) {

        table.removeAllViews();

        int[] cellCount = new int[precision];
        int[] activityCount = new int[3];

        for(Sample sample : this.currentSamples) {
            cellCount[sample.getCellID()]++;
            activityCount[sample.getActivityID()]++;
        }

        String[] allCells = getResources().getStringArray(R.array.cell_array);
        String[] allActivity = getResources().getStringArray(R.array.activity_array);

        for(int i = 0; i < precision+4; i++) {
            String nameCell,sampleCell;
            int fontSize = 14;
            if(i < precision) {
                nameCell = allCells[i];
                sampleCell = cellCount[i] + " samples";
            }else if(i >= precision && i < precision+3) {
                nameCell = allActivity[i-precision];
                sampleCell = activityCount[i-precision] + " samples";
            }else{
                nameCell = getResources().getString(R.string.textview_label_total_samples);
                sampleCell = this.currentSamples.size()+" samples";
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

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    private static void fillRadioGroup(Context context, RadioGroup radioGroup, int stringArrayId, int precision){
        int index = 0;
        for (String s : context.getResources().getStringArray(stringArrayId)){
            if(index == precision) {
                break;
            }
            index++;

            RadioButton radioButton = new RadioButton(context);
            radioButton.setText(s);
            radioButton.setHighlightColor(Color.WHITE);
            radioButton.setTextColor(Color.WHITE);
            ColorStateList colorStateList = new ColorStateList(
                    new int[][]{
                            new int[]{-android.R.attr.state_checked},
                            new int[]{android.R.attr.state_checked}
                    },
                    new int[]{
                            Color.GRAY,
                            Color.WHITE
                    }
            );
            radioButton.setButtonTintList(colorStateList);
            radioGroup.addView(radioButton);
        }

        if(radioGroup.getChildCount() > 0)
            radioGroup.check(radioGroup.getChildAt(0).getId());
    }

    public void addToTraining(int cellID, int activityID) {

        HashMap<String, Integer> networks = new HashMap<String, Integer>();

        if (ActivityCompat.checkSelfPermission(TrainingActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(TrainingActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_ACCESS_COARSE_LOCATION);

            Toast.makeText(getApplicationContext(), R.string.error_add_training_text, Toast.LENGTH_SHORT).show();
        } else {
            networks = Util.findNetworks(wifiManager);

            float[] activityFeature = accelerometer.getFeature(AccelerometerListener.MIN_MAX);
            /*Log.i("activityFeature", String.format("[%.2f,%.2f,%.2f]",
                activityFeature[0], activityFeature[1], activityFeature[2]));*/
            Sample newSample = new Sample(this.currentSamples.size(), cellID, activityID, networks, activityFeature);
            this.currentSamples.add(newSample);

            Util.saveSamples(this.currentSamples,this.db);

            Toast.makeText(getApplicationContext(), R.string.confirm_add_training_text, Toast.LENGTH_SHORT).show();

            this.loadTableData(this.tableSamples);
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
                ActivityCompat.requestPermissions(TrainingActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_ACCESS_COARSE_LOCATION);
            } else {
                wifiManager.startScan();
            }
        } else {
            Toast.makeText(TrainingActivity.this, "scanning", Toast.LENGTH_SHORT).show();
            wifiManager.startScan();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiverWifi);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_ACCESS_COARSE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(TrainingActivity.this, "permission granted", Toast.LENGTH_SHORT).show();
                    wifiManager.startScan();
                } else {

                    Toast.makeText(TrainingActivity.this, "permission not granted", Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
        }
    }
}
