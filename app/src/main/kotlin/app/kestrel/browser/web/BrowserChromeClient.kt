package app.kestrel.browser.web

import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

/** Title/icon/progress, windows, dialogs, permissions, file chooser and fullscreen video for one tab. */
class BrowserChromeClient(private val host: TabHost, private val tab: Tab) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        tab.progress = newProgress
        if (newProgress < 100) tab.loading = true
        host.tabChanged(tab)
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        tab.title = title ?: ""
        host.tabChanged(tab)
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap?) {
        tab.favicon = icon?.let { if (it.width > 48) Bitmap.createScaledBitmap(it, 32, 32, true) else it }
        host.tabChanged(tab)
    }

    override fun getDefaultVideoPoster(): Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    // ---- windows (popups). Pages may only open windows from a user gesture; $popup list rules are applied to the first navigation.
    override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
        if (!isUserGesture) { host.popupBlocked(tab); return false }
        val child = host.newPopupTab(tab)
        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        transport.webView = child.webView
        resultMsg.sendToTarget()
        return true
    }

    override fun onCloseWindow(window: WebView) { host.closeTab(tab) }

    // ---- JavaScript dialogs: our own, with origin shown and spam protection
    override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean = host.jsAlert(tab, url, message, result)
    override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean = host.jsConfirm(tab, url, message, result)
    override fun onJsPrompt(view: WebView, url: String, message: String, defaultValue: String?, result: JsPromptResult): Boolean =
        host.jsPrompt(tab, url, message, defaultValue, result)

    /** "Leave site?" prompts are a favourite of scareware; leaving is always allowed. */
    override fun onJsBeforeUnload(view: WebView, url: String, message: String, result: JsResult): Boolean { result.confirm(); return true }

    // ---- permissions: always ask, never remember across sessions
    override fun onPermissionRequest(request: PermissionRequest) = host.webPermission(tab, request)
    override fun onPermissionRequestCanceled(request: PermissionRequest) = host.webPermissionCanceled(request)
    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) = host.geolocation(tab, origin, callback)

    // ---- files and fullscreen
    override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean =
        host.chooseFiles(filePathCallback, params)

    override fun onShowCustomView(view: View, callback: CustomViewCallback) = host.enterFullscreen(view, callback)
    override fun onHideCustomView() = host.exitFullscreen()
}
