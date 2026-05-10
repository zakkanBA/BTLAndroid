package com.example.btland.offline;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PendingActionDao {
    @Insert
    long insert(PendingAction action);

    @Query("SELECT * FROM pending_actions ORDER BY createdAt ASC")
    List<PendingAction> getAll();

    @Query("DELETE FROM pending_actions WHERE id = :id")
    void deleteById(long id);
}
