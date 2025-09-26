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
import java.text.SimpleDateFormat;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assistant.R;
import com.example.assistant.databinding.FragmentCoursesBinding;
import com.example.assistant.ui.course.adapter.CourseAdapter;
import com.example.assistant.ui.course.CourseViewModel;
import com.example.assistant.ui.course.model.Course;
import com.example.assistant.ui.course.model.Assignment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CoursesFragment extends Fragment {

    private FragmentCoursesBinding binding;
    private CourseAdapter courseAdapter;
    private CourseViewModel courseViewModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 观察课程数据变化
        courseViewModel.getCourses().observe(getViewLifecycleOwner(), courses -> {
            Log.d("CoursesFragment", "观察到课程数据更新，数量: " + (courses != null ? courses.size() : 0));
            
            // 处理空数据情况
            if (courses == null || courses.isEmpty()) {
                Log.d("CoursesFragment", "课程数据为空，显示空状态");
                binding.emptyCoursesText.setVisibility(View.VISIBLE);
                binding.courseRecyclerView.setVisibility(View.GONE);
            } else {
                // 有课程数据，显示列表并隐藏空状态
                binding.emptyCoursesText.setVisibility(View.GONE);
                binding.courseRecyclerView.setVisibility(View.VISIBLE);
                setCourses(courses);
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("CoursesFragment", "onCreateView 开始");
        binding = FragmentCoursesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 初始化ViewModel
        courseViewModel = new ViewModelProvider(requireActivity()).get(CourseViewModel.class);
        Log.d("CoursesFragment", "ViewModel初始化完成: " + (courseViewModel != null ? "成功" : "失败"));
        
        // 设置RecyclerView
        RecyclerView recyclerView = binding.courseRecyclerView;
        Log.d("CoursesFragment", "RecyclerView: " + (recyclerView != null ? "不为空" : "为空"));
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        Log.d("CoursesFragment", "设置LayoutManager完成");
        
        courseAdapter = new CourseAdapter();
        Log.d("CoursesFragment", "创建CourseAdapter完成");
        
        recyclerView.setAdapter(courseAdapter);
        Log.d("CoursesFragment", "设置Adapter完成");

        // 加载当日课程
        loadTodayCourses();
        Log.d("CoursesFragment", "loadTodayCourses 调用完成");

        return root;
    }

    private void loadTodayCourses() {
        Log.d("CoursesFragment", "开始加载今日课程数据");
        
        // 检查courseViewModel是否初始化成功
        if (courseViewModel == null) {
            Log.e("CoursesFragment", "courseViewModel未初始化，无法加载课程数据");
            
            // 如果ViewModel未初始化，使用默认数据作为后备
            List<Course> defaultCourses = new ArrayList<>();
            setCourses(defaultCourses);
            return;
        }
        
        // 检查ViewModel中是否已有课程数据
        List<Course> existingCourses = courseViewModel.getCourses().getValue();
        
        if (existingCourses != null && !existingCourses.isEmpty()) {
            Log.d("CoursesFragment", "ViewModel中已有课程数据，数量: " + existingCourses.size());
            setCourses(existingCourses);
        } else {
            Log.d("CoursesFragment", "ViewModel中暂无数据，尝试加载");
            
            // 主动从ViewModel加载当日课程数据
            try {
                if (courseViewModel.getSelectedDate() != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String dateStr = dateFormat.format(courseViewModel.getSelectedDate());
                    Log.d("CoursesFragment", "主动加载日期: " + dateStr + " 的课程数据");
                    courseViewModel.loadCourses(dateStr);
                } else {
                    Log.d("CoursesFragment", "未选择日期，尝试加载默认日期课程");
                    courseViewModel.loadCourses(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date()));
                }
                
                // 再次检查是否加载成功
                existingCourses = courseViewModel.getCourses().getValue();
                if (existingCourses != null && !existingCourses.isEmpty()) {
                    Log.d("CoursesFragment", "加载成功，获取到课程数据，数量: " + existingCourses.size());
                    setCourses(existingCourses);
                    return;
                }
            } catch (Exception e) {
                Log.e("CoursesFragment", "加载课程数据出错: " + e.getMessage());
            }
            
            // 如果加载失败，使用默认数据作为后备
            Log.d("CoursesFragment", "加载失败，使用默认课程数据");
            List<Course> defaultCourses = new ArrayList<>();
            //defaultCourses.add(new Course("默认课程1", "计算机学院", "张教授", "上午 8:00-10:00", "A栋101"));
            //defaultCourses.add(new Course("默认课程2", "计算机学院", "李教授", "下午 2:00-4:00", "B栋202"));
            setCourses(defaultCourses);
        }
    }

    public void setCourses(List<Course> courses) {
        Log.d("CoursesFragment", "设置课程数据: " + (courses != null ? courses.size() : 0) + " 门课程");
        if (courseAdapter != null) {
            courseAdapter.setCourses(courses);
        } else {
            Log.e("CoursesFragment", "courseAdapter 为空，无法设置课程数据");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // 空实现
    }

    @Override
    public void onResume() {
        super.onResume();
        // 空实现
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        courseAdapter = null; // 同时将courseAdapter也置为null，确保重建时正确初始化
    }
}