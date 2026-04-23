package com.example.btland;

import android.app.Application;

import com.example.btland.utils.ThemePreferences;

public class BTLApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemePreferences.applySavedNightMode(this);
    }
}
