/**
 * MIT License
 * Copyright (c) 2023 illu@biubiu.org
 */
package com.example.assistant.ui.course.tab;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assistant.R;
import com.example.assistant.databinding.FragmentAssignmentsBinding;
import com.example.assistant.ui.course.CourseViewModel;
import com.example.assistant.ui.course.adapter.AssignmentAdapter;
import com.example.assistant.ui.course.model.Assignment;

import java.util.ArrayList;
import java.util.List;

public class AssignmentsFragment extends Fragment {

    private FragmentAssignmentsBinding binding;
    private CourseViewModel courseViewModel;
    private AssignmentAdapter assignmentAdapter;
    // 用于保存当视图未创建时传入的作业数据
    private List<Assignment> pendingAssignments = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAssignmentsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 初始化RecyclerView
        RecyclerView recyclerView = binding.assignmentsRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        assignmentAdapter = new AssignmentAdapter();
        recyclerView.setAdapter(assignmentAdapter);

        // 初始化ViewModel
        courseViewModel = new ViewModelProvider(requireActivity()).get(CourseViewModel.class);

        return root;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 检查是否有等待处理的作业数据
        if (pendingAssignments != null) {
            Log.d("AssignmentsFragment", "视图创建完成，处理pendingAssignments中的作业数据");
            setAssignments(pendingAssignments);
            pendingAssignments = null; // 清空已处理的数据
        }
        
        // 确保ViewModel已初始化
        if (courseViewModel != null) {
            // 观察作业数据变化
            courseViewModel.getAssignments().observe(getViewLifecycleOwner(), assignments -> {
                Log.d("AssignmentsFragment", "观察到作业数据更新");
                
                // 处理null情况
                if (assignments == null) {
                    Log.d("AssignmentsFragment", "作业数据为null，使用空列表");
                    updateAssignmentsList(new ArrayList<>());
                } else {
                    updateAssignmentsList(assignments);
                }
            });
            
            // 尝试获取当前已有的作业数据（如果有）
            List<Assignment> currentAssignments = courseViewModel.getAssignments().getValue();
            if (currentAssignments != null && !currentAssignments.isEmpty()) {
                updateAssignmentsList(currentAssignments);
            }
        } else {
            Log.e("AssignmentsFragment", "ViewModel未初始化，无法观察数据变化");
        }
    }

    // 更新作业列表
    private void updateAssignmentsList(List<Assignment> assignments) {
        Log.d("AssignmentsFragment", "接收到作业数据数量: " + assignments.size());
        
        // 添加assignmentAdapter的null检查
        if (assignmentAdapter != null) {
            assignmentAdapter.setAssignments(assignments);
        } else {
            Log.w("AssignmentsFragment", "assignmentAdapter为null，无法更新作业列表");
        }
        
        // 添加binding的null检查
        if (binding != null) {
            // 根据作业列表是否为空，显示或隐藏空提示文本
            if (assignments.isEmpty()) {
                binding.emptyText.setVisibility(View.VISIBLE);
                binding.assignmentsRecyclerView.setVisibility(View.GONE);
            } else {
                binding.emptyText.setVisibility(View.GONE);
                binding.assignmentsRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }
    
    // 公共方法，供外部直接设置作业数据
    public void setAssignments(List<Assignment> assignments) {
        Log.d("AssignmentsFragment", "通过公共方法设置作业数据: " + (assignments != null ? assignments.size() : 0) + " 个作业");
        
        // 检查视图是否已创建（binding和assignmentAdapter是否已初始化）
        if (binding != null && assignmentAdapter != null) {
            // 视图已创建，直接更新
            if (assignments == null) {
                updateAssignmentsList(new ArrayList<>());
            } else {
                updateAssignmentsList(assignments);
            }
        } else {
            // 视图未创建，保存数据以供后续使用
            Log.d("AssignmentsFragment", "视图未创建，保存作业数据到pendingAssignments");
            pendingAssignments = assignments;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}