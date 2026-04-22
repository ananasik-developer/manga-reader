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
import com.example.manga_reader.utils.ZipHelper;
import com.google.android.material.card.MaterialCardView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReaderActivity extends AppCompatActivity {
    private boolean isLocalMode = false;
    private String localMangaPath = null;

    private ExecutorService executor = Executors.newSingleThreadExecutor();
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

        isLocalMode = getIntent().getBooleanExtra("IS_LOCAL", false);
        if (isLocalMode) {
            localMangaPath = getIntent().getStringExtra("LOCAL_MANGA_PATH");
            chapterTitle = getIntent().getStringExtra("LOCAL_MANGA_TITLE");
            if (chapterTitle == null) chapterTitle = "Манга";
        }

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

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    int currentItem = viewPager.getCurrentItem();
                    if (currentItem == fullUrls.size() - 1) {
                        nextChapterPanel.setVisibility(View.GONE);
                    }
                }
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
                    int lastVisible = lm.findLastVisibleItemPosition();
                    int totalItems = verticalAdapter.getItemCount();

                    // ВАЖНО: используем firstVisible для номера страницы
                    if (firstVisible >= 0 && firstVisible < totalItems) {
                        currentPage = firstVisible;
                        updatePageIndicator(currentPage);
                    }

                    // Если ушли с последней страницы - скрываем кнопку
                    if (lastVisible < totalItems - 1) {
                        nextChapterPanel.setVisibility(View.GONE);
                    }

                    // Если дошли до последней страницы - проверяем
                    if (lastVisible >= totalItems - 1 && totalItems > 0) {
                        checkIfLastPage(totalItems - 1);
                    }
                }

                // Если скроллим вверх с последней страницы - скрываем кнопку
                if (dy < 0 && currentPage == fullUrls.size() - 1) {
                    nextChapterPanel.setVisibility(View.GONE);
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView rv, int newState) {
                super.onScrollStateChanged(rv, newState);
                // Когда скролл остановился - проверяем ещё раз
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                    if (lm != null) {
                        int lastVisible = lm.findLastVisibleItemPosition();
                        int totalItems = verticalAdapter.getItemCount();

                        // Если последний элемент виден полностью - обновляем счётчик на последнюю страницу
                        if (lastVisible == totalItems - 1) {
                            currentPage = totalItems - 1;
                            updatePageIndicator(currentPage);
                            checkIfLastPage(totalItems - 1);
                        }
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

            if (page == fullUrls.size() - 1) {
                checkIfLastPage(page);
            } else {
                nextChapterPanel.setVisibility(View.GONE);
            }
        }
    }

    private void checkIfLastPage(int position) {
        if (fullUrls.isEmpty()) return;

        if (position == fullUrls.size() - 1) {
            markChapterAsRead();

            if (nextChapterId != null && !nextChapterId.isEmpty()) {
                nextChapterPanel.setVisibility(View.VISIBLE);
                nextChapterPanel.bringToFront();
            }
        } else {
            nextChapterPanel.setVisibility(View.GONE);
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
        if (mangaId == null || mangaId.isEmpty()) return;

        MangaDexService s = ApiClient.getService();
        List<String> langs = new ArrayList<>();
        langs.add("ru");
        langs.add("en");

        s.getChapters(mangaId, langs, 500, "asc").enqueue(new Callback<ChapterListResponse>() {
            @Override
            public void onResponse(Call<ChapterListResponse> c, Response<ChapterListResponse> r) {
                if (r.body() != null) {
                    List<ChapterResponse> chaps = r.body().getData();

                    for (int i = 0; i < chaps.size(); i++) {
                        ChapterResponse ch = chaps.get(i);

                        if (ch.getId().equals(chapterId)) {
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

                                if (!fullUrls.isEmpty() && currentPage == fullUrls.size() - 1) {
                                    runOnUiThread(() -> {
                                        nextChapterPanel.setVisibility(View.VISIBLE);
                                        nextChapterPanel.bringToFront();
                                    });
                                }
                            }
                            break;
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<ChapterListResponse> c, Throwable t) {}
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
        }
    }

    private void loadPages() {
        if (isLocalMode) {
            loadLocalPages();
            return;
        }

        // Существующий код загрузки из API
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

                    SharedPreferences prefs = getSharedPreferences("manga_reader_prefs", MODE_PRIVATE);
                    Set<String> readChapters = prefs.getStringSet("read_chapters", new HashSet<>());
                    isChapterMarkedAsRead = readChapters.contains(chapterId);

                    if (fullUrls.size() == 1) {
                        checkIfLastPage(0);
                    }
                }
            }
            @Override
            public void onFailure(Call<ChapterPagesResponse> c, Throwable t) {}
        });
    }

    private void loadLocalPages() {
        if (localMangaPath == null) return;

        executor.execute(() -> {
            List<String> images = ZipHelper.getImagesFromFolder(localMangaPath);
            fullUrls.clear();
            fullUrls.addAll(images);

            runOnUiThread(() -> {
                pagerAdapter.setImageUrls(new ArrayList<>(fullUrls));
                verticalAdapter.setImageUrls(new ArrayList<>(fullUrls));
                updatePageIndicator(0);

                if (fullUrls.size() == 1) {
                    checkIfLastPage(0);
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}