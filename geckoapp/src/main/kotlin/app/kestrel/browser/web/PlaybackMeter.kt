package app.kestrel.browser.web

import android.os.SystemClock
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener

/**
 * ANS60d (owner message 60: "see if there are any additional ways to make playback smoother or faster or more optimized for my hardware. I want it as optimized as possible"): what a film's playback REALLY did on
 * this tablet, measured the same way in the full-screen player and in the player inside the page: how long until it could play, how often and how long it stopped to wait for the network (a stall: buffering after the
 * first picture, not caused by a seek and not while paused), the link speed the player itself measured, the size of the picture it ended up with and how often that changed, the pictures it dropped, and how much was
 * buffered ahead when it ended. One line per film for Diagnostics and the emulator lab ("stats"), and the sums in [NativePlayerStats] for the Diagnostics screen: the numbers the next round of tuning needs.
 * Nothing is counted or logged for a private tab's film. Everything runs on the thread the player's listeners run on (the main thread).
 */
class PlaybackMeter(private val priv: Boolean, private val say: (String, String) -> Unit) : AnalyticsListener {
    private val startedAt = SystemClock.uptimeMillis()
    private var readyMs = -1L
    private var firstFrameAt = 0L
    private var wantPlay = true
    private var seeking = false
    private var stallStartedAt = 0L
    private var stalls = 0
    private var stalledMs = 0L
    private var longestStallMs = 0L
    private var videoBps = 0
    private var switches = 0
    private var linkBps = 0L
    private var dropped = 0
    private var finished = false

    /** The person picked another picture (or the cap changed): the wait that follows is the new picture arriving, not the network failing. */
    override fun onTrackSelectionParametersChanged(eventTime: AnalyticsListener.EventTime, trackSelectionParameters: androidx.media3.common.TrackSelectionParameters) {
        if (firstFrameAt != 0L) { seeking = true; endStall(SystemClock.uptimeMillis(), counted = false) }
    }

    override fun onPlayWhenReadyChanged(eventTime: AnalyticsListener.EventTime, playWhenReady: Boolean, reason: Int) {
        wantPlay = playWhenReady
        if (!playWhenReady) endStall(SystemClock.uptimeMillis(), counted = false)             // (a pause while waiting is not the network's fault)
    }

    override fun onPositionDiscontinuity(eventTime: AnalyticsListener.EventTime, oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) { seeking = true; endStall(SystemClock.uptimeMillis(), counted = false) }
    }

    override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
        val now = SystemClock.uptimeMillis()
        when (state) {
            Player.STATE_READY -> {
                if (readyMs < 0L) { readyMs = now - startedAt; say("ready", "playable $readyMs ms after the start") }
                endStall(now, counted = true); seeking = false
            }
            Player.STATE_BUFFERING -> if (firstFrameAt != 0L && !seeking && wantPlay && stallStartedAt == 0L) stallStartedAt = now
            else -> endStall(now, counted = false)
        }
    }

    private fun endStall(now: Long, counted: Boolean) {
        if (stallStartedAt == 0L) return
        val ms = now - stallStartedAt
        stallStartedAt = 0L
        if (counted && ms >= 200L) {                                                    // (a blink of buffering under 0.2 s is the decoder's, not the network's)
            stalls++; stalledMs += ms; if (ms > longestStallMs) longestStallMs = ms
            say("stall", "waited ${ms} ms for the network (the ${stalls}. time, ${stalledMs} ms in all)")
        }
    }

    override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, output: Any, renderTimeMs: Long) { if (firstFrameAt == 0L) firstFrameAt = SystemClock.uptimeMillis() }

    override fun onBandwidthEstimate(eventTime: AnalyticsListener.EventTime, totalLoadTimeMs: Int, totalBytesLoaded: Long, bitrateEstimate: Long) { linkBps = bitrateEstimate }

    override fun onVideoInputFormatChanged(eventTime: AnalyticsListener.EventTime, format: Format, decoderReuseEvaluation: DecoderReuseEvaluation?) {
        val bps = if (format.averageBitrate > 0) format.averageBitrate else format.bitrate
        if (bps > 0) { if (videoBps != 0 && bps != videoBps) switches++; videoBps = bps }
    }

    override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) { dropped += droppedFrames }

    /** The film ended: one line, and the sums for Diagnostics. Call before the player is released. */
    fun finish(p: ExoPlayer) {
        if (finished) return
        finished = true
        endStall(SystemClock.uptimeMillis(), counted = false)
        var ahead = 0L
        try { ahead = p.totalBufferedDuration } catch (e: Throwable) { /* gone */ }
        say("stats", "playable after ${if (readyMs >= 0) "$readyMs ms" else "never"}, ${stalls} stall(s) of ${stalledMs} ms in all (longest ${longestStallMs} ms), link estimate ${mbit(linkBps)}, picture ${mbit(videoBps.toLong())}, ${switches} size change(s), $dropped dropped, ${ahead / 1000} s buffered ahead at the end")
        if (priv) return
        NativePlayerStats.readyMs = readyMs
        NativePlayerStats.stalls += stalls; NativePlayerStats.stalledMs += stalledMs
        if (linkBps > 0L) NativePlayerStats.linkBps = linkBps
        if (videoBps > 0) NativePlayerStats.videoBps = videoBps
        NativePlayerStats.switches += switches
    }

    private fun mbit(bps: Long): String = if (bps <= 0L) "-" else { val t = (bps + 50_000L) / 100_000L; "${t / 10}.${t % 10} Mbit/s" }
}
