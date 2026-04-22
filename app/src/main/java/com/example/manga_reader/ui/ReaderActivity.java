package com.example.manga_reader.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MotionEvent;
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
import com.example.manga_reader.data.models.ChapterPagesResponse;
import com.google.android.material.card.MaterialCardView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class ReaderActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private RecyclerView recyclerView;
    private ReaderPagerAdapter pagerAdapter;
    private VerticalReaderAdapter verticalAdapter;
    private String chapterId;
    private String chapterTitle;

    // UI элементы панели управления
    private MaterialCardView bottomPanel;
    private TextView pageIndicator;
    private TextView chapterTitleView;
    private AppCompatImageButton toggleModeButton;

    private boolean isHorizontalMode = true;
    private List<String> fullUrls = new ArrayList<>();
    private int currentPage = 0;

    // Для отслеживания свайпов
    private float startY;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        chapterId = getIntent().getStringExtra("CHAPTER_ID");
        chapterTitle = getIntent().getStringExtra("CHAPTER_TITLE");
        if (chapterTitle == null) {
            chapterTitle = "Глава";
        }

        initViews();
        setupViews();
        setupTouchListener();
        setupListeners();
        setupImmersiveMode();
        loadPages();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        recyclerView = findViewById(R.id.recyclerView);
        bottomPanel = findViewById(R.id.bottomPanel);
        pageIndicator = findViewById(R.id.pageIndicator);
        chapterTitleView = findViewById(R.id.chapterTitle);
        toggleModeButton = findViewById(R.id.toggleModeButton);

        String displayTitle = chapterTitle;
        if (chapterTitle.length() > 35) {
            displayTitle = chapterTitle.substring(0, 32) + "...";
        }
        chapterTitleView.setText(displayTitle);

        bottomPanel.setVisibility(View.VISIBLE);
    }

    private void setupViews() {
        // Настройка ViewPager для горизонтального режима
        pagerAdapter = new ReaderPagerAdapter(this, new ArrayList<>());
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPage = position;
                updatePageIndicator(position);
            }
        });

        // Настройка RecyclerView для вертикального режима
        verticalAdapter = new VerticalReaderAdapter(this, new ArrayList<>());

        // Создаем кастомный LinearLayoutManager с улучшенной инерцией
        LinearLayoutManager layoutManager = new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return true;
            }
        };

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(verticalAdapter);
        recyclerView.setVisibility(View.GONE);

        // ВАЖНО: Настройки для плавного скролла с инерцией
        recyclerView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        recyclerView.setNestedScrollingEnabled(true);
        recyclerView.setHasFixedSize(false);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        // Увеличиваем скорость флинга через рефлексию
        try {
            java.lang.reflect.Field field = RecyclerView.class.getDeclaredField("mMaxFlingVelocity");
            field.setAccessible(true);
            field.set(recyclerView, 20000); // Увеличиваем максимальную скорость (стандарт 8000)
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Отслеживание скролла в вертикальном режиме
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();

                    if (firstVisiblePosition >= 0) {
                        currentPage = firstVisiblePosition;
                        updatePageIndicator(currentPage);
                    }
                }
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchListener() {
        View.OnTouchListener touchListener = (v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startY = event.getY();
                    v.performClick();
                    break;

                case MotionEvent.ACTION_UP:
                    float endY = event.getY();
                    float diffY = endY - startY;

                    if (Math.abs(diffY) > 150) {
                        if (diffY > 0) {
                            hidePanel();
                        } else {
                            showPanel();
                        }
                        return true;
                    } else if (Math.abs(diffY) < 50) {
                        togglePanel();
                    }
                    v.performClick();
                    break;
            }
            return false;
        };

        viewPager.setOnTouchListener(touchListener);
        recyclerView.setOnTouchListener(touchListener);
    }

    private void setupListeners() {
        toggleModeButton.setOnClickListener(v -> toggleReadingMode());
    }

    private void setupImmersiveMode() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
    }

    private void showPanel() {
        if (bottomPanel.getVisibility() != View.VISIBLE) {
            bottomPanel.setVisibility(View.VISIBLE);
            bottomPanel.setAlpha(0f);
            bottomPanel.animate()
                    .alpha(1f)
                    .setDuration(250)
                    .start();
        }
    }

    private void hidePanel() {
        if (bottomPanel.getVisibility() == View.VISIBLE) {
            bottomPanel.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction(() -> bottomPanel.setVisibility(View.GONE))
                    .start();
        }
    }

    private void togglePanel() {
        if (bottomPanel.getVisibility() == View.VISIBLE) {
            hidePanel();
        } else {
            showPanel();
        }
    }

    private void toggleReadingMode() {
        isHorizontalMode = !isHorizontalMode;

        if (isHorizontalMode) {
            viewPager.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            viewPager.setCurrentItem(currentPage, false);
            toggleModeButton.setImageResource(R.drawable.ic_vertical_mode);
            Toast.makeText(this, "Горизонтальный режим", Toast.LENGTH_SHORT).show();
        } else {
            viewPager.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            if (currentPage >= 0 && currentPage < fullUrls.size()) {
                recyclerView.scrollToPosition(currentPage);
            }

            toggleModeButton.setImageResource(R.drawable.ic_horizontal_mode);
            Toast.makeText(this, "Вертикальный режим", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePageIndicator(int currentPage) {
        int totalPages = fullUrls.size();
        if (totalPages > 0) {
            String indicator = (currentPage + 1) + " / " + totalPages;
            pageIndicator.setText(indicator);
        }
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

                    fullUrls.clear();
                    for (String fileName : fileNames) {
                        String url = baseUrl + "/data/" + hash + "/" + fileName;
                        fullUrls.add(url);
                    }

                    pagerAdapter.setImageUrls(fullUrls);
                    verticalAdapter.setImageUrls(fullUrls);
                    updatePageIndicator(0);

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
                        "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}