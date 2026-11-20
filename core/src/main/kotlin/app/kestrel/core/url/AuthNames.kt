package app.kestrel.core.url

/**
 * RVW2 / security S2: what a "sign in" dialog says about WHO is asking. The dialog used to be titled with the host of the page the tab was on: a site that answered a link or an image with `401` and a realm like
 * "Session expired, enter your password" was shown as "Sign in to <the page you were reading>". The engine says which server asked (the address of the request): that host is the title, and when it is not the
 * page's host the text says so.
 */
object AuthNames {
    class Label(val host: String, val note: String)

    fun label(challengerUri: String?, pageUrl: String?): Label {
        val c = UrlParts.hostOf(challengerUri ?: "")?.lowercase()?.takeIf { it.isNotEmpty() }
        val p = UrlParts.hostOf(pageUrl ?: "")?.lowercase()?.takeIf { it.isNotEmpty() }
        val host = c ?: p ?: "this site"
        val note = if (c != null && p != null && c != p) "This sign-in is asked for by $c, not by the page you are on ($p). " else ""
        return Label(host, note)
    }
}
