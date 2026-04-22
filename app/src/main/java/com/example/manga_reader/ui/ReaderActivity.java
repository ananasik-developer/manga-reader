package com.example.manga_reader.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.example.manga_reader.R;
import com.example.manga_reader.data.api.ApiClient;
import com.example.manga_reader.data.api.MangaDexService;
import com.example.manga_reader.data.models.ChapterListResponse;
import com.example.manga_reader.data.models.ChapterPagesResponse;
import com.example.manga_reader.data.models.ChapterResponse;
import com.google.android.material.card.MaterialCardView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReaderActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private RecyclerView recyclerView;
    private ReaderPagerAdapter pagerAdapter;
    private VerticalReaderAdapter verticalAdapter;
    private String chapterId;
    private String chapterTitle;
    private String mangaId;

    private MaterialCardView bottomPanel;
    private MaterialCardView nextChapterPanel;
    private TextView pageIndicator;
    private TextView chapterTitleView;
    private AppCompatImageButton toggleModeButton;
    private TextView nextChapterButton;

    private boolean isHorizontalMode = true;
    private List<String> fullUrls = new ArrayList<>();
    private int currentPage = 0;
    private boolean isChapterMarkedAsRead = false;
    private String nextChapterId = null;
    private String nextChapterTitle = null;
    private boolean nextChapterLoaded = false;

    private float startY;
    private float startX;
    private VelocityTracker velocityTracker;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        chapterId = getIntent().getStringExtra("CHAPTER_ID");
        chapterTitle = getIntent().getStringExtra("CHAPTER_TITLE");
        mangaId = getIntent().getStringExtra("MANGA_ID");
        if (chapterTitle == null) chapterTitle = "Глава";

        initViews();
        setupViews();
        setupTouchListener();
        setupListeners();
        setupImmersiveMode();

        loadNextChapterInfo();
        loadPages();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        recyclerView = findViewById(R.id.recyclerView);
        bottomPanel = findViewById(R.id.bottomPanel);
        nextChapterPanel = findViewById(R.id.nextChapterPanel);
        pageIndicator = findViewById(R.id.pageIndicator);
        chapterTitleView = findViewById(R.id.chapterTitle);
        toggleModeButton = findViewById(R.id.toggleModeButton);
        nextChapterButton = findViewById(R.id.nextChapterButton);

        String displayTitle = chapterTitle.length() > 35 ? chapterTitle.substring(0, 32) + "..." : chapterTitle;
        chapterTitleView.setText(displayTitle);

        bottomPanel.setVisibility(View.VISIBLE);
        nextChapterPanel.setVisibility(View.GONE);
    }

    private void setupViews() {
        pagerAdapter = new ReaderPagerAdapter(this, new ArrayList<>());
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        viewPager.setOffscreenPageLimit(2);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPage = position;
                updatePageIndicator(position);
                checkIfLastPage(position);
            }
        });

        verticalAdapter = new VerticalReaderAdapter(this, new ArrayList<>());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(verticalAdapter);
        recyclerView.setVisibility(View.GONE);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm != null) {
                    int firstVisible = lm.findFirstVisibleItemPosition();
                    if (firstVisible >= 0) {
                        currentPage = firstVisible;
                        updatePageIndicator(currentPage);
                    }

                    int lastVisible = lm.findLastVisibleItemPosition();
                    int totalItems = verticalAdapter.getItemCount();
                    if (lastVisible >= totalItems - 1 && totalItems > 0) {
                        checkIfLastPage(totalItems - 1);
                    }
                }
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchListener() {
        View.OnTouchListener listener = (v, event) -> {
            if (v == viewPager) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startY = event.getY();
                        startX = event.getX();
                        v.performClick();
                        break;
                    case MotionEvent.ACTION_UP:
                        float diffY = Math.abs(event.getY() - startY);
                        float diffX = Math.abs(event.getX() - startX);
                        if (diffY > 150 && diffY > diffX) {
                            if (event.getY() > startY) hidePanels();
                            else showPanel();
                            return true;
                        } else if (diffY < 50 && diffX < 50) {
                            togglePanels();
                        }
                        v.performClick();
                        break;
                }
            } else if (v == recyclerView) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startY = event.getY();
                        startX = event.getX();
                        v.performClick();
                        return false;
                    case MotionEvent.ACTION_UP:
                        float diffY = Math.abs(event.getY() - startY);
                        float diffX = Math.abs(event.getX() - startX);
                        if (diffY < 30 && diffX < 30) togglePanels();
                        v.performClick();
                        return false;
                }
            }
            return false;
        };
        viewPager.setOnTouchListener(listener);
        recyclerView.setOnTouchListener(listener);
    }

    private void setupListeners() {
        toggleModeButton.setOnClickListener(v -> toggleReadingMode());
        nextChapterButton.setOnClickListener(v -> {
            if (nextChapterId != null && !nextChapterId.isEmpty()) {
                loadNextChapter();
            } else {
                Toast.makeText(this, "Следующая глава не найдена", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupImmersiveMode() {
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (c != null) {
            c.hide(WindowInsetsCompat.Type.systemBars());
            c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    private void showPanel() {
        if (bottomPanel.getVisibility() != View.VISIBLE) {
            bottomPanel.setVisibility(View.VISIBLE);
            bottomPanel.setAlpha(0f);
            bottomPanel.animate().alpha(1f).setDuration(250).start();
        }
    }

    private void hidePanels() {
        if (bottomPanel.getVisibility() == View.VISIBLE) {
            bottomPanel.animate().alpha(0f).setDuration(250).withEndAction(() -> bottomPanel.setVisibility(View.GONE)).start();
        }
    }

    private void togglePanels() {
        if (bottomPanel.getVisibility() == View.VISIBLE) hidePanels();
        else showPanel();
    }

    private void toggleReadingMode() {
        isHorizontalMode = !isHorizontalMode;
        if (isHorizontalMode) {
            viewPager.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            viewPager.setCurrentItem(currentPage, false);
            toggleModeButton.setImageResource(R.drawable.ic_vertical_mode);
        } else {
            viewPager.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            if (currentPage < fullUrls.size()) recyclerView.scrollToPosition(currentPage);
            toggleModeButton.setImageResource(R.drawable.ic_horizontal_mode);
        }
    }

    private void updatePageIndicator(int page) {
        if (!fullUrls.isEmpty()) {
            String text = (page + 1) + " / " + fullUrls.size();
            pageIndicator.setText(text);

            // ПРИНУДИТЕЛЬНАЯ ПРОВЕРКА при обновлении индикатора
            if (page == fullUrls.size() - 1) {
                checkIfLastPage(page);
            }
        }
    }

    private void checkIfLastPage(int position) {
        if (fullUrls.isEmpty()) return;

        // Логируем для отладки
        android.util.Log.d("READER", "checkIfLastPage: position=" + position + ", total=" + fullUrls.size() + ", nextChapterId=" + nextChapterId);

        if (position == fullUrls.size() - 1) {
            android.util.Log.d("READER", "LAST PAGE DETECTED!");
            markChapterAsRead();

            // Пробуем показать кнопку сразу
            showNextChapterButtonIfReady();

            // И еще раз через 500мс на случай если ID еще не загрузился
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                showNextChapterButtonIfReady();
            }, 500);
        }
    }

    private void showNextChapterButtonIfReady() {
        android.util.Log.d("READER", "showNextChapterButtonIfReady: nextChapterId=" + nextChapterId);

        if (nextChapterId != null && !nextChapterId.isEmpty()) {
            nextChapterPanel.setVisibility(View.VISIBLE);
            nextChapterPanel.bringToFront();
            android.util.Log.d("READER", "Button shown!");
        } else {
            android.util.Log.d("READER", "Button NOT shown - no nextChapterId");
        }
    }

    private void markChapterAsRead() {
        if (isChapterMarkedAsRead || chapterId == null) return;
        isChapterMarkedAsRead = true;
        SharedPreferences p = getSharedPreferences("manga_reader_prefs", MODE_PRIVATE);
        Set<String> set = new HashSet<>(p.getStringSet("read_chapters", new HashSet<>()));
        set.add(chapterId);
        p.edit().putStringSet("read_chapters", set).apply();
    }

    private void loadNextChapterInfo() {
        if (mangaId == null || mangaId.isEmpty()) {
            android.util.Log.e("READER", "mangaId is null or empty!");
            return;
        }

        android.util.Log.d("READER", "Loading chapters for mangaId: " + mangaId + ", current chapterId: " + chapterId);

        MangaDexService s = ApiClient.getService();
        List<String> langs = new ArrayList<>();
        langs.add("ru");
        langs.add("en");

        // Увеличиваем лимит до 500 на всякий случай
        s.getChapters(mangaId, langs, 500, "asc").enqueue(new Callback<ChapterListResponse>() {
            @Override
            public void onResponse(Call<ChapterListResponse> c, Response<ChapterListResponse> r) {
                if (r.body() != null) {
                    List<ChapterResponse> chaps = r.body().getData();
                    android.util.Log.d("READER", "Total chapters loaded: " + chaps.size());

                    boolean foundCurrent = false;

                    for (int i = 0; i < chaps.size(); i++) {
                        ChapterResponse ch = chaps.get(i);
                        String chId = ch.getId();
                        String chNum = ch.getAttributes().getChapter();

                        android.util.Log.d("READER", "Chapter " + i + ": id=" + chId + ", number=" + chNum);

                        if (chId.equals(chapterId)) {
                            foundCurrent = true;
                            android.util.Log.d("READER", "FOUND current chapter at position " + i);

                            if (i + 1 < chaps.size()) {
                                ChapterResponse next = chaps.get(i + 1);
                                nextChapterId = next.getId();
                                String num = next.getAttributes().getChapter();
                                String title = next.getAttributes().getTitle();

                                if (num != null && !num.isEmpty()) {
                                    nextChapterTitle = "Глава " + num;
                                } else {
                                    nextChapterTitle = "Следующая глава";
                                }

                                if (title != null && !title.isEmpty()) {
                                    nextChapterTitle += ": " + title;
                                }

                                android.util.Log.d("READER", "NEXT chapter found: id=" + nextChapterId + ", title=" + nextChapterTitle);

                                // Если мы уже на последней странице, показываем кнопку
                                if (!fullUrls.isEmpty() && currentPage == fullUrls.size() - 1) {
                                    runOnUiThread(() -> {
                                        android.util.Log.d("READER", "Showing button from loadNextChapterInfo");
                                        nextChapterPanel.setVisibility(View.VISIBLE);
                                        nextChapterPanel.bringToFront();
                                    });
                                }
                            } else {
                                android.util.Log.d("READER", "This is the LAST chapter (no next chapter)");
                            }
                            break;
                        }
                    }

                    if (!foundCurrent) {
                        android.util.Log.e("READER", "Current chapter NOT FOUND in the list!");
                    }
                } else {
                    android.util.Log.e("READER", "Response body is null");
                }
            }
            @Override
            public void onFailure(Call<ChapterListResponse> c, Throwable t) {
                android.util.Log.e("READER", "Failed to load chapters: " + t.getMessage());
            }
        });
    }

    private void loadNextChapter() {
        if (nextChapterId != null && !nextChapterId.isEmpty()) {
            Intent intent = new Intent(this, ReaderActivity.class);
            intent.putExtra("CHAPTER_ID", nextChapterId);
            intent.putExtra("MANGA_ID", mangaId);
            intent.putExtra("CHAPTER_TITLE", nextChapterTitle);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Это последняя глава", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPages() {
        ApiClient.getService().getChapterPages(chapterId).enqueue(new Callback<ChapterPagesResponse>() {
            @Override
            public void onResponse(Call<ChapterPagesResponse> c, Response<ChapterPagesResponse> r) {
                if (r.body() != null) {
                    ChapterPagesResponse p = r.body();
                    String base = p.getBaseUrl();
                    String hash = p.getChapter().getHash();
                    fullUrls.clear();
                    for (String f : p.getChapter().getData()) {
                        fullUrls.add(base + "/data/" + hash + "/" + f);
                    }
                    pagerAdapter.setImageUrls(new ArrayList<>(fullUrls));
                    verticalAdapter.setImageUrls(new ArrayList<>(fullUrls));
                    updatePageIndicator(0);

                    // Проверяем прочитана ли глава
                    SharedPreferences prefs = getSharedPreferences("manga_reader_prefs", MODE_PRIVATE);
                    Set<String> readChapters = prefs.getStringSet("read_chapters", new HashSet<>());
                    isChapterMarkedAsRead = readChapters.contains(chapterId);

                    if (fullUrls.size() == 1) {
                        checkIfLastPage(0);
                    }
                } else {
                    Toast.makeText(ReaderActivity.this, "Ошибка загрузки страниц", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ChapterPagesResponse> c, Throwable t) {
                Toast.makeText(ReaderActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }
}