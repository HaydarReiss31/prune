package com.haydareiss.pkgx;

/** One row in the list: an installed app, or an app removed for the current user. */
final class AppEntry {
    final String label;
    final String packageName;
    final boolean system;
    /** False when the app is currently enabled=false (frozen). */
    final boolean enabled;
    /** False for apps that were removed for this user (listed in the Removed tab). */
    final boolean installed;
    /** True when Prune refuses to remove it (see {@link Safety}). */
    final boolean protectedApp;
    final UadInfo info;

    AppEntry(String label, String packageName, boolean system, boolean enabled,
             boolean installed, boolean protectedApp, UadInfo info) {
        this.label = label;
        this.packageName = packageName;
        this.system = system;
        this.enabled = enabled;
        this.installed = installed;
        this.protectedApp = protectedApp;
        this.info = info;
    }
}
