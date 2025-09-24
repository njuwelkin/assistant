/**
 * MIT License
 * Copyright (c) 2023 illu@biubiu.org
 */
package com.example.assistant.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TimePeriod implements Serializable {
    private String id;
    private String name;
    private String startTime;
    private String endTime;
    private RepeatType repeatType;
    private List<Integer> selectedDays;
    private boolean enabled;

    public enum RepeatType {
        ONCE,
        DAILY,
        WEEKLY,
        SELECTED_DAYS
    }

    public TimePeriod() {
        this.id = generateId();
        this.name = "";
        this.startTime = "08:00";
        this.endTime = "22:00";
        this.repeatType = RepeatType.DAILY;
        this.selectedDays = new ArrayList<>();
        this.enabled = true;
    }

    public TimePeriod(String name, String startTime, String endTime, RepeatType repeatType, List<Integer> selectedDays, boolean enabled) {
        this.id = generateId();
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.repeatType = repeatType;
        this.selectedDays = selectedDays != null ? new ArrayList<>(selectedDays) : new ArrayList<>();
        this.enabled = enabled;
    }

    private String generateId() {
        return "period_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public RepeatType getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(RepeatType repeatType) {
        this.repeatType = repeatType;
    }

    public List<Integer> getSelectedDays() {
        return new ArrayList<>(selectedDays);
    }

    public void setSelectedDays(List<Integer> selectedDays) {
        this.selectedDays = selectedDays != null ? new ArrayList<>(selectedDays) : new ArrayList<>();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // 将时间字符串转换为分钟数（用于比较）
    public static int timeToMinutes(String time) {
        try {
            String[] parts = time.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            return hours * 60 + minutes;
        } catch (Exception e) {
            return 0;
        }
    }

    // 将分钟数转换为时间字符串
    public static String minutesToTime(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        return String.format("%02d:%02d", hours, mins);
    }

    // 检查指定时间是否在当前时段内
    public boolean containsTime(String time) {
        if (!enabled) {
            return false;
        }
        
        int currentMinutes = timeToMinutes(time);
        int startMinutes = timeToMinutes(startTime);
        int endMinutes = timeToMinutes(endTime);
        
        // 处理跨天的情况
        if (endMinutes < startMinutes) {
            return currentMinutes >= startMinutes || currentMinutes <= endMinutes;
        } else {
            return currentMinutes >= startMinutes && currentMinutes <= endMinutes;
        }
    }

    @Override
    public String toString() {
        return "TimePeriod{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", repeatType=" + repeatType +
                ", selectedDays=" + selectedDays +
                ", enabled=" + enabled +
                '}';
    }
}