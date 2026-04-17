package com.example.btland.models;

import com.google.firebase.Timestamp;

public class Message {
    private String senderId;
    private String content;
    private Timestamp timestamp;

    public Message() {}

    public Message(String senderId, String content, Timestamp timestamp) {
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getSenderId() { return senderId; }
    public String getContent() { return content; }
    public Timestamp getTimestamp() { return timestamp; }
}