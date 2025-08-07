package com.example.universalyoga.admin.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.universalyoga.admin.R;
import com.example.universalyoga.admin.models.Course;
import java.util.ArrayList;
import java.util.List;

/**
 * This adapter acts as a bridge between the data (a list of courses)
 * and the UI (the RecyclerView on the main screen).
 */
public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    // The list of courses to be displayed.
    private final List<Course> courseList;
    // The listener to handle clicks on items.
    private final OnItemInteractionListener listener;

    /**
     * An interface that the hosting Activity (MainActivity) must implement
     * to receive click and delete events from the adapter.
     */
    public interface OnItemInteractionListener {
        void onItemDelete(long courseId);
        void onItemClick(Course course);
        void onItemEdit(Course course);
    }

    /**
     * Constructor for the CourseAdapter.
     * @param courseList The initial list of courses.
     * @param listener The activity that will handle the interaction events.
     */
    public CourseAdapter(List<Course> courseList, OnItemInteractionListener listener) {
        this.courseList = courseList;
        this.listener = listener;
    }

    /**
     * A method to update the adapter's data set and refresh the RecyclerView.
     * @param newCourses The new list of courses to display.
     */
    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<Course> newCourses) {
        this.courseList.clear();
        this.courseList.addAll(newCourses);
        notifyDataSetChanged();
    }

    /**
     * Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
     */
    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single list item.
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_course, parent, false);
        return new CourseViewHolder(itemView);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method updates the contents of the ViewHolder to reflect the item at the given position.
     */
    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course currentCourse = courseList.get(position);
        holder.bind(currentCourse, listener);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     */
    @Override
    public int getItemCount() {
        return courseList.size();
    }

    /**
     * A ViewHolder describes an item view and metadata about its place within the RecyclerView.
     */
    static class CourseViewHolder extends RecyclerView.ViewHolder {
        // UI elements for a single list item.
        LinearLayout infoLayout;
        TextView textViewCourseType, textViewCourseDayTime;
        // Changed from TextView to ImageView
        ImageView imageViewEdit, imageViewDelete;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            // Find the views by their ID from the layout file.
            infoLayout = itemView.findViewById(R.id.course_info_layout);
            textViewCourseType = itemView.findViewById(R.id.text_view_course_type);
            textViewCourseDayTime = itemView.findViewById(R.id.text_view_course_day_time);
            // Correctly reference the ImageViews
            imageViewEdit = itemView.findViewById(R.id.image_view_edit);
            imageViewDelete = itemView.findViewById(R.id.image_view_delete);
        }

        /**
         * Binds a Course object to the ViewHolder and sets up click listeners.
         * @param course The course data to display.
         * @param listener The listener to notify of user interactions.
         */
        public void bind(final Course course, final OnItemInteractionListener listener) {
            // Set the text for the course details.
            textViewCourseType.setText(course.getType());
            String dayTime = course.getDayOfWeek() + ", " + course.getTime();
            textViewCourseDayTime.setText(dayTime);

            // Set click listeners.
            // Click on the main info area to view instances.
            infoLayout.setOnClickListener(v -> listener.onItemClick(course));
            // Click on the edit icon to edit the course.
            imageViewEdit.setOnClickListener(v -> listener.onItemEdit(course));
            // Click on the delete icon to delete the course.
            imageViewDelete.setOnClickListener(v -> listener.onItemDelete(course.getId()));
        }
    }
}