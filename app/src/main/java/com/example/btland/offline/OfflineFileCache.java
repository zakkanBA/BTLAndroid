package com.example.btland.offline;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class OfflineFileCache {

    private OfflineFileCache() {
    }

    public static List<String> copyUris(Context context, List<Uri> sourceUris, String prefix) throws Exception {
        List<String> cachedUris = new ArrayList<>();
        if (sourceUris == null) {
            return cachedUris;
        }

        for (Uri uri : sourceUris) {
            cachedUris.add(copyUri(context, uri, prefix).toString());
        }
        return cachedUris;
    }

    public static String copyUriNullable(Context context, Uri sourceUri, String prefix) throws Exception {
        if (sourceUri == null) {
            return "";
        }
        return copyUri(context, sourceUri, prefix).toString();
    }

    private static Uri copyUri(Context context, Uri sourceUri, String prefix) throws Exception {
        File directory = new File(context.getFilesDir(), "offline_media");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Không tạo được thư mục offline_media");
        }

        File target = new File(directory, prefix + "_" + System.nanoTime() + ".jpg");
        try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
             FileOutputStream outputStream = new FileOutputStream(target)) {
            if (inputStream == null) {
                throw new IllegalStateException("Không đọc được ảnh offline");
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        }
        return Uri.fromFile(target);
    }
}
