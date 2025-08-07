package com.example.universalyoga.admin.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.universalyoga.admin.R;
import com.example.universalyoga.admin.models.SearchResult;
import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchResultViewHolder> {

    private final List<SearchResult> resultList;
    private final OnSearchResultClickListener listener;

    public interface OnSearchResultClickListener {
        void onResultClick(SearchResult result);
    }

    public SearchAdapter(List<SearchResult> resultList, OnSearchResultClickListener listener) {
        this.resultList = resultList;
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<SearchResult> newResults) {
        resultList.clear();
        resultList.addAll(newResults);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_search_result, parent, false);
        return new SearchResultViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
        SearchResult currentResult = resultList.get(position);
        holder.bind(currentResult, listener);
    }

    @Override
    public int getItemCount() {
        return resultList.size();
    }

    static class SearchResultViewHolder extends RecyclerView.ViewHolder {
        TextView textViewCourseType, textViewInstanceDate, textViewTeacher;

        public SearchResultViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewCourseType = itemView.findViewById(R.id.text_view_search_course_type);
            textViewInstanceDate = itemView.findViewById(R.id.text_view_search_instance_date);
            textViewTeacher = itemView.findViewById(R.id.text_view_search_teacher);
        }

        public void bind(final SearchResult result, final OnSearchResultClickListener listener) {
            textViewCourseType.setText(result.getCourseType() + " (" + result.getDayOfWeek() + ")");
            textViewInstanceDate.setText("Date: " + result.getInstanceDate());
            textViewTeacher.setText("Teacher: " + result.getInstanceTeacher());

            itemView.setOnClickListener(v -> listener.onResultClick(result));
        }
    }
}