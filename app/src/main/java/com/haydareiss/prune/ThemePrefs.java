package com.haydareiss.prune;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

/** Stores and applies the theme choice (light/dark/system) and the pure-black dark option. */
final class ThemePrefs {

    private static final String FILE = "theme";
    private static final String KEY_MODE = "mode";
    private static final String KEY_BLACK = "pure_black";

    private ThemePrefs() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static int mode(Context c) {
        return prefs(c).getInt(KEY_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    static void setMode(Context c, int mode) {
        prefs(c).edit().putInt(KEY_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    static boolean pureBlack(Context c) {
        return prefs(c).getBoolean(KEY_BLACK, false);
    }

    static void setPureBlack(Context c, boolean on) {
        prefs(c).edit().putBoolean(KEY_BLACK, on).apply();
    }

    /** Call once at startup. */
    static void applySavedMode(Context c) {
        AppCompatDelegate.setDefaultNightMode(mode(c));
    }

    /** Call from Activity.onCreate after super.onCreate and before inflating the layout. */
    static void applyOverlay(android.app.Activity a) {
        boolean night = (a.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (night && pureBlack(a)) {
            a.getTheme().applyStyle(R.style.ThemeOverlay_Prune_PureBlack, true);
        }
    }
}
