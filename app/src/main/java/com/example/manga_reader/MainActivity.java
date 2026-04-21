package com.example.manga_reader;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.example.manga_reader.R;
import com.example.manga_reader.data.api.ApiClient;
import com.example.manga_reader.data.models.MangaListResponse;
import com.example.manga_reader.data.models.MangaResponse;
import com.example.manga_reader.ui.MangaAdapter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_THEME = "dark_theme";
    private static final String KEY_LAST_SEARCH = "last_search";
    private static final String KEY_MANGA_LIST = "manga_list";
    private static final String KEY_NEED_RESTORE = "need_restore"; // ← НОВЫЙ КЛЮЧ

    private boolean isDarkTheme = false;
    private MaterialToolbar toolbar;
    private EditText editTextSearch;
    private Button buttonSearch;
    private RecyclerView recyclerView;
    private ProgressBar progressBarMain;
    private ImageView placeholderImage;
    private MangaAdapter adapter;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isDarkTheme = prefs.getBoolean(KEY_THEME, false);

        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        updateThemeIcon();

        boolean needRestore = prefs.getBoolean(KEY_NEED_RESTORE, false);

        if (needRestore) {
            String lastSearch = prefs.getString(KEY_LAST_SEARCH, "");
            if (!lastSearch.isEmpty()) {
                editTextSearch.setText(lastSearch);
            }
            loadSavedMangaList();

            prefs.edit().putBoolean(KEY_NEED_RESTORE, false).apply();
        } else {
            editTextSearch.setText("");
            recyclerView.setVisibility(View.GONE);
            placeholderImage.setVisibility(View.VISIBLE);

            prefs.edit().remove(KEY_LAST_SEARCH).apply();
            prefs.edit().remove(KEY_MANGA_LIST).apply();
        }

        toolbar.setNavigationOnClickListener(v -> {
            String currentSearch = editTextSearch.getText().toString().trim();
            prefs.edit().putString(KEY_LAST_SEARCH, currentSearch).apply();

            if (adapter != null && adapter.getItemCount() > 0) {
                saveMangaList(adapter.getMangaList());
            }

            prefs.edit().putBoolean(KEY_NEED_RESTORE, true).apply();

            isDarkTheme = !isDarkTheme;
            prefs.edit().putBoolean(KEY_THEME, isDarkTheme).apply();

            recreate();
        });

        buttonSearch.setOnClickListener(v -> {
            String query = editTextSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                prefs.edit().putString(KEY_LAST_SEARCH, query).apply();
                searchManga(query);
            } else {
                Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show();
                recyclerView.setVisibility(View.GONE);
                placeholderImage.setVisibility(View.VISIBLE);
                prefs.edit().remove(KEY_LAST_SEARCH).apply();
                prefs.edit().remove(KEY_MANGA_LIST).apply();
            }
        });
    }

    private void initViews() {
        toolbar = findViewById(R.id.topAppBar);
        editTextSearch = findViewById(R.id.editTextSearch);
        buttonSearch = findViewById(R.id.buttonSearch);
        recyclerView = findViewById(R.id.recyclerViewManga);
        progressBarMain = findViewById(R.id.progressBarMain);
        placeholderImage = findViewById(R.id.imageViewPlaceholder);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MangaAdapter(this);
        recyclerView.setAdapter(adapter);
    }

    private void updateThemeIcon() {
        if (isDarkTheme) {
            toolbar.setNavigationIcon(R.drawable.ic_theme_dark_24);
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_theme_24);
        }
    }

    private void saveMangaList(List<MangaResponse> list) {
        if (list != null && !list.isEmpty()) {
            Gson gson = new Gson();
            String json = gson.toJson(list);
            prefs.edit().putString(KEY_MANGA_LIST, json).apply();
        }
    }

    private void loadSavedMangaList() {
        String json = prefs.getString(KEY_MANGA_LIST, null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<MangaResponse>>(){}.getType();
            List<MangaResponse> savedList = gson.fromJson(json, type);
            if (savedList != null && !savedList.isEmpty()) {
                adapter.setMangaList(savedList);
                recyclerView.setVisibility(View.VISIBLE);
                placeholderImage.setVisibility(View.GONE);
            } else {
                recyclerView.setVisibility(View.GONE);
                placeholderImage.setVisibility(View.VISIBLE);
            }
        } else {
            recyclerView.setVisibility(View.GONE);
            placeholderImage.setVisibility(View.VISIBLE);
        }
    }

    private void searchManga(String title) {
        progressBarMain.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        placeholderImage.setVisibility(View.GONE);

        ApiClient.getService().searchManga(title, 50).enqueue(new Callback<MangaListResponse>() {
            @Override
            public void onResponse(Call<MangaListResponse> call, Response<MangaListResponse> response) {
                progressBarMain.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    List<MangaResponse> mangaList = response.body().getData();
                    if (mangaList != null && !mangaList.isEmpty()) {
                        adapter.setMangaList(mangaList);
                        saveMangaList(mangaList);
                        recyclerView.setVisibility(View.VISIBLE);
                        placeholderImage.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this,
                                "Найдено: " + mangaList.size(),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        placeholderImage.setVisibility(View.VISIBLE);
                        Toast.makeText(MainActivity.this,
                                "Ничего не найдено", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    recyclerView.setVisibility(View.GONE);
                    placeholderImage.setVisibility(View.VISIBLE);
                    Toast.makeText(MainActivity.this,
                            "Ошибка: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MangaListResponse> call, Throwable t) {
                progressBarMain.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
                placeholderImage.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this,
                        "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }
}