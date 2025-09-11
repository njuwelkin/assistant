package com.example.assistant.ui.course.adapter;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.assistant.ui.course.tab.ActivitiesFragment;
import com.example.assistant.ui.course.tab.AssignmentsFragment;
import com.example.assistant.ui.course.tab.CoursesFragment;

public class CourseTabAdapter extends FragmentStateAdapter {

    private final CoursesFragment coursesFragment;
    private final AssignmentsFragment assignmentsFragment;
    private final ActivitiesFragment activitiesFragment;

    public CourseTabAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        // 初始化各个标签页的Fragment
        Log.d("CourseTabAdapter", "创建CoursesFragment实例");
        coursesFragment = new CoursesFragment();
        assignmentsFragment = new AssignmentsFragment();
        activitiesFragment = new ActivitiesFragment();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // 根据位置返回对应的Fragment
        switch (position) {
            case 0:
                return coursesFragment;
            case 1:
                return assignmentsFragment;
            case 2:
                return activitiesFragment;
            default:
                return coursesFragment;
        }
    }

    @Override
    public int getItemCount() {
        // 返回标签页数量
        return 3;
    }

    // 获取课程标签页Fragment，用于外部更新数据
    public CoursesFragment getCoursesFragment() {
        Log.d("CourseTabAdapter", "返回CoursesFragment实例: " + (coursesFragment != null ? "不为空" : "为空"));
        return coursesFragment;
    }
    
    // 获取作业标签页Fragment，用于外部更新数据
    public AssignmentsFragment getAssignmentsFragment() {
        Log.d("CourseTabAdapter", "返回AssignmentsFragment实例: " + (assignmentsFragment != null ? "不为空" : "为空"));
        return assignmentsFragment;
    }
}