package com.example.btland.offline;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {PendingAction.class, CachedMessage.class}, version = 2, exportSchema = false)
public abstract class OfflineDatabase extends RoomDatabase {

    private static volatile OfflineDatabase instance;

    public abstract PendingActionDao pendingActionDao();
    public abstract CachedMessageDao cachedMessageDao();

    /** Migration từ version 1 → 2: tạo bảng cached_messages */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cached_messages` ("
                            + "`messageId` TEXT NOT NULL, "
                            + "`conversationId` TEXT NOT NULL, "
                            + "`senderId` TEXT NOT NULL, "
                            + "`receiverId` TEXT NOT NULL, "
                            + "`content` TEXT NOT NULL, "
                            + "`timestampMs` INTEGER NOT NULL, "
                            + "`read` INTEGER NOT NULL, "
                            + "PRIMARY KEY(`messageId`))"
            );
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_cached_messages_conversationId` "
                            + "ON `cached_messages` (`conversationId`)"
            );
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_cached_messages_timestampMs` "
                            + "ON `cached_messages` (`timestampMs`)"
            );
        }
    };

    public static OfflineDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (OfflineDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    OfflineDatabase.class,
                                    "btland_offline.db")
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return instance;
    }
}