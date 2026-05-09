package com.xiaoju.browser;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class BookmarkActivity extends AppCompatActivity {

    private ListView listView;
    private BookmarkManager bookmarkManager;
    private List<BookmarkManager.BookmarkItem> bookmarks;
    private BookmarkAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.bookmarks);
        }

        bookmarkManager = new BookmarkManager(this);
        listView = findViewById(R.id.list_view);
        TextView emptyView = findViewById(R.id.empty_view);
        listView.setEmptyView(emptyView);

        loadBookmarks();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String url = bookmarks.get(position).url;
            Intent result = new Intent();
            result.putExtra("url", url);
            setResult(RESULT_OK, result);
            finish();
        });

        registerForContextMenu(listView);
    }

    private void loadBookmarks() {
        bookmarks = bookmarkManager.getAll();
        adapter = new BookmarkAdapter(this, bookmarks);
        listView.setAdapter(adapter);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, 1, 0, R.string.delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            BookmarkManager.BookmarkItem bm = bookmarks.get(info.position);
            new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirm)
                .setPositiveButton(R.string.confirm, (d, w) -> {
                    bookmarkManager.delete(bm.id);
                    loadBookmarks();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
