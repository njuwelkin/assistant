package com.example.assistant.ui.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.assistant.model.Message;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class ChatViewModel extends ViewModel {
    private static final String TAG = "ChatViewModel";
    
    // WebSocket服务器URL
    private static final String WEB_SOCKET_URL_BASE = "wss://biubiu.org:443/ws";
    // 认证Token (硬编码用于测试)
    private static final String WEB_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMSIsImV4cCI6MTc1OTMwODc5OX0.Yrcdvmq65GtFJmfCxQ-DpiGeU60FjB66BVvs9wCGHl4";
    // 完整的WebSocket连接URL，包含认证token
    private static final String WEB_SOCKET_URL = WEB_SOCKET_URL_BASE + "?token=" + WEB_TOKEN;
    
    // 数据存储
    private final MutableLiveData<List<Message>> messageListLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> connectionStatusLiveData = new MutableLiveData<>("(Connecting...)");
    private final MutableLiveData<Boolean> isStreamingLiveData = new MutableLiveData<>(false);
    
    // WebSocket相关
    private WebSocket webSocket;
    private OkHttpClient client;
    private Handler handler;
    
    // 消息状态跟踪
    private int currentAiThinkingMessageId = -1;
    private int currentAiMessageId = -1;
    private boolean isReconnecting = false;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    
    public ChatViewModel() {
        // 初始化Handler
        handler = new Handler(Looper.getMainLooper());
        
        // 初始化WebSocket连接
        initWebSocket();
    }
    
    // 获取消息列表的LiveData
    public LiveData<List<Message>> getMessageListLiveData() {
        return messageListLiveData;
    }
    
    // 获取连接状态的LiveData
    public LiveData<String> getConnectionStatusLiveData() {
        return connectionStatusLiveData;
    }
    
    // 获取是否正在流式处理的LiveData
    public LiveData<Boolean> getIsStreamingLiveData() {
        return isStreamingLiveData;
    }
    
    private void initWebSocket() {
        // 如果已经连接，则先关闭
        if (webSocket != null) {
            webSocket.close(1000, "Reconnecting");
        }
        
        // 创建自定义OkHttpClient，配置SSL证书验证
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        
        // 添加SSL证书信任逻辑
        try {
            // 获取SSL上下文
            SSLContext sslContext = SSLContext.getInstance("TLS");
            
            // 创建信任所有证书的TrustManager
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                    
                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                    
                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
            };
            
            // 初始化SSL上下文
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            // 添加SSL套接字工厂
            clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager)trustAllCerts[0]);
            
            // 忽略主机名验证
            clientBuilder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        client = clientBuilder.build();
        Request request = new Request.Builder().url(WEB_SOCKET_URL).build();
        WebSocketListener webSocketListener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);
                handler.post(() -> {
                    connectionStatusLiveData.setValue("(Connected)");
                    isReconnecting = false;
                    reconnectAttempts = 0;
                });
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
                handler.post(() -> {
                    try {
                        // 解析JSON消息
                        JSONObject data = new JSONObject(text);
                        String type = data.getString("type");
                        
                        if ("start".equals(type)) {
                            // 开始流式响应
                            isStreamingLiveData.setValue(true);
                            
                            // 创建思考消息
                            currentAiThinkingMessageId = addMessage("", Message.TYPE_AI_THINK);
                            currentAiMessageId = -1;
                            
                            connectionStatusLiveData.setValue("Generating response...");
                            
                        } else if ("chunk".equals(type)) {
                            // 处理消息片段
                            String content = data.getString("content");
                            boolean isThinking = data.optBoolean("is_thinking", false);
                            
                            if (content != null && !content.isEmpty()) {
                                // 根据is_thinking属性决定更新哪个消息
                                if (isThinking) {
                                    // 处理思考消息
                                    if (currentAiThinkingMessageId == -1) {
                                        // 如果还没有思考消息，则创建一个
                                        currentAiThinkingMessageId = addMessage(content, Message.TYPE_AI_THINK);
                                    } else {
                                        // 更新现有思考消息
                                        updateMessage(currentAiThinkingMessageId, content);
                                    }
                                } else {
                                    // 处理普通AI消息
                                    if (currentAiMessageId == -1) {
                                        // 如果还没有AI消息，则创建一个
                                        currentAiMessageId = addMessage(content, Message.TYPE_AI);
                                    } else {
                                        // 更新现有AI消息
                                        updateMessage(currentAiMessageId, content);
                                    }
                                }
                            }
                            
                        } else if ("end".equals(type)) {
                            // 结束流式响应
                            isStreamingLiveData.setValue(false);
                            currentAiThinkingMessageId = -1;
                            currentAiMessageId = -1;
                            connectionStatusLiveData.setValue("(Connected)");
                            
                        } else if ("status".equals(type)) {
                            // 更新状态消息
                            connectionStatusLiveData.setValue(data.getString("content"));
                            
                        } else if ("confirm".equals(type)) {
                            // 处理确认消息
                            String confirmContent = data.getString("content");
                            JSONObject confirmData = new JSONObject(confirmContent);
                            String conversationId = confirmData.getString("conversation_id");
                            
                            // 添加确认消息
                            addMessage("需要执行SQL操作，请确认。会话ID: " + conversationId, Message.TYPE_AI_THINK);
                        } else {
                            // 处理其他类型的消息或原始文本消息
                            addMessage(text, Message.TYPE_AI);
                        }
                        
                    } catch (JSONException e) {
                        e.printStackTrace();
                        // 如果不是JSON格式，直接显示消息
                        addMessage(text, Message.TYPE_AI);
                    }
                });
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                super.onClosing(webSocket, code, reason);
                handler.post(() -> {
                    connectionStatusLiveData.setValue("(Disconnected)");
                    addMessage("Disconnected from server", Message.TYPE_AI_THINK);
                    
                    // 尝试重新连接
                    if (!isReconnecting && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                        scheduleReconnect();
                    }
                });
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
                handler.post(() -> {
                    connectionStatusLiveData.setValue("(Connection Failed)");
                    addMessage("Failed to connect to server: " + t.getMessage(), Message.TYPE_AI_THINK);
                    
                    // 尝试重新连接
                    if (!isReconnecting && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                        scheduleReconnect();
                    }
                });
            }
        };

        webSocket = client.newWebSocket(request, webSocketListener);
    }
    
    // 安排重新连接
    private void scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "Max reconnection attempts reached");
            return;
        }
        
        isReconnecting = true;
        reconnectAttempts++;
        
        // 指数退避算法，等待时间逐渐增加
        long waitTime = (long) Math.pow(2, reconnectAttempts) * 1000;
        waitTime = Math.min(waitTime, 30000); // 最多等待30秒
        
        handler.postDelayed(() -> {
            Log.d(TAG, "Attempting to reconnect (" + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS + ")");
            initWebSocket();
        }, waitTime);
    }
    
    // 发送消息
    public void sendMessage(String message) {
        if (message != null && !message.isEmpty() && !isStreamingLiveData.getValue()) {
            // 添加用户消息到列表
            addMessage(message, Message.TYPE_USER);
            
            // 通过WebSocket发送格式化的JSON消息
            if (webSocket != null) {
                try {
                    JSONObject messageObj = new JSONObject();
                    messageObj.put("type", "message");
                    messageObj.put("content", message);
                    webSocket.send(messageObj.toString());
                    connectionStatusLiveData.setValue("Sending...");
                } catch (JSONException e) {
                    e.printStackTrace();
                    // 如果JSON格式化失败，直接发送原始消息
                    webSocket.send(message);
                }
            }
        }
    }
    
    // 添加消息并返回索引
    private int addMessage(String content, int type) {
        List<Message> currentList = messageListLiveData.getValue();
        if (currentList == null) {
            currentList = new ArrayList<>();
        }
        
        currentList.add(new Message(content, type));
        int newMessageIndex = currentList.size() - 1;
        
        // 使用新的列表对象触发LiveData更新
        List<Message> updatedList = new ArrayList<>(currentList);
        messageListLiveData.setValue(updatedList);
        
        return newMessageIndex;
    }
    
    // 更新现有消息的内容
    private void updateMessage(int messageId, String content) {
        List<Message> currentList = messageListLiveData.getValue();
        if (currentList != null && messageId >= 0 && messageId < currentList.size()) {
            Message message = currentList.get(messageId);
            String currentContent = message.getContent();
            
            // 检查是否是首次更新，如果是则直接设置内容，否则追加内容
            if (currentContent.isEmpty()) {
                message.setContent(content);
            } else {
                message.setContent(currentContent + content);
            }
            
            // 使用新的列表对象触发LiveData更新
            List<Message> updatedList = new ArrayList<>(currentList);
            messageListLiveData.setValue(updatedList);
        }
    }
    
    // 断开WebSocket连接
    public void disconnectWebSocket() {
        if (webSocket != null) {
            webSocket.close(1000, "Disconnecting");
            webSocket = null;
        }
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // 断开WebSocket连接
        disconnectWebSocket();
        
        // 清除Handler中的所有回调
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}