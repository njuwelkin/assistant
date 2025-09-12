package com.example.assistant.ui.course.tab;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assistant.R;
import com.example.assistant.databinding.FragmentActivitiesBinding;
import com.example.assistant.ui.course.CourseViewModel;
import com.example.assistant.ui.course.adapter.ActivityAdapter;
import com.example.assistant.ui.course.model.Activity;

import java.util.ArrayList;
import java.util.List;

public class ActivitiesFragment extends Fragment {

    private FragmentActivitiesBinding binding;
    private CourseViewModel courseViewModel;
    private ActivityAdapter activityAdapter;
    // 用于保存当视图未创建时传入的活动数据
    private List<Activity> pendingActivities = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentActivitiesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 初始化RecyclerView
        RecyclerView recyclerView = binding.activitiesRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        activityAdapter = new ActivityAdapter();
        recyclerView.setAdapter(activityAdapter);

        // 初始化ViewModel
        courseViewModel = new ViewModelProvider(requireActivity()).get(CourseViewModel.class);

        return root;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 检查是否有等待处理的活动数据
        if (pendingActivities != null) {
            Log.d("ActivitiesFragment", "视图创建完成，处理pendingActivities中的活动数据");
            setActivities(pendingActivities);
            pendingActivities = null; // 清空已处理的数据
        }
        
        // 确保ViewModel已初始化
        if (courseViewModel != null) {
            // 观察活动数据变化
            courseViewModel.getActivities().observe(getViewLifecycleOwner(), activities -> {
                Log.d("ActivitiesFragment", "观察到活动数据更新");
                
                // 处理null情况
                if (activities == null) {
                    Log.d("ActivitiesFragment", "活动数据为null，使用空列表");
                    updateActivitiesList(new ArrayList<>());
                } else {
                    updateActivitiesList(activities);
                }
            });
            
            // 尝试获取当前已有的活动数据（如果有）
            List<Activity> currentActivities = courseViewModel.getActivities().getValue();
            if (currentActivities != null && !currentActivities.isEmpty()) {
                updateActivitiesList(currentActivities);
            }
        } else {
            Log.e("ActivitiesFragment", "ViewModel未初始化，无法观察数据变化");
        }
    }

    // 更新活动列表
    private void updateActivitiesList(List<Activity> activities) {
        Log.d("ActivitiesFragment", "接收到活动数据数量: " + activities.size());
        
        // 添加activityAdapter的null检查
        if (activityAdapter != null) {
            activityAdapter.setActivities(activities);
        } else {
            Log.w("ActivitiesFragment", "activityAdapter为null，无法更新活动列表");
        }
        
        // 添加binding的null检查
        if (binding != null) {
            // 根据活动列表是否为空，显示或隐藏空提示文本
            if (activities.isEmpty()) {
                binding.emptyText.setVisibility(View.VISIBLE);
                binding.activitiesRecyclerView.setVisibility(View.GONE);
            } else {
                binding.emptyText.setVisibility(View.GONE);
                binding.activitiesRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }
    
    // 公共方法，供外部直接设置活动数据
    public void setActivities(List<Activity> activities) {
        Log.d("ActivitiesFragment", "通过公共方法设置活动数据: " + (activities != null ? activities.size() : 0) + " 个活动");
        
        // 检查视图是否已创建（binding和activityAdapter是否已初始化）
        if (binding != null && activityAdapter != null) {
            // 视图已创建，直接更新
            if (activities == null) {
                updateActivitiesList(new ArrayList<>());
            } else {
                updateActivitiesList(activities);
            }
        } else {
            // 视图未创建，保存数据以供后续使用
            Log.d("ActivitiesFragment", "视图未创建，保存活动数据到pendingActivities");
            pendingActivities = activities;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}