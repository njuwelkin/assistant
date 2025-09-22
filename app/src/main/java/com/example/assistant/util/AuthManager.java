package com.example.assistant.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

public class AuthManager {
    private static final String PREFS_NAME = "user_prefs";
    private static final String TOKEN_KEY = "auth_token";
    private static final String LOGIN_TIME_KEY = "login_time";
    // 登录超时时间（7天）
    private static final long SESSION_TIMEOUT_MS = TimeUnit.DAYS.toMillis(7);

    // 检查用户是否已登录（包括检查token是否存在和是否超时）
    public static boolean isLoggedIn(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String token = preferences.getString(TOKEN_KEY, null);
        long loginTime = preferences.getLong(LOGIN_TIME_KEY, 0);

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
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getString(TOKEN_KEY, null);
    }

    // 保存用户认证信息
    public static void saveAuthInfo(Context context, String token) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(TOKEN_KEY, token);
        editor.putLong(LOGIN_TIME_KEY, System.currentTimeMillis());
        editor.apply();
    }

    // 清除用户认证信息（登出）
    public static void clearAuthInfo(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(TOKEN_KEY);
        editor.remove(LOGIN_TIME_KEY);
        editor.apply();
    }
}