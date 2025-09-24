/**
 * MIT License
 * Copyright (c) 2023 illu@biubiu.org
 */
package com.example.assistant.util;

import android.content.Context;

import java.util.concurrent.TimeUnit;

public class AuthManager {
    // 登录超时时间（7天）
    private static final long SESSION_TIMEOUT_MS = TimeUnit.DAYS.toMillis(7);

    // 检查用户是否已登录（包括检查token是否存在和是否超时）
    public static boolean isLoggedIn(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        String token = dbHelper.getAuthToken();
        long loginTime = dbHelper.getLoginTime();

        // 检查token是否存在且未超时
        if (token != null && !token.isEmpty()) {
            // 检查是否超时
            long currentTime = System.currentTimeMillis();
            return (currentTime - loginTime) < SESSION_TIMEOUT_MS;
        }

        return false;
    }

    // 获取用户认证token
    public static String getAuthToken(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        return dbHelper.getAuthToken();
    }

    // 保存用户认证信息
    public static void saveAuthInfo(Context context, String token) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        dbHelper.saveAuthInfo(token, System.currentTimeMillis());
    }

    // 清除用户认证信息（登出）
    public static void clearAuthInfo(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        dbHelper.clearAuthInfo();
    }
}