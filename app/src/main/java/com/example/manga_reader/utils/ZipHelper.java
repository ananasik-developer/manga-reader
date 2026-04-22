package com.example.manga_reader.utils;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipHelper {

    private static final String TAG = "ZipHelper";

    public static class UnzipResult {
        public boolean success;
        public String folderPath;
        public String coverPath;
        public int pageCount;
        public String errorMessage;
    }

    public static UnzipResult unzipManga(Context context, File zipFile, String mangaTitle) {
        UnzipResult result = new UnzipResult();

        // Проверяем, что файл существует и не пустой
        if (!zipFile.exists() || zipFile.length() == 0) {
            result.success = false;
            result.errorMessage = "Файл не найден или пустой";
            return result;
        }

        // Проверяем, что это ZIP архив
        if (!isZipFile(zipFile)) {
            result.success = false;
            result.errorMessage = "Файл не является ZIP/CBZ архивом";
            return result;
        }

        String safeTitle = mangaTitle.replaceAll("[^a-zA-Z0-9а-яА-Я_\\-]", "_");
        if (safeTitle.isEmpty()) {
            safeTitle = "manga_" + System.currentTimeMillis();
        }

        File targetDir = new File(context.getFilesDir(), "local_manga/" + safeTitle);

        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        List<String> imageFiles = new ArrayList<>();

        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            byte[] buffer = new byte[8192];

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (entry.isDirectory()) {
                    continue;
                }

                String entryName = entry.getName();

                // Пропускаем служебные файлы
                if (entryName.contains("__MACOSX") ||
                        entryName.startsWith(".") ||
                        entryName.endsWith(".DS_Store") ||
                        entryName.endsWith(".txt") ||
                        entryName.endsWith(".xml") ||
                        entryName.endsWith(".json")) {
                    continue;
                }

                // Проверяем, является ли файл изображением
                String lowerName = entryName.toLowerCase();
                if (!(lowerName.endsWith(".jpg") ||
                        lowerName.endsWith(".jpeg") ||
                        lowerName.endsWith(".png") ||
                        lowerName.endsWith(".webp") ||
                        lowerName.endsWith(".bmp") ||
                        lowerName.endsWith(".gif"))) {
                    continue;
                }

                // Берём только имя файла без пути
                String fileName = new File(entryName).getName();
                File outputFile = new File(targetDir, fileName);

                // Если файл уже есть, добавляем номер
                int counter = 1;
                String baseName = fileName;
                String extension = "";
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    baseName = fileName.substring(0, dotIndex);
                    extension = fileName.substring(dotIndex);
                }

                while (outputFile.exists()) {
                    outputFile = new File(targetDir, baseName + "_" + counter + extension);
                    counter++;
                }

                try (InputStream is = zip.getInputStream(entry);
                     FileOutputStream fos = new FileOutputStream(outputFile)) {
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }

                imageFiles.add(outputFile.getAbsolutePath());
            }

            // Сортируем изображения
            Collections.sort(imageFiles, (a, b) -> {
                String nameA = new File(a).getName();
                String nameB = new File(b).getName();

                int numA = extractNumber(nameA);
                int numB = extractNumber(nameB);
                if (numA != -1 && numB != -1) {
                    return Integer.compare(numA, numB);
                }

                return nameA.compareToIgnoreCase(nameB);
            });

            result.success = true;
            result.folderPath = targetDir.getAbsolutePath();
            result.pageCount = imageFiles.size();

            if (!imageFiles.isEmpty()) {
                result.coverPath = imageFiles.get(0);
            }

            Log.d(TAG, "Unzipped " + result.pageCount + " pages");

        } catch (Exception e) {
            result.success = false;
            result.errorMessage = e.getMessage();
            Log.e(TAG, "Unzip error: " + e.getMessage());

            try {
                deleteFolder(targetDir);
            } catch (Exception ex) {
                Log.e(TAG, "Delete error", ex);
            }
        }

        return result;
    }

    private static boolean isZipFile(File file) {
        if (file == null || !file.exists()) return false;

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] signature = new byte[4];
            if (fis.read(signature) != 4) return false;

            // Проверяем сигнатуру ZIP файла: PK (0x50 0x4B 0x03 0x04 или 0x50 0x4B 0x05 0x06)
            return (signature[0] == 0x50 && signature[1] == 0x4B &&
                    (signature[2] == 0x03 || signature[2] == 0x05) &&
                    signature[3] == 0x04) ||
                    (signature[0] == 0x50 && signature[1] == 0x4B &&
                            signature[2] == 0x05 && signature[3] == 0x06);
        } catch (Exception e) {
            return false;
        }
    }

    private static int extractNumber(String name) {
        StringBuilder numStr = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isDigit(c)) {
                numStr.append(c);
            }
        }
        if (numStr.length() > 0) {
            try {
                return Integer.parseInt(numStr.toString());
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public static List<String> getImagesFromFolder(String folderPath) {
        List<String> images = new ArrayList<>();
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            return images;
        }

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String name = file.getName().toLowerCase();
                    if (name.endsWith(".jpg") ||
                            name.endsWith(".jpeg") ||
                            name.endsWith(".png") ||
                            name.endsWith(".webp") ||
                            name.endsWith(".bmp") ||
                            name.endsWith(".gif")) {
                        images.add(file.getAbsolutePath());
                    }
                }
            }
        }

        Collections.sort(images, (a, b) -> {
            String nameA = new File(a).getName();
            String nameB = new File(b).getName();

            int numA = extractNumber(nameA);
            int numB = extractNumber(nameB);
            if (numA != -1 && numB != -1) {
                return Integer.compare(numA, numB);
            }

            return nameA.compareToIgnoreCase(nameB);
        });

        return images;
    }

    public static boolean deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteFolder(file);
                }
            }
        }
        return folder.delete();
    }
}