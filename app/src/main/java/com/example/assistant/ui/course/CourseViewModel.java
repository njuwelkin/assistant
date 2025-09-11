package com.example.assistant.ui.course;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.util.Log;

import com.example.assistant.ui.course.model.Course;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CourseViewModel extends ViewModel {

    // 课程数据
    private final MutableLiveData<List<Course>> _courses = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Course>> getCourses() {
        return _courses;
    }
    
    // 作业数据（暂时为空）
    private final MutableLiveData<List<Object>> _assignments = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Object>> getAssignments() {
        return _assignments;
    }
    
    // 活动数据（暂时为空）
    private final MutableLiveData<List<Object>> _activities = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Object>> getActivities() {
        return _activities;
    }

    private Date selectedDate;

    public Date getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(Date date) {
        this.selectedDate = date;
    }

    // 加载指定日期的课程
    public void loadCourses(String date) {
        Log.d("CourseViewModel", "加载日期: " + date + " 的课程数据");
        // 这里应该是从API或数据库加载课程数据
        // 为了演示，我们创建一些模拟数据
        List<Course> courses = generateMockCourses(date);
        Log.d("CourseViewModel", "生成课程数据数量: " + courses.size());
        _courses.setValue(courses);
        Log.d("CourseViewModel", "课程数据已设置到LiveData");
    }

    // 生成模拟课程数据
    private List<Course> generateMockCourses(String date) {
        List<Course> courses = new ArrayList<>();
        
        // 获取日期中的日部分
        String dayOfMonth = date.substring(date.lastIndexOf('-') + 1);
        int day = Integer.parseInt(dayOfMonth);
        
        // 根据不同日期生成不同的课程数据，确保每天都有课程显示
        // 修改为小学五年级课程
        if (day % 3 == 1) {
            courses.add(new Course("语文", "小学五年级", "王老师", "上午 8:30-9:10", "五(1)班教室"));
            courses.add(new Course("数学", "小学五年级", "李老师", "上午 9:20-10:00", "五(1)班教室"));
            courses.add(new Course("英语", "小学五年级", "张老师", "下午 2:00-2:40", "五(1)班教室"));
            courses.add(new Course("科学", "小学五年级", "陈老师", "下午 2:50-3:30", "科学实验室"));
        } else if (day % 3 == 2) {
            courses.add(new Course("数学", "小学五年级", "李老师", "上午 8:30-9:10", "五(1)班教室"));
            courses.add(new Course("语文", "小学五年级", "王老师", "上午 9:20-10:00", "五(1)班教室"));
            courses.add(new Course("美术", "小学五年级", "刘老师", "下午 2:00-2:40", "美术室"));
            courses.add(new Course("体育", "小学五年级", "赵老师", "下午 2:50-3:30", "操场"));
        } else {
            courses.add(new Course("语文", "小学五年级", "王老师", "上午 8:30-9:10", "五(1)班教室"));
            courses.add(new Course("英语", "小学五年级", "张老师", "上午 9:20-10:00", "五(1)班教室"));
            courses.add(new Course("数学", "小学五年级", "李老师", "上午 10:20-11:00", "五(1)班教室"));
            courses.add(new Course("音乐", "小学五年级", "黄老师", "下午 2:00-2:40", "音乐室"));
            courses.add(new Course("道德与法治", "小学五年级", "吴老师", "下午 2:50-3:30", "五(1)班教室"));
        }
        
        return courses;
    }
}