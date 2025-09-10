package com.example.assistant.ui.course;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.assistant.ui.course.model.Course;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CourseViewModel extends ViewModel {

    private final MutableLiveData<List<Course>> _courses = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Course>> getCourses() {
        return _courses;
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
        // 这里应该是从API或数据库加载课程数据
        // 为了演示，我们创建一些模拟数据
        List<Course> courses = generateMockCourses(date);
        _courses.setValue(courses);
    }

    // 生成模拟课程数据
    private List<Course> generateMockCourses(String date) {
        List<Course> courses = new ArrayList<>();
        
        // 根据不同日期生成不同的课程数据
        if (date.contains("-01-01")) {
            // 1月1日的课程
            courses.add(new Course("数据结构", "计算机学院", "张三", "上午 8:00-10:00", "A栋101"));
            courses.add(new Course("操作系统", "计算机学院", "李四", "下午 2:00-4:00", "B栋202"));
        } else if (date.contains("-01-02")) {
            // 1月2日的课程
            courses.add(new Course("计算机网络", "计算机学院", "王五", "上午 10:00-12:00", "C栋303"));
            courses.add(new Course("软件工程", "计算机学院", "赵六", "下午 4:00-6:00", "D栋404"));
        } else if (date.contains("-01-03")) {
            // 1月3日的课程
            courses.add(new Course("数据库系统", "计算机学院", "钱七", "上午 8:00-10:00", "A栋101"));
            courses.add(new Course("人工智能导论", "计算机学院", "孙八", "下午 2:00-4:00", "B栋202"));
            courses.add(new Course("编译原理", "计算机学院", "周九", "晚上 7:00-9:00", "E栋505"));
        } else {
            // 默认课程
            courses.add(new Course("高等数学", "理学院", "吴老师", "上午 8:00-10:00", "A栋101"));
            courses.add(new Course("大学物理", "理学院", "郑老师", "下午 2:00-4:00", "B栋202"));
        }
        
        return courses;
    }
}