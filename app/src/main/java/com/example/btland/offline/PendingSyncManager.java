package com.example.btland.offline;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.example.btland.models.Message;
import com.example.btland.utils.FirebaseStorageHelper;
import com.example.btland.utils.NetworkUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PendingSyncManager {

    public interface QueueCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private static final String TYPE_POST = "post";
    private static final String TYPE_MESSAGE = "message";

    private static volatile PendingSyncManager instance;

    private final Context appContext;
    private final PendingActionDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();
    private boolean syncing;
    private boolean syncRequested;
    private boolean networkCallbackRegistered;

    private PendingSyncManager(Context context) {
        appContext = context.getApplicationContext();
        dao = OfflineDatabase.getInstance(appContext).pendingActionDao();
    }

    public static PendingSyncManager getInstance(Context context) {
        if (instance == null) {
            synchronized (PendingSyncManager.class) {
                if (instance == null) {
                    instance = new PendingSyncManager(context);
                }
            }
        }
        return instance;
    }

    public void start() {
        registerNetworkCallback();
        syncNow();
    }

    public void queuePost(PendingPostPayload payload, QueueCallback callback) {
        queue(TYPE_POST, payload, callback);
    }

    public void queueMessage(PendingMessagePayload payload, QueueCallback callback) {
        queue(TYPE_MESSAGE, payload, callback);
    }

    private void queue(String type, Object payload, QueueCallback callback) {
        executor.execute(() -> {
            try {
                PendingAction action = new PendingAction();
                action.type = type;
                action.encryptedPayload = OfflineCrypto.encrypt(gson.toJson(payload));
                action.createdAt = System.currentTimeMillis();
                dao.insert(action);
                mainHandler.post(callback::onSuccess);
                syncNow();
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(errorMessage(e)));
            }
        });
    }

    public void syncNow() {
        if (syncing) {
            syncRequested = true;
            return;
        }
        if (!NetworkUtils.isOnline(appContext) || FirebaseAuth.getInstance().getUid() == null) {
            return;
        }

        syncing = true;
        syncRequested = false;
        executor.execute(() -> {
            List<PendingAction> actions = dao.getAll();
            mainHandler.post(() -> syncNext(actions, 0));
        });
    }

    private void syncNext(List<PendingAction> actions, int index) {
        if (index >= actions.size()) {
            syncing = false;
            if (syncRequested) {
                syncNow();
            }
            return;
        }

        PendingAction action = actions.get(index);
        try {
            String json = OfflineCrypto.decrypt(action.encryptedPayload);
            if (TYPE_MESSAGE.equals(action.type)) {
                syncMessage(action, gson.fromJson(json, PendingMessagePayload.class), () -> syncNext(actions, index + 1));
                return;
            }
            if (TYPE_POST.equals(action.type)) {
                syncPost(action, gson.fromJson(json, PendingPostPayload.class), () -> syncNext(actions, index + 1));
                return;
            }
            deleteAction(action.id, () -> syncNext(actions, index + 1));
        } catch (Exception e) {
            syncing = false;
        }
    }

    private void syncMessage(PendingAction action, PendingMessagePayload payload, Runnable onDone) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Timestamp timestamp = new Timestamp(new Date(payload.createdAt));
        Message message = new Message(payload.senderId, payload.receiverId, payload.content, timestamp, false);
        message.setMessageId(payload.messageId);

        db.collection("conversations")
                .document(payload.conversationId)
                .collection("messages")
                .document(payload.messageId)
                .set(message)
                .addOnSuccessListener(unused -> {
                    Map<String, Object> participantNames = new HashMap<>();
                    participantNames.put(payload.senderId, payload.senderName);
                    participantNames.put(payload.receiverId, payload.receiverName);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("conversationId", payload.conversationId);
                    updates.put("userIds", Arrays.asList(payload.senderId, payload.receiverId));
                    updates.put("lastMessage", payload.content);
                    updates.put("lastTimestamp", timestamp);
                    updates.put("participantNames", participantNames);
                    updates.put("unreadCounts." + payload.senderId, 0L);
                    updates.put("unreadCounts." + payload.receiverId, FieldValue.increment(1));

                    db.collection("conversations")
                            .document(payload.conversationId)
                            .set(updates, SetOptions.merge())
                            .addOnSuccessListener(result -> deleteAction(action.id, onDone))
                            .addOnFailureListener(e -> syncing = false);
                })
                .addOnFailureListener(e -> syncing = false);
    }

    private void syncPost(PendingAction action, PendingPostPayload payload, Runnable onDone) {
        String storageFolder = "posts/" + payload.postId;
        List<String> uploadedImageUrls = new ArrayList<>();
        uploadPostImages(payload, storageFolder, 0, uploadedImageUrls, new UploadSequenceCallback() {
            @Override
            public void onSuccess() {
                uploadPostPanorama(payload, storageFolder, new PanoramaCallback() {
                    @Override
                    public void onComplete(String panoramaUrl, String panoramaPath) {
                        savePostDocument(action, payload, storageFolder, uploadedImageUrls, panoramaUrl, panoramaPath, onDone);
                    }

                    @Override
                    public void onError() {
                        syncing = false;
                    }
                });
            }

            @Override
            public void onError() {
                syncing = false;
            }
        });
    }

    private void uploadPostImages(PendingPostPayload payload, String storageFolder, int index,
                                  List<String> uploadedImageUrls, UploadSequenceCallback callback) {
        if (index >= payload.imageUris.size()) {
            callback.onSuccess();
            return;
        }

        String storagePath = storageFolder + "/image_" + System.currentTimeMillis() + "_" + index + ".jpg";
        FirebaseStorageHelper.uploadFile(Uri.parse(payload.imageUris.get(index)), storagePath, new FirebaseStorageHelper.UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl, String path) {
                uploadedImageUrls.add(downloadUrl);
                uploadPostImages(payload, storageFolder, index + 1, uploadedImageUrls, callback);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError();
            }
        });
    }

    private void uploadPostPanorama(PendingPostPayload payload, String storageFolder, PanoramaCallback callback) {
        if (payload.panoramaUri == null || payload.panoramaUri.trim().isEmpty()) {
            callback.onComplete("", "");
            return;
        }

        String storagePath = storageFolder + "/panorama_" + System.currentTimeMillis() + ".jpg";
        FirebaseStorageHelper.uploadFile(Uri.parse(payload.panoramaUri), storagePath, new FirebaseStorageHelper.UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl, String path) {
                callback.onComplete(downloadUrl, path);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError();
            }
        });
    }

    private void savePostDocument(PendingAction action, PendingPostPayload payload, String storageFolder,
                                  List<String> imageUrls, String panoramaUrl, String panoramaPath, Runnable onDone) {
        Timestamp timestamp = new Timestamp(new Date(payload.createdAt));
        Map<String, Object> post = new HashMap<>();
        post.put("postId", payload.postId);
        post.put("userId", payload.userId);
        post.put("ownerName", payload.ownerName);
        post.put("ownerPhone", payload.ownerPhone);
        post.put("title", payload.title);
        post.put("description", payload.description);
        post.put("price", payload.price);
        post.put("area", payload.area);
        post.put("address", payload.address);
        post.put("district", payload.district);
        post.put("roomType", payload.roomType);
        post.put("lat", payload.lat);
        post.put("lng", payload.lng);
        post.put("type", payload.type);
        post.put("amenities", payload.amenities);
        post.put("images", imageUrls);
        post.put("panoramaImage", panoramaUrl);
        post.put("panoramaPath", panoramaPath);
        post.put("storageFolder", storageFolder);
        post.put("status", "active");
        post.put("active", true);
        post.put("adminHiddenByBan", false);
        post.put("createdAt", timestamp);
        post.put("updatedAt", Timestamp.now());

        FirebaseFirestore.getInstance()
                .collection("posts")
                .document(payload.postId)
                .set(post)
                .addOnSuccessListener(unused -> deleteAction(action.id, onDone))
                .addOnFailureListener(e -> syncing = false);
    }

    private void deleteAction(long id, Runnable onDeleted) {
        executor.execute(() -> {
            dao.deleteById(id);
            mainHandler.post(onDeleted);
        });
    }

    private void registerNetworkCallback() {
        if (networkCallbackRegistered) {
            return;
        }

        ConnectivityManager manager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return;
        }

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        manager.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                syncNow();
            }
        });
        networkCallbackRegistered = true;
    }

    private String errorMessage(Exception e) {
        return e == null || e.getMessage() == null ? "Không lưu được hàng đợi offline" : e.getMessage();
    }

    interface UploadSequenceCallback {
        void onSuccess();
        void onError();
    }

    interface PanoramaCallback {
        void onComplete(String panoramaUrl, String panoramaPath);
        void onError();
    }
}
