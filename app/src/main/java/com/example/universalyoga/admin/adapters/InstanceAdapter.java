package com.example.universalyoga.admin.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // Import ImageView
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.universalyoga.admin.R;
import com.example.universalyoga.admin.models.ClassInstance;
import java.util.List;

/**
 * Adapter for the class instances RecyclerView.
 * It connects the list of ClassInstance data to the UI.
 */
public class InstanceAdapter extends RecyclerView.Adapter<InstanceAdapter.InstanceViewHolder> {

    private final List<ClassInstance> instanceList;
    private final OnInstanceInteractionListener listener;

    /**
     * Interface for handling clicks and other interactions on items.
     * The hosting Activity must implement this interface.
     */
    public interface OnInstanceInteractionListener {
        void onInstanceClick(ClassInstance instance);
        void onInstanceDelete(long instanceId);
    }

    public InstanceAdapter(List<ClassInstance> instanceList, OnInstanceInteractionListener listener) {
        this.instanceList = instanceList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public InstanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single row.
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_instance, parent, false);
        return new InstanceViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull InstanceViewHolder holder, int position) {
        // Get the data for the current position and bind it to the ViewHolder.
        ClassInstance currentInstance = instanceList.get(position);
        holder.bind(currentInstance, listener);
    }

    @Override
    public int getItemCount() {
        // Return the total number of items in the list.
        return instanceList.size();
    }

    /**
     * ViewHolder for a single class instance item.
     * It holds references to the UI views for a single row.
     */
    static class InstanceViewHolder extends RecyclerView.ViewHolder {
        LinearLayout infoLayout;
        TextView textViewDate, textViewTeacher;
        ImageView imageViewDelete; // CORRECTED: Changed from TextView to ImageView

        public InstanceViewHolder(@NonNull View itemView) {
            super(itemView);
            // Find views by their ID.
            infoLayout = itemView.findViewById(R.id.instance_info_layout);
            textViewDate = itemView.findViewById(R.id.text_view_instance_date);
            textViewTeacher = itemView.findViewById(R.id.text_view_instance_teacher);
            // CORRECTED: Reference the ImageView with the correct ID.
            imageViewDelete = itemView.findViewById(R.id.image_view_instance_delete);
        }

        /**
         * Binds data from a ClassInstance object to the views and sets click listeners.
         * @param instance The data object for the current row.
         * @param listener The listener to handle interaction events.
         */
        public void bind(final ClassInstance instance, final OnInstanceInteractionListener listener) {
            // Set the data to the TextViews.
            textViewDate.setText(instance.getDate());
            textViewTeacher.setText("Teacher: " + instance.getTeacher());

            // Set click listener for the main info area (for editing).
            infoLayout.setOnClickListener(v -> listener.onInstanceClick(instance));
            // Set click listener for the delete icon.
            imageViewDelete.setOnClickListener(v -> listener.onInstanceDelete(instance.getId()));
        }
    }
}