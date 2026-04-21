package com.example.manga_reader.ui;

import android.content.Context;
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

        // Название
        String title = "Без названия";
        if (manga.getAttributes() != null &&
                manga.getAttributes().getTitle() != null &&
                manga.getAttributes().getTitle().getEnglishTitle() != null) {  // ← getEnglishTitle(), а не getEn()
            title = manga.getAttributes().getTitle().getEnglishTitle();
        }
        holder.textViewTitle.setText(title);

        // Обложка (пока будет пусто, URL обложки мы добавим позже)
        // Пока поставим placeholder
        Glide.with(context)
                .load(R.drawable.ic_launcher_foreground) // временная заглушка
                .into(holder.imageViewCover);
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
