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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private enum Mode { HOME, BROWSING }
    private Mode currentMode = Mode.HOME;

    // UI：主页
    private LinearLayout homeScreen;
    private EditText homeSearchBar;

    // UI：浏览
    private Toolbar toolbarTop;
    private EditText addressBar;
    private ProgressBar progressBar;
    private FrameLayout webContainer;

    // UI：底部工具栏
    private LinearLayout bottomToolbar;
    private ImageButton btnBack, btnForward, btnRefreshTop;
    private ImageButton btnHomeBottom, btnTabs, btnMenu;

    // WebView 管理
    private List<TabInfo> tabList = new ArrayList<>();
    private int currentTabIndex = 0;
    private AlertDialog tabDialog;

    // 数据
    private BookmarkManager bookmarkManager;
    private HistoryManager historyManager;
    private SharedPreferences prefs;

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
        setupListeners();
        showHome();
    }

    private void initViews() {
        homeScreen = findViewById(R.id.home_screen);
        homeSearchBar = findViewById(R.id.home_search_bar);

        toolbarTop = findViewById(R.id.toolbar_top);
        addressBar = findViewById(R.id.address_bar);
        progressBar = findViewById(R.id.progress_bar);
        webContainer = findViewById(R.id.web_container);

        bottomToolbar = findViewById(R.id.bottom_toolbar);
        btnBack = findViewById(R.id.btn_back);
        btnForward = findViewById(R.id.btn_forward);
        btnRefreshTop = findViewById(R.id.btn_refresh_top);
        btnHomeBottom = findViewById(R.id.btn_home_bottom);
        btnTabs = findViewById(R.id.btn_tabs);
        btnMenu = findViewById(R.id.btn_menu);
    }

    private void setupListeners() {
        homeSearchBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                String input = homeSearchBar.getText().toString().trim();
                if (!TextUtils.isEmpty(input)) {
                    enterBrowsingMode();
                    addNewTab(convertToUrl(input));
                }
                return true;
            }
            return false;
        });

        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                loadUrl(addressBar.getText().toString().trim());
                hideKeyboard();
                return true;
            }
            return false;
        });

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

        btnBack.setOnClickListener(v -> {
            WebView wv = getCurrentWebView();
            if (wv != null && wv.canGoBack()) wv.goBack();
        });

        btnForward.setOnClickListener(v -> {
            WebView wv = getCurrentWebView();
            if (wv != null && wv.canGoForward()) wv.goForward();
        });

        btnRefreshTop.setOnClickListener(v -> {
            WebView wv = getCurrentWebView();
            if (wv != null) {
                if (wv.getProgress() < 100) {
                    wv.stopLoading();
                } else {
                    wv.reload();
                }
            }
        });

        btnHomeBottom.setOnClickListener(v -> showHome());

        btnTabs.setOnClickListener(v -> showTabManager());

        btnMenu.setOnClickListener(v -> showBottomMenu());
    }

    // =================== 模式切换 ===================

    private void showHome() {
        currentMode = Mode.HOME;
        homeScreen.setVisibility(View.VISIBLE);
        toolbarTop.setVisibility(View.GONE);
        webContainer.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        for (TabInfo tab : tabList) {
            webContainer.removeView(tab.webView);
            tab.webView.destroy();
        }
        tabList.clear();
        currentTabIndex = 0;
    }

    private void enterBrowsingMode() {
        currentMode = Mode.BROWSING;
        homeScreen.setVisibility(View.GONE);
        toolbarTop.setVisibility(View.VISIBLE);
        webContainer.setVisibility(View.VISIBLE);
    }

    // =================== 标签页管理 ===================

    private void addNewTab(String url) {
        enterBrowsingMode();

        WebView webView = createWebView();
        webContainer.addView(webView);

        String title = (url != null) ? url : "";
        TabInfo tab = new TabInfo(webView, title);
        tabList.add(tab);
        currentTabIndex = tabList.size() - 1;

        if (url != null) {
            webView.loadUrl(url);
        }

        updateAddressBar();
        updateNavButtons();
    }

    private WebView getCurrentWebView() {
        if (tabList.isEmpty() || currentTabIndex >= tabList.size()) return null;
        return tabList.get(currentTabIndex).webView;
    }

    private void updateAddressBar() {
        WebView wv = getCurrentWebView();
        if (wv != null) {
            String url = wv.getUrl();
            if (url != null && !url.equals("about:blank")) {
                addressBar.setText(url);
            }
        }
    }

    private void updateNavButtons() {
        WebView wv = getCurrentWebView();
        if (wv == null) {
            btnBack.setEnabled(false);
            btnForward.setEnabled(false);
            btnBack.setAlpha(0.4f);
            btnForward.setAlpha(0.4f);
            return;
        }
        btnBack.setEnabled(wv.canGoBack());
        btnForward.setEnabled(wv.canGoForward());
        btnBack.setAlpha(wv.canGoBack() ? 1.0f : 0.4f);
        btnForward.setAlpha(wv.canGoForward() ? 1.0f : 0.4f);
    }

    // =================== URL 加载 ===================

    private String convertToUrl(String input) {
        if (TextUtils.isEmpty(input)) return null;
        if (input.startsWith("http://") || input.startsWith("https://")) {
            return input;
        } else if (input.contains(".") && !input.contains(" ")) {
            return "https://" + input;
        } else {
            String engine = prefs.getString(PREF_SEARCH_ENGINE, "https://www.baidu.com/s?wd=");
            return engine + Uri.encode(input);
        }
    }

    private void loadUrl(String input) {
        String url = convertToUrl(input);
        if (url == null) return;
        WebView wv = getCurrentWebView();
        if (wv != null) {
            wv.loadUrl(url);
        } else {
            enterBrowsingMode();
            addNewTab(url);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(addressBar.getWindowToken(), 0);
    }

    // =================== 标签页管理 ===================

    private void showTabManager() {
        if (tabList.isEmpty()) {
            enterBrowsingMode();
            addNewTab(null);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("标签页 (" + tabList.size() + ")");

        // 创建标签页列表
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        for (int i = 0; i < tabList.size(); i++) {
            final int index = i;
            TabInfo tab = tabList.get(i);

            // 每个标签页的布局
            LinearLayout tabLayout = new LinearLayout(this);
            tabLayout.setOrientation(LinearLayout.HORIZONTAL);
            tabLayout.setPadding(16, 24, 16, 24);
            tabLayout.setBackgroundResource(android.R.drawable.list_selector_background);

            // 标签页标题
            TextView tv = new TextView(this);
            String displayText = tab.webView.getTitle();
            if (TextUtils.isEmpty(displayText)) {
                displayText = tab.webView.getUrl();
            }
            if (TextUtils.isEmpty(displayText)) {
                displayText = "新标签页";
            }
            tv.setText(displayText);
            tv.setTextSize(16);
            tv.setTextColor(0xFF000000);
            tv.setSingleLine(true);
            tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            tv.setLayoutParams(textParams);
            tv.setPadding(0, 0, 16, 0);

            // 关闭按钮
            ImageButton closeBtn = new ImageButton(this);
            closeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            closeBtn.setBackground(null);
            closeBtn.setPadding(8, 8, 8, 8);
            final int closeIndex = i;
            closeBtn.setOnClickListener(v -> {
                removeTab(closeIndex);
            });

            tabLayout.addView(tv);
            tabLayout.addView(closeBtn);

            // 点击切换到该标签页
            final int clickIndex = i;
            tabLayout.setOnClickListener(v -> {
                switchToTab(clickIndex);
                if (tabDialog != null) tabDialog.dismiss();
            });

            layout.addView(tabLayout);

            // 分隔线
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(0xFFE0E0E0);
            layout.addView(divider);
        }

        // 新建标签页按钮
        Button newTabBtn = new Button(this);
        newTabBtn.setText("+ 新建标签页");
        newTabBtn.setBackground(null);
        newTabBtn.setTextColor(0xFFFF6600);
        newTabBtn.setPadding(16, 24, 16, 24);
        newTabBtn.setOnClickListener(v -> {
            if (tabDialog != null) tabDialog.dismiss();
            enterBrowsingMode();
            addNewTab(null);
        });
        layout.addView(newTabBtn);

        builder.setView(layout);
        builder.setNegativeButton("关闭", null);

        tabDialog = builder.create();
        tabDialog.show();
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabList.size()) return;

        enterBrowsingMode();

        // 隐藏当前WebView
        WebView current = getCurrentWebView();
        if (current != null) {
            current.setVisibility(View.GONE);
        }

        // 显示选中的WebView
        currentTabIndex = index;
        TabInfo tab = tabList.get(currentTabIndex);
        tab.webView.setVisibility(View.VISIBLE);

        updateAddressBar();
        updateNavButtons();
    }

    private void removeTab(int index) {
        if (index < 0 || index >= tabList.size()) return;

        TabInfo tab = tabList.get(index);
        webContainer.removeView(tab.webView);
        tab.webView.destroy();
        tabList.remove(index);

        if (tabList.isEmpty()) {
            if (tabDialog != null) tabDialog.dismiss();
            showHome();
            return;
        }

        if (currentTabIndex >= tabList.size()) {
            currentTabIndex = tabList.size() - 1;
        }

        switchToTab(currentTabIndex);
        showTabManager(); // 刷新对话框
    }

    // =================== 底部菜单 ===================

    private void showBottomMenu() {
    String[] items = {
            getString(R.string.add_bookmark),
            getString(R.string.bookmarks),
            getString(R.string.history),
            getString(R.string.desktop_site),
            getString(R.string.share),
            getString(R.string.clear_data),
            getString(R.string.settings)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.more);
        builder.setItems(items, (dialog, which) -> {
            switch (which) {
                case 0: addBookmark(); break;
                case 1: startActivity(new Intent(this, BookmarkActivity.class)); break;
                case 2: startActivity(new Intent(this, HistoryActivity.class)); break;
                case 3: toggleDesktopSite(); break;
                case 4: shareCurrentUrl(); break;
                case 5: showClearDataDialog(); break;
                case 6: startActivity(new Intent(this, SettingsActivity.class)); break;
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void addBookmark() {
        WebView wv = getCurrentWebView();
        if (wv == null) return;
        String url = wv.getUrl();
        String title = wv.getTitle();
        if (TextUtils.isEmpty(url) || "about:blank".equals(url)) return;
        bookmarkManager.addBookmark(title, url);
        Toast.makeText(this, R.string.bookmark_saved, Toast.LENGTH_SHORT).show();
    }

    private void shareCurrentUrl() {
        WebView wv = getCurrentWebView();
        if (wv == null) return;
        String url = wv.getUrl();
        if (TextUtils.isEmpty(url) || "about:blank".equals(url)) return;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, url);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
    }

    private boolean isDesktopMode() {
        if (tabList.isEmpty() || currentTabIndex >= tabList.size()) return false;
        return tabList.get(currentTabIndex).desktopMode;
    }

    private void toggleDesktopSite() {
        if (tabList.isEmpty() || currentTabIndex >= tabList.size()) return;
        WebView wv = getCurrentWebView();
        if (wv == null) return;
        WebSettings settings = wv.getSettings();
        TabInfo tab = tabList.get(currentTabIndex);
        if (!tab.desktopMode) {
            settings.setUserAgentString(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
            );
            tab.desktopMode = true;
        } else {
            settings.setUserAgentString(null);
            tab.desktopMode = false;
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

    // =================== 返回键 ===================

    @Override
    public void onBackPressed() {
        if (currentMode == Mode.HOME) {
            super.onBackPressed();
            return;
        }
        WebView wv = getCurrentWebView();
        if (wv != null && wv.canGoBack()) {
            wv.goBack();
        } else {
            showHome();
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

    // =================== WebView 工厂 ===================

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

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        webView.setLayoutParams(lp);

        webView.setWebViewClient(new XiaoJuWebViewClient());
        webView.setWebChromeClient(new XiaoJuChromeClient());

        return webView;
    }

    // =================== WebViewClient ===================

    private class XiaoJuWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (url.startsWith("http://") || url.startsWith("https://")) {
                return false;
            }
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } catch (Exception e) { /* ignore */ }
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progressBar.setVisibility(View.VISIBLE);
            btnRefreshTop.setImageResource(R.drawable.ic_stop);
            if (isCurrentWebView(view)) {
                addressBar.setText(url);
                updateNavButtons();
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.GONE);
            btnRefreshTop.setImageResource(R.drawable.ic_refresh);
            if (isCurrentWebView(view)) {
                updateNavButtons();
                if (!TextUtils.isEmpty(url) && !url.equals("about:blank")) {
                    historyManager.addHistory(view.getTitle(), url);
                }
            }
        }
    }

    private class XiaoJuChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (isCurrentWebView(view)) {
                progressBar.setProgress(newProgress);
            }
        }
    }

    private boolean isCurrentWebView(WebView view) {
        return !tabList.isEmpty() && currentTabIndex < tabList.size()
            && tabList.get(currentTabIndex).webView == view;
    }

    // =================== 数据类 ===================

    static class TabInfo {
        WebView webView;
        String title;
        boolean desktopMode;

        TabInfo(WebView webView, String title) {
            this.webView = webView;
            this.title = title;
            this.desktopMode = false;
        }
    }
}
