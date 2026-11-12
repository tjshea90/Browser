package app.kestrel.browser.download

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.util.Log
import app.kestrel.browser.Io
import app.kestrel.browser.diag.Health
import app.kestrel.core.download.Sink
import app.kestrel.core.download.SinkFactory
import app.kestrel.core.media.Adts
import app.kestrel.core.media.AudioClock
import app.kestrel.core.media.StreamFinisher
import java.nio.ByteBuffer

/**
 * MPEG-TS -> MP4 with the platform's own MediaExtractor + MediaMuxer (no ffmpeg, no native code of ours). Only H.264 / H.265 video with AAC (or no) audio is converted, because that is what an MP4
 * can hold and what the muxer can write; anything else, any doubt and any exception keep the .ts exactly as it was. The samples are copied unchanged (no re-encoding): fast, lossless, little heap
 * (one sample buffer). Needs about as much free room as the stream itself, so it declines when there is not.
 */
class Remuxer(private val ctx: Context, private val sinks: SinkFactory) : StreamFinisher {

    override fun convert(j: StreamFinisher.Joined): StreamFinisher.Converted? {
        if (j.container != "ts") return null
        if (!enoughRoom(j.size)) { j.declined = "there is not enough free room on the tablet (about ${(roomNeeded(j.size) shr 20) + 1} MB more are needed)"; return declined(j.declined!!) }
        val r = if (j.parts.size > 1) convertParts(j) else convertWhole(j)
        if (r == null && j.declined == null) j.declined = lastDecline ?: "Android's video converter could not read this stream"
        return r
    }

    @Volatile private var lastDecline: String? = null

