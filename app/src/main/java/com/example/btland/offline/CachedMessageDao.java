package com.example.btland.offline;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CachedMessageDao {

    /** Upsert: nếu messageId trùng thì replace (cập nhật trạng thái read) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CachedMessage> messages);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CachedMessage message);

    @Query("SELECT * FROM cached_messages WHERE conversationId = :conversationId ORDER BY timestampMs ASC")
    List<CachedMessage> getByConversation(String conversationId);

    @Query("DELETE FROM cached_messages WHERE conversationId = :conversationId")
    void deleteByConversation(String conversationId);
}