package es.carview.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Independent implementation of a CarStream-like video browser.
 *
 * Intended for parked use. This is NOT an Android Auto video projection
 * integration and does not attempt to evade car driver-distraction limits.
 */
public class MainActivity extends Activity {

    private static final String HOME_URL = "https://m.youtube.com/";
    private static final String PREFS = "carview_prefs";
    private static final String KEY_DESKTOP = "desktop_mode";
    private static final String KEY_BOOKMARKS = "bookmarks_json";
    private static final String KEY_HISTORY = "history_json";
    private static final int PICK_VIDEO_REQUEST = 501;
    private static final int VOICE_REQUEST = 502;
    private static final int HISTORY_LIMIT = 35;

    private FrameLayout rootLayer;
    private LinearLayout browserShell;
    private View topBar;
    private View navigationBar;
    private View safetyLabel;
    private WebView webView;
    private EditText addressField;
    private ProgressBar progressBar;
    private ImageButton backButton;
    private ImageButton forwardButton;
    private ImageButton starButton;
    private ImageButton menuButton;

    private SharedPreferences preferences;
    private boolean desktopMode;
    private String mobileUserAgent;
    private String desktopUserAgent;
    private boolean immersiveBrowser;
    private FrameLayout fullScreenContainer;
    private View fullScreenView;
    private WebChromeClient.CustomViewCallback fullScreenCallback;
    private FrameLayout localVideoContainer;
    private VideoView localVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        desktopMode = preferences.getBoolean(KEY_DESKTOP, false);

        rootLayer = findViewById(R.id.root_layer);
        browserShell = findViewById(R.id.browser_shell);
        topBar = findViewById(R.id.top_bar);
        navigationBar = findViewById(R.id.navigation_bar);
        safetyLabel = findViewById(R.id.safety_label);
        webView = findViewById(R.id.web_view);
        addressField = findViewById(R.id.address_field);
        progressBar = findViewById(R.id.progress_bar);
        backButton = findViewById(R.id.back_button);
        forwardButton = findViewById(R.id.forward_button);
        starButton = findViewById(R.id.star_button);
        menuButton = findViewById(R.id.menu_button);

