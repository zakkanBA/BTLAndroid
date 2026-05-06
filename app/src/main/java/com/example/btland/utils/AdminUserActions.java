package com.example.btland.utils;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public final class AdminUserActions {

    public interface ActionCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private AdminUserActions() {
    }

    public static void setUserBanned(String userId, boolean banned, ActionCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("User không hợp lệ");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();
                    DocumentReference userRef = db.collection("users").document(userId);
                    batch.update(userRef, "isBanned", banned);

                    for (DocumentSnapshot postDoc : querySnapshot.getDocuments()) {
                        Map<String, Object> updates = buildPostBanUpdates(postDoc, banned);
                        if (!updates.isEmpty()) {
                            batch.update(postDoc.getReference(), updates);
                        }
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(errorMessage(e)));
                })
                .addOnFailureListener(e -> callback.onError(errorMessage(e)));
    }

    private static Map<String, Object> buildPostBanUpdates(DocumentSnapshot postDoc, boolean banned) {
        Map<String, Object> updates = new HashMap<>();
        boolean active = !Boolean.FALSE.equals(postDoc.getBoolean("active"));
        boolean hiddenByBan = Boolean.TRUE.equals(postDoc.getBoolean("adminHiddenByBan"));

        if (banned && active) {
            updates.put("active", false);
            updates.put("status", "hidden");
            updates.put("adminHiddenByBan", true);
            updates.put("updatedAt", Timestamp.now());
            return updates;
        }

        if (!banned && hiddenByBan) {
            updates.put("active", true);
            updates.put("status", "active");
            updates.put("adminHiddenByBan", false);
            updates.put("updatedAt", Timestamp.now());
        }
        return updates;
    }

    private static String errorMessage(Exception e) {
        return e == null || e.getMessage() == null ? "Không cập nhật được tài khoản" : e.getMessage();
    }
}
