package com.example.assistant.ui.course.model;

public class Course {
    private String name;
    private String department;
    private String teacher;
    private String time;
    private String location;

    public Course(String name, String department, String teacher, String time, String location) {
        this.name = name;
        this.department = department;
        this.teacher = teacher;
        this.time = time;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}