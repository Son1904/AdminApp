package com.example.universalyoga.admin.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.universalyoga.admin.R;
import com.example.universalyoga.admin.adapters.InstanceAdapter;
import com.example.universalyoga.admin.data.database.DatabaseHelper;
import com.example.universalyoga.admin.models.ClassInstance;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.CompositeDateValidator;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * This Activity manages the display, creation, editing, and deletion of class instances
 * for a specific course. It also handles syncing these changes with Cloud Firestore.
 */
public class ClassInstanceActivity extends AppCompatActivity implements InstanceAdapter.OnInstanceInteractionListener {

    // UI Components and Class Variables
    private RecyclerView recyclerView;
    private InstanceAdapter instanceAdapter;
    private List<ClassInstance> instanceList;
    private DatabaseHelper dbHelper;
    private long courseId = -1;
    private String requiredDayOfWeek;
    private FloatingActionButton fabAddInstance;

    /**
     * Called when the Activity is first created. This is where you should do all of your normal
     * static set up: create views, bind data to lists, etc.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_instance);

        // Setup the Toolbar with a title and a back button.
        Toolbar toolbar = findViewById(R.id.toolbar_instances);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize helpers and retrieve data from the Intent.
        dbHelper = new DatabaseHelper(this);
        courseId = getIntent().getLongExtra("COURSE_ID", -1);
        requiredDayOfWeek = getIntent().getStringExtra("COURSE_DAY_OF_WEEK");

        // Validate that necessary data was passed from the previous screen.
        if (courseId == -1 || requiredDayOfWeek == null) {
            Toast.makeText(this, "Error: Course data not found.", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity if data is missing.
            return;
        }

        // Set the activity title based on the course's day of the week.
        setTitle(requiredDayOfWeek + " Classes");

        // Setup the RecyclerView to display the list of instances.
        recyclerView = findViewById(R.id.recycler_view_instances);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        instanceList = new ArrayList<>();
        instanceAdapter = new InstanceAdapter(instanceList, this);
        recyclerView.setAdapter(instanceAdapter);

        // Setup the FloatingActionButton to open the "add instance" dialog.
        fabAddInstance = findViewById(R.id.fab_add_instance);
        fabAddInstance.setOnClickListener(v -> showInstanceDialog(null));

        // Load the initial list of instances from the database.
        loadInstances();
    }

    /**
     * This method is called when the activity will start interacting with the user.
     * We reload the instances here to ensure the list is always up-to-date
     * when the user navigates back to this screen.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadInstances();
    }

    /**
     * Displays a dialog for adding a new class instance or editing an existing one.
     * @param instance The ClassInstance to edit, or null to create a new one.
     */
    private void showInstanceDialog(@Nullable final ClassInstance instance) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_instance, null);
        builder.setView(dialogView);

        // Find views within the dialog layout.
        final EditText editTextDate = dialogView.findViewById(R.id.edit_text_instance_date);
        final Spinner spinnerTeacher = dialogView.findViewById(R.id.spinner_instance_teacher);
        final EditText editTextComments = dialogView.findViewById(R.id.edit_text_instance_comments);

        // Setup the teacher spinner with data from strings.xml.
        ArrayAdapter<CharSequence> teacherAdapter = ArrayAdapter.createFromResource(this,
                R.array.teachers_array, android.R.layout.simple_spinner_item);
        teacherAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTeacher.setAdapter(teacherAdapter);

        // If in "edit mode", populate the dialog with existing data.
        if (instance != null) {
            builder.setTitle("Edit Instance");
            editTextDate.setText(instance.getDate());
            setSpinnerToValue(spinnerTeacher, instance.getTeacher());
            editTextComments.setText(instance.getComments()); // Display existing comments.
        } else {
            builder.setTitle("Add New Instance");
        }

        // Set up the click listener for the date EditText to show the MaterialDatePicker.
        editTextDate.setOnClickListener(v -> {
            int requiredDayInt = getDayOfWeekFromString(requiredDayOfWeek);
            if (requiredDayInt == -1) {
                Toast.makeText(this, "Invalid course day.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create validation rules for the date picker.
            CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
            ArrayList<CalendarConstraints.DateValidator> validators = new ArrayList<>();
            validators.add(DateValidatorPointForward.now()); // Rule 1: Only allow dates from today onwards.
            validators.add(new DayOfWeekValidator(requiredDayInt)); // Rule 2: Only allow the correct day of the week.
            constraintsBuilder.setValidator(CompositeDateValidator.allOf(validators));

            // Build the MaterialDatePicker.
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select a " + requiredDayOfWeek)
                    .setCalendarConstraints(constraintsBuilder.build())
                    .build();

            // Handle the positive button click (when the user selects a date).
            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                calendar.setTimeInMillis(selection);
                editTextDate.setText(sdf.format(calendar.getTime()));
            });

            // Show the date picker.
            datePicker.show(getSupportFragmentManager(), "MATERIAL_DATE_PICKER");
        });

        builder.setPositiveButton("Save", null); // Set to null to override the default closing behavior.
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();

        // Override the "Save" button's click listener to prevent the dialog from closing on validation errors.
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String dateStr = editTextDate.getText().toString().trim();
                String teacher = spinnerTeacher.getSelectedItem().toString();
                String comments = editTextComments.getText().toString().trim();

                // If input is valid, save the data and dismiss the dialog.
                if (validateInstanceInput(editTextDate, spinnerTeacher)) {
                    if (instance == null) {
                        addInstanceToDb(dateStr, teacher, comments);
                    } else {
                        updateInstanceInDb(instance.getId(), dateStr, teacher, comments);
                    }
                    dialog.dismiss(); // Only dismiss if validation passes.
                }
            });
        });

        dialog.show();
    }

    /**
     * Validates the user input for the class instance dialog.
     * @return true if all required fields are valid, otherwise false.
     */
    private boolean validateInstanceInput(EditText editTextDate, Spinner spinnerTeacher) {
        boolean isValid = true;
        String dateStr = editTextDate.getText().toString().trim();

        // Clear previous errors.
        editTextDate.setError(null);
        TextView teacherErrorText = (TextView) spinnerTeacher.getSelectedView();
        teacherErrorText.setError(null);

        if (TextUtils.isEmpty(dateStr)) {
            editTextDate.setError("Date is required");
            isValid = false;
        }

        if (spinnerTeacher.getSelectedItemPosition() == 0) {
            teacherErrorText.setError("Required");
            teacherErrorText.setTextColor(Color.RED);
            isValid = false;
        }

        return isValid;
    }

    /**
     * Adds a new class instance to the local SQLite database and syncs it to Firestore.
     */
    private void addInstanceToDb(String date, String teacher, String comments) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_INSTANCE_COURSE_ID, courseId);
        values.put(DatabaseHelper.COLUMN_INSTANCE_DATE, date);
        values.put(DatabaseHelper.COLUMN_INSTANCE_TEACHER, teacher);
        values.put(DatabaseHelper.COLUMN_INSTANCE_COMMENTS, comments);

        long newRowId = db.insert(DatabaseHelper.TABLE_INSTANCES, null, values);
        if (newRowId != -1) {
            Toast.makeText(this, "Instance added locally!", Toast.LENGTH_SHORT).show();
            syncInstanceToFirestore(newRowId, values); // Sync the change to the cloud.
            loadInstances(); // Refresh the list.
        } else {
            Toast.makeText(this, "Error adding instance.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Updates an existing class instance in the local SQLite database and syncs it to Firestore.
     */
    private void updateInstanceInDb(long id, String date, String teacher, String comments) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_INSTANCE_DATE, date);
        values.put(DatabaseHelper.COLUMN_INSTANCE_TEACHER, teacher);
        values.put(DatabaseHelper.COLUMN_INSTANCE_COMMENTS, comments);

        String selection = DatabaseHelper.COLUMN_INSTANCE_ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };

        int count = db.update(DatabaseHelper.TABLE_INSTANCES, values, selection, selectionArgs);
        if (count > 0) {
            Toast.makeText(this, "Instance updated locally!", Toast.LENGTH_SHORT).show();
            syncInstanceToFirestore(id, values); // Sync the change to the cloud.
            loadInstances(); // Refresh the list.
        } else {
            Toast.makeText(this, "Error updating instance.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Deletes a class instance from the local SQLite database and syncs the deletion to Firestore.
     */
    private void deleteInstanceFromDb(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = DatabaseHelper.COLUMN_INSTANCE_ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };
        int deletedRows = db.delete(DatabaseHelper.TABLE_INSTANCES, selection, selectionArgs);
        if (deletedRows > 0) {
            Toast.makeText(this, "Instance deleted locally.", Toast.LENGTH_SHORT).show();
            deleteInstanceFromFirestore(id); // Sync the deletion to the cloud.
            loadInstances(); // Refresh the list.
        } else {
            Toast.makeText(this, "Error deleting instance.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Uploads a single class instance's data (on create or update) to Cloud Firestore.
     */
    private void syncInstanceToFirestore(long instanceId, ContentValues values) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No network. Instance will sync later.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore firestoreDb = FirebaseFirestore.getInstance();
        Map<String, Object> instanceData = new HashMap<>();
        instanceData.put("date", values.getAsString(DatabaseHelper.COLUMN_INSTANCE_DATE));
        instanceData.put("teacher", values.getAsString(DatabaseHelper.COLUMN_INSTANCE_TEACHER));
        instanceData.put("comments", values.getAsString(DatabaseHelper.COLUMN_INSTANCE_COMMENTS));

        // Note: This creates the instance document inside a subcollection of the parent course document.
        firestoreDb.collection("courses").document(String.valueOf(courseId))
                .collection("instances").document(String.valueOf(instanceId))
                .set(instanceData)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Instance synced.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Instance sync failed.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Deletes a single class instance document from Cloud Firestore.
     */
    private void deleteInstanceFromFirestore(long instanceId) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No network. Deletion will sync later.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore firestoreDb = FirebaseFirestore.getInstance();
        firestoreDb.collection("courses").document(String.valueOf(courseId))
                .collection("instances").document(String.valueOf(instanceId))
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Instance deleted from cloud.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete from cloud.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Loads all instances for the current course from the local SQLite database and updates the RecyclerView.
     */
    @SuppressLint("NotifyDataSetChanged")
    private void loadInstances() {
        instanceList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // We need to query for the comments column as well.
        String[] projection = {
                DatabaseHelper.COLUMN_INSTANCE_ID,
                DatabaseHelper.COLUMN_INSTANCE_DATE,
                DatabaseHelper.COLUMN_INSTANCE_TEACHER,
                DatabaseHelper.COLUMN_INSTANCE_COMMENTS
        };

        String selection = DatabaseHelper.COLUMN_INSTANCE_COURSE_ID + " = ?";
        String[] selectionArgs = { String.valueOf(courseId) };
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_INSTANCES, projection, selection, selectionArgs, null, null, null
        );
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_INSTANCE_ID));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_INSTANCE_DATE));
            String teacher = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_INSTANCE_TEACHER));
            String comments = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_INSTANCE_COMMENTS));
            instanceList.add(new ClassInstance(id, date, teacher, comments));
        }
        cursor.close();
        instanceAdapter.notifyDataSetChanged();

        // Check FAB visibility after loading data.
        checkScrollAndShowFab();
    }

    // --- HELPER METHODS AND CALLBACKS ---

    /**
     * Checks if the RecyclerView is scrollable. If not, it forces the FAB to be visible.
     */
    private void checkScrollAndShowFab() {
        boolean isScrollable = recyclerView.computeVerticalScrollRange() > recyclerView.getHeight();
        if (!isScrollable && !fabAddInstance.isShown()) {
            fabAddInstance.show();
        }
    }

    /**
     * Checks if a network connection is available.
     * @return true if connected, false otherwise.
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Converts a day name (e.g., "Monday") into its corresponding Calendar constant (e.g., Calendar.MONDAY).
     */
    private int getDayOfWeekFromString(String day) {
        switch (day.toLowerCase()) {
            case "sunday": return Calendar.SUNDAY;
            case "monday": return Calendar.MONDAY;
            case "tuesday": return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday": return Calendar.THURSDAY;
            case "friday": return Calendar.FRIDAY;
            case "saturday": return Calendar.SATURDAY;
            default: return -1;
        }
    }

    /**
     * Sets the selection of a Spinner to a specific string value.
     */
    private void setSpinnerToValue(Spinner spinner, String value) {
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinner.getAdapter();
        if (adapter != null && value != null) {
            int position = adapter.getPosition(value);
            spinner.setSelection(position);
        }
    }

    /**
     * Validates if a given date string matches the required day of the week.
     */
    private boolean isDateValid(String dateStr, String requiredDay) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
            sdf.setLenient(false);
            Date date = sdf.parse(dateStr);
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.ENGLISH);
            return dayFormat.format(date).equalsIgnoreCase(requiredDay);
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Handles clicks on menu items in the Toolbar (specifically, the back button).
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Close the current activity.
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Callback from the adapter, triggered when the user clicks on an instance to edit it.
     */
    @Override
    public void onInstanceClick(ClassInstance instance) {
        showInstanceDialog(instance);
    }

    /**
     * Callback from the adapter, triggered when the user clicks the delete button for an instance.
     */
    @Override
    public void onInstanceDelete(long instanceId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Instance")
                .setMessage("Are you sure you want to delete this instance?")
                .setPositiveButton("Delete", (dialog, which) -> deleteInstanceFromDb(instanceId))
                .setNegativeButton("Cancel", null)
                .show();
    }
}