package com.example.whereami;

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

public class SettingsActivity extends AppCompatActivity {

    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        db = openOrCreateDatabase("database.db", MODE_PRIVATE, null);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button resetButton = (Button) findViewById(R.id.button_reset_training);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Util.resetSamples(db);

                Toast.makeText(getApplicationContext(), R.string.confirm_reset_training_text, Toast.LENGTH_SHORT).show();
            }
        });

        Button exportButton = (Button) findViewById(R.id.button_export);
        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (ContextCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                    Toast.makeText(SettingsActivity.this, "EXTERNAL WRITE ACCESS PERMITTED", Toast.LENGTH_SHORT).show();
                }else {
                    boolean export = Util.exportSamples(db);

                    if (export) {
                        Toast.makeText(SettingsActivity.this, "Database Exported!", Toast.LENGTH_LONG).show();
                    }else{
                        Toast.makeText(SettingsActivity.this, "There are no samples to export!", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }
}
