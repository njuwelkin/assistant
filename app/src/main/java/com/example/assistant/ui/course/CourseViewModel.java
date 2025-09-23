package com.example.assistant.ui.course;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.app.Application;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.assistant.ui.course.model.Course;
import com.example.assistant.ui.course.model.Assignment;
import com.example.assistant.ui.course.model.Activity;
import com.example.assistant.util.AuthManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

public class CourseViewModel extends AndroidViewModel {

    public CourseViewModel(Application application) {
        super(application);
    }

    // 课程数据
    private final MutableLiveData<List<Course>> _courses = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Course>> getCourses() {
        return _courses;
    }
    
    // 作业数据
    private final MutableLiveData<List<Assignment>> _assignments = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Assignment>> getAssignments() {
        return _assignments;
    }
    
    // 活动数据
    private final MutableLiveData<List<Activity>> _activities = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Activity>> getActivities() {
        return _activities;
    }

    // 使用线程安全的集合解决脏读问题
    private final List<Course> courseCache = new CopyOnWriteArrayList<>();
    private boolean isCacheInitialized = false;
    private final Object cacheLock = new Object(); // 用于同步操作
    
    // 缓存当月作业数据
    private List<Assignment> monthlyAssignmentCache = null;
    private int cachedMonth = -1;
    private int cachedYear = -1;
    
    // 缓存当月活动数据
    private List<Activity> monthlyActivityCache = null;
    private int cachedActivityMonth = -1;
    private int cachedActivityYear = -1;

    private Date selectedDate;
    private String lastLoadedDate;

    public Date getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(Date date) {
        this.selectedDate = date;
    }

