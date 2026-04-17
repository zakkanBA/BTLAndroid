package com.example.btland.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CloudinaryHelper {

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onError(String errorMessage);
    }

    private static final String CLOUD_NAME = "dywqmxdx4";
    private static final String UPLOAD_PRESET = "btland_unsigned";

    public static void uploadImage(Context context, Uri imageUri, UploadCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                File file = createTempFileFromUri(context, imageUri);
                if (file == null || !file.exists()) {
                    callback.onError("Không đọc được file ảnh");
                    return;
                }

                OkHttpClient client = new OkHttpClient();

                RequestBody fileBody = RequestBody.create(file, MediaType.parse("image/*"));

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", file.getName(), fileBody)
                        .addFormDataPart("upload_preset", UPLOAD_PRESET)
                        .build();

                String url = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

                Request request = new Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    callback.onError("Upload lỗi: " + responseBody);
                    return;
                }

                JSONObject jsonObject = new JSONObject(responseBody);
                String secureUrl = jsonObject.getString("secure_url");
                callback.onSuccess(secureUrl);

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    private static File createTempFileFromUri(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            String fileName = getFileName(context, uri);
            if (fileName == null) {
                fileName = "upload_" + System.currentTimeMillis() + ".jpg";
            }

            File tempFile = new File(context.getCacheDir(), fileName);
            FileOutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            return tempFile;
        } catch (Exception e) {
            return null;
        }
    }

    private static String getFileName(Context context, Uri uri) {
        String result = null;

        if ("content".equals(uri.getScheme())) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }

        if (result == null) {
            result = uri.getLastPathSegment();
        }

        return result;
    }
}