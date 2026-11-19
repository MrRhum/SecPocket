package com.secpocket.app;

/**
 * Static metadata for one topic in the field library.
 */
public final class Topic {
    public final String name;
    public final String shortName;
    public final String description;
    public final int color;
    public final int iconRes;

    public Topic(String name, String shortName, int color, int iconRes, String description) {
        this.name = name;
        this.shortName = shortName;
        this.color = color;
        this.iconRes = iconRes;
        this.description = description;
    }
}
