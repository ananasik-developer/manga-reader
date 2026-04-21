package com.example.manga_reader.data.api;

import com.example.manga_reader.data.models.MangaListResponse;
import retrofit2.Call;
import retrofit2.http.Query;
import retrofit2.http.GET;

public interface MangaDexService {
    @GET("manga")
    Call<MangaListResponse> searchManga(
            @Query("title") String title,
            @Query("limit") int limit
    );
}
