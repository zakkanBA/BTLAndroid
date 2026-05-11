package com.example.btland.models;

import com.google.firebase.Timestamp;

public class Message {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String content;
    private Timestamp timestamp;
    private boolean read;
    // true = tin nhắn đang chờ gửi (offline), không lưu lên Firestore
    private transient boolean pending;

    public Message() {}

    public Message(String senderId, String receiverId, String content, Timestamp timestamp, boolean read) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.timestamp = timestamp;
        this.read = read;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public boolean isPending() { return pending; }
    public void setPending(boolean pending) { this.pending = pending; }
}