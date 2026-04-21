package com.example.manga_reader.ui;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.manga_reader.R;
import com.example.manga_reader.data.api.ApiClient;
import com.example.manga_reader.data.api.MangaDexService;
import com.example.manga_reader.data.models.ChapterListResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;

public class ChaptersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChapterAdapter adapter;
    private TextView textViewTitle;
    private String mangaId;
    private String mangaTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapters);

        // Получаем данные из Intent
        mangaId = getIntent().getStringExtra("MANGA_ID");
        mangaTitle = getIntent().getStringExtra("MANGA_TITLE");

        textViewTitle = findViewById(R.id.textViewMangaTitle);
        textViewTitle.setText(mangaTitle != null ? mangaTitle : "Главы");

        recyclerView = findViewById(R.id.recyclerViewChapters);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ChapterAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        loadChapters();
    }

    private void loadChapters() {
        MangaDexService service = ApiClient.getService();
        Call<ChapterListResponse> call = service.getChapters(mangaId, "en", 100);

        call.enqueue(new Callback<ChapterListResponse>() {
            @Override
            public void onResponse(Call<ChapterListResponse> call, Response<ChapterListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setChapters(response.body().getData());
                    Toast.makeText(ChaptersActivity.this,
                            "Найдено глав: " + response.body().getData().size(),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ChaptersActivity.this,
                            "Ошибка загрузки глав", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ChapterListResponse> call, Throwable t) {
                Toast.makeText(ChaptersActivity.this,
                        "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }
}