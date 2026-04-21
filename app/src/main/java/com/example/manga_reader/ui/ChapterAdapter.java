package com.example.manga_reader.ui;

import android.content.Context;
import android.content.Intent;
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
import com.example.manga_reader.ui.ReaderActivity;

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
        View view = LayoutInflater.from(context).inflate(R.layout.item_chapter, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChapterResponse chapter = chapters.get(position);

        String chapterNum = chapter.getAttributes().getChapter();
        String title = chapter.getAttributes().getTitle();
        int pages = chapter.getAttributes().getPages();
        String language = chapter.getAttributes().getLanguage(); // Получаем язык

        String mainText = (chapterNum != null && !chapterNum.isEmpty())
                ? "Глава " + chapterNum
                : "Без номера";

        if (title != null && !title.isEmpty()) {
            mainText += ": " + title;
        }
        String langDisplay = getLanguageDisplay(language);
        holder.textTitle.setText(mainText);
        holder.textInfo.setText("📄 " + pages + " • " + langDisplay);

        holder.itemView.setOnClickListener(v -> {
            String chapterId = chapter.getId();
            Intent intent = new Intent(context, ReaderActivity.class);
            intent.putExtra("CHAPTER_ID", chapterId);
            context.startActivity(intent);
        });
    }
    private String getLanguageDisplay(String code) {
        if (code == null) return "?";
        switch (code) {
            case "ru": return "🇷🇺 RU";
            case "en": return "🇬🇧 EN";
            case "ja": return "🇯🇵 JP";
            case "zh": return "🇨🇳 ZH";
            default: return code.toUpperCase();
        }
    }

    @Override
    public int getItemCount() {
        return chapters != null ? chapters.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textViewChapterTitle);
            textInfo = itemView.findViewById(R.id.textViewChapterInfo);
        }
    }
}
