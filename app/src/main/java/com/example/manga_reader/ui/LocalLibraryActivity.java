package com.example.manga_reader.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.manga_reader.R;
import com.example.manga_reader.data.database.AppDatabase;
import com.example.manga_reader.data.models.LocalManga;
import com.example.manga_reader.utils.ZipHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalLibraryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textEmpty;
    private LocalMangaAdapter adapter;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private ActivityResultLauncher<String[]> pickFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_library);

        pickFile = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        importFile(uri);
                    }
                }
        );

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ImageButton fabAdd = findViewById(R.id.fabAdd);
        recyclerView = findViewById(R.id.recyclerViewLocal);
        textEmpty = findViewById(R.id.textEmpty);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new LocalMangaAdapter(new ArrayList<>(), manga -> {
            Intent intent = new Intent(this, ReaderActivity.class);
            intent.putExtra("LOCAL_MANGA_PATH", manga.getFolderPath());
            intent.putExtra("LOCAL_MANGA_TITLE", manga.getTitle());
            intent.putExtra("IS_LOCAL", true);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Добавить мангу")
                    .setMessage("Выберите формат:")
                    .setPositiveButton("ZIP/CBZ архив", (d, w) -> {
                        pickFile.launch(new String[]{"application/zip", "application/x-cbz", "application/cbz", "*/*"});
                    })
                    .setNegativeButton("Папка с изображениями", (d, w) -> {
                        Toast.makeText(this, "Функция в разработке", Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });

        loadLocalManga();
    }

    private void loadLocalManga() {
        try {
            AppDatabase.getInstance(this).localMangaDao()
                    .getAllManga().observe(this, mangaList -> {
                        if (mangaList != null && !mangaList.isEmpty()) {
                            adapter.setMangaList(mangaList);
                            recyclerView.setVisibility(View.VISIBLE);
                            textEmpty.setVisibility(View.GONE);
                        } else {
                            recyclerView.setVisibility(View.GONE);
                            textEmpty.setVisibility(View.VISIBLE);
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            recyclerView.setVisibility(View.GONE);
            textEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void importFile(Uri uri) {
        runOnUiThread(() -> Toast.makeText(this, "Обработка файла...", Toast.LENGTH_SHORT).show());

        executor.execute(() -> {
            try {
                String fileName = getFileName(uri);
                if (fileName == null || fileName.isEmpty()) {
                    fileName = "Манга_" + System.currentTimeMillis();
                }

                // Проверяем расширение
                String lowerName = fileName.toLowerCase();
                boolean isZip = lowerName.endsWith(".zip") || lowerName.endsWith(".cbz");

                if (!isZip) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "❌ Выберите ZIP или CBZ файл", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                // Копируем файл
                File tempFile = new File(getCacheDir(), "temp_manga_import.zip");

                try (InputStream is = getContentResolver().openInputStream(uri);
                     FileOutputStream fos = new FileOutputStream(tempFile)) {

                    if (is == null) {
                        runOnUiThread(() -> Toast.makeText(this, "Не удалось открыть файл", Toast.LENGTH_LONG).show());
                        return;
                    }

                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }

                // Проверяем сигнатуру ZIP
                if (!isValidZip(tempFile)) {
                    tempFile.delete();
                    runOnUiThread(() -> {
                        Toast.makeText(this, "❌ Файл повреждён или не является ZIP архивом\nПопробуйте другой файл", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                // Убираем расширение для названия
                String mangaTitle = fileName;
                if (mangaTitle.toLowerCase().endsWith(".zip")) {
                    mangaTitle = mangaTitle.substring(0, mangaTitle.length() - 4);
                } else if (mangaTitle.toLowerCase().endsWith(".cbz")) {
                    mangaTitle = mangaTitle.substring(0, mangaTitle.length() - 4);
                }

                // Распаковываем
                ZipHelper.UnzipResult result = ZipHelper.unzipManga(this, tempFile, mangaTitle);
                tempFile.delete();

                if (result.success && result.pageCount > 0) {
                    LocalManga manga = new LocalManga(
                            mangaTitle,
                            result.folderPath,
                            result.coverPath,
                            result.pageCount
                    );

                    AppDatabase.getInstance(this).localMangaDao().insert(manga);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "✅ Добавлено: " + result.pageCount + " страниц", Toast.LENGTH_SHORT).show();
                    });
                } else if (result.success && result.pageCount == 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "❌ В архиве нет изображений\nПоддерживаются JPG, PNG, WEBP", Toast.LENGTH_LONG).show();
                        if (result.folderPath != null) {
                            ZipHelper.deleteFolder(new File(result.folderPath));
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "❌ Ошибка распаковки архива", Toast.LENGTH_LONG).show();
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "❌ Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private boolean isValidZip(File file) {
        try (InputStream is = new java.io.FileInputStream(file)) {
            byte[] header = new byte[4];
            if (is.read(header) != 4) return false;

            // PK\x03\x04 или PK\x05\x06 или PK\x07\x08
            return header[0] == 0x50 && header[1] == 0x4B &&
                    (header[2] == 0x03 || header[2] == 0x05 || header[2] == 0x07) &&
                    header[3] == 0x04;
        } catch (Exception e) {
            return false;
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        if (result == null) {
            result = "manga.zip";
        }
        return result;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}