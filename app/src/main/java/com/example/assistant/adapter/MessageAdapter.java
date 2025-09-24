/**
 * MIT License
 * Copyright (c) 2023 illu@biubiu.org
 */
package com.example.assistant.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assistant.R;
import com.example.assistant.model.Message;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    // 定义确认消息回调接口
    public interface ConfirmMessageListener {
        void onConfirm(String conversationId);
        void onCancel(String conversationId);
    }
    
    private ConfirmMessageListener confirmMessageListener;
     private List<Message> messageList;
     private Context context;

    public MessageAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;
    }
    
    // 设置确认消息监听器
    public void setConfirmMessageListener(ConfirmMessageListener listener) {
        this.confirmMessageListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == Message.TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_user, parent, false);
            return new UserMessageViewHolder(view);
        } else if (viewType == Message.TYPE_AI_THINK) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_ai_think, parent, false);
            return new AiThinkMessageViewHolder(view);
        } else if (viewType == Message.TYPE_CONFIRM) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_confirm, parent, false);
            return new ConfirmMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_ai, parent, false);
            return new AiMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).messageText.setText(message.getContent());
        } else if (holder instanceof AiThinkMessageViewHolder) {
            ((AiThinkMessageViewHolder) holder).messageText.setText(message.getContent());
        } else if (holder instanceof AiMessageViewHolder) {
            ((AiMessageViewHolder) holder).messageText.setText(message.getContent());
        } else if (holder instanceof ConfirmMessageViewHolder) {
            ((ConfirmMessageViewHolder) holder).bind(message, confirmMessageListener);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }
    
    // 设置消息列表并更新UI
    public void setMessages(List<Message> messages) {
        this.messageList = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).getType();
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.user_message_text);
        }
    }

    static class AiMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        public AiMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.ai_message_text);
        }
    }

    static class AiThinkMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        public AiThinkMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.ai_think_message_text);
        }
    }
    
    static class ConfirmMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        Button confirmButton;
        Button cancelButton;
        TextView confirmTitle;

        public ConfirmMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.confirm_message_text);
            confirmButton = itemView.findViewById(R.id.confirm_button);
            cancelButton = itemView.findViewById(R.id.cancel_button);
            confirmTitle = itemView.findViewById(R.id.confirm_title);
        }
        
        public void bind(Message message, ConfirmMessageListener listener) {
            messageText.setText(message.getContent());
            
            // 尝试从confirmData中解析更多信息
            String conversationId = "";
            
            try {
                if (message.getConfirmData() != null) {
                    JSONObject confirmData = new JSONObject(message.getConfirmData());
                    conversationId = confirmData.getString("conversation_id");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            
            // 根据消息的确认状态设置按钮和标题
            int confirmStatus = message.getConfirmStatus();
            if (confirmStatus == Message.STATUS_CONFIRMED) {
                confirmButton.setEnabled(false);
                cancelButton.setEnabled(false);
                confirmTitle.setText("已确认");
            } else if (confirmStatus == Message.STATUS_CANCELED) {
                confirmButton.setEnabled(false);
                cancelButton.setEnabled(false);
                confirmTitle.setText("已取消");
            } else {
                // 待处理状态，按钮可用
                confirmButton.setEnabled(true);
                cancelButton.setEnabled(true);
                confirmTitle.setText("需要确认");
            }
            
            // 设置确认按钮点击事件
            final String finalConversationId = conversationId;
            confirmButton.setOnClickListener(v -> {
                if (listener != null && message.getConfirmStatus() == Message.STATUS_PENDING) {
                    listener.onConfirm(finalConversationId);
                    // 立即更新UI状态以提供即时反馈
                    message.setConfirmStatus(Message.STATUS_CONFIRMED);
                    confirmButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                    confirmTitle.setText("已确认");
                }
            });
            
            // 设置取消按钮点击事件
            cancelButton.setOnClickListener(v -> {
                if (listener != null && message.getConfirmStatus() == Message.STATUS_PENDING) {
                    listener.onCancel(finalConversationId);
                    // 立即更新UI状态以提供即时反馈
                    message.setConfirmStatus(Message.STATUS_CANCELED);
                    confirmButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                    confirmTitle.setText("已取消");
                }
            });
        }
    }
}