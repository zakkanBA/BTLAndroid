package com.example.btland.models;

import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Conversation {
    private String conversationId;
    private List<String> userIds;
    private String lastMessage;
    private Timestamp lastTimestamp;
    private Map<String, Long> unreadCounts;
    private Map<String, String> participantNames;

    public Conversation() {
        unreadCounts = new HashMap<>();
        participantNames = new HashMap<>();
    }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public List<String> getUserIds() { return userIds; }
    public void setUserIds(List<String> userIds) { this.userIds = userIds; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public Timestamp getLastTimestamp() { return lastTimestamp; }
    public void setLastTimestamp(Timestamp lastTimestamp) { this.lastTimestamp = lastTimestamp; }

    public Map<String, Long> getUnreadCounts() {
        return unreadCounts == null ? new HashMap<>() : unreadCounts;
    }

    public void setUnreadCounts(Map<String, Long> unreadCounts) {
        this.unreadCounts = unreadCounts;
    }

    public Map<String, String> getParticipantNames() {
        return participantNames == null ? new HashMap<>() : participantNames;
    }

    public void setParticipantNames(Map<String, String> participantNames) {
        this.participantNames = participantNames;
    }
}
