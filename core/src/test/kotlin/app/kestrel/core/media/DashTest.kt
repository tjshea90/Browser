package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashTest {
    private val url = "https://cdn.example.com/v/abc/manifest.mpd?sig=1"

    private val timeline = """<?xml version="1.0" encoding="UTF-8"?>
<MPD xmlns="urn:mpeg:dash:schema:mpd:2011" type="static" mediaPresentationDuration="PT20S" minBufferTime="PT2S">
  <Period id="0">
    <AdaptationSet contentType="video" segmentAlignment="true" mimeType="video/mp4">
      <SegmentTemplate timescale="90000" initialization="vid/$RepresentationID$/init.mp4" media="vid/$RepresentationID$/seg-$Number$.m4s" startNumber="1">
        <SegmentTimeline><S t="0" d="360000" r="4"/></SegmentTimeline>
      </SegmentTemplate>
      <Representation id="v720" bandwidth="1500000" width="1280" height="720" codecs="avc1.64001f" frameRate="30"/>
      <Representation id="v360" bandwidth="500000" width="640" height="360" codecs="avc1.4d401e" frameRate="30"/>
    </AdaptationSet>
    <AdaptationSet contentType="audio" lang="en" mimeType="audio/mp4">
      <SegmentTemplate timescale="44100" initialization="aud/init.mp4" media="aud/seg-$Number%05d$.m4s">
        <SegmentTimeline><S t="0" d="176400" r="4"/></SegmentTimeline>
      </SegmentTemplate>
      <Representation id="a1" bandwidth="96000" codecs="mp4a.40.2" audioSamplingRate="44100"/>
    </AdaptationSet>
  </Period>
</MPD>"""

    @Test fun templateWithATimelineBecomesASegmentPlan() {
        val m = Dash.parse(timeline, url)!!
        assertNull(m.problem)
        assertEquals(20_000L, m.durationMs)
        assertEquals(listOf("v720", "v360"), m.video.map { it.id })                          // biggest picture first
        assertEquals("a1", m.audio.single().id)
        val v = m.video[0]
        assertEquals("720p30".replace("30", ""), v.label); assertEquals("H.264", v.videoCodec); assertTrue(v.mp4Friendly)
        val p = v.media!!
        assertEquals("https://cdn.example.com/v/abc/vid/v720/init.mp4", p.init?.url)
        assertEquals(5, p.segments.size)                                                      // r="4" means four MORE after the first
        assertEquals("https://cdn.example.com/v/abc/vid/v720/seg-1.m4s", p.segments[0].url)
        assertEquals("https://cdn.example.com/v/abc/vid/v720/seg-5.m4s", p.segments[4].url)
        assertEquals(4000L, p.segments[0].durationMs); assertEquals(20_000L, p.durationMs)
        assertEquals("mp4", p.container)
        val a = m.audio[0].media!!
        assertEquals("https://cdn.example.com/v/abc/aud/seg-00003.m4s", a.segments[2].url)    // $Number%05d$
        assertTrue(m.audio[0].mp4Friendly)
    }

    @Test fun templateWithADurationCountsTheSegments() {
        val mpd = """<MPD type="static" mediaPresentationDuration="PT25S"><Period><AdaptationSet mimeType="video/mp4">
            <SegmentTemplate timescale="1000" duration="10000" media="s-${'$'}Number${'$'}.m4s" initialization="init.mp4" startNumber="0"/>
            <Representation id="x" bandwidth="800000" width="640" height="360" codecs="avc1.42c01e"/></AdaptationSet></Period></MPD>"""
        val p = Dash.parse(mpd, "https://h.test/d/m.mpd")!!.reps[0].media!!
        assertEquals(3, p.segments.size)                                                      // 25 s / 10 s rounded up
        assertEquals(listOf("https://h.test/d/s-0.m4s", "https://h.test/d/s-1.m4s", "https://h.test/d/s-2.m4s"), p.segments.map { it.url })
    }

    @Test fun segmentListWithByteRanges() {
        val mpd = """<MPD type="static" mediaPresentationDuration="PT12S"><BaseURL>https://media.test/files/</BaseURL><Period><AdaptationSet mimeType="video/mp4"><Representation id="r" bandwidth="900000" height="480" width="854" codecs="avc1.42c01e">
            <BaseURL>movie.mp4</BaseURL>
            <SegmentList timescale="1000" duration="6000"><Initialization range="0-899"/><SegmentURL mediaRange="900-100899"/><SegmentURL mediaRange="100900-210899"/></SegmentList>
            </Representation></AdaptationSet></Period></MPD>"""
        val p = Dash.parse(mpd, "https://x.test/a.mpd")!!.reps[0].media!!
        assertEquals("https://media.test/files/movie.mp4", p.init?.url); assertEquals(0L, p.init?.from); assertEquals(900L, p.init?.len)
        assertEquals(listOf(900L, 100900L), p.segments.map { it.from }); assertEquals(listOf(100_000L, 110_000L), p.segments.map { it.len })
        assertEquals(900L + 100_000 + 110_000, p.exactBytes)
    }

    @Test fun oneFileRepresentation() {
        val mpd = """<MPD type="static" mediaPresentationDuration="PT1M"><Period><AdaptationSet mimeType="video/mp4"><Representation id="1" bandwidth="1000000" height="720" width="1280" codecs="avc1.64001f">
            <BaseURL>https://cdn.test/whole.mp4</BaseURL><SegmentBase indexRange="800-1400"><Initialization range="0-799"/></SegmentBase></Representation></AdaptationSet></Period></MPD>"""
        val p = Dash.parse(mpd, "https://x.test/a.mpd")!!.reps[0].media!!
        assertEquals(1, p.segments.size); assertEquals("https://cdn.test/whole.mp4", p.segments[0].url); assertNull(p.init)   // the file already holds its own init section
    }

    @Test fun liveDrmAndMultiPeriodAreFlagged() {
        assertEquals(Hls.Problem.LIVE, Dash.parse("""<MPD type="dynamic" availabilityStartTime="2026-01-01T00:00:00Z"><Period><AdaptationSet mimeType="video/mp4"><Representation id="1" bandwidth="1"/></AdaptationSet></Period></MPD>""", url)!!.problem)
        val drm = """<MPD type="static" mediaPresentationDuration="PT10S"><Period><AdaptationSet mimeType="video/mp4"><ContentProtection schemeIdUri="urn:uuid:edef8ba9-79d6-4ace-a3c8-27dcd51d21ed"/><Representation id="1" bandwidth="1"><BaseURL>a.mp4</BaseURL></Representation></AdaptationSet></Period></MPD>"""
        assertEquals(Hls.Problem.DRM, Dash.parse(drm, url)!!.problem)
        val two = """<MPD type="static" mediaPresentationDuration="PT20S"><Period duration="PT10S"><AdaptationSet mimeType="video/mp4"><Representation id="1" bandwidth="1"><BaseURL>a.mp4</BaseURL></Representation></AdaptationSet></Period><Period><AdaptationSet mimeType="video/mp4"><Representation id="1" bandwidth="1"><BaseURL>b.mp4</BaseURL></Representation></AdaptationSet></Period></MPD>"""
        assertEquals(Hls.Problem.UNSUPPORTED, Dash.parse(two, url)!!.problem)
    }

    @Test fun notAnMpd() {
        assertNull(Dash.parse("<html><body>no</body></html>", url))
        assertNull(Dash.parse("#EXTM3U\n#EXTINF:1,\na.ts", url))
        assertNull(Dash.parse("", url))
        assertFalse(Dash.parse(timeline, url)!!.reps.isEmpty())
        assertNotNull(Dash.parse(timeline.replace(" xmlns=\"urn:mpeg:dash:schema:mpd:2011\"", ""), url))
    }

    @Test fun templatesAndDurations() {
        assertEquals("a/720/00042/9000.m4s", Dash.fill("a/${'$'}RepresentationID${'$'}/${'$'}Number%05d${'$'}/${'$'}Time${'$'}.m4s", "720", 1, 42, 9000))
        assertEquals("cost\$5", Dash.fill("cost\$\$5", "x", 1, 1, 1))
        assertEquals(3_723_500L, Dash.parseDuration("PT1H2M3.5S"))
        assertEquals(90_000L, Dash.parseDuration("PT1M30S"))
        assertEquals(0L, Dash.parseDuration(""))
    }
}
