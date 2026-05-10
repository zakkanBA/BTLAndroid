package com.example.btland.offline;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_actions")
public class PendingAction {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String type = "";

    @NonNull
    public String encryptedPayload = "";

    public long createdAt;
}
