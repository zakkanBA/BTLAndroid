package com.example.btland;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.btland.utils.ThemePreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.lang.ref.WeakReference;

public class BTLApplication extends Application {

    private WeakReference<Activity> currentActivityRef = new WeakReference<>(null);
    private FirebaseAuth.AuthStateListener authStateListener;
    private ListenerRegistration banListenerRegistration;
    private String watchedUserId;
    private boolean banDialogShowing;
    private boolean pendingBanDialog;

    @Override
    public void onCreate() {
        super.onCreate();
        ThemePreferences.applySavedNightMode(this);
        registerForegroundActivityTracker();
        registerBanStateWatcher();
    }

    private void registerForegroundActivityTracker() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                currentActivityRef = new WeakReference<>(activity);
                if (pendingBanDialog) {
                    showBannedDialogIfPossible();
                }
            }

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                Activity currentActivity = currentActivityRef.get();
                if (currentActivity == activity) {
                    currentActivityRef = new WeakReference<>(null);
                }
            }
        });
    }

    private void registerBanStateWatcher() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null) {
                stopBanListener();
                return;
            }
            startBanListener(user.getUid());
        };
        auth.addAuthStateListener(authStateListener);
    }

    private void startBanListener(String userId) {
        if (userId.equals(watchedUserId) && banListenerRegistration != null) {
            return;
        }

        stopBanListener();
        watchedUserId = userId;
        banListenerRegistration = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot == null || !snapshot.exists()) {
                        return;
                    }
                    boolean isBanned = Boolean.TRUE.equals(snapshot.getBoolean("isBanned"));
                    if (isBanned) {
                        pendingBanDialog = true;
                        showBannedDialogIfPossible();
                    }
                });
    }

    private void stopBanListener() {
        if (banListenerRegistration != null) {
            banListenerRegistration.remove();
            banListenerRegistration = null;
        }
        watchedUserId = null;
        pendingBanDialog = false;
        banDialogShowing = false;
    }

    private void showBannedDialogIfPossible() {
        Activity activity = currentActivityRef.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || banDialogShowing) {
            return;
        }

        pendingBanDialog = false;
        banDialogShowing = true;
        activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
                .setTitle("Tài khoản đã bị khóa")
                .setMessage("Tài khoản của bạn đã bị quản trị viên khóa. Ứng dụng sẽ thoát sau khi bạn xác nhận.")
                .setCancelable(false)
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    activity.finishAffinity();
                    banDialogShowing = false;
                })
                .show());
    }
}
