package com.example.manga_reader.data.models;

import android.icu.text.CaseMap;

import com.google.gson.annotations.SerializedName;

import java.util.jar.Attributes;

public class MangaResponse {
    @SerializedName("id")
    private String id;

    @SerializedName("attributes")
    private Attributes attributes;

    public String getId() { return id; }
    public Attributes getAttributes() { return attributes; }

    public static class Attributes {
        @SerializedName("title")
        private Title title;                    // ← Поле title

        @SerializedName("description")
        private Description description;

        public Title getTitle() { return title; }
        public Description getDescription() { return description; }

        public static class Title extends java.util.LinkedHashMap<String, String> {
            public String getAnyTitle() {
                // Пробуем английский
                if (containsKey("en")) return get("en");
                // Пробуем японский (ромадзи)
                if (containsKey("ja-ro")) return get("ja-ro");
                // Пробуем русский (вдруг повезет)
                if (containsKey("ru")) return get("ru");
                // Иначе берем первое попавшееся значение
                if (!isEmpty()) return values().iterator().next();
                return "Без названия";
            }
        }

        public static class Description {
            @SerializedName("en")
            private String englishDesc;

            public String getEnglishDesc() { return englishDesc; }
        }
    }
}