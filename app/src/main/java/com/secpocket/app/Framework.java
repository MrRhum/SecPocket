package com.secpocket.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * One framework brief. Immutable once parsed.
 */
public final class Framework {
    public final String id;
    public final String title;
    public final String shortTitle;
    public final String issuer;
    public final String edition;
    public final String region;
    public final String primaryTopic;
    public final String type;
    public final String level;
    public final String hook;
    public final String summary;
    public final String link;
    public final List<String> topics;
    public final List<String> keyPoints;
    public final List<String> crownJewels;
    public final List<String> clientUse;
    public final List<String> tags;
    public final boolean essential;

    private final String searchText;

    private Framework(JSONObject json) throws JSONException {
        id = json.getString("id");
        title = json.getString("title");
        shortTitle = json.optString("shortTitle", title);
        issuer = json.getString("issuer");
        edition = json.optString("edition", "");
        region = json.optString("region", "Global");
        primaryTopic = json.getString("primaryTopic");
        type = json.optString("type", "Framework");
        level = json.optString("level", "Foundation");
        hook = json.optString("hook", "");
        summary = json.getString("summary");
        link = json.optString("link", "");
        topics = list(json.optJSONArray("topics"));
        keyPoints = list(json.optJSONArray("keyPoints"));
        crownJewels = list(json.optJSONArray("crownJewels"));
        clientUse = list(json.optJSONArray("clientUse"));
        tags = list(json.optJSONArray("tags"));
        essential = json.optBoolean("essential", false);
        searchText = buildSearchText();
    }

    public static Framework fromJson(JSONObject json) throws JSONException {
        return new Framework(json);
    }

    private static List<String> list(JSONArray array) {
        if (array == null) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i, "").trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return Collections.unmodifiableList(values);
    }

    private String buildSearchText() {
        StringBuilder builder = new StringBuilder(768);
        builder.append(title).append(' ')
                .append(shortTitle).append(' ')
                .append(issuer).append(' ')
                .append(edition).append(' ')
                .append(region).append(' ')
                .append(primaryTopic).append(' ')
                .append(type).append(' ')
                .append(level).append(' ')
                .append(hook).append(' ')
                .append(summary).append(' ');
        append(builder, topics);
        append(builder, keyPoints);
        append(builder, crownJewels);
        append(builder, clientUse);
        append(builder, tags);
        if (issuer.toUpperCase(Locale.ROOT).contains("ANSSI")) {
            builder.append(" ainsi");
        }
        return compact(builder.toString());
    }

    private static void append(StringBuilder builder, List<String> values) {
        for (String value : values) {
            builder.append(value).append(' ');
        }
    }

    private static String compact(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]", "");
    }

    /**
     * AND-style search across the compacted search blob. This makes "wifi"
     * match "Wi-Fi" and strips accents from French titles.
     */
    public boolean matches(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String[] tokens = query.trim().toLowerCase(Locale.ROOT).split("\\s+");
        for (String token : tokens) {
            String needle = compact(token);
            if (!needle.isEmpty() && !searchText.contains(needle)) {
                return false;
            }
        }
        return true;
    }

    public int readingMinutes() {
        int words = 0;
        words += countWords(summary);
        for (String value : keyPoints) {
            words += countWords(value);
        }
        for (String value : crownJewels) {
            words += countWords(value);
        }
        for (String value : clientUse) {
            words += countWords(value);
        }
        return Math.max(2, (int) Math.ceil(words / 180.0));
    }

    private static int countWords(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        return value.trim().split("\\s+").length;
    }
}
