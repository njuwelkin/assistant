/**
 * MIT License
 * Copyright (c) 2023 illu@biubiu.org
 */
package com.example.assistant.ui.course.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assistant.R;
import com.example.assistant.ui.course.model.Assignment;

import java.util.ArrayList;
import java.util.List;

public class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.AssignmentViewHolder> {

    private List<Assignment> assignments = new ArrayList<>();

    @NonNull
    @Override
    public AssignmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_assignment, parent, false);
        return new AssignmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AssignmentViewHolder holder, int position) {
        Assignment assignment = assignments.get(position);
        holder.courseNameTextView.setText(assignment.getCourseName());
        holder.titleTextView.setText(assignment.getTitle());
        holder.descriptionTextView.setText(assignment.getDescription());
        holder.dueDateTextView.setText("截止日期: " + assignment.getDueDate());
        holder.completedCheckBox.setChecked(assignment.isCompleted());
    }

    @Override
    public int getItemCount() {
        return assignments.size();
    }

    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments;
        notifyDataSetChanged();
    }

    static class AssignmentViewHolder extends RecyclerView.ViewHolder {
        TextView courseNameTextView;
        TextView titleTextView;
        TextView descriptionTextView;
        TextView dueDateTextView;
        CheckBox completedCheckBox;

        public AssignmentViewHolder(@NonNull View itemView) {
            super(itemView);
            courseNameTextView = itemView.findViewById(R.id.assignment_course_name_text_view);
            titleTextView = itemView.findViewById(R.id.assignment_title_text_view);
            descriptionTextView = itemView.findViewById(R.id.assignment_description_text_view);
            dueDateTextView = itemView.findViewById(R.id.assignment_due_date_text_view);
            completedCheckBox = itemView.findViewById(R.id.assignment_completed_checkbox);
        }
    }
}