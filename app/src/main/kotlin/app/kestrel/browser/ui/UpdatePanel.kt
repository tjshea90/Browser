package app.kestrel.browser.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import app.kestrel.browser.Flavor
import app.kestrel.browser.Io
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.update.UpdateManager
import app.kestrel.browser.update.UpdateManager.Phase
import app.kestrel.core.update.UpdateLogic

/**
 * The "Updates" sheet: what the updater knows, and the one button that makes sense next (Check now / Download / Pause / Install). It redraws itself whenever
 * [UpdateManager] changes (download progress included) and stops listening when the sheet closes.
 */
class UpdatePanel(ctx: Context, private val pal: Pal, private val prefs: Prefs, private val actions: Actions) : LinearLayout(ctx) {
    interface Actions {
        fun openPage(url: String)
        /** The user tapped Download: the activity asks first on a metered link (a hotspot), then calls [UpdateManager.download]. */
        fun download()
        fun openInstallSettings()
        /** Android's Storage screen, to free room for the installer. */
        fun openStorageSettings()
        /** Clear the browser's own cache (network and image caches, page previews); [done] gets the bytes that went, on the main thread. */
        fun clearCache(done: (Long) -> Unit)
        /** The Downloads screen, to delete big files that were already watched. */
        fun openDownloads()
    }

    private val listener: () -> Unit = { render() }
    private var drawnPhase: Phase? = null
    private var bar: Bar? = null
    private var progressText: TextView? = null
    private var otherWay = false
    private var saving = false
    private var otherWayError: String? = null

