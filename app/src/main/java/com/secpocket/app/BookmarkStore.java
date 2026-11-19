package com.secpocket.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Local-only bookmark storage. No account, no network.
 */
public final class BookmarkStore {
    private static final String PREFS = "secpocket_prefs";
    private static final String KEY_SAVED = "saved_ids";

    private BookmarkStore() {
    }

    public static boolean isSaved(Context context, String id) {
        return read(context).contains(id);
    }

    public static boolean toggle(Context context, String id) {
        Set<String> saved = read(context);
        boolean nowSaved;
        if (saved.contains(id)) {
            saved.remove(id);
            nowSaved = false;
        } else {
            saved.add(id);
            nowSaved = true;
        }
        write(context, saved);
        return nowSaved;
    }

    public static Set<String> saved(Context context) {
        return read(context);
    }

    public static int count(Context context) {
        return read(context).size();
    }

    public static void clear(Context context) {
        prefs(context).edit().remove(KEY_SAVED).apply();
    }

    private static Set<String> read(Context context) {
        StringSetHolder holder = new StringSetHolder(prefs(context).getStringSet(KEY_SAVED,
                Collections.emptySet()));
        return holder.values;
    }

    private static void write(Context context, Set<String> values) {
        prefs(context).edit().putStringSet(KEY_SAVED, new HashSet<>(values)).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static final class StringSetHolder {
        final Set<String> values;

        StringSetHolder(Set<String> source) {
            values = new HashSet<>(source);
        }
    }
}
