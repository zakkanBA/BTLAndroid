package com.example.btland.models;

import com.google.firebase.Timestamp;
import java.util.List;

public class Conversation {
    private String conversationId;
    private List<String> userIds;
    private String lastMessage;
    private Timestamp lastTimestamp;

    public Conversation() {}

    public String getConversationId() { return conversationId; }
    public List<String> getUserIds() { return userIds; }
    public String getLastMessage() { return lastMessage; }
    public Timestamp getLastTimestamp() { return lastTimestamp; }
}