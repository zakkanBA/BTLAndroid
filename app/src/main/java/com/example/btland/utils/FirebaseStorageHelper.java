package com.example.btland.utils;

import android.net.Uri;
import android.text.TextUtils;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public final class FirebaseStorageHelper {

    public interface UploadCallback {
        void onSuccess(String downloadUrl, String storagePath);
        void onError(String errorMessage);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private FirebaseStorageHelper() {
    }

    public static void uploadFile(Uri uri, String storagePath, UploadCallback callback) {
        if (uri == null) {
            callback.onError("Không có tệp nào được chọn để tải lên.");
            return;
        }

        StorageReference reference = rootReference().child(storagePath);
        reference.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new IllegalStateException("Upload failed");
                    }
                    return reference.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri ->
                        callback.onSuccess(downloadUri.toString(), storagePath))
                .addOnFailureListener(e -> callback.onError(describeError(e)));
    }

    public static void deleteFolder(String folderPath, ActionCallback callback) {
        if (folderPath == null || folderPath.trim().isEmpty()) {
            callback.onSuccess();
            return;
        }
        deleteReference(rootReference().child(folderPath), callback);
    }

    private static void deleteReference(StorageReference reference, ActionCallback callback) {
        reference.listAll()
                .addOnSuccessListener(result -> deleteListResult(reference, result, callback))
                .addOnFailureListener(e -> {
                    if (e.getMessage() != null && e.getMessage().contains("Object does not exist")) {
                        callback.onSuccess();
                    } else {
                        callback.onError(describeError(e));
                    }
                });
    }

    private static void deleteListResult(StorageReference reference, ListResult result, ActionCallback callback) {
        List<Task<?>> tasks = new ArrayList<>();
        for (StorageReference item : result.getItems()) {
            tasks.add(item.delete());
        }
        for (StorageReference prefix : result.getPrefixes()) {
            tasks.add(prefix.listAll().continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException() != null
                            ? task.getException()
                            : new IllegalStateException("Delete folder failed");
                }
                ListResult nested = task.getResult();
                List<Task<?>> nestedTasks = new ArrayList<>();
                for (StorageReference item : nested.getItems()) {
                    nestedTasks.add(item.delete());
                }
                for (StorageReference childPrefix : nested.getPrefixes()) {
                    nestedTasks.add(childPrefix.delete());
                }
                return Tasks.whenAllComplete(nestedTasks);
            }));
        }

        Tasks.whenAllComplete(tasks)
                .addOnSuccessListener(unused ->
                        reference.delete()
                                .addOnSuccessListener(v -> callback.onSuccess())
                                .addOnFailureListener(e -> callback.onSuccess()))
                .addOnFailureListener(e -> callback.onError(describeError(e)));
    }

    private static StorageReference rootReference() {
        String bucket = FirebaseApp.getInstance().getOptions().getStorageBucket();
        if (TextUtils.isEmpty(bucket)) {
            return FirebaseStorage.getInstance().getReference();
        }

        String bucketUrl = bucket.startsWith("gs://") ? bucket : "gs://" + bucket;
        return FirebaseStorage.getInstance(bucketUrl).getReference();
    }

    private static String describeError(Exception exception) {
        if (exception instanceof StorageException) {
            StorageException storageException = (StorageException) exception;
            switch (storageException.getErrorCode()) {
                case StorageException.ERROR_BUCKET_NOT_FOUND:
                    return "Bucket Firebase Storage chưa được tạo hoặc sai tên bucket. Hãy kiểm tra lại google-services.json và phần Storage trong Firebase Console.";
                case StorageException.ERROR_PROJECT_NOT_FOUND:
                    return "Không tìm thấy project Firebase Storage. Hãy kiểm tra đúng project btland-4085e và bật dịch vụ Storage.";
                case StorageException.ERROR_NOT_AUTHENTICATED:
                    return "Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại rồi thử tải ảnh.";
                case StorageException.ERROR_NOT_AUTHORIZED:
                    return "Storage Rules đang chặn quyền tải ảnh. Hãy publish file storage.rules lên Firebase Console.";
                case StorageException.ERROR_QUOTA_EXCEEDED:
                    return "Firebase Storage đã vượt hạn mức hiện tại.";
                case StorageException.ERROR_RETRY_LIMIT_EXCEEDED:
                    return "Kết nối mạng không ổn định hoặc Storage phản hồi quá chậm. Hãy kiểm tra Internet rồi thử lại.";
                case StorageException.ERROR_OBJECT_NOT_FOUND:
                    return "Tệp trên Firebase Storage không tồn tại hoặc vừa bị xóa.";
                case StorageException.ERROR_INVALID_CHECKSUM:
                    return "Tệp tải lên bị lỗi checksum. Hãy chọn lại ảnh rồi thử lại.";
                default:
                    break;
            }
        }

        String message = exception == null ? null : exception.getMessage();
        return TextUtils.isEmpty(message)
                ? "Có lỗi chưa xác định khi làm việc với Firebase Storage."
                : "Firebase Storage lỗi: " + message;
    }
}
