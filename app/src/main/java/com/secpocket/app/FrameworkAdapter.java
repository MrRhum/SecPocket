package com.secpocket.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * List adapter for the field library. It renders section headers, brief cards
 * and one empty-state row without any external dependency.
 */
public final class FrameworkAdapter extends BaseAdapter {
    public interface Listener {
        void onOpen(Framework framework);

        void onToggleSave(Framework framework);

        void onEmptyAction();
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_EMPTY = 2;

    private final Context context;
    private final LayoutInflater inflater;
    private final Listener listener;
    private final List<Row> rows = new ArrayList<>();

    public FrameworkAdapter(Context context, Listener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    public void setItems(List<Framework> items, boolean grouped, String emptyTitle,
                         String emptyBody, String emptyAction, int emptyIcon) {
        rows.clear();
        if (items.isEmpty()) {
            rows.add(Row.empty(emptyTitle, emptyBody, emptyAction, emptyIcon));
        } else if (grouped) {
            Map<String, List<Framework>> groups = new LinkedHashMap<>();
            for (Framework framework : items) {
                List<Framework> group = groups.get(framework.primaryTopic);
                if (group == null) {
                    group = new ArrayList<>();
                    groups.put(framework.primaryTopic, group);
                }
                group.add(framework);
            }
            for (Map.Entry<String, List<Framework>> entry : groups.entrySet()) {
                rows.add(Row.header(entry.getKey(), entry.getValue().size()));
                for (Framework framework : entry.getValue()) {
                    rows.add(Row.item(framework));
                }
            }
        } else {
            for (Framework framework : items) {
                rows.add(Row.item(framework));
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public Object getItem(int position) {
        return rows.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Row row = rows.get(position);
        if (row.type == TYPE_HEADER) {
            HeaderHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_topic_header, parent, false);
                holder = new HeaderHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (HeaderHolder) convertView.getTag();
            }
            bindHeader(holder, row);
            return convertView;
        }
        if (row.type == TYPE_EMPTY) {
            EmptyHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_empty, parent, false);
                holder = new EmptyHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (EmptyHolder) convertView.getTag();
            }
            bindEmpty(holder, row);
            return convertView;
        }

        ItemHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_framework, parent, false);
            holder = new ItemHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ItemHolder) convertView.getTag();
        }
        bindItem(holder, row.framework);
        return convertView;
    }

    private void bindHeader(HeaderHolder holder, Row row) {
        Topic topic = Topics.byName(row.header);
        holder.name.setText(row.header.toUpperCase(Locale.ROOT));
        holder.name.setTextColor(Ui.TEXT_MUTED);
        holder.icon.setImageResource(topic.iconRes);
        holder.icon.setColorFilter(topic.color);
        holder.count.setText(String.format(Locale.ROOT, "%d", row.headerCount));
    }

    private void bindEmpty(EmptyHolder holder, Row row) {
        holder.title.setText(row.emptyTitle);
        holder.body.setText(row.emptyBody);
        holder.icon.setImageResource(row.emptyIcon);
        holder.icon.setColorFilter(Ui.ACCENT);
        holder.iconTile.setBackground(Ui.rounded(context, 0x1F4FD1C5, 19f));
        if (row.emptyAction == null || row.emptyAction.isEmpty()) {
            holder.action.setVisibility(View.GONE);
        } else {
            holder.action.setVisibility(View.VISIBLE);
            holder.action.setText(row.emptyAction);
            holder.action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onEmptyAction();
                }
            });
        }
    }

    private void bindItem(ItemHolder holder, final Framework framework) {
        Topic topic = Topics.byName(framework.primaryTopic);
        holder.card.setBackground(Ui.card(context));
        holder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onOpen(framework);
            }
        });

        holder.iconTile.setBackground(Ui.rounded(context, Ui.withAlpha(topic.color, 0x24), 13f));
        holder.icon.setImageResource(topic.iconRes);
        holder.icon.setColorFilter(topic.color);

        holder.title.setText(framework.title);
        String meta = framework.issuer;
        if (!framework.edition.isEmpty()) {
            meta = meta + "  \u00B7  " + framework.edition;
        }
        holder.meta.setText(meta);

        if (framework.hook.isEmpty()) {
            holder.hook.setVisibility(View.GONE);
        } else {
            holder.hook.setVisibility(View.VISIBLE);
            holder.hook.setText(framework.hook);
        }

        holder.topicBadge.setText(Topics.shortLabel(framework.primaryTopic).toUpperCase(Locale.ROOT));
        holder.topicBadge.setTextColor(topic.color);
        holder.topicBadge.setBackground(Ui.pillBackground(context, topic.color));
        holder.essentialBadge.setVisibility(framework.essential ? View.VISIBLE : View.GONE);
        holder.essentialBadge.setBackground(Ui.rounded(context, 0x332B2415, 7f));

        final boolean saved = BookmarkStore.isSaved(context, framework.id);
        holder.bookmark.setImageResource(saved ? R.drawable.ic_bookmark : R.drawable.ic_bookmark_border);
        holder.bookmark.setColorFilter(saved ? Ui.GOLD : Ui.TEXT_MUTED);
        holder.bookmark.setContentDescription(context.getString(
                saved ? R.string.bookmark_remove : R.string.bookmark_add));
        holder.bookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onToggleSave(framework);
            }
        });
    }

    private static final class HeaderHolder {
        final ImageView icon;
        final TextView name;
        final TextView count;

        HeaderHolder(View view) {
            icon = view.findViewById(R.id.topicIcon);
            name = view.findViewById(R.id.topicName);
            count = view.findViewById(R.id.topicCount);
        }
    }

    private static final class ItemHolder {
        final LinearLayout card;
        final FrameLayout iconTile;
        final ImageView icon;
        final TextView title;
        final TextView meta;
        final TextView hook;
        final TextView topicBadge;
        final TextView essentialBadge;
        final ImageButton bookmark;

        ItemHolder(View view) {
            card = view.findViewById(R.id.card);
            iconTile = view.findViewById(R.id.iconTile);
            icon = view.findViewById(R.id.topicIcon);
            title = view.findViewById(R.id.title);
            meta = view.findViewById(R.id.meta);
            hook = view.findViewById(R.id.hook);
            topicBadge = view.findViewById(R.id.topicBadge);
            essentialBadge = view.findViewById(R.id.essentialBadge);
            bookmark = view.findViewById(R.id.bookmark);
        }
    }

    private static final class EmptyHolder {
        final FrameLayout iconTile;
        final ImageView icon;
        final TextView title;
        final TextView body;
        final TextView action;

        EmptyHolder(View view) {
            iconTile = view.findViewById(R.id.emptyIconTile);
            icon = view.findViewById(R.id.emptyIcon);
            title = view.findViewById(R.id.emptyTitle);
            body = view.findViewById(R.id.emptyBody);
            action = view.findViewById(R.id.emptyAction);
        }
    }

    private static final class Row {
        final int type;
        final Framework framework;
        final String header;
        final int headerCount;
        final String emptyTitle;
        final String emptyBody;
        final String emptyAction;
        final int emptyIcon;

        private Row(int type, Framework framework, String header, int headerCount,
                    String emptyTitle, String emptyBody, String emptyAction, int emptyIcon) {
            this.type = type;
            this.framework = framework;
            this.header = header;
            this.headerCount = headerCount;
            this.emptyTitle = emptyTitle;
            this.emptyBody = emptyBody;
            this.emptyAction = emptyAction;
            this.emptyIcon = emptyIcon;
        }

        static Row header(String name, int count) {
            return new Row(TYPE_HEADER, null, name, count, null, null, null, 0);
        }

        static Row item(Framework framework) {
            return new Row(TYPE_ITEM, framework, null, 0, null, null, null, 0);
        }

        static Row empty(String title, String body, String action, int icon) {
            return new Row(TYPE_EMPTY, null, null, 0, title, body, action, icon);
        }
    }
}
