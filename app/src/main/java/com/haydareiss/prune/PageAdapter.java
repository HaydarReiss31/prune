package com.haydareiss.prune;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/** The two pages of the pager: page 0 lists installed apps, page 1 lists removed apps. */
final class PageAdapter extends RecyclerView.Adapter<PageAdapter.Holder> {

    private final AppAdapter[] lists;
    private final Holder[] holders = new Holder[2];
    private final int[] emptyText = {R.string.empty_apps, R.string.empty_removed};
    private final boolean[] emptyVisible = new boolean[2];

    PageAdapter(AppAdapter[] lists) {
        this.lists = lists;
    }

    @Override
    public int getItemCount() {
        return lists.length;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_list, parent, false);
        Holder h = new Holder(v);
        h.list.setLayoutManager(new LinearLayoutManager(parent.getContext()));
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        h.list.setAdapter(lists[position]);
        holders[position] = h;
        applyEmpty(position);
    }

    /** Shows or hides the empty-state text of a page (no-op while the page is not created yet). */
    void setEmpty(int page, int textRes, boolean visible) {
        emptyText[page] = textRes;
        emptyVisible[page] = visible;
        applyEmpty(page);
    }

    private void applyEmpty(int page) {
        Holder h = holders[page];
        if (h == null) return;
        h.empty.setText(emptyText[page]);
        h.empty.setVisibility(emptyVisible[page] ? View.VISIBLE : View.GONE);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final RecyclerView list;
        final TextView empty;

        Holder(@NonNull View itemView) {
            super(itemView);
            list = itemView.findViewById(R.id.page_list);
            empty = itemView.findViewById(R.id.page_empty);
        }
    }
}
