package com.example.assistant.util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.concurrent.TimeUnit;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "auth_db";
    private static final int DATABASE_VERSION = 1;

    // 认证表名
    private static final String TABLE_AUTH = "auth";
    // 表字段
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TOKEN = "token";
    private static final String COLUMN_LOGIN_TIME = "login_time";

    // 创建表的SQL语句
    private static final String CREATE_AUTH_TABLE = "CREATE TABLE " + TABLE_AUTH + "(" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_TOKEN + " TEXT, " +
            COLUMN_LOGIN_TIME + " INTEGER" +
            ");";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建认证表
        db.execSQL(CREATE_AUTH_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 如果数据库版本更新，删除旧表并创建新表
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AUTH);
        onCreate(db);
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
}