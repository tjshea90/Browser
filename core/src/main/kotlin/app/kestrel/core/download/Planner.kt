package app.kestrel.core.download

/** Splitting a download into byte ranges and handing them to connections (research/DOWNLOADS.md section 3.2). Pure; the engine calls it under its lock. */
object Planner {
    const val MIN_SPLIT = 2L shl 20       // never split off less than this
    const val ALIGN = 64L shl 10          // split points are 64 KiB aligned

    /** Bytes to ask for next: a quarter of what is left, between 512 KiB and [maxWindow] - finer toward the end so the last connection's tail is short. */
    fun window(s: Seg, maxWindow: Long): Long {
        val rem = if (s.end < 0) maxWindow else s.end - s.cursor
        return minOf(maxOf(rem, 1L), (rem / 4).coerceIn(512L shl 10, maxWindow))
    }

    /** How many connections this download may use right now. [globalFree] = connections left over all downloads; [running] = this download's workers. */
    fun cap(rec: Rec, cfg: Config, speedBps: Double, globalFree: Int, running: Int): Int {
        if (!cfg.parallel || rec.ranges == 0 || !rec.hasValidator || rec.total <= 0) return 1
        val remaining = rec.total - rec.done
        if (remaining < cfg.minGrowRemaining) return maxOf(1, running)
        if (speedBps > 0 && remaining / maxOf(speedBps, 32768.0) < cfg.minGrowEtaSec) return maxOf(1, running)
        return minOf(cfg.maxConnsPerDownload, running + globalFree).coerceAtLeast(1)
    }

    /**
     * The next piece of work for a free connection: an idle unfinished segment (lowest start), else a NEW segment stolen from the upper half of the biggest
     * tail of a running one (the victim's end is lowered; it stops itself when its cursor reaches it). Returns null when nothing is worth splitting.
     * [liveAhead]: how far past the cursor of an open-ended (first) reply a steal must start - that reply is closed at the new boundary, so keep ~3 s of data.
     */
    fun nextWork(segs: MutableList<Seg>, liveAhead: Long): Seg? {
        var idle: Seg? = null
        for (s in segs) if (!s.worker && !s.done && (idle == null || s.start < idle.start)) idle = s
        if (idle != null) return idle
        var victim: Seg? = null
        var vFrom = 0L
        var vTail = 0L
        for (v in segs) {
            if (!v.worker || v.end < 0) continue
            // a windowed reply's unrequested tail is free to take; an open-ended reply must be cut ahead of its cursor
            val from = if (v.reqEnd < 0) v.cursor + maxOf(MIN_SPLIT, liveAhead) else maxOf(v.reqEnd, v.cursor)
            val tail = v.end - from
            if (tail > vTail || (tail == vTail && victim != null && victim.reqEnd < 0 && v.reqEnd >= 0)) { victim = v; vFrom = from; vTail = tail }
        }
        if (victim == null || vTail < 2 * MIN_SPLIT) return null
        val mid = ((vFrom + vTail / 2) / ALIGN) * ALIGN
        if (mid <= vFrom || mid >= victim.end) return null
        val s = Seg(segs.maxOf { it.idx } + 1, mid, victim.end, mid)
        victim.end = mid
        segs.add(s)
        return s
    }

    /** A thief's first reply was a plain 200 (no range support): give its range back to the victim. */
    fun undoSplit(segs: MutableList<Seg>, thief: Seg): Boolean {
        if (thief.cursor != thief.start) return false
        val v = segs.firstOrNull { it !== thief && it.end == thief.start } ?: return false
        v.end = thief.end
        segs.remove(thief)
        return true
    }

