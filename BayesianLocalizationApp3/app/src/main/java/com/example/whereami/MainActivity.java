package com.example.whereami;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.View.OnClickListener;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    private WifiManager wifiManager;
    WifiReceiver receiverWifi;
    SQLiteDatabase db;
    GridLayout leftGridLayout;
    List<String> networkBBSIDs;
    boolean sensingFinished;
    TextView senseLabel;
    Button buttonSense, buttonTraining;
    String[] cells;
    Bayes bayes;

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

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_whereami_icon_white);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        checkPermissions();

        System.out.println("STARTED");

        db = openOrCreateDatabase("database.db", MODE_PRIVATE, null);
        Database.createDatabases(db);

        networkBBSIDs = Database.getNetworkBSSIDs(db);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "Turning WiFi ON...", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        senseLabel = (TextView) findViewById(R.id.textview_label_sense);
        leftGridLayout = (GridLayout) findViewById(R.id.gridlayout_left_cells);
        cells = this.getResources().getStringArray(R.array.cell_array);

        resetSensing();

        buttonSense = (Button) findViewById(R.id.button_sense);
        buttonTraining = (Button) findViewById(R.id.button_training);
        buttonSense.setOnClickListener(this);
        buttonTraining.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_training: {
                startActivity(new Intent(this,TrainingActivity.class));
                break;
            }
            case R.id.button_sense: {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                }else{
                    sense();
                }
                break;
            }
        }
    }

    public void sense() {
        if(sensingFinished) {
            restartActivity(MainActivity.this);
        }else {

            wifiManager.startScan();
            List<ScanResult> scanResults = wifiManager.getScanResults();

            // Include scanned APs only if the BSSID is also in the list of processed networks
            // Use the absolute value for RSSI to be able to simply take the RSSI value as index
            List<AccessPoint> accessPoints = new ArrayList<>();
            for(ScanResult scanResult : scanResults) {
                if(networkBBSIDs.contains(scanResult.BSSID)) {
                    accessPoints.add(new AccessPoint(scanResult.BSSID,Math.abs(scanResult.level)));
                }
            }

            scanResults.clear();

            // Sort scanned APs on best (lowest) positive RSSI
            Collections.sort(accessPoints);

            for(AccessPoint ap : accessPoints) {
                // Requirement that there should be at least 30 samples of the corresponding AP
                if(Database.getSampleCount(db,ap.getBSSID()) < 30) {
                    continue;
                }

                Log.i("AP",ap.toString());
                Log.i("BEFORE",Arrays.toString(bayes.getPrior()));
                double[] probabilities = Util.getProbabilities(db,ap.getBSSID(),ap.getRSSI());
                Log.i("PROBS",Arrays.toString(probabilities));
                bayes.calculatePosterior(probabilities);
                Log.i("AFTER",Arrays.toString(bayes.getPosterior()));
            }

            scanResults.clear();

            // When user chooses to sense networks the posterior is used to see if a cell has a high localization probability
            if (bayes.isConverged()) {
                System.out.println("CONVERGED: "+Arrays.toString(bayes.getPosterior()));
                senseLabel.setText(getResources().getString(R.string.textview_sense_results));
                buttonSense.setText(getResources().getString(R.string.button_sense_reset));
                sensingFinished = true;
            } else {
                senseLabel.setText(getResources().getString(R.string.textview_sense_again));
            }

            // Show results on the view and set a countdown timer on the button before next sense can be executed
            createCellGrid(leftGridLayout, cells, bayes.getPosterior());
            countDownTimer.start();
        }
    }

    public static void restartActivity(Activity act) {
        act.recreate();
    }

    void resetSensing() {
        bayes = new Bayes();
        sensingFinished = false;
        senseLabel.setText(getResources().getString(R.string.textview_sense_to_see));
    }

    CountDownTimer countDownTimer = new CountDownTimer(10000, 1000) {
        public void onTick(long millis) {
            buttonSense.setEnabled(false);
            buttonSense.setText("SENSE IN " + millis / 1000);
        }

        public void onFinish() {
            buttonSense.setEnabled(true);
            if (sensingFinished) {
                buttonSense.setText("RESET SENSE RESULTS");
            } else {
                buttonSense.setText("SENSE");
            }
        }
    };

    public void createCellGrid(GridLayout gridLayout, String[] cellNames, double[] results) {
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
            textView.setHeight(60);
            textView.setText(cellNames[i] + " - " + String.format("%.4f", results[i]));
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

        networkBBSIDs = Database.getNetworkBSSIDs(db);
        leftGridLayout.removeAllViews();
        resetSensing();
        receiverWifi = new WifiReceiver(wifiManager);
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        getWifi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiverWifi);
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
