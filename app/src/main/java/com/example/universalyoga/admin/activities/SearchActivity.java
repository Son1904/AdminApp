package com.example.universalyoga.admin.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.universalyoga.admin.R;
import com.example.universalyoga.admin.adapters.SearchAdapter;
import com.example.universalyoga.admin.data.database.DatabaseHelper;
import com.example.universalyoga.admin.models.SearchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * This Activity allows the user to search for class instances in the database
 * based on a query and displays the results in a list.
 */
public class SearchActivity extends AppCompatActivity implements SearchAdapter.OnSearchResultClickListener {

    // Class variables for UI components, data, and the database helper.
    private DatabaseHelper dbHelper;
    private SearchAdapter searchAdapter;
    private List<SearchResult> searchResults;
    private RecyclerView recyclerView;
    private SearchView searchView;

    /**
     * Called when the Activity is first created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Setup the Toolbar with a title and a back button.
        Toolbar toolbar = findViewById(R.id.toolbar_search);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize components.
        dbHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.recycler_view_search_results);
        searchView = findViewById(R.id.search_view);

        // Setup the RecyclerView.
        searchResults = new ArrayList<>();
        searchAdapter = new SearchAdapter(searchResults, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(searchAdapter);

        // Activate the search functionality.
        setupSearch();
    }

    /**
     * Sets up the listener for the SearchView to handle user queries.
     */
    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            /**
             * Called when the user submits the query (e.g., presses Enter).
             * We can keep this to hide the keyboard.
             */
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                return true;
            }

            /**
             * Called when the query text is changed by the user.
             * This will trigger the search in real-time.
             */
            @Override
            public boolean onQueryTextChange(String newText) {
                // Perform the search every time the user types or deletes a character.
                performSearch(newText);
                return true;
            }
        });
    }

    /**
     * Performs a JOIN query on the database to search across multiple tables and fields.
     * @param query The user's search keyword.
     */
    @SuppressLint("NotifyDataSetChanged")
    private void performSearch(String query) {
        List<SearchResult> newResults = new ArrayList<>();

        // If the query is not empty, perform the search.
        if (!query.isEmpty()) {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String searchQuery = "%" + query + "%";

            String rawQuery = "SELECT c." + DatabaseHelper.COLUMN_COURSE_ID + ", c." + DatabaseHelper.COLUMN_COURSE_TYPE +
                    ", c." + DatabaseHelper.COLUMN_COURSE_DAY_OF_WEEK +
                    ", i." + DatabaseHelper.COLUMN_INSTANCE_DATE + ", i." + DatabaseHelper.COLUMN_INSTANCE_TEACHER +
                    " FROM " + DatabaseHelper.TABLE_COURSES + " c" +
                    " JOIN " + DatabaseHelper.TABLE_INSTANCES + " i ON c." + DatabaseHelper.COLUMN_COURSE_ID + " = i." + DatabaseHelper.COLUMN_INSTANCE_COURSE_ID +
                    " WHERE i." + DatabaseHelper.COLUMN_INSTANCE_TEACHER + " LIKE ?" +
                    " OR i." + DatabaseHelper.COLUMN_INSTANCE_DATE + " LIKE ?" +
                    " OR c." + DatabaseHelper.COLUMN_COURSE_DAY_OF_WEEK + " LIKE ?";

            Cursor cursor = db.rawQuery(rawQuery, new String[]{searchQuery, searchQuery, searchQuery});

            while (cursor.moveToNext()) {
                @SuppressLint("Range") long courseId = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_ID));
                @SuppressLint("Range") String courseType = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_TYPE));
                @SuppressLint("Range") String dayOfWeek = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_DAY_OF_WEEK));
                @SuppressLint("Range") String instanceDate = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_INSTANCE_DATE));
                @SuppressLint("Range") String instanceTeacher = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_INSTANCE_TEACHER));

                newResults.add(new SearchResult(courseId, courseType, instanceDate, instanceTeacher, dayOfWeek));
            }
            cursor.close();
        }

        // Update the adapter with the new search results (or an empty list if the query is empty).
        searchAdapter.updateData(newResults);

        // Optional: Show a message if no results were found for a non-empty query.
        if (newResults.isEmpty() && !query.isEmpty()) {
            Toast.makeText(this, "No results found for '" + query + "'", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Callback from SearchAdapter when a result item is clicked.
     */
    @Override
    public void onResultClick(SearchResult result) {
        Intent intent = new Intent(this, ClassInstanceActivity.class);
        intent.putExtra("COURSE_ID", result.getCourseId());
        intent.putExtra("COURSE_DAY_OF_WEEK", result.getDayOfWeek());
        startActivity(intent);
    }

    /**
     * Handles clicks on menu items in the Toolbar (e.g., the back button).
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