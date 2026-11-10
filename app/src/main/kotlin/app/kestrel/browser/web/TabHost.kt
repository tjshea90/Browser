package app.kestrel.browser.web

import android.app.Activity
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.HttpAuthHandler
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

/** Everything the per-tab WebView clients need from the browser UI (implemented by BrowserActivity). */
interface TabHost {
    val activity: Activity

    fun tabChanged(tab: Tab)
    fun shieldCountChanged(tab: Tab)
    fun errorChanged(tab: Tab)
    fun pageFinished(tab: Tab)
    fun popupBlocked(tab: Tab)
    fun toast(message: String)

    /** Create a tab (+WebView) for a page-opened window and select it. */
    fun newPopupTab(opener: Tab): Tab
    fun closeTab(tab: Tab)
    fun rendererGone(tab: Tab, view: WebView, crashed: Boolean)

    fun enterFullscreen(view: View, cb: WebChromeClient.CustomViewCallback)
    fun exitFullscreen()
    fun chooseFiles(cb: ValueCallback<Array<Uri>>, params: WebChromeClient.FileChooserParams): Boolean

    fun webPermission(tab: Tab, request: PermissionRequest)
    fun webPermissionCanceled(request: PermissionRequest)
    fun geolocation(tab: Tab, origin: String, cb: GeolocationPermissions.Callback)

    fun jsAlert(tab: Tab, url: String, message: String, r: JsResult): Boolean
    fun jsConfirm(tab: Tab, url: String, message: String, r: JsResult): Boolean
    fun jsPrompt(tab: Tab, url: String, message: String, default: String?, r: JsPromptResult): Boolean
    fun httpAuth(tab: Tab, handler: HttpAuthHandler, host: String, realm: String)
    fun confirmResubmit(resend: Message, dontResend: Message)

    fun download(tab: Tab, url: String, userAgent: String?, contentDisposition: String?, mime: String?, length: Long)

    /** Non-web URL (intent:, mailto:, custom scheme). Returns true when handled/blocked (never loaded into the WebView). */
    fun externalLaunch(tab: Tab, url: String): Boolean
    fun hitTestLongPress(tab: Tab, view: WebView): Boolean
}
