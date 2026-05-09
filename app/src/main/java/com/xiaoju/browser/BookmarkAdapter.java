package com.xiaoju.browser;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookmarkAdapter extends BaseAdapter {

    private Context context;
    private List<BookmarkManager.BookmarkItem> items;
    private SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

    public BookmarkAdapter(Context context, List<BookmarkManager.BookmarkItem> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() { return items.size(); }

    @Override
    public Object getItem(int position) { return items.get(position); }

    @Override
    public long getItemId(int position) { return items.get(position).id; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_bookmark, parent, false);
        }
        BookmarkManager.BookmarkItem item = items.get(position);
        TextView title = convertView.findViewById(R.id.item_title);
        TextView url = convertView.findViewById(R.id.item_url);
        title.setText(item.title);
        url.setText(item.url);
        return convertView;
    }
}
