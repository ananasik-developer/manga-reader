package com.example.manga_reader.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.manga_reader.R;
import com.example.manga_reader.data.models.ChapterResponse;
import java.util.List;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ViewHolder> {

    private Context context;
    private List<ChapterResponse> chapters;

    public ChapterAdapter(Context context, List<ChapterResponse> chapters) {
        this.context = context;
        this.chapters = chapters;
    }

    public void setChapters(List<ChapterResponse> chapters) {
        this.chapters = chapters;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChapterResponse chapter = chapters.get(position);

        String chapterNum = chapter.getAttributes().getChapter();
        String title = chapter.getAttributes().getTitle();
        int pages = chapter.getAttributes().getPages();

        String mainText = (chapterNum != null && !chapterNum.isEmpty())
                ? "Глава " + chapterNum
                : "Без номера";

        if (title != null && !title.isEmpty()) {
            mainText += ": " + title;
        }

        holder.text1.setText(mainText);
        holder.text2.setText("Страниц: " + pages);

        holder.itemView.setOnClickListener(v -> {
            String chapterId = chapter.getId();
            Toast.makeText(context, "ID главы: " + chapterId, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return chapters != null ? chapters.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }
}
