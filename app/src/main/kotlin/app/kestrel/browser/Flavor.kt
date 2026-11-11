package app.kestrel.browser

/** Facts about the engine build that shared UI text must get right. The WebView build keeps the defaults; the Gecko build sets them at startup. */
object Flavor {
    /** The engine encrypts DNS lookups itself (DNS over HTTPS), so Settings must not tell the user to configure Private DNS system-wide. */
    @JvmField var secureDnsBuiltIn = false

    /** Which GitHub Releases belong to THIS app: tags start with this ("kestrel-v" = Kestrel on the WebView, "kestrel-g-v" = Kestrel G). Used by the in-app updater. */
    @JvmField var updateTagPrefix = "kestrel-v"

    /** The app's name as shown in the updater ("Kestrel" / "Kestrel G"). */
    @JvmField var title = "Kestrel"

    /** The engine has the "Scroll feel" prefs (Gecko build only): Settings shows the row. */
    @JvmField var hasScrollFeel = false
}
