package app.kestrel.browser

/** Which process we are in: the regular one, or (WebView build only) the isolated `:private` window process. The Gecko build keeps it false. */
object Proc {
    @JvmField var isPrivate = false
}
