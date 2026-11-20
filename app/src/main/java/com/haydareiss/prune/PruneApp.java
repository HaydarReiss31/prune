package com.haydareiss.prune;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

public class PruneApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemePrefs.applySavedMode(this);
        // Material You colors on Android 12+, the static theme elsewhere.
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
