package com.example.assistant.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.ArrayList;
import java.util.List;

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
    private static final String WEB_SOCKET_URL = "ws://10.0.2.2:8000/ws"; 
    private Handler handler;

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
        addMessage("Welcome to PocketFlow Chat!", Message.TYPE_AI);

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

        // 初始化WebSocket连接
        initWebSocket();

        return root;
    }

    private void initWebSocket() {
        client = new OkHttpClient();
        Request request = new Request.Builder().url(WEB_SOCKET_URL).build();
        WebSocketListener webSocketListener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);
                handler.post(() -> {
                    statusText.setText("(Connected)");
                    addMessage("Connected to server", Message.TYPE_AI_THINK);
                });
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
                handler.post(() -> {
                    // 模拟AI思考过程
                    int lastIndex = messageList.size() - 1;
                    if (lastIndex >= 0 && messageList.get(lastIndex).getType() == Message.TYPE_AI_THINK) {
                        // 移除思考消息
                        messageList.remove(lastIndex);
                        messageAdapter.notifyItemRemoved(lastIndex);
                    }
                    // 添加AI回复
                    addMessage(text, Message.TYPE_AI);
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
        if (!message.isEmpty()) {
            // 添加用户消息到列表
            addMessage(message, Message.TYPE_USER);
            
            // 模拟AI思考过程
            addMessage("Thinking...", Message.TYPE_AI_THINK);

            // 清空输入框
            messageInput.setText("");

            // 通过WebSocket发送消息
            if (webSocket != null) {
                webSocket.send(message);
            }
        }
    }

    private void addMessage(String content, int type) {
        messageList.add(new Message(content, type));
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        // 滚动到底部
        messagesRecyclerView.scrollToPosition(messageList.size() - 1);
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