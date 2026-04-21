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

        public Title getTitle() { return title; }      // ← Геттер getTitle()
        public Description getDescription() { return description; }

        public static class Title {
            @SerializedName("en")
            private String englishTitle;        // ← Поле englishTitle

            public String getEnglishTitle() { return englishTitle; } // ← Геттер getEnglishTitle()
        }

        public static class Description {
            @SerializedName("en")
            private String englishDesc;

            public String getEnglishDesc() { return englishDesc; }
        }
    }
}