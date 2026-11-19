package com.secpocket.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Search-first library screen. Filtering, grouping and saved-state handling
 * all run locally against the embedded catalog.
 */
public final class MainActivity extends Activity {
    private static final String STATE_QUERY = "query";
    private static final String STATE_TOPIC = "topic";
    private static final String STATE_ESSENTIALS = "essentials";
    private static final String STATE_SAVED = "saved";

    private static final String MODE_ALL = "all";
    private static final String MODE_ESSENTIALS = "essentials";
    private static final String MODE_SAVED = "saved";
    private static final String MODE_TOPIC = "topic";

    private Catalog catalog;
    private FrameworkAdapter adapter;
    private ListView list;

    private EditText searchInput;
    private ImageButton btnClear;
    private LinearLayout chipRow;
    private TextView savedChip;
    private TextView eyebrow;
    private TextView listLabel;
    private TextView resultCount;

    private String query = "";
    private String selectedTopic;
    private boolean essentialsOnly;
    private boolean savedOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.styleBars(this);
        setContentView(R.layout.activity_main);

        catalog = Catalog.get(this);
        list = findViewById(R.id.list);
        adapter = new FrameworkAdapter(this, new FrameworkAdapter.Listener() {
            @Override
            public void onOpen(Framework framework) {
                openDetail(framework);
            }

            @Override
            public void onToggleSave(Framework framework) {
                toggleSaved(framework);
            }

            @Override
            public void onEmptyAction() {
                resetAll();
            }
        });

        View header = getLayoutInflater().inflate(R.layout.view_list_header, list, false);
        bindHeader(header);
        list.addHeaderView(header, null, false);
        list.setAdapter(adapter);

        if (savedInstanceState != null) {
            query = nonNull(savedInstanceState.getString(STATE_QUERY));
            selectedTopic = savedInstanceState.getString(STATE_TOPIC);
            essentialsOnly = savedInstanceState.getBoolean(STATE_ESSENTIALS, false);
            savedOnly = savedInstanceState.getBoolean(STATE_SAVED, false);
        }

