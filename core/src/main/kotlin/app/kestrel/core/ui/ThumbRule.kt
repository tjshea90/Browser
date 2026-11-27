package app.kestrel.core.ui

import app.kestrel.core.media.PageIdentity

/**
 * TP1 (owner 2026-11-18: "One of my tabs has the wrong preview. It shows the tab thumbnail for a site i went to before. Make sure the tab previews are actually for the site in the tab."):
 * a tab's preview picture belongs to ONE page. It is taken for [PageIdentity.of] the tab's address at the moment of the request, and it may be kept only when, at the moment the picture arrives, the tab is still
 * on that same page, no new load has started since, and the page has finished loading (a page that is still loading may still show the PREVIOUS page's last frame under its new address).
 */
object ThumbRule {
    /** The page a preview is taken for (empty for the new-tab page, a blank page or anything that is not http(s): no preview is kept for it). */
    fun pageOf(url: String): String = PageIdentity.of(url)

    /** May the picture that arrived be kept for the tab? */
    fun accept(takenFor: String, nowUrl: String, loadStartedThen: Long, loadStartedNow: Long, loadingNow: Boolean): Boolean =
        takenFor.isNotEmpty() && !loadingNow && loadStartedThen == loadStartedNow && pageOf(nowUrl) == takenFor

    /** Does a stored preview (taken for [storedFor]) still show the page the tab is on? */
    fun stillShows(storedFor: String?, nowUrl: String): Boolean = !storedFor.isNullOrEmpty() && storedFor == pageOf(nowUrl)
}
