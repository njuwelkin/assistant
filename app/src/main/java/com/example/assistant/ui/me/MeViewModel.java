/**
 * MIT License
 * Copyright (c) 2023 illu@biubiu.org
 */
package com.example.assistant.ui.me;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.assistant.model.TimePeriod;
import com.example.assistant.database.DatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MeViewModel extends AndroidViewModel {
    private static final String TAG = "MeViewModel";
    private static final String PREF_NAME = "app_settings";
    private static final String KEY_DAILY_TIME_LIMIT = "daily_time_limit";
    private static final String KEY_PASSWORD_SET = "password_set";
    private static final String KEY_ENCRYPTED_PASSWORD = "encrypted_password";
    
    private MutableLiveData<Integer> dailyTimeLimit = new MutableLiveData<>();
    private MutableLiveData<Boolean> passwordSet = new MutableLiveData<>();
    private MutableLiveData<String> timePeriodStart = new MutableLiveData<>();
    private MutableLiveData<String> timePeriodEnd = new MutableLiveData<>();
    private MutableLiveData<List<TimePeriod>> timePeriods = new MutableLiveData<>();
    
    private DatabaseHelper databaseHelper;
    private SharedPreferences preferences;

    public MeViewModel(@NonNull Application application) {
        super(application);
        preferences = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        databaseHelper = new DatabaseHelper(application);
        
        // 初始化默认值
        dailyTimeLimit.setValue(120); // 默认2小时
        passwordSet.setValue(preferences.getBoolean(KEY_PASSWORD_SET, false));
        timePeriodStart.setValue("08:00");
        timePeriodEnd.setValue("22:00");
        timePeriods.setValue(new ArrayList<>());
    }

    // 初始化数据库
    public void initDatabase(Context context) {
        try {
            SQLiteDatabase db = databaseHelper.getReadableDatabase();
            
            // 加载每日使用时长限制
            Cursor cursor = db.rawQuery("SELECT value FROM settings WHERE key = ?", 
                    new String[]{KEY_DAILY_TIME_LIMIT});
            if (cursor.moveToFirst()) {
                String savedTimeLimit = cursor.getString(0);
                if (savedTimeLimit != null && !savedTimeLimit.isEmpty()) {
                    try {
                        dailyTimeLimit.setValue(Integer.parseInt(savedTimeLimit));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Failed to parse daily time limit: " + savedTimeLimit, e);
                    }
                }
            }
            cursor.close();
            
            // 加载时间限制时段
            loadTimePeriods();
            
            db.close();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing database", e);
        }
    }

    // 获取每日使用时长限制
    public LiveData<Integer> getDailyTimeLimit() {
        return dailyTimeLimit;
    }

    // 设置每日使用时长限制
    public void setDailyTimeLimit(int minutes) {
        dailyTimeLimit.setValue(minutes);
        saveSetting(KEY_DAILY_TIME_LIMIT, String.valueOf(minutes));
    }

    // 获取密码设置状态
    public LiveData<Boolean> isPasswordSet() {
        return passwordSet;
    }

    // 验证密码
    public boolean verifyPassword(String password) {
        if (!passwordSet.getValue()) {
            return true; // 未设置密码时，任何密码都视为有效
        }
        
        String encryptedPassword = preferences.getString(KEY_ENCRYPTED_PASSWORD, "");
        return encryptedPassword.equals(encryptPassword(password));
    }

    // 设置密码
    public void setPassword(String password) {
        if (password.isEmpty()) {
            // 清除密码
            preferences.edit().putBoolean(KEY_PASSWORD_SET, false)
                    .remove(KEY_ENCRYPTED_PASSWORD).apply();
            passwordSet.setValue(false);
        } else {
            // 设置新密码
            String encryptedPassword = encryptPassword(password);
            preferences.edit().putBoolean(KEY_PASSWORD_SET, true)
                    .putString(KEY_ENCRYPTED_PASSWORD, encryptedPassword).apply();
            passwordSet.setValue(true);
        }
    }

    // 简单的密码加密（实际应用中应该使用更安全的加密方式）
    private String encryptPassword(String password) {
        // 这里只是一个简单的示例，实际应用中应该使用更安全的加密方式
        return String.valueOf(password.hashCode());
    }

    // 保存设置到数据库
    private void saveSetting(String key, String value) {
        try {
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            db.execSQL("DELETE FROM settings WHERE key = ?", new String[]{key});
            db.execSQL("INSERT INTO settings (key, value) VALUES (?, ?)", 
                    new Object[]{key, value});
            db.close();
        } catch (Exception e) {
            Log.e(TAG, "Error saving setting: " + key, e);
        }
    }

    // 获取时间限制时段
    public LiveData<List<TimePeriod>> getTimePeriods() {
        return timePeriods;
    }

    // 加载时间限制时段
    private void loadTimePeriods() {
        try {
            List<TimePeriod> loadedPeriods = new ArrayList<>();
            SQLiteDatabase db = databaseHelper.getReadableDatabase();
            
            Cursor cursor = db.rawQuery("SELECT id, name, start_time, end_time, repeat_type, selected_days, enabled FROM time_periods", null);
            while (cursor.moveToNext()) {
                TimePeriod period = new TimePeriod();
                period.setId(cursor.getString(0));
                period.setName(cursor.getString(1));
                period.setStartTime(cursor.getString(2));
                period.setEndTime(cursor.getString(3));
                
                try {
                    int repeatTypeValue = cursor.getInt(4);
                    TimePeriod.RepeatType repeatType = TimePeriod.RepeatType.values()[repeatTypeValue];
                    period.setRepeatType(repeatType);
                } catch (Exception e) {
                    period.setRepeatType(TimePeriod.RepeatType.DAILY);
                }
                
                // 解析selected_days（格式为逗号分隔的数字）
                String selectedDaysStr = cursor.getString(5);
                List<Integer> selectedDays = new ArrayList<>();
                if (selectedDaysStr != null && !selectedDaysStr.isEmpty()) {
                    String[] daysArray = selectedDaysStr.split(",");
                    for (String day : daysArray) {
                        try {
                            selectedDays.add(Integer.parseInt(day.trim()));
                        } catch (NumberFormatException e) {
                            // 忽略无效的天数
                        }
                    }
                }
                period.setSelectedDays(selectedDays);
                
                period.setEnabled(cursor.getInt(6) == 1);
                loadedPeriods.add(period);
            }
            cursor.close();
            db.close();
            
            timePeriods.setValue(loadedPeriods);
        } catch (Exception e) {
            Log.e(TAG, "Error loading time periods", e);
            timePeriods.setValue(new ArrayList<>());
        }
    }

    // 保存时间限制时段
    public void saveTimePeriods(List<TimePeriod> periods) {
        try {
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            db.beginTransaction();
            
            // 先删除所有现有时段
            db.execSQL("DELETE FROM time_periods");
            
            // 插入新时段
            for (TimePeriod period : periods) {
                // 格式化selected_days为逗号分隔的字符串
                StringBuilder selectedDaysStr = new StringBuilder();
                List<Integer> selectedDays = period.getSelectedDays();
                if (selectedDays != null && !selectedDays.isEmpty()) {
                    for (int i = 0; i < selectedDays.size(); i++) {
                        selectedDaysStr.append(selectedDays.get(i));
                        if (i < selectedDays.size() - 1) {
                            selectedDaysStr.append(",");
                        }
                    }
                }
                
                db.execSQL(
                        "INSERT INTO time_periods (id, name, start_time, end_time, repeat_type, selected_days, enabled) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        new Object[]{
                                period.getId(),
                                period.getName(),
                                period.getStartTime(),
                                period.getEndTime(),
                                period.getRepeatType().ordinal(),
                                selectedDaysStr.toString(),
                                period.isEnabled() ? 1 : 0
                        }
                );
            }
            
            db.setTransactionSuccessful();
            db.endTransaction();
            db.close();
            
            // 更新LiveData
            timePeriods.setValue(new ArrayList<>(periods));
        } catch (Exception e) {
            Log.e(TAG, "Error saving time periods", e);
        }
    }

    // 检查当前时间是否在禁止使用时段内
    public boolean isInRestrictedTime() {
        List<TimePeriod> periods = timePeriods.getValue();
        if (periods == null || periods.isEmpty()) {
            return false;
        }
        
        // 获取当前时间
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        
        // 检查是否在任何启用的时段内
        for (TimePeriod period : periods) {
            if (period.isEnabled() && period.containsTime(currentTime)) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // 关闭数据库连接
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}