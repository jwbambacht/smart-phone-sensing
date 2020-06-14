package com.example.whereami;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity implements OnClickListener {

    SharedPreferences settingsSharedPreferences;
    Spinner layoutSpinner, sensitivitySpinner, stepsizeSpinner, steptimeSpinner, particlesSpinner;
    private Button buttonSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        buttonSave = (Button) findViewById(R.id.button_save_settings);
        buttonSave.setOnClickListener(this);

        // Obtain values from shared preferences
        settingsSharedPreferences = getApplicationContext().getSharedPreferences("SETTINGS", 0);
        String selectedLayout = settingsSharedPreferences.getString("layout", "Joost");
        String selectedSensitivity = settingsSharedPreferences.getString("sensitivity", "10");
        String selectedStepSize = settingsSharedPreferences.getString("stepsize", "1");
        String selectedStepTime = settingsSharedPreferences.getString("steptime", "0.5");
        String selectedParticles = settingsSharedPreferences.getString("particles", "2000");

        // Create Spinners
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

        steptimeSpinner = (Spinner) findViewById(R.id.spinner_steptime);
        ArrayAdapter<String> steptimeAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.steptime_array));
        steptimeSpinner.setAdapter(steptimeAdapter);
        steptimeSpinner.setSelection(steptimeAdapter.getPosition(selectedStepTime));

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
            case R.id.button_save_settings: {
                String newLayout = layoutSpinner.getSelectedItem().toString();
                String newSensitivity = sensitivitySpinner.getSelectedItem().toString();
                String newStepSize = stepsizeSpinner.getSelectedItem().toString();
                String newStepTime = steptimeSpinner.getSelectedItem().toString();
                String newParticles = particlesSpinner.getSelectedItem().toString();

                SharedPreferences.Editor editor = settingsSharedPreferences.edit();
                editor.putString("layout",newLayout);
                editor.putString("sensitivity",newSensitivity);
                editor.putString("stepsize",newStepSize);
                editor.putString("steptime",newStepTime);
                editor.putString("particles",newParticles);
                editor.commit();

                Toast.makeText(getApplicationContext(), R.string.confirm_save_settings_text, Toast.LENGTH_SHORT).show();

                break;
            }
        }
    }
}
