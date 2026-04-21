package com.example.manga_reader.ui;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.manga_reader.R;
import com.example.manga_reader.data.api.ApiClient;
import com.example.manga_reader.data.api.MangaDexService;
import com.example.manga_reader.data.models.ChapterPagesResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class ReaderActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private ReaderPagerAdapter adapter;
    private String chapterId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_reader);
        chapterId = getIntent().getStringExtra("CHAPTER_ID");

        viewPager = findViewById(R.id.viewPager);
        adapter = new ReaderPagerAdapter(this, new ArrayList<>());
        viewPager.setAdapter(adapter);

        loadPages();
    }

    private void loadPages() {
        MangaDexService service = ApiClient.getService();
        Call<ChapterPagesResponse> call = service.getChapterPages(chapterId);

        call.enqueue(new Callback<ChapterPagesResponse>() {
            @Override
            public void onResponse(Call<ChapterPagesResponse> call, Response<ChapterPagesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ChapterPagesResponse pages = response.body();
                    String baseUrl = pages.getBaseUrl();
                    String hash = pages.getChapter().getHash();
                    List<String> fileNames = pages.getChapter().getData();

                    List<String> fullUrls = new ArrayList<>();
                    for (String fileName : fileNames) {
                        String url = baseUrl + "/data/" + hash + "/" + fileName;
                        fullUrls.add(url);
                    }

                    adapter.setImageUrls(fullUrls);
                    Toast.makeText(ReaderActivity.this,
                            "Загружено страниц: " + fullUrls.size(),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ReaderActivity.this,
                            "Ошибка загрузки страниц", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ChapterPagesResponse> call, Throwable t) {
                Toast.makeText(ReaderActivity.this,
                        "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }
}