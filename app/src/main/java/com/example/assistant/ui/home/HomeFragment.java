package com.example.assistant.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assistant.R;
import com.example.assistant.adapter.MessageAdapter;
import com.example.assistant.databinding.FragmentHomeBinding;
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

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private RecyclerView messagesRecyclerView;
    private EditText messageInput;
    private TextView statusText;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    private WebSocket webSocket;
    private OkHttpClient client;
    // WebSocket服务器URL
    private static final String WEB_SOCKET_URL_BASE = "wss://biubiu.org:443/ws";
    // 认证Token (硬编码用于测试)
    private static final String WEB_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMSIsImV4cCI6MTc1OTMwODc5OX0.Yrcdvmq65GtFJmfCxQ-DpiGeU60FjB66BVvs9wCGHl4";
    // 完整的WebSocket连接URL，包含认证token
    private static final String WEB_SOCKET_URL = WEB_SOCKET_URL_BASE + "?token=" + WEB_TOKEN;
    private Handler handler;

    // 消息状态跟踪
    private boolean isStreaming = false;
    private int currentAiThinkingMessageId = -1;
    private int currentAiMessageId = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 初始化视图组件
        messagesRecyclerView = binding.messagesRecyclerView;
        messageInput = binding.messageInput;
        statusText = binding.statusText;

        // 初始化消息列表和适配器
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);

        // 设置RecyclerView
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        messagesRecyclerView.setAdapter(messageAdapter);

        // 添加欢迎消息
        //addMessage("Welcome to PocketFlow Chat!", Message.TYPE_AI);

        // 初始化Handler
        handler = new Handler(Looper.getMainLooper());

        // 设置发送按钮点击事件
        binding.sendButton.setOnClickListener(v -> sendMessage());

        // 设置输入框文本变化监听
        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 输入框有内容时启用发送按钮
                binding.sendButton.setEnabled(s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 添加键盘和布局变化监听器，动态调整输入区域位置
        setupKeyboardVisibilityListener(root);

        // 初始化WebSocket连接
        initWebSocket();

        return root;
    }

    // 设置键盘可见性监听器，动态调整输入区域位置
    private void setupKeyboardVisibilityListener(final View rootView) {
        // 使用DecorView来监听布局变化，这是检测键盘状态的更可靠方式
        View decorView = getActivity().getWindow().getDecorView();
        decorView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (isAdded() && getContext() != null) {
                    // 获取窗口可见区域的rect
                    Rect rect = new Rect();
                    decorView.getWindowVisibleDisplayFrame(rect);
                    
                    // 计算屏幕高度和可见区域高度
                    int screenHeight = decorView.getRootView().getHeight();
                    int visibleHeight = rect.bottom - rect.top;
                    
                    // 计算差值，判断键盘是否弹出
                    int heightDiff = screenHeight - visibleHeight;
                    
                    // 如果差值超过屏幕高度的1/3，认为键盘弹出
                    boolean isKeyboardVisible = heightDiff > screenHeight / 3;
                    
                    // 获取底部导航栏高度
                    int navBarHeight = getNavigationBarHeight();
                    
                    // 根据键盘状态调整输入区域的底部边距
                    ViewGroup.MarginLayoutParams params = 
                        (ViewGroup.MarginLayoutParams) binding.inputArea.getLayoutParams();
                    
                    if (isKeyboardVisible) {
                        // 键盘弹出时，减少底部边距，确保输入框可见
                        params.bottomMargin = heightDiff + navBarHeight - 60; // 小边距
                    } else {
                        // 键盘收起时，增加底部边距，避开导航栏
                        params.bottomMargin = navBarHeight + 100; // 导航栏高度+小边距
                    }
                    
                    binding.inputArea.requestLayout();
                }
            }
        });
    }

    // 获取导航栏高度
    private int getNavigationBarHeight() {
        if (getContext() == null) return 0;
        
        WindowInsets insets = getActivity().getWindow().getDecorView().getRootWindowInsets();
        if (insets != null) {
            return insets.getStableInsetBottom();
        }
        
        // 兼容旧版本的备用方法
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        
        return 80; // 默认值
    }

    private void initWebSocket() {
        // 创建自定义OkHttpClient，配置SSL证书验证
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        
        // 添加SSL证书信任逻辑，解决Trust anchor not found问题
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
                    statusText.setText("(Connected)");
                    //addMessage("Connected to server", Message.TYPE_AI_THINK);
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
                        isStreaming = true;
                        
                        // 创建思考消息
                        currentAiThinkingMessageId = addMessage("", Message.TYPE_AI_THINK);
                        currentAiMessageId = -1;
                        
                        statusText.setText("Generating response...");
                        binding.sendButton.setEnabled(false);
                        
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
                        isStreaming = false;
                        currentAiThinkingMessageId = -1;
                        currentAiMessageId = -1;
                        binding.sendButton.setEnabled(true);
                        statusText.setText("(Connected)");
                        messageInput.requestFocus();
                        
                    } else if ("status".equals(type)) {
                        // 更新状态消息
                        statusText.setText(data.getString("content"));
                        
                    } else if ("confirm".equals(type)) {
                        // 处理确认消息（可以根据需要实现SQL确认功能）
                        String confirmContent = data.getString("content");
                        JSONObject confirmData = new JSONObject(confirmContent);
                        String conversationId = confirmData.getString("conversation_id");
                        
                        // 这里可以添加确认对话框的逻辑
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
                    statusText.setText("(Disconnected)");
                    addMessage("Disconnected from server", Message.TYPE_AI_THINK);
                });
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
                handler.post(() -> {
                    statusText.setText("(Connection Failed)");
                    addMessage("Failed to connect to server: " + t.getMessage(), Message.TYPE_AI_THINK);
                    Toast.makeText(getContext(), "Connection failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        };

        webSocket = client.newWebSocket(request, webSocketListener);
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (!message.isEmpty() && !isStreaming) {
            // 添加用户消息到列表
            addMessage(message, Message.TYPE_USER);

            // 清空输入框
            messageInput.setText("");

            // 通过WebSocket发送格式化的JSON消息
            if (webSocket != null) {
                try {
                    JSONObject messageObj = new JSONObject();
                    messageObj.put("type", "message");
                    messageObj.put("content", message);
                    webSocket.send(messageObj.toString());
                    statusText.setText("Sending...");
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
        messageList.add(new Message(content, type));
        int newMessageIndex = messageList.size() - 1;
        messageAdapter.notifyItemInserted(newMessageIndex);
        // 滚动到底部
        messagesRecyclerView.scrollToPosition(newMessageIndex);
        return newMessageIndex;
    }
    
    // 更新现有消息的内容
    private void updateMessage(int messageId, String content) {
        if (messageId >= 0 && messageId < messageList.size()) {
            Message message = messageList.get(messageId);
            String currentContent = message.getContent();
            
            // 检查是否是首次更新，如果是则直接设置内容，否则追加内容
            if (currentContent.isEmpty()) {
                message.setContent(content);
            } else {
                message.setContent(currentContent + content);
            }
            
            messageAdapter.notifyItemChanged(messageId);
            // 滚动到底部
            messagesRecyclerView.scrollToPosition(messageList.size() - 1);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 关闭WebSocket连接
        if (webSocket != null) {
            webSocket.close(1000, "Fragment destroyed");
        }
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
        binding = null;
    }
}