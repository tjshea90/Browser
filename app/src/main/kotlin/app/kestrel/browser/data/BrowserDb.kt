package app.kestrel.browser.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import app.kestrel.core.url.UrlParts

class Entry(val url: String, val title: String, val visits: Int = 0, val last: Long = 0)

/**
 * History + bookmarks in the framework SQLite (no Room). ALL methods block on disk: call them from the IO thread only.
 * History is capped (rows and age) so it can never grow unbounded on a small device.
 */
class BrowserDb(ctx: Context) : SQLiteOpenHelper(ctx.applicationContext, "browser.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE history(url TEXT PRIMARY KEY, title TEXT NOT NULL DEFAULT '', host TEXT NOT NULL DEFAULT '', visits INTEGER NOT NULL DEFAULT 1, last INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX history_last ON history(last DESC)")
        db.execSQL("CREATE INDEX history_host ON history(host)")
        db.execSQL("CREATE TABLE bookmarks(url TEXT PRIMARY KEY, title TEXT NOT NULL DEFAULT '', added INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    // ---- history

    fun recordVisit(url: String, title: String) {
        if (!(url.startsWith("http://") || url.startsWith("https://")) || url.length > 2048) return
        val host = UrlParts.hostOf(url) ?: ""
        val now = System.currentTimeMillis()
        val db = writableDatabase
        val cv = ContentValues().apply { put("url", url); put("title", title.take(200)); put("host", host); put("visits", 1); put("last", now) }
        if (db.insertWithOnConflict("history", null, cv, SQLiteDatabase.CONFLICT_IGNORE) == -1L) {
            db.execSQL(
                "UPDATE history SET visits = visits + 1, last = ?, title = CASE WHEN ? != '' THEN ? ELSE title END WHERE url = ?",
                arrayOf<Any>(now, title.take(200), title.take(200), url),
            )
        }
    }

    fun history(limit: Int): List<Entry> = query("SELECT url, title, visits, last FROM history ORDER BY last DESC LIMIT ?", limit.toString())

    fun deleteHistory(url: String) { writableDatabase.delete("history", "url = ?", arrayOf(url)) }
    fun clearHistory() { writableDatabase.delete("history", null, null) }

    /** Keep at most [maxRows] rows and nothing older than [maxAgeDays]. */
    fun purge(maxRows: Int, maxAgeDays: Int) {
        val db = writableDatabase
        db.delete("history", "last < ?", arrayOf((System.currentTimeMillis() - maxAgeDays * 86_400_000L).toString()))
        db.execSQL("DELETE FROM history WHERE url NOT IN (SELECT url FROM history ORDER BY last DESC LIMIT ?)", arrayOf<Any>(maxRows))
    }

    /** Most-visited sites as (host, https://host/). */
    fun topSites(limit: Int): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        readableDatabase.rawQuery(
            "SELECT host, SUM(visits) v FROM history WHERE host != '' GROUP BY host ORDER BY v DESC, MAX(last) DESC LIMIT ?", arrayOf(limit.toString()),
        ).use { c -> while (c.moveToNext()) out.add(c.getString(0) to "https://${c.getString(0)}/") }
        return out
    }

    /** Omnibox suggestions from bookmarks first, then history. */
    fun suggest(q: String, limit: Int): List<Entry> {
        val like = "%" + q.replace("%", "").replace("_", "") + "%"
        val out = ArrayList<Entry>()
        out += query("SELECT url, title, 0, added FROM bookmarks WHERE url LIKE ? OR title LIKE ? ORDER BY added DESC LIMIT ?", like, like, limit.toString())
        if (out.size < limit) {
            out += query("SELECT url, title, visits, last FROM history WHERE url LIKE ? OR title LIKE ? ORDER BY visits DESC, last DESC LIMIT ?", like, like, (limit - out.size).toString())
        }
        return out.distinctBy { it.url }
    }

    // ---- bookmarks

    fun bookmarks(): List<Entry> = query("SELECT url, title, 0, added FROM bookmarks ORDER BY added DESC")
    fun isBookmarked(url: String): Boolean = readableDatabase.rawQuery("SELECT 1 FROM bookmarks WHERE url = ?", arrayOf(url)).use { it.moveToFirst() }

    fun addBookmark(url: String, title: String) {
        val cv = ContentValues().apply { put("url", url); put("title", title.take(200)); put("added", System.currentTimeMillis()) }
        writableDatabase.insertWithOnConflict("bookmarks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removeBookmark(url: String) { writableDatabase.delete("bookmarks", "url = ?", arrayOf(url)) }

    private fun query(sql: String, vararg args: String): List<Entry> {
        val out = ArrayList<Entry>()
        readableDatabase.rawQuery(sql, args).use { c -> while (c.moveToNext()) out.add(Entry(c.getString(0), c.getString(1) ?: "", c.getInt(2), c.getLong(3))) }
        return out
    }
}
