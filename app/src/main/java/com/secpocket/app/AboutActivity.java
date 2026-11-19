package com.secpocket.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * Methodology, privacy posture, version and a local "forget my saved briefs"
 * control.
 */
public final class AboutActivity extends Activity {
    private TextView clearSaved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.styleBars(this);
        setContentView(R.layout.activity_about);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Catalog catalog = Catalog.get(this);
        TextView versionLine = findViewById(R.id.versionLine);
        TextView statGuides = findViewById(R.id.statGuides);
        TextView statDomains = findViewById(R.id.statDomains);
        clearSaved = findViewById(R.id.btnClearSaved);
        TextView footer = findViewById(R.id.footer);

        String version = "1.0";
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (info.versionName != null) {
                version = info.versionName;
            }
        } catch (Exception ignored) {
            // Fall back to the bundled version label.
        }

        String updated = catalog.updated();
        if (updated.isEmpty()) {
            versionLine.setText("v" + version);
        } else {
            versionLine.setText("v" + version + "  \u00B7  Catalog " + updated);
        }
        statGuides.setText(String.format(Locale.ROOT, "%d", catalog.all().size()));
        statDomains.setText(String.format(Locale.ROOT, "%d", Topics.all().size()));
        footer.setText("Built for the field.\nSecPocket " + version);
        updateClearLabel();

        clearSaved.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmClearSaved();
            }
        });
    }

    private void updateClearLabel() {
        int count = BookmarkStore.count(this);
        clearSaved.setText(count == 0
                ? "No saved briefs on this device"
                : "Clear saved briefs (" + count + ")");
        clearSaved.setAlpha(count == 0 ? 0.6f : 1f);
    }

    private void confirmClearSaved() {
        final int count = BookmarkStore.count(this);
        if (count == 0) {
            Toast.makeText(this, "There are no saved briefs to clear.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Clear saved briefs?")
                .setMessage("This removes " + count + (count == 1 ? " brief" : " briefs")
                        + " from this device. It cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        BookmarkStore.clear(AboutActivity.this);
                        updateClearLabel();
                        Toast.makeText(AboutActivity.this, "Saved briefs cleared.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (clearSaved != null) {
            updateClearLabel();
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
