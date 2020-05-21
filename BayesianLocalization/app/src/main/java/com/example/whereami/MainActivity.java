package com.example.whereami;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Result;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    WifiReceiver receiverWifi;
    SQLiteDatabase db;
    GridLayout leftGridLayout;
    double[] cellBeliefs;
    HashMap<String,Network> networks;

    String[] permissions = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_whereami_icon_white);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = openOrCreateDatabase("database.db", MODE_PRIVATE, null);
        Util.createDatabases(db);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "Turning WiFi ON...", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        networks = Util.readData(this);

        Button training = (Button) findViewById(R.id.button_training);
        training.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(view.getContext(),TrainingActivity.class));
            }
        });

        String[] cells = this.getResources().getStringArray(R.array.cell_array);
        cellBeliefs = new double[] { 0.125, 0.125, 0.125, 0.125, 0.125, 0.125, 0.125, 0.125};

        leftGridLayout = (GridLayout) findViewById(R.id.gridlayout_left_cells);

        Button sense = (Button) findViewById(R.id.button_sense);
        sense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                } else {

                    Toast.makeText(MainActivity.this,"Sensing...", Toast.LENGTH_SHORT).show();

                    List<ResultScan> resultScans = new ArrayList<>();
                    wifiManager.startScan();

                    // Leave out networks that are not in the training results (not captured or cleaned out for some reason)
                    for(ScanResult scanResult : wifiManager.getScanResults()) {
                        if(networks.containsKey(scanResult.BSSID)) {
                            resultScans.add(new ResultScan(scanResult.BSSID,Math.abs(scanResult.level)));
                        }
                    }

                    Collections.sort(resultScans);

                    for(ResultScan res : resultScans) {
                        Log.i(res.getBSSID(),res.getRSSI()+"");
                    }

                    BigDecimal[] prior = new BigDecimal[8];
                    Arrays.fill(prior, new BigDecimal(0.125));

                    BigDecimal[] posterior = new BigDecimal[8];

                    for(int i = 0; i < 3; i++) {
                        Log.i("End Vector "+ i,"");
                        for(BigDecimal pri : prior) {
                            Log.i("",""+pri.doubleValue());
                        }

                        ResultScan result = resultScans.get(i);
                        String BSSID = result.getBSSID();
                        int RSSI = result.getRSSI();
                        BigDecimal norm_sum = new BigDecimal(0);

                        BigDecimal[] probs = networks.get(BSSID).getProbabilitiesForRSSI(RSSI);

                        for(int j = 0; j < 8; j++) {
                            Log.i("Prob "+j,probs[j]+"");
                            posterior[j] = prior[j].multiply(probs[j]);
                            norm_sum = norm_sum.add(posterior[j]);
                        }

                        // Normalize posterior
                        for(int j = 0; j < 8; j++) {
                            posterior[j] = posterior[j].divide(norm_sum, 300, RoundingMode.CEILING);
                        }

                        Log.i("End Vector "+ i,"");
                        for(BigDecimal post : posterior) {
                            Log.i("",""+post.doubleValue());
                        }

                        prior = posterior;

                    }


//                    int scanID = Util.getMaximumScanID(db);


                    // Apply the KNN algorithm in order to obtain the cell prediction
//                    cellBeliefs = Util.BayesianLocalization(networks,cellBeliefs);

//                    createCellGrid(leftGridLayout, cells, cellBeliefs);
                }
            }
        });
    }

    public void createCellGrid(GridLayout gridLayout, String[] cellNames, float[] results) {
        gridLayout.removeAllViews();

        int idx = Util.findMaxValue(results);

        for(int i = 0; i < 8; i++) {
            TextView textView = new TextView(this);
            textView.setTextColor(0xFFF8F9FA);

            if(i == idx) {
                textView.setTextColor(0xFF343A40);
                textView.setBackgroundResource(R.drawable.cell_open);
            }else{
                textView.setBackgroundResource(R.drawable.cell_closed);
            }
            textView.setGravity(Gravity.CENTER_VERTICAL);
            textView.setHeight(70);
            textView.setText(cellNames[i]);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

            gridLayout.addView(textView);

            GridLayout.LayoutParams param = new GridLayout.LayoutParams();
            param.height = GridLayout.LayoutParams.WRAP_CONTENT;
            param.width = GridLayout.LayoutParams.MATCH_PARENT;
            param.setGravity(Gravity.CENTER);
            param.bottomMargin = 20;
            textView.setLayoutParams(param);
        }
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
        leftGridLayout.removeAllViews();
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
                Toast.makeText(MainActivity.this, "Location turned off", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            } else {
                wifiManager.startScan();
            }
        } else {
            Toast.makeText(MainActivity.this, "Scanning networks", Toast.LENGTH_SHORT).show();
            wifiManager.startScan();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiverWifi);
    }

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Permissions granted", Toast.LENGTH_SHORT).show();
            }
            return;
        }
    }
}
