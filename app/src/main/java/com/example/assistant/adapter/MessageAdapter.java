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
        TextView confirmContentText;
        Button confirmButton;
        Button cancelButton;
        TextView confirmTitle;

        public ConfirmMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.confirm_message_text);
            confirmContentText = itemView.findViewById(R.id.confirm_content_text);
            confirmButton = itemView.findViewById(R.id.confirm_button);
            cancelButton = itemView.findViewById(R.id.cancel_button);
            confirmTitle = itemView.findViewById(R.id.confirm_title);
        }
        
        public void bind(Message message, ConfirmMessageListener listener) {
            messageText.setText(message.getContent());
            
            // 尝试从confirmData中解析更多信息
            String conversationId = "";
            String confirmDetails = "";
            
            try {
                if (message.getConfirmData() != null) {
                    JSONObject confirmData = new JSONObject(message.getConfirmData());
                    conversationId = confirmData.getString("conversation_id");
                    
                    // 显示confirm_list内容
                    if (confirmData.has("confirm_list")) {
                        JSONArray confirmList = confirmData.getJSONArray("confirm_list");
                        StringBuilder detailsBuilder = new StringBuilder();
                        for (int i = 0; i < confirmList.length(); i++) {
                            detailsBuilder.append(confirmList.getString(i));
                            if (i < confirmList.length() - 1) {
                                detailsBuilder.append("\n");
                            }
                        }
                        confirmDetails = detailsBuilder.toString();
                        confirmContentText.setText(confirmDetails);
                        confirmContentText.setVisibility(View.VISIBLE);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            
            // 设置确认按钮点击事件
            final String finalConversationId = conversationId;
            confirmButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConfirm(finalConversationId);
                    // 禁用按钮防止重复点击
                    confirmButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                    confirmTitle.setText("已确认");
                }
            });
            
            // 设置取消按钮点击事件
            cancelButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCancel(finalConversationId);
                    // 禁用按钮防止重复点击
                    confirmButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                    confirmTitle.setText("已取消");
                }
            });
        }
    }
}