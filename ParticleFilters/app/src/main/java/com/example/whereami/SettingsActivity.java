package com.example.whereami;

import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity implements OnClickListener {

    SharedPreferences settingsSharedPreferences;
    Spinner layoutSpinner, sensitivitySpinner, stepsizeSpinner, particlesSpinner;
    private Button buttonSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        settingsSharedPreferences = getApplicationContext().getSharedPreferences("SETTINGS", 0);

        buttonSave = (Button) findViewById(R.id.button_save_settings);
        buttonSave.setOnClickListener(this);

        String selectedLayout = settingsSharedPreferences.getString("layout", "Joost");
        String selectedSensitivity = settingsSharedPreferences.getString("sensitivity", "10");
        String selectedStepSize = settingsSharedPreferences.getString("stepsize", "1");
        String selectedParticles = settingsSharedPreferences.getString("particles", "5000");

        layoutSpinner = (Spinner) findViewById(R.id.spinner_layout);
        ArrayAdapter<String> layoutAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.layout_array));
        layoutSpinner.setAdapter(layoutAdapter);
        layoutSpinner.setSelection(layoutAdapter.getPosition(selectedLayout));

        sensitivitySpinner = (Spinner) findViewById(R.id.spinner_sensitivity);
        ArrayAdapter<String> sensitivityAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.sensitivity_array));
        sensitivitySpinner.setAdapter(sensitivityAdapter);
        sensitivitySpinner.setSelection(sensitivityAdapter.getPosition(selectedSensitivity));

        stepsizeSpinner = (Spinner) findViewById(R.id.spinner_stepsize);
        ArrayAdapter<String> stepsizeAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.stepsize_array));
        stepsizeSpinner.setAdapter(stepsizeAdapter);
        stepsizeSpinner.setSelection(stepsizeAdapter.getPosition(selectedStepSize));

        particlesSpinner = (Spinner) findViewById(R.id.spinner_particles);
        ArrayAdapter<String> particlesAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.particles_array));
        particlesSpinner.setAdapter(particlesAdapter);
        particlesSpinner.setSelection(particlesAdapter.getPosition(selectedParticles));
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // SAVE BUTTON
            case R.id.button_save_settings: {
                String newLayout = layoutSpinner.getSelectedItem().toString();
                String newSensitivity = sensitivitySpinner.getSelectedItem().toString();
                String newStepSize = stepsizeSpinner.getSelectedItem().toString();
                String newParticles = particlesSpinner.getSelectedItem().toString();

                SharedPreferences.Editor editor = settingsSharedPreferences.edit();
                editor.putString("layout",newLayout);
                editor.putString("sensitivity",newSensitivity);
                editor.putString("stepsize",newStepSize);
                editor.putString("particles",newParticles);
                editor.commit();

                Toast.makeText(getApplicationContext(), R.string.confirm_save_settings_text, Toast.LENGTH_SHORT).show();

                break;
            }
        }
    }
}
