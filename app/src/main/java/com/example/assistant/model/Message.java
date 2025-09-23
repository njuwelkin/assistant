package com.example.assistant.model;

public class Message {
    public static final int TYPE_USER = 0;
    public static final int TYPE_AI = 1;
    public static final int TYPE_AI_THINK = 2;
    public static final int TYPE_CONFIRM = 3;
    
    // 确认消息的状态常量
    public static final int STATUS_PENDING = 0;      // 待处理
    public static final int STATUS_CONFIRMED = 1;    // 已确认
    public static final int STATUS_CANCELED = 2;     // 已取消

    private String content;
    private String confirmData; // 存储确认消息的原始数据
    private int type;
    private int confirmStatus;  // 确认消息的状态

    public Message(String content, int type) {
        this.content = content;
        this.type = type;
        this.confirmData = null;
        this.confirmStatus = STATUS_PENDING;
    }

    public Message(String content, int type, String confirmData) {
        this.content = content;
        this.type = type;
        this.confirmData = confirmData;
        this.confirmStatus = STATUS_PENDING;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getConfirmData() {
        return confirmData;
    }

    public void setConfirmData(String confirmData) {
        this.confirmData = confirmData;
    }
    
    // 获取确认消息状态
    public int getConfirmStatus() {
        return confirmStatus;
    }
    
    // 设置确认消息状态
    public void setConfirmStatus(int confirmStatus) {
        this.confirmStatus = confirmStatus;
    }
}