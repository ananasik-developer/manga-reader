package com.example.manga_reader.data.models;

import java.util.List;

public class ChapterPagesResponse {
    private String result;
    private String baseUrl;
    private ChapterData chapter;

    public String getBaseUrl() { return baseUrl; }
    public ChapterData getChapter() { return chapter; }

    public static class ChapterData {
        private String hash;
        private List<String> data;
        private List<String> dataSaver;

        public String getHash() { return hash; }
        public List<String> getData() { return data; }
        public List<String> getDataSaver() { return dataSaver; }
    }
}