        searchInput.setText(query);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                query = text.toString();
                btnClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilters(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        btnClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchInput.setText("");
                searchInput.requestFocus();
            }
        });

        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN;
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || enter) {
                    searchInput.clearFocus();
                    hideKeyboard();
                    return true;
                }
                return false;
            }
        });

        findViewById(R.id.btnSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSearch();
            }
        });
        findViewById(R.id.btnSaved).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectSaved();
            }
        });
        findViewById(R.id.btnInfo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        buildChips();
        applyFilters(true);
    }

    private void bindHeader(View header) {
        eyebrow = header.findViewById(R.id.eyebrow);
        listLabel = header.findViewById(R.id.listLabel);
        resultCount = header.findViewById(R.id.resultCount);
        chipRow = header.findViewById(R.id.chipRow);
        searchInput = header.findViewById(R.id.searchInput);
        btnClear = header.findViewById(R.id.btnClear);
        listLabel.setSingleLine(true);
        listLabel.setEllipsize(TextUtils.TruncateAt.END);
    }

    private void buildChips() {
        chipRow.removeAllViews();
        addChip(MODE_ALL, "All", mode().equals(MODE_ALL), Ui.ACCENT, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectAll();
            }
        });
        addChip(MODE_ESSENTIALS, "Essentials", mode().equals(MODE_ESSENTIALS), Ui.ACCENT, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectEssentials();
            }
        });
        savedChip = addChip(MODE_SAVED, savedLabel(), mode().equals(MODE_SAVED), Ui.GOLD, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectSaved();
            }
        });
        for (final Topic topic : Topics.all()) {
            boolean selected = mode().equals(MODE_TOPIC) && topic.name.equals(selectedTopic);
            addChip(MODE_TOPIC + ":" + topic.name, topic.shortName, selected, topic.color,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            selectTopic(topic.name);
                        }
                    });
        }
    }

    private TextView addChip(String key, String label, boolean selected, int accent,
                             View.OnClickListener listener) {
        TextView chip = Ui.chip(this, label, selected, accent);
        chip.setTag(key);
        chip.setOnClickListener(listener);
        chipRow.addView(chip);
        return chip;
    }

    private void refreshChipStyles() {
        for (int i = 0; i < chipRow.getChildCount(); i++) {
            TextView chip = (TextView) chipRow.getChildAt(i);
            Object tag = chip.getTag();
            String key = tag == null ? MODE_ALL : tag.toString();
            boolean selected;
            int accent = Ui.ACCENT;
            if (MODE_SAVED.equals(key)) {
                selected = savedOnly;
                accent = Ui.GOLD;
            } else if (MODE_ESSENTIALS.equals(key)) {
                selected = essentialsOnly;
            } else if (key.startsWith(MODE_TOPIC + ":")) {
                String name = key.substring(MODE_TOPIC.length() + 1);
                selected = name.equals(selectedTopic);
                accent = Topics.byName(name).color;
            } else {
                selected = mode().equals(MODE_ALL);
            }
            Ui.styleChip(this, chip, selected, accent);
        }
    }

    private String savedLabel() {
        int count = BookmarkStore.count(this);
        return count == 0 ? "Saved" : "Saved " + count;
    }

    private String mode() {
        if (selectedTopic != null) {
            return MODE_TOPIC;
        }
        if (essentialsOnly) {
            return MODE_ESSENTIALS;
        }
        if (savedOnly) {
            return MODE_SAVED;
        }
        return MODE_ALL;
    }

    private void selectAll() {
        selectedTopic = null;
        essentialsOnly = false;
        savedOnly = false;
        refreshChipStyles();
        applyFilters(true);
    }

    private void selectEssentials() {
        selectedTopic = null;
        essentialsOnly = true;
        savedOnly = false;
        refreshChipStyles();
        applyFilters(true);
    }

    private void selectSaved() {
        selectedTopic = null;
        essentialsOnly = false;
        savedOnly = true;
        refreshChipStyles();
        applyFilters(true);
    }

    private void selectTopic(String name) {
        selectedTopic = name;
        essentialsOnly = false;
        savedOnly = false;
        refreshChipStyles();
        applyFilters(true);
    }

    private void applyFilters(boolean scrollTop) {
        Set<String> saved = BookmarkStore.saved(this);
        List<Framework> items = new ArrayList<>();
        String trimmedQuery = query.trim();
        boolean hasQuery = !trimmedQuery.isEmpty();

        for (Framework framework : catalog.all()) {
            if (essentialsOnly && !framework.essential) {
                continue;
            }
            if (savedOnly && !saved.contains(framework.id)) {
                continue;
            }
            if (selectedTopic != null && !framework.primaryTopic.equals(selectedTopic)
                    && !framework.topics.contains(selectedTopic)) {
                continue;
            }
            if (hasQuery && !framework.matches(trimmedQuery)) {
                continue;
            }
            items.add(framework);
        }

        boolean grouped = !hasQuery && selectedTopic == null;
        EmptyCopy empty = emptyCopy(hasQuery, trimmedQuery, items.isEmpty());
        adapter.setItems(items, grouped, empty.title, empty.body, empty.action, empty.icon);

        updateHeaderLabels(items.size(), hasQuery);
        if (scrollTop) {
            list.setSelection(0);
        }
    }

    private void updateHeaderLabels(int count, boolean hasQuery) {
        if (catalog.loadFailed() || catalog.all().isEmpty()) {
            eyebrow.setText("CATALOG UNAVAILABLE");
        } else {
            eyebrow.setText("FIELD LIBRARY  \u00B7  " + catalog.all().size() + " GUIDES");
        }

        if (hasQuery) {
            listLabel.setText("SEARCH RESULTS");
            resultCount.setText(count + (count == 1 ? " match" : " matches"));
            return;
        }
        if (selectedTopic != null) {
            listLabel.setText(selectedTopic.toUpperCase(Locale.ROOT));
        } else if (essentialsOnly) {
            listLabel.setText("ESSENTIAL BRIEFS");
        } else if (savedOnly) {
            listLabel.setText("SAVED BRIEFS");
        } else {
            listLabel.setText("BROWSE ALL");
        }
        resultCount.setText(count + (count == 1 ? " guide" : " guides"));
    }

    private EmptyCopy emptyCopy(boolean hasQuery, String trimmedQuery, boolean isListEmpty) {
        if (catalog.loadFailed() || catalog.all().isEmpty()) {
            return new EmptyCopy("Catalog unavailable",
                    "The embedded library could not be read. Reinstall SecPocket to restore the 77 field briefs.",
                    "", R.drawable.ic_info);
        }
        if (hasQuery) {
            return new EmptyCopy("No matching briefs",
                    "No brief matches \"" + trimmedQuery + "\". Try a framework, issuer, topic or acronym.",
                    "Clear search", R.drawable.ic_search);
        }
        if (savedOnly) {
            return new EmptyCopy("Nothing saved yet",
                    "Tap the bookmark on any brief to keep it here. Saved briefs stay on this device and never leave it.",
                    "Browse the library", R.drawable.ic_bookmark_border);
        }
        if (selectedTopic != null) {
            return new EmptyCopy("No briefs in this domain",
                    "Nothing is indexed under " + selectedTopic + " yet. Try another topic or browse the full library.",
                    "Browse all", Topics.byName(selectedTopic).iconRes);
        }
        if (essentialsOnly) {
            return new EmptyCopy("No essential briefs",
                    "The essentials filter is empty. Browse the full library instead.",
                    "Browse all", R.drawable.ic_diamond);
        }
        if (isListEmpty) {
            return new EmptyCopy("Library is empty",
                    "The embedded catalog loaded without any briefs. Reinstall SecPocket to restore it.",
                    "", R.drawable.ic_info);
        }
        return new EmptyCopy("", "", "", R.drawable.ic_search);
    }

    private void toggleSaved(Framework framework) {
        boolean nowSaved = BookmarkStore.toggle(this, framework.id);
        if (savedChip != null) {
            savedChip.setText(savedLabel());
        }
        applyFilters(false);
        Toast.makeText(this, nowSaved ? "Saved to your briefs" : "Removed from saved",
                Toast.LENGTH_SHORT).show();
    }

    private void openDetail(Framework framework) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_ID, framework.id);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void showSearch() {
        list.setSelection(0);
        list.postDelayed(new Runnable() {
            @Override
            public void run() {
                searchInput.requestFocus();
                InputMethodManager keyboard = (InputMethodManager)
                        getSystemService(INPUT_METHOD_SERVICE);
                if (keyboard != null) {
                    keyboard.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 140);
    }

    private void resetAll() {
        selectedTopic = null;
        essentialsOnly = false;
        savedOnly = false;
        query = "";
        refreshChipStyles();
        if (searchInput.getText().length() > 0) {
            searchInput.setText("");
        } else {
            btnClear.setVisibility(View.GONE);
            applyFilters(true);
        }
        hideKeyboard();
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus == null) {
            return;
        }
        InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (keyboard != null) {
            keyboard.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
        focus.clearFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (savedChip != null) {
            savedChip.setText(savedLabel());
        }
        if (adapter != null) {
            applyFilters(false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_QUERY, query);
        outState.putString(STATE_TOPIC, selectedTopic);
        outState.putBoolean(STATE_ESSENTIALS, essentialsOnly);
        outState.putBoolean(STATE_SAVED, savedOnly);
    }

    @Override
    public void onBackPressed() {
        if (!query.isEmpty() || selectedTopic != null || essentialsOnly || savedOnly) {
            resetAll();
            return;
        }
        super.onBackPressed();
    }

    private static String nonNull(String value) {
        return value == null ? "" : value;
    }

    private static final class EmptyCopy {
        final String title;
        final String body;
        final String action;
        final int icon;

        EmptyCopy(String title, String body, String action, int icon) {
            this.title = title;
            this.body = body;
            this.action = action;
            this.icon = icon;
        }
    }
}
