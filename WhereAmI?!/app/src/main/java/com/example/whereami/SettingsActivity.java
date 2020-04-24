package com.example.whereami;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Spinner methodSpinner = (Spinner) findViewById(R.id.spinner_method);
        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.method_array));
        methodSpinner.setAdapter(methodAdapter);

        Spinner precisionSpinner = (Spinner) findViewById(R.id.spinner_precision);
        ArrayAdapter<String> precisionAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.precision_array));
        precisionSpinner.setAdapter(precisionAdapter);

        Button resetButton = (Button) findViewById(R.id.button_reset_training);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("ALL_SAMPLES",0);
                Util.resetLocations(sharedPreferences);

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
