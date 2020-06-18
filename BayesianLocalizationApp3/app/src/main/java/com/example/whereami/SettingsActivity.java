package com.example.whereami;

import android.view.View.OnClickListener;
import android.Manifest;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;

public class SettingsActivity extends AppCompatActivity implements OnClickListener {

    SQLiteDatabase db;
    Button exportButton, importButton, resetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Obtain database endpoint
        db = openOrCreateDatabase("database.db", MODE_PRIVATE, null);

        // Initialize buttons and click listeners
        resetButton = (Button) findViewById(R.id.button_reset_training);
        exportButton = (Button) findViewById(R.id.button_export);
        importButton = (Button) findViewById(R.id.button_import);
        resetButton.setOnClickListener(this);
        exportButton.setOnClickListener(this);
        importButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_export: {
                exportSamples();
                break;
            }
            case R.id.button_import: {
                importSamples();
                break;
            }
            case R.id.button_reset_training: {
                Database.resetSamples(db);
                Toast.makeText(getApplicationContext(), R.string.confirm_reset_training_text, Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

    // Method that enables exporting samples
    public void exportSamples() {
        if (ContextCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            Toast.makeText(SettingsActivity.this, "EXTERNAL WRITE ACCESS PERMITTED", Toast.LENGTH_SHORT).show();
        }else {
            if(Database.exportData(db)) {
                Toast.makeText(SettingsActivity.this, "Database Exported!", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(SettingsActivity.this, "There are no samples to export!", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void importSamples() {
        Toast.makeText(SettingsActivity.this, "Database import started!", Toast.LENGTH_LONG).show();
        Toast toast = Toast.makeText(SettingsActivity.this, "Data imported!", Toast.LENGTH_LONG);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Database.importData(db, SettingsActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                toast.show();
            }
        });
        thread.start();
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }
}