    /** null when the segments are consistent: sorted, contiguous from 0, ending at [total] (or open-ended), each cursor inside its range. */
    fun check(segs: List<Seg>, total: Long): String? {
        if (segs.isEmpty()) return "no segments"
        val sorted = segs.sortedBy { it.start }
        if (sorted.first().start != 0L) return "first segment starts at ${sorted.first().start}"
        for ((i, s) in sorted.withIndex()) {
            if (s.cursor < s.start) return "cursor before start in segment ${s.idx}"
            if (s.end >= 0 && s.cursor > s.end) return "cursor past end in segment ${s.idx}"
            if (i + 1 < sorted.size && s.end != sorted[i + 1].start) return "gap or overlap after segment ${s.idx}"
        }
        val last = sorted.last()
        if (total >= 0 && last.end != total) return "last segment ends at ${last.end}, total is $total"
        return null
    }
}

/** Human text for sizes and speeds (the same strings in the list, the notification and the tests). */
object Format {
    fun bytes(n: Long): String = when {
        n < 0 -> "unknown size"
        n < 1024 -> "$n B"
        n < 1024L * 1024 -> "${n / 1024} KB"
        n < 1024L * 1024 * 1024 -> "%.1f MB".format(java.util.Locale.ROOT, n / 1048576.0)
        else -> "%.2f GB".format(java.util.Locale.ROOT, n / 1073741824.0)
    }

    fun speed(bps: Long): String = bytes(bps) + "/s"

    fun eta(sec: Long): String = when {
        sec < 0 -> ""
        sec < 60 -> "$sec s left"
        sec < 3600 -> "${sec / 60} min left"
        else -> "${sec / 3600} h ${(sec % 3600) / 60} min left"
    }

    /** The one status line under a download's name. */
    fun status(v: DownloadView): String = when (v.state) {
        DlState.QUEUED -> "Waiting in line"
        DlState.STARTING -> "Starting…"
        DlState.RUNNING -> buildString {
            if (v.total > 0) append((v.fraction * 100).toInt()).append(" %") else append(bytes(v.done))
            if (v.bytesPerSec > 0) append("  ·  ").append(speed(v.bytesPerSec))
            if (v.etaSec >= 0) append("  ·  ").append(eta(v.etaSec))
            if (v.connections > 1) append("  ·  ").append(v.connections).append(" connections")
        }
        DlState.PAUSED_USER, DlState.PAUSED_SYSTEM -> "Paused  ·  " + if (v.total > 0) "${(v.fraction * 100).toInt()} %" else bytes(v.done)
        DlState.WAITING_NETWORK -> "Waiting for the connection…"
        DlState.NEEDS_DECISION -> when (v.reason) {
            Reason.FILE_CHANGED -> "The file changed on the server. Restarting downloads it again (${bytes(v.total)})."
            Reason.NO_RANGE -> "This server cannot continue a partial download. Restarting downloads it again."
            else -> "Needs a decision"
        }
        DlState.VERIFYING -> "Finishing…"
        DlState.COMPLETED -> "Done  ·  " + bytes(v.total.takeIf { it >= 0 } ?: v.done)
        DlState.CANCELED -> "Canceled"
        DlState.FAILED -> failure(v)
    }

    fun failure(v: DownloadView): String = when (v.reason) {
        Reason.HTTP -> "The server answered ${v.detail}"
        Reason.LINK_EXPIRED -> "The link expired. Open the page again."
        Reason.NO_SPACE -> "Not enough free space on this tablet"
        Reason.STORAGE -> "Could not write the file"
        Reason.FS_LIMIT -> "The file is too big for this storage"
        Reason.INSECURE_REDIRECT -> "Refused: the secure link redirected to an unencrypted one"
        Reason.TLS -> "Could not verify the site's security certificate"
        Reason.LOCAL_NET -> "Blocked: the link points to a device on your own network"
        Reason.DIGEST -> "The file does not match its checksum"
        Reason.STALLED -> "The connection stalled"
        Reason.NETWORK -> "The connection failed"
        Reason.BAD_URL -> "This address cannot be downloaded"
        else -> "Failed"
    }
}
