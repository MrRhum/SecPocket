package com.secpocket.app;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the embedded catalog once per process.
 */
public final class Catalog {
    private static final String TAG = "SecPocketCatalog";
    private static final String ASSET = "frameworks.json";

    private static Catalog instance;

    private final List<Framework> frameworks;
    private final Map<String, Framework> byId;
    private final Map<String, Integer> topicCounts;
    private final String updated;
    private final boolean loadFailed;

    private Catalog(Context context) {
        List<Framework> loaded = new ArrayList<>();
        Map<String, Framework> lookup = new HashMap<>();
        String loadedUpdated = "";
        boolean failed = false;
        try (InputStream input = context.getAssets().open(ASSET);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line);
            }
            JSONObject root = new JSONObject(text.toString());
            loadedUpdated = root.optString("updated", "");
            JSONArray array = root.getJSONArray("frameworks");
            for (int i = 0; i < array.length(); i++) {
                Framework framework = Framework.fromJson(array.getJSONObject(i));
                loaded.add(framework);
                lookup.put(framework.id, framework);
            }
        } catch (Exception error) {
            failed = true;
            Log.e(TAG, "Unable to load embedded catalog", error);
        }

        // Deduplicate defensively while preserving catalog order.
        List<Framework> unique = new ArrayList<>(loaded.size());
        Map<String, Boolean> seen = new HashMap<>();
        for (Framework framework : loaded) {
            if (!seen.containsKey(framework.id)) {
                unique.add(framework);
                seen.put(framework.id, Boolean.TRUE);
            }
        }

        frameworks = Collections.unmodifiableList(unique);
        byId = Collections.unmodifiableMap(lookup);
        updated = loadedUpdated;
        loadFailed = failed;

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Topic topic : Topics.all()) {
            int count = 0;
            for (Framework framework : frameworks) {
                if (framework.topics.contains(topic.name)) {
                    count++;
                }
            }
            counts.put(topic.name, count);
        }
        topicCounts = Collections.unmodifiableMap(counts);
    }

    public static synchronized Catalog get(Context context) {
        if (instance == null) {
            instance = new Catalog(context.getApplicationContext());
        }
        return instance;
    }

    public List<Framework> all() {
        return frameworks;
    }

    public Framework find(String id) {
        return byId.get(id);
    }

    public int countForTopic(String name) {
        Integer count = topicCounts.get(name);
        return count == null ? 0 : count;
    }

    public int countEssential() {
        int count = 0;
        for (Framework framework : frameworks) {
            if (framework.essential) {
                count++;
            }
        }
        return count;
    }

    public String updated() {
        return updated;
    }

    public boolean loadFailed() {
        return loadFailed;
    }
}
