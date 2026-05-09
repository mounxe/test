package com.xiaoju.browser;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // 标签页列表
    private List<TabInfo> tabList = new ArrayList<>();
    private int currentTabIndex = 0;

    // UI组件
    private EditText addressBar;
    private ProgressBar progressBar;
    private ImageButton btnBack, btnForward, btnRefresh, btnHome;
    private LinearLayout tabStrip;
    private HorizontalScrollView tabScrollView;
    private FrameLayout webContainer;
    private Toolbar toolbar;

    // 数据管理
    private BookmarkManager bookmarkManager;
    private HistoryManager historyManager;
    private SharedPreferences prefs;

    private static final String HOME_URL = "https://www.baidu.com";
    private static final String PREF_NAME = "xiaoju_prefs";
    private static final String PREF_SEARCH_ENGINE = "search_engine";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        bookmarkManager = new BookmarkManager(this);
        historyManager = new HistoryManager(this);

        initViews();
        setupToolbar();
        setupAddressBar();
        setupNavigationButtons();

        // 处理外部打开的URL
        Intent intent = getIntent();
        String initUrl = null;
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            initUrl = intent.getData().toString();
        }

        // 创建第一个标签页
        addNewTab(initUrl != null ? initUrl : HOME_URL);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        addressBar = findViewById(R.id.address_bar);
        progressBar = findViewById(R.id.progress_bar);
        btnBack = findViewById(R.id.btn_back);
        btnForward = findViewById(R.id.btn_forward);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnHome = findViewById(R.id.btn_home);
        tabStrip = findViewById(R.id.tab_strip);
        tabScrollView = findViewById(R.id.tab_scroll_view);
        webContainer = findViewById(R.id.web_container);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void setupAddressBar() {
        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUrl(addressBar.getText().toString().trim());
                hideKeyboard();
                return true;
            }
            return false;
        });

        // 长按复制URL
        addressBar.setOnLongClickListener(v -> {
            String url = addressBar.getText().toString();
            if (!TextUtils.isEmpty(url)) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("URL", url);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, R.string.url_copied, Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    private void setupNavigationButtons() {
        btnBack.setOnClickListener(v -> {
            WebView wv = getCurrentWebView();
            if (wv != null && wv.canGoBack()) wv.goBack();
        });

        btnForward.setOnClickListener(v -> {
            WebView wv = getCurrentWebView();
            if (wv != null && wv.canGoForward()) wv.goForward();
        });

        btnRefresh.setOnClickListener(v -> {
            WebView wv = getCurrentWebView();
            if (wv != null) {
                if (wv.getProgress() < 100) {
                    wv.stopLoading();
                    btnRefresh.setImageResource(R.drawable.ic_refresh);
                } else {
                    wv.reload();
                }
            }
        });

        btnHome.setOnClickListener(v -> {
            WebView wv = getCurrentWebView();
            if (wv != null) wv.loadUrl(HOME_URL);
        });
    }

    // =================== 标签页管理 ===================

    private void addNewTab(String url) {
        // 隐藏当前WebView
        if (!tabList.isEmpty() && currentTabIndex < tabList.size()) {
            tabList.get(currentTabIndex).webView.setVisibility(View.GONE);
        }

        WebView webView = createWebView();
        webContainer.addView(webView);

        TabInfo tab = new TabInfo(webView, url);
        tabList.add(tab);
        currentTabIndex = tabList.size() - 1;

        webView.loadUrl(url);
        refreshTabStrip();
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabList.size()) return;
        if (currentTabIndex < tabList.size()) {
            tabList.get(currentTabIndex).webView.setVisibility(View.GONE);
        }
        currentTabIndex = index;
        tabList.get(currentTabIndex).webView.setVisibility(View.VISIBLE);
        refreshTabStrip();
        updateAddressBar();
        updateNavButtons();
    }

    private void closeTab(int index) {
        if (tabList.size() <= 1) {
            Toast.makeText(this, R.string.last_tab_hint, Toast.LENGTH_SHORT).show();
            return;
        }
        WebView wv = tabList.get(index).webView;
        webContainer.removeView(wv);
        wv.destroy();
        tabList.remove(index);

        if (currentTabIndex >= tabList.size()) {
            currentTabIndex = tabList.size() - 1;
        }
        tabList.get(currentTabIndex).webView.setVisibility(View.VISIBLE);
        refreshTabStrip();
        updateAddressBar();
        updateNavButtons();
    }

    private void refreshTabStrip() {
        tabStrip.removeAllViews();
        for (int i = 0; i < tabList.size(); i++) {
            final int idx = i;
            View tabView = LayoutInflater.from(this).inflate(R.layout.item_tab, tabStrip, false);
            TextView title = tabView.findViewById(R.id.tab_title);
            ImageButton close = tabView.findViewById(R.id.tab_close);

            String tabTitle = tabList.get(i).title;
            title.setText(TextUtils.isEmpty(tabTitle) ? getString(R.string.new_tab) : tabTitle);

            if (i == currentTabIndex) {
                tabView.setSelected(true);
            }

            tabView.setOnClickListener(v -> switchToTab(idx));
            close.setOnClickListener(v -> closeTab(idx));

            tabStrip.addView(tabView);
        }
        // 滚动到当前标签
        tabScrollView.post(() -> {
            View current = tabStrip.getChildAt(currentTabIndex);
            if (current != null) tabScrollView.smoothScrollTo(current.getLeft(), 0);
        });
    }

    // =================== WebView工厂 ===================

    private WebView createWebView() {
        WebView webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setTextZoom(100);
        settings.setUserAgentString(settings.getUserAgentString() + " XiaoJuBrowser/1.0");

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        webView.setLayoutParams(lp);

        webView.setWebViewClient(new XiaoJuWebViewClient());
        webView.setWebChromeClient(new XiaoJuChromeClient());

        return webView;
    }

    // =================== URL加载 ===================

    private void loadUrl(String input) {
        if (TextUtils.isEmpty(input)) return;
        String url;
        if (input.startsWith("http://") || input.startsWith("https://") || input.startsWith("file://")) {
            url = input;
        } else if (input.contains(".") && !input.contains(" ")) {
            url = "https://" + input;
        } else {
            // 搜索
            String engine = prefs.getString(PREF_SEARCH_ENGINE, "https://www.baidu.com/s?wd=");
            url = engine + Uri.encode(input);
        }
        WebView wv = getCurrentWebView();
        if (wv != null) wv.loadUrl(url);
    }

    private WebView getCurrentWebView() {
        if (tabList.isEmpty() || currentTabIndex >= tabList.size()) return null;
        return tabList.get(currentTabIndex).webView;
    }

    private void updateAddressBar() {
        WebView wv = getCurrentWebView();
        if (wv != null) addressBar.setText(wv.getUrl());
    }

    private void updateNavButtons() {
        WebView wv = getCurrentWebView();
        if (wv == null) return;
        btnBack.setEnabled(wv.canGoBack());
        btnForward.setEnabled(wv.canGoForward());
        btnBack.setAlpha(wv.canGoBack() ? 1.0f : 0.4f);
        btnForward.setAlpha(wv.canGoForward() ? 1.0f : 0.4f);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(addressBar.getWindowToken(), 0);
    }

    // =================== 菜单 ===================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_new_tab) {
            addNewTab(HOME_URL);
            return true;
        } else if (id == R.id.menu_bookmark_add) {
            addBookmark();
            return true;
        } else if (id == R.id.menu_bookmarks) {
            startActivity(new Intent(this, BookmarkActivity.class));
            return true;
        } else if (id == R.id.menu_history) {
            startActivity(new Intent(this, HistoryActivity.class));
            return true;
        } else if (id == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.menu_share) {
            shareCurrentUrl();
            return true;
        } else if (id == R.id.menu_desktop_site) {
            toggleDesktopSite(item);
            return true;
        } else if (id == R.id.menu_clear_data) {
            showClearDataDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addBookmark() {
        WebView wv = getCurrentWebView();
        if (wv == null) return;
        String url = wv.getUrl();
        String title = wv.getTitle();
        if (TextUtils.isEmpty(url)) return;

        bookmarkManager.addBookmark(title, url);
        Toast.makeText(this, R.string.bookmark_saved, Toast.LENGTH_SHORT).show();
    }

    private void shareCurrentUrl() {
        WebView wv = getCurrentWebView();
        if (wv == null) return;
        String url = wv.getUrl();
        if (TextUtils.isEmpty(url)) return;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, url);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
    }

    private void toggleDesktopSite(MenuItem item) {
        WebView wv = getCurrentWebView();
        if (wv == null) return;
        WebSettings settings = wv.getSettings();
        boolean isDesktop = item.isChecked();
        item.setChecked(!isDesktop);
        if (!isDesktop) {
            settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36");
        } else {
            settings.setUserAgentString(null);
        }
        wv.reload();
    }

    private void showClearDataDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.clear_data)
            .setMessage(R.string.clear_data_confirm)
            .setPositiveButton(R.string.confirm, (d, w) -> {
                historyManager.clearAll();
                WebView wv = getCurrentWebView();
                if (wv != null) wv.clearCache(true);
                Toast.makeText(this, R.string.data_cleared, Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    // =================== 返回键处理 ===================

    @Override
    public void onBackPressed() {
        WebView wv = getCurrentWebView();
        if (wv != null && wv.canGoBack()) {
            wv.goBack();
        } else {
            // 关闭当前标签或退出
            if (tabList.size() > 1) {
                closeTab(currentTabIndex);
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onDestroy() {
        for (TabInfo tab : tabList) {
            tab.webView.destroy();
        }
        tabList.clear();
        super.onDestroy();
    }

    // =================== WebViewClient ===================

    private class XiaoJuWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (url.startsWith("http://") || url.startsWith("https://")) {
                return false;
            }
            // 处理其他scheme (tel, mailto等)
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } catch (Exception e) {
                // ignore
            }
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progressBar.setVisibility(View.VISIBLE);
            btnRefresh.setImageResource(R.drawable.ic_stop);
            if (isCurrentWebView(view)) {
                addressBar.setText(url);
                updateNavButtons();
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.GONE);
            btnRefresh.setImageResource(R.drawable.ic_refresh);
            if (isCurrentWebView(view)) {
                updateNavButtons();
                // 保存历史
                String title = view.getTitle();
                if (!TextUtils.isEmpty(url) && !url.equals("about:blank")) {
                    historyManager.addHistory(title, url);
                }
                // 更新标签标题
                for (TabInfo tab : tabList) {
                    if (tab.webView == view) {
                        tab.title = TextUtils.isEmpty(title) ? url : title;
                    }
                }
                refreshTabStrip();
            }
        }
    }

    // =================== WebChromeClient ===================

    private class XiaoJuChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (isCurrentWebView(view)) {
                progressBar.setProgress(newProgress);
            }
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            for (TabInfo tab : tabList) {
                if (tab.webView == view) {
                    tab.title = title;
                }
            }
            refreshTabStrip();
        }
    }

    private boolean isCurrentWebView(WebView view) {
        return !tabList.isEmpty() && currentTabIndex < tabList.size()
            && tabList.get(currentTabIndex).webView == view;
    }

    // =================== 数据类 ===================

    static class TabInfo {
        WebView webView;
        String url;
        String title;

        TabInfo(WebView webView, String url) {
            this.webView = webView;
            this.url = url;
            this.title = "";
        }
    }
}