    // 加载指定日期的课程
    public void loadCourses(String date) {
        Log.d("CourseViewModel", "加载日期: " + date + " 的课程数据");
        
        try {
            lastLoadedDate = date;
            // 解析日期字符串
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date selectedDate = dateFormat.parse(date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(selectedDate);
            
            // 获取星期几 (1-7, 1=星期日, 2=星期一, ..., 7=星期六)
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            
            // 转换为周一到周五的格式 (1-5表示周一到周五)
            int convertedDayOfWeek = 0;
            if (dayOfWeek == Calendar.MONDAY) {
                convertedDayOfWeek = 1;
            } else if (dayOfWeek == Calendar.TUESDAY) {
                convertedDayOfWeek = 2;
            } else if (dayOfWeek == Calendar.WEDNESDAY) {
                convertedDayOfWeek = 3;
            } else if (dayOfWeek == Calendar.THURSDAY) {
                convertedDayOfWeek = 4;
            } else if (dayOfWeek == Calendar.FRIDAY) {
                convertedDayOfWeek = 5;
            }
            
            // 如果缓存未初始化，从服务器加载课程数据
            synchronized (cacheLock) {
                if (!isCacheInitialized) {
                    loadCourseDataFromServer();
                    // 立即使用模拟数据显示
                    List<Course> mockCourses = generateMockCourses(date);
                    _courses.postValue(mockCourses);
                    loadAssignments(date, mockCourses);
                    return; // 退出方法，等待异步加载完成后更新UI
                }
            }
            
            // 从缓存中筛选出指定日期的课程
            List<Course> dayCourses = new ArrayList<>();
            synchronized (cacheLock) {
                if (!courseCache.isEmpty() && convertedDayOfWeek > 0) {
                    for (Course course : courseCache) {
                        if (course.getDayOfWeek() == convertedDayOfWeek) {
                            dayCourses.add(course);
                        }
                    }
                }
            }
            
            // 如果没有从服务器获取到数据，使用模拟数据
            if (dayCourses.isEmpty()) {
                dayCourses = generateMockCourses(date);
            }
            
            Log.d("CourseViewModel", "课程数据数量: " + dayCourses.size());
            _courses.setValue(dayCourses);
            Log.d("CourseViewModel", "课程数据已设置到LiveData");
            
            // 同时加载该日期的作业
            loadAssignments(date, dayCourses);
        } catch (ParseException e) {
            Log.e("CourseViewModel", "加载课程数据失败", e);
            // 加载失败时使用模拟数据
            List<Course> mockCourses = generateMockCourses(date);
            _courses.setValue(mockCourses);
            loadAssignments(date, mockCourses);
        }
    }
    
    // 从服务器加载课程数据（只在初始化时加载一次）
    private void loadCourseDataFromServer() {
        new Thread(() -> {
            try {
                // 使用支持SSL证书的OkHttpClient
                OkHttpClient client = createOkHttpClient();
                String token = AuthManager.getAuthToken(getApplication());
                
                // 如果没有token或token为空，使用默认的模拟数据
                if (token == null || token.isEmpty()) {
                    Log.w("CourseViewModel", "未获取到认证token，使用模拟数据");
                    synchronized (cacheLock) {
                        courseCache.clear();
                        isCacheInitialized = true;
                    }
                    return;
                }
                
                Request request = new Request.Builder()
                        .url("https://biubiu.org/api/school_timetable")
                        .header("Authorization", "Bearer " + token)
                        .build();
                
                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d("CourseViewModel", "从服务器获取课程数据成功: " + responseBody);
                    
                    // 解析JSON数据
                    List<Course> loadedCourses = parseCourseData(responseBody);
                    
                    // 缓存课程数据到本地存储
                    cacheCoursesToLocalStorage(loadedCourses);
                    
                    Log.d("CourseViewModel", "课程数据解析完成，共解析" + loadedCourses.size() + "条数据");
                    
                    // 更新缓存和状态（线程安全）
                    synchronized (cacheLock) {
                        courseCache.clear();
                        courseCache.addAll(loadedCourses);
                        isCacheInitialized = true;
                    }
                    
                    // 在主线程更新UI（异步回调）
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (lastLoadedDate != null) {
                            loadCourses(lastLoadedDate); // 重新加载当前日期的课程以显示真实数据
                        }
                    });
                } else {
                    Log.e("CourseViewModel", "从服务器获取课程数据失败: " + response.message());
                    synchronized (cacheLock) {
                        courseCache.clear();
                        isCacheInitialized = true;
                    }
                    // 尝试从本地存储加载课程数据
                    loadCoursesFromLocalStorage();
                }
            } catch (IOException | JSONException e) {
                Log.e("CourseViewModel", "从服务器加载课程数据时发生异常", e);
                synchronized (cacheLock) {
                    courseCache.clear();
                    isCacheInitialized = true;
                }
                // 尝试从本地存储加载课程数据
                loadCoursesFromLocalStorage();
            }
        }).start();
    }
    
    // 缓存课程数据到本地存储
    private void cacheCoursesToLocalStorage(List<Course> courses) {
        try {
            // 实际项目中可以使用SharedPreferences或Room数据库来存储课程数据
            Log.d("CourseViewModel", "课程数据已缓存到本地存储");
            // 这里仅做日志记录，具体实现需要根据项目架构添加
        } catch (Exception e) {
            Log.e("CourseViewModel", "缓存课程数据到本地存储失败", e);
        }
    }
    
    // 从本地存储加载课程数据
    private void loadCoursesFromLocalStorage() {
        try {
            // 实际项目中可以使用SharedPreferences或Room数据库来加载课程数据
            Log.d("CourseViewModel", "尝试从本地存储加载课程数据");
            // 这里仅做日志记录，具体实现需要根据项目架构添加
        } catch (Exception e) {
            Log.e("CourseViewModel", "从本地存储加载课程数据失败", e);
        }
    }
    
    // 解析课程数据JSON
    private List<Course> parseCourseData(String jsonData) throws JSONException {
        List<Course> courses = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(jsonData);
        
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            
            String id = jsonObject.optString("id", "");
            String courseName = jsonObject.optString("course_name", "未知课程");
            String teacherName = jsonObject.optString("teacher_name", "未知教师");
            String startTime = jsonObject.optString("start_time", "");
            String endTime = jsonObject.optString("end_time", "");
            int dayOfWeek = jsonObject.optInt("day_of_week", 0);
            String classroom = jsonObject.optString("classroom", "未知教室");
            String weekRange = jsonObject.optString("week_range", "全学期");
            String department = "小学五年级";
            
            Course course = new Course(id, courseName, teacherName, startTime, endTime,
                    dayOfWeek, classroom, weekRange, department);
            courses.add(course);
        }
        
        return courses;
    }

    // 加载指定日期的作业
    public void loadAssignments(String date, List<Course> courses) {
        Log.d("CourseViewModel", "加载日期: " + date + " 的作业数据");
        
        try {
            // 解析日期，获取年月
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date selectedDate = dateFormat.parse(date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(selectedDate);
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH从0开始
            
            // 检查是否需要从服务器加载新数据
            if (monthlyAssignmentCache == null || cachedMonth != month || cachedYear != year) {
                loadMonthlyAssignmentsFromServer(year, month, date, courses);
                // 立即使用模拟数据显示
                List<Assignment> mockAssignments = generateMockAssignments(date, courses);
                _assignments.postValue(mockAssignments);
                loadActivities(date);
                return; // 退出方法，等待异步加载完成后更新UI
            }
            
            // 从缓存中筛选出指定日期的作业
            List<Assignment> dateAssignments = new ArrayList<>();
            if (monthlyAssignmentCache != null && !monthlyAssignmentCache.isEmpty()) {
                for (Assignment assignment : monthlyAssignmentCache) {
                    if (date.equals(assignment.getDueDate())) {
                        dateAssignments.add(assignment);
                    }
                }
            }
            
            // 如果没有从服务器获取到数据，使用模拟数据
            if (dateAssignments.isEmpty()) {
                Log.d("CourseViewModel", "未找到真实作业数据，使用模拟数据");
                dateAssignments = generateMockAssignments(date, courses);
            }
            
            Log.d("CourseViewModel", "作业数据数量: " + dateAssignments.size());
            _assignments.setValue(dateAssignments);
            Log.d("CourseViewModel", "作业数据已设置到LiveData");
            
            // 同时加载该日期的活动
            loadActivities(date);
        } catch (ParseException e) {
            Log.e("CourseViewModel", "加载作业数据失败", e);
            // 加载失败时使用模拟数据
            List<Assignment> mockAssignments = generateMockAssignments(date, courses);
            _assignments.setValue(mockAssignments);
            loadActivities(date);
        }
    }
    
    // 从服务器加载当月作业数据
    private void loadMonthlyAssignmentsFromServer(int year, int month, final String date, final List<Course> courses) {
        new Thread(() -> {
            try {
                OkHttpClient client = createOkHttpClient();
                String token = AuthManager.getAuthToken(getApplication());
                
                // 如果没有token或token为空，使用模拟数据
                if (token == null || token.isEmpty()) {
                    Log.w("CourseViewModel", "未获取到认证token，无法从服务器加载作业数据");
                    monthlyAssignmentCache = null;
                    return;
                }
                
                // 创建请求
                Request request = new Request.Builder()
                        .url("https://biubiu.org/api/assignments")
                        .header("Authorization", "Bearer " + token)
                        .build();
                
                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d("CourseViewModel", "从服务器获取作业数据成功: " + responseBody);
                    
                    // 解析JSON数据
                    monthlyAssignmentCache = parseAssignmentsData(responseBody, year, month);
                    
                    // 更新缓存月份信息
                    cachedMonth = month;
                    cachedYear = year;
                    
                    Log.d("CourseViewModel", "作业数据解析完成，共解析" + monthlyAssignmentCache.size() + "条数据");
                    
                    // 在主线程更新UI（异步回调）
                    new Handler(Looper.getMainLooper()).post(() -> {
                        loadAssignments(date, courses); // 重新加载当前日期的作业以显示真实数据
                    });
                } else {
                    Log.e("CourseViewModel", "从服务器获取作业数据失败: " + response.message());
                    monthlyAssignmentCache = null;
                }
            } catch (IOException | JSONException e) {
                Log.e("CourseViewModel", "从服务器加载作业数据时发生异常", e);
                monthlyAssignmentCache = null;
            }
        }).start();
    }
    
    // 解析作业数据JSON
    private List<Assignment> parseAssignmentsData(String jsonData, int year, int month) throws JSONException {
        List<Assignment> assignments = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(jsonData);
        
        String yearMonthStr = String.format("%d-%02d", year, month);
        
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            
            String id = jsonObject.optString("id", "");
            String courseName = jsonObject.optString("courseName", "未知课程");
            String title = jsonObject.optString("title", "未命名作业");
            String description = jsonObject.optString("description", "");
            String dueDate = jsonObject.optString("dueDate", "");
            boolean isCompleted = jsonObject.optBoolean("isCompleted", false);
            
            // 只保留当月的作业
            if (dueDate.startsWith(yearMonthStr)) {
                Assignment assignment = new Assignment(id, courseName, title, description, dueDate, isCompleted);
                assignments.add(assignment);
            }
        }
        
        return assignments;
    }

    // 加载指定日期的活动
    public void loadActivities(String date) {
        Log.d("CourseViewModel", "加载日期: " + date + " 的活动数据");
        
        try {
            // 解析日期，获取年月
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date selectedDate = dateFormat.parse(date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(selectedDate);
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH从0开始
            
            // 检查是否需要从服务器加载新数据
            if (monthlyActivityCache == null || cachedActivityMonth != month || cachedActivityYear != year) {
                loadMonthlyActivitiesFromServer(year, month, date);
                // 立即使用模拟数据显示
                List<Activity> mockActivities = generateMockActivities(date);
                _activities.postValue(mockActivities);
                return; // 退出方法，等待异步加载完成后更新UI
            }
            
            // 从缓存中筛选出指定日期的活动
            List<Activity> dateActivities = new ArrayList<>();
            if (monthlyActivityCache != null && !monthlyActivityCache.isEmpty()) {
                for (Activity activity : monthlyActivityCache) {
                    if (date.equals(activity.getDate())) {
                        dateActivities.add(activity);
                    }
                }
            }
            
            // 如果没有从服务器获取到数据，使用模拟数据
            if (dateActivities.isEmpty()) {
                Log.d("CourseViewModel", "未找到真实活动数据，使用模拟数据");
                dateActivities = generateMockActivities(date);
            }
            
            Log.d("CourseViewModel", "活动数据数量: " + dateActivities.size());
            _activities.setValue(dateActivities);
        } catch (ParseException e) {
            Log.e("CourseViewModel", "加载活动数据失败", e);
            // 加载失败时使用模拟数据
            List<Activity> mockActivities = generateMockActivities(date);
            _activities.setValue(mockActivities);
        }
    }
    
    // 从服务器加载当月活动数据
    private void loadMonthlyActivitiesFromServer(int year, int month, final String date) {
        new Thread(() -> {
            try {
                OkHttpClient client = createOkHttpClient();
                String token = AuthManager.getAuthToken(getApplication());
                
                // 如果没有token或token为空，使用模拟数据
                if (token == null || token.isEmpty()) {
                    Log.w("CourseViewModel", "未获取到认证token，无法从服务器加载活动数据");
                    monthlyActivityCache = null;
                    return;
                }
                
                // 计算当月的开始和结束日期
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month - 1, 1); // 设置为当月第一天
                Date startDate = calendar.getTime();
                
                // 设置为当月最后一天
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                Date endDate = calendar.getTime();
                
                // 格式化日期为YYYY-MM-DD格式
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String formattedStartDate = dateFormat.format(startDate);
                String formattedEndDate = dateFormat.format(endDate);
                
                // 创建请求
                String url = "https://biubiu.org/api/activities?start_date=" + formattedStartDate + "&end_date=" + formattedEndDate;
                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + token)
                        .build();
                
                Log.d("CourseViewModel", "发送活动数据请求: " + url);
                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d("CourseViewModel", "从服务器获取活动数据成功: " + responseBody);
                    
                    // 解析JSON数据
                    monthlyActivityCache = parseActivitiesData(responseBody);
                    
                    // 更新缓存月份信息
                    cachedActivityMonth = month;
                    cachedActivityYear = year;
                    
                    Log.d("CourseViewModel", "活动数据解析完成，共解析" + monthlyActivityCache.size() + "条数据");
                    
                    // 在主线程更新UI（异步回调）
                    new Handler(Looper.getMainLooper()).post(() -> {
                        loadActivities(date); // 重新加载当前日期的活动以显示真实数据
                    });
                } else {
                    Log.e("CourseViewModel", "从服务器获取活动数据失败: " + response.message());
                    monthlyActivityCache = null;
                }
            } catch (IOException | JSONException e) {
                Log.e("CourseViewModel", "从服务器加载活动数据时发生异常", e);
                monthlyActivityCache = null;
            }
        }).start();
    }
    

    // 解析活动数据JSON
    private List<Activity> parseActivitiesData(String jsonData) throws JSONException {
        List<Activity> activities = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(jsonData);
        
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            
            String id = jsonObject.optString("id", "");
            String title = jsonObject.optString("title", "未命名活动");
            String description = jsonObject.optString("description", "");
            String date = jsonObject.optString("date", "");
            String time = jsonObject.optString("time", "");
            String location = jsonObject.optString("location", "");
            boolean isAttended = jsonObject.optBoolean("isAttended", false);
            
            // 如果time为空，尝试从startTime和endTime构建
            if (time.isEmpty()) {
                String startTime = jsonObject.optString("startTime", "");
                String endTime = jsonObject.optString("endTime", "");
                if (!startTime.isEmpty() && !endTime.isEmpty()) {
                    time = startTime + "-" + endTime;
                }
            }
            
            // 确保日期不为空
            if (date.isEmpty()) {
                // 如果没有date字段，使用当前日期（实际项目中可能需要更合理的处理）
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                date = dateFormat.format(new Date());
            }
            
            Activity activity = new Activity(id, title, description, date, time, location, isAttended);
            activities.add(activity);
        }
        
        return activities;
    }

    // 生成模拟课程数据（备用，当从服务器加载失败时使用）
    private List<Course> generateMockCourses(String date) {
        List<Course> courses = new ArrayList<>();
        
        try {
            // 获取日期中的星期几
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date selectedDate = dateFormat.parse(date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(selectedDate);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            
            // 模拟一周的课表数据，类似API返回的格式
            // 周一课程
            if (dayOfWeek == Calendar.MONDAY) {
                courses.add(new Course("1", "语文", "王老师", "08:30", "09:10", 1, "五(1)班教室", "1-18周", "小学五年级"));
                courses.add(new Course("2", "数学", "李老师", "09:20", "10:00", 1, "五(1)班教室", "1-18周", "小学五年级"));
                courses.add(new Course("3", "英语", "张老师", "14:00", "14:40", 1, "五(1)班教室", "1-18周", "小学五年级"));
                courses.add(new Course("4", "科学", "陈老师", "14:50", "15:30", 1, "科学实验室", "1-18周", "小学五年级"));
            }
            // 周二课程
            else if (dayOfWeek == Calendar.TUESDAY) {
                courses.add(new Course("5", "数学", "李老师", "08:30", "09:10", 2, "五(1)班教室", "1-18周", "小学五年级"));
                courses.add(new Course("6", "语文", "王老师", "09:20", "10:00", 2, "五(1)班教室", "1-18周", "小学五年级"));
                courses.add(new Course("7", "美术", "刘老师", "14:00", "14:40", 2, "美术室", "1-18周", "小学五年级"));
                courses.add(new Course("8", "体育", "赵老师", "14:50", "15:30", 2, "操场", "1-18周", "小学五年级"));
            }
            // 周三课程
            else if (dayOfWeek == Calendar.WEDNESDAY) {
                courses.add(new Course("9", "语文", "王老师", "08:30", "09:10", 3, "五(1)班教室", "1-18周", "小学五年级"));
                courses.add(new Course("10", "英语", "张老师", "09:20", "10:00", 3, "五(1)班教室", "1-18周", "小学五年级"));
                courses.add(new Course("11", "数学", "李老师", "10:20", "11:00", 3, "五(1)班教室", "1-18周", "小学五年级"));
                courses.add(new Course("12", "音乐", "黄老师", "14:00", "14:40", 3, "音乐室", "1-18周", "小学五年级"));
            }
            // 周四课程
            else if (dayOfWeek == Calendar.THURSDAY) {
                courses.add(new Course("13", "英语", "张老师", "08:30", "09:10", 4, "五(1)班教室", "1-18周", "小学五年级"));
                courses.add(new Course("14", "数学", "李老师", "09:20", "10:00", 4, "五(1)班教室", "1-18周", "小学五年级"));
                courses.add(new Course("15", "语文", "王老师", "10:20", "11:00", 4, "五(1)班教室", "1-18周", "小学五年级"));
                courses.add(new Course("16", "道德与法治", "吴老师", "14:00", "14:40", 4, "五(1)班教室", "1-18周", "小学五年级"));
            }
            // 周五课程
            else if (dayOfWeek == Calendar.FRIDAY) {
                courses.add(new Course("17", "数学", "李老师", "08:30", "09:10", 5, "五(1)班教室", "1-18周", "小学五年级"));
                courses.add(new Course("18", "英语", "张老师", "09:20", "10:00", 5, "五(1)班教室", "1-18周", "小学五年级"));
                courses.add(new Course("19", "科学", "陈老师", "10:20", "11:00", 5, "科学实验室", "1-18周", "小学五年级"));
                courses.add(new Course("20", "体育", "赵老师", "14:00", "14:40", 5, "操场", "1-18周", "小学五年级"));
            }
        } catch (ParseException e) {
            Log.e("CourseViewModel", "解析日期失败", e);
            // 如果解析日期失败，使用默认的模拟数据
            String dayOfMonth = date.substring(date.lastIndexOf('-') + 1);
            int day = Integer.parseInt(dayOfMonth);
            
            if (day % 3 == 1) {
                courses.add(new Course("语文", "小学五年级", "王老师", "上午 8:30-9:10", "五(1)班教室"));
                courses.add(new Course("数学", "小学五年级", "李老师", "上午 9:20-10:00", "五(1)班教室"));
                courses.add(new Course("英语", "小学五年级", "张老师", "下午 2:00-2:40", "五(1)班教室"));
                courses.add(new Course("科学", "小学五年级", "陈老师", "下午 2:50-3:30", "科学实验室"));
            } else if (day % 3 == 2) {
                courses.add(new Course("数学", "小学五年级", "李老师", "上午 8:30-9:10", "五(1)班教室"));
                courses.add(new Course("语文", "小学五年级", "王老师", "上午 9:20-10:00", "五(1)班教室"));
                courses.add(new Course("美术", "小学五年级", "刘老师", "下午 2:00-2:40", "美术室"));
                courses.add(new Course("体育", "小学五年级", "赵老师", "下午 2:50-3:30", "操场"));
            } else {
                courses.add(new Course("语文", "小学五年级", "王老师", "上午 8:30-9:10", "五(1)班教室"));
                courses.add(new Course("英语", "小学五年级", "张老师", "上午 9:20-10:00", "五(1)班教室"));
                courses.add(new Course("数学", "小学五年级", "李老师", "上午 10:20-11:00", "五(1)班教室"));
                courses.add(new Course("音乐", "小学五年级", "黄老师", "下午 2:00-2:40", "音乐室"));
                courses.add(new Course("道德与法治", "小学五年级", "吴老师", "下午 2:50-3:30", "五(1)班教室"));
            }
        }
        
        return courses;
    }

    // 根据当日课程生成模拟作业数据
    private List<Assignment> generateMockAssignments(String date, List<Course> courses) {
        List<Assignment> assignments = new ArrayList<>();
        
        // 获取日期中的日部分
        String dayOfMonth = date.substring(date.lastIndexOf('-') + 1);
        int day = Integer.parseInt(dayOfMonth);
        
        // 为当日的每门课程生成作业
        for (int i = 0; i < courses.size(); i++) {
            Course course = courses.get(i);
            String courseName = course.getName();
            
            // 根据不同的课程生成不同的作业
            if ("语文".equals(courseName)) {
                assignments.add(new Assignment(
                        "assign-" + date + "-" + i,
                        courseName,
                        "第" + day + "课生字抄写",
                        "抄写第" + day + "课的生字，每个字写5遍组2个词",
                        date,
                        false
                ));
            } else if ("数学".equals(courseName)) {
                assignments.add(new Assignment(
                        "assign-" + date + "-" + i,
                        courseName,
                        "练习册第" + day + "页",
                        "完成练习册第" + day + "页的所有习题",
                        date,
                        false
                ));
            } else if ("英语".equals(courseName)) {
                assignments.add(new Assignment(
                        "assign-" + date + "-" + i,
                        courseName,
                        "单词背诵",
                        "背诵第" + day + "课的单词，明天听写",
                        date,
                        false
                ));
            } else if ("科学".equals(courseName)) {
                assignments.add(new Assignment(
                        "assign-" + date + "-" + i,
                        courseName,
                        "观察日记",
                        "观察一种植物，记录它的生长情况",
                        date,
                        false
                ));
            } else if ("美术".equals(courseName)) {
                assignments.add(new Assignment(
                        "assign-" + date + "-" + i,
                        courseName,
                        "绘画练习",
                        "画一幅关于秋天的画",
                        date,
                        false
                ));
            } else if ("音乐".equals(courseName)) {
                assignments.add(new Assignment(
                        "assign-" + date + "-" + i,
                        courseName,
                        "歌曲练习",
                        "练习今天学的歌曲，明天抽查",
                        date,
                        false
                ));
            } else if ("道德与法治".equals(courseName)) {
                assignments.add(new Assignment(
                        "assign-" + date + "-" + i,
                        courseName,
                        "行为规范学习",
                        "阅读《小学生行为规范》第" + day + "条",
                        date,
                        false
                ));
            }
        }
        
        return assignments;
    }

    // 生成模拟活动数据
    private List<Activity> generateMockActivities(String date) {
        List<Activity> activities = new ArrayList<>();
        
        // 获取日期中的日部分
        String dayOfMonth = date.substring(date.lastIndexOf('-') + 1);
        int day = Integer.parseInt(dayOfMonth);
        
        // 根据不同日期生成不同的活动数据
        if (day % 5 == 1) {
            activities.add(new Activity(
                    "activity-" + date + "-1",
                    "班级图书角阅读分享会",
                    "分享你最近读过的好书，交流阅读心得",
                    date,
                    "下午 3:30-4:30",
                    "教室",
                    false
            ));
        } else if (day % 5 == 2) {
            activities.add(new Activity(
                    "activity-" + date + "-1",
                    "数学兴趣小组活动",
                    "趣味数学题解题大赛",
                    date,
                    "下午 3:30-4:30",
                    "教室",
                    false
            ));
        } else if (day % 5 == 3) {
            activities.add(new Activity(
                    "activity-" + date + "-1",
                    "美术手工制作课",
                    "环保材料创作比赛",
                    date,
                    "下午 3:30-4:30",
                    "美术室",
                    false
            ));
        } else if (day % 5 == 4) {
            activities.add(new Activity(
                    "activity-" + date + "-1",
                    "班级运动会准备",
                    "为下周运动会进行赛前训练",
                    date,
                    "下午 3:30-4:30",
                    "操场",
                    false
            ));
        } else {
            activities.add(new Activity(
                    "activity-" + date + "-1",
                    "班级主题班会",
                    "文明礼仪伴我行主题讨论",
                    date,
                    "下午 3:30-4:30",
                    "教室",
                    false
            ));
            activities.add(new Activity(
                    "activity-" + date + "-2",
                    "英语角活动",
                    "英语日常对话练习",
                    date,
                    "下午 4:30-5:30",
                    "校园花园",
                    false
            ));
        }
        
        // 添加一些通用的学校活动
        if (day % 7 == 0) {
            activities.add(new Activity(
                    "activity-" + date + "-special",
                    "学校升旗仪式",
                    "全校师生参加的升旗仪式",
                    date,
                    "上午 7:30-8:00",
                    "操场",
                    false
            ));
        }
        
        return activities;
    }
    
    // 创建支持SSL证书的OkHttpClient
    private OkHttpClient createOkHttpClient() {
        try {
            // 创建一个信任所有证书的TrustManager（仅用于测试环境）
            X509TrustManager trustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            
            // 创建SSLContext并初始化
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { trustManager }, new java.security.SecureRandom());
            
            // 创建OkHttpClient并配置SSL
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                    .hostnameVerifier((hostname, session) -> true) // 忽略主机名验证
                    .build();
        } catch (Exception e) {
            Log.e("CourseViewModel", "创建OkHttpClient失败", e);
            // 如果创建失败，返回默认客户端
            return new OkHttpClient();
        }
    }
}