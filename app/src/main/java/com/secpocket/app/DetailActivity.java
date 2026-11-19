package com.secpocket.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A single field brief: summary, crown jewels, core pillars and the client
 * playbook, plus the official source.
 */
public final class DetailActivity extends Activity {
    public static final String EXTRA_ID = "com.secpocket.app.FRAMEWORK_ID";

    private Framework framework;
    private ImageButton btnBookmark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.styleBars(this);
        String id = getIntent().getStringExtra(EXTRA_ID);
        Catalog catalog = Catalog.get(this);
        framework = id == null ? null : catalog.find(id);
        if (framework == null) {
            Toast.makeText(this, "This brief is no longer available.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_detail);
        bind(framework, catalog);
        animateIn();
    }

    private void bind(final Framework item, Catalog catalog) {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBookmark = findViewById(R.id.btnBookmark);
        ImageButton btnShare = findViewById(R.id.btnShare);
        FrameLayout iconTile = findViewById(R.id.iconTile);
        ImageView topicIcon = findViewById(R.id.topicIcon);
        TextView topicLabel = findViewById(R.id.topicLabel);
        TextView essentialLabel = findViewById(R.id.essentialLabel);
        TextView title = findViewById(R.id.title);
        TextView metaLine = findViewById(R.id.metaLine);
        LinearLayout metaChips = findViewById(R.id.metaChips);
        TextView summary = findViewById(R.id.summary);
        LinearLayout crownCard = findViewById(R.id.crownCard);
        LinearLayout crownList = findViewById(R.id.crownList);
        LinearLayout pillarsList = findViewById(R.id.pillarsList);
        LinearLayout clientCard = findViewById(R.id.clientCard);
        LinearLayout clientList = findViewById(R.id.clientList);
        TextView tagsLabel = findViewById(R.id.tagsLabel);
        View tagsSection = findViewById(R.id.tagsSection);
        LinearLayout topicTags = findViewById(R.id.topicTags);
        LinearLayout sourceButton = findViewById(R.id.sourceButton);
        TextView disclaimer = findViewById(R.id.disclaimer);
        TextView footer = findViewById(R.id.footer);

        final Topic topic = Topics.byName(item.primaryTopic);
        iconTile.setBackground(Ui.rounded(this, Ui.withAlpha(topic.color, 0x26), 16f));
        topicIcon.setImageResource(topic.iconRes);
        topicIcon.setColorFilter(topic.color);
        topicLabel.setText(topic.name.toUpperCase(Locale.ROOT));
        topicLabel.setTextColor(topic.color);

        essentialLabel.setVisibility(item.essential ? View.VISIBLE : View.GONE);
        essentialLabel.setBackground(Ui.rounded(this, 0x332B2415, 7f));

        title.setText(item.title);
        StringBuilder meta = new StringBuilder(item.issuer);
        if (!item.edition.isEmpty()) {
            meta.append("  \u00B7  ").append(item.edition);
        }
        metaLine.setText(meta.toString());

        metaChips.removeAllViews();
        addMetaChip(metaChips, item.type);
        addMetaChip(metaChips, item.level);
        addMetaChip(metaChips, item.region);
        addMetaChip(metaChips, item.readingMinutes() + " min read");

        summary.setText(item.summary);
        summary.setTextIsSelectable(true);

        crownCard.setBackground(Ui.roundedStroke(this, 0xFF1D1A12, 0x55F5C451, 16f, 1f));
        crownList.removeAllViews();
        for (int i = 0; i < item.crownJewels.size(); i++) {
            View row = Ui.bulletRow(this, item.crownJewels.get(i), Ui.GOLD);
            Ui.addWithTop(crownList, row, i == 0 ? 0 : 10);
        }

        pillarsList.removeAllViews();
        for (int i = 0; i < item.keyPoints.size(); i++) {
            View row = Ui.numberedRow(this,
                    String.format(Locale.ROOT, "%02d", i + 1), item.keyPoints.get(i), topic.color);
            Ui.addWithTop(pillarsList, row, i == 0 ? 0 : 12);
        }

        clientCard.setBackground(Ui.roundedStroke(this, Ui.SURFACE, Ui.STROKE, 16f, 1f));
        clientList.removeAllViews();
        for (int i = 0; i < item.clientUse.size(); i++) {
            View row = Ui.checkRow(this, item.clientUse.get(i), Ui.ACCENT);
            Ui.addWithTop(clientList, row, i == 0 ? 0 : 11);
        }

        bindTags(item, tagsLabel, tagsSection, topicTags);

        sourceButton.setBackground(Ui.outlineCard(this, Ui.SURFACE, Ui.STROKE));
        if (item.link.startsWith("http://") || item.link.startsWith("https://")) {
            sourceButton.setVisibility(View.VISIBLE);
            sourceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openLink(item.link);
                }
            });
        } else {
            sourceButton.setVisibility(View.GONE);
        }

        disclaimer.setText("Field note, not the standard. This is an original condensed interpretation "
                + "for professional orientation. It is not legal advice, an audit opinion, or a substitute "
                + "for the official text. Confirm the current edition, scope and applicability before "
                + "relying on it in an engagement.");

        String updated = catalog.updated();
        if (updated.isEmpty()) {
            footer.setText("SecPocket 1.0  \u00B7  Offline field library");
        } else {
            footer.setText("SecPocket 1.0  \u00B7  Catalog updated " + updated);
        }

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        updateBookmark();
        btnBookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean saved = BookmarkStore.toggle(DetailActivity.this, item.id);
                updateBookmark();
                Toast.makeText(DetailActivity.this,
                        saved ? "Saved to your briefs" : "Removed from saved",
                        Toast.LENGTH_SHORT).show();
            }
        });
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                share(item);
            }
        });
    }

    private void addMetaChip(LinearLayout parent, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        parent.addView(Ui.metaChip(this, text));
    }

    private void bindTags(Framework item, TextView label, View section, LinearLayout tags) {
        List<String> values = new ArrayList<>();
        for (String name : item.topics) {
            if (!name.equals(item.primaryTopic)) {
                values.add(name);
            }
        }
        boolean tagMode = values.isEmpty();
        if (tagMode) {
            values.addAll(item.tags);
        }
        tags.removeAllViews();
        if (values.isEmpty()) {
            label.setVisibility(View.GONE);
            section.setVisibility(View.GONE);
            return;
        }
        label.setVisibility(View.VISIBLE);
        section.setVisibility(View.VISIBLE);
        label.setText(tagMode ? "TAGS" : "ALSO APPLIES TO");
        for (String value : values) {
            int color = tagMode ? Ui.ACCENT : Topics.colorFor(value);
            tags.addView(Ui.topicPill(this, value, color));
        }
    }

    private void updateBookmark() {
        boolean saved = BookmarkStore.isSaved(this, framework.id);
        btnBookmark.setImageResource(saved ? R.drawable.ic_bookmark : R.drawable.ic_bookmark_border);
        btnBookmark.setColorFilter(saved ? Ui.GOLD : Ui.TEXT_SECONDARY);
        btnBookmark.setContentDescription(getString(
                saved ? R.string.bookmark_remove : R.string.bookmark_add));
    }

    private void share(Framework item) {
        StringBuilder text = new StringBuilder(512);
        text.append(item.title).append('\n').append(item.issuer);
        if (!item.edition.isEmpty()) {
            text.append("  \u00B7  ").append(item.edition);
        }
        text.append("\n\n").append(item.summary);
        if (!item.crownJewels.isEmpty()) {
            text.append("\n\nCrown jewels:");
            int limit = Math.min(4, item.crownJewels.size());
            for (int i = 0; i < limit; i++) {
                text.append("\n\u2022 ").append(item.crownJewels.get(i));
            }
        }
        if (!item.link.isEmpty()) {
            text.append("\n\n").append(item.link);
        }
        text.append("\n\nShared from SecPocket");

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, item.title + " - SecPocket");
        send.putExtra(Intent.EXTRA_TEXT, text.toString());
        startActivity(Intent.createChooser(send, "Share brief"));
    }

    private void openLink(String link) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "No browser is available to open the source.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void animateIn() {
        View content = findViewById(R.id.content);
        content.setAlpha(0f);
        content.setTranslationY(Ui.dp(this, 12));
        content.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (framework != null && btnBookmark != null) {
            updateBookmark();
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
