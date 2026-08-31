package work.mcwws.webapp;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * 鸿蒙 / 部分国产 ROM 对 Material BottomNavigation 不稳定，改用原生底部栏。
 */
public class MainActivity extends AppCompatActivity {
    static final String HOST = "mcs.ryanstudio.work";
    /** 地图 iframe、资源子域；樱花自签证书需与主站一并放行 */
    static final String MAP_HOST = "mcsmap.ryanstudio.work";
    static final String APP_UA_SUFFIX = " MCWWSApp/1.1.2";

    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private View errorOverlay;
    private LinearLayout bottomNav;
    private ValueCallback<Uri[]> filePathCallback;
    private boolean pageHadError;
    private boolean selectingTab;
    @IdRes
    private int currentTabId = R.id.nav_shop;

    private final int[] tabIds = {
            R.id.nav_shop,
            R.id.nav_map,
            R.id.nav_manage,
            R.id.nav_me
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
            bindViews();
            configureBottomNav();
            configureWebView();
            startInitialPage();
        } catch (Throwable t) {
            Toast.makeText(this, "启动失败: " + t.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void bindViews() {
        webView = findViewById(R.id.webView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);
        errorOverlay = findViewById(R.id.errorOverlay);
        bottomNav = findViewById(R.id.bottomNav);
        Button retryButton = findViewById(R.id.retryButton);
        retryButton.setOnClickListener(v -> openTab(currentTabId, true));

        swipeRefresh.setColorSchemeResources(R.color.progress);
        swipeRefresh.setOnRefreshListener(this::reloadCurrent);
        swipeRefresh.setOnChildScrollUpCallback((parent, child) ->
                webView != null && webView.getScrollY() > 0);
    }

    private void configureBottomNav() {
        for (int id : tabIds) {
            View tab = findViewById(id);
            if (tab == null) {
                continue;
            }
            final int tabId = id;
            tab.setOnClickListener(v -> {
                if (currentTabId == tabId) {
                    openTab(tabId, true);
                } else {
                    openTab(tabId, false);
                }
            });
        }
        paintSelectedTab(currentTabId);
    }

    private void startInitialPage() {
        Uri deepLink = resolveDeepLink();
        if (deepLink != null) {
            int tab = tabIdForPath(deepLink.getPath());
            currentTabId = tab;
            paintSelectedTab(tab);
            updateSwipeForTab(tab);
            webView.loadUrl(deepLink.toString());
            return;
        }
        openTab(R.id.nav_shop, true);
    }

    private Uri resolveDeepLink() {
        Intent intent = getIntent();
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            Uri uri = intent.getData();
            if (HOST.equalsIgnoreCase(uri.getHost())) {
                return uri;
            }
        }
        return null;
    }

    private void openTab(@IdRes int tabId, boolean forceReload) {
        String target = urlForTab(tabId);
        String current = webView.getUrl();
        boolean sameSection = current != null && tabIdForPath(safePath(current)) == tabId;
        currentTabId = tabId;
        paintSelectedTab(tabId);
        updateSwipeForTab(tabId);
        errorOverlay.setVisibility(View.GONE);
        pageHadError = false;

        if (!forceReload && sameSection && current != null) {
            if (!pathsEquivalent(current, target)) {
                webView.loadUrl(target);
            }
            return;
        }
        webView.loadUrl(target);
    }

    private void paintSelectedTab(@IdRes int selectedId) {
        if (selectingTab) {
            return;
        }
        selectingTab = true;
        int active = ContextCompat.getColor(this, R.color.progress);
        int inactive = ContextCompat.getColor(this, R.color.nav_inactive);
        for (int id : tabIds) {
            View tab = findViewById(id);
            if (!(tab instanceof ViewGroup)) {
                continue;
            }
            boolean selected = id == selectedId;
            int color = selected ? active : inactive;
            ViewGroup group = (ViewGroup) tab;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof ImageView) {
                    ImageView image = (ImageView) child;
                    if (Build.VERSION.SDK_INT >= 21) {
                        image.setImageTintList(ColorStateList.valueOf(color));
                    } else {
                        image.setColorFilter(color);
                    }
                } else if (child instanceof TextView) {
                    ((TextView) child).setTextColor(color);
                }
            }
            tab.setSelected(selected);
        }
        selectingTab = false;
    }

    private void updateSwipeForTab(@IdRes int tabId) {
        swipeRefresh.setEnabled(tabId != R.id.nav_map);
    }

    private String urlForTab(@IdRes int tabId) {
        if (tabId == R.id.nav_map) {
            return getString(R.string.tab_map_url);
        }
        if (tabId == R.id.nav_manage) {
            return getString(R.string.tab_manage_url);
        }
        if (tabId == R.id.nav_me) {
            return getString(R.string.tab_me_url);
        }
        return getString(R.string.tab_shop_url);
    }

    @IdRes
    private int tabIdForPath(String path) {
        if (path == null) {
            return R.id.nav_shop;
        }
        String p = path.toLowerCase();
        if (p.contains("map.html")) {
            return R.id.nav_map;
        }
        if (p.contains("/manage/") || p.contains("admin.html") || p.contains("shop-locations")) {
            return R.id.nav_manage;
        }
        if (p.contains("ledger.html") || p.endsWith("/ledger")) {
            return R.id.nav_me;
        }
        return R.id.nav_shop;
    }

    private String safePath(String url) {
        try {
            return Uri.parse(url).getPath();
        } catch (Exception e) {
            return "/";
        }
    }

    private boolean pathsEquivalent(String urlA, String urlB) {
        try {
            return normalizePath(Uri.parse(urlA).getPath())
                    .equals(normalizePath(Uri.parse(urlB).getPath()));
        } catch (Exception e) {
            return false;
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= 21) {
            cookies.setAcceptThirdPartyCookies(webView, true);
        }

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        if (Build.VERSION.SDK_INT >= 21) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        String ua = settings.getUserAgentString();
        settings.setUserAgentString((ua == null ? "" : ua) + APP_UA_SUFFIX);

        webView.setBackgroundColor(Color.parseColor("#050505"));
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= 21) {
                    return handleUri(request.getUrl());
                }
                return false;
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUri(Uri.parse(url));
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                pageHadError = false;
                errorOverlay.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                syncTabFromUrl(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                swipeRefresh.setRefreshing(false);
                progressBar.setVisibility(View.GONE);
                CookieManager.getInstance().flush();
                syncTabFromUrl(url);
                injectAppShellPolish();
                if (url != null && !pageHadError) {
                    String path = normalizePath(safePath(url));
                    if ("/".equals(path) || "/home.html".equalsIgnoreCase(path)) {
                        webView.loadUrl(getString(R.string.tab_shop_url));
                    }
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (Build.VERSION.SDK_INT >= 23 && request.isForMainFrame()) {
                    showLoadError();
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                showLoadError();
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                String url = error != null ? error.getUrl() : null;
                Uri uri = url != null ? Uri.parse(url) : null;
                String host = uri != null ? uri.getHost() : null;
                if (isTrustedSslHost(host)) {
                    handler.proceed();
                    return;
                }
                handler.cancel();
                if (isMainFrameUrl(url)) {
                    showLoadError();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 || pageHadError ? View.GONE : View.VISIBLE);
            }

            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams
            ) {
                // 鸿蒙上系统文件选择器差异大：直接交给外部浏览器下载/打开，避免闪退
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                return false;
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.error_title, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isTrustedSslHost(@Nullable String host) {
        if (host == null) {
            return false;
        }
        return HOST.equalsIgnoreCase(host) || MAP_HOST.equalsIgnoreCase(host);
    }

    private boolean isMainFrameUrl(@Nullable String url) {
        if (url == null || webView == null) {
            return true;
        }
        String current = webView.getUrl();
        if (current == null || current.isEmpty()) {
            return true;
        }
        try {
            Uri failing = Uri.parse(url);
            Uri main = Uri.parse(current);
            return failing.getHost() != null
                    && failing.getHost().equalsIgnoreCase(main.getHost())
                    && normalizePath(failing.getPath()).equals(normalizePath(main.getPath()));
        } catch (Exception e) {
            return true;
        }
    }

    private void showLoadError() {
        pageHadError = true;
        swipeRefresh.setRefreshing(false);
        progressBar.setVisibility(View.GONE);
        errorOverlay.setVisibility(View.VISIBLE);
    }

    private void syncTabFromUrl(String url) {
        if (url == null) {
            return;
        }
        try {
            Uri uri = Uri.parse(url);
            if (!HOST.equalsIgnoreCase(uri.getHost())) {
                return;
            }
            int tab = tabIdForPath(uri.getPath());
            if (tab != currentTabId) {
                currentTabId = tab;
                paintSelectedTab(tab);
                updateSwipeForTab(tab);
            }
        } catch (Exception ignored) {
            /* ignore */
        }
    }

    private void injectAppShellPolish() {
        String js = "(function(){"
                + "if(document.getElementById('mcwws-app-shell-style'))return;"
                + "var s=document.createElement('style');"
                + "s.id='mcwws-app-shell-style';"
                + "s.textContent=["
                + "'.nav-link[href=\"home.html\"],a.nav-link[title*=\"更多服务\"]{display:none!important}',"
                + "'.services-hub-install-hint,#mcwwsAppDownload,.mcwws-auth-popover-services-link{display:none!important}',"
                + "'.services-map-auth-float{bottom:12px!important}'"
                + "].join('');"
                + "document.head.appendChild(s);"
                + "})();";
        webView.evaluateJavascript(js, null);
    }

    private boolean handleUri(Uri uri) {
        if (uri == null) {
            return true;
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            return false;
        }
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            if (HOST.equalsIgnoreCase(uri.getHost())) {
                syncTabFromUrl(uri.toString());
                return false;
            }
            openExternal(uri);
            return true;
        }
        openExternal(uri);
        return true;
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException ignored) {
            /* ignore */
        }
    }

    private void reloadCurrent() {
        errorOverlay.setVisibility(View.GONE);
        pageHadError = false;
        webView.reload();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        if (currentTabId != R.id.nav_shop) {
            openTab(R.id.nav_shop, true);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        if (webView != null) {
            webView.onPause();
        }
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
