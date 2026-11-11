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
            while (true) {
                buf.clear()
                val size = ex.readSampleData(buf, 0)
                if (size < 0) break
                val track = ex.sampleTrackIndex
                val t = ex.sampleTime
                val target = when (track) { vt -> vOut; at -> aOut; else -> -1 }
                if (target >= 0 && t >= 0) {
                    if (base < 0) base = t
                    info.set(0, size, (t - base).coerceAtLeast(0), if (ex.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                    m.writeSampleData(target, buf, info)
                    written++; if (track == vt) videoSamples++
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

    private fun enoughRoom(size: Long): Boolean = try {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        StatFs(dir.absolutePath).availableBytes > size + (64L shl 20)
    } catch (e: Exception) { false }
}
