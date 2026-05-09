package com.xiaoju.browser;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private RadioGroup searchEngineGroup;
    private EditText customEngineEdit;

    private static final String PREF_NAME = "xiaoju_prefs";
    private static final String PREF_SEARCH_ENGINE = "search_engine";
    private static final String PREF_CUSTOM_ENGINE = "custom_engine";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings);
        }

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        searchEngineGroup = findViewById(R.id.search_engine_group);
        customEngineEdit = findViewById(R.id.custom_engine_edit);

        // 加载当前设置
        String currentEngine = prefs.getString(PREF_SEARCH_ENGINE, "https://www.baidu.com/s?wd=");
        String customEngine = prefs.getString(PREF_CUSTOM_ENGINE, "");
        customEngineEdit.setText(customEngine);

        if (currentEngine.equals("https://www.baidu.com/s?wd=")) {
            searchEngineGroup.check(R.id.rb_baidu);
        } else if (currentEngine.equals("https://www.bing.com/search?q=")) {
            searchEngineGroup.check(R.id.rb_bing);
        } else if (currentEngine.equals("https://www.google.com/search?q=")) {
            searchEngineGroup.check(R.id.rb_google);
        } else if (currentEngine.equals("https://sogou.com/web?query=")) {
            searchEngineGroup.check(R.id.rb_sogou);
        } else {
            searchEngineGroup.check(R.id.rb_custom);
        }

        findViewById(R.id.btn_save).setOnClickListener(v -> saveSettings());
    }

    private void saveSettings() {
        String engine;
        int checked = searchEngineGroup.getCheckedRadioButtonId();
        if (checked == R.id.rb_baidu) {
            engine = "https://www.baidu.com/s?wd=";
        } else if (checked == R.id.rb_bing) {
            engine = "https://www.bing.com/search?q=";
        } else if (checked == R.id.rb_google) {
            engine = "https://www.google.com/search?q=";
        } else if (checked == R.id.rb_sogou) {
            engine = "https://sogou.com/web?query=";
        } else {
            String custom = customEngineEdit.getText().toString().trim();
            engine = custom.isEmpty() ? "https://www.baidu.com/s?wd=" : custom;
        }

        prefs.edit()
            .putString(PREF_SEARCH_ENGINE, engine)
            .putString(PREF_CUSTOM_ENGINE, customEngineEdit.getText().toString().trim())
            .apply();

        android.widget.Toast.makeText(this, R.string.settings_saved, android.widget.Toast.LENGTH_SHORT).show();
        finish();
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
