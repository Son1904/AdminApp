package com.example.universalyoga.admin.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.universalyoga.admin.R;

/**
 * This Activity displays the details entered by the user for one final confirmation.
 * The user can either confirm to save the data or go back to edit it.
 */
public class ConfirmCourseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_course);

        // Setup the Toolbar with a title and back button.
        Toolbar toolbar = findViewById(R.id.toolbar_confirm);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Find all TextViews in the layout.
        TextView textViewDay = findViewById(R.id.text_view_day_confirm);
        TextView textViewTime = findViewById(R.id.text_view_time_confirm);
        TextView textViewCapacity = findViewById(R.id.text_view_capacity_confirm);
        TextView textViewDuration = findViewById(R.id.text_view_duration_confirm);
        TextView textViewPrice = findViewById(R.id.text_view_price_confirm);
        TextView textViewType = findViewById(R.id.text_view_type_confirm);
        TextView textViewDescription = findViewById(R.id.text_view_description_confirm);

        // Get the data passed from the previous activity.
        Intent intent = getIntent();
        String day = intent.getStringExtra("day");
        String time = intent.getStringExtra("time");
        String capacity = intent.getStringExtra("capacity");
        String duration = intent.getStringExtra("duration");
        String price = intent.getStringExtra("price");
        String type = intent.getStringExtra("type");
        String description = intent.getStringExtra("description");

        // Set the data to the corresponding TextViews.
        textViewDay.setText(day);
        textViewTime.setText(time);
        textViewCapacity.setText(capacity);
        textViewDuration.setText(duration + " minutes");
        textViewPrice.setText("£" + String.format("%.2f", Double.parseDouble(price)));
        textViewType.setText(type);
        textViewDescription.setText(description);

        // Find buttons.
        Button buttonConfirm = findViewById(R.id.button_confirm);
        Button buttonEdit = findViewById(R.id.button_edit);

        // Set listener for the Confirm button.
        // It returns RESULT_OK to notify the previous activity to proceed with saving.
        buttonConfirm.setOnClickListener(v -> {
            setResult(Activity.RESULT_OK);
            finish();
        });

        // Set listener for the Edit button.
        // It returns RESULT_CANCELED to notify the previous activity that the user wants to edit.
        buttonEdit.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
    }

    /**
     * Handles clicks on menu items in the Toolbar (e.g., the back button).
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Treat the back button press the same as pressing "Edit".
            setResult(Activity.RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}