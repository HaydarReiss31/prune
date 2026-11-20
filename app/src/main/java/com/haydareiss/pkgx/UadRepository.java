package com.haydareiss.pkgx;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Loads assets/uad_lists.json (a compact export of the UAD-ng lists:
 * l = list, d = description, r = removal level, dep = dependencies, nb = needed by).
 */
final class UadRepository {

    private static volatile Map<String, UadInfo> cache;

    private UadRepository() {
    }

    /** Blocking, call from a background thread. Never returns null. */
    static Map<String, UadInfo> load(Context context) {
        Map<String, UadInfo> cached = cache;
        if (cached != null) return cached;
        synchronized (UadRepository.class) {
            if (cache != null) return cache;
            Map<String, UadInfo> map = new HashMap<>(8192);
            try (InputStream in = context.getAssets().open("uad_lists.json")) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream(1 << 20);
                byte[] chunk = new byte[16384];
                int n;
                while ((n = in.read(chunk)) != -1) {
                    buffer.write(chunk, 0, n);
                }
                JSONObject root = new JSONObject(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
                Iterator<String> keys = root.keys();
                while (keys.hasNext()) {
                    String pkg = keys.next();
                    JSONObject o = root.optJSONObject(pkg);
                    if (o == null) continue;
                    map.put(pkg, new UadInfo(
                            UadInfo.Level.parse(o.optString("r")),
                            o.optString("l"),
                            o.optString("d"),
                            strings(o.optJSONArray("dep")),
                            strings(o.optJSONArray("nb"))));
                }
            } catch (Throwable ignored) {
                // A missing or broken list only means every app shows as "Unlisted".
            }
            cache = map;
            return map;
        }
    }

    private static List<String> strings(JSONArray array) {
        if (array == null || array.length() == 0) return Collections.emptyList();
        List<String> out = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            String s = array.optString(i);
            if (s != null && !s.isEmpty()) out.add(s);
        }
        return out;
    }
}
