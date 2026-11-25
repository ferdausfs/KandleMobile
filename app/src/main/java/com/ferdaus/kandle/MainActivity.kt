package com.ferdaus.kandle

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebViewAssetLoader
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private lateinit var refresh: SwipeRefreshLayout
    private lateinit var bottom: BottomNavigationView
    private lateinit var statusSpacer: View
    private lateinit var navHost: FrameLayout
    private lateinit var navTick: View
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var immersive = false

    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        fileCallback?.onReceiveValue(uri?.let { arrayOf(it) } ?: emptyArray())
        fileCallback = null
    }

    private val multiFilePicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        fileCallback?.onReceiveValue(uris?.toTypedArray() ?: emptyArray())
        fileCallback = null
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)
        progress = findViewById(R.id.progress)
        refresh = findViewById(R.id.refresh)
        bottom = findViewById(R.id.bottom)
        statusSpacer = findViewById(R.id.status_spacer)
        navHost = findViewById(R.id.nav_host)
        navTick = findViewById(R.id.nav_tick)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            statusSpacer.layoutParams.height = bars.top
            statusSpacer.requestLayout()
            val landscape = resources.configuration.orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE
            if (ime.bottom > 0) {
                bottom.visibility = View.GONE
                navHost.setPadding(0, 0, 0, 0)
                refresh.setPadding(0, 0, 0, ime.bottom)
            } else {
                bottom.visibility = if (immersive || landscape) View.GONE else View.VISIBLE
                navHost.visibility = if (immersive || landscape) View.GONE else View.VISIBLE
                navHost.setPadding(0, 0, 0, bars.bottom)
                refresh.setPadding(0, 0, 0, 0)
            }
            insets
        }
        wireChrome()
        requestNotificationPermission()
        setupWebView()
        bottom.selectedItemId = R.id.tab_chart
        moveTick(1)
        updateChromeVisibility()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (immersive) {
                    immersive = false
                    updateChromeVisibility()
                    return
                }
                webView.evaluateJavascript(
                    "!!document.querySelector('.modal-search,.alerts-modal,.modal-indicators," +
                        ".lay-menu,.tb-menu:not([hidden]),section.script-panel:not([hidden])')",
                ) { open ->
                    if (open == "true") {
                        sendKey(KeyEvent.KEYCODE_ESCAPE, alt = false)
                    } else if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        finish()
                    }
                }
            }
        })
    }

    private fun wireChrome() {
        bottom.setOnItemSelectedListener { item ->
            bottom.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            handleTab(item.itemId)
            true
        }
        bottom.setOnItemReselectedListener { item ->
            bottom.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            handleTab(item.itemId)
        }
        refresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.kandle_accent))
        refresh.setOnRefreshListener { webView.reload() }
        // The chart owns vertical drags; reload lives in the Menu sheet.
        refresh.isEnabled = false
    }

    private val assetLoader by lazy {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            textZoom = (100 * resources.configuration.fontScale).toInt()
        }
        CookieManager.getInstance().setAcceptCookie(true)
        webView.addJavascriptInterface(NativeBridge(), "AndroidNative")
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                assetLoader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (UrlPolicy.isInternal(url)) return false
                when {
                    url.startsWith("mailto:") -> runCatching {
                        startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(url)))
                    }
                    url.startsWith("tel:") -> runCatching {
                        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(url)))
                    }
                    UrlPolicy.isExternal(url) -> openExternal(url)
                }
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                injectHelper()
                progress.visibility = View.GONE
                refresh.isRefreshing = false
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse,
            ) {
                if (request.isForMainFrame) {
                    showError("HTTP ${errorResponse.statusCode}")
                }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    progress.visibility = View.GONE
                    refresh.isRefreshing = false
                    showError(error.description?.toString().orEmpty())
                }
            }

            override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = null
                progress.visibility = View.GONE
                refresh.isRefreshing = false
                Toast.makeText(
                    this@MainActivity,
                    "Chart engine restarted — unsaved work may be lost",
                    Toast.LENGTH_LONG,
                ).show()
                recreateWebView()
                return true
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress.isIndeterminate = false
                progress.progress = newProgress
                progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }

            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams,
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                val types = params.acceptTypes.takeIf { it.isNotEmpty() } ?: arrayOf("*/*")
                val multiple = params.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE
                return runCatching {
                    if (multiple) multiFilePicker.launch(types) else filePicker.launch(types)
                    fileCallback = callback
                    true
                }.getOrElse {
                    callback.onReceiveValue(null)
                    false
                }
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                // The bundled app declares no camera or microphone features.
                request.deny()
            }
        }
        webView.setDownloadListener { url, _, _, _, _ -> openExternal(url) }
        webView.loadUrl(UrlPolicy.BASE + "index.html")
    }

    private fun injectHelper() {
        val script = """
            (function () {
              if (window.__kandleNative) return;
              window.__kandleNative = {
                key: function (k, alt) {
                  var event = new KeyboardEvent('keydown', { key: k, altKey: !!alt, bubbles: true, cancelable: true });
                  document.dispatchEvent(event);
                  window.dispatchEvent(event);
                },
                click: function (text) {
                  var target = String(text).toLowerCase();
                  var els = Array.prototype.slice.call(document.querySelectorAll('button,[role="button"],a'));
                  for (var i = 0; i < els.length; i++) {
                    var label = ((els[i].textContent || '') + ' ' + (els[i].getAttribute('aria-label') || '')).trim().toLowerCase();
                    if (label === target || label.indexOf(target) === 0) { els[i].click(); return true; }
                  }
                  return false;
                },
                clickTitle: function (prefix) {
                  var target = String(prefix).toLowerCase();
                  var els = Array.prototype.slice.call(document.querySelectorAll('button,[role="button"]'));
                  for (var i = 0; i < els.length; i++) {
                    var title = (els[i].getAttribute('title') || '').toLowerCase();
                    if (title.indexOf(target) === 0) { els[i].click(); return true; }
                  }
                  return false;
                }
              };
              var reportTheme = function () {
                try {
                  AndroidNative.theme(document.documentElement.getAttribute('data-theme') || 'dark');
                } catch (e) { /* bridge missing */ }
              };
              reportTheme();
              new MutationObserver(reportTheme).observe(document.documentElement, {
                attributes: true,
                attributeFilter: ['data-theme']
              });
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    private fun js(code: String) = webView.evaluateJavascript(code, null)

    private fun jsWithFeedback(code: String, failureMessage: String) {
        webView.evaluateJavascript(code) { result ->
            if (result != "true") {
                Toast.makeText(this, failureMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendKey(keyCode: Int, alt: Boolean) {
        val meta = if (alt) KeyEvent.META_ALT_ON else 0
        webView.requestFocus()
        webView.dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyCode, 0, meta))
        webView.dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, keyCode, 0, meta))
    }

    private fun openExternal(url: String) {
        if (!UrlPolicy.isExternal(url)) return
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    private fun shareCurrent() {
        js(
            "(function(){var fallback='https://kernel-hunter.github.io/kandle/';" +
                "function done(u){AndroidNative.share(u||fallback);}" +
                "try{if(window.kandle&&window.kandle.share){Promise.resolve(window.kandle.share()).then(done)" +
                ".catch(function(){done(fallback);});}else{done(fallback);}}catch(e){done(fallback);}})()",
        )
    }

    private fun recreateWebView() {
        (webView.parent as? android.view.ViewGroup)?.removeView(webView)
        webView.stopLoading()
        webView.destroy()
        webView = WebView(this)
        refresh.addView(
            webView,
            android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        setupWebView()
    }

    private fun handleTab(itemId: Int) {
        when (itemId) {
            R.id.tab_chart -> {
                sendKey(KeyEvent.KEYCODE_ESCAPE, alt = false)
                js("window.scrollTo(0, 0)")
            }
            R.id.tab_watchlist -> jsWithFeedback(
                "(function(){try{if(!window.kandle||!window.kandle.watchlist)return false;" +
                    "window.kandle.watchlist.toggle(true);return true;}catch(e){return false;}})()",
                "Watchlist is not ready yet",
            )
            R.id.tab_scripts -> jsWithFeedback(
                "(function(){var b=document.querySelector('button[title^=\"Script editor\"]');" +
                    "if(!b)return false;b.click();return true;})()",
                "Script editor is not available",
            )
            R.id.tab_explore -> js("document.querySelector('.symbol-btn') && document.querySelector('.symbol-btn').click()")
            R.id.tab_more -> showMore()
        }
        val index = (0 until bottom.menu.size()).indexOfFirst { bottom.menu.getItem(it).itemId == itemId }
        moveTick(index)
    }

    private fun moveTick(index: Int) {
        if (index < 0) return
        navHost.post {
            val width = bottom.width
            if (width == 0) return@post
            val itemWidth = width / 5f
            val tickWidth = navTick.width.toFloat()
            val target = index * itemWidth + itemWidth / 2f - tickWidth / 2f
            navTick.translationX = target - navTick.left
        }
    }

    private fun applyTheme(theme: String) {
        val light = theme == "light"
        val bg = if (light) 0xFFFFFFFF.toInt() else 0xFF0F1115.toInt()
        val panel = if (light) 0xFFF7F8FA.toInt() else 0xFF151920.toInt()
        val border = if (light) 0xFFE1E4E9.toInt() else 0xFF252B34.toInt()
        val text = if (light) 0xFF69717C.toInt() else 0xFF8A929E.toInt()
        val accent = if (light) 0xFF1A6FE0.toInt() else 0xFF2F80ED.toInt()

        findViewById<View>(R.id.root).setBackgroundColor(bg)
        statusSpacer.setBackgroundColor(panel)
        navHost.setBackgroundColor(panel)
        bottom.setBackgroundColor(panel)
        webView.setBackgroundColor(bg)
        navTick.backgroundTintList = android.content.res.ColorStateList.valueOf(accent)
        refresh.setColorSchemeColors(accent, text)
        refresh.setProgressBackgroundColorSchemeColor(panel)

        val tint = android.content.res.ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(accent, text),
        )
        bottom.itemIconTintList = tint
        bottom.itemTextColor = tint
        window.statusBarColor = panel
        window.navigationBarColor = panel
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
    }

    private fun clearCache() {
        webView.clearCache(true)
        Toast.makeText(this, R.string.clear_cache_done, Toast.LENGTH_SHORT).show()
        webView.reload()
    }

    private fun showAbout() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about)
            .setMessage(R.string.about_body)
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private fun showMore() {
        val items = arrayOf(
            "Reload",
            "Share chart",
            "Open in browser",
            "Light / dark theme",
            "Full screen chart",
            "Clear cache",
            "About",
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.more_title)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> webView.reload()
                    1 -> shareCurrent()
                    2 -> openExternal("https://kernel-hunter.github.io/kandle/")
                    3 -> js("document.querySelector('button[title^=\"Toggle theme\"]')?.click()")
                    4 -> {
                        immersive = !immersive
                        updateChromeVisibility()
                    }
                    5 -> clearCache()
                    6 -> showAbout()
                }
            }
            .show()
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.error_title)
            .setMessage(if (message.isBlank()) getString(R.string.error_body) else message)
            .setCancelable(false)
            .setPositiveButton(R.string.retry) { _, _ -> recreateWebView() }
            .setNegativeButton(R.string.close) { _, _ -> finish() }
            .show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        moveTick(bottom.selectedItemId.let { id ->
            (0 until bottom.menu.size()).indexOfFirst { bottom.menu.getItem(it).itemId == id }
        })
        updateChromeVisibility()
    }

    private fun updateChromeVisibility() {
        val landscape = resources.configuration.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE
        bottom.visibility = if (immersive || landscape) View.GONE else View.VISIBLE
        navHost.visibility = if (immersive || landscape) View.GONE else View.VISIBLE
        statusSpacer.visibility = if (immersive) View.GONE else View.VISIBLE
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (immersive) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onDestroy() {
        fileCallback?.onReceiveValue(null)
        fileCallback = null
        webView.removeJavascriptInterface("AndroidNative")
        (webView.parent as? android.view.ViewGroup)?.removeView(webView)
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }

    inner class NativeBridge {
        @android.webkit.JavascriptInterface
        fun notify(title: String, body: String) {
            Notifications.show(this@MainActivity, title, body)
        }

        @android.webkit.JavascriptInterface
        fun canNotify(): Boolean = Notifications.enabled(this@MainActivity)

        @android.webkit.JavascriptInterface
        fun requestNotifyPermission() {
            runOnUiThread { requestNotificationPermission() }
        }

        @android.webkit.JavascriptInterface
        fun theme(theme: String) {
            runOnUiThread { applyTheme(theme) }
        }

        @android.webkit.JavascriptInterface
        fun share(url: String) {
            runOnUiThread {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                }
                runCatching {
                    startActivity(Intent.createChooser(intent, getString(R.string.share_chart)))
                }.onFailure {
                    android.util.Log.e("Kandle", "share failed", it)
                    Toast.makeText(this@MainActivity, "No app to share with", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
