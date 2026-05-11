package com.example.btland.offline;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Bảng cache tin nhắn để đọc khi offline.
 * Mỗi tin nhắn được định danh bởi messageId (unique).
 */
@Entity(
        tableName = "cached_messages",
        indices = {@Index("conversationId"), @Index("timestampMs")}
)
public class CachedMessage {

    @PrimaryKey
    @NonNull
    public String messageId = "";

    @NonNull
    public String conversationId = "";

    @NonNull
    public String senderId = "";

    @NonNull
    public String receiverId = "";

    @NonNull
    public String content = "";

    /** Epoch millis, dùng để ORDER BY */
    public long timestampMs;

    public boolean read;
}