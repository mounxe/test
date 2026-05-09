package com.xiaoju.browser;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private ListView listView;
    private HistoryManager historyManager;
    private List<HistoryManager.HistoryItem> historyList;
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.history);
        }

        historyManager = new HistoryManager(this);
        listView = findViewById(R.id.list_view);
        TextView emptyView = findViewById(R.id.empty_view);
        listView.setEmptyView(emptyView);

        loadHistory();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String url = historyList.get(position).url;
            Intent result = new Intent();
            result.putExtra("url", url);
            setResult(RESULT_OK, result);
            finish();
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            HistoryManager.HistoryItem item = historyList.get(position);
            new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirm)
                .setPositiveButton(R.string.confirm, (d, w) -> {
                    historyManager.delete(item.id);
                    loadHistory();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
            return true;
        });
    }

    private void loadHistory() {
        historyList = historyManager.getAll();
        adapter = new HistoryAdapter(this, historyList);
        listView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        menu.add(0, 1, 0, R.string.clear_all);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == 1) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.clear_all)
                .setMessage(R.string.clear_history_confirm)
                .setPositiveButton(R.string.confirm, (d, w) -> {
                    historyManager.clearAll();
                    loadHistory();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        }
        return super.onOptionsItemSelected(item);
    }
}
