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
        if (j.container != "ts" || !enoughRoom(j.size)) return null
        val ex = MediaExtractor()
        var out: Sink? = null
        var muxer: MediaMuxer? = null
        var started = false
        try {
            ex.setDataSource(ctx, Uri.parse(j.uri), null)
            var vt = -1; var at = -1
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                when (f.getString(MediaFormat.KEY_MIME)) {
                    "video/avc", "video/hevc" -> if (vt < 0) vt = i
                    "audio/mp4a-latm" -> if (at < 0) at = i
                    else -> return null // an audio codec (MP3, AC-3...) or data track the MP4 muxer cannot take: keep the .ts
                }
            }
            if (vt < 0) return null
            val vf = ex.getTrackFormat(vt)
            val af = if (at >= 0) ex.getTrackFormat(at) else null
            if (af != null && !af.containsKey("csd-0")) return null // AAC without its configuration cannot be put in an MP4
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
            return StreamFinisher.Converted(uri, sink.displayName ?: name, "video/mp4", len)
        } catch (e: Throwable) {
            Log.w(Io.TAG, "remux failed, keeping the .ts", e)
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

    private fun enoughRoom(size: Long): Boolean = try {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        StatFs(dir.absolutePath).availableBytes > size + (64L shl 20)
    } catch (e: Exception) { false }
}
