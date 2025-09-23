package com.example.assistant.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.assistant.model.TimePeriod;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "assistant.db";
    private static final int DATABASE_VERSION = 1;
    
    // 表名
    public static final String TABLE_SETTINGS = "settings";
    public static final String TABLE_TIME_PERIODS = "time_periods";
    public static final String TABLE_AUTH = "auth_info";
    
    // settings表列名
    public static final String KEY_DAILY_TIME_LIMIT = "daily_time_limit"; // 每日使用时长限制
    public static final String KEY_LAST_LOGIN_TIME = "last_login_time"; // 最后登录时间
    public static final String KEY_PARENT_PASSWORD = "parent_password"; // 家长密码
    
    // settings表列名
    public static final String COLUMN_KEY = "key";
    public static final String COLUMN_VALUE = "value";
    
    // time_periods表列名
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_START_TIME = "start_time";
    public static final String COLUMN_END_TIME = "end_time";
    public static final String COLUMN_REPEAT_TYPE = "repeat_type";
    public static final String COLUMN_SELECTED_DAYS = "selected_days";
    public static final String COLUMN_ENABLED = "enabled";
    
    // 创建settings表的SQL语句
    private static final String CREATE_TABLE_SETTINGS = "CREATE TABLE " + TABLE_SETTINGS + "(" +
            COLUMN_KEY + " TEXT PRIMARY KEY, " +
            COLUMN_VALUE + " TEXT" +
            ")";
    
    // 创建time_periods表的SQL语句
    private static final String CREATE_TABLE_TIME_PERIODS = "CREATE TABLE " + TABLE_TIME_PERIODS + "(" +
            COLUMN_ID + " TEXT PRIMARY KEY, " +
            COLUMN_NAME + " TEXT NOT NULL, " +
            COLUMN_START_TIME + " TEXT NOT NULL, " +
            COLUMN_END_TIME + " TEXT NOT NULL, " +
            COLUMN_REPEAT_TYPE + " INTEGER DEFAULT 1, " +
            COLUMN_SELECTED_DAYS + " TEXT, " +
            COLUMN_ENABLED + " INTEGER DEFAULT 1" +
            ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建settings表
        db.execSQL(CREATE_TABLE_SETTINGS);
        Log.d(TAG, "Created table: " + TABLE_SETTINGS);
        
        // 创建time_periods表
        db.execSQL(CREATE_TABLE_TIME_PERIODS);
        Log.d(TAG, "Created table: " + TABLE_TIME_PERIODS);
        
        // 插入默认设置
        insertDefaultSettings(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 当数据库版本更新时的处理逻辑
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        
        // 如果需要，这里可以添加表结构升级逻辑
        if (oldVersion < 1) {
            // 版本1的初始实现
        }
    }

    // 插入默认设置
    private void insertDefaultSettings(SQLiteDatabase db) {
        // 插入默认的每日使用时长限制（120分钟 = 2小时）
        db.execSQL("INSERT INTO " + TABLE_SETTINGS + " (" + COLUMN_KEY + ", " + COLUMN_VALUE + ") VALUES (?, ?)",
                new Object[]{"daily_time_limit", "120"});
        
        // 可以添加其他默认设置...
    }

    // 清空所有表数据（用于调试或重置功能）
    public void clearAllData() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_SETTINGS);
        db.execSQL("DELETE FROM " + TABLE_TIME_PERIODS);
        insertDefaultSettings(db);
        db.close();
    }
    
    // 保存用户设置
    public void saveUserSetting(String key, String value) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            
            // 先删除已存在的设置
            db.execSQL("DELETE FROM " + TABLE_SETTINGS + " WHERE " + COLUMN_KEY + " = ?", new String[]{key});
            
            // 插入新设置
            db.execSQL("INSERT INTO " + TABLE_SETTINGS + " (" + COLUMN_KEY + ", " + COLUMN_VALUE + ") VALUES (?, ?)",
                    new Object[]{key, value});
            
            db.setTransactionSuccessful();
            db.endTransaction();
            db.close();
        } catch (Exception e) {
            Log.e(TAG, "Error saving user setting: " + key, e);
        }
    }
    
    // 获取用户设置
    public String getUserSetting(String key) {
        String value = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + COLUMN_VALUE + " FROM " + TABLE_SETTINGS + " WHERE " + COLUMN_KEY + " = ?",
                    new String[]{key});
            
            if (cursor.moveToFirst()) {
                value = cursor.getString(0);
            }
            
            cursor.close();
            db.close();
        } catch (Exception e) {
            Log.e(TAG, "Error getting user setting: " + key, e);
        }
        return value;
    }
    
    // 获取所有时段设置
    public List<TimePeriod> getTimePeriods() {
        List<TimePeriod> periods = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + COLUMN_ID + ", " + COLUMN_NAME + ", " + 
                    COLUMN_START_TIME + ", " + COLUMN_END_TIME + ", " + 
                    COLUMN_REPEAT_TYPE + ", " + COLUMN_SELECTED_DAYS + ", " + 
                    COLUMN_ENABLED + " FROM " + TABLE_TIME_PERIODS, null);
            
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
                periods.add(period);
            }
            
            cursor.close();
            db.close();
        } catch (Exception e) {
            Log.e(TAG, "Error getting time periods", e);
        }
        return periods;
    }
    
    // 保存时段设置
    public void saveTimePeriods(List<TimePeriod> periods) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            
            // 先删除所有现有时段
            db.execSQL("DELETE FROM " + TABLE_TIME_PERIODS);
            
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
                        "INSERT INTO " + TABLE_TIME_PERIODS + " (" + COLUMN_ID + ", " + COLUMN_NAME + ", " + 
                        COLUMN_START_TIME + ", " + COLUMN_END_TIME + ", " + COLUMN_REPEAT_TYPE + ", " + 
                        COLUMN_SELECTED_DAYS + ", " + COLUMN_ENABLED + ") VALUES (?, ?, ?, ?, ?, ?, ?)",
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
        } catch (Exception e) {
            Log.e(TAG, "Error saving time periods", e);
        }
    }
    
    // 检查家长密码是否已设置
    public boolean isParentPasswordSet() {
        String password = getUserSetting(KEY_PARENT_PASSWORD);
        return password != null && !password.isEmpty();
    }
    
    // 获取家长密码
    public String getParentPassword() {
        return getUserSetting(KEY_PARENT_PASSWORD);
    }
    
    // 清除家长密码
    public void clearParentPassword() {
        saveUserSetting(KEY_PARENT_PASSWORD, "");
    }
    
    // 保存认证信息
    public void saveAuthInfo(String token, long timestamp) {
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            db.beginTransaction();
            
            // 先删除已存在的认证信息
            db.execSQL("DELETE FROM " + TABLE_AUTH);
            
            // 插入新认证信息
            db.execSQL("INSERT INTO " + TABLE_AUTH + " (token, timestamp) VALUES (?, ?)",
                    new Object[]{token, timestamp});
            
            db.setTransactionSuccessful();
            db.endTransaction();
        } catch (Exception e) {
            Log.e(TAG, "Error saving auth info", e);
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }
}