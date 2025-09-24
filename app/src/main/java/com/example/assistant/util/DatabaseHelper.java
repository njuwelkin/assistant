/**
 * MIT License
 * Copyright (c) 2023 illu@biubiu.org
 */
package com.example.assistant.util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.assistant.model.TimePeriod;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "auth_db";
    private static final int DATABASE_VERSION = 4; // 增加版本号以支持时段设置表

    // 认证表名
    private static final String TABLE_AUTH = "auth";
    // 表字段
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TOKEN = "token";
    private static final String COLUMN_LOGIN_TIME = "login_time";

    // 家长密码表名
    private static final String TABLE_PARENT_PASSWORD = "parent_password";
    // 家长密码表字段
    private static final String COLUMN_PASSWORD_HASH = "password_hash";
    private static final String COLUMN_CREATED_TIME = "created_time";
    
    // 用户设置表名
    private static final String TABLE_USER_SETTINGS = "user_settings";
    // 用户设置表字段
    private static final String COLUMN_SETTING_KEY = "setting_key";
    private static final String COLUMN_SETTING_VALUE = "setting_value";
    private static final String COLUMN_UPDATED_TIME = "updated_time";
    
    // 设置键名常量
    public static final String KEY_DAILY_TIME_LIMIT = "daily_time_limit"; // 每日使用时长限制(分钟)
    public static final String KEY_PARENT_PHONE_NUMBER = "parent_phone_number"; // 家长电话号码

    // 时段表名
    private static final String TABLE_TIME_PERIODS = "time_periods";
    // 时段表字段
    private static final String COLUMN_PERIOD_ID = "id";
    private static final String COLUMN_PERIOD_NAME = "name";
    private static final String COLUMN_START_TIME = "start_time";
    private static final String COLUMN_END_TIME = "end_time";
    private static final String COLUMN_REPEAT_TYPE = "repeat_type";
    private static final String COLUMN_SELECTED_DAYS = "selected_days";
    private static final String COLUMN_ENABLED = "enabled";

    // 创建认证表的SQL语句
    private static final String CREATE_AUTH_TABLE = "CREATE TABLE " + TABLE_AUTH + "(" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_TOKEN + " TEXT, " +
            COLUMN_LOGIN_TIME + " INTEGER" +
            ");";

    // 创建家长密码表的SQL语句
    private static final String CREATE_PARENT_PASSWORD_TABLE = "CREATE TABLE " + TABLE_PARENT_PASSWORD + "(" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_PASSWORD_HASH + " TEXT NOT NULL, " +
            COLUMN_CREATED_TIME + " INTEGER NOT NULL" +
            ");";
    
    // 创建用户设置表的SQL语句
    private static final String CREATE_USER_SETTINGS_TABLE = "CREATE TABLE " + TABLE_USER_SETTINGS + "(" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_SETTING_KEY + " TEXT NOT NULL UNIQUE, " +
            COLUMN_SETTING_VALUE + " TEXT NOT NULL, " +
            COLUMN_UPDATED_TIME + " INTEGER NOT NULL" +
            ");";

    // 创建时段表的SQL语句
    private static final String CREATE_TIME_PERIODS_TABLE = "CREATE TABLE " + TABLE_TIME_PERIODS + "(" +
            COLUMN_PERIOD_ID + " TEXT PRIMARY KEY, " +
            COLUMN_PERIOD_NAME + " TEXT, " +
            COLUMN_START_TIME + " TEXT, " +
            COLUMN_END_TIME + " TEXT, " +
            COLUMN_REPEAT_TYPE + " TEXT, " +
            COLUMN_SELECTED_DAYS + " TEXT, " +
            COLUMN_ENABLED + " INTEGER" +
            ");";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建认证表
    db.execSQL(CREATE_AUTH_TABLE);
    // 创建家长密码表
    db.execSQL(CREATE_PARENT_PASSWORD_TABLE);
    // 创建用户设置表
    db.execSQL(CREATE_USER_SETTINGS_TABLE);
    // 创建时段表
    db.execSQL(CREATE_TIME_PERIODS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 如果是从版本1升级到版本2，添加家长密码表
        if (oldVersion < 2) {
            db.execSQL(CREATE_PARENT_PASSWORD_TABLE);
        }
        // 如果是从版本2升级到版本3，添加用户设置表
        if (oldVersion < 3) {
            db.execSQL(CREATE_USER_SETTINGS_TABLE);
        }
        // 如果是从版本3升级到版本4，添加时段表
        if (oldVersion < 4) {
            db.execSQL(CREATE_TIME_PERIODS_TABLE);
        }
    }

    // 保存认证信息
    public void saveAuthInfo(String token, long loginTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // 先删除旧的认证信息
            db.delete(TABLE_AUTH, null, null);
            // 插入新的认证信息
            db.execSQL("INSERT INTO " + TABLE_AUTH + "(" + COLUMN_TOKEN + ", " + COLUMN_LOGIN_TIME + ") VALUES(?, ?)",
                    new Object[]{token, loginTime});
        } catch (Exception e) {
            Log.e(TAG, "Failed to save auth info", e);
        } finally {
            db.close();
        }
    }

    // 获取token
    public String getAuthToken() {
        SQLiteDatabase db = this.getReadableDatabase();
        String token = null;
        try {
            Cursor cursor = db.query(TABLE_AUTH, new String[]{COLUMN_TOKEN}, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                token = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TOKEN));
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get auth token", e);
        } finally {
            db.close();
        }
        return token;
    }

    // 获取登录时间
    public long getLoginTime() {
        SQLiteDatabase db = this.getReadableDatabase();
        long loginTime = 0;
        try {
            Cursor cursor = db.query(TABLE_AUTH, new String[]{COLUMN_LOGIN_TIME}, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                loginTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LOGIN_TIME));
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get login time", e);
        } finally {
            db.close();
        }
        return loginTime;
    }
    
    // 保存家长密码
    public boolean saveParentPassword(String passwordHash) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // 先删除旧的密码信息
            db.delete(TABLE_PARENT_PASSWORD, null, null);
            // 插入新的密码信息
            db.execSQL("INSERT INTO " + TABLE_PARENT_PASSWORD + "(" + COLUMN_PASSWORD_HASH + ", " + COLUMN_CREATED_TIME + ") VALUES(?, ?)",
                    new Object[]{passwordHash, System.currentTimeMillis()});
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save parent password", e);
            return false;
        } finally {
            db.close();
        }
    }
    
    // 获取家长密码
    public String getParentPassword() {
        SQLiteDatabase db = this.getReadableDatabase();
        String passwordHash = null;
        try {
            Cursor cursor = db.query(TABLE_PARENT_PASSWORD, new String[]{COLUMN_PASSWORD_HASH}, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                passwordHash = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD_HASH));
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get parent password", e);
        } finally {
            db.close();
        }
        return passwordHash;
    }
    
    // 检查家长密码是否已设置
    public boolean isParentPasswordSet() {
        String passwordHash = getParentPassword();
        return passwordHash != null && !passwordHash.isEmpty();
    }
    
    // 清除家长密码
    public void clearParentPassword() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete(TABLE_PARENT_PASSWORD, null, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear parent password", e);
        } finally {
            db.close();
        }
    }

    // 清除认证信息
    public void clearAuthInfo() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete(TABLE_AUTH, null, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear auth info", e);
        } finally {
            db.close();
        }
    }
    
    // 保存用户设置
    public boolean saveUserSetting(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // 先检查是否已存在该设置
            Cursor cursor = db.query(TABLE_USER_SETTINGS, 
                    new String[]{COLUMN_ID}, 
                    COLUMN_SETTING_KEY + " = ?", 
                    new String[]{key}, 
                    null, null, null);
            
            long currentTime = System.currentTimeMillis();
            
            if (cursor != null && cursor.moveToFirst()) {
                // 如果已存在，更新值
                db.execSQL("UPDATE " + TABLE_USER_SETTINGS + " SET " + 
                        COLUMN_SETTING_VALUE + " = ?, " + 
                        COLUMN_UPDATED_TIME + " = ? WHERE " + 
                        COLUMN_SETTING_KEY + " = ?",
                        new Object[]{value, currentTime, key});
                cursor.close();
            } else {
                // 如果不存在，插入新值
                db.execSQL("INSERT INTO " + TABLE_USER_SETTINGS + "(" + 
                        COLUMN_SETTING_KEY + ", " + 
                        COLUMN_SETTING_VALUE + ", " + 
                        COLUMN_UPDATED_TIME + ") VALUES(?, ?, ?)",
                        new Object[]{key, value, currentTime});
                if (cursor != null) {
                    cursor.close();
                }
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save user setting: " + key, e);
            return false;
        } finally {
            db.close();
        }
    }
    
    // 获取用户设置
    public String getUserSetting(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        String value = null;
        try {
            Cursor cursor = db.query(TABLE_USER_SETTINGS, 
                    new String[]{COLUMN_SETTING_VALUE}, 
                    COLUMN_SETTING_KEY + " = ?", 
                    new String[]{key}, 
                    null, null, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                value = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SETTING_VALUE));
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get user setting: " + key, e);
        } finally {
            db.close();
        }
        return value;
    }
    
    // 获取所有时段
    public List<TimePeriod> getTimePeriods() {
        List<TimePeriod> timePeriods = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        try {
            Cursor cursor = db.query(TABLE_TIME_PERIODS, null, null, null, null, null, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PERIOD_ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PERIOD_NAME));
                    String startTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_START_TIME));
                    String endTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_END_TIME));
                    String repeatTypeStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REPEAT_TYPE));
                    String selectedDaysStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SELECTED_DAYS));
                    boolean enabled = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENABLED)) == 1;
                    
                    TimePeriod.RepeatType repeatType = TimePeriod.RepeatType.valueOf(repeatTypeStr);
                    List<Integer> selectedDays = new ArrayList<>();
                    
                    if (selectedDaysStr != null && !selectedDaysStr.isEmpty()) {
                        String[] daysArray = selectedDaysStr.split(",");
                        for (String day : daysArray) {
                            try {
                                selectedDays.add(Integer.parseInt(day.trim()));
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Invalid day format: " + day, e);
                            }
                        }
                    }
                    
                    TimePeriod period = new TimePeriod(name, startTime, endTime, repeatType, selectedDays, enabled);
                    period.setId(id);
                    timePeriods.add(period);
                } while (cursor.moveToNext());
                
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get time periods", e);
        } finally {
            db.close();
        }
        
        return timePeriods;
    }
    
    // 保存所有时段
    public boolean saveTimePeriods(List<TimePeriod> timePeriods) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        try {
            db.beginTransaction();
            
            // 先删除所有现有时段
            db.delete(TABLE_TIME_PERIODS, null, null);
            
            // 插入新的时段
            for (TimePeriod period : timePeriods) {
                StringBuilder selectedDaysBuilder = new StringBuilder();
                List<Integer> selectedDays = period.getSelectedDays();
                
                if (selectedDays != null && !selectedDays.isEmpty()) {
                    for (int i = 0; i < selectedDays.size(); i++) {
                        if (i > 0) {
                            selectedDaysBuilder.append(",");
                        }
                        selectedDaysBuilder.append(selectedDays.get(i));
                    }
                }
                
                db.execSQL("INSERT INTO " + TABLE_TIME_PERIODS + "(" +
                        COLUMN_PERIOD_ID + ", " +
                        COLUMN_PERIOD_NAME + ", " +
                        COLUMN_START_TIME + ", " +
                        COLUMN_END_TIME + ", " +
                        COLUMN_REPEAT_TYPE + ", " +
                        COLUMN_SELECTED_DAYS + ", " +
                        COLUMN_ENABLED + ") VALUES(?, ?, ?, ?, ?, ?, ?)",
                        new Object[]{
                                period.getId(),
                                period.getName(),
                                period.getStartTime(),
                                period.getEndTime(),
                                period.getRepeatType().name(),
                                selectedDaysBuilder.toString(),
                                period.isEnabled() ? 1 : 0
                        });
            }
            
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save time periods", e);
            return false;
        } finally {
            db.endTransaction();
            db.close();
        }
    }
}