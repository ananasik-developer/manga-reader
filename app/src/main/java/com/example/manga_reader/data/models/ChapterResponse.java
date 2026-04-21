package com.example.manga_reader.data.models;

import java.util.List;

public class ChapterResponse {
    private String id;
    private Attributes attributes;
    private List<Relationship> relationships;  // ← ВАЖНО: Добавляем связи

    public String getId() { return id; }
    public Attributes getAttributes() { return attributes; }
    public List<Relationship> getRelationships() { return relationships; }

    // Метод для получения языка из связей
    public String getLanguage() {
        if (relationships == null) return null;
        for (Relationship rel : relationships) {
            if ("translated_language".equals(rel.getType()) && rel.getAttributes() != null) {
                return rel.getAttributes().getCode();
            }
        }
        return null;
    }

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

    public static class Relationship {
        private String id;
        private String type;
        private LanguageAttributes attributes;

        public String getId() { return id; }
        public String getType() { return type; }
        public LanguageAttributes getAttributes() { return attributes; }
    }

    public static class LanguageAttributes {
        private String code;  // ← Здесь хранится "ru", "en", "ja" и т.д.

        public String getCode() { return code; }
    }
}
