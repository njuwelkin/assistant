package com.example.assistant.ui.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MeViewModel extends ViewModel {

    // 用户信息相关数据
    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<String> userId = new MutableLiveData<>();
    private final MutableLiveData<String> avatarUri = new MutableLiveData<>();

    // 用户设置相关数据
    private final MutableLiveData<Boolean> parentPasswordSet = new MutableLiveData<>();
    private final MutableLiveData<String> lastLoginTime = new MutableLiveData<>();
    
    // 使用时间管理相关数据
    private final MutableLiveData<Integer> dailyTimeLimit = new MutableLiveData<>(); // 单位：分钟
    private final MutableLiveData<String> timePeriodStart = new MutableLiveData<>();
    private final MutableLiveData<String> timePeriodEnd = new MutableLiveData<>();
    
    // 应用缓存相关数据
    private final MutableLiveData<String> cacheSize = new MutableLiveData<>();

    public MeViewModel() {
        // 初始化默认值
        username.setValue("付伊路");
        userId.setValue("20230001");
        parentPasswordSet.setValue(false);
        dailyTimeLimit.setValue(120); // 默认每日使用2小时
        timePeriodStart.setValue("08:00"); // 默认开始时间
        timePeriodEnd.setValue("22:00"); // 默认结束时间
        cacheSize.setValue("5.2MB"); // 模拟缓存大小
        // 在实际应用中，这些数据应该从本地存储或远程服务器获取
    }

    // 获取用户名的LiveData
    public LiveData<String> getUsername() {
        return username;
    }

    // 更新用户名
    public void updateUsername(String newUsername) {
        username.setValue(newUsername);
    }

    // 获取用户ID的LiveData
    public LiveData<String> getUserId() {
        return userId;
    }

    // 获取头像URI的LiveData
    public LiveData<String> getAvatarUri() {
        return avatarUri;
    }

    // 更新头像URI
    public void updateAvatarUri(String uri) {
        avatarUri.setValue(uri);
    }

    // 获取家长密码设置状态的LiveData
    public LiveData<Boolean> isParentPasswordSet() {
        return parentPasswordSet;
    }

    // 设置家长密码
    public void setParentPassword(String password) {
        // 实际应用中应该有密码加密和存储逻辑
        parentPasswordSet.setValue(true);
    }

    // 清除家长密码
    public void clearParentPassword() {
        // 实际应用中应该有清除密码的逻辑
        parentPasswordSet.setValue(false);
    }

    // 获取最后登录时间的LiveData
    public LiveData<String> getLastLoginTime() {
        return lastLoginTime;
    }

    // 更新最后登录时间
    public void updateLastLoginTime(String time) {
        lastLoginTime.setValue(time);
    }
    
    // 获取每日使用时长限制的LiveData
    public LiveData<Integer> getDailyTimeLimit() {
        return dailyTimeLimit;
    }
    
    // 设置每日使用时长限制
    public void setDailyTimeLimit(int minutes) {
        dailyTimeLimit.setValue(minutes);
    }
    
    // 获取使用时段开始时间的LiveData
    public LiveData<String> getTimePeriodStart() {
        return timePeriodStart;
    }
    
    // 设置使用时段开始时间
    public void setTimePeriodStart(String startTime) {
        timePeriodStart.setValue(startTime);
    }
    
    // 获取使用时段结束时间的LiveData
    public LiveData<String> getTimePeriodEnd() {
        return timePeriodEnd;
    }
    
    // 设置使用时段结束时间
    public void setTimePeriodEnd(String endTime) {
        timePeriodEnd.setValue(endTime);
    }
    
    // 获取缓存大小的LiveData
    public LiveData<String> getCacheSize() {
        return cacheSize;
    }
    
    // 设置缓存大小
    public void setCacheSize(String size) {
        cacheSize.setValue(size);
    }
}