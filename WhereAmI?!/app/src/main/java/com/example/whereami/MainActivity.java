package com.example.whereami;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private final int MY_PERMISSIONS_ACCESS_COARSE_LOCATION = 1;
    WifiReceiver receiverWifi;
    SharedPreferences settingsSharedPreferences, allSamplesSharedPreferences;
    TextView textViewCell, textViewActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewCell = findViewById(R.id.textview_cell_result);
        textViewActivity = findViewById(R.id.textview_activity_result);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "Turning WiFi ON...", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        allSamplesSharedPreferences = getApplicationContext().getSharedPreferences("ALL_SAMPLES", 0);
        settingsSharedPreferences = getApplicationContext().getSharedPreferences("SETTINGS", 0);

        Button training = (Button) findViewById(R.id.button_training);
        training.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(view.getContext(),TrainingActivity.class));
            }
        });

        Button sense = (Button) findViewById(R.id.button_sense);
        sense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(Util.loadSamples(allSamplesSharedPreferences).size() < 8) {
                    Toast.makeText(MainActivity.this, R.string.error_not_enough_samples, Toast.LENGTH_SHORT).show();
                    return;
                }

                HashMap<String, Integer> networks = new HashMap<String, Integer>();
                HashMap<String, int[]> tempNetworks = new HashMap<String,int[]>();

                String cellResult = null;
                String activityResult = null;

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_ACCESS_COARSE_LOCATION);
                } else {

                    Toast.makeText(MainActivity.this,"Sensing...", Toast.LENGTH_SHORT).show();
//                    Toast.makeText(MainActivity.this, "Searching for networks...", Toast.LENGTH_SHORT).show();
//                    wifiManager.getScanResults();
//
//                    for(ScanResult scan : wifiManager.getScanResults()) {
//                        networks.put(scan.BSSID,scan.level);
//                    }
//                    Toast.makeText(MainActivity.this, "Found " + networks.size() + " network(s)", Toast.LENGTH_SHORT).show();

                    int rounds = 5;

                    for(int i = 0; i < rounds; i++) {
                        wifiManager.startScan();
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

                        try {
                            Thread.sleep(200);
                        } catch(InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }

                    }

                    cellResult = KNN(networks);

                }

                if(cellResult != null) {
                    textViewCell.setText(cellResult);
                }
                if(activityResult != null) {
                    textViewActivity.setText(activityResult);
                }

            }
        });
    }

    public String KNN(HashMap<String, Integer> sensedNetworks) {

        List<Sample> allSamples = Util.loadSamples(allSamplesSharedPreferences);

        List<Result> results = new ArrayList<>();

        String cell = null;

        int k = (int) Math.floor(Math.sqrt(allSamples.size()));

        if(k % 2 == 0) {
            k++;
        }

        for(Sample sample : allSamples) {
            double distance = 0;

            HashMap<String,Integer> networks = sample.getNetworks();

            for(Map.Entry<String,Integer> entry : networks.entrySet()) {
                String BSSID = entry.getKey();
                Integer RSSI = entry.getValue();

                if(sensedNetworks.containsKey(BSSID)) {
                    distance += Math.pow(Math.abs(RSSI-sensedNetworks.get(BSSID)),2);
                }else{
                    distance += Math.pow(RSSI,2);
                }
            }

            for(Map.Entry<String,Integer> entry : sensedNetworks.entrySet()) {
                String BSSID = entry.getKey();
                Integer RSSI = entry.getValue();

                if(!networks.containsKey(BSSID)) {
                    distance += Math.pow(RSSI,2);
                }
            }

            results.add(new Result(sample, Math.sqrt(distance)));

        }

        Collections.sort(results, (a, b) -> a.getDistance() < b.getDistance() ? -1 : a.getDistance() == b.getDistance() ? 0 : 1);

        for(Result res : results) {
            Log.i(" ",res.toString()+"");
        }

        results = results.subList(0,k);

        int precision = Util.getPrecision(settingsSharedPreferences);
        int[] cellCounts = new int[precision];

        Log.i("k=",k+"");

        for(Result res : results) {
            Log.i(" ",res.toString()+"");
            cellCounts[res.getCellID()]++;
        }

        Log.i("Result", Arrays.toString(cellCounts));
        int largestValue = cellCounts[0];
        int largestIndex = 0;
        for (int i = 1; i < cellCounts.length; i++) {
            if ( cellCounts[i] >= largestValue ) {
                largestValue = cellCounts[i];
                largestIndex = i;
            }
        }

        String[] cells = this.getResources().getStringArray(R.array.cell_array);

        return cells[largestIndex];
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                this.startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        textViewCell.setText(R.string.textview_sense_to_see);
        textViewActivity.setText(R.string.textview_sense_to_see);
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
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "location turned off", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_ACCESS_COARSE_LOCATION);
            } else {
                wifiManager.startScan();
            }
        } else {
            Toast.makeText(MainActivity.this, "scanning", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(MainActivity.this, "permission granted", Toast.LENGTH_SHORT).show();
                    wifiManager.startScan();
                } else {

                    Toast.makeText(MainActivity.this, "permission not granted", Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
        }
    }
}
