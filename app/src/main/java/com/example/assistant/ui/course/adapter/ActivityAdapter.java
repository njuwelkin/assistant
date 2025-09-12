package com.example.assistant.ui.course.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.assistant.R;
import com.example.assistant.ui.course.model.Activity;
import java.util.ArrayList;
import java.util.List;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {

    private List<Activity> activities = new ArrayList<>();

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        Activity activity = activities.get(position);
        holder.titleTextView.setText(activity.getTitle());
        holder.descriptionTextView.setText(activity.getDescription());
        holder.timeTextView.setText(activity.getTime());
        holder.locationTextView.setText(activity.getLocation());
        holder.statusTextView.setText(activity.isAttended() ? "已参加" : "未参加");
        holder.statusTextView.setTextColor(activity.isAttended() ? 
                holder.itemView.getContext().getResources().getColor(R.color.green) : 
                holder.itemView.getContext().getResources().getColor(R.color.gray));
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    public void setActivities(List<Activity> activities) {
        this.activities = activities != null ? activities : new ArrayList<>();
        notifyDataSetChanged();
    }

    public static class ActivityViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTextView;
        public TextView descriptionTextView;
        public TextView timeTextView;
        public TextView locationTextView;
        public TextView statusTextView;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.activity_title);
            descriptionTextView = itemView.findViewById(R.id.activity_description);
            timeTextView = itemView.findViewById(R.id.activity_time);
            locationTextView = itemView.findViewById(R.id.activity_location);
            statusTextView = itemView.findViewById(R.id.activity_status);
        }
    }
}