package com.example.assistant.ui.notifications;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.example.assistant.util.DatabaseHelper;
import com.example.assistant.util.AuthManager;
import com.example.assistant.model.TimePeriod;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
    private final MutableLiveData<List<TimePeriod>> timePeriods = new MutableLiveData<>();
    
    // 应用缓存相关数据
    private final MutableLiveData<String> cacheSize = new MutableLiveData<>();
    
    // 头像文件保存的目录
    private static final String AVATAR_DIR = "images";
    
    // 数据库帮助类
    private DatabaseHelper databaseHelper;

    public MeViewModel() {
        // 初始化默认值
        username.setValue("付伊路");
        userId.setValue("20230001");
        parentPasswordSet.setValue(false);
        dailyTimeLimit.setValue(120); // 默认每日使用2小时
        timePeriodStart.setValue(""); // 不设置默认开始时间
        timePeriodEnd.setValue(""); // 不设置默认结束时间
        timePeriods.setValue(new ArrayList<>()); // 初始化时段列表为空
        cacheSize.setValue("5.2MB"); // 模拟缓存大小
    }
    
    // 初始化数据库帮助类
    public void initDatabase(Context context) {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper(context);
            // 检查家长密码是否已设置
            boolean isSet = databaseHelper.isParentPasswordSet();
            parentPasswordSet.setValue(isSet);
            
            // 从数据库加载每日使用时长设置
            String savedTimeLimit = databaseHelper.getUserSetting(DatabaseHelper.KEY_DAILY_TIME_LIMIT);
            if (savedTimeLimit != null && !savedTimeLimit.isEmpty()) {
                try {
                    int minutes = Integer.parseInt(savedTimeLimit);
                    dailyTimeLimit.setValue(minutes);
                } catch (NumberFormatException e) {
                    Log.e("MeViewModel", "Failed to parse daily time limit: " + savedTimeLimit, e);
                }
            }
            
            // 从数据库加载时段设置
            List<TimePeriod> savedTimePeriods = databaseHelper.getTimePeriods();
            if (savedTimePeriods != null && !savedTimePeriods.isEmpty()) {
                timePeriods.setValue(savedTimePeriods);
            } else {
                // 如果没有保存的时段，添加一个默认的晚间休息时段
                List<TimePeriod> defaultPeriods = new ArrayList<>();
                defaultPeriods.add(new TimePeriod("晚间休息", "22:00", "06:00", TimePeriod.RepeatType.DAILY, new ArrayList<>(), true));
                timePeriods.setValue(defaultPeriods);
            }
        }
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
    // 设置家长密码
    public void setParentPassword(String password) {
        if (databaseHelper != null) {
            String hashedPassword = hashPassword(password);
            boolean success = databaseHelper.saveParentPassword(hashedPassword);
            if (success) {
                parentPasswordSet.setValue(true);
                Log.d("MeViewModel", "家长密码设置成功");
            } else {
                Log.e("MeViewModel", "家长密码设置失败");
            }
        }
    }
    
    // 验证家长密码
    public boolean verifyParentPassword(String password) {
        if (databaseHelper != null) {
            String storedPassword = databaseHelper.getParentPassword();
            String hashedPassword = hashPassword(password);
            return hashedPassword != null && hashedPassword.equals(storedPassword);
        }
        return false;
    }

    // 清除家长密码
    public void clearParentPassword() {
        if (databaseHelper != null) {
            databaseHelper.clearParentPassword();
        }
        parentPasswordSet.setValue(false);
    }
    
    // 密码加密（使用SHA-256）
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e("MeViewModel", "密码加密失败: " + e.getMessage());
            return null;
        }
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
    public void setDailyTimeLimit(Context context, int minutes) {
        dailyTimeLimit.setValue(minutes);
        
        // 如果数据库已初始化，保存到数据库
        if (databaseHelper == null && context != null) {
            initDatabase(context);
        }
        
        if (databaseHelper != null) {
            databaseHelper.saveUserSetting(DatabaseHelper.KEY_DAILY_TIME_LIMIT, String.valueOf(minutes));
        }
    }
    
    // 设置每日使用时长限制（不保存到数据库）
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
    
    // 获取时段列表的LiveData
    public LiveData<List<TimePeriod>> getTimePeriods() {
        return timePeriods;
    }
    
    // 保存时段列表
    public void saveTimePeriods(List<TimePeriod> periods) {
        timePeriods.setValue(periods);
        
        // 如果数据库已初始化，保存到数据库
        if (databaseHelper != null) {
            databaseHelper.saveTimePeriods(periods);
        }
    }
    
    // 获取缓存大小的LiveData
    public LiveData<String> getCacheSize() {
        return cacheSize;
    }
    
    // 设置缓存大小
    public void setCacheSize(String size) {
        cacheSize.setValue(size);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // 清理资源
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}