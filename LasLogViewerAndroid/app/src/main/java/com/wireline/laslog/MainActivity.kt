package com.wireline.laslog

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.OutputStreamWriter

/**
 * Thin native shell around the offline LAS log viewer / formation evaluation
 * web app (app/src/main/assets/las_log_viewer.html). All parsing and
 * petrophysical calculations run inside the WebView in JavaScript, exactly
 * as they do in a desktop browser - this class only bridges two things a
 * WebView can't do on its own:
 *
 *  1. Letting <input type="file"> open Android's system file picker so the
 *     user can browse to a .las file anywhere on the device.
 *  2. Letting the "Export processed CSV" button save through Android's
 *     "Save As" dialog (Storage Access Framework) instead of relying on a
 *     browser download, which WebView does not handle for blob: URLs.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingExportContent: String? = null

    companion object {
        private const val FILE_CHOOSER_REQUEST_CODE = 51426
        private const val CREATE_DOC_REQUEST_CODE = 51427
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        webView.addJavascriptInterface(ExportBridge(), "AndroidExport")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                filePathCb: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = filePathCb

                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                return try {
                    startActivityForResult(
                        Intent.createChooser(intent, "Select LAS file"),
                        FILE_CHOOSER_REQUEST_CODE
                    )
                    true
                } catch (e: Exception) {
                    filePathCallback = null
                    Toast.makeText(this@MainActivity, "No file picker available on this device", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        }

        webView.loadUrl("file:///android_asset/las_log_viewer.html")
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            FILE_CHOOSER_REQUEST_CODE -> {
                val results: Array<Uri>? =
                    if (resultCode == Activity.RESULT_OK && data?.data != null) arrayOf(data.data!!) else null
                filePathCallback?.onReceiveValue(results)
                filePathCallback = null
            }
            CREATE_DOC_REQUEST_CODE -> {
                val content = pendingExportContent
                if (resultCode == Activity.RESULT_OK && data?.data != null && content != null) {
                    try {
                        contentResolver.openOutputStream(data.data!!)?.use { out ->
                            OutputStreamWriter(out).use { it.write(content) }
                        }
                        Toast.makeText(this, "CSV saved", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Could not save file: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                pendingExportContent = null
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    /** Exposed to the page's JS as window.AndroidExport */
    inner class ExportBridge {
        @JavascriptInterface
        fun exportCsv(filename: String, content: String) {
            pendingExportContent = content
            runOnUiThread {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/csv"
                    putExtra(Intent.EXTRA_TITLE, filename)
                }
                try {
                    startActivityForResult(intent, CREATE_DOC_REQUEST_CODE)
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "No file saver available on this device", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
