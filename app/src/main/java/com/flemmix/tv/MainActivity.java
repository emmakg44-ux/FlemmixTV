package com.flemmix.tv;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
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
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

    private WebView webView;
    private ProgressBar progressBar;

    // URL cible
    private static final String TARGET_URL = "https://flemmix.win/";

    // =============================================
    // BLOQUEUR DE PUBLICITÉS
    // Liste des domaines publicitaires à bloquer
    // =============================================
    private static final Set<String> AD_DOMAINS = new HashSet<>(Arrays.asList(
        // Réseaux publicitaires classiques
        "googlesyndication.com",
        "doubleclick.net",
        "googleadservices.com",
        "google-analytics.com",
        "googletagmanager.com",
        "googletagservices.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",

        // Publicités vidéo / pop-ups fréquents sur sites streaming
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "exoclick.com",
        "trafficjunky.net",
        "juicyads.com",
        "adsterra.com",
        "hilltopads.net",
        "clickadu.com",
        "plugrush.com",
        "adspyglass.com",
        "trafficholder.com",
        "revcontent.com",
        "mgid.com",
        "valueclick.com",
        "adnxs.com",
        "adsrvr.org",
        "advertising.com",
        "outbrain.com",
        "taboola.com",
        "zedo.com",
        "bidswitch.net",
        "rubiconproject.com",
        "openx.net",
        "pubmatic.com",
        "smartadserver.com",
        "criteo.com",
        "criteo.net",
        "admixer.net",
        "adform.net",
        "adblade.com",
        "media.net",
        "adroll.com",
        "quantserve.com",
        "scorecardresearch.com",
        "cdn.syndication.twimg.com",
        "ads.twitter.com",
        "amazon-adsystem.com",
        "adsymptotic.com",
        "yandex.ru/adv",
        "yandexadexchange.net",
        "adhigh.net",
        "adtelligent.com",
        "adskeeper.co.uk",
        "onclickads.net",
        "megapush.com",
        "go.ad2up.com",
        "cdn.adpushup.com",
        "adfox.ru",
        "vidoomy.com",
        "streamads.net",
        "streamrail.net",

        // Anti-adblock contournement
        "blockadblock.com",
        "detectadblock.com",
        "fuckadblock.js.org",

        // Trackers & analytics
        "hotjar.com",
        "mouseflow.com",
        "fullstory.com",
        "segment.com",
        "mixpanel.com",
        "amplitude.com",
        "intercom.io",
        "zendesk.com/embeddable_framework",
        "facebook.com/tr",
        "connect.facebook.net",
        "analytics.tiktok.com",

        // Miners de cryptomonnaie (souvent sur sites streaming)
        "coinhive.com",
        "cryptoloot.pro",
        "coin-have.com",
        "minero.cc",
        "webminepool.com",
        "jsecoin.com"
    ));

    // Mots-clés dans les URLs à bloquer
    private static final String[] AD_URL_KEYWORDS = {
        "/ads/", "/ad/", "/advertisement/", "/banner/", "/popup/",
        "pop.js", "popad", "popunder", "/tracking/", "/tracker/",
        "?adid=", "&adid=", "ad_unit", "adsense", "adscript",
        "prebid.js", "gpt.js", "show_ads", "/pagead/", "ad_frame"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Plein écran total
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Cacher la barre de navigation
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progress_bar);

        setupWebView();
        webView.loadUrl(TARGET_URL);
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();

        // Activer JavaScript (nécessaire pour les sites streaming)
        settings.setJavaScriptEnabled(true);

        // Support médias
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Zoom & affichage
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        // User-Agent Android TV (plus compatible avec les lecteurs vidéo)
        settings.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 10; SHIELD Android TV) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36"
        );

        // Cookies
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        // Activer l'accélération hardware
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // =============================================
        // CLIENT WEB AVEC BLOQUEUR DE PUBS
        // =============================================
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString().toLowerCase();
                String host = request.getUrl().getHost();

                if (host != null) {
                    host = host.toLowerCase();

                    // Vérifier si le domaine est dans la liste noire
                    for (String adDomain : AD_DOMAINS) {
                        if (host.contains(adDomain) || host.endsWith("." + adDomain)) {
                            // Retourner une réponse vide (bloquer)
                            return new WebResourceResponse(
                                "text/plain", "utf-8",
                                new ByteArrayInputStream("".getBytes())
                            );
                        }
                    }
                }

                // Vérifier les mots-clés dans l'URL
                for (String keyword : AD_URL_KEYWORDS) {
                    if (url.contains(keyword)) {
                        return new WebResourceResponse(
                            "text/plain", "utf-8",
                            new ByteArrayInputStream("".getBytes())
                        );
                    }
                }

                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);

                // Injection CSS pour améliorer l'affichage TV
                injectTVStyles(view);

                // Injection JS pour bloquer les pop-ups restants
                injectAdBlockerJS(view);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed(); // Accepter les certificats sur certains sites
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // Rester dans le WebView (ne pas ouvrir le navigateur externe)
                view.loadUrl(url);
                return true;
            }
        });

        // =============================================
        // CHROME CLIENT POUR VIDÉO PLEIN ÉCRAN
        // =============================================
        webView.setWebChromeClient(new WebChromeClient() {
            private View customView;

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // Plein écran pour la vidéo
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                webView.setVisibility(View.INVISIBLE);
                setContentView(view);
            }

            @Override
            public void onHideCustomView() {
                // Quitter le plein écran vidéo
                setContentView(R.layout.activity_main);
                webView = findViewById(R.id.webview);
                progressBar = findViewById(R.id.progress_bar);
                setupWebView();
                webView.loadUrl(TARGET_URL);
                customView = null;
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog,
                                          boolean isUserGesture, android.os.Message resultMsg) {
                // Bloquer les pop-ups publicitaires
                if (!isUserGesture) return false;
                return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
            }
        });
    }

    // =============================================
    // STYLES CSS POUR OPTIMISER L'AFFICHAGE TV
    // =============================================
    private void injectTVStyles(WebView view) {
        String css =
            "* { cursor: default !important; }" +
            "body { overflow-x: hidden !important; }" +
            // Cacher les bannières publicitaires communes
            ".ads, .ad, .advertisement, .adsbygoogle, " +
            "[class*='banner'], [class*='popup'], [id*='popup'], " +
            "[class*='advert'], [id*='advert'], [class*='sponsor'], " +
            ".overlay-ad, .video-ad, .preroll { " +
            "   display: none !important; " +
            "}" +
            // Améliorer les boutons pour la télécommande
            "a, button { outline: 3px solid transparent !important; }" +
            "a:focus, button:focus { " +
            "   outline: 3px solid #E50914 !important; " +
            "   transform: scale(1.05) !important; " +
            "}";

        String js = "javascript:(function() {" +
            "var style = document.createElement('style');" +
            "style.innerHTML = '" + css.replace("'", "\\'") + "';" +
            "document.head.appendChild(style);" +
            "})()";

        view.loadUrl(js);
    }

    // =============================================
    // BLOQUEUR JS POUR POP-UPS ET PUB RESTANTES
    // =============================================
    private void injectAdBlockerJS(WebView view) {
        String js = "javascript:(function() {" +
            // Bloquer window.open (pop-ups)
            "window.open = function() { return null; };" +

            // Bloquer alert/confirm publicitaires
            "var _originalAlert = window.alert;" +
            "window.alert = function(msg) {" +
            "   if (msg && msg.length > 100) return;" +
            "   _originalAlert(msg);" +
            "};" +

            // Observer pour supprimer les éléments pub qui apparaissent dynamiquement
            "var observer = new MutationObserver(function(mutations) {" +
            "   var adSelectors = [" +
            "       '[class*=\"popup\"]', '[id*=\"popup\"]'," +
            "       '[class*=\"overlay\"]', '.modal-backdrop'," +
            "       '[class*=\"advert\"]', '[class*=\"banner\"]'," +
            "       'iframe[src*=\"ads\"]', 'iframe[src*=\"pop\"]'" +
            "   ];" +
            "   adSelectors.forEach(function(sel) {" +
            "       document.querySelectorAll(sel).forEach(function(el) {" +
            "           if (el.offsetWidth > 200 && el.style.position === 'fixed') {" +
            "               el.remove();" +
            "           }" +
            "       });" +
            "   });" +
            "});" +
            "observer.observe(document.body, { childList: true, subtree: true });" +
            "})()";

        view.loadUrl(js);
    }

    // =============================================
    // NAVIGATION TÉLÉCOMMANDE TV (D-PAD)
    // =============================================
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                if (webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_DPAD_UP:
                webView.scrollBy(0, -150);
                return true;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                webView.scrollBy(0, 150);
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                webView.scrollBy(-200, 0);
                return true;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                webView.scrollBy(200, 0);
                return true;

            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                // Envoyer espace pour play/pause
                webView.evaluateJavascript(
                    "document.activeElement && document.activeElement.dispatchEvent(" +
                    "new KeyboardEvent('keydown', {keyCode: 32, which: 32}));", null
                );
                return true;

            case KeyEvent.KEYCODE_MENU:
                Toast.makeText(this, "Flemmix TV • Retour arrière: touche ←", Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
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

        // Garder plein écran après reprise
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
