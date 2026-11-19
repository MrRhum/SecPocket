package com.secpocket.app;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Small drawing and typography helpers. Everything here uses platform APIs
 * only, so the app stays free of AndroidX.
 */
final class Ui {
    static final int BG = 0xFF0A101C;
    static final int SURFACE = 0xFF111A2B;
    static final int SURFACE_ALT = 0xFF162136;
    static final int STROKE = 0xFF22314A;
    static final int TEXT_PRIMARY = 0xFFEAF1FC;
    static final int TEXT_SECONDARY = 0xFFB8C7DD;
    static final int TEXT_MUTED = 0xFF8194AF;
    static final int ACCENT = 0xFF4FD1C5;
    static final int GOLD = 0xFFF5C451;

    private static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private static final Typeface BLACK = Typeface.create("sans-serif-black", Typeface.NORMAL);

    private Ui() {
    }

    static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics()));
    }

    static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    static GradientDrawable rounded(Context context, int color, float radiusDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(context, radiusDp));
        return shape;
    }

    static GradientDrawable roundedStroke(Context context, int fill, int strokeColor,
                                          float radiusDp, float strokeDp) {
        GradientDrawable shape = rounded(context, fill, radiusDp);
        shape.setStroke(dp(context, strokeDp), strokeColor);
        return shape;
    }

    static Drawable ripple(Drawable content) {
        return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), content, null);
    }

    static Drawable card(Context context) {
        return ripple(roundedStroke(context, SURFACE, STROKE, 16f, 1f));
    }

    static Drawable outlineCard(Context context, int fill, int strokeColor) {
        return ripple(roundedStroke(context, fill, strokeColor, 16f, 1f));
    }

    static Drawable pillBackground(Context context, int color) {
        return rounded(context, withAlpha(color, 0x24), 7f);
    }

    static TextView chip(Context context, String label, boolean selected, int accentColor) {
        TextView chip = new TextView(context);
        chip.setText(label);
        chip.setTextSize(12f);
        chip.setTypeface(MEDIUM);
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setAllCaps(false);
        chip.setPadding(dp(context, 15), 0, dp(context, 15), 0);
        chip.setMinHeight(dp(context, 36));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(context, 36));
        params.setMarginEnd(dp(context, 8));
        chip.setLayoutParams(params);
        styleChip(context, chip, selected, accentColor);
        return chip;
    }

    static void styleChip(Context context, TextView chip, boolean selected, int accentColor) {
        chip.setTextColor(selected ? 0xFF071018 : TEXT_SECONDARY);
        if (selected) {
            chip.setBackground(ripple(rounded(context, accentColor, 18f)));
        } else {
            chip.setBackground(ripple(roundedStroke(context, SURFACE, STROKE, 18f, 1f)));
        }
    }

    static TextView metaChip(Context context, String label) {
        TextView chip = new TextView(context);
        chip.setText(label);
        chip.setTextSize(10.5f);
        chip.setTypeface(MEDIUM);
        chip.setTextColor(TEXT_SECONDARY);
        chip.setPadding(dp(context, 9), dp(context, 5), dp(context, 9), dp(context, 5));
        chip.setBackground(roundedStroke(context, SURFACE_ALT, STROKE, 7f, 1f));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(dp(context, 6));
        chip.setLayoutParams(params);
        return chip;
    }

    static TextView topicPill(Context context, String label, int color) {
        TextView pill = new TextView(context);
        pill.setText(label);
        pill.setTextSize(10.5f);
        pill.setTypeface(MEDIUM);
        pill.setTextColor(color);
        pill.setPadding(dp(context, 10), dp(context, 6), dp(context, 10), dp(context, 6));
        pill.setBackground(ripple(rounded(context, withAlpha(color, 0x20), 8f)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(dp(context, 7));
        pill.setLayoutParams(params);
        return pill;
    }

    static LinearLayout bulletRow(Context context, String text, int markerColor) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView marker = new TextView(context);
        marker.setText("\u2022");
        marker.setTextSize(15f);
        marker.setTextColor(markerColor);
        marker.setIncludeFontPadding(false);
        marker.setPadding(0, dp(context, 1), 0, 0);
        row.addView(marker, new LinearLayout.LayoutParams(dp(context, 17),
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView body = new TextView(context);
        body.setText(text);
        body.setTextSize(13.5f);
        body.setTextColor(TEXT_SECONDARY);
        body.setLineSpacing(0f, 1.32f);
        row.addView(body, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    static LinearLayout numberedRow(Context context, String number, String text, int accentColor) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView index = new TextView(context);
        index.setText(number);
        index.setTextSize(11f);
        index.setLetterSpacing(0.08f);
        index.setTypeface(MEDIUM);
        index.setTextColor(accentColor);
        index.setIncludeFontPadding(false);
        index.setPadding(0, dp(context, 3), 0, 0);
        row.addView(index, new LinearLayout.LayoutParams(dp(context, 30),
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView body = new TextView(context);
        body.setText(text);
        body.setTextSize(13.5f);
        body.setTextColor(TEXT_SECONDARY);
        body.setLineSpacing(0f, 1.32f);
        row.addView(body, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    static LinearLayout checkRow(Context context, String text, int accentColor) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);

        ImageView check = new ImageView(context);
        check.setImageResource(R.drawable.ic_check);
        check.setColorFilter(accentColor);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                dp(context, 15), dp(context, 15));
        iconParams.topMargin = dp(context, 3);
        row.addView(check, iconParams);

        TextView body = new TextView(context);
        body.setText(text);
        body.setTextSize(13.5f);
        body.setTextColor(TEXT_SECONDARY);
        body.setLineSpacing(0f, 1.32f);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMarginStart(dp(context, 10));
        row.addView(body, textParams);
        return row;
    }

    static void addWithTop(LinearLayout parent, View child, int topDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(parent.getContext(), topDp);
        parent.addView(child, params);
    }

    static void styleBars(Activity activity) {
        Window window = activity.getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(BG);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    static void tint(ImageView view, int color) {
        view.setColorFilter(color);
    }

    static Typeface medium() {
        return MEDIUM;
    }

    static Typeface black() {
        return BLACK;
    }
}
