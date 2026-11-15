package com.erdinc1911.karar

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Base64
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.io.InputStream

class MainActivity : ComponentActivity() {
    companion object { const val ADRES = "https://appassets.androidplatform.net/assets/www/index.html" }
    private lateinit var web: WebView
    private lateinit var loader: WebViewAssetLoader
    private var dosyaGeri: ValueCallback<Array<Uri>>? = null
    private var izinIstegi: PermissionRequest? = null
    private var tamEkran: View? = null

    private val dosyaSec = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val cb = dosyaGeri; dosyaGeri = null
        if (cb == null) return@registerForActivityResult
        val d = r.data
        val list = mutableListOf<Uri>()
        if (r.resultCode == RESULT_OK && d != null) {
            d.clipData?.let { c -> for (i in 0 until c.itemCount) list.add(c.getItemAt(i).uri) }
            if (list.isEmpty()) d.data?.let { list.add(it) }
        }
        cb.onReceiveValue(list.toTypedArray())
    }
    private val mikIzni = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        val r = izinIstegi; izinIstegi = null
        if (r != null) { if (ok) r.grant(r.resources) else r.deny() }
    }
    private val bildirimIzni = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Test sürümlerinde uygulama içi sayfa bilgisayardan incelenebilir (otomatik testler için)
        try { if (packageManager.getPackageInfo(packageName, 0).versionName?.startsWith("test") == true) WebView.setWebContentsDebuggingEnabled(true) } catch (_: Exception) {}
        loader = WebViewAssetLoader.Builder().addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        web = WebView(this)
        // Uygulama sistem çubuklarının (saat/pil çubuğu, gezinme çubuğu, klavye) altında kalmasın: Android 15'te kenardan kenara zorunlu
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val kok = FrameLayout(this).apply { setBackgroundColor(0xFFEAEEE8.toInt()); addView(web) }
        setContentView(kok)
        ViewCompat.setOnApplyWindowInsetsListener(kok) { v, ins ->
            val b = ins.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            v.setPadding(b.left, b.top, b.right, b.bottom); WindowInsetsCompat.CONSUMED
        }
        WindowInsetsControllerCompat(window, window.decorView).apply { isAppearanceLightStatusBars = true; isAppearanceLightNavigationBars = true }
        web.overScrollMode = View.OVER_SCROLL_NEVER
        // Sayfayı çalıştıran WebView işlemi, uygulama görünmezken (ekran kapalı, başka uygulama) önemsiz sayılıp dondurulmasın:
        // önceliği uygulamanın kendi önceliğine bağlı kalır; ayrıştırma hizmeti çalışırken uygulama "ön planda" sayılır.
        web.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
        web.isVerticalScrollBarEnabled = false; web.isHorizontalScrollBarEnabled = false
        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            textZoom = 100
            setSupportZoom(false); builtInZoomControls = false; displayZoomControls = false   // uygulama gibi: iki parmakla büyütme yok
        }
        web.addJavascriptInterface(Kopru(), "KararNative")
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(v: WebView, req: WebResourceRequest): WebResourceResponse? =
                yerelDosya(req.url.toString()) ?: loader.shouldInterceptRequest(req.url)
            override fun shouldOverrideUrlLoading(v: WebView, req: WebResourceRequest): Boolean {
                val u = req.url
                if (u.host == "appassets.androidplatform.net") return false
                try { startActivity(Intent(Intent.ACTION_VIEW, u)) } catch (_: Exception) {}
                return true   // dış bağlantılar tarayıcıda açılır
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(m: ConsoleMessage): Boolean { android.util.Log.i("KararWeb", "${m.messageLevel()} ${m.sourceId()?.substringAfterLast('/')}:${m.lineNumber()} ${m.message()}"); return true }
            override fun onShowFileChooser(v: WebView, cb: ValueCallback<Array<Uri>>, p: FileChooserParams): Boolean {
                dosyaGeri?.onReceiveValue(null); dosyaGeri = cb
                val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    val turler = p.acceptTypes.flatMap { it.split(",") }.map { it.trim() }.filter { it.contains("/") }
                    if (turler.isNotEmpty() && p.acceptTypes.none { it.contains(".karar") || it.contains(".lrc") || it.contains(".json") }) putExtra(Intent.EXTRA_MIME_TYPES, turler.toTypedArray())
                    if (p.mode == FileChooserParams.MODE_OPEN_MULTIPLE) putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                return try { dosyaSec.launch(i); true } catch (e: Exception) { dosyaGeri = null; false }
            }
            override fun onPermissionRequest(r: PermissionRequest) {
                runOnUiThread {
                    if (r.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) r.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                        else { izinIstegi = r; mikIzni.launch(Manifest.permission.RECORD_AUDIO) }
                    } else r.deny()
                }
            }
            override fun onShowCustomView(view: View, cb: CustomViewCallback) {
                tamEkran = view; (window.decorView as FrameLayout).addView(view, FrameLayout.LayoutParams(-1, -1))
            }
            override fun onHideCustomView() { tamEkran?.let { (window.decorView as FrameLayout).removeView(it) }; tamEkran = null }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("(window.__kararBack&&window.__kararBack())?'1':'0'") { r ->
                    if (!r.contains("1")) moveTaskToBack(true)   // uygulamayı kapatma: arka planda ayrıştırma sürsün
                }
            }
        })
        val geri = savedInstanceState?.let { web.restoreState(it) }
        if (geri == null || web.url == null) web.loadUrl(ADRES)
    }

    override fun onPause() { super.onPause(); web.resumeTimers() }
    override fun onSaveInstanceState(out: Bundle) { super.onSaveInstanceState(out); web.saveState(out) }
    override fun onDestroy() { if (!AyristirmaHizmeti.calisiyor) web.destroy(); super.onDestroy() }

    // İnternetteki kütüphane ve model adresleri uygulamanın içindeki kopyalarla karşılanır (internetsiz çalışır)
    private fun yerelDosya(url: String): WebResourceResponse? {
        val yol = when {
            url.startsWith("https://cdn.jsdelivr.net/npm/onnxruntime-web@1.20.1/dist/") -> "cdn/ort-1.20.1/" + url.substringAfterLast('/').substringBefore('?')
            url.startsWith("https://cdn.jsdelivr.net/npm/onnxruntime-web@1.22.0/dist/") -> "cdn/ort-1.22.0/" + url.substringAfterLast('/').substringBefore('?')
            url.startsWith("https://cdn.jsdelivr.net/npm/lamejs@1.2.1/") -> "cdn/lamejs/lame.min.js"
            url.contains("UVR-MDX-NET-Voc_FT.onnx") -> "models/UVR-MDX-NET-Voc_FT.onnx"
            url.startsWith("https://fonts.googleapis.com/css2") -> "fonts/fonts.css"
            url.startsWith("https://fonts.gstatic.com/") -> "fonts/" + url.removePrefix("https://fonts.gstatic.com/").replace('/', '_')
            else -> return null
        }
        val inp: InputStream = try { assets.open(yol) } catch (_: Exception) { return null }
        val tur = when { yol.endsWith(".wasm") -> "application/wasm"; yol.endsWith(".onnx") -> "application/octet-stream"; yol.endsWith(".css") -> "text/css"; yol.contains("fonts/") -> "font/woff2"; else -> "text/javascript" }
        val boy = try { assets.openFd(yol).use { it.length } } catch (_: Exception) { -1L }
        val h = mutableMapOf("Access-Control-Allow-Origin" to "*", "Cache-Control" to "max-age=31536000")
        if (boy > 0) h["Content-Length"] = boy.toString()
        return WebResourceResponse(tur, null, 200, "OK", h, inp)
    }

    inner class Kopru {
        // ekran kapalıyken de sürsün: ön plan hizmeti + işlemciyi uyanık tutma
        @JavascriptInterface fun keepAwake(on: Boolean) { runOnUiThread { if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) } }
        @JavascriptInterface fun foreground(on: Boolean, text: String) {
            runOnUiThread {
                if (on && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) bildirimIzni.launch(Manifest.permission.POST_NOTIFICATIONS)
                try { if (on) AyristirmaHizmeti.baslat(this@MainActivity, text) else AyristirmaHizmeti.durdur() } catch (_: Exception) {}
            }
        }
        @JavascriptInterface fun battery(): String {
            val b = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val lv = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val sc = b?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val st = b?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val ch = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
            return "{\"level\":${if (lv >= 0) lv.toDouble() / sc else 1.0},\"charging\":$ch}"
        }
        @JavascriptInterface fun saveFile(name: String, mime: String, b64: String): String {
            return try {
                val data = Base64.decode(b64, Base64.DEFAULT)
                if (Build.VERSION.SDK_INT >= 29) {
                    val v = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, name); put(MediaStore.Downloads.MIME_TYPE, mime)
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Karar")
                    }
                    val u = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v) ?: throw Exception("yer açılamadı")
                    contentResolver.openOutputStream(u)!!.use { it.write(data) }
                } else {
                    val d = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Karar").apply { mkdirs() }
                    File(d, name).writeBytes(data)
                }
                runOnUiThread { Toast.makeText(this@MainActivity, "İndirilenler › Karar klasörüne kaydedildi: $name", Toast.LENGTH_LONG).show() }
                "“$name” İndirilenler › Karar klasörüne kaydedildi"
            } catch (e: Exception) { "Kaydedilemedi: ${e.message}" }
        }
        @JavascriptInterface fun openBatterySettings() {
            runOnUiThread { try { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } catch (_: Exception) {} }
        }
        @JavascriptInterface fun isApp(): Boolean = true
        @JavascriptInterface fun openAppSettings() {
            runOnUiThread { try { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) } catch (_: Exception) {} }
        }
    }
}
