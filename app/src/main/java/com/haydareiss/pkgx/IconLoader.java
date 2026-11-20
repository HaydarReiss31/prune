package com.haydareiss.pkgx;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Loads app icons on demand so hundreds of rows do not hold hundreds of bitmaps. */
final class IconLoader {

    private final PackageManager pm;
    private final LruCache<String, Drawable> cache = new LruCache<>(150);
    private final ExecutorService pool = Executors.newFixedThreadPool(3);
    private final Handler main = new Handler(Looper.getMainLooper());

    IconLoader(PackageManager pm) {
        this.pm = pm;
    }

    void load(final String packageName, final ImageView view) {
        view.setTag(packageName);
        Drawable cached = cache.get(packageName);
        if (cached != null) {
            view.setImageDrawable(cached);
            return;
        }
        view.setImageResource(android.R.drawable.sym_def_app_icon);
        pool.execute(() -> {
            final Drawable icon;
            try {
                // MATCH_UNINSTALLED_PACKAGES: apps removed for this user still have an icon.
                @SuppressWarnings("deprecation")
                ApplicationInfo ai = pm.getApplicationInfo(packageName,
                        PackageManager.MATCH_UNINSTALLED_PACKAGES);
                icon = ai.loadIcon(pm);
            } catch (Throwable t) {
                return;
            }
            cache.put(packageName, icon);
            main.post(() -> {
                if (packageName.equals(view.getTag())) view.setImageDrawable(icon);
            });
        });
    }

    void shutdown() {
        pool.shutdownNow();
    }
}
