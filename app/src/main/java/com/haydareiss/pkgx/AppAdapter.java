package com.haydareiss.pkgx;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class AppAdapter extends RecyclerView.Adapter<AppAdapter.Holder> {

    interface Listener {
        /** Row tapped: toggle its selection. */
        void onToggle(AppEntry entry, int position);

        /** Info button tapped: show the details sheet. */
        void onDetails(AppEntry entry);
    }

    private final List<AppEntry> items = new ArrayList<>();
    private final IconLoader icons;
    private final Set<String> selected;
    private final Listener listener;

    AppAdapter(IconLoader icons, Set<String> selected, Listener listener) {
        this.icons = icons;
        this.selected = selected;
        this.listener = listener;
    }

    void submit(List<AppEntry> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final AppEntry e = items.get(position);
        icons.load(e.packageName, h.icon);
        h.name.setText(e.label);
        h.pkg.setText(e.packageName);

        String description = e.info.description.trim();
        h.desc.setText(description);
        h.desc.setVisibility(description.isEmpty() ? View.GONE : View.VISIBLE);

        LevelUi.style(h.level, e.info.level);

        if (e.protectedApp) {
            h.extra.setText(R.string.tag_protected);
            h.extra.setVisibility(View.VISIBLE);
        } else if (e.installed && !e.enabled) {
            h.extra.setText(R.string.tag_frozen);
            h.extra.setVisibility(View.VISIBLE);
        } else {
            h.extra.setVisibility(View.GONE);
        }

        boolean isSelected = selected.contains(e.packageName);
        h.itemView.setActivated(isSelected);
        h.check.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        h.itemView.setAlpha(e.protectedApp ? 0.6f : 1f);

        h.itemView.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) listener.onToggle(e, pos);
        });
        h.details.setOnClickListener(v -> listener.onDetails(e));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;
        final TextView pkg;
        final TextView desc;
        final TextView level;
        final TextView extra;
        final ImageView check;
        final View details;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.name);
            pkg = itemView.findViewById(R.id.pkg);
            desc = itemView.findViewById(R.id.desc);
            level = itemView.findViewById(R.id.level);
            extra = itemView.findViewById(R.id.extra);
            check = itemView.findViewById(R.id.check);
            details = itemView.findViewById(R.id.details);
        }
    }
}
