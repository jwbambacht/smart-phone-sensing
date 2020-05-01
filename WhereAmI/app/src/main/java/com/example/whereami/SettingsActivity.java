package com.example.whereami;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    SharedPreferences settingsSharedPreferences;
    String currentMethod;
    int currentPrecision;
    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        db = openOrCreateDatabase("database.db", MODE_PRIVATE, null);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        settingsSharedPreferences = getApplicationContext().getSharedPreferences("SETTINGS", 0);

        String currentMethod = Util.getMethod(settingsSharedPreferences);
        currentPrecision = Util.getPrecision(settingsSharedPreferences);

        Spinner methodSpinner = (Spinner) findViewById(R.id.spinner_method);
        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.method_array));
        methodSpinner.setAdapter(methodAdapter);
        methodSpinner.setSelection(methodAdapter.getPosition(currentMethod));

        Spinner precisionSpinner = (Spinner) findViewById(R.id.spinner_precision);
        ArrayAdapter<String> precisionAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.precision_array));
        precisionSpinner.setAdapter(precisionAdapter);
        precisionSpinner.setSelection(precisionAdapter.getPosition(currentPrecision+""));

        Button saveButton = (Button) findViewById(R.id.button_save_settings);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String method = methodSpinner.getSelectedItem().toString();
                int precision = Integer.parseInt(precisionSpinner.getSelectedItem().toString());

                Util.setPrecision(settingsSharedPreferences, precision);
                Util.setMethod(settingsSharedPreferences, method);

                if(currentMethod != method || currentPrecision != precision) {
                    Util.resetSamples(db);
                }

                Toast.makeText(getApplicationContext(), R.string.confirm_save_settings_text, Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), R.string.confirm_reset_training_text, Toast.LENGTH_SHORT).show();
            }
        });

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
