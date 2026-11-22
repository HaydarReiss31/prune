package com.haydareiss.prune;

import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.haydareiss.prune.databinding.ActivitySettingsBinding;

/** Settings screen: theme, pure black, language and about. */
public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemePrefs.applyOverlay(this);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, 0, bars.right, bars.bottom);
            return insets;
        });

        binding.toolbar.setNavigationOnClickListener(
                v -> getOnBackPressedDispatcher().onBackPressed());

        binding.swBlack.setChecked(ThemePrefs.pureBlack(this));
        binding.swBlack.setOnCheckedChangeListener((b, checked) -> {
            ThemePrefs.setPureBlack(this, checked);
            recreate();
        });
        binding.rowBlack.setOnClickListener(v -> binding.swBlack.toggle());

        binding.rowTheme.setOnClickListener(v -> chooseTheme());
        binding.rowLanguage.setOnClickListener(v -> chooseLanguage());
        binding.rowAbout.setOnClickListener(v -> showAbout());

        binding.tvVersion.setText(getString(R.string.settings_version, versionName()));
        refreshValues();
    }

    private String versionName() {
        try {
            String v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return v == null ? "" : v;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    private int themeIndex() {
        int mode = ThemePrefs.mode(this);
        return mode == AppCompatDelegate.MODE_NIGHT_NO ? 1
                : mode == AppCompatDelegate.MODE_NIGHT_YES ? 2 : 0;
    }

    private int languageIndex() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        String lang = locales.isEmpty() || locales.get(0) == null ? "" : locales.get(0).getLanguage();
        return "en".equals(lang) ? 1 : "tr".equals(lang) ? 2 : 0;
    }

    private String[] themeNames() {
        return new String[]{getString(R.string.theme_system), getString(R.string.theme_light),
                getString(R.string.theme_dark)};
    }

    private String[] languageNames() {
        return new String[]{getString(R.string.lang_system), getString(R.string.lang_english),
                getString(R.string.lang_turkish)};
    }

    private void refreshValues() {
        binding.tvTheme.setText(themeNames()[themeIndex()]);
        binding.tvLanguage.setText(languageNames()[languageIndex()]);
    }

    private void chooseTheme() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.menu_theme)
                .setSingleChoiceItems(themeNames(), themeIndex(), (dialog, which) -> {
                    dialog.dismiss();
                    // AppCompat recreates every open activity when the mode really changes.
                    ThemePrefs.setMode(this, which == 1 ? AppCompatDelegate.MODE_NIGHT_NO
                            : which == 2 ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    refreshValues();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void chooseLanguage() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.menu_language)
                .setSingleChoiceItems(languageNames(), languageIndex(), (dialog, which) -> {
                    dialog.dismiss();
                    AppCompatDelegate.setApplicationLocales(which == 1
                            ? LocaleListCompat.forLanguageTags("en")
                            : which == 2 ? LocaleListCompat.forLanguageTags("tr")
                            : LocaleListCompat.getEmptyLocaleList());
                    refreshValues();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showAbout() {
        // The removal list may still need parsing, so count it off the main thread.
        new Thread(() -> {
            final int size = UadRepository.load(getApplicationContext()).size();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.about_title)
                        .setMessage(getString(R.string.about_message, size))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            });
        }, "prune-about").start();
    }
}
