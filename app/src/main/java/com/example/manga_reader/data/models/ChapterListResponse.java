package com.example.manga_reader.data.models;

import java.util.List;

public class ChapterListResponse {
    private List<ChapterResponse> data;
    private int total;
    private int limit;
    private int offset;

    public List<ChapterResponse> getData() { return data; }
    public int getTotal() { return total; }
    public int getLimit() { return limit; }
    public int getOffset() { return offset; }
}
