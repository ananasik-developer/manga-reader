package com.example.manga_reader.data.models;

public class ChapterResponse {
    private String id;
    private Attributes attributes;

    public String getId() { return id; }
    public Attributes getAttributes() { return attributes; }

    public static class Attributes {
        private String chapter;
        private String title;
        private String publishAt;
        private int pages;

        public String getChapter() { return chapter; }
        public String getTitle() { return title; }
        public String getPublishAt() { return publishAt; }
        public int getPages() { return pages; }
    }
}
