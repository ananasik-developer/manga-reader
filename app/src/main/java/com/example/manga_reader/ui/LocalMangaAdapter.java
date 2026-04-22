package com.example.manga_reader.ui;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.manga_reader.R;
import com.example.manga_reader.data.database.AppDatabase;
import com.example.manga_reader.data.models.LocalManga;
import com.example.manga_reader.utils.ZipHelper;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalMangaAdapter extends RecyclerView.Adapter<LocalMangaAdapter.ViewHolder> {

    private List<LocalManga> mangaList;
    private OnMangaClickListener listener;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface OnMangaClickListener {
        void onClick(LocalManga manga);
    }

    public LocalMangaAdapter(List<LocalManga> mangaList, OnMangaClickListener listener) {
        this.mangaList = mangaList;
        this.listener = listener;
    }

    public void setMangaList(List<LocalManga> mangaList) {
        this.mangaList = mangaList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_local_manga, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocalManga manga = mangaList.get(position);
        holder.titleText.setText(manga.getTitle());
        holder.chapterText.setText("Страниц: " + manga.getPageCount());

        if (manga.getCoverPath() != null && !manga.getCoverPath().isEmpty()) {
            File coverFile = new File(manga.getCoverPath());
            if (coverFile.exists()) {
                Glide.with(holder.itemView.getContext())
                        .load(coverFile)
                        .placeholder(R.drawable.placeholder_manga)
                        .into(holder.coverImage);
            } else {
                holder.coverImage.setImageResource(R.drawable.placeholder_manga);
            }
        } else {
            holder.coverImage.setImageResource(R.drawable.placeholder_manga);
        }

        // Клик по карточке - открыть мангу
        holder.itemView.setOnClickListener(v -> listener.onClick(manga));

        // Кнопка удаления
        holder.buttonDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Удалить мангу?")
                    .setMessage("Удалить \"" + manga.getTitle() + "\"?")
                    .setPositiveButton("Удалить", (dialog, which) -> {
                        deleteManga(v, manga, position);
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });
    }

    private void deleteManga(View view, LocalManga manga, int position) {
        executor.execute(() -> {
            // Удаляем папку с файлами
            File folder = new File(manga.getFolderPath());
            ZipHelper.deleteFolder(folder);

            // Удаляем из базы данных
            AppDatabase.getInstance(view.getContext()).localMangaDao().delete(manga);

            view.post(() -> {
                mangaList.remove(position);
                notifyItemRemoved(position);
                Toast.makeText(view.getContext(), "Манга удалена", Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    public int getItemCount() {
        return mangaList != null ? mangaList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImage;
        TextView titleText;
        TextView chapterText;
        ImageButton buttonDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImage = itemView.findViewById(R.id.imageCover);
            titleText = itemView.findViewById(R.id.textTitle);
            chapterText = itemView.findViewById(R.id.textChapterCount);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
}