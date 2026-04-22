package com.example.manga_reader.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.manga_reader.R;
import com.example.manga_reader.data.models.ChapterResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ViewHolder> {

    private Context context;
    private List<ChapterResponse> chapters;
    private String mangaTitle;
    private String mangaId;

    public ChapterAdapter(Context context, List<ChapterResponse> chapters) {
        this.context = context;
        this.chapters = chapters;
    }

    public void setMangaTitle(String mangaTitle) {
        this.mangaTitle = mangaTitle;
    }

    public void setMangaId(String mangaId) {
        this.mangaId = mangaId;
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
        String language = chapter.getLanguage();

        // Формируем полное название главы для отображения
        String mainText = (chapterNum != null && !chapterNum.isEmpty())
                ? "Глава " + chapterNum
                : "Без номера";

        if (title != null && !title.isEmpty()) {
            mainText += ": " + title;
        }

        String langDisplay = getLanguageDisplay(language);
        holder.textTitle.setText(mainText);
        holder.textInfo.setText("📄 " + pages + " • " + langDisplay);

        // Проверяем, прочитана ли глава
        SharedPreferences prefs = context.getSharedPreferences("manga_reader_prefs", Context.MODE_PRIVATE);
        Set<String> readChapters = prefs.getStringSet("read_chapters", new HashSet<>());

        if (readChapters.contains(chapter.getId())) {
            // Прочитанная глава - делаем серой
            holder.textTitle.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            holder.textInfo.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            holder.itemView.setAlpha(0.7f);
        } else {
            // Непрочитанная глава - белая
            holder.textTitle.setTextColor(context.getResources().getColor(android.R.color.white));
            holder.textInfo.setTextColor(context.getResources().getColor(android.R.color.white));
            holder.itemView.setAlpha(1.0f);
        }

        holder.itemView.setOnClickListener(v -> {
            String chapterId = chapter.getId();

            // Формируем заголовок для ReaderActivity
            StringBuilder chapterTitle = new StringBuilder();
            if (mangaTitle != null && !mangaTitle.isEmpty()) {
                chapterTitle.append(mangaTitle);
            }
            if (chapterNum != null && !chapterNum.isEmpty()) {
                if (chapterTitle.length() > 0) chapterTitle.append(" - ");
                chapterTitle.append("Гл. ").append(chapterNum);
            }
            if (title != null && !title.isEmpty()) {
                if (chapterTitle.length() > 0) chapterTitle.append(": ");
                chapterTitle.append(title);
            }

            Intent intent = new Intent(context, ReaderActivity.class);
            intent.putExtra("CHAPTER_ID", chapterId);
            intent.putExtra("CHAPTER_TITLE", chapterTitle.toString());
            intent.putExtra("MANGA_ID", mangaId);
            context.startActivity(intent);
        });
    }

    private String getLanguageDisplay(String code) {
        if (code == null) return "🌐";
        switch (code.toLowerCase()) {
            case "ru": return "🇷🇺 RU";
            case "en": return "🇬🇧 EN";
            case "gb": return "🇬🇧 EN";
            case "us": return "🇺🇸 EN";
            case "ja": return "🇯🇵 JP";
            case "ko": return "🇰🇷 KR";
            case "zh": return "🇨🇳 ZH";
            case "fr": return "🇫🇷 FR";
            case "es": return "🇪🇸 ES";
            case "pt": return "🇵🇹 PT";
            case "br": return "🇧🇷 BR";
            case "it": return "🇮🇹 IT";
            case "de": return "🇩🇪 DE";
            default: return "🌐 " + code.toUpperCase();
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