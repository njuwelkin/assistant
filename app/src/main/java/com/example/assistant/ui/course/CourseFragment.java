package com.example.assistant.ui.course;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assistant.R;
import com.example.assistant.databinding.FragmentDashboardBinding;
import com.example.assistant.databinding.FragmentCourseBinding;
import com.example.assistant.ui.course.adapter.CourseAdapter;
import com.example.assistant.ui.course.model.Course;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CourseFragment extends Fragment {

    private FragmentCourseBinding binding;
    private CourseViewModel courseViewModel;
    private CourseAdapter courseAdapter;
    private Calendar calendar;
    private int currentMonth;
    private int currentYear;
    private TextView[] dayTextViews;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCourseBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        courseViewModel = new ViewModelProvider(this).get(CourseViewModel.class);

        // 初始化日历相关变量
        calendar = Calendar.getInstance();
        currentMonth = calendar.get(Calendar.MONTH);
        currentYear = calendar.get(Calendar.YEAR);

        // 初始化日期文本视图数组
        dayTextViews = new TextView[42]; // 最大6行7列
        initDayTextViews();

        // 设置RecyclerView
        RecyclerView recyclerView = binding.courseRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        courseAdapter = new CourseAdapter();
        recyclerView.setAdapter(courseAdapter);

        // 初始化UI
        updateCalendar();
        updateMonthYearDisplay();
        loadCoursesForCurrentDay();

        // 设置按钮点击监听器
        binding.prevMonthButton.setOnClickListener(v -> { onPrevMonth(); });
        binding.nextMonthButton.setOnClickListener(v -> { onNextMonth(); });

        // 监听课程数据变化
        courseViewModel.getCourses().observe(getViewLifecycleOwner(), courses -> {
            courseAdapter.setCourses(courses);
        });

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
        // 加载指定日期的课程
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateStr = dateFormat.format(date);
        courseViewModel.loadCourses(dateStr);
        
        // 更新选中日期显示
        binding.selectedDateTextView.setText(dateFormat.format(date));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}