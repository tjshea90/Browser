package app.kestrel.core.media

/** The stream cannot become one MP4 (no picture at the end, damaged beyond use). The caller falls back to saving the stream as it is. */
class MuxException(message: String) : RuntimeException(message)

/**
 * Makes ONE MP4 out of the segments of an MPEG-TS HLS stream while they are downloaded: no `.ts` file, no second copy, no wait at the end (design: research/MP4_MUX.md).
 *
 * Per segment the engine calls [beginSegment], [feed] (decrypted bytes, any piece size) and [endSegment]; between segments it calls [checkpoint] now and then; at the end [finish].
 * After a kill, [recover] brings the muxer back to the last checkpoint and names the segment to continue with.
 *
 * What it does with the stream: H.264 + AAC only (anything else is a [probe] failure and the engine keeps the old path); leading pictures before the first keyframe and sound before the
 * first kept picture are dropped; time stamps are put on one timeline, and every `#EXT-X-DISCONTINUITY` run is laid after the previous one. A run whose picture or sound format differs from
 * the first (spliced-in advertising) is left out, unless it is longer than everything kept so far (an advertisement BEFORE the film): then the earlier part is discarded instead.
 */
class TsToMp4(
    out: MuxOut,
    private val journal: MuxJournal,
    /** Makes everything written to the output durable; called before each journal block, so the journal never claims more than the file holds. */
    private val syncOut: () -> Unit,
    /** The whole playlist's playing time (for the "part of the stream was left out" warning). */
    private val totalMs: Long,
    /** The clock for the file's creation time (tests pass a fixed one so two runs give identical bytes). */
    nowMs: () -> Long = { System.currentTimeMillis() },
) : TsDemux.Out {
    class Finished(val length: Long, val warning: String?)

    private val mux = Mp4Mux(out, nowMs)
    private val demux = TsDemux(this)

    /** Bytes written to the output so far (the MP4 body; the `moov` follows at [finish]). */
    val position: Long get() = mux.pos

    // ---- state of the whole stream (saved in every journal block)
    private var mainKey: String? = null
    private var keptMs = 0L
    private var droppedRuns = 0
    private var droppedMs = 0L
    private var skipping = false                      // the rest of a left-out run

    // ---- state of the current run
    private var recovered = false
    private var curRunMs = 0L
    private var resolved = false                      // format compared with the main one, samples flow straight into the muxer
    private var videoStarted = false
    private var rSps: ByteArray? = null
    private var rSpsInfo: AvcSps? = null
    private val rPps = ArrayList<ByteArray>()
    private var rAsc: ByteArray? = null
    private var rRate = 0
    private var rChannels = 0
    private var audioIgnored = false
    private var hasOrigin = false
    private var origin = 0L
    private var runStart = 0L
    private var lastInDts = 0L

    private class PendV(val b: ByteArray, val dts: Long, val pts: Long, val sync: Boolean)
    private class PendA(val b: ByteArray, val sizes: IntArray, val n: Int, val pts: Long)
    private val pendV = ArrayList<PendV>()
    private val pendA = ArrayList<PendA>()
    private var pendBytes = 0

    private val sample = Bytes(1 shl 16)
    private val abuf = Bytes(4096)

    // ================================================================================================ lifecycle

    /** A new output: writes the file header and forgets any journal. */
    fun startFresh() { journal.reset(); mux.start() }

    /** Brings the muxer back to the state of the last checkpoint and returns the segment index to continue with; null = nothing to recover (call [startFresh]). */
    fun recover(): Int? {
        val blocks = journal.recover()
        if (blocks.isEmpty()) return null
        return try {
            var last: ByteArray? = null
            for (blk in blocks) {
                val r = Reader(blk)
                val n = r.varint().toInt()
                mux.replay(r.bytes(n))
                last = blk.copyOfRange(r.pos, blk.size)
            }
            val next = readState(Reader(last!!))
            recovered = true
            next
        } catch (e: RuntimeException) { null }
    }

    /** A segment starts. [discontinuity] is the playlist's flag; [runMs] is the playing time of the run this segment begins (only used when it begins one). */
    fun beginSegment(index: Int, discontinuity: Boolean, runMs: Long) {
        val resume = recovered && !discontinuity && index != 0
        if (index == 0 || discontinuity || recovered) {
            startRun(if (resume) 0L else runMs, resume)
            recovered = false
        }
    }

    fun feed(b: ByteArray, off: Int, len: Int) { demux.feed(b, off, len) }

    /** The segment is over: its last pictures are flushed, and a run whose format could not be compared yet is settled. */
    fun endSegment() {
        demux.endSegment()
        if (!resolved && !skipping) resolve()
    }

    /** Between segments: syncs the output, then appends what changed to the journal. [next] is the index of the first segment NOT yet fed. */
    fun checkpoint(next: Int) {
        syncOut()
        val ev = mux.drainJournal()
        val blk = Bytes(ev.size + 64)
        blk.varint(ev.size.toLong()); blk.put(ev)
        writeState(blk, next)
        journal.append(blk.a.copyOf(blk.n))
    }

    /** Writes the `moov` and finishes the file. Throws [MuxException] when there is no picture. */
    fun finish(): Finished {
        if (!resolved && !skipping) resolve()
        if (mux.videoCount == 0) throw MuxException("no picture could be read from the stream")
        val total = mux.finish()
        journal.reset()
        val warn = when {
            droppedRuns > 0 -> "$droppedRuns part${if (droppedRuns > 1) "s" else ""} of the stream (about ${(droppedMs + 500) / 1000} s) use a different picture or sound format and were left out."
            totalMs > 0 && keptMs * 2 < totalMs -> "Only part of the stream could be saved."
            else -> null
        }
        return Finished(total, warn)
    }

    private fun writeState(b: Bytes, next: Int) {
        b.varint(next.toLong())
        val k = mainKey?.toByteArray(Charsets.UTF_8)
        b.varint(if (k == null) 0L else k.size + 1L); if (k != null) b.put(k)
        b.varint(keptMs); b.varint(droppedRuns.toLong()); b.varint(droppedMs); b.u8(if (skipping) 1 else 0)
    }

    private fun readState(r: Reader): Int {
        val next = r.varint().toInt()
        val kl = r.varint().toInt()
        mainKey = if (kl == 0) null else String(r.bytes(kl - 1), Charsets.UTF_8)
        keptMs = r.varint(); droppedRuns = r.varint().toInt(); droppedMs = r.varint(); skipping = r.u8() == 1
        return next
    }

    // ================================================================================================ runs

    private fun startRun(runMs: Long, resumed: Boolean) {
        curRunMs = runMs
        resolved = false
        videoStarted = false
        rSps = null; rSpsInfo = null; rPps.clear(); rAsc = null; rRate = 0; rChannels = 0; audioIgnored = false
        hasOrigin = false
        pendV.clear(); pendA.clear(); pendBytes = 0
        if (!resumed) skipping = false
        mux.closeChunk()
    }

    private fun audioAbsent() = demux.programKnown && demux.audioType == 0

    private fun keyComplete() = rSpsInfo != null && (rAsc != null || audioAbsent() || audioIgnored)

    private fun hex(b: ByteArray): String { val s = StringBuilder(b.size * 2); for (x in b) s.append(Character.forDigit((x.toInt() shr 4) and 15, 16)).append(Character.forDigit(x.toInt() and 15, 16)); return s.toString() }

    /** Compares this run's format with the main one and either lets its samples through, drops the run, or (a longer different run) replaces everything kept so far. */
    private fun resolve() {
        val info = rSpsInfo ?: return
        val sps = rSps ?: return
        resolved = true
        val supported = demux.videoType == 0x1B && demux.problem == null && (demux.audioType == 0 || demux.audioType == 0x0F)
        val hasSound = rAsc != null && !audioIgnored && demux.audioType == 0x0F
        val key = hex(sps) + "|" + rPps.joinToString(",") { hex(it) } + "|" + (if (hasSound) hex(rAsc!!) + "@" + rRate else "-")
        val first = mainKey == null
        val replace = !first && supported && key != mainKey && curRunMs > keptMs
        if (!supported || (!first && key != mainKey && !replace)) {
            skipping = true; droppedRuns++; droppedMs += curRunMs
            pendV.clear(); pendA.clear(); pendBytes = 0
            return
        }
        if (replace) { mux.rollback(INIT); droppedRuns++; droppedMs += keptMs; keptMs = 0 }
        if (first || replace) {
            mux.setVideo(info.width, info.height, Avc.avcC(sps, info, rPps))
            if (hasSound) mux.setAudio(rAsc!!, rRate, rChannels)
            mainKey = key
            runStart = 0
        } else runStart = endOfTimeline()
        keptMs += curRunMs
        for (p in pendV) emitVideo(p.b, p.b.size, p.dts, p.pts, p.sync)
        for (p in pendA) emitAudio(p.b, p.sizes, p.n, p.pts)
        pendV.clear(); pendA.clear(); pendBytes = 0
    }

    /** Where the next run begins: after the later of the last picture and the end of the sound, in 90 kHz ticks. */
    private fun endOfTimeline(): Long = if (mux.videoCount == 0) 0L else maxOf(mux.lastVideoDts + mux.videoDuration, mux.audioEnd90k)

    // ================================================================================================ picture

    override fun video(es: ByteArray, len: Int, pts: Long, dts: Long) {
        if (skipping) return
        val capturing = !videoStarted
        val s = sample
        s.clear()
        var sync = false; var recovery = false; var slice = -1
        Avc.forEachNal(es, 0, len) { a, e ->
            val t = es[a].toInt() and 0x1F
            if (t != Avc.NAL_AUD && t != 10 && t != 11 && t != 12) {
                if (t == Avc.NAL_IDR) sync = true
                else if (t == Avc.NAL_SPS) { if (capturing && rSps == null) { rSps = es.copyOfRange(a, e); rSpsInfo = Avc.parseSps(es, a, e - a) } }
                else if (t == Avc.NAL_PPS) { if (capturing && rPps.size < 8 && rPps.none { it.size == e - a && java.util.Arrays.equals(it, es.copyOfRange(a, e)) }) rPps.add(es.copyOfRange(a, e)) }
                else if (t == Avc.NAL_SEI) { if (Avc.hasRecoveryPoint(es, a, e - a)) recovery = true }
                else if (t == Avc.NAL_SLICE && slice < 0) slice = Avc.sliceType(es, a, e - a)
                s.u32(e - a); s.put(es, a, e - a)
            }
        }
        if (s.n == 0) return
        if (!sync && recovery && (slice == 2 || slice == 7)) sync = true      // an open-GOP stream: a recovery point stands in for the keyframe
        if (!videoStarted) { if (!sync || rSpsInfo == null) return; videoStarted = true }
        val d = if (dts >= 0) dts else maxOf(pts, 0L)
        val p = if (pts >= 0) pts else d
        if (resolved) emitVideo(s.a, s.n, d, p, sync)
        else {
            pendV.add(PendV(s.a.copyOf(s.n), d, p, sync)); pendBytes += s.n
            if (pendBytes > MAX_PENDING) { audioIgnored = true }             // the sound never showed up: go on without it
            if (keyComplete()) resolve()
        }
    }

    private fun emitVideo(b: ByteArray, n: Int, dts: Long, pts: Long, sync: Boolean) {
        if (!hasOrigin) { origin = dts; hasOrigin = true; lastInDts = dts }
        else {
            val jump = dts - lastInDts
            if (jump > JUMP_FORWARD || jump < -JUMP_BACK) { origin = dts; runStart = endOfTimeline() }   // the stream's clock restarted without a tag: start a new run on the muxer's timeline
            lastInDts = dts
        }
        var md = dts - origin + runStart
        val mp = pts - origin + runStart
        if (mux.videoCount > 0 && md <= mux.lastVideoDts) md = mux.lastVideoDts + 1
        mux.addVideo(b, 0, n, md, (mp - md).coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt(), sync)
    }

    // ================================================================================================ sound

    override fun audio(es: ByteArray, len: Int, pts: Long) {
        if (skipping || demux.audioType != 0x0F) return
        val frames = Adts.frames(es, 0, len)
        if (frames.isEmpty()) return
        if (rAsc == null) Adts.config(es, 0, len)?.let { rAsc = it.asc; rRate = it.sampleRate; rChannels = it.channels }
        if (!videoStarted || audioIgnored) { if (!resolved && keyComplete()) resolve(); return }   // sound before the first kept picture is dropped
        val a = abuf
        a.clear()
        val sizes = IntArray(frames.size)
        for ((i, f) in frames.withIndex()) { a.put(es, f.payloadOffset, f.payloadLength); sizes[i] = f.payloadLength }
        if (resolved) { if (mux.hasAudioConfig) emitAudio(a.a, sizes, sizes.size, pts, a.n) }
        else { pendA.add(PendA(a.a.copyOf(a.n), sizes, sizes.size, pts)); pendBytes += a.n; if (keyComplete()) resolve() }
    }

    private fun emitAudio(b: ByteArray, sizes: IntArray, n: Int, pts: Long, total: Int = b.size) {
        val rate = mux.sampleRate
        var tick: Long
        if (pts < 0) tick = if (mux.audioCount > 0) mux.audioEndTick else ((lastInDts - origin + runStart).coerceAtLeast(0L) * rate + 45000) / 90000
        else tick = ((pts - origin + runStart) * rate + (if (pts >= origin - runStart) 45000 else -45000)) / 90000
        if (mux.audioCount > 0 && Math.abs(tick - mux.audioEndTick) > 5L * rate) tick = mux.audioEndTick     // a jump the picture track did not make: keep the sound on its own grid
        mux.addAudio(b, 0, total, sizes, n, tick)
    }

    // ================================================================================================ probe

    companion object {
        private val INIT = Mp4Mux.Mark(Mp4Mux.HEADER_END, 0, 0, 0, 0L)
        private const val MAX_PENDING = 8 shl 20
        private const val JUMP_FORWARD = 10 * 90000L
        private const val JUMP_BACK = 90000L

        /**
         * Looks at the start of a stream's first segment and says why it cannot be written as an MP4 here, or null when it can: H.264 picture with its parameters, and sound that is AAC or absent.
         * Costs one pass over at most the bytes given.
         */
        fun probe(b: ByteArray, off: Int, len: Int): String? {
            var sps = false
            var aac: Adts.Config? = null
            val d = TsDemux(object : TsDemux.Out {
                override fun video(es: ByteArray, len: Int, pts: Long, dts: Long) {
                    if (!sps) Avc.forEachNal(es, 0, len) { a, e -> if ((es[a].toInt() and 0x1F) == Avc.NAL_SPS && Avc.parseSps(es, a, e - a) != null) sps = true }
                }
                override fun audio(es: ByteArray, len: Int, pts: Long) { if (aac == null) aac = Adts.config(es, 0, len) }
            })
            d.feed(b, off, len); d.endSegment()
            d.problem?.let { return "the stream is $it" }
            if (!d.programKnown) return "the first part of the stream has no program table"
            if (d.videoType == 0) return "no picture stream"
            if (d.videoType != 0x1B) return "the picture is not H.264 (stream type 0x${Integer.toHexString(d.videoType)})"
            if (d.audioType != 0 && d.audioType != 0x0F) return "the sound is not AAC (stream type 0x${Integer.toHexString(d.audioType)})"
            if (!sps) return "no keyframe with its parameters in the first part of the stream"
            if (d.audioType == 0x0F && aac == null) return "the AAC sound cannot be read"
            return null
        }
    }
}
