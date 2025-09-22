package com.example.assistant.ui.notifications;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.example.assistant.util.AuthManager;

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
    
    // 头像文件保存的目录
    private static final String AVATAR_DIR = "images";

    public MeViewModel() {
        // 初始化默认值
        username.setValue("付伊路");
        userId.setValue("20230001");
        parentPasswordSet.setValue(false);
        dailyTimeLimit.setValue(120); // 默认每日使用2小时
        timePeriodStart.setValue("08:00"); // 默认开始时间
        timePeriodEnd.setValue("22:00"); // 默认结束时间
        cacheSize.setValue("5.2MB"); // 模拟缓存大小
    }

    /**
     * 更新用户名（现在只处理本地数据，不再访问服务器）
     */
    public void updateUsername(Context context, String newUsername) {
        username.setValue(newUsername);
        Log.d("MeViewModel", "Username updated locally: " + newUsername);
    }

    /**
     * 检查本地是否有保存的头像文件
     */
    public static String checkLocalAvatarFile(Context context) {
        // 检查头像存储目录
        File storageDir = new File(context.getExternalFilesDir(null), AVATAR_DIR);
        if (!storageDir.exists() || !storageDir.isDirectory()) {
            return null;
        }
        
        // 直接查找固定名称的头像文件
        File avatarFile = new File(storageDir, "AVATAR_current.jpg");
        
        // 如果文件存在，返回其绝对路径
        return avatarFile.exists() && avatarFile.isFile() ? avatarFile.getAbsolutePath() : null;
    }
    
    /**
     * 将头像数据保存到文件并返回URI字符串
     * 替换原有的头像文件，确保目录中只有一个头像文件
     */
    public String saveAvatarToFile(Context context, byte[] avatarData) {
        try {
            // 使用与FileProvider配置匹配的存储位置（external-files-path）
            File storageDir = new File(context.getExternalFilesDir(null), AVATAR_DIR);
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            } else {
                // 删除目录中已存在的所有头像文件
                File[] existingFiles = storageDir.listFiles();
                if (existingFiles != null) {
                    for (File file : existingFiles) {
                        if (file.isFile() && file.getName().startsWith("AVATAR_") && file.getName().endsWith(".jpg")) {
                            if (file.delete()) {
                                Log.d("MeViewModel", "Deleted old avatar file: " + file.getName());
                            } else {
                                Log.w("MeViewModel", "Failed to delete old avatar file: " + file.getName());
                            }
                        }
                    }
                }
            }
            
            // 使用固定的文件名保存新头像
            String imageFileName = "AVATAR_current.jpg";
            File avatarFile = new File(storageDir, imageFileName);
            
            // 将字节数组写入文件
            FileOutputStream fos = new FileOutputStream(avatarFile);
            fos.write(avatarData);
            fos.close();
            
            // 返回文件URI
            return avatarFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e("MeViewModel", "Failed to save avatar to file", e);
            return null;
        }
    }

    // 初始化默认头像数据
    public void initializeDefaultAvatar(Context context) {
        // 这里可以添加初始化默认头像的逻辑
        // 例如，可以使用一个内置的默认头像文件
        Log.d("MeViewModel", "Initialized avatar handling - only local storage is used");
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
        // 先设置为null，强制触发UI更新（解决固定文件名导致的URI不变问题）
        avatarUri.setValue(null);
        // 然后设置为实际的URI值
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