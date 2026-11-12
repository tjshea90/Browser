package app.kestrel.browser.ui

import android.graphics.Bitmap

/**
 * Recent pictures of pages a tab has LEFT, kept in memory only (never written to disk, private tabs included), so that Back / Forward can show the page the user is going to the
 * moment it is pressed when the engine has to load that page again (a page that forbids caching such as webmail, or one the engine's back/forward cache no longer holds).
 * Most Back presses are answered by the engine's own back/forward cache and never use a picture.
 *
 * At most [MAX] pictures (480 px wide, RGB_565, about 750 KB each), least recently used out first; a tab's pictures go when the tab closes or is hibernated, and all of them when the
 * system asks for memory. Keyed by tab and address (without the #fragment).
 */
object BackPictures {
    const val MAX = 4

    private class Key(val tab: Long, val url: String) {
        override fun equals(other: Any?) = other is Key && other.tab == tab && other.url == url
        override fun hashCode() = (tab xor (tab ushr 32)).toInt() * 31 + url.hashCode()
    }

    private val map = object : LinkedHashMap<Key, Bitmap>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, Bitmap>?): Boolean = size > MAX
    }

    fun keyOf(url: String): String = url.substringBefore('#')

    @Synchronized fun put(tab: Long, url: String, picture: Bitmap) { map[Key(tab, keyOf(url))] = picture }

    @Synchronized fun get(tab: Long, url: String): Bitmap? = map[Key(tab, keyOf(url))]?.takeIf { !it.isRecycled }

    @Synchronized fun dropTab(tab: Long) { val it = map.keys.iterator(); while (it.hasNext()) if (it.next().tab == tab) it.remove() }

    @Synchronized fun clear() { map.clear() }

    @Synchronized fun size(): Int = map.size
}
