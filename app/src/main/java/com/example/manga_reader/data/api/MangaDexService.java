package com.example.manga_reader.data.api;

import com.example.manga_reader.data.models.ChapterListResponse;
import com.example.manga_reader.data.models.ChapterPagesResponse;
import com.example.manga_reader.data.models.MangaListResponse;
import retrofit2.Call;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.GET;

public interface MangaDexService {
    @GET("manga")
    Call<MangaListResponse> searchManga(
            @Query("title") String title,
            @Query("limit") int limit
    );

    @GET("manga/{id}/feed")
    Call<ChapterListResponse> getChapters(
            @Path("id") String mangaId,
            @Query("translatedLanguage[]") String language,
            @Query("limit") int limit
    );

    @GET("at-home/server/{chapterId}")
    Call<ChapterPagesResponse> getChapterPages(@Path("chapterId") String chapterId);
}
