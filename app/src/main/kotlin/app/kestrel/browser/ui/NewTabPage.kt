package app.kestrel.browser.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.kestrel.browser.R

class TopSite(val host: String, val url: String)

/** Native new-tab page: search box, top sites, and honest local stats. No news, no sponsored tiles, no WebView needed. */
class NewTabPage(ctx: Context, private val pal: Pal, private val l: Listener) : ScrollView(ctx) {
    interface Listener {
        fun onSearchTap()
        fun onOpen(url: String)
    }

    private val column = LinearLayout(ctx)
    private val tiles = LinearLayout(ctx)
    private val stats = LinearLayout(ctx)
    private val notice = LinearLayout(ctx)

    init {
        setBackgroundColor(pal.windowBg)
        isFillViewport = true
        column.orientation = LinearLayout.VERTICAL
        column.gravity = Gravity.CENTER_HORIZONTAL
        column.setPadding(ctx.dp(16), ctx.dp(40), ctx.dp(16), ctx.dp(24))
        addView(column, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val brand = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        brand.addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_shield); setColorFilter(if (pal.isPrivate) pal.fg else pal.accent)
            layoutParams = LinearLayout.LayoutParams(ctx.dp(36), ctx.dp(36))
        })
        brand.addView(label(ctx, if (pal.isPrivate) "  Private window" else "  Kestrel", 28f, pal.fg, bold = true))
        column.addView(brand)

        if (pal.isPrivate) {
            column.addView(label(ctx, "Pages you visit here leave nothing behind when you close this window: no history, no cookies, no cache, no tab list. Downloaded files and bookmarks are kept.", 14f, pal.muted).apply {
                gravity = Gravity.CENTER
                setPadding(ctx.dp(8), ctx.dp(16), ctx.dp(8), 0)
                includeFontPadding = true
            })
        }

        val search = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.dp(16), 0, ctx.dp(16), 0)
            background = ripple(pal.fg, roundedBg(pal.pill, ctx.dpf(26f)), ctx.dpf(26f))
            isClickable = true
            isFocusable = true
            contentDescription = "Search or type address"
            setOnClickListener { l.onSearchTap() }
        }
        search.addView(ImageView(ctx).apply { setImageResource(R.drawable.ic_search); setColorFilter(pal.muted); layoutParams = LinearLayout.LayoutParams(ctx.dp(22), ctx.dp(22)) })
        search.addView(label(ctx, "  Search or type address", 16f, pal.muted))
        column.addView(search, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ctx.dp(52)).apply { topMargin = ctx.dp(28); bottomMargin = ctx.dp(24) })

        tiles.orientation = LinearLayout.VERTICAL
        column.addView(tiles, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        stats.orientation = LinearLayout.VERTICAL
        stats.setPadding(ctx.dp(16), ctx.dp(14), ctx.dp(16), ctx.dp(14))
        stats.background = roundedBg(pal.card, ctx.dpf(14f), pal.divider, ctx.dp(1))
        column.addView(stats, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = ctx.dp(24) })

        notice.orientation = LinearLayout.VERTICAL
        column.addView(notice, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = ctx.dp(12) })
    }

    fun bindSites(sites: List<TopSite>) {
        val ctx = context
        tiles.removeAllViews()
        if (sites.isEmpty()) return
        val perRow = 4
        sites.chunked(perRow).forEach { rowSites ->
            val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            for (i in 0 until perRow) {
                val site = rowSites.getOrNull(i)
                row.addView(if (site != null) tile(site) else View(ctx), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }
            tiles.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun tile(site: TopSite): View {
        val ctx = context
        val t = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(ctx.dp(4), ctx.dp(8), ctx.dp(4), ctx.dp(8))
            background = ripple(pal.fg, null, ctx.dpf(12f))
            isClickable = true
            isFocusable = true
            contentDescription = site.host
            setOnClickListener { l.onOpen(site.url) }
        }
        t.addView(LetterAvatar(ctx, site.host, pal), LinearLayout.LayoutParams(ctx.dp(48), ctx.dp(48)))
        t.addView(label(ctx, site.host.removePrefix("www."), 12f, pal.fg).apply {
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END; gravity = Gravity.CENTER
            setPadding(0, ctx.dp(6), 0, 0); includeFontPadding = true
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        return t
    }

    fun bindStats(blockedTotal: Long, dataUsedBytes: Long, listStatus: String, metered: Boolean) {
        val ctx = context
        stats.removeAllViews()
        stats.addView(label(ctx, "${fmtCount(blockedTotal)} ads & trackers blocked", 15f, pal.fg, bold = true))
        if (dataUsedBytes >= 0) {
            stats.addView(label(ctx, "Data used by this browser: ${fmtBytes(dataUsedBytes)}", 13f, pal.muted).apply { setPadding(0, ctx.dp(6), 0, 0); includeFontPadding = true })
        }
        stats.addView(label(ctx, "Filters: $listStatus" + if (metered) "  ·  metered connection" else "", 13f, pal.muted).apply { setPadding(0, ctx.dp(6), 0, 0); includeFontPadding = true })
    }

    /** Warning cards (outdated WebView etc.). Each entry: title + body. */
    fun bindNotices(items: List<Pair<String, String>>) {
        val ctx = context
        notice.removeAllViews()
        for ((title, body) in items) {
            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(ctx.dp(16), ctx.dp(12), ctx.dp(16), ctx.dp(12))
                background = roundedBg(pal.card, ctx.dpf(14f), pal.warn, ctx.dp(1))
            }
            card.addView(label(ctx, title, 14f, pal.warn, bold = true))
            card.addView(label(ctx, body, 13f, pal.fg).apply { setPadding(0, ctx.dp(4), 0, 0); includeFontPadding = true })
            notice.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = ctx.dp(8) })
        }
    }

    companion object {
        fun fmtCount(n: Long): String = when {
            n < 1000 -> n.toString()
            n < 1_000_000 -> "%.1fk".format(n / 1000.0)
            else -> "%.1fM".format(n / 1_000_000.0)
        }

        fun fmtBytes(b: Long): String = when {
            b < 1024 * 1024 -> "${b / 1024} KB"
            b < 1024L * 1024 * 1024 -> "%.1f MB".format(b / 1048576.0)
            else -> "%.2f GB".format(b / 1073741824.0)
        }
    }
}

