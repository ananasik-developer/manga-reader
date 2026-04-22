package com.example.manga_reader.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
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
import java.util.List;

public class ChaptersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChapterAdapter adapter;
    private TextView textViewTitle;
    private ProgressBar progressBar;
    private String mangaId;
    private String mangaTitle;

    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем список при возврате из читалки
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapters);

        mangaId = getIntent().getStringExtra("MANGA_ID");
        mangaTitle = getIntent().getStringExtra("MANGA_TITLE");

        textViewTitle = findViewById(R.id.textViewMangaTitle);
        textViewTitle.setText(mangaTitle != null ? mangaTitle : "Главы");

        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerViewChapters);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ChapterAdapter(this, new ArrayList<>());
        adapter.setMangaTitle(mangaTitle);
        adapter.setMangaId(mangaId);// Передаем название манги в адаптер
        recyclerView.setAdapter(adapter);

        loadChapters();
    }

    private void loadChapters() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        MangaDexService service = ApiClient.getService();
        List<String> targetLanguages = new ArrayList<>();
        targetLanguages.add("ru");
        targetLanguages.add("en");

        Call<ChapterListResponse> call = service.getChapters(mangaId, targetLanguages, 100, "asc");

        call.enqueue(new Callback<ChapterListResponse>() {
            @Override
            public void onResponse(Call<ChapterListResponse> call, Response<ChapterListResponse> response) {
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);

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
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                Toast.makeText(ChaptersActivity.this,
                        "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}