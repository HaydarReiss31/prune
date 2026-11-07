package com.haydareiss.pkgx;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Packages Prune refuses to uninstall, regardless of what the removal list says. */
final class Safety {

    static final String MANAGER_PACKAGE = "com.hamondev.shevery";

    private static final Set<String> CORE = new HashSet<>(Arrays.asList(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.phone",
            "com.android.shell",
            "com.android.keychain",
            "com.android.inputdevices",
            "com.android.server.telecom",
            "com.android.providers.settings",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller"
    ));

    private Safety() {
    }

    /**
     * Packages that depend on the current device state: Prune itself, the Shevery manager,
     * the default launcher and the enabled keyboards. Call from a background thread.
     */
    static Set<String> dynamicProtected(Context context) {
        Set<String> out = new HashSet<>();
        out.add(context.getPackageName());
        out.add(MANAGER_PACKAGE);
        PackageManager pm = context.getPackageManager();
        try {
            Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            ResolveInfo def = pm.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
            if (def != null && def.activityInfo != null) {
                String pkg = def.activityInfo.packageName;
                if ("android".equals(pkg)) {
                    // No default launcher chosen yet: protect every launcher.
                    List<ResolveInfo> all = pm.queryIntentActivities(home, 0);
                    for (ResolveInfo ri : all) {
                        if (ri.activityInfo != null) out.add(ri.activityInfo.packageName);
                    }
                } else {
                    out.add(pkg);
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            InputMethodManager imm =
                    (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                for (InputMethodInfo info : imm.getEnabledInputMethodList()) {
                    out.add(info.getPackageName());
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    static boolean isProtected(String packageName, Set<String> dynamic) {
        return CORE.contains(packageName) || dynamic.contains(packageName);
    }
}
