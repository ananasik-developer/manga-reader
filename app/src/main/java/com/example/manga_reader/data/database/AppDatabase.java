package com.example.manga_reader.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.manga_reader.data.models.LocalManga;

@Database(entities = {LocalManga.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract LocalMangaDao localMangaDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "manga_reader_db"
                    ).fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}