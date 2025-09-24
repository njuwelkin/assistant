/**
 * MIT License
 * Copyright (c) 2023 illu@biubiu.org
 */
package com.example.assistant.ui.course.model;

public class Course {
    private String id;
    private String courseName;
    private String teacherName;
    private String startTime;
    private String endTime;
    private int dayOfWeek;
    private String classroom;
    private String weekRange;
    private String department;
    private String time; // 保留此字段以保持向后兼容

    // 默认构造函数，用于JSON解析
    public Course() {
    }

    // 现有构造函数，保持向后兼容
    public Course(String name, String department, String teacher, String time, String location) {
        this.courseName = name;
        this.department = department;
        this.teacherName = teacher;
        this.time = time;
        this.classroom = location;
    }

    // 新的构造函数，用于从API数据创建课程对象
    public Course(String id, String courseName, String teacherName, String startTime, String endTime,
                  int dayOfWeek, String classroom, String weekRange, String department) {
        this.id = id;
        this.courseName = courseName;
        this.teacherName = teacherName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.dayOfWeek = dayOfWeek;
        this.classroom = classroom;
        this.weekRange = weekRange;
        this.department = department;
        this.time = startTime + " - " + endTime;
    }

    // 以下是原有方法，保持向后兼容
    public String getName() {
        return courseName;
    }

    public void setName(String name) {
        this.courseName = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getTeacher() {
        return teacherName;
    }

    public void setTeacher(String teacher) {
        this.teacherName = teacher;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLocation() {
        return classroom;
    }

    public void setLocation(String location) {
        this.classroom = location;
    }

    // 以下是新添加的方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getClassroom() {
        return classroom;
    }

    public void setClassroom(String classroom) {
        this.classroom = classroom;
    }

    public String getWeekRange() {
        return weekRange;
    }

    public void setWeekRange(String weekRange) {
        this.weekRange = weekRange;
    }
}