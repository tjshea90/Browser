package app.kestrel.browser

/**
 * The private window. It runs in its own process (`:private`, see the manifest) so it has a separate WebView data directory:
 * cookies, cache and storage never mix with the regular window and are wiped when the process starts. [Proc.isPrivate] switches off
 * history, bookmarks, the tab session and search suggestions, turns on FLAG_SECURE (no screenshots / recents thumbnail) and tells the
 * keyboard not to learn from what is typed.
 */
class PrivateActivity : BrowserActivity()
