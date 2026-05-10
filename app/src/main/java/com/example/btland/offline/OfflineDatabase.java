package com.example.btland.offline;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {PendingAction.class}, version = 1, exportSchema = false)
public abstract class OfflineDatabase extends RoomDatabase {

    private static volatile OfflineDatabase instance;

    public abstract PendingActionDao pendingActionDao();

    public static OfflineDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (OfflineDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    OfflineDatabase.class,
                                    "btland_offline.db")
                            .build();
                }
            }
        }
        return instance;
    }
}
