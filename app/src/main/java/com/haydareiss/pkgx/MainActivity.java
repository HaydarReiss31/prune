package com.haydareiss.pkgx;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.hamondev.shevery.api.SheveryPrivilege;
import com.haydareiss.pkgx.databinding.ActivityMainBinding;
import com.haydareiss.pkgx.databinding.DialogConfirmBinding;
import com.haydareiss.pkgx.databinding.SheetDetailsBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMISSION = 1;
    private static final int PREVIEW_LIMIT = 12;
    private static final Pattern PACKAGE_NAME = Pattern.compile("^[A-Za-z0-9_.]+$");

    private ActivityMainBinding binding;
    private AppAdapter adapter;
    private IconLoader icons;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private final List<AppEntry> installed = new ArrayList<>();
    private final List<AppEntry> removed = new ArrayList<>();
    private final Set<String> selected = new LinkedHashSet<>();
    private List<AppEntry> shown = new ArrayList<>();

    private enum Sort { NAME, PACKAGE, RISK }

    private Sort sort = Sort.NAME;
    private boolean showRemoved = false;
    /** null means "all levels". */
    private UadInfo.Level levelFilter = null;
    private int uadSize = 0;

    private final SheveryPrivilege.OnBinderReceivedListener binderReceived = this::updateState;
    private final SheveryPrivilege.OnBinderDeadListener binderDead = this::updateState;
    private final SheveryPrivilege.OnPermissionResultListener permissionResult =
            (requestCode, grantResult) -> updateState();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        icons = new IconLoader(getPackageManager());
        adapter = new AppAdapter(icons, selected, new AppAdapter.Listener() {
            @Override
            public void onToggle(AppEntry entry, int position) {
                toggle(entry, position);
            }

            @Override
            public void onDetails(AppEntry entry) {
                showDetails(entry);
            }
        });
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);

        binding.toolbar.inflateMenu(R.menu.main_menu);
        binding.toolbar.setOnMenuItemClickListener(this::onMenuItem);

        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.tab_apps));
        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.tab_removed));
        binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showRemoved = tab.getPosition() == 1;
                selected.clear();
                binding.cbUser.setVisibility(showRemoved ? View.GONE : View.VISIBLE);
                applyFilter();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        binding.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        binding.chips.setOnCheckedStateChangeListener((group, ids) -> {
            levelFilter = ids.isEmpty() ? null : levelForChip(ids.get(0));
            applyFilter();
        });
        binding.cbUser.setOnCheckedChangeListener((b, checked) -> applyFilter());
        binding.btnAction.setOnClickListener(v -> onActionButton());
        binding.fab.setOnClickListener(v -> confirmAndRun(resolveSelected(), showRemoved));

        // Fast-pull: if the Shevery server is already running, grab its binder right away.
        try {
            SheveryPrivilege.connectFast(this);
        } catch (Throwable ignored) {
        }
        SheveryPrivilege.addBinderReceivedListenerSticky(binderReceived);
        SheveryPrivilege.addBinderDeadListener(binderDead);
        SheveryPrivilege.addPermissionResultListener(permissionResult);

        updateState();
        loadApps();
    }

    @Override
    protected void onDestroy() {
        SheveryPrivilege.removeBinderReceivedListener(binderReceived);
        SheveryPrivilege.removeBinderDeadListener(binderDead);
        SheveryPrivilege.removePermissionResultListener(permissionResult);
        io.shutdownNow();
        if (icons != null) icons.shutdown();
        super.onDestroy();
    }

    // ---------------------------------------------------------------- connection state

    private boolean isReady() {
        try {
            return SheveryPrivilege.pingBinder() && SheveryPrivilege.isGranted();
        } catch (Throwable t) {
            return false;
        }
    }

    private void updateState() {
        runOnUiThread(() -> {
            if (binding == null) return;
            if (!SheveryPrivilege.pingBinder()) {
                binding.status.setText(R.string.state_no_server);
                binding.btnAction.setText(R.string.btn_retry);
                binding.btnAction.setVisibility(View.VISIBLE);
                return;
            }
            boolean granted = false;
            try {
                granted = SheveryPrivilege.isGranted();
            } catch (Throwable ignored) {
            }
            if (granted) {
                int uid = -1;
                try {
                    uid = SheveryPrivilege.getUid();
                } catch (Throwable ignored) {
                }
                binding.status.setText(getString(R.string.state_ready, uid));
                binding.btnAction.setVisibility(View.GONE);
            } else {
                binding.status.setText(R.string.state_no_permission);
                binding.btnAction.setText(R.string.btn_request);
                binding.btnAction.setVisibility(View.VISIBLE);
            }
        });
    }

    private void onActionButton() {
        if (!SheveryPrivilege.pingBinder()) {
            try {
                SheveryPrivilege.connectFast(this);
            } catch (Throwable ignored) {
            }
            updateState();
            return;
        }
        try {
            if (SheveryPrivilege.shouldShowRequestPermissionRationale()) {
                // The user denied with "don't ask again": the grant has to be done inside Shevery.
                toast(R.string.denied_hint);
                return;
            }
            SheveryPrivilege.requestPermission(REQ_PERMISSION);
        } catch (Throwable t) {
            toast(getString(R.string.failed, String.valueOf(t.getMessage())));
        }
    }

    // ---------------------------------------------------------------- list

    private void loadApps() {
        binding.progress.setVisibility(View.VISIBLE);
        io.execute(() -> {
            PackageManager pm = getPackageManager();
            Map<String, UadInfo> uad = UadRepository.load(this);
            Set<String> dynamic = Safety.dynamicProtected(this);

            @SuppressWarnings("deprecation")
            List<ApplicationInfo> infos =
                    pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES);

            List<AppEntry> in = new ArrayList<>();
            List<AppEntry> out = new ArrayList<>();
            for (ApplicationInfo ai : infos) {
                CharSequence label = ai.loadLabel(pm);
                boolean system = (ai.flags & (ApplicationInfo.FLAG_SYSTEM
                        | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
                boolean isInstalled = (ai.flags & ApplicationInfo.FLAG_INSTALLED) != 0;
                UadInfo info = uad.get(ai.packageName);
                AppEntry entry = new AppEntry(
                        label != null ? label.toString() : ai.packageName,
                        ai.packageName,
                        system,
                        ai.enabled,
                        isInstalled,
                        Safety.isProtected(ai.packageName, dynamic),
                        info != null ? info : UadInfo.UNLISTED);
                (isInstalled ? in : out).add(entry);
            }
            Collections.sort(in, (a, b) -> a.label.compareToIgnoreCase(b.label));
            Collections.sort(out, (a, b) -> a.label.compareToIgnoreCase(b.label));

            main.post(() -> {
                if (isDestroyed()) return;
                installed.clear();
                installed.addAll(in);
                removed.clear();
                removed.addAll(out);
                uadSize = uad.size();
                binding.progress.setVisibility(View.GONE);
                applyFilter();
            });
        });
    }

    private void applyFilter() {
        String q = binding.search.getText().toString().trim().toLowerCase(Locale.ROOT);
        boolean showUser = binding.cbUser.isChecked();
        List<AppEntry> base = showRemoved ? removed : installed;
        List<AppEntry> filtered = new ArrayList<>();
        for (AppEntry e : base) {
            if (!showRemoved && !e.system && !showUser) continue;
            if (levelFilter != null && e.info.level != levelFilter) continue;
            if (!q.isEmpty()
                    && !e.label.toLowerCase(Locale.ROOT).contains(q)
                    && !e.packageName.toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            filtered.add(e);
        }
        sortList(filtered);
        shown = filtered;
        adapter.submit(filtered);
        binding.count.setText(getString(R.string.count, filtered.size()));

        boolean loading = binding.progress.getVisibility() == View.VISIBLE;
        boolean noFilters = q.isEmpty() && levelFilter == null;
        binding.empty.setText(showRemoved && noFilters ? R.string.empty_removed : R.string.empty_apps);
        binding.empty.setVisibility(filtered.isEmpty() && !loading ? View.VISIBLE : View.GONE);
        updateFab();
    }

    private void sortList(List<AppEntry> list) {
        switch (sort) {
            case PACKAGE:
                Collections.sort(list, (a, b) -> a.packageName.compareToIgnoreCase(b.packageName));
                break;
            case RISK:
                Collections.sort(list, (a, b) -> {
                    int c = a.info.level.compareTo(b.info.level);
                    return c != 0 ? c : a.label.compareToIgnoreCase(b.label);
                });
                break;
            default:
                Collections.sort(list, (a, b) -> a.label.compareToIgnoreCase(b.label));
                break;
        }
    }

    private UadInfo.Level levelForChip(int chipId) {
        if (chipId == R.id.chip_recommended) return UadInfo.Level.RECOMMENDED;
        if (chipId == R.id.chip_advanced) return UadInfo.Level.ADVANCED;
        if (chipId == R.id.chip_expert) return UadInfo.Level.EXPERT;
        if (chipId == R.id.chip_unsafe) return UadInfo.Level.UNSAFE;
        if (chipId == R.id.chip_unlisted) return UadInfo.Level.UNLISTED;
        return null;
    }

    // ---------------------------------------------------------------- selection

    private void toggle(AppEntry e, int position) {
        if (e.protectedApp) {
            toast(R.string.protected_app);
            return;
        }
        if (!selected.remove(e.packageName)) selected.add(e.packageName);
        binding.list.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        adapter.notifyItemChanged(position);
        updateFab();
    }

    private void updateFab() {
        int n = selected.size();
        binding.toolbar.setSubtitle(n == 0 ? null : getString(R.string.selected_count, n));
        if (n == 0) {
            binding.fab.hide();
            return;
        }
        binding.fab.setText(getString(showRemoved ? R.string.fab_restore : R.string.fab_remove, n));
        binding.fab.setIconResource(showRemoved ? R.drawable.ic_restore : R.drawable.ic_delete);
        binding.fab.show();
    }

    private boolean onMenuItem(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_select_all) {
            for (AppEntry e : shown) {
                if (!e.protectedApp) selected.add(e.packageName);
            }
            adapter.notifyDataSetChanged();
            updateFab();
            return true;
        }
        if (id == R.id.menu_select_recommended) {
            int added = 0;
            for (AppEntry e : shown) {
                if (!e.protectedApp && e.info.level == UadInfo.Level.RECOMMENDED
                        && selected.add(e.packageName)) {
                    added++;
                }
            }
            if (added == 0) toast(R.string.none_recommended);
            adapter.notifyDataSetChanged();
            updateFab();
            return true;
        }
        if (id == R.id.sort_name || id == R.id.sort_package || id == R.id.sort_risk) {
            item.setChecked(true);
            sort = id == R.id.sort_package ? Sort.PACKAGE
                    : id == R.id.sort_risk ? Sort.RISK : Sort.NAME;
            applyFilter();
            return true;
        }
        if (id == R.id.menu_clear) {
            selected.clear();
            adapter.notifyDataSetChanged();
            updateFab();
            return true;
        }
        if (id == R.id.menu_refresh) {
            loadApps();
            return true;
        }
        if (id == R.id.menu_about) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.about_title)
                    .setMessage(getString(R.string.about_message, uadSize))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return true;
        }
        return false;
    }

    /** Selected packages from the current tab's full list (selection survives filtering). */
    private List<AppEntry> resolveSelected() {
        List<AppEntry> out = new ArrayList<>();
        for (AppEntry e : showRemoved ? removed : installed) {
            if (selected.contains(e.packageName) && !e.protectedApp) out.add(e);
        }
        return out;
    }

    // ---------------------------------------------------------------- details sheet

    private void showDetails(AppEntry e) {
        SheetDetailsBinding s = SheetDetailsBinding.inflate(getLayoutInflater());
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        sheet.setContentView(s.getRoot());

        icons.load(e.packageName, s.sheetIcon);
        s.sheetName.setText(e.label);
        s.sheetPkg.setText(e.packageName);
        LevelUi.style(s.sheetLevel, e.info.level);

        if (e.info.list.isEmpty()) {
            s.sheetSource.setVisibility(View.GONE);
        } else {
            s.sheetSource.setText(getString(R.string.source_label, e.info.list));
        }

        if (e.info.description.trim().isEmpty()) {
            s.sheetDesc.setText(R.string.no_description);
        } else {
            s.sheetDesc.setText(e.info.description.trim());
        }

        if (!e.info.neededBy.isEmpty()) {
            s.sheetNeeded.setText(TextUtils.join("\n", e.info.neededBy));
            s.sheetNeededLabel.setVisibility(View.VISIBLE);
            s.sheetNeeded.setVisibility(View.VISIBLE);
        }
        if (!e.info.dependencies.isEmpty()) {
            s.sheetDepends.setText(TextUtils.join("\n", e.info.dependencies));
            s.sheetDependsLabel.setVisibility(View.VISIBLE);
            s.sheetDepends.setVisibility(View.VISIBLE);
        }

        if (e.protectedApp) {
            s.sheetPrimary.setText(R.string.tag_protected);
            s.sheetPrimary.setEnabled(false);
        } else {
            s.sheetPrimary.setText(e.installed ? R.string.action_remove : R.string.action_restore);
            s.sheetPrimary.setOnClickListener(v -> {
                sheet.dismiss();
                confirmAndRun(Collections.singletonList(e), !e.installed);
            });
        }
        s.sheetInfo.setOnClickListener(v -> openAppInfo(e));
        sheet.show();
    }

    private void openAppInfo(AppEntry e) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", e.packageName, null));
        try {
            startActivity(intent);
        } catch (Throwable t) {
            toast(getString(R.string.failed, String.valueOf(t.getMessage())));
        }
    }

    // ---------------------------------------------------------------- remove / restore

    private void confirmAndRun(List<AppEntry> targets, boolean restore) {
        if (targets.isEmpty()) return;
        if (!isReady()) {
            toast(R.string.not_ready);
            return;
        }
        int n = targets.size();
        int risky = 0;
        for (AppEntry e : targets) {
            if (e.info.level == UadInfo.Level.EXPERT || e.info.level == UadInfo.Level.UNSAFE) risky++;
        }
        final boolean needAck = !restore && risky > 0;

        DialogConfirmBinding d = DialogConfirmBinding.inflate(getLayoutInflater());
        d.msg.setText(restore ? R.string.confirm_restore_msg : R.string.confirm_remove_msg);
        d.summary.setText(levelSummary(targets));
        d.apps.setText(preview(targets));
        d.keepData.setChecked(true);
        d.keepData.setVisibility(restore ? View.GONE : View.VISIBLE);
        d.ack.setVisibility(needAck ? View.VISIBLE : View.GONE);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(restore ? R.string.confirm_restore_title
                        : R.string.confirm_remove_title, n))
                .setView(d.getRoot())
                .setPositiveButton(restore ? R.string.action_restore : R.string.action_remove,
                        (x, w) -> runBatch(targets, restore, d.keepData.isChecked()))
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();

        if (needAck) {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setEnabled(false);
            d.ack.setOnCheckedChangeListener((b, checked) -> positive.setEnabled(checked));
        }
    }

    private String levelSummary(List<AppEntry> targets) {
        int[] counts = new int[UadInfo.Level.values().length];
        for (AppEntry e : targets) counts[e.info.level.ordinal()]++;
        List<String> parts = new ArrayList<>();
        for (UadInfo.Level l : UadInfo.Level.values()) {
            if (counts[l.ordinal()] > 0) {
                parts.add(counts[l.ordinal()] + " " + getString(LevelUi.nameRes(l)));
            }
        }
        return TextUtils.join(" \u00b7 ", parts);
    }

    private String preview(List<AppEntry> targets) {
        StringBuilder sb = new StringBuilder();
        int shownCount = Math.min(targets.size(), PREVIEW_LIMIT);
        for (int i = 0; i < shownCount; i++) {
            sb.append("• ").append(targets.get(i).label).append('\n');
        }
        if (targets.size() > shownCount) {
            sb.append(getString(R.string.and_more, targets.size() - shownCount));
        }
        return sb.toString().trim();
    }

    private void runBatch(List<AppEntry> targets, boolean restore, boolean keepData) {
        final int total = targets.size();
        AlertDialog progress = new MaterialAlertDialogBuilder(this)
                .setMessage(getString(R.string.working, 0, total))
                .setCancelable(false)
                .create();
        progress.show();

        io.execute(() -> {
            List<AppEntry> succeeded = new ArrayList<>();
            List<String> failures = new ArrayList<>();
            for (int i = 0; i < total; i++) {
                AppEntry e = targets.get(i);
                final int step = i + 1;
                main.post(() -> progress.setMessage(getString(R.string.working, step, total)));

                if (!PACKAGE_NAME.matcher(e.packageName).matches()) {
                    failures.add(e.label + ": invalid package name");
                    continue;
                }
                String[] cmd;
                if (restore) {
                    cmd = new String[]{"pm", "install-existing", "--user", "0", e.packageName};
                } else if (keepData) {
                    cmd = new String[]{"pm", "uninstall", "-k", "--user", "0", e.packageName};
                } else {
                    cmd = new String[]{"pm", "uninstall", "--user", "0", e.packageName};
                }
                PrivilegedShell.Result r = PrivilegedShell.run(cmd);
                if (r.ok()) {
                    succeeded.add(e);
                } else {
                    failures.add(e.label + ": " + r.summary());
                }
                if (Thread.currentThread().isInterrupted()) break;
            }
            main.post(() -> {
                if (isDestroyed()) return;
                progress.dismiss();
                selected.clear();
                showResult(succeeded, failures, restore);
                loadApps();
            });
        });
    }

    private void showResult(List<AppEntry> succeeded, List<String> failures, boolean restore) {
        final int ok = succeeded.size();
        if (failures.isEmpty()) {
            if (ok == 0) return;
            Snackbar bar = Snackbar.make(binding.getRoot(),
                    getString(restore ? R.string.snack_restored : R.string.snack_removed, ok), 10_000);
            if (!restore) {
                bar.setAction(R.string.action_undo, v -> runBatch(succeeded, true, false));
            }
            bar.show();
            return;
        }
        StringBuilder msg = new StringBuilder(getString(R.string.result_summary, ok, failures.size()));
        List<String> head = failures.subList(0, Math.min(failures.size(), 8));
        msg.append("\n\n").append(getString(R.string.result_failures, TextUtils.join("\n", head)));
        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.result_title)
                .setMessage(msg.toString())
                .setPositiveButton(android.R.string.ok, null);
        if (!restore && ok > 0) {
            b.setNeutralButton(R.string.action_undo, (x, w) -> runBatch(succeeded, true, false));
        }
        b.show();
    }

    // ---------------------------------------------------------------- helpers

    private void toast(int res) {
        Toast.makeText(this, res, Toast.LENGTH_LONG).show();
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }
}
