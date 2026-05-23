package com.flemmix.tv;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

    private WebView webView;
    private ProgressBar progressBar;
    private TextView errorView;
    private FrameLayout rootLayout;
    private ImageView cursorView;
    private FrameLayout customViewContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private boolean isVideoFullscreen = false;

    // Variables pour le curseur souris virtuel
    private int cursorX = 0;
    private int cursorY = 0;
    private int screenWidth = 1920;  // valeurs par défaut, mises à jour au démarrage
    private int screenHeight = 1080;
    private boolean cursorVisible = true;
    private static final int CURSOR_STEP = 20; // pixels par pression de touche

    private static final String TARGET_URL = "https://flemmix.win/";

    // Blocage des pubs (même liste que précédemment)
    private static final Set<String> AD_DOMAINS = new HashSet<>(Arrays.asList(
        "googlesyndication.com", "doubleclick.net", "googleadservices.com",
        "google-analytics.com", "googletagmanager.com", "googletagservices.com",
        "adservice.google.com", "pagead2.googlesyndication.com",
        "popads.net", "popcash.net", "propellerads.com", "exoclick.com",
        "trafficjunky.net", "juicyads.com", "adsterra.com", "hilltopads.net",
        "clickadu.com", "plugrush.com", "revcontent.com", "mgid.com",
        "adnxs.com", "adsrvr.org", "advertising.com", "outbrain.com",
        "taboola.com", "rubiconproject.com", "openx.net", "pubmatic.com",
        "smartadserver.com", "criteo.com", "criteo.net", "media.net",
        "adroll.com", "quantserve.com", "scorecardresearch.com",
        "amazon-adsystem.com", "yandexadexchange.net", "onclickads.net",
        "vidoomy.com", "streamads.net", "blockadblock.com", "detectadblock.com",
        "hotjar.com", "connect.facebook.net", "coinhive.com", "cryptoloot.pro"
    ));

    private static final String[] AD_KEYWORDS = {
        "/ads/", "/ad/", "/advertisement/", "/banner/", "/popup/",
        "pop.js", "popad", "popunder", "/tracking/", "adsense",
        "prebid.js", "/pagead/", "show_ads"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progress_bar);
        errorView = findViewById(R.id.error_view);
        rootLayout = findViewById(R.id.root_layout);
        customViewContainer = findViewById(R.id.custom_view_container);

        // Création du curseur
        createCursor();

        hideSystemUI();
        setupWebView();

        // Récupérer les dimensions réelles de l'écran
        rootLayout.post(() -> {
            screenWidth = rootLayout.getWidth();
            screenHeight = rootLayout.getHeight();
            cursorX = screenWidth / 2;
            cursorY = screenHeight / 2;
            updateCursorPosition();
        });

        if (isConnected()) {
            webView.loadUrl(TARGET_URL);
        } else {
            showError("Pas de connexion internet.\nVérifie ton réseau et appuie sur OK pour réessayer.");
        }
    }

    private void createCursor() {
        // Créer une petite image bitmap pour le curseur (un cercle rouge avec contour blanc)
        int size = 40;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.RED);
        canvas.drawCircle(size/2, size/2, size/2 - 2, paint);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawCircle(size/2, size/2, size/2 - 2, paint);
        // petite croix au centre
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(2);
        canvas.drawLine(size/2 - 6, size/2, size/2 + 6, size/2, paint);
        canvas.drawLine(size/2, size/2 - 6, size/2, size/2 + 6, paint);

        cursorView = new ImageView(this);
        cursorView.setImageDrawable(new BitmapDrawable(getResources(), bmp));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        cursorView.setLayoutParams(params);
        cursorView.setVisibility(View.VISIBLE);
        rootLayout.addView(cursorView);
        cursorView.bringToFront();
    }

    private void updateCursorPosition() {
        if (cursorView != null) {
            cursorView.setX(cursorX - cursorView.getWidth()/2);
            cursorView.setY(cursorY - cursorView.getHeight()/2);
        }
    }

    private void moveCursor(int dx, int dy) {
        cursorX += dx;
        cursorY += dy;
        // Limites
        cursorX = Math.max(0, Math.min(screenWidth, cursorX));
        cursorY = Math.max(0, Math.min(screenHeight, cursorY));
        updateCursorPosition();
        // Optionnel : faire un hover sur l'élément sous le curseur (simule souris)
        sendMouseHover(cursorX, cursorY);
    }

    private void sendMouseHover(int x, int y) {
        // Envoie un événement de survol à la WebView (pour les effets CSS)
        long now = android.os.SystemClock.uptimeMillis();
        MotionEvent hoverEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_HOVER_MOVE, x, y, 0);
        webView.dispatchGenericMotionEvent(hoverEvent);
        hoverEvent.recycle();
    }

    private void performClickAtCursor() {
        // Envoie un clic à la position du curseur
        long now = android.os.SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, cursorX, cursorY, 0);
        webView.dispatchTouchEvent(downEvent);
        downEvent.recycle();
        MotionEvent upEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, cursorX, cursorY, 0);
        webView.dispatchTouchEvent(upEvent);
        upEvent.recycle();
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private void showError(String msg) {
        errorView.setText(msg);
        errorView.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        if (cursorView != null) cursorView.setVisibility(View.GONE);
    }

    private void hideError() {
        errorView.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        if (cursorView != null) cursorView.setVisibility(View.VISIBLE);
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 11; SHIELD Android TV) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Safari/537.36"
        );

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setBackgroundColor(0xFF000000);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setFocusable(false); // Important : on ne veut pas de focus interne, le curseur gère tout
        webView.setFocusableInTouchMode(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                String url = req.getUrl().toString().toLowerCase();
                String host = req.getUrl().getHost();
                if (host != null) {
                    host = host.toLowerCase();
                    for (String d : AD_DOMAINS) {
                        if (host.contains(d)) {
                            return new WebResourceResponse("text/plain", "utf-8",
                                    new ByteArrayInputStream("".getBytes()));
                        }
                    }
                }
                for (String k : AD_KEYWORDS) {
                    if (url.contains(k)) {
                        return new WebResourceResponse("text/plain", "utf-8",
                                new ByteArrayInputStream("".getBytes()));
                    }
                }
                return super.shouldInterceptRequest(view, req);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                hideError();
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                injectAdBlocker(view);
                // Réafficher le curseur après chargement
                if (cursorView != null) cursorView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                progressBar.setVisibility(View.GONE);
                if (failingUrl != null && failingUrl.equals(TARGET_URL)) {
                    showError("Impossible de charger la page.\nAppuie sur OK pour réessayer.");
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String url = req.getUrl().toString();
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    return true;
                }
                view.loadUrl(url);
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                isVideoFullscreen = true;
                webView.setVisibility(View.GONE);
                if (cursorView != null) cursorView.setVisibility(View.GONE);
                if (customViewContainer != null) {
                    customViewContainer.setVisibility(View.VISIBLE);
                    customViewContainer.addView(customView);
                } else {
                    setContentView(customView);
                }
                hideSystemUI();
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                isVideoFullscreen = false;
                if (customViewContainer != null) {
                    customViewContainer.removeView(customView);
                    customViewContainer.setVisibility(View.GONE);
                } else {
                    setContentView(R.layout.activity_main);
                    // Reconstitution des vues
                    webView = findViewById(R.id.webview);
                    progressBar = findViewById(R.id.progress_bar);
                    errorView = findViewById(R.id.error_view);
                    rootLayout = findViewById(R.id.root_layout);
                    customViewContainer = findViewById(R.id.custom_view_container);
                    setupWebView();
                    webView.loadUrl(TARGET_URL);
                    createCursor();
                    rootLayout.post(() -> {
                        screenWidth = rootLayout.getWidth();
                        screenHeight = rootLayout.getHeight();
                        cursorX = screenWidth / 2;
                        cursorY = screenHeight / 2;
                        updateCursorPosition();
                    });
                }
                webView.setVisibility(View.VISIBLE);
                if (cursorView != null) cursorView.setVisibility(View.VISIBLE);
                customView = null;
                customViewCallback.onCustomViewHidden();
                hideSystemUI();
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog,
                                          boolean isUserGesture, android.os.Message resultMsg) {
                return false;
            }

            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progress == 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    private void injectAdBlocker(WebView view) {
        String js = "javascript:(function(){" +
            "var s=document.createElement('style');" +
            "s.innerHTML='" +
                "body{background:#000!important;}" +
                ".ads,.ad,.advertisement,.adsbygoogle," +
                "[class*=popup],[id*=popup],[class*=overlay]," +
                "[class*=advert],[class*=banner],[class*=sponsor]," +
                ".video-ad,.preroll,.interstitial{display:none!important;}" +
                "*{cursor:none!important;}" + // on cache le curseur du système
            "';" +
            "document.head.appendChild(s);" +
            "window.open=function(){return null;};" +
            "setInterval(function(){" +
                "document.querySelectorAll('*').forEach(function(el){" +
                    "var st=window.getComputedStyle(el);" +
                    "if((st.position==='fixed'||st.position==='absolute')" +
                    "&&parseInt(st.zIndex)>1000" +
                    "&&el.tagName!=='VIDEO'" +
                    "&&el.offsetWidth>100){" +
                        "el.style.display='none';}" +
                "});},2000);" +
        "})()";
        view.loadUrl(js);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Gestion écran d'erreur
        if (errorView != null && errorView.getVisibility() == View.VISIBLE &&
            (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
            hideError();
            webView.loadUrl(TARGET_URL);
            return true;
        }

        // En mode vidéo plein écran, on délègue au ChromeClient
        if (isVideoFullscreen) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    webView.evaluateJavascript("var v=document.querySelector('video');if(v){if(v.paused)v.play();else v.pause();}", null);
                    return true;
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    webView.evaluateJavascript("var v=document.querySelector('video');if(v)v.currentTime+=10;", null);
                    return true;
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                    webView.evaluateJavascript("var v=document.querySelector('video');if(v)v.currentTime-=10;", null);
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    if (customView != null) {
                        customViewCallback.onCustomViewHidden();
                        return true;
                    }
                    break;
            }
            return super.onKeyDown(keyCode, event);
        }

        // Navigation curseur
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                moveCursor(0, -CURSOR_STEP);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                moveCursor(0, CURSOR_STEP);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                moveCursor(-CURSOR_STEP, 0);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                moveCursor(CURSOR_STEP, 0);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                performClickAtCursor();
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                webView.evaluateJavascript("var v=document.querySelector('video');if(v){if(v.paused)v.play();else v.pause();}", null);
                return true;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                webView.evaluateJavascript("var v=document.querySelector('video');if(v)v.currentTime+=10;", null);
                return true;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                webView.evaluateJavascript("var v=document.querySelector('video');if(v)v.currentTime-=10;", null);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (isVideoFullscreen && customView != null) {
            customViewCallback.onCustomViewHidden();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        hideSystemUI();
        if (cursorView != null) cursorView.setVisibility(View.VISIBLE);
        // Re-vérifier les dimensions
        rootLayout.post(() -> {
            screenWidth = rootLayout.getWidth();
            screenHeight = rootLayout.getHeight();
            cursorX = Math.min(cursorX, screenWidth);
            cursorY = Math.min(cursorY, screenHeight);
            updateCursorPosition();
        });
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
