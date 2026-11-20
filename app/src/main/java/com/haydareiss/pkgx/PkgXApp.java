package com.haydareiss.pkgx;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

public class PkgXApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Material You colors on Android 12+, the static theme elsewhere.
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
