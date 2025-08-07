package com.example.universalyoga.admin.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import com.example.universalyoga.admin.R;
import com.example.universalyoga.admin.data.database.DatabaseHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * This Activity handles the creation and editing of a yoga course schedule.
 * It provides a form for the user to enter all course details, validates the input,
 * shows a confirmation screen, and then saves the data to both the local SQLite database
 * and the remote Cloud Firestore database.
 */
public class CourseDetailsActivity extends AppCompatActivity {

    // A request code used when starting the confirmation activity.
    private static final int CONFIRMATION_REQUEST_CODE = 1;

    // UI component variables
    private TextInputEditText editTextPrice, editTextDescription;
    private Spinner spinnerDayOfWeek, spinnerTime, spinnerCapacity, spinnerDuration, spinnerClassType;
    private Button buttonSave;

    // Helper for database operations
    private DatabaseHelper dbHelper;

    // Variable to store the ID of the course being edited. -1 indicates "add new" mode.
    private long courseId = -1;

    /**
     * Called when the Activity is first created.
     * This is where you should do all of your normal static set up: create views,
     * bind data to lists, etc.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_details);

        // Setup the Toolbar with a title and a back button.
        Toolbar toolbar = findViewById(R.id.toolbar_course_details);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize the database helper and UI views.
        dbHelper = new DatabaseHelper(this);
        initViews();
        setupSpinners();

        // Check if an ID was passed via the Intent, which indicates "edit mode".
        if (getIntent().hasExtra("COURSE_ID")) {
            courseId = getIntent().getLongExtra("COURSE_ID", -1);
            if (courseId != -1) {
                setTitle("Edit Course"); // Change the Toolbar title for clarity.
                loadCourseData(courseId); // Load existing data into the form.
            }
        }

        // Set the click listener for the save button.
        buttonSave.setOnClickListener(v -> proceedToConfirmation());
    }

    /**
     * Initializes all UI component variables by finding them in the layout file.
     */
    private void initViews() {
        spinnerDayOfWeek = findViewById(R.id.spinner_day_of_week);
        spinnerTime = findViewById(R.id.spinner_time);
        spinnerCapacity = findViewById(R.id.spinner_capacity);
        spinnerDuration = findViewById(R.id.spinner_duration);
        spinnerClassType = findViewById(R.id.spinner_class_type);
        editTextPrice = findViewById(R.id.edit_text_price);
        editTextDescription = findViewById(R.id.edit_text_description);
        buttonSave = findViewById(R.id.button_save_course);
    }

    /**
     * Populates all Spinners with their respective data from string arrays.
     */
    private void setupSpinners() {
        setupSpinner(spinnerDayOfWeek, R.array.days_of_week_array);
        setupSpinner(spinnerTime, R.array.time_slots_array);
        setupSpinner(spinnerCapacity, R.array.capacity_array);
        setupSpinner(spinnerDuration, R.array.duration_array);
        setupSpinner(spinnerClassType, R.array.class_types_array);
    }

