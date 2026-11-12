package app.kestrel.browser.download

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import app.kestrel.core.download.DlState
import app.kestrel.core.download.Reason
import app.kestrel.core.download.Rec
import app.kestrel.core.download.Seg
import app.kestrel.core.download.Store
import org.json.JSONObject

/**
 * downloads.db: its own file (WAL, synchronous=NORMAL) so a download checkpoint never queues behind history writes. Offsets are written only after the
 * engine fsynced the data (research/DOWNLOADS.md 3.4). Private downloads never reach this class.
 */
class SqliteStore(ctx: Context) : SQLiteOpenHelper(ctx.applicationContext, "downloads.db", null, 3), Store {
    override fun onConfigure(db: SQLiteDatabase) {
        db.enableWriteAheadLogging()
        try { db.rawQuery("PRAGMA synchronous=NORMAL", null).close() } catch (e: Exception) { /* the default (FULL in WAL on some builds) is only slower */ }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE downloads(id INTEGER PRIMARY KEY, state INTEGER NOT NULL, reason INTEGER NOT NULL DEFAULT 0, detail INTEGER NOT NULL DEFAULT 0, prio INTEGER NOT NULL DEFAULT 0, " +
                "url TEXT NOT NULL, final_url TEXT, final_at INTEGER, page_url TEXT, req_headers TEXT, name TEXT NOT NULL, mime TEXT, dest_kind INTEGER NOT NULL DEFAULT 0, dest_uri TEXT, " +
                "total INTEGER NOT NULL DEFAULT -1, etag TEXT, last_mod TEXT, date_hdr TEXT, ranges INTEGER NOT NULL DEFAULT -1, fetcher INTEGER NOT NULL DEFAULT 0, risk INTEGER NOT NULL DEFAULT 0, " +
                "metered_ok INTEGER NOT NULL DEFAULT 0, done INTEGER NOT NULL DEFAULT 0, fails INTEGER NOT NULL DEFAULT 0, conns_peak INTEGER NOT NULL DEFAULT 0, " +
                "created INTEGER NOT NULL, updated INTEGER NOT NULL, last_progress INTEGER NOT NULL DEFAULT 0, finished INTEGER NOT NULL DEFAULT 0, name_fixed INTEGER NOT NULL DEFAULT 0, crashes INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL("CREATE TABLE segments(dl INTEGER NOT NULL, idx INTEGER NOT NULL, s_start INTEGER NOT NULL, s_end INTEGER NOT NULL, s_cursor INTEGER NOT NULL, PRIMARY KEY(dl, idx))")
        db.execSQL("CREATE INDEX dl_state ON downloads(state, prio)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("ALTER TABLE downloads ADD COLUMN name_fixed INTEGER NOT NULL DEFAULT 0") // version 2: a caller-chosen file name
        if (oldVersion < 3) db.execSQL("ALTER TABLE downloads ADD COLUMN crashes INTEGER NOT NULL DEFAULT 0") // version 3: restarts in a row that ended the process mid-download (auto-resume cap)
    }

    override fun loadAll(): List<Rec> {
        val db = readableDatabase
        val out = ArrayList<Rec>()
        db.rawQuery("SELECT id,state,reason,detail,prio,url,final_url,final_at,page_url,req_headers,name,mime,dest_kind,dest_uri,total,etag,last_mod,date_hdr,ranges,fetcher,risk,metered_ok,done,fails,conns_peak,created,updated,last_progress,finished,name_fixed FROM downloads ORDER BY id", null).use { c ->
            while (c.moveToNext()) {
                val r = Rec(c.getLong(0))
                r.state = DlState.of(c.getInt(1)); r.reason = Reason.of(c.getInt(2)); r.detail = c.getInt(3); r.prio = c.getInt(4)
                r.url = c.getString(5); r.finalUrl = c.getString(6); r.finalAt = c.getLong(7); r.pageUrl = c.getString(8)
                r.headers = parseHeaders(c.getString(9)); r.name = c.getString(10); r.mime = c.getString(11); r.destKind = c.getInt(12); r.destUri = c.getString(13)
                r.total = c.getLong(14); r.etag = c.getString(15); r.lastMod = c.getString(16); r.dateHdr = c.getString(17); r.ranges = c.getInt(18); r.fetcher = c.getInt(19)
                r.risk = c.getInt(20); r.meteredOk = c.getInt(21) != 0; r.done = c.getLong(22); r.fails = c.getInt(23); r.connsPeak = c.getInt(24)
                r.created = c.getLong(25); r.updated = c.getLong(26); r.lastProgress = c.getLong(27); r.finished = c.getLong(28); r.nameFixed = c.getInt(29) != 0
                out.add(r)
            }
        }
        for (r in out) db.rawQuery("SELECT idx,s_start,s_end,s_cursor FROM segments WHERE dl=? ORDER BY idx", arrayOf(r.id.toString())).use { c ->
            while (c.moveToNext()) r.segs.add(Seg(c.getInt(0), c.getLong(1), c.getLong(2), c.getLong(3)))
        }
        return out
    }

    override fun insert(r: Rec) = save(r)

    override fun save(r: Rec) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val v = ContentValues()
            v.put("id", r.id); v.put("state", r.state.code); v.put("reason", r.reason.code); v.put("detail", r.detail); v.put("prio", r.prio)
            v.put("url", r.url); v.put("final_url", r.finalUrl); v.put("final_at", r.finalAt); v.put("page_url", r.pageUrl)
            v.put("req_headers", if (r.headers.isEmpty()) null else JSONObject(r.headers as Map<*, *>).toString())
            v.put("name", r.name); v.put("mime", r.mime); v.put("dest_kind", r.destKind); v.put("dest_uri", r.destUri)
            v.put("total", r.total); v.put("etag", r.etag); v.put("last_mod", r.lastMod); v.put("date_hdr", r.dateHdr); v.put("ranges", r.ranges); v.put("fetcher", r.fetcher)
            v.put("risk", r.risk); v.put("metered_ok", if (r.meteredOk) 1 else 0); v.put("done", r.done); v.put("fails", r.fails); v.put("conns_peak", r.connsPeak)
            v.put("created", r.created); v.put("updated", r.updated); v.put("last_progress", r.lastProgress); v.put("finished", r.finished); v.put("name_fixed", if (r.nameFixed) 1 else 0)
            db.insertWithOnConflict("downloads", null, v, SQLiteDatabase.CONFLICT_REPLACE)
            db.delete("segments", "dl=?", arrayOf(r.id.toString()))
            for (s in r.segs) {
                val sv = ContentValues()
                sv.put("dl", r.id); sv.put("idx", s.idx); sv.put("s_start", s.start); sv.put("s_end", s.end); sv.put("s_cursor", s.cursor)
                db.insert("segments", null, sv)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    override fun delete(id: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try { db.delete("segments", "dl=?", arrayOf(id.toString())); db.delete("downloads", "id=?", arrayOf(id.toString())); db.setTransactionSuccessful() } finally { db.endTransaction() }
    }

    private fun parseHeaders(s: String?): Map<String, String> {
        if (s.isNullOrEmpty()) return emptyMap()
        return try { val j = JSONObject(s); val m = HashMap<String, String>(); for (k in j.keys()) m[k] = j.getString(k); m } catch (e: Exception) { emptyMap() }
    }
}
