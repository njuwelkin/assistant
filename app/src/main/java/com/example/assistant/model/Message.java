package com.example.assistant.model;

public class Message {
    public static final int TYPE_USER = 0;
    public static final int TYPE_AI = 1;
    public static final int TYPE_AI_THINK = 2;
    public static final int TYPE_CONFIRM = 3;

    private String content;
    private String confirmData; // 存储确认消息的原始数据
    private int type;

    public Message(String content, int type) {
        this.content = content;
        this.type = type;
        this.confirmData = null;
    }

    public Message(String content, int type, String confirmData) {
        this.content = content;
        this.type = type;
        this.confirmData = confirmData;
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
}