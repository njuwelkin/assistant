package com.example.assistant.ui.home;

import android.os.Bundle;
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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assistant.R;
import com.example.assistant.databinding.FragmentChatBinding;
import com.example.assistant.adapter.MessageAdapter;
import com.example.assistant.model.Message;
import com.example.assistant.ui.chat.ChatViewModel;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private RecyclerView messagesRecyclerView;
    private EditText messageInput;
    private TextView statusText;
    private MessageAdapter messageAdapter;
    private ChatViewModel chatViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 初始化视图组件
        messagesRecyclerView = binding.messagesRecyclerView;
        messageInput = binding.messageInput;
        statusText = binding.statusText;

        // 初始化ViewModel
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        // 初始化消息适配器
        messageAdapter = new MessageAdapter(getContext(), new ArrayList<>());
        
        // 设置确认消息监听器
        messageAdapter.setConfirmMessageListener(new MessageAdapter.ConfirmMessageListener() {
            @Override
            public void onConfirm(String conversationId) {
                if (chatViewModel != null) {
                    chatViewModel.sendConfirmResponse(conversationId);
                }
            }
            
            @Override
            public void onCancel(String conversationId) {
                if (chatViewModel != null) {
                    chatViewModel.sendCancelResponse(conversationId);
                }
            }
        });

        // 设置RecyclerView
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        messagesRecyclerView.setAdapter(messageAdapter);

        // 观察消息列表变化
        chatViewModel.getMessageListLiveData().observe(getViewLifecycleOwner(), new Observer<List<Message>>() {
            @Override
            public void onChanged(List<Message> messages) {
                // 创建新的消息列表副本，确保状态更新能够正确触发UI刷新
                messageAdapter.setMessages(new ArrayList<>(messages));
                messageAdapter.notifyDataSetChanged();
                // 滚动到底部
                if (messages.size() > 0) {
                    messagesRecyclerView.scrollToPosition(messages.size() - 1);
                }
            }
        });

        // 观察连接状态变化
        chatViewModel.getConnectionStatusLiveData().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String status) {
                statusText.setText(status);
            }
        });

        // 观察是否正在流式处理
        chatViewModel.getIsStreamingLiveData().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isStreaming) {
                // 根据流式处理状态更新发送按钮状态
                binding.sendButton.setEnabled(messageInput.getText().toString().trim().length() > 0 && !isStreaming);
            }
        });

        // 设置发送按钮点击事件
        binding.sendButton.setOnClickListener(v -> sendMessage());

        // 设置输入框文本变化监听
        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 输入框有内容且不在流式处理时启用发送按钮
                if (chatViewModel.getIsStreamingLiveData().getValue() != null) {
                    binding.sendButton.setEnabled(s.toString().trim().length() > 0 && !chatViewModel.getIsStreamingLiveData().getValue());
                } else {
                    binding.sendButton.setEnabled(s.toString().trim().length() > 0);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 添加键盘和布局变化监听器，动态调整输入区域位置
        setupKeyboardVisibilityListener(root);

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

    // WebSocket相关逻辑现在由ChatViewModel处理，不再需要这些方法

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (!message.isEmpty() && chatViewModel.getIsStreamingLiveData().getValue() != null && !chatViewModel.getIsStreamingLiveData().getValue()) {
            // 使用ViewModel发送消息
            chatViewModel.sendMessage(message);

            // 清空输入框
            messageInput.setText("");
        }
    }

    // 消息的添加和更新现在由ViewModel处理，不再需要这些方法

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}