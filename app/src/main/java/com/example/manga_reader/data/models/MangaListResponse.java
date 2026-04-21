package com.example.manga_reader.data.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MangaListResponse {
    @SerializedName("data")
    private List<MangaResponse> data;
    public List<MangaResponse> getData() { return data; }
}
