package com.example.manga_reader;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
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
import com.example.manga_reader.ui.LocalLibraryActivity;
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

    private boolean isDarkTheme = false;
    private MaterialToolbar toolbar;
    private EditText editTextSearch;
    private ImageButton buttonSearch;
    private ImageButton buttonLocal;
    private RecyclerView recyclerView;
    private ProgressBar progressBarMain;
    private ImageView placeholderImage;
    private MangaAdapter adapter;
    private SharedPreferences prefs;

    private List<MangaResponse> savedMangaList = new ArrayList<>();
    private String savedSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isDarkTheme = prefs.getBoolean(KEY_THEME, false);

        if (savedInstanceState != null) {
            savedSearchQuery = savedInstanceState.getString("search_query", "");
            String json = savedInstanceState.getString("manga_list", "");
            if (!json.isEmpty()) {
                Type type = new TypeToken<List<MangaResponse>>(){}.getType();
                savedMangaList = new Gson().fromJson(json, type);
            }
        }

        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        if (!savedSearchQuery.isEmpty()) {
            editTextSearch.setText(savedSearchQuery);
        }
        if (savedMangaList != null && !savedMangaList.isEmpty()) {
            adapter.setMangaList(savedMangaList);
            recyclerView.setVisibility(View.VISIBLE);
            placeholderImage.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.GONE);
            placeholderImage.setVisibility(View.VISIBLE);
        }

        toolbar.setNavigationOnClickListener(v -> {
            savedSearchQuery = editTextSearch.getText().toString().trim();
            if (adapter != null && adapter.getItemCount() > 0) {
                savedMangaList = adapter.getMangaList();
            }

            isDarkTheme = !isDarkTheme;
            prefs.edit().putBoolean(KEY_THEME, isDarkTheme).apply();
            applyTheme();
            recreate();
        });

        buttonSearch.setOnClickListener(v -> {
            String query = editTextSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                savedSearchQuery = query;
                searchManga(query);
            } else {
                Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show();
            }
        });

        buttonLocal.setOnClickListener(v -> {
            try {
                startActivity(new Intent(MainActivity.this, LocalLibraryActivity.class));
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Ошибка открытия библиотеки", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyTheme() {
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.topAppBar);
        editTextSearch = findViewById(R.id.editTextSearch);
        buttonSearch = findViewById(R.id.buttonSearch);
        buttonLocal = findViewById(R.id.buttonLocal);
        recyclerView = findViewById(R.id.recyclerViewManga);
        progressBarMain = findViewById(R.id.progressBarMain);
        placeholderImage = findViewById(R.id.imageViewPlaceholder);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MangaAdapter(this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("search_query", editTextSearch.getText().toString().trim());
        if (adapter != null && adapter.getItemCount() > 0) {
            String json = new Gson().toJson(adapter.getMangaList());
            outState.putString("manga_list", json);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isDarkTheme) {
            toolbar.setNavigationIcon(R.drawable.ic_theme_light);
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_theme_dark);
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
                        savedMangaList = mangaList;
                        recyclerView.setVisibility(View.VISIBLE);
                        placeholderImage.setVisibility(View.GONE);
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        placeholderImage.setVisibility(View.VISIBLE);
                        Toast.makeText(MainActivity.this, "Ничего не найдено", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    recyclerView.setVisibility(View.GONE);
                    placeholderImage.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<MangaListResponse> call, Throwable t) {
                progressBarMain.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
                placeholderImage.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }
}