    /** Reads the finished MP4 with MediaExtractor (what Android's own players use): tracks, length, the first sample and a seek to the middle. Logged to Diagnostics either way. */
    override fun verify(uri: String): String? {
        val ex = MediaExtractor()
        return try {
            ex.setDataSource(ctx, Uri.parse(uri), null)
            var video = 0; var audio = 0; var durationUs = 0L
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i); val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) video++ else if (mime.startsWith("audio/")) audio++
                if (f.containsKey(MediaFormat.KEY_DURATION)) durationUs = maxOf(durationUs, f.getLong(MediaFormat.KEY_DURATION))
            }
            if (video == 0) { Health.log("mp4", "check failed", "no picture track"); return "no picture track" }
            ex.selectTrack(0)
            val buf = ByteBuffer.allocate(4 shl 20)
            val first = ex.readSampleData(buf, 0)
            if (durationUs > 2_000_000) ex.seekTo(durationUs / 2, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val mid = ex.sampleTime
            Health.log("mp4", "check ok", "video=$video audio=$audio ${durationUs / 1000} ms first=$first midAt=${mid / 1000} ms")
            if (first <= 0) "no first sample" else null
        } catch (e: Throwable) {
            Health.log("mp4", "check failed", e.toString().take(160))
            e.javaClass.simpleName
        } finally { try { ex.release() } catch (e: Exception) { /* ignore */ } }
    }

    /** Which track of an extractor is the picture and which the sound the MP4 can hold; anything else (timed metadata, subtitles) is not part of the result. */
    private class Picked(val video: Int, val audio: Int, val unsupportedAudio: Boolean)

    private fun pick(ex: MediaExtractor): Picked {
        var vt = -1; var at = -1; var bad = false
        for (i in 0 until ex.trackCount) {
            val mime = ex.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
            when {
                mime == "video/avc" || mime == "video/hevc" -> if (vt < 0) vt = i
                mime == "audio/mp4a-latm" -> if (at < 0) at = i
                mime.startsWith("audio/") -> bad = true // MP3, AC-3 ...: an MP4 from this muxer cannot carry it
            }
        }
        return Picked(vt, at, bad)
    }

    private fun declined(why: String): StreamFinisher.Converted? {
        Log.w(Io.TAG, "remux declined: $why"); Health.log("remux", "declined", why)
        lastDecline = why
        return null
    }

    private fun formatKey(vf: MediaFormat, af: MediaFormat?): String {
        fun i(f: MediaFormat, k: String) = if (f.containsKey(k)) f.getInteger(k) else 0
        val v = "${vf.getString(MediaFormat.KEY_MIME)}:${i(vf, MediaFormat.KEY_WIDTH)}x${i(vf, MediaFormat.KEY_HEIGHT)}"
        val a = if (af == null) "none" else "${af.getString(MediaFormat.KEY_MIME)}:${i(af, MediaFormat.KEY_SAMPLE_RATE)}:${i(af, MediaFormat.KEY_CHANNEL_COUNT)}"
        return "$v|$a"
    }

    /**
     * A stream with #EXT-X-DISCONTINUITY marks: every run between two marks has its own time stamps (they start over), so each is read on its own and the runs are laid end to end. Runs whose
     * picture or sound format differs from the main one (the format with the most playing time) are spliced-in extras (an advertisement, a promo card) and are left out; when that would
     * be more than half of the stream, nothing is converted and the .ts stays.
     */
    private fun convertParts(j: StreamFinisher.Joined): StreamFinisher.Converted? {
        val pfd = try { ctx.contentResolver.openFileDescriptor(Uri.parse(j.uri), "r") } catch (e: Exception) { null } ?: return declined("cannot reopen the joined file")
        var out: Sink? = null
        var muxer: MediaMuxer? = null
        var started = false
        try {
            class Run(val part: StreamFinisher.Part, val key: String, val vf: MediaFormat, val af: MediaFormat?)
            val runs = ArrayList<Run>()
            for (p in j.parts) {
                val ex = MediaExtractor()
                try {
                    ex.setDataSource(pfd.fileDescriptor, p.offset, p.length)
                    val pk = ex.let { pick(it) }
                    if (pk.video < 0 || (pk.audio < 0 && pk.unsupportedAudio)) continue
                    val vf = ex.getTrackFormat(pk.video); val af = if (pk.audio >= 0) ex.getTrackFormat(pk.audio) else null
                    if (af != null && !af.containsKey("csd-0")) continue
                    runs.add(Run(p, formatKey(vf, af), vf, af))
                } catch (e: Exception) { /* an unreadable run is simply not part of the result */ } finally { try { ex.release() } catch (e: Exception) { /* ignore */ } }
            }
            if (runs.isEmpty()) return declined("no run of ${j.parts.size} has a picture the MP4 can hold")
            val weight = { r: Run -> maxOf(r.part.durationMs, 1L) }
            val perKey = HashMap<String, Long>()
            for (r in runs) perKey[r.key] = (perKey[r.key] ?: 0L) + weight(r)
            val main = perKey.maxByOrNull { it.value }!!.key
            val keep = runs.filter { it.key == main }
            val total = j.parts.sumOf { maxOf(it.durationMs, 1L) }
            if (keep.sumOf(weight) * 2 < total) return declined("the runs differ too much (${keep.size} of ${j.parts.size} share a format)")
            val vf = keep[0].vf; val af = keep[0].af
            val name = j.stem + ".mp4"
            val sink = sinks.create(name, "video/mp4", -1, j.priv, j.destKind).also { out = it }
            val fd = sink.descriptor() ?: run { sink.discard(); return declined("no file descriptor for the result") }
            val m = MediaMuxer(fd, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4).also { muxer = it }
            val vOut = m.addTrack(vf)
            val aOut = if (af != null) m.addTrack(af) else -1
            val buf = ByteBuffer.allocate(maxOf(if (vf.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) vf.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 0, 2 shl 20).coerceAtMost(8 shl 20))
            val info = MediaCodec.BufferInfo()
            val rate = if (af != null && af.containsKey(MediaFormat.KEY_SAMPLE_RATE)) af.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
            val aclock = AudioClock()
            m.start(); started = true
            var cursor = 0L; var written = 0; var videoSamples = 0
            for (r in keep) {
                val ex = MediaExtractor()
                try {
                    ex.setDataSource(pfd.fileDescriptor, r.part.offset, r.part.length)
                    val pk = pick(ex)
                    ex.selectTrack(pk.video); if (pk.audio >= 0 && aOut >= 0) ex.selectTrack(pk.audio)
                    var base = -1L; var maxV = cursor; var minV = Long.MAX_VALUE; var vCount = 0
                    while (true) {
                        buf.clear()
                        val size = ex.readSampleData(buf, 0)
                        if (size < 0) break
                        val track = ex.sampleTrackIndex
                        val t = ex.sampleTime
                        if (t >= 0) {
                            if (base < 0) base = t
                            val rel = (t - base).coerceAtLeast(0) + cursor
                            if (track == pk.video) {
                                info.set(0, size, rel, if (ex.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                                m.writeSampleData(vOut, buf, info)
                                written++; videoSamples++; vCount++
                                if (rel > maxV) maxV = rel
                                if (rel < minV) minV = rel
                            } else if (track == pk.audio && aOut >= 0) written += writeAudio(m, aOut, buf, size, rel, info, aclock)
                        }
                        ex.advance()
                    }
                    // the next run starts where this one ended: its last picture's time plus one frame, or the end of its sound, whichever is later
                    val frameUs = if (vCount > 1) (maxV - minV) / (vCount - 1) else 33_333L
                    val vEnd = if (vCount > 0) maxV + frameUs else cursor
                    val aEnd = if (aclock.last >= 0) aclock.last + 1_000_000L * 1024 / rate else 0L
                    cursor = maxOf(vEnd, aEnd, cursor + 1)
                } finally { try { ex.release() } catch (e: Exception) { /* ignore */ } }
            }
            if (videoSamples == 0) throw IllegalStateException("no video samples")
            m.stop(); started = false; m.release(); muxer = null
            sink.sync()
            val len = sink.length()
            val uri = sink.publish() ?: throw IllegalStateException("no uri")
            out = null
            Log.i(Io.TAG, "remux ok (${keep.size} of ${j.parts.size} runs): ${j.size} -> $len bytes, $written samples")
            Health.log("remux", "ok", "${keep.size} of ${j.parts.size} runs, $written samples, ${cursor / 1000} ms")
            return StreamFinisher.Converted(uri, sink.displayName ?: name, "video/mp4", len)
        } catch (e: Throwable) {
            Log.w(Io.TAG, "remux of runs failed, keeping the .ts", e); Health.log("remux", "failed", e.toString().take(160))
            return null
        } finally {
            try { if (started) muxer?.stop() } catch (e: Exception) { /* a muxer that never got a sample throws here */ }
            try { muxer?.release() } catch (e: Exception) { /* ignore */ }
            try { out?.discard() } catch (e: Exception) { /* ignore */ }
            try { pfd.close() } catch (e: Exception) { /* ignore */ }
        }
    }

    /** One run (no discontinuity): the whole joined file read as it is. */
    private fun convertWhole(j: StreamFinisher.Joined): StreamFinisher.Converted? {
        val ex = MediaExtractor()
        var out: Sink? = null
        var muxer: MediaMuxer? = null
        var started = false
        try {
            ex.setDataSource(ctx, Uri.parse(j.uri), null)
            val pk = pick(ex)
            val vt = pk.video; val at = pk.audio
            if (vt < 0) return declined("no H.264 / H.265 picture in the stream")
            if (at < 0 && pk.unsupportedAudio) return declined("the sound is not AAC, the MP4 muxer cannot take it")
            val vf = ex.getTrackFormat(vt)
            val af = if (at >= 0) ex.getTrackFormat(at) else null
            if (af != null && !af.containsKey("csd-0")) return declined("AAC without its configuration") // cannot be put in an MP4
            val name = j.stem + ".mp4"
            val sink = sinks.create(name, "video/mp4", -1, j.priv, j.destKind).also { out = it }
            val fd = sink.descriptor() ?: run { sink.discard(); return null }
            val m = MediaMuxer(fd, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4).also { muxer = it }
            val vOut = m.addTrack(vf)
            val aOut = if (af != null) m.addTrack(af) else -1
            ex.selectTrack(vt); if (at >= 0) ex.selectTrack(at)
            val maxIn = maxOf(if (vf.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) vf.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 0, 2 shl 20).coerceAtMost(8 shl 20)
            val buf = ByteBuffer.allocate(maxIn)
            val info = MediaCodec.BufferInfo()
            m.start(); started = true
            var base = -1L
            var written = 0; var videoSamples = 0
            val aclock = AudioClock()
            while (true) {
                buf.clear()
                val size = ex.readSampleData(buf, 0)
                if (size < 0) break
                val track = ex.sampleTrackIndex
                val t = ex.sampleTime
                val target = when (track) { vt -> vOut; at -> aOut; else -> -1 }
                if (target >= 0 && t >= 0) {
                    if (base < 0) base = t
                    val rel = (t - base).coerceAtLeast(0)
                    if (track == at) written += writeAudio(m, target, buf, size, rel, info, aclock)
                    else {
                        info.set(0, size, rel, if (ex.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                        m.writeSampleData(target, buf, info)
                        written++; videoSamples++
                    }
                }
                ex.advance()
            }
            if (videoSamples == 0) throw IllegalStateException("no video samples")
            m.stop(); started = false; m.release(); muxer = null
            sink.sync()
            val len = sink.length()
            val uri = sink.publish() ?: throw IllegalStateException("no uri")
            out = null
            Log.i(Io.TAG, "remux ok: ${j.size} -> $len bytes, $written samples")
            Health.log("remux", "ok", "$written samples")
            return StreamFinisher.Converted(uri, sink.displayName ?: name, "video/mp4", len)
        } catch (e: Throwable) {
            Log.w(Io.TAG, "remux failed, keeping the .ts", e); Health.log("remux", "failed", e.toString().take(160))
            return null
        } finally {
            try { if (started) muxer?.stop() } catch (e: Exception) { /* a muxer that never got a sample throws here */ }
            try { muxer?.release() } catch (e: Exception) { /* ignore */ }
            try { out?.discard() } catch (e: Exception) { /* ignore */ }
            try { ex.release() } catch (e: Exception) { /* ignore */ }
        }
    }

    /**
     * Picture and sound that DASH (or an HLS master) keeps in two files become ONE MP4: both are demuxed and the samples are written in time order. Anything the muxer or the extractor
     * refuses returns null (the two files then stay as downloads of their own).
     */
    override fun merge(m: StreamFinisher.Merge): StreamFinisher.Converted? {
        if (!enoughRoom(m.size)) return null
        val vx = MediaExtractor(); val ax = MediaExtractor()
        var out: Sink? = null
        var muxer: MediaMuxer? = null
        var started = false
        try {
            vx.setDataSource(ctx, Uri.parse(m.videoUri), null)
            ax.setDataSource(ctx, Uri.parse(m.audioUri), null)
            var vt = -1; var at = -1
            for (i in 0 until vx.trackCount) if (vt < 0 && vx.getTrackFormat(i).getString(MediaFormat.KEY_MIME).let { it == "video/avc" || it == "video/hevc" }) vt = i
            for (i in 0 until ax.trackCount) if (at < 0 && ax.getTrackFormat(i).getString(MediaFormat.KEY_MIME) == "audio/mp4a-latm") at = i
            if (vt < 0 || at < 0) return null
            val vf = vx.getTrackFormat(vt); val af = ax.getTrackFormat(at)
            if (!af.containsKey("csd-0")) return null
            val name = m.stem + ".mp4"
            val sink = sinks.create(name, "video/mp4", -1, m.priv, m.destKind).also { out = it }
            val fd = sink.descriptor() ?: run { sink.discard(); return null }
            val mux = MediaMuxer(fd, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4).also { muxer = it }
            val vOut = mux.addTrack(vf); val aOut = mux.addTrack(af)
            vx.selectTrack(vt); ax.selectTrack(at)
            val vBuf = ByteBuffer.allocate(maxOf(if (vf.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) vf.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 0, 2 shl 20).coerceAtMost(8 shl 20))
            val aBuf = ByteBuffer.allocate(maxOf(if (af.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) af.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 0, 256 shl 10).coerceAtMost(4 shl 20))
            val info = MediaCodec.BufferInfo()
            val aclock = AudioClock()
            mux.start(); started = true
            val vBase = vx.sampleTime.coerceAtLeast(0); val aBase = ax.sampleTime.coerceAtLeast(0)
            val base = minOf(vBase, aBase)
            var videoSamples = 0
            while (true) {
                val vt0 = vx.sampleTime; val at0 = ax.sampleTime            // -1 when that extractor has no more samples
                if (vt0 < 0 && at0 < 0) break
                if (vt0 >= 0 && (at0 < 0 || vt0 <= at0)) {
                    vBuf.clear(); val size = vx.readSampleData(vBuf, 0)
                    if (size < 0) { vx.advance(); continue }
                    info.set(0, size, (vt0 - base).coerceAtLeast(0), if (vx.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                    mux.writeSampleData(vOut, vBuf, info); videoSamples++
                    vx.advance()
                } else {
                    aBuf.clear(); val size = ax.readSampleData(aBuf, 0)
                    if (size < 0) { ax.advance(); continue }
                    writeAudio(mux, aOut, aBuf, size, (at0 - base).coerceAtLeast(0), info, aclock, rawKeepsStreamTime = true)
                    ax.advance()
                }
            }
            if (videoSamples == 0) throw IllegalStateException("no video samples")
            mux.stop(); started = false; mux.release(); muxer = null
            sink.sync()
            val len = sink.length()
            val uri = sink.publish() ?: throw IllegalStateException("no uri")
            out = null
            Log.i(Io.TAG, "merge ok: ${m.size} -> $len bytes")
            return StreamFinisher.Converted(uri, sink.displayName ?: name, "video/mp4", len)
        } catch (e: Throwable) {
            Log.w(Io.TAG, "merge failed, keeping the two files", e)
            return null
        } finally {
            try { if (started) muxer?.stop() } catch (e: Exception) { /* ignore */ }
            try { muxer?.release() } catch (e: Exception) { /* ignore */ }
            try { out?.discard() } catch (e: Exception) { /* ignore */ }
            try { vx.release() } catch (e: Exception) { /* ignore */ }
            try { ax.release() } catch (e: Exception) { /* ignore */ }
        }
    }

    /**
     * The platform's TS extractor hands over AAC the way it sits in the stream: several ADTS frames (each with its header) per sample. An MP4 wants ONE raw frame per sample, so
     * every frame is cut out and given its own time on the exact 1024-sample grid ([AudioClock]). A sample that is not ADTS is written as it is. Returns how many samples were written.
     */
    private fun writeAudio(m: MediaMuxer, track: Int, buf: ByteBuffer, size: Int, timeUs: Long, info: MediaCodec.BufferInfo, clock: AudioClock, rawKeepsStreamTime: Boolean = false): Int {
        val frames = Adts.frames(buf.array(), buf.arrayOffset(), size)
        if (frames.isEmpty()) {
            // raw AAC (from an MP4): one frame per sample already, with the stream's own exact time
            buf.clear(); val t = if (rawKeepsStreamTime) maxOf(timeUs, clock.last + 1) else maxOf(timeUs, clock.last + 1); info.set(0, size, t, 0); m.writeSampleData(track, buf, info); clock.noteRaw(t); return 1
        }
        clock.sync(timeUs, frames[0].sampleRate)
        for (f in frames) {
            buf.clear()
            info.set(f.payloadOffset - buf.arrayOffset(), f.payloadLength, clock.next(f.sampleRate), 0)
            m.writeSampleData(track, buf, info)
        }
        return frames.size
    }

    /** The MP4 is about 95 % of the stream (the transport stream's own overhead is left out); the .ts stays until the MP4 is whole. */
    private fun roomNeeded(size: Long): Long = size / 20 * 19 + (24L shl 20)

    private fun enoughRoom(size: Long): Boolean = try {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        StatFs(dir.absolutePath).availableBytes > roomNeeded(size)
    } catch (e: Exception) { false }
}
