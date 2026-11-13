package app.kestrel.core.media

/** The stream cannot become one MP4 (no picture at the end, damaged beyond use). The caller falls back to saving the stream as it is. */
class MuxException(message: String) : RuntimeException(message)

/**
 * Makes ONE MP4 out of the segments of an MPEG-TS HLS stream while they are downloaded: no `.ts` file, no second copy, no wait at the end (design: research/MP4_MUX.md).
 *
 * Per segment the engine calls [beginSegment], [feed] (decrypted bytes, any piece size) and [endSegment]; between segments it calls [checkpoint] now and then; at the end [finish].
 * After a kill, [recover] brings the muxer back to the last checkpoint and names the segment to continue with.
 *
 * What it does with the stream: H.264 or H.265 picture, AAC, AC-3, E-AC-3 or MP3 sound (anything else is a [probe] failure and the engine keeps the old path); leading pictures before the first keyframe and sound before the
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
    /** False when the run lengths given to [beginSegment] are only relative weights (bytes of a finished .ts whose playlist is gone): the warning then says no seconds. */
    private val unitsAreMs: Boolean = true,
) : TsDemux.Out {
    class Finished(val length: Long, val warning: String?)

    private val mux = Mp4Mux(out, nowMs)
    private val demux = TsDemux(this)

    /** True while the rest of a run is being left out (its format is not the main one): the engine need not fetch the segments that remain in that run. */
    val leavingRunOut: Boolean get() = skipping

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
    private var rSps: ByteArray? = null                // the SPS of the run's first picture (H.264 or H.265)
    private var rSpsInfo: AvcSps? = null               // H.264 only
    private var rHevcInfo: HevcSps? = null             // H.265 only
    private var rVps: ByteArray? = null                // H.265 only
    private val rPps = ArrayList<ByteArray>()
    private var rAudio: AudioCfg? = null
    private var audioIgnored = false
    /** The clock: a stream time t lands on the muxer's timeline at t + [delta] (90 kHz). Valid once the run's first picture has been placed; saved in the journal so a resume carries on seamlessly. */
    private var deltaValid = false
    private var delta = 0L
    private var runStart = 0L                         // where this run's first picture goes (the end of what is kept), used to derive [delta]
    private var lastInDts = NONE

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
            droppedRuns > 0 -> "$droppedRuns part${if (droppedRuns > 1) "s" else ""} of the stream${if (unitsAreMs) " (about ${(droppedMs + 500) / 1000} s)" else ""} use a different picture or sound format and were left out."
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
        b.u8(if (deltaValid) 1 else 0); b.zigzag(delta)
    }

    private fun readState(r: Reader): Int {
        val next = r.varint().toInt()
        val kl = r.varint().toInt()
        mainKey = if (kl == 0) null else String(r.bytes(kl - 1), Charsets.UTF_8)
        keptMs = r.varint(); droppedRuns = r.varint().toInt(); droppedMs = r.varint(); skipping = r.u8() == 1
        deltaValid = r.u8() == 1; delta = r.zigzag()
        return next
    }

    // ================================================================================================ runs

    private fun startRun(runMs: Long, resumed: Boolean) {
        curRunMs = runMs
        resolved = false
        videoStarted = false
        rSps = null; rSpsInfo = null; rHevcInfo = null; rVps = null; rPps.clear(); rAudio = null; audioIgnored = false
        lastInDts = NONE
        pendV.clear(); pendA.clear(); pendBytes = 0
        if (!resumed) { skipping = false; deltaValid = false; mux.closeChunk() }   // a resumed segment continues the run: same clock, same open chunk
    }

    private fun audioAbsent() = demux.programKnown && demux.audioType == 0

    private fun keyComplete() = (rSpsInfo != null || rHevcInfo != null) && (rAudio != null || audioAbsent() || audioIgnored)

    private fun hex(b: ByteArray): String { val s = StringBuilder(b.size * 2); for (x in b) s.append(Character.forDigit((x.toInt() shr 4) and 15, 16)).append(Character.forDigit(x.toInt() and 15, 16)); return s.toString() }

    /** Compares this run's format with the main one and either lets its samples through, drops the run, or (a longer different run) replaces everything kept so far. */
    private fun resolve() {
        val info = rSpsInfo
        val hinfo = rHevcInfo
        if (info == null && hinfo == null) return
        val sps = rSps ?: return
        resolved = true
        val vt = demux.videoType
        val supported = (vt == 0x1B || vt == 0x24) && demux.problem == null && (demux.audioType == 0 || demux.audioType in SUPPORTED_AUDIO)
        val snd = rAudio
        val hasSound = snd != null && !audioIgnored
        val soundKey = if (hasSound) (if (snd!!.kind == Mp4Mux.KIND_AAC) "" else "k${snd.kind}:") + hex(snd.config) + "@" + snd.rate else "-"
        val key = (if (hinfo != null) "H265:" + hex(rVps ?: ByteArray(0)) + "|" else "") + hex(sps) + "|" + rPps.joinToString(",") { hex(it) } + "|" + soundKey
        val first = mainKey == null
        val replace = !first && supported && key != mainKey && curRunMs > keptMs
        if (!supported || (!first && key != mainKey && !replace)) {
            skipping = true; droppedRuns++; droppedMs += curRunMs
            pendV.clear(); pendA.clear(); pendBytes = 0
            return
        }
        if (replace) { mux.rollback(INIT); droppedRuns++; droppedMs += keptMs; keptMs = 0; deltaValid = false }
        if (first || replace) {
            if (hinfo != null) mux.setVideoHevc(hinfo.width, hinfo.height, Hevc.hvcC(rVps, sps, hinfo, rPps))
            else mux.setVideo(info!!.width, info.height, Avc.avcC(sps, info, rPps))
            if (hasSound) mux.setAudio(snd!!)
            mainKey = key
            runStart = 0
        } else if (!deltaValid) runStart = endOfTimeline()
        keptMs += curRunMs
        for (p in pendV) emitVideo(p.b, p.b.size, p.dts, p.pts, p.sync)
        for (p in pendA) emitAudio(p.b, p.sizes, p.n, p.pts)
        pendV.clear(); pendA.clear(); pendBytes = 0
    }

    /** Where the next run begins: after the later of the last picture and the end of the sound, in 90 kHz ticks. */
    private fun endOfTimeline(): Long = if (mux.videoCount == 0) 0L else maxOf(mux.lastVideoDts + mux.videoDuration, mux.audioEnd90k)

    // ================================================================================================ picture

    override fun video(es: ByteArray, len: Int, pts: Long, dts: Long) {
        if (demux.videoType == 0x24) { videoHevc(es, len, pts, dts); return }
        if (resolved || skipping) peekFormatChange(es, len)
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

    /**
     * A new picture size / profile announced by an SPS in the middle of a run is a splice nobody tagged (live-to-VOD streams, careless advertising servers): it starts a run of its own, judged
     * like a tagged one of unknown length (kept only when it is the main format again, left out otherwise). Looks at the first 2 KB of the access unit only (parameter sets come before the slices).
     */
    private fun peekFormatChange(es: ByteArray, len: Int) {
        val known = rSpsInfo ?: return
        val w = minOf(len, 2048)
        var found: ByteArray? = null
        Avc.forEachNal(es, 0, w) { a, e -> if (found == null && (es[a].toInt() and 0x1F) == Avc.NAL_SPS && e < w - 4) found = es.copyOfRange(a, e) }
        val sps = found ?: return
        if (java.util.Arrays.equals(sps, rSps)) return
        val info = Avc.parseSps(sps, 0, sps.size) ?: return
        if (info.width == known.width && info.height == known.height && info.profile == known.profile && info.chromaFormat == known.chromaFormat && info.bitDepthLuma == known.bitDepthLuma) return
        startRun(0L, resumed = false)
    }

    /** The same job as [video] for an H.265 picture: parameter sets go to `hvcC` (not into the samples), AUD / end-of-sequence / filler units are left out, IRAP pictures are the keyframes. */
    private fun videoHevc(es: ByteArray, len: Int, pts: Long, dts: Long) {
        if (resolved || skipping) peekFormatChangeHevc(es, len)
        if (skipping) return
        val capturing = !videoStarted
        val s = sample
        s.clear()
        var sync = false
        Avc.forEachNal(es, 0, len) { a, e ->
            if (e - a >= 3) {
                val t = Hevc.nalType(es, a)
                when (t) {
                    Hevc.NAL_AUD, Hevc.NAL_EOS, Hevc.NAL_EOB, Hevc.NAL_FILLER -> {}
                    Hevc.NAL_VPS -> if (capturing && rVps == null) rVps = es.copyOfRange(a, e)
                    Hevc.NAL_SPS -> if (capturing && rSps == null) { rSps = es.copyOfRange(a, e); rHevcInfo = Hevc.parseSps(es, a, e - a) }
                    Hevc.NAL_PPS -> if (capturing && rPps.size < 8 && rPps.none { it.size == e - a && java.util.Arrays.equals(it, es.copyOfRange(a, e)) }) rPps.add(es.copyOfRange(a, e))
                    else -> { if (Hevc.isIrap(t)) sync = true; s.u32(e - a); s.put(es, a, e - a) }
                }
            }
        }
        if (s.n == 0) return
        if (!videoStarted) { if (!sync || rHevcInfo == null) return; videoStarted = true }
        val d = if (dts >= 0) dts else maxOf(pts, 0L)
        val p = if (pts >= 0) pts else d
        if (resolved) emitVideo(s.a, s.n, d, p, sync)
        else {
            pendV.add(PendV(s.a.copyOf(s.n), d, p, sync)); pendBytes += s.n
            if (pendBytes > MAX_PENDING) { audioIgnored = true }
            if (keyComplete()) resolve()
        }
    }

    /** [peekFormatChange] for H.265: a new picture size / profile in an SPS in the middle of a run starts a run of its own. */
    private fun peekFormatChangeHevc(es: ByteArray, len: Int) {
        val known = rHevcInfo ?: return
        val w = minOf(len, 2048)
        var found: ByteArray? = null
        Avc.forEachNal(es, 0, w) { a, e -> if (found == null && e - a >= 3 && Hevc.nalType(es, a) == Hevc.NAL_SPS && e < w - 4) found = es.copyOfRange(a, e) }
        val sps = found ?: return
        if (java.util.Arrays.equals(sps, rSps)) return
        val info = Hevc.parseSps(sps, 0, sps.size) ?: return
        if (info.width == known.width && info.height == known.height && info.profileIdc == known.profileIdc && info.chromaFormat == known.chromaFormat && info.bitDepthLuma == known.bitDepthLuma) return
        startRun(0L, resumed = false)
    }

    private fun emitVideo(b: ByteArray, n: Int, dts: Long, pts: Long, sync: Boolean) {
        if (!deltaValid) { delta = runStart - dts; deltaValid = true }
        else if (lastInDts != NONE) {
            val jump = dts - lastInDts
            if (jump > JUMP_FORWARD || jump < -JUMP_BACK) delta = endOfTimeline() - dts      // the stream's clock restarted without a tag: a new run on the muxer's timeline
        }
        lastInDts = dts
        var md = dts + delta
        val mp = pts + delta
        if (mux.videoCount > 0 && md <= mux.lastVideoDts) md = mux.lastVideoDts + 1
        mux.addVideo(b, 0, n, md, (mp - md).coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt(), sync)
    }

    // ================================================================================================ sound

    override fun audio(es: ByteArray, len: Int, pts: Long) {
        if (skipping) return
        val offs: IntArray; val lens: IntArray
        when (demux.audioType) {
            0x0F -> {
                val frames = Adts.frames(es, 0, len)
                if (frames.isEmpty()) return
                if (rAudio == null) Adts.config(es, 0, len)?.let { rAudio = AudioCfg(Mp4Mux.KIND_AAC, it.sampleRate, it.channels, it.asc, Mp4Mux.AAC_FRAME) }
                offs = IntArray(frames.size) { frames[it].payloadOffset }; lens = IntArray(frames.size) { frames[it].payloadLength }
            }
            0x81 -> {
                val f = Ac3.frames(es, 0, len)
                if (f.isEmpty()) return
                if (rAudio == null) Ac3.info(es, f[0].offset, len)?.let { rAudio = AudioCfg(Mp4Mux.KIND_AC3, it.sampleRate, it.channels, it.config, it.frameSamples) }
                offs = IntArray(f.size) { f[it].offset }; lens = IntArray(f.size) { f[it].length }
            }
            0x87 -> {
                val f = Eac3.frames(es, 0, len)
                if (f.isEmpty()) return
                if (rAudio == null) Eac3.info(es, f[0].offset, len)?.let { rAudio = AudioCfg(Mp4Mux.KIND_EAC3, it.sampleRate, it.channels, it.config, it.frameSamples) }
                offs = IntArray(f.size) { f[it].offset }; lens = IntArray(f.size) { f[it].length }
            }
            0x03, 0x04 -> {
                val f = Mp3.frames(es, 0, len)
                if (f.isEmpty()) return
                if (rAudio == null) Mp3.info(es, f[0].offset, len)?.let { rAudio = AudioCfg(Mp4Mux.KIND_MP3, it.sampleRate, it.channels, it.config, it.frameSamples) }
                offs = IntArray(f.size) { f[it].offset }; lens = IntArray(f.size) { f[it].length }
            }
            else -> return
        }
        if (!videoStarted || audioIgnored) { if (!resolved && keyComplete()) resolve(); return }   // sound before the first kept picture is dropped
        val a = abuf
        a.clear()
        val sizes = IntArray(offs.size)
        for (i in offs.indices) { a.put(es, offs[i], lens[i]); sizes[i] = lens[i] }
        if (resolved) { if (mux.hasAudioConfig) emitAudio(a.a, sizes, sizes.size, pts, a.n) }
        else { pendA.add(PendA(a.a.copyOf(a.n), sizes, sizes.size, pts)); pendBytes += a.n; if (keyComplete()) resolve() }
    }

    private fun emitAudio(b: ByteArray, sizes: IntArray, n: Int, pts: Long, total: Int = b.size) {
        val rate = mux.sampleRate
        var tick: Long
        if (pts < 0) tick = if (mux.audioCount > 0) mux.audioEndTick else (mux.lastVideoDts * rate + 45000) / 90000
        else { val t = pts + delta; tick = (t * rate + (if (t >= 0) 45000 else -45000)) / 90000 }
        if (mux.audioCount > 0 && Math.abs(tick - mux.audioEndTick) > 5L * rate) tick = mux.audioEndTick     // a jump the picture track did not make: keep the sound on its own grid
        mux.addAudio(b, 0, total, sizes, n, tick)
    }

    // ================================================================================================ probe

    companion object {
        private val INIT = Mp4Mux.Mark(Mp4Mux.HEADER_END, 0, 0, 0, 0L)
        private const val MAX_PENDING = 8 shl 20
        private const val JUMP_FORWARD = 10 * 90000L
        private const val JUMP_BACK = 90000L
        private const val NONE = Long.MIN_VALUE
        private val SUPPORTED_AUDIO = intArrayOf(0x0F, 0x81, 0x87, 0x03, 0x04)

        private fun videoName(t: Int) = when (t) { 0x24 -> "H.265 (HEVC)"; 0x01, 0x02 -> "MPEG-2 video"; 0x10 -> "MPEG-4 video"; 0xEA -> "VC-1"; else -> "an unknown video format (stream type 0x${Integer.toHexString(t)})" }
        private fun audioName(t: Int) = when (t) { 0x81 -> "AC-3"; 0x87 -> "E-AC-3"; 0x03, 0x04 -> "MP3 (MPEG audio)"; 0x11 -> "AAC in LATM"; 0x82, 0x8A -> "DTS"; else -> "an unknown sound format (stream type 0x${Integer.toHexString(t)})" }

        /**
         * Looks at the start of a stream's first segment and says why it cannot be written as an MP4 here, or null when it can: an H.264 or H.265 picture with its parameters, and sound that is AAC, AC-3, E-AC-3, MP3 or absent.
         * Costs one pass over at most the bytes given.
         */
        fun probe(b: ByteArray, off: Int, len: Int): String? {
            var sps = false
            var audio: AudioCfg? = null
            var dm: TsDemux? = null
            val d = TsDemux(object : TsDemux.Out {
                override fun video(es: ByteArray, len: Int, pts: Long, dts: Long) {
                    if (sps) return
                    if (dm?.videoType == 0x24) Avc.forEachNal(es, 0, len) { a, e -> if (e - a >= 3 && Hevc.nalType(es, a) == Hevc.NAL_SPS && Hevc.parseSps(es, a, e - a) != null) sps = true }
                    else Avc.forEachNal(es, 0, len) { a, e -> if ((es[a].toInt() and 0x1F) == Avc.NAL_SPS && Avc.parseSps(es, a, e - a) != null) sps = true }
                }
                override fun audio(es: ByteArray, len: Int, pts: Long) {
                    if (audio != null) return
                    audio = when (dm?.audioType) {
                        0x0F -> Adts.config(es, 0, len)?.let { AudioCfg(Mp4Mux.KIND_AAC, it.sampleRate, it.channels, it.asc, Mp4Mux.AAC_FRAME) }
                        0x81 -> Ac3.frames(es, 0, len).firstOrNull()?.let { f -> Ac3.info(es, f.offset, len)?.let { AudioCfg(Mp4Mux.KIND_AC3, it.sampleRate, it.channels, it.config, it.frameSamples) } }
                        0x87 -> Eac3.frames(es, 0, len).firstOrNull()?.let { f -> Eac3.info(es, f.offset, len)?.let { AudioCfg(Mp4Mux.KIND_EAC3, it.sampleRate, it.channels, it.config, it.frameSamples) } }
                        0x03, 0x04 -> Mp3.frames(es, 0, len).firstOrNull()?.let { f -> Mp3.info(es, f.offset, len)?.let { AudioCfg(Mp4Mux.KIND_MP3, it.sampleRate, it.channels, it.config, it.frameSamples) } }
                        else -> null
                    }
                }
            })
            dm = d
            try { d.feed(b, off, len); d.endSegment() } catch (e: RuntimeException) { return "the first part of the stream could not be read" }
            d.problem?.let { return "the stream is $it" }
            if (!d.programKnown) return "the first part of the stream has no program table"
            if (d.videoType == 0) return "no picture stream"
            if (d.videoType != 0x1B && d.videoType != 0x24) return "the picture is ${videoName(d.videoType)}, not H.264 or H.265"
            if (d.audioType != 0 && d.audioType !in SUPPORTED_AUDIO) return "the sound is ${audioName(d.audioType)}, not AAC, AC-3, E-AC-3 or MP3"
            if (!sps) return "no keyframe with its parameters in the first part of the stream"
            if (d.audioType != 0 && audio == null) return "the ${audioName(d.audioType)} sound cannot be read"
            return null
        }
    }
}
