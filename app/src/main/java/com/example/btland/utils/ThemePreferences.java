package com.example.btland.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemePreferences {

    private static final String PREFS_NAME = "btland_preferences";
    private static final String KEY_AUTO_THEME = "auto_theme";
    private static final String KEY_MANUAL_MODE = "manual_mode";
    private static final String KEY_LAST_APPLIED_MODE = "last_applied_mode";

    private ThemePreferences() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isAutoThemeEnabled(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_THEME, true);
    }

    public static void setAutoThemeEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AUTO_THEME, enabled).apply();
    }

    public static int getManualNightMode(Context context) {
        return prefs(context).getInt(KEY_MANUAL_MODE, AppCompatDelegate.MODE_NIGHT_NO);
    }

    public static void setManualNightMode(Context context, int mode) {
        prefs(context).edit().putInt(KEY_MANUAL_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static int getLastAppliedMode(Context context) {
        return prefs(context).getInt(KEY_LAST_APPLIED_MODE, getManualNightMode(context));
    }

    public static void setLastAppliedMode(Context context, int mode) {
        prefs(context).edit().putInt(KEY_LAST_APPLIED_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static void applySavedNightMode(Context context) {
        if (isAutoThemeEnabled(context)) {
            AppCompatDelegate.setDefaultNightMode(getLastAppliedMode(context));
        } else {
            AppCompatDelegate.setDefaultNightMode(getManualNightMode(context));
        }
    }
}
