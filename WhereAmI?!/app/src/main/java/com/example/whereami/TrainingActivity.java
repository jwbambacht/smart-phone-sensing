package com.example.whereami;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class TrainingActivity extends AppCompatActivity {

    List<Location> currentLocations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("ALL_SAMPLES",0);

        this.currentLocations = Util.loadLocations(sharedPreferences);

        final RadioGroup radioGroupCells = (RadioGroup) findViewById(R.id.radiogroup_cells);
        fillRadioGroup(this, radioGroupCells, R.array.cell_array);

        final RadioGroup radioGroupActivity = (RadioGroup) findViewById(R.id.radiogroup_activity);
        fillRadioGroup(this, radioGroupActivity, R.array.activity_array);

        Button trainButton = (Button) findViewById(R.id.button_add_training);
        trainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int selectedCell = radioGroupCells.getCheckedRadioButtonId() % radioGroupCells.getChildCount();
                int selectedActivity = (radioGroupActivity.getCheckedRadioButtonId()-radioGroupCells.getChildCount()) % radioGroupActivity.getChildCount();

                addToTraining(selectedCell, selectedActivity);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    private static void fillRadioGroup(Context context, RadioGroup radioGroup, int stringArrayId){
        for (String s : context.getResources().getStringArray(stringArrayId)){
            RadioButton radioButton = new RadioButton(context);
            radioButton.setText(s);
            radioButton.setHighlightColor(Color.WHITE);
            radioButton.setTextColor(Color.WHITE);
            ColorStateList colorStateList = new ColorStateList(
                    new int[][]{
                            new int[]{-android.R.attr.state_checked},
                            new int[]{android.R.attr.state_checked}
                    },
                    new int[]{
                            Color.GRAY,
                            Color.WHITE
                    }
            );
            radioButton.setButtonTintList(colorStateList);
            radioGroup.addView(radioButton);
        }

        if(radioGroup.getChildCount() > 0)
            radioGroup.check(radioGroup.getChildAt(0).getId());
    }

    public void addToTraining(int cellID, int activityID) {
        Log.i("Selected Cell", cellID + "");
        Log.i("Select Activity", activityID + "");

        // GET CHOSEN METHOD FROM SETTINGS
        SharedPreferences settings = getApplicationContext().getSharedPreferences("SETTINGS", 0);
        int method = settings.getInt("method", 0);

//        SharedPreferences.Editor editor = settings.edit();
//        editor.putInt("precision",4);

        if (method == 0) {
            Log.i("Method", "KNN");
        } else if (method == 1) {
            Log.i("Method", "Bayesian Filters");
        } else if (method == 2) {
            Log.i("Method", "Particle Filters");
        }

        byte[] BSSID = new byte[0];
        int[] RSSI = new int[0];

        try {
            Location newLocation = new Location(cellID, activityID, BSSID, RSSI);
            this.currentLocations.add(newLocation);
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("ALL_SAMPLES",0);
            Util.saveLocations(sharedPreferences, this.currentLocations);

            Toast.makeText(getApplicationContext(), R.string.confirm_add_training_text, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), R.string.error_add_training_text, Toast.LENGTH_SHORT).show();
            throw new RuntimeException(e);
        }
    }
}
