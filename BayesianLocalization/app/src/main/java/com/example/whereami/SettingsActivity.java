package com.example.whereami;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    SharedPreferences settingsSharedPreferences;
    int currentPrecision;
    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        db = openOrCreateDatabase("database.db", MODE_PRIVATE, null);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        settingsSharedPreferences = getApplicationContext().getSharedPreferences("SETTINGS", 0);

        Button resetButton = (Button) findViewById(R.id.button_reset_training);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Util.resetSamples(db);

                Toast.makeText(getApplicationContext(), R.string.confirm_reset_training_text, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

}
