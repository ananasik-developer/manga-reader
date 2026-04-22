package com.example.manga_reader.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.example.manga_reader.R;
import java.util.ArrayList;
import java.util.List;

public class ReaderPagerAdapter extends RecyclerView.Adapter<ReaderPagerAdapter.PageViewHolder> {

    private Context context;
    private List<String> imageUrls;

    public ReaderPagerAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_reader_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        // Очищаем предыдущее изображение
        holder.photoView.setImageDrawable(null);

        // Загружаем новое
        Glide.with(context)
                .load(imageUrls.get(position))
                .placeholder(android.R.color.black)
                .into(holder.photoView);
    }

    @Override
    public int getItemCount() {
        return imageUrls != null ? imageUrls.size() : 0;
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        PhotoView photoView;

        public PageViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.photoView);
        }
    }
}