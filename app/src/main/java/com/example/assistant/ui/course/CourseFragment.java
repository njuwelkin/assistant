/**
 * MIT License
 * Copyright (c) 2023 illu@biubiu.org
 */
package com.example.assistant.ui.course;

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
import androidx.viewpager2.widget.ViewPager2;

import com.example.assistant.databinding.FragmentCourseBinding;
import com.example.assistant.ui.course.adapter.CourseTabAdapter;
import com.example.assistant.ui.course.tab.CoursesFragment;
import com.example.assistant.ui.course.tab.AssignmentsFragment;
import com.example.assistant.ui.course.tab.ActivitiesFragment;
import com.example.assistant.R;
import com.example.assistant.ui.course.model.Course;
import com.example.assistant.ui.course.CourseViewModel;
import com.example.assistant.ui.course.model.Activity;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CourseFragment extends Fragment {

    private FragmentCourseBinding binding;
    private CourseViewModel courseViewModel;
    private CourseTabAdapter courseTabAdapter;
    private TabLayoutMediator tabLayoutMediator;
    private Calendar calendar;
    private int currentMonth;
    private int currentYear;
    private TextView[] dayTextViews;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCourseBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        courseViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getActivity().getApplication())).get(CourseViewModel.class);

        // 初始化日历相关变量
        calendar = Calendar.getInstance();
        currentMonth = calendar.get(Calendar.MONTH);
        currentYear = calendar.get(Calendar.YEAR);

        // 初始化日期文本视图数组
        dayTextViews = new TextView[42]; // 最大6行7列
        initDayTextViews();

        // 初始化标签页适配器和数据观察
        initOrResetTabAdapter();

        // 初始化UI
        updateCalendar();
        updateMonthYearDisplay();
        loadCoursesForCurrentDay();

        // 设置按钮点击监听器
        binding.prevMonthButton.setOnClickListener(v -> { onPrevMonth(); });
        binding.nextMonthButton.setOnClickListener(v -> { onNextMonth(); });



        return root;
    }

    private void initDayTextViews() {
        // 初始化42个日期文本视图
        int[] ids = new int[] {
                R.id.day1, R.id.day2, R.id.day3, R.id.day4, R.id.day5, R.id.day6, R.id.day7,
                R.id.day8, R.id.day9, R.id.day10, R.id.day11, R.id.day12, R.id.day13, R.id.day14,
                R.id.day15, R.id.day16, R.id.day17, R.id.day18, R.id.day19, R.id.day20, R.id.day21,
                R.id.day22, R.id.day23, R.id.day24, R.id.day25, R.id.day26, R.id.day27, R.id.day28,
                R.id.day29, R.id.day30, R.id.day31, R.id.day32, R.id.day33, R.id.day34, R.id.day35,
                R.id.day36, R.id.day37, R.id.day38, R.id.day39, R.id.day40, R.id.day41, R.id.day42
        };

        for (int i = 0; i < ids.length; i++) {
            dayTextViews[i] = binding.getRoot().findViewById(ids[i]);
            final int position = i;
            dayTextViews[i].setOnClickListener(v -> {
                // 检查文本是否为空
                if (!dayTextViews[position].getText().toString().trim().isEmpty()) {
                    int day = Integer.parseInt(dayTextViews[position].getText().toString());
                    onDaySelected(day);
                }
            });
        }
    }

    /**
     * 初始化或重置标签页适配器，重新创建所有Fragment
     */
    private void initOrResetTabAdapter() {
        ViewPager2 viewPager = binding.viewPager;
        TabLayout tabLayout = binding.tabLayout;
        
        // 移除之前的TabLayoutMediator连接
        if (tabLayoutMediator != null) {
            tabLayoutMediator.detach();
        }
        
        // 重新创建标签页适配器，这会创建新的Fragment实例
        courseTabAdapter = new CourseTabAdapter(getActivity());
        viewPager.setAdapter(courseTabAdapter);
        
        // 重新连接TabLayout和ViewPager2
        tabLayoutMediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            // 设置标签文本
            switch (position) {
                case 0:
                    tab.setText("课程");
                    break;
                case 1:
                    tab.setText("作业");
                    break;
                case 2:
                    tab.setText("活动");
                    break;
            }
        });
        tabLayoutMediator.attach();
        
        // 重新观察数据变化并传递给新的Fragment
        setupDataObservers();
    }
    
    /**
     * 设置数据观察者，将数据传递给标签页Fragment
     */
    private void setupDataObservers() {
        // 监听课程数据变化，将数据传递给课程标签页Fragment
        courseViewModel.getCourses().observe(getViewLifecycleOwner(), courses -> {
            Log.d("CourseFragment", "接收到课程数据更新，数量: " + (courses != null ? courses.size() : 0));
            if (courseTabAdapter != null) {
                CoursesFragment coursesFragment = courseTabAdapter.getCoursesFragment();
                Log.d("CourseFragment", "CoursesFragment实例: " + (coursesFragment != null ? "不为空" : "为空"));
                if (coursesFragment != null) {
                    coursesFragment.setCourses(courses);
                }
            }
        });
        
        // 监听作业数据变化，将数据传递给作业标签页Fragment
        courseViewModel.getAssignments().observe(getViewLifecycleOwner(), assignments -> {
            Log.d("CourseFragment", "接收到作业数据更新，数量: " + (assignments != null ? assignments.size() : 0));
            if (courseTabAdapter != null) {
                AssignmentsFragment assignmentsFragment = courseTabAdapter.getAssignmentsFragment();
                Log.d("CourseFragment", "AssignmentsFragment实例: " + (assignmentsFragment != null ? "不为空" : "为空"));
                if (assignmentsFragment != null) {
                    assignmentsFragment.setAssignments(assignments);
                }
            }
        });
        
        // 监听活动数据变化，将数据传递给活动标签页Fragment
        courseViewModel.getActivities().observe(getViewLifecycleOwner(), activities -> {
            Log.d("CourseFragment", "接收到活动数据更新，数量: " + (activities != null ? activities.size() : 0));
            if (courseTabAdapter != null) {
                ActivitiesFragment activitiesFragment = courseTabAdapter.getActivitiesFragment();
                if (activitiesFragment != null) {
                    activitiesFragment.setActivities(activities);
                }
            }
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d("CourseFragment", "onResume: 页面重新可见");
        
        // 当从其他页面切换回来时，重新创建TabAdapter以重新创建CoursesFragment
        // 注意：这会导致所有标签页的Fragment都被重新创建
        initOrResetTabAdapter();
    }
    
    private void updateCalendar() {
        // 清空所有日期文本视图
        for (TextView textView : dayTextViews) {
            textView.setText("");
            textView.setBackgroundResource(0);
            textView.setTextColor(getResources().getColor(R.color.black));
        }

        // 获取当月第一天是星期几
        Calendar tempCalendar = (Calendar) calendar.clone();
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK) - 1; // 转为0-6，0表示星期日

        // 获取当月的天数
        int daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // 填充当月日期
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date today = new Date();
        String todayStr = dateFormat.format(today);

        for (int i = 0; i < daysInMonth; i++) {
            int position = firstDayOfWeek + i;
            if (position < dayTextViews.length) {
                dayTextViews[position].setText(String.valueOf(i + 1));
                
                // 检查是否是今天
                tempCalendar.set(Calendar.DAY_OF_MONTH, i + 1);
                String currentDateStr = dateFormat.format(tempCalendar.getTime());
                if (currentDateStr.equals(todayStr)) {
                    dayTextViews[position].setBackgroundResource(R.drawable.circle_today);
                    dayTextViews[position].setTextColor(getResources().getColor(R.color.white));
                }
            }
        }

        // 高亮选中的日期（如果有）
        if (courseViewModel.getSelectedDate() != null) {
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.setTime(courseViewModel.getSelectedDate());
            if (selectedCalendar.get(Calendar.MONTH) == currentMonth && 
                selectedCalendar.get(Calendar.YEAR) == currentYear) {
                int dayOfMonth = selectedCalendar.get(Calendar.DAY_OF_MONTH);
                int position = firstDayOfWeek + dayOfMonth - 1;
                if (position < dayTextViews.length) {
                    dayTextViews[position].setBackgroundResource(R.drawable.circle_selected);
                    dayTextViews[position].setTextColor(getResources().getColor(R.color.white));
                }
            }
        }
    }

    private void updateMonthYearDisplay() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy年MM月", Locale.getDefault());
        binding.monthYearTextView.setText(monthFormat.format(calendar.getTime()));
    }

    private void onPrevMonth() {
        calendar.add(Calendar.MONTH, -1);
        currentMonth = calendar.get(Calendar.MONTH);
        currentYear = calendar.get(Calendar.YEAR);
        updateCalendar();
        updateMonthYearDisplay();
    }

    private void onNextMonth() {
        calendar.add(Calendar.MONTH, 1);
        currentMonth = calendar.get(Calendar.MONTH);
        currentYear = calendar.get(Calendar.YEAR);
        updateCalendar();
        updateMonthYearDisplay();
    }

    private void onDaySelected(int day) {
        Calendar selectedCalendar = (Calendar) calendar.clone();
        selectedCalendar.set(Calendar.DAY_OF_MONTH, day);
        courseViewModel.setSelectedDate(selectedCalendar.getTime());
        loadCoursesForDate(selectedCalendar.getTime());
        updateCalendar(); // 刷新日历以高亮选中的日期
    }

    private void loadCoursesForCurrentDay() {
        Calendar today = Calendar.getInstance();
        courseViewModel.setSelectedDate(today.getTime());
        loadCoursesForDate(today.getTime());
    }

    private void loadCoursesForDate(Date date) {
        // 加载指定日期的课程和作业
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateStr = dateFormat.format(date);
        courseViewModel.loadCourses(dateStr);
        // 当ViewModel中加载课程时，会自动同时加载对应的作业数据
        Log.d("CourseFragment", "已加载日期: " + dateStr + " 的课程和作业数据");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}