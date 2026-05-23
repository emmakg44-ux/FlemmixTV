package com.flemmix.tv;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.KeyEvent;
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
    private FrameLayout customViewContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private boolean isVideoFullscreen = false;

    private static final String TARGET_URL = "https://flemmix.win/";

    // Liste des domaines publicitaires (bloqués)
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
        customViewContainer = findViewById(R.id.custom_view_container); // à ajouter dans le layout

        hideSystemUI();
        setupWebView();

        if (isConnected()) {
            webView.loadUrl(TARGET_URL);
        } else {
            showError("Pas de connexion internet.\nVérifie ton réseau et appuie sur OK pour réessayer.");
        }
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
    }

    private void hideError() {
        errorView.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
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
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus();

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
                injectStylesAndAdBlocker(view);
                // Force le focus sur le WebView pour la télécommande
                view.requestFocus();
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
                handler.proceed(); // Accepte les certificats SSL (si site safe)
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String url = req.getUrl().toString();
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    return true; // bloque les intents non web
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

                // Cache le WebView et affiche le conteneur plein écran
                webView.setVisibility(View.GONE);
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
                    // Re-récupérer les références
                    webView = findViewById(R.id.webview);
                    progressBar = findViewById(R.id.progress_bar);
                    errorView = findViewById(R.id.error_view);
                    customViewContainer = findViewById(R.id.custom_view_container);
                    setupWebView(); // Reconfigure le WebView
                    webView.loadUrl(TARGET_URL);
                }
                webView.setVisibility(View.VISIBLE);
                customView = null;
                customViewCallback.onCustomViewHidden();
                hideSystemUI();
                webView.requestFocus();
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog,
                                          boolean isUserGesture, android.os.Message resultMsg) {
                // Empêche l'ouverture de nouvelles fenêtres (popups)
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

    private void injectStylesAndAdBlocker(WebView view) {
        // CSS + JS combiné pour masquer les pubs, améliorer le focus et empêcher les popups
        String js = "javascript:(function(){" +
            // Supprime les popups résiduelles
            "var killPopups=setInterval(function(){" +
                "var divs=document.querySelectorAll('div[style*=\"fixed\"],div[style*=\"absolute\"]');" +
                "for(var i=0;i<divs.length;i++){" +
                    "var d=divs[i];" +
                    "if((d.offsetWidth>200 || d.offsetHeight>100) && (d.style.zIndex>1000 || d.style.position==='fixed')){" +
                        "if(!d.querySelector('video')) d.style.display='none';" +
                    "}" +
                "}" +
            "},1000);" +

            // Style CSS renforcé
            "var s=document.createElement('style');" +
            "s.innerHTML='" +
                "body{background:#000!important;margin:0;padding:0;}" +
                ".ads,.ad,.advertisement,.adsbygoogle," +
                "[class*=popup],[id*=popup],[class*=overlay]," +
                "[class*=advert],[class*=banner],[class*=sponsor]," +
                ".video-ad,.preroll,.interstitial,.stick-ad," +
                "[class*=googleAd],[id*=googleAd],ins.adsbygoogle{" +
                "display:none!important;visibility:hidden!important;height:0!important;width:0!important;}" +

                // Style de focus pour la télécommande
                "a:focus,button:focus,input:focus,select:focus,textarea:focus,[tabindex]:focus,div[role=button]:focus{" +
                    "outline:3px solid #E50914!important;" +
                    "outline-offset:3px!important;" +
                    "border-radius:4px!important;" +
                    "box-shadow:0 0 0 3px rgba(229,9,20,0.4)!important;" +
                "}" +
                // Cache le curseur souris
                "*{cursor:none!important;}" +
            "';" +
            "document.head.appendChild(s);" +

            // Bloque window.open et les redirections de popups
            "window.open=function(){return null;};" +
            "var originalAlert=window.alert; window.alert=function(){};" +
            "var originalConfirm=window.confirm; window.confirm=function(){return false;};" +

            // Supprime les iframes suspectes (pubs)
            "var iframes=document.querySelectorAll('iframe');" +
            "for(var i=0;i<iframes.length;i++){" +
                "var src=iframes[i].src.toLowerCase();" +
                "if(src.indexOf('ads')!==-1 || src.indexOf('pop')!==-1 || src.indexOf('doubleclick')!==-1){" +
                    "iframes[i].style.display='none';" +
                    "iframes[i].remove();" +
                "}" +
            "}" +

            // Nettoie les intervalles et timeouts potentiellement malicieux toutes les 5 secondes
            "setInterval(function(){" +
                "var highestTimeoutId = setTimeout(function(){},0);" +
                "for(var i=0;i<highestTimeoutId;i++){" +
                    "clearTimeout(i); clearInterval(i);" +
                "}" +
            "},5000);" +
        "})()";
        view.loadUrl(js);
    }

    // Navigation DPAD améliorée
    private void navigateFocus(String direction) {
        String js = "javascript:(function(){" +
            "var focusable = Array.from(document.querySelectorAll('a, button, input, textarea, select, [tabindex]:not([tabindex=\"-1\"]), [role=\"button\"], video'));" +
            "focusable = focusable.filter(el => el.offsetParent !== null && getComputedStyle(el).visibility !== 'hidden');" +
            "if(focusable.length === 0) return;" +
            "var active = document.activeElement;" +
            "var index = focusable.indexOf(active);" +
            "var newIndex = index;" +
            "var step = 0;" +
            "if('" + direction + "' === 'up') { step = -1; }" +
            "else if('" + direction + "' === 'down') { step = 1; }" +
            "else if('" + direction + "' === 'left') { step = -1; }" +
            "else if('" + direction + "' === 'right') { step = 1; }" +
            "else return;" +
            "if (index !== -1) {" +
                "newIndex = index + step;" +
                "if(newIndex >= 0 && newIndex < focusable.length) {" +
                    "focusable[newIndex].focus();" +
                    "focusable[newIndex].scrollIntoView({block: 'center', behavior: 'smooth'});" +
                    "return;" +
                "}" +
            "}" +
            // Si aucun élément focusable adjacent, on scroll la page
            "if('" + direction + "' === 'up') window.scrollBy(0, -200);" +
            "else if('" + direction + "' === 'down') window.scrollBy(0, 200);" +
            "else if('" + direction + "' === 'left') window.scrollBy(-200, 0);" +
            "else if('" + direction + "' === 'right') window.scrollBy(200, 0);" +
        "})()";
        webView.evaluateJavascript(js, null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Gestion de l'écran d'erreur : OK recharge
        if (errorView != null && errorView.getVisibility() == View.VISIBLE &&
            (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
            hideError();
            webView.loadUrl(TARGET_URL);
            return true;
        }

        // Gestion des touches média même en vidéo plein écran
        if (isVideoFullscreen) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    webView.evaluateJavascript(
                        "var v=document.querySelector('video');if(v){if(v.paused)v.play();else v.pause();}", null);
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

        // Navigation standard sur la WebView
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                if (webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                webView.evaluateJavascript(
                    "var el = document.activeElement; if(el && (el.click || (el.tagName === 'A'))) { if(el.click) el.click(); else if(el.href) window.location=el.href; }", null);
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
                navigateFocus("up");
                return true;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                navigateFocus("down");
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                navigateFocus("left");
                return true;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                navigateFocus("right");
                return true;

            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                webView.evaluateJavascript(
                    "var v=document.querySelector('video');if(v){if(v.paused)v.play();else v.pause();}", null);
                return true;

            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                webView.evaluateJavascript(
                    "var v=document.querySelector('video');if(v)v.currentTime+=10;", null);
                return true;

            case KeyEvent.KEYCODE_MEDIA_REWIND:
                webView.evaluateJavascript(
                    "var v=document.querySelector('video');if(v)v.currentTime-=10;", null);
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
        webView.requestFocus();
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