    init {
        orientation = VERTICAL
        setPadding(ctx.dp(20), ctx.dp(4), ctx.dp(20), ctx.dp(18))
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); UpdateManager.addListener(listener); render(); UpdateManager.recheckSpace(context); UpdateManager.hintSpace(context) } // a stale "not enough space" goes away as soon as there is enough
    override fun onDetachedFromWindow() { UpdateManager.removeListener(listener); super.onDetachedFromWindow() }

    /** Tests only: draw the sheet for the current [UpdateManager.state] without attaching it to a window (attaching also starts a storage re-check). */
    internal fun renderForTest() { render() }

    private fun render() {
        val s = UpdateManager.state
        if (s.phase == Phase.DOWNLOADING && drawnPhase == Phase.DOWNLOADING && bar != null) { // a progress tick: move the bar, keep everything else
            bar!!.fraction = if (s.total > 0) s.done.toFloat() / s.total else 0f
            progressText?.text = progressLine(s)
            return
        }
        drawnPhase = s.phase
        removeAllViews()
        bar = null; progressText = null
        val ctx = context
        val o = s.offer
        val name = Flavor.title
        val installed = UpdateManager.installedName(ctx)

        fun headline(t: String, color: Int = pal.fg) = addView(label(ctx, t, 19f, color, bold = true).apply { includeFontPadding = true; setPadding(0, ctx.dp(8), 0, ctx.dp(4)) })
        fun sub(t: String, color: Int = pal.muted) = addView(label(ctx, t, 14f, color).apply { includeFontPadding = true; setPadding(0, 0, 0, ctx.dp(8)) })
        fun buttons(vararg b: TextView) {
            val row = LinearLayout(ctx).apply { orientation = HORIZONTAL; setPadding(0, ctx.dp(10), 0, ctx.dp(4)) }
            for ((i, v) in b.withIndex()) row.addView(v, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { if (i > 0) marginStart = ctx.dp(10) })
            addView(row)
        }
        fun primary(t: String, f: () -> Unit) = pillButton(ctx, t, pal.accent, pal.accentOn) { f() }
        fun secondary(t: String, f: () -> Unit) = outlineButton(ctx, t, pal.pill, pal.divider, pal.fg) { f() }

        /** The heads-up above Download / Install now when the tablet has too little room for Android's installer (a hint only: the install is still tried). */
        fun roomHint() {
            val h = UpdateManager.spaceHint?.takeIf { it.tag == o?.tag && it.shortBy > 0 } ?: return
            val f = UpdateLogic::formatSize
            sub("Heads-up: this tablet has ${f(h.free)} free and Android wants about ${f(h.need)} free to install this update (Android keeps ${if (h.reserve > 0) f(h.reserve) else "part of the storage"} free, plus room for the update itself): about ${f(h.shortBy)} more. " +
                "Without it the installer refuses the file after it is downloaded.", pal.warn)
            if (h.cacheBytes >= (10L shl 20)) buttons(secondary("Clear cache (${f(h.cacheBytes)})") {
                actions.clearCache { freed ->
                    android.widget.Toast.makeText(ctx, "Cleared ${f(freed)}", android.widget.Toast.LENGTH_SHORT).show()
                    Io.main.postDelayed({ UpdateManager.hintSpace(ctx) }, 1200)
                    Io.main.postDelayed({ UpdateManager.hintSpace(ctx) }, 3500)
                }
            })
        }

        when (s.phase) {
            Phase.IDLE, Phase.UP_TO_DATE -> {
                headline(if (s.checkedAt > 0) "$name is up to date" else "Check for a new version")
                sub("You have version $installed." + if (s.checkedAt > 0) " Checked ${ago(s.checkedAt)}." else "")
                buttons(primary("Check now") { UpdateManager.check(ctx, manual = true) })
            }
            Phase.CHECKING -> { headline("Checking GitHub…"); sub("A small request to GitHub Releases; no page you visit is involved.") }
            Phase.AVAILABLE -> {
                headline("Update available: ${o?.versionName ?: ""}")
                sub("You have $installed. " + (o?.let { UpdateLogic.formatSize(it.size) + " from GitHub Releases." } ?: ""))
                notes(o?.notes)
                roomHint()
                val partial = s.done > 0 && o != null && s.done < o.size
                buttons(
                    primary(if (partial) "Continue download" else "Download " + (o?.let { UpdateLogic.formatSize(it.size) } ?: "")) { actions.download() },
                    secondary("Release page") { o?.pageUrl?.takeIf { it.isNotEmpty() }?.let { actions.openPage(it) } },
                )
            }
            Phase.DOWNLOADING -> {
                headline("Downloading ${o?.versionName ?: ""}")
                val b = Bar(ctx, pal); bar = b
                b.fraction = if (s.total > 0) s.done.toFloat() / s.total else 0f
                addView(b, LayoutParams(LayoutParams.MATCH_PARENT, ctx.dp(10)).apply { topMargin = ctx.dp(6); bottomMargin = ctx.dp(8) })
                val t = label(ctx, progressLine(s), 14f, pal.muted).apply { includeFontPadding = true; setPadding(0, 0, 0, ctx.dp(8)) }
                progressText = t; addView(t)
                buttons(secondary("Pause") { UpdateManager.pause() })
            }
            Phase.READY -> {
                headline("Ready to install ${o?.versionName ?: ""}")
                sub(if (o?.sha256 != null) "The file was checked against the checksum published with the release. Android will ask you to confirm." else "Android will ask you to confirm.")
                roomHint()
                buttons(primary("Install now") { UpdateManager.install(ctx) }, secondary("Delete download") { UpdateManager.discard(ctx) })
            }
            Phase.INSTALLING -> { headline("Waiting for Android's installer…"); sub("Confirm on the screen Android shows. $name restarts when it is done.") }
            Phase.NEEDS_PERMISSION -> {
                headline("Android needs your OK first", pal.warn)
                sub("Open settings, switch on \"Allow from this source\" for $name, come back and tap Install. This is asked once.")
                buttons(primary("Open settings") { actions.openInstallSettings() }, secondary("Install") { UpdateManager.install(ctx) })
            }
            Phase.NEEDS_SPACE -> {
                val retry = { if (s.fileKept) UpdateManager.install(ctx) else actions.download() }
                val fmt = UpdateLogic::formatSize
                val tenMb = 10L shl 20
                if (otherWay) {
                    headline("Install the other way")
                    if (s.otherShort > 0) {
                        // the uninstall would NOT free enough: the owner's tablet (1.62 GB free, the install needs about 3.1 GB, an uninstall gives back about 0.5 GB) would have been left with no browser at all
                        sub("Not yet. After the uninstall this tablet would have about ${fmt(s.otherFreeAfter)} free and the install needs about ${fmt(s.otherNeed)}: ${fmt(UpdateLogic.roundUpTenMb(s.otherShort))} short. If you uninstalled now you would be left without $name and Android would still refuse the saved file.", pal.danger)
                        sub("Make room first (clear the cache, delete downloads you have watched, or free space in Android's Storage screen), then come back: this screen checks again by itself.", pal.fg)
                        buttons(primary("Back") { otherWay = false; otherWayError = null; render() }, secondary("Storage settings") { actions.openStorageSettings() })
                    } else {
                        sub("1. Tap Save and uninstall. The update (${o?.let { UpdateLogic.formatSize(it.size) } ?: ""}) is copied to your Downloads folder, then Android asks whether to uninstall $name.", pal.fg)
                        sub("2. Confirm the uninstall.", pal.fg)
                        sub("3. Open Files, then Downloads, and tap the saved update (named like ${o?.apkName ?: "kestrel-g"}). Allow installing from Files if Android asks.", pal.fg)
                        sub("Before you uninstall: Settings, Back up & restore saves your bookmarks, history, settings and open tabs to a file you restore afterwards. Your downloaded files stay in Downloads.", pal.warn)
                        otherWayError?.let { sub(it, pal.danger) }
                        buttons(
                            primary(if (saving) "Saving…" else "Save and uninstall") {
                                if (!saving) {
                                    saving = true; otherWayError = null; render()
                                    UpdateManager.saveForManualInstall(ctx) { saved, why ->
                                        saving = false
                                        if (saved != null) { android.widget.Toast.makeText(ctx, "Saved to Downloads: $saved", android.widget.Toast.LENGTH_LONG).show(); UpdateManager.uninstallSelf(ctx) }
                                        else { otherWayError = why; render() }
                                    }
                                }
                            },
                            secondary("Back") { otherWay = false; otherWayError = null; render() },
                        )
                    }
                } else {
                    headline("Not enough free storage to update", pal.warn)
                    sub("Free now: ${fmt(s.freeNow)}. Android insists on keeping ${fmt(s.reserveBytes)} free after any install (5 % of this tablet's storage, at most 500 MB) and needs about ${fmt(s.installedBytes)} of room while it installs, so an update in place needs about ${fmt(s.needInPlace)} free" +
                        (if (s.shortBy > 0) ": about ${fmt(s.shortBy)} more than you have." else "."), pal.fg)
                    sub(if (s.fileKept) "The update file is already downloaded: nothing is downloaded again." else "The update file (${o?.let { fmt(it.size) } ?: ""}) still has to be downloaded.")
                    // what this app can free on its own: its cache, the installers of earlier updates, and a look at its biggest downloads
                    if (s.cacheBytes >= tenMb) {
                        sub("$name's cache holds ${fmt(s.cacheBytes)} of pages and pictures. Clearing it only makes pages you open again load a little slower.", pal.fg)
                        buttons(primary("Clear cache (${fmt(s.cacheBytes)})") {
                            actions.clearCache { freed ->
                                android.widget.Toast.makeText(ctx, "Cleared ${fmt(freed)}", android.widget.Toast.LENGTH_SHORT).show()
                                Io.main.postDelayed({ UpdateManager.recheckSpace(ctx) }, 1200)
                                Io.main.postDelayed({ UpdateManager.recheckSpace(ctx) }, 3500)
                            }
                        })
                    }
                    val old = app.kestrel.browser.download.DownloadController.oldInstallers()
                    if (old.isNotEmpty()) {
                        val bytes = old.sumOf { maxOf(it.total, it.done) }
                        sub("${old.size} Kestrel installer ${if (old.size == 1) "file from an earlier update is" else "files from earlier updates are"} still in your Downloads (${fmt(bytes)}).", pal.fg)
                        buttons(primary("Delete old installers") {
                            val freed = app.kestrel.browser.download.DownloadController.deleteOldInstallers()
                            android.widget.Toast.makeText(ctx, "Deleted ${fmt(freed)}", android.widget.Toast.LENGTH_SHORT).show()
                            // the files go away on the download engine's own thread and Android's free-space figure lags a little: look again twice
                            Io.main.postDelayed({ UpdateManager.recheckSpace(ctx) }, 1200)
                            Io.main.postDelayed({ UpdateManager.recheckSpace(ctx) }, 3500)
                        })
                    }
                    val big = app.kestrel.browser.download.DownloadController.views()
                        .filter { it.state == app.kestrel.core.download.DlState.COMPLETED && !it.priv && maxOf(it.total, it.done) >= 50L * 1024 * 1024 && !app.kestrel.browser.download.DownloadController.oldInstallers().contains(it) }
                        .sortedByDescending { maxOf(it.total, it.done) }.take(3)
                    if (big.isNotEmpty()) {
                        sub("Your biggest downloads: " + big.joinToString("; ") { "${it.name.take(34)} (${fmt(maxOf(it.total, it.done))})" } + ". Deleting what you have already watched is the quickest way to make room.", pal.fg)
                        buttons(secondary("Open Downloads") { actions.openDownloads() })
                    }
                    if (s.otherShort > 0) sub("Uninstalling first would not help yet: afterwards the tablet would have about ${fmt(s.otherFreeAfter)} free and the install needs about ${fmt(s.otherNeed)}.")
                    else sub("Another way that works now: uninstall first and install the saved update (your bookmarks and settings need a backup first).")
                    buttons(primary("Try again") { retry() }, secondary("Storage settings") { actions.openStorageSettings() })
                    if (s.otherShort <= 0) buttons(secondary("Install the other way") { otherWay = true; render() })
                }
            }
            Phase.ERROR -> {
                headline("That did not work", pal.danger)
                sub(s.message, pal.fg)
                val page = o?.pageUrl?.takeIf { it.isNotEmpty() }
                val retry = if (o != null && (s.message.contains("download", true) || s.message.contains("Connection lost", true) || s.message.contains("checksum", true) || s.message.contains("space", true)))
                    primary("Try again") { actions.download() } else primary("Check now") { UpdateManager.check(ctx, manual = true) }
                if (page != null) buttons(retry, secondary("Release page") { actions.openPage(page) }) else buttons(retry)
            }
        }

        // the one setting: look once a day (when the app opens) or only on request
        val row = LinearLayout(ctx).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, ctx.dp(14), 0, 0); minimumHeight = ctx.dp(56) }
        val txt = LinearLayout(ctx).apply { orientation = VERTICAL }
        txt.addView(label(ctx, "Look for updates by itself", 15f, pal.fg).apply { includeFontPadding = true })
        txt.addView(label(ctx, "Once a day, when the app opens: one tiny request, no background activity.", 12f, pal.muted).apply { includeFontPadding = true; setPadding(0, ctx.dp(2), 0, 0) })
        row.addView(txt, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        row.addView(Switch(ctx).apply { isChecked = prefs.autoUpdateCheck; setOnCheckedChangeListener { _, on -> prefs.autoUpdateCheck = on } })
        addView(row)
    }

    private fun notes(text: String?) {
        if (text.isNullOrBlank()) return
        val ctx = context
        addView(label(ctx, text, 14f, pal.fg).apply {
            maxLines = 8; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true
            setPadding(ctx.dp(12), ctx.dp(10), ctx.dp(12), ctx.dp(10))
            background = ChamferDrawable(pal.pill, ctx.dpf(8f), ChamferDrawable.TL or ChamferDrawable.BR)
        })
    }

    private fun progressLine(s: UpdateManager.State): String =
        if (s.total > 0) "${UpdateLogic.formatSize(s.done)} of ${UpdateLogic.formatSize(s.total)}  ·  ${(s.done * 100 / s.total).toInt()} %" else UpdateLogic.formatSize(s.done)

    private fun ago(t: Long): String {
        val m = (System.currentTimeMillis() - t) / 60_000
        return when { m < 1 -> "just now"; m < 60 -> "$m min ago"; m < 48 * 60 -> "${m / 60} h ago"; else -> "${m / 1440} days ago" }
    }

    /** A flat chamfered progress bar: no animation, it just redraws at the next tick. */
    private class Bar(ctx: Context, private val pal: Pal) : View(ctx) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val box = RectF()
        private val path = Path()
        var fraction = 0f
            set(v) { val c = v.coerceIn(0f, 1f); if (c != field) { field = c; invalidate() } }

        override fun onDraw(c: Canvas) {
            box.set(0f, 0f, width.toFloat(), height.toFloat())
            ChamferDrawable.path(box, context.dpf(4f), ChamferDrawable.TL or ChamferDrawable.BR, path)
            paint.color = pal.pill; c.drawPath(path, paint)
            c.save(); c.clipPath(path)
            paint.color = pal.accent; c.drawRect(0f, 0f, width * fraction, height.toFloat(), paint)
            c.restore()
        }
    }
}