/** Round avatar with the site's first letter (no favicon downloads: saves data). */
class LetterAvatar(ctx: Context, host: String, private val pal: Pal) : View(ctx) {
    private val letter = host.removePrefix("www.").firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tone(host.hashCode()) }
    private val fg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF2F3F5.toInt(); textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    private val shape = android.graphics.Path()
    private val box = android.graphics.RectF()

    /** A chamfered tile in a deep, muted tone picked from the host name (the CERE identity: no bright circles). */
    override fun onDraw(c: Canvas) {
        box.set(0f, 0f, width.toFloat(), height.toFloat())
        ChamferDrawable.path(box, height * 0.28f, ChamferDrawable.TL or ChamferDrawable.BR, shape)
        c.drawPath(shape, bg)
        fg.textSize = height * 0.56f
        c.drawText(letter, width / 2f, height / 2f - (fg.descent() + fg.ascent()) / 2f, fg)
    }

    private fun tone(h: Int): Int {
        // cool, deep tones only (the REAPER look: black and blue); violet is left to the private skin
        val colors = intArrayOf(0xFF123F6B.toInt(), 0xFF0B5A6B.toInt(), 0xFF1F3F8C.toInt(), 0xFF174A5E.toInt(), 0xFF0F5B52.toInt(), 0xFF3F5160.toInt(), 0xFF1B4F8C.toInt(), 0xFF0E4D75.toInt())
        return colors[(h and 0x7fffffff) % colors.size]
    }
}
