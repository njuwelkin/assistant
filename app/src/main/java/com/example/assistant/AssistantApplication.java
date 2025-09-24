/**
 * MIT License
 * Copyright (c) 2023 illu@biubiu.org
 */
package com.example.assistant;

import android.app.Application;
import android.content.Intent;

import com.example.assistant.util.AuthManager;

public class AssistantApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 这里可以进行一些全局初始化操作
        // 注意：不要在这里直接启动Activity，应该在MainActivity中进行登录检查
    }

    // 检查登录状态并跳转到相应页面
    public static void checkLoginStatus(MainActivity activity) {
        if (!AuthManager.isLoggedIn(activity)) {
            // 如果用户未登录或登录已超时，跳转到登录页面
            Intent intent = new Intent(activity, LoginActivity.class);
            activity.startActivity(intent);
            activity.finish();
        }
    }
}