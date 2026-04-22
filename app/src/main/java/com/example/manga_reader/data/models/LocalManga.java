package com.example.manga_reader.data.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_manga")
public class LocalManga {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String title;
    private String folderPath;
    private String coverPath;
    private int pageCount;
    private long lastReadTime;

    public LocalManga(String title, String folderPath, String coverPath, int pageCount) {
        this.title = title;
        this.folderPath = folderPath;
        this.coverPath = coverPath;
        this.pageCount = pageCount;
        this.lastReadTime = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFolderPath() { return folderPath; }
    public void setFolderPath(String folderPath) { this.folderPath = folderPath; }

    public String getCoverPath() { return coverPath; }
    public void setCoverPath(String coverPath) { this.coverPath = coverPath; }

    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }

    public long getLastReadTime() { return lastReadTime; }
    public void setLastReadTime(long lastReadTime) { this.lastReadTime = lastReadTime; }
}