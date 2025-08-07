package com.example.universalyoga.admin;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.universalyoga.admin.activities.ClassInstanceActivity;
import com.example.universalyoga.admin.activities.CourseDetailsActivity;
import com.example.universalyoga.admin.activities.SearchActivity;
import com.example.universalyoga.admin.adapters.CourseAdapter;
import com.example.universalyoga.admin.data.database.DatabaseHelper;
import com.example.universalyoga.admin.models.Course;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the main screen of the Admin App.
 * It displays a list of all yoga courses and provides options to add, edit, delete,
 * search, reset, and upload data.
 */
public class MainActivity extends AppCompatActivity implements CourseAdapter.OnItemInteractionListener {

    // UI Components and Class Variables
    private FloatingActionButton fabAddCourse;
    private RecyclerView recyclerView;
    private CourseAdapter courseAdapter;
    private List<Course> courseList;
    private DatabaseHelper dbHelper;

    /**
     * Called when the Activity is first created. This is where you should do all of your
     * normal static set up: create views, bind data to lists, etc.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup the Toolbar as the app bar for the activity.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize the database helper and UI components.
        dbHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.recycler_view_courses);
        fabAddCourse = findViewById(R.id.fab_add_course);

        // Setup the RecyclerView to display the list of courses.
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        courseList = new ArrayList<>();
        courseAdapter = new CourseAdapter(courseList, this);
        recyclerView.setAdapter(courseAdapter);

        // Set a click listener for the FloatingActionButton to open the course creation screen.
        fabAddCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CourseDetailsActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * This method is called when the activity will start interacting with the user.
     * We reload the courses here to ensure the list is always up-to-date.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadCourses();
    }

    /**
     * Initializes the contents of the Activity's standard options menu.
     * @param menu The options menu in which you place your items.
     * @return You must return true for the menu to be displayed.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_search) {
            startActivity(new Intent(this, SearchActivity.class));
            return true;
        } else if (itemId == R.id.action_reset_database) {
            showResetConfirmationDialog();
            return true;
        } else if (itemId == R.id.action_upload) {
            uploadData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Loads all courses from the local SQLite database and updates the RecyclerView.
     */
    @SuppressLint("NotifyDataSetChanged")
    private void loadCourses() {
        List<Course> newCourses = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                DatabaseHelper.COLUMN_COURSE_ID,
                DatabaseHelper.COLUMN_COURSE_TYPE,
                DatabaseHelper.COLUMN_COURSE_DAY_OF_WEEK,
                DatabaseHelper.COLUMN_COURSE_TIME
        };
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_COURSES, projection, null, null, null, null, null
        );
        while (cursor.moveToNext()) {
            @SuppressLint("Range") long id = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_ID));
            @SuppressLint("Range") String type = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_TYPE));
            @SuppressLint("Range") String day = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_DAY_OF_WEEK));
            @SuppressLint("Range") String time = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_TIME));
            newCourses.add(new Course(id, day, time, type));
        }
        cursor.close();

        if (courseAdapter != null) {
            courseAdapter.updateData(newCourses);
        }
        checkScrollAndShowFab();
    }

    /**
     * Callback from the adapter, triggered when the user clicks the delete button for a course.
     */
    @Override
    public void onItemDelete(long courseId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete this course? This will also delete all its class instances.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteCourseFromDb(courseId);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Callback from the adapter, triggered when the user clicks on the main info area of a course.
     * This opens the ClassInstanceActivity to view/manage instances for that course.
     */
    @Override
    public void onItemClick(Course course) {
        Intent intent = new Intent(this, ClassInstanceActivity.class);
        intent.putExtra("COURSE_ID", course.getId());
        intent.putExtra("COURSE_DAY_OF_WEEK", course.getDayOfWeek());
        startActivity(intent);
    }

    /**
     * Callback from the adapter, triggered when the user clicks the edit icon for a course.
     * This opens the CourseDetailsActivity in "edit mode".
     */
    @Override
    public void onItemEdit(Course course) {
        Intent intent = new Intent(this, CourseDetailsActivity.class);
        intent.putExtra("COURSE_ID", course.getId());
        startActivity(intent);
    }

    /**
     * Deletes a course from the local SQLite database and syncs the deletion to Firestore.
     */
    private void deleteCourseFromDb(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = DatabaseHelper.COLUMN_COURSE_ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };
        int deletedRows = db.delete(DatabaseHelper.TABLE_COURSES, selection, selectionArgs);
        if (deletedRows > 0) {
            Toast.makeText(this, "Course deleted locally.", Toast.LENGTH_SHORT).show();
            deleteCourseFromFirestore(id);
            loadCourses();
        } else {
            Toast.makeText(this, "Error deleting course.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Deletes a course document from Cloud Firestore.
     */
    private void deleteCourseFromFirestore(long id) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No network. Deletion will sync later.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore firestoreDb = FirebaseFirestore.getInstance();
        firestoreDb.collection("courses").document(String.valueOf(id))
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Deleted from cloud.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Failed to delete from cloud.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Uploads all local data from SQLite to Cloud Firestore in a single batch operation.
     */
    private void uploadData() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection available.", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "Starting upload...", Toast.LENGTH_SHORT).show();

        FirebaseFirestore firestoreDb = FirebaseFirestore.getInstance();
        WriteBatch batch = firestoreDb.batch();
        SQLiteDatabase sqliteDb = dbHelper.getReadableDatabase();

        Cursor courseCursor = sqliteDb.query(DatabaseHelper.TABLE_COURSES, null, null, null, null, null, null);

        if (courseCursor.getCount() == 0) {
            Toast.makeText(this, "No data to upload.", Toast.LENGTH_SHORT).show();
            courseCursor.close();
            return;
        }

        while (courseCursor.moveToNext()) {
            @SuppressLint("Range") long courseId = courseCursor.getLong(courseCursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_ID));
            @SuppressLint("Range") String dayOfWeek = courseCursor.getString(courseCursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_DAY_OF_WEEK));
            @SuppressLint("Range") String time = courseCursor.getString(courseCursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_TIME));
            @SuppressLint("Range") String type = courseCursor.getString(courseCursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_TYPE));
            @SuppressLint("Range") int capacity = courseCursor.getInt(courseCursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_CAPACITY));
            @SuppressLint("Range") int duration = courseCursor.getInt(courseCursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_DURATION));
            @SuppressLint("Range") double price = courseCursor.getDouble(courseCursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_PRICE));
            @SuppressLint("Range") String description = courseCursor.getString(courseCursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_DESCRIPTION));

            Map<String, Object> courseData = new HashMap<>();
            courseData.put("dayOfWeek", dayOfWeek);
            courseData.put("time", time);
            courseData.put("type", type);
            courseData.put("capacity", capacity);
            courseData.put("duration", duration);
            courseData.put("price", price);
            courseData.put("description", description);

            batch.set(firestoreDb.collection("courses").document(String.valueOf(courseId)), courseData);

            Cursor instanceCursor = sqliteDb.query(
                    DatabaseHelper.TABLE_INSTANCES, null,
                    DatabaseHelper.COLUMN_INSTANCE_COURSE_ID + " = ?",
                    new String[]{String.valueOf(courseId)},
                    null, null, null
            );

            while (instanceCursor.moveToNext()) {
                @SuppressLint("Range") long instanceId = instanceCursor.getLong(instanceCursor.getColumnIndex(DatabaseHelper.COLUMN_INSTANCE_ID));
                @SuppressLint("Range") String date = instanceCursor.getString(instanceCursor.getColumnIndex(DatabaseHelper.COLUMN_INSTANCE_DATE));
                @SuppressLint("Range") String teacher = instanceCursor.getString(instanceCursor.getColumnIndex(DatabaseHelper.COLUMN_INSTANCE_TEACHER));
                @SuppressLint("Range") String comments = instanceCursor.getString(instanceCursor.getColumnIndex(DatabaseHelper.COLUMN_INSTANCE_COMMENTS));

                Map<String, Object> instanceData = new HashMap<>();
                instanceData.put("date", date);
                instanceData.put("teacher", teacher);
                instanceData.put("comments", comments);

                batch.set(firestoreDb.collection("courses").document(String.valueOf(courseId))
                        .collection("instances").document(String.valueOf(instanceId)), instanceData);
            }
            instanceCursor.close();
        }
        courseCursor.close();

        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(MainActivity.this, "All data uploaded successfully!", Toast.LENGTH_LONG).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(MainActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Shows a confirmation dialog before deleting all data from the local database.
     */
    private void showResetConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Database")
                .setMessage("Are you sure you want to delete all courses? This action cannot be undone.")
                .setPositiveButton("Yes, Reset", (dialog, which) -> resetDatabase())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Deletes all data from the 'courses' and 'instances' tables in the local SQLite database.
     */
    private void resetDatabase() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_COURSES, null, null);
        db.delete(DatabaseHelper.TABLE_INSTANCES, null, null);
        Toast.makeText(this, "Database has been reset.", Toast.LENGTH_SHORT).show();
        loadCourses();
    }

    /**
     * Checks if the RecyclerView is scrollable. If not, it forces the FAB to be visible.
     */
    private void checkScrollAndShowFab() {
        boolean isScrollable = recyclerView.computeVerticalScrollRange() > recyclerView.getHeight();
        if (!isScrollable && !fabAddCourse.isShown()) {
            fabAddCourse.show();
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
}