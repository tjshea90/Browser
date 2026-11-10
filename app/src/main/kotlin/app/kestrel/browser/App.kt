package app.kestrel.browser

import android.app.Application
import android.content.Context
import android.webkit.WebView
import app.kestrel.browser.shield.Engine

/** Which process we are in: the regular one, or the isolated `:private` window process. */
object Proc {
    @JvmField var isPrivate = false
}

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Proc.isPrivate = (getProcessName() ?: "").endsWith(":private")
        if (Proc.isPrivate) {
            // Must run before ANY WebView/CookieManager use in this process: gives the private window its own, separate
            // cookie jar / cache / storage directory, which is then wiped so nothing from a previous private session survives.
            WebView.setDataDirectorySuffix("private")
            wipePrivateData(this)
        }
        Engine.start(this) // background thread: loads the filter snapshot + Public Suffix List
    }

    companion object {
        /** Deletes every WebView directory belonging to the private suffix (names contain "webview" and "private"). */
        fun wipePrivateData(ctx: Context) {
            for (root in listOfNotNull(ctx.dataDir, ctx.cacheDir)) {
                root.listFiles()?.forEach { f ->
                    val n = f.name.lowercase()
                    if (n.contains("webview") && n.contains("private")) f.deleteRecursively()
                }
            }
        }
    }
}