        applyWindowInsets();
        configureWebView();
        configureControls();
        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(HOME_URL);
        }
    }

    private void applyWindowInsets() {
        // targetSdk 35 uses edge-to-edge layout on Android 15+.
        rootLayer.setOnApplyWindowInsetsListener((view, insets) -> {
            int top;
            int bottom;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                        android.view.WindowInsets.Type.systemBars());
                top = bars.top;
                bottom = bars.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            // Full-screen video may use the entire screen; otherwise respect bars.
            if (fullScreenView == null && localVideoContainer == null && !immersiveBrowser) {
                view.setPadding(0, top, 0, bottom);
            } else {
                view.setPadding(0, 0, 0, 0);
            }
            return insets;
        });
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true); // Required by the YouTube website.
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            settings.setSafeBrowsingEnabled(true);
        }
        mobileUserAgent = settings.getUserAgentString();
        desktopUserAgent = mobileUserAgent
                .replaceFirst("\\(Linux; Android[^)]*\\)", "(X11; Linux x86_64)")
                .replace("Version/4.0 ", "")
                .replace(" Mobile Safari", " Safari");
        if (desktopMode) settings.setUserAgentString(desktopUserAgent);
        CookieManager.getInstance().setAcceptCookie(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if (scheme == null) return true;
                // Reject unexpected app/intent schemes; never load local files or arbitrary intents.
                if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                    if (request.isForMainFrame()) {
                        showToast("Este tipo de enlace no está disponible aquí");
                    }
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(8);
                addressField.setText(url);
                updateButtons();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                addressField.setText(url);
                rememberPage(url, view.getTitle());
                updateButtons();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                if (request.isForMainFrame()) {
                    showToast("No se pudo cargar la página. Comprueba tu conexión.");
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress >= 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                showFullScreenVideo(view, callback);
            }

            @Override
            public void onHideCustomView() {
                hideFullScreenVideo();
            }
        });
        updateButtons();
    }

    private void configureControls() {
        backButton.setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
        });
        forwardButton.setOnClickListener(v -> {
            if (webView.canGoForward()) webView.goForward();
        });
        findViewById(R.id.home_button).setOnClickListener(v -> webView.loadUrl(HOME_URL));
        findViewById(R.id.reload_button).setOnClickListener(v -> webView.reload());
        findViewById(R.id.go_button).setOnClickListener(v -> navigateToInput());
        addressField.setOnEditorActionListener((textView, action, event) -> {
            if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_SEARCH
                    || action == EditorInfo.IME_ACTION_DONE) {
                navigateToInput();
                return true;
            }
            return false;
        });
        starButton.setOnClickListener(v -> toggleBookmark());
        starButton.setOnLongClickListener(v -> {
            showSavedList(KEY_BOOKMARKS, "Favoritos");
            return true;
        });
        menuButton.setOnClickListener(this::showOptionsMenu);
        findViewById(R.id.voice_button).setOnClickListener(v -> startVoiceSearch());
    }

    private void navigateToInput() {
        String typed = addressField.getText().toString().trim();
        if (typed.isEmpty()) return;
        String url;
        if (typed.matches("(?i)^https?://.*")) {
            url = typed;
        } else if (!typed.contains(" ") && typed.contains(".")
                && !typed.startsWith(".")) {
            url = "https://" + typed;
        } else {
            url = "https://m.youtube.com/results?search_query="
                    + Uri.encode(typed);
        }
        try {
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
                showToast("Escribe una URL web válida");
                return;
            }
            webView.loadUrl(url);
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(addressField.getWindowToken(), 0);
            addressField.clearFocus();
        } catch (IllegalArgumentException e) {
            showToast("Dirección no válida");
        }
    }

    private void updateButtons() {
        if (webView == null) return;
        backButton.setAlpha(webView.canGoBack() ? 1.0f : 0.38f);
        forwardButton.setAlpha(webView.canGoForward() ? 1.0f : 0.38f);
        String url = currentUrl();
        starButton.setAlpha(containsUrl(KEY_BOOKMARKS, url) ? 1.0f : 0.64f);
    }

    private String currentUrl() {
        String url = webView.getUrl();
        return url == null ? HOME_URL : url;
    }

    private String currentTitle() {
        String title = webView.getTitle();
        if (title == null || title.isEmpty()) return currentUrl();
        return title;
    }

    private void showOptionsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Inicio");
        popup.getMenu().add(0, 2, 1, "Actualizar");
        popup.getMenu().add(0, 3, 2, "Favoritos");
        popup.getMenu().add(0, 4, 3, "Historial");
        popup.getMenu().add(0, 5, 4, desktopMode ? "Desactivar modo escritorio" : "Activar modo escritorio");
        popup.getMenu().add(0, 6, 5, "Abrir vídeo del móvil");
        popup.getMenu().add(0, 7, 6, "Pantalla completa");
        popup.getMenu().add(0, 8, 7, "Abrir página en navegador");
        popup.getMenu().add(0, 9, 8, "Ajustes e información");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: webView.loadUrl(HOME_URL); break;
                case 2: webView.reload(); break;
                case 3: showSavedList(KEY_BOOKMARKS, "Favoritos"); break;
                case 4: showSavedList(KEY_HISTORY, "Historial"); break;
                case 5: changeDesktopMode(); break;
                case 6: chooseLocalVideo(); break;
                case 7: setBrowserImmersive(true); break;
                case 8: openInBrowser(); break;
                case 9: showSettingsDialog(); break;
                default: return false;
            }
            return true;
        });
        popup.show();
    }

    private void changeDesktopMode() {
        desktopMode = !desktopMode;
        preferences.edit().putBoolean(KEY_DESKTOP, desktopMode).apply();
        webView.getSettings().setUserAgentString(
                desktopMode ? desktopUserAgent : mobileUserAgent);
        webView.reload();
        showToast(desktopMode ? "Modo escritorio activado" : "Modo móvil activado");
    }

    private void toggleBookmark() {
        String url = currentUrl();
        if (!url.startsWith("https://") && !url.startsWith("http://")) return;
        JSONArray saved = readJson(KEY_BOOKMARKS);
        JSONArray next = new JSONArray();
        boolean removed = false;
        for (int i = 0; i < saved.length(); i++) {
            JSONObject entry = saved.optJSONObject(i);
            if (entry == null) continue;
            if (url.equals(entry.optString("url"))) {
                removed = true;
            } else {
                next.put(entry);
            }
        }
        if (removed) {
            writeJson(KEY_BOOKMARKS, next);
            showToast("Eliminado de favoritos");
        } else {
            next.put(makeEntry(url, currentTitle()));
            writeJson(KEY_BOOKMARKS, next);
            showToast("Guardado en favoritos");
        }
        updateButtons();
    }

    private void rememberPage(String url, String title) {
        if (url == null || !url.startsWith("https://") && !url.startsWith("http://")) {
            return;
        }
        JSONArray old = readJson(KEY_HISTORY);
        JSONArray next = new JSONArray();
        next.put(makeEntry(url, title));
        for (int i = 0; i < old.length() && next.length() < HISTORY_LIMIT; i++) {
            JSONObject e = old.optJSONObject(i);
            if (e != null && !url.equals(e.optString("url"))) next.put(e);
        }
        writeJson(KEY_HISTORY, next);
    }

    private JSONObject makeEntry(String url, String title) {
        JSONObject object = new JSONObject();
        try {
            object.put("url", url);
            object.put("title", title == null || title.trim().isEmpty() ? url : title.trim());
        } catch (JSONException ignored) {
            // JSONObject.put cannot fail for these string values.
        }
        return object;
    }

    private JSONArray readJson(String key) {
        try {
            return new JSONArray(preferences.getString(key, "[]"));
        } catch (JSONException ignored) {
            return new JSONArray();
        }
    }

    private void writeJson(String key, JSONArray array) {
        preferences.edit().putString(key, array.toString()).apply();
    }

    private boolean containsUrl(String key, String url) {
        JSONArray array = readJson(key);
        for (int i = 0; i < array.length(); i++) {
            JSONObject entry = array.optJSONObject(i);
            if (entry != null && url.equals(entry.optString("url"))) return true;
        }
        return false;
    }

    private void showSavedList(String key, String label) {
        JSONArray entries = readJson(key);
        if (entries.length() == 0) {
            showToast(label + ": todavía no hay páginas guardadas");
            return;
        }
        String[] labels = new String[entries.length()];
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.optJSONObject(i);
            labels[i] = entry == null ? "Página" : entry.optString("title", "Página");
        }
        new AlertDialog.Builder(this)
                .setTitle(label)
                .setItems(labels, (dialog, position) -> {
                    JSONObject selected = entries.optJSONObject(position);
                    if (selected == null) return;
                    String url = selected.optString("url");
                    new AlertDialog.Builder(this)
                            .setTitle(selected.optString("title", "Página"))
                            .setMessage(url)
                            .setPositiveButton("Abrir", (d, w) -> webView.loadUrl(url))
                            .setNeutralButton("Eliminar", (d, w) -> removeEntry(key, url))
                            .setNegativeButton("Cancelar", null)
                            .show();
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void removeEntry(String key, String url) {
        JSONArray old = readJson(key);
        JSONArray next = new JSONArray();
        for (int i = 0; i < old.length(); i++) {
            JSONObject entry = old.optJSONObject(i);
            if (entry != null && !url.equals(entry.optString("url"))) next.put(entry);
        }
        writeJson(key, next);
        updateButtons();
        showToast("Entrada eliminada");
    }

    private void showSettingsDialog() {
        String details = "CarView 1.0.0\n\n"
                + "Navegador de YouTube, favoritos, historial, modo escritorio y vídeos locales.\n\n"
                + "Reproduce vídeos solamente cuando el vehículo esté estacionado. "
                + "Android Auto convencional no ofrece soporte oficial general para "
                + "vídeo de terceros. Esta aplicación no elude sus restricciones.\n\n"
                + "YouTube puede limitar funciones o inicio de sesión dentro de WebView.";
        new AlertDialog.Builder(this)
                .setTitle("Ajustes e información")
                .setMessage(details)
                .setPositiveButton(desktopMode ? "Usar modo móvil" : "Usar modo escritorio",
                        (dialog, which) -> changeDesktopMode())
                .setNeutralButton("Borrar historial", (dialog, which) -> {
                    new AlertDialog.Builder(this)
                            .setTitle("¿Borrar historial?")
                            .setMessage("Se borrarán las direcciones visitadas, no los favoritos.")
                            .setPositiveButton("Borrar", (d, w) -> {
                                writeJson(KEY_HISTORY, new JSONArray());
                                showToast("Historial borrado");
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void startVoiceSearch() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Busca un vídeo");
        try {
            startActivityForResult(intent, VOICE_REQUEST);
        } catch (ActivityNotFoundException exception) {
            showToast("No hay una aplicación de reconocimiento de voz instalada");
        }
    }

    private void chooseLocalVideo() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(intent, PICK_VIDEO_REQUEST);
        } catch (ActivityNotFoundException e) {
            showToast("No se encontró un selector de archivos");
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == PICK_VIDEO_REQUEST && data.getData() != null) {
            showLocalVideo(data.getData());
        } else if (requestCode == VOICE_REQUEST) {
            ArrayList<String> matches = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) {
                addressField.setText(matches.get(0));
                navigateToInput();
            }
        }
    }

    private void showLocalVideo(Uri videoUri) {
        closeLocalVideo();
        if (fullScreenView != null) hideFullScreenVideo();
        webView.onPause();
        localVideoContainer = new FrameLayout(this);
        localVideoContainer.setBackgroundColor(Color.BLACK);
        localVideo = new VideoView(this);
        FrameLayout.LayoutParams videoParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        videoParams.gravity = Gravity.CENTER;
        localVideoContainer.addView(localVideo, videoParams);

        TextView closeButton = new TextView(this);
        closeButton.setText("✕  Cerrar vídeo");
        closeButton.setTextSize(17);
        closeButton.setTextColor(Color.WHITE);
        closeButton.setBackgroundColor(0xDD16181E);
        closeButton.setPadding(dp(18), dp(12), dp(18), dp(12));
        closeButton.setOnClickListener(v -> closeLocalVideo());
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        closeParams.topMargin = dp(20);
        closeParams.rightMargin = dp(20);
        localVideoContainer.addView(closeButton, closeParams);

        browserShell.setVisibility(View.GONE);
        rootLayer.addView(localVideoContainer,
                new FrameLayout.LayoutParams(-1, -1));
        setImmersiveSystemUi(true);
        MediaController controller = new MediaController(this);
        controller.setAnchorView(localVideo);
        localVideo.setMediaController(controller);
        localVideo.setOnErrorListener((mp, what, extra) -> {
            showToast("El formato o códec del vídeo no es compatible con este dispositivo");
            closeLocalVideo();
            return true;
        });
        localVideo.setOnPreparedListener(mp -> {
            if (localVideo == null || localVideoContainer == null) return;
            mp.setScreenOnWhilePlaying(true);
            localVideo.start();
            controller.show(2400);
        });
        localVideo.setVideoURI(videoUri);
        localVideo.requestFocus();
        rootLayer.requestApplyInsets();
    }

    private void closeLocalVideo() {
        if (localVideoContainer == null) return;
        if (localVideo != null) {
            localVideo.stopPlayback();
            localVideo = null;
        }
        rootLayer.removeView(localVideoContainer);
        localVideoContainer.removeAllViews();
        localVideoContainer = null;
        browserShell.setVisibility(View.VISIBLE);
        webView.onResume();
        setImmersiveSystemUi(false);
        rootLayer.requestApplyInsets();
    }

    private void showFullScreenVideo(View view, WebChromeClient.CustomViewCallback callback) {
        if (fullScreenView != null) {
            callback.onCustomViewHidden();
            return;
        }
        fullScreenView = view;
        fullScreenCallback = callback;
        fullScreenContainer = new FrameLayout(this);
        fullScreenContainer.setBackgroundColor(Color.BLACK);
        fullScreenContainer.addView(view, new FrameLayout.LayoutParams(-1, -1));
        rootLayer.addView(fullScreenContainer, new FrameLayout.LayoutParams(-1, -1));
        browserShell.setVisibility(View.GONE);
        setImmersiveSystemUi(true);
        rootLayer.requestApplyInsets();
    }

    private void hideFullScreenVideo() {
        if (fullScreenView == null) return;
        rootLayer.removeView(fullScreenContainer);
        fullScreenContainer.removeAllViews();
        fullScreenContainer = null;
        fullScreenView = null;
        WebChromeClient.CustomViewCallback callback = fullScreenCallback;
        fullScreenCallback = null;
        if (callback != null) callback.onCustomViewHidden();
        browserShell.setVisibility(View.VISIBLE);
        setImmersiveSystemUi(immersiveBrowser);
        rootLayer.requestApplyInsets();
    }

    private void setBrowserImmersive(boolean value) {
        immersiveBrowser = value;
        topBar.setVisibility(value ? View.GONE : View.VISIBLE);
        navigationBar.setVisibility(value ? View.GONE : View.VISIBLE);
        safetyLabel.setVisibility(value ? View.GONE : View.VISIBLE);
        setImmersiveSystemUi(value);
        rootLayer.requestApplyInsets();
        if (value) showToast("Pulsa Atrás para salir de pantalla completa");
    }

    @SuppressWarnings("deprecation")
    private void setImmersiveSystemUi(boolean immersive) {
        if (immersive) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void openInBrowser() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl()));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            showToast("No hay un navegador disponible");
        }
    }

    private int dp(int size) {
        return (int) (getResources().getDisplayMetrics().density * size + 0.5f);
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (fullScreenView != null) {
            hideFullScreenVideo();
        } else if (localVideoContainer != null) {
            closeLocalVideo();
        } else if (immersiveBrowser) {
            setBrowserImmersive(false);
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Don't allow web audio/video to continue when the activity is obscured,
        // for example by the parked/driving transition on a compatible head unit.
        webView.evaluateJavascript(
                "(function(){document.querySelectorAll('video,audio').forEach(function(m){m.pause();});})();",
                null);
        webView.onPause();
        if (localVideo != null && localVideo.isPlaying()) {
            localVideo.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (localVideoContainer == null) webView.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (localVideoContainer != null) closeLocalVideo();
        if (fullScreenView != null) hideFullScreenVideo();
        ((ViewGroup) webView.getParent()).removeView(webView);
        webView.destroy();
        super.onDestroy();
    }
}
