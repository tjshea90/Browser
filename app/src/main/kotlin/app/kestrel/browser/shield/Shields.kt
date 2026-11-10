package app.kestrel.browser.shield

import android.content.Context
import app.kestrel.browser.Io
import app.kestrel.browser.Proc
import app.kestrel.browser.data.Prefs
import app.kestrel.core.shield.ShieldSettings
import app.kestrel.core.shield.SiteShieldStore
import app.kestrel.core.shield.SiteShields
import app.kestrel.core.shield.effective
import java.io.File

/** Per-site Shields overrides (persisted) combined with the global defaults. Thread-safe: read from WebView IO threads. */
object Shields {
    private val lock = Any()
    private var store = SiteShieldStore()
    private var file: File? = null

    fun init(ctx: Context) {
        if (file != null) return
        val f = File(ctx.filesDir, "site_shields.txt")
        file = f
        Io.exec {
            if (f.isFile) {
                val loaded = SiteShieldStore.decode(f.readText())
                synchronized(lock) { store = loaded }
            }
        }
    }

    /** Effective settings for a page on [host] (null host = defaults). */
    fun effective(prefs: Prefs, host: String?): ShieldSettings {
        val g = prefs.globalShields()
        val s = if (host == null) null else synchronized(lock) { store[host] }
        return (s?.applyTo(g) ?: g).effective()
    }

    fun overrideFor(host: String): SiteShields? = synchronized(lock) { store[host] }

    fun setOverride(host: String, s: SiteShields) {
        val text: String
        synchronized(lock) { store.set(host, s); text = store.encode() }
        val f = file
        if (f == null || Proc.isPrivate) return // private window: changes live for the session only
        Io.exec {
            val tmp = File(f.parentFile, f.name + ".tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
        }
    }

    fun clearAll() {
        synchronized(lock) { store = SiteShieldStore() }
        file?.let { f -> Io.exec { f.delete() } }
    }
}