    /**
     * A helper method to set up a single Spinner with an adapter from a resource array.
     * @param spinner The Spinner to set up.
     * @param arrayResId The resource ID of the string-array containing the options.
     */
    private void setupSpinner(Spinner spinner, int arrayResId) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                arrayResId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    /**
     * Loads the data for a specific course from the SQLite database and populates the UI fields.
     * This is used when the activity is in "edit mode".
     * @param id The ID of the course to load.
     */
    @SuppressLint("Range")
    private void loadCourseData(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COLUMN_COURSE_ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };
        Cursor cursor = db.query(DatabaseHelper.TABLE_COURSES, null, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            // Set the selection for each Spinner.
            setSpinnerToValue(spinnerDayOfWeek, cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_DAY_OF_WEEK)));
            setSpinnerToValue(spinnerTime, cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_TIME)));
            setSpinnerToValue(spinnerClassType, cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_TYPE)));
            setSpinnerToValue(spinnerCapacity, String.valueOf(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_CAPACITY))));
            setSpinnerToValue(spinnerDuration, String.valueOf(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_DURATION))));

            // Set the text for the EditTexts.
            editTextPrice.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_PRICE))));
            editTextDescription.setText(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_DESCRIPTION)));
        }
        cursor.close();
    }

    /**
     * A helper method to find the position of a value in a Spinner's adapter and set the selection.
     * @param spinner The Spinner to modify.
     * @param value The string value to select.
     */
    private void setSpinnerToValue(Spinner spinner, String value) {
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinner.getAdapter();
        if (adapter != null && value != null) {
            int position = adapter.getPosition(value);
            spinner.setSelection(position);
        }
    }

    /**
     * Validates input, then packages the data into an Intent and starts the confirmation activity.
     */
    private void proceedToConfirmation() {
        if (!validateInput()) return; // Stop if validation fails.

        Intent intent = new Intent(this, ConfirmCourseActivity.class);
        intent.putExtra("day", spinnerDayOfWeek.getSelectedItem().toString());
        intent.putExtra("time", spinnerTime.getSelectedItem().toString());
        intent.putExtra("capacity", spinnerCapacity.getSelectedItem().toString());
        intent.putExtra("duration", spinnerDuration.getSelectedItem().toString());
        intent.putExtra("price", editTextPrice.getText().toString());
        intent.putExtra("type", spinnerClassType.getSelectedItem().toString());
        intent.putExtra("description", editTextDescription.getText().toString());
        startActivityForResult(intent, CONFIRMATION_REQUEST_CODE);
    }

    /**
     * Called when the confirmation activity finishes.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // If the user confirmed the details, proceed to save to the database.
        if (requestCode == CONFIRMATION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            saveCourseToDatabase();
        }
    }

    /**
     * Saves the course data to the local SQLite database. It handles both creating a new course (INSERT)
     * and updating an existing one (UPDATE).
     */
    private void saveCourseToDatabase() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_COURSE_DAY_OF_WEEK, spinnerDayOfWeek.getSelectedItem().toString());
        values.put(DatabaseHelper.COLUMN_COURSE_TIME, spinnerTime.getSelectedItem().toString());
        values.put(DatabaseHelper.COLUMN_COURSE_TYPE, spinnerClassType.getSelectedItem().toString());
        values.put(DatabaseHelper.COLUMN_COURSE_CAPACITY, Integer.parseInt(spinnerCapacity.getSelectedItem().toString()));
        values.put(DatabaseHelper.COLUMN_COURSE_DURATION, Integer.parseInt(spinnerDuration.getSelectedItem().toString()));
        values.put(DatabaseHelper.COLUMN_COURSE_DESCRIPTION, editTextDescription.getText().toString().trim());
        values.put(DatabaseHelper.COLUMN_COURSE_PRICE, Double.parseDouble(editTextPrice.getText().toString()));

        if (courseId == -1) { // "Add new" mode
            long newRowId = db.insert(DatabaseHelper.TABLE_COURSES, null, values);
            if (newRowId != -1) {
                Toast.makeText(this, "Course saved locally!", Toast.LENGTH_SHORT).show();
                syncSingleCourseToFirestore(newRowId, values); // Sync the new course to the cloud.
                finish(); // Close the activity.
            } else {
                Toast.makeText(this, "Error saving course.", Toast.LENGTH_SHORT).show();
            }
        } else { // "Edit" mode
            String selection = DatabaseHelper.COLUMN_COURSE_ID + " = ?";
            String[] selectionArgs = { String.valueOf(courseId) };
            int count = db.update(DatabaseHelper.TABLE_COURSES, values, selection, selectionArgs);
            if (count > 0) {
                Toast.makeText(this, "Course updated locally!", Toast.LENGTH_SHORT).show();
                syncSingleCourseToFirestore(courseId, values); // Sync the updated course to the cloud.
                finish(); // Close the activity.
            } else {
                Toast.makeText(this, "Error updating course.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Syncs a single course's data (on create or update) to Cloud Firestore.
     * @param id The ID of the course.
     * @param values The ContentValues containing the course data.
     */
    private void syncSingleCourseToFirestore(long id, ContentValues values) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No network. Will sync later.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore firestoreDb = FirebaseFirestore.getInstance();
        Map<String, Object> courseData = new HashMap<>();
        courseData.put("dayOfWeek", values.getAsString(DatabaseHelper.COLUMN_COURSE_DAY_OF_WEEK));
        courseData.put("time", values.getAsString(DatabaseHelper.COLUMN_COURSE_TIME));
        courseData.put("type", values.getAsString(DatabaseHelper.COLUMN_COURSE_TYPE));
        courseData.put("capacity", values.getAsInteger(DatabaseHelper.COLUMN_COURSE_CAPACITY));
        courseData.put("duration", values.getAsInteger(DatabaseHelper.COLUMN_COURSE_DURATION));
        courseData.put("price", values.getAsDouble(DatabaseHelper.COLUMN_COURSE_PRICE));
        courseData.put("description", values.getAsString(DatabaseHelper.COLUMN_COURSE_DESCRIPTION));

        firestoreDb.collection("courses").document(String.valueOf(id))
                .set(courseData)
                .addOnSuccessListener(aVoid -> Toast.makeText(CourseDetailsActivity.this, "Synced to cloud.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(CourseDetailsActivity.this, "Sync failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Checks if a network connection is available.
     * @return true if connected, false otherwise.
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Validates all required input fields in the form.
     * @return true if all inputs are valid, otherwise false.
     */
    private boolean validateInput() {
        boolean isValid = true;

        // Validate all Spinners to ensure a selection has been made.
        if (spinnerDayOfWeek.getSelectedItemPosition() == 0) {
            TextView errorText = (TextView) spinnerDayOfWeek.getSelectedView();
            errorText.setError("Required");
            isValid = false;
        }
        if (spinnerTime.getSelectedItemPosition() == 0) {
            TextView errorText = (TextView) spinnerTime.getSelectedView();
            errorText.setError("Required");
            isValid = false;
        }
        if (spinnerCapacity.getSelectedItemPosition() == 0) {
            TextView errorText = (TextView) spinnerCapacity.getSelectedView();
            errorText.setError("Required");
            isValid = false;
        }
        if (spinnerDuration.getSelectedItemPosition() == 0) {
            TextView errorText = (TextView) spinnerDuration.getSelectedView();
            errorText.setError("Required");
            isValid = false;
        }
        if (spinnerClassType.getSelectedItemPosition() == 0) {
            TextView errorText = (TextView) spinnerClassType.getSelectedView();
            errorText.setError("Required");
            isValid = false;
        }

        // Validate the price field.
        String priceStr = editTextPrice.getText().toString().trim();
        if (TextUtils.isEmpty(priceStr)) {
            editTextPrice.setError("This field is required");
            isValid = false;
        } else {
            try {
                double price = Double.parseDouble(priceStr);
                if (price <= 0) {
                    editTextPrice.setError("Price must be a positive number");
                    isValid = false;
                } else if (price > 1000) {
                    editTextPrice.setError("Price seems too high (max £1000)");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                editTextPrice.setError("Please enter a valid price");
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * Handles clicks on menu items in the Toolbar (specifically, the back button).
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}