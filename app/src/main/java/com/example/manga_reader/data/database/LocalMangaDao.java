package com.example.manga_reader.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.manga_reader.data.models.LocalManga;
import java.util.List;

@Dao
public interface LocalMangaDao {
    @Query("SELECT * FROM local_manga ORDER BY lastReadTime DESC")
    LiveData<List<LocalManga>> getAllManga();

    @Query("SELECT * FROM local_manga WHERE id = :id")
    LocalManga getMangaById(long id);

    @Insert
    long insert(LocalManga manga);

    @Update
    void update(LocalManga manga);

    @Delete
    void delete(LocalManga manga);
}