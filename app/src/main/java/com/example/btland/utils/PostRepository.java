package com.example.btland.utils;

import android.content.Context;

import com.example.btland.models.Post;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public final class PostRepository {

    public interface ActionCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private PostRepository() {
    }

    public static void setPostActive(Post post, boolean active, ActionCallback callback) {
        if (post == null || post.getPostId() == null || post.getPostId().trim().isEmpty()) {
            callback.onError("Bài đăng không hợp lệ");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("active", active);
        updates.put("status", active ? "active" : "hidden");
        updates.put("updatedAt", Timestamp.now());

        FirebaseFirestore.getInstance()
                .collection("posts")
                .document(post.getPostId())
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public static void deletePost(Context context, Post post, boolean adminAction, ActionCallback callback) {
        if (post == null || post.getPostId() == null || post.getPostId().trim().isEmpty()) {
            callback.onError("Bài đăng không hợp lệ");
            return;
        }

        FirebaseStorageHelper.deleteFolder(post.getStorageFolder(), new FirebaseStorageHelper.ActionCallback() {
            @Override
            public void onSuccess() {
                deletePostDocument(post, adminAction, callback);
            }

            @Override
            public void onError(String errorMessage) {
                deletePostDocument(post, adminAction, callback);
            }
        });
    }

    private static void deletePostDocument(Post post, boolean adminAction, ActionCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts")
                .document(post.getPostId())
                .delete()
                .addOnSuccessListener(unused -> {
                    if (adminAction && post.getUserId() != null && !post.getUserId().trim().isEmpty()) {
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("userId", post.getUserId());
                        notification.put("type", "admin_post_removed");
                        notification.put("postId", post.getPostId());
                        notification.put("title", "Bài đăng đã bị gỡ");
                        notification.put("message", "Quản trị viên đã xóa bài đăng \"" + post.getTitle() + "\" do vi phạm.");
                        notification.put("createdAt", FieldValue.serverTimestamp());
                        notification.put("read", false);
                        db.collection("notifications").add(notification);
                    }
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
