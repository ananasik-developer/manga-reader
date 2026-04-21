package com.example.manga_reader.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.manga_reader.R;
import com.example.manga_reader.data.models.MangaResponse;
import java.util.ArrayList;
import java.util.List;
import com.example.manga_reader.ui.ChaptersActivity;

public class MangaAdapter extends RecyclerView.Adapter<MangaAdapter.ViewHolder> {

    private Context context;
    private List<MangaResponse> mangaList = new ArrayList<>();

    public MangaAdapter(Context context) {
        this.context = context;
    }

    public void setMangaList(List<MangaResponse> list) {
        this.mangaList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_manga, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MangaResponse manga = mangaList.get(position);

        String title = "Без названия";
        if (manga.getAttributes() != null &&
                manga.getAttributes().getTitle() != null &&
                manga.getAttributes().getTitle().getEnglishTitle() != null) {
            title = manga.getAttributes().getTitle().getEnglishTitle();
        }
        holder.textViewTitle.setText(title);

        Glide.with(context)
                .load(R.drawable.ic_launcher_foreground)
                .into(holder.imageViewCover);

        holder.itemView.setOnClickListener(v -> {
            MangaResponse clickedManga = mangaList.get(position);
            String mangaId = clickedManga.getId();

            String mangaTitle = "Без названия";
            if (clickedManga.getAttributes() != null &&
                    clickedManga.getAttributes().getTitle() != null &&
                    clickedManga.getAttributes().getTitle().getEnglishTitle() != null) {
                mangaTitle = clickedManga.getAttributes().getTitle().getEnglishTitle();
            }

            Intent intent = new Intent(context, ChaptersActivity.class);
            intent.putExtra("MANGA_ID", mangaId);
            intent.putExtra("MANGA_TITLE", mangaTitle);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return mangaList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewCover;
        TextView textViewTitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewCover = itemView.findViewById(R.id.imageViewCover);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
        }
    }
}
