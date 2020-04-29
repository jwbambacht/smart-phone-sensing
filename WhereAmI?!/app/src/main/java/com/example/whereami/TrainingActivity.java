package com.example.whereami;

import android.Manifest;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrainingActivity extends AppCompatActivity {

    List<Sample> currentSamples;
    private WifiManager wifiManager;
    private final int MY_PERMISSIONS_ACCESS_COARSE_LOCATION = 1;
    WifiReceiver receiverWifi;
    SharedPreferences allSamplesSharedPreferences,settingsSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        allSamplesSharedPreferences = getApplicationContext().getSharedPreferences("ALL_SAMPLES",0);
        this.currentSamples = Util.loadSamples(allSamplesSharedPreferences);

        settingsSharedPreferences = getApplicationContext().getSharedPreferences("SETTINGS", 0);
        int precision = Util.getPrecision(settingsSharedPreferences);

        final RadioGroup radioGroupCells = (RadioGroup) findViewById(R.id.radiogroup_cells);
        fillRadioGroup(this, radioGroupCells, R.array.cell_array,precision);

        final RadioGroup radioGroupActivity = (RadioGroup) findViewById(R.id.radiogroup_activity);
        fillRadioGroup(this, radioGroupActivity, R.array.activity_array, Integer.MAX_VALUE);

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
        Log.i("Selected Cell", cellID + "");
        Log.i("Select Activity", activityID + "");

        HashMap<String,Integer> networks = new HashMap<String,Integer>();
        HashMap<String,int[]> tempNetworks = new HashMap<String,int[]>();

        if (ActivityCompat.checkSelfPermission(TrainingActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(TrainingActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_ACCESS_COARSE_LOCATION);
        } else {
            wifiManager.startScan();
//            Toast.makeText(TrainingActivity.this, "Searching for networks...", Toast.LENGTH_SHORT).show();

            int rounds = 5;

            for(int i = 0; i < rounds; i++) {
                List<ScanResult> scanResults = wifiManager.getScanResults();

                Log.i("ScanResults",scanResults.size()+"");

                for(ScanResult scan : scanResults) {
                    String BSSID = scan.BSSID;

                    int[] rssi;

                    if(tempNetworks.containsKey(BSSID)) {
                        rssi = tempNetworks.get(BSSID);
                        tempNetworks.remove(BSSID);
                    }else{
                        rssi = new int[rounds];
                    }

                    rssi[i] = scan.level;

                    tempNetworks.put(BSSID,rssi);
                }
            }

            for(Map.Entry<String,int[]> entry : tempNetworks.entrySet()) {
                String BSSID = entry.getKey();
                int[] arrayRSSI = entry.getValue();

                int nonZeroElements = 0;
                int sum = 0;
                for(int i = 0; i < rounds; i++) {
                    if(arrayRSSI[i] < 0) {
                        nonZeroElements++;
                        sum += arrayRSSI[i];
                    }
                }

                networks.put(BSSID,(int) Math.floor(sum/nonZeroElements));

            }


//            for(ScanResult scan : wifiManager.getScanResults()) {
//                networks.put(scan.BSSID,scan.level);
//            }
//            Toast.makeText(TrainingActivity.this, "Found " + networks.size() + " network(s)", Toast.LENGTH_SHORT).show();

        }

        Sample newSample = new Sample(cellID, activityID, networks);
        this.currentSamples.add(newSample);

        Util.saveSamples(allSamplesSharedPreferences, this.currentSamples);

        Toast.makeText(getApplicationContext(), R.string.confirm_add_training_text, Toast.LENGTH_SHORT).show();
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
