package app.kestrel.browser

/** Facts about the engine build that shared UI text must get right. The WebView build keeps the defaults; the Gecko build sets them at startup. */
object Flavor {
    /** The engine encrypts DNS lookups itself (DNS over HTTPS), so Settings must not tell the user to configure Private DNS system-wide. */
    @JvmField var secureDnsBuiltIn = false
}
