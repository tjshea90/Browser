package app.kestrel.core.filter

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** TEMPORARY probe (not committed): which video-player ad SDK requests does the bundled snapshot block? */
class ZzProbeTest {
    @Test fun probe() {
        val kfs = File("../app/src/main/assets/filters/default.kfs"); val pslFile = File("../app/src/main/assets/psl.txt")
        assumeTrue(kfs.isFile && pslFile.isFile)
        val fs = FilterSetCodec.read(kfs.readBytes()); val psl = Psl.parse(pslFile.readLines().asSequence())
        val s = RequestType.SCRIPT; val x = RequestType.XHR; val sub = RequestType.SUBDOCUMENT; val img = RequestType.IMAGE; val media = RequestType.MEDIA
        val cases = listOf(
            Triple("https://imasdk.googleapis.com/js/sdkloader/ima3.js", "news.example", s),
            Triple("https://imasdk.googleapis.com/js/core/bridge3.595.0_en.html", "news.example", sub),
            Triple("https://pubads.g.doubleclick.net/gampad/ads?iu=/123/video&sz=640x480", "news.example", x),
            Triple("https://securepubads.g.doubleclick.net/tag/js/gpt.js", "news.example", s),
            Triple("https://www.googletagservices.com/tag/js/gpt.js", "news.example", s),
            Triple("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js", "news.example", s),
            Triple("https://vast.adsrvr.org/x", "news.example", x),
            Triple("https://ssl.p.jwpcdn.com/player/v/8.8.2/jwplayer.js", "news.example", s),
            Triple("https://cdn.jwplayer.com/libraries/abc.js", "news.example", s),
            Triple("https://content.jwplatform.com/manifests/abc.m3u8", "news.example", media),
            Triple("https://jwpltx.com/v1/jwplayer6/ping.gif?e=s", "news.example", img),
            Triple("https://imasdk.googleapis.com/js/sdkloader/ima3_dai.js", "news.example", s),
            Triple("https://s.ytimg.com/yts/jsbin/player.js", "www.youtube.com", s),
            Triple("https://www.youtube.com/api/stats/ads?ver=2", "www.youtube.com", x),
            Triple("https://static.vidazoo.com/basev/vwpt.js", "news.example", s),
            Triple("https://tags.tiqcdn.com/utag/x/main/prod/utag.js", "news.example", s),
            Triple("https://www.googletagmanager.com/gtm.js?id=GTM-ABC", "news.example", s),
            Triple("https://connect.facebook.net/en_US/fbevents.js", "news.example", s),
            Triple("https://cdn.plyr.io/3.7.8/plyr.js", "news.example", s),
            Triple("https://vjs.zencdn.net/7.20.3/video.min.js", "news.example", s),
            Triple("https://players.brightcove.net/123/default_default/index.min.js", "news.example", s),
            Triple("https://static.doubleverify.com/dvtp_src.js", "news.example", s),
            Triple("https://pixel.adsafeprotected.com/services/pub?anid=1", "news.example", img),
            Triple("https://cdn.jsdelivr.net/npm/hls.js@1/dist/hls.min.js", "news.example", s),
        )
        for ((u, p, t) in cases) { val v = fs.check(u, p, t, psl); println("PROBE " + (if (FilterSet.isBlocked(v)) "BLOCK " else "allow ") + u) }
    }
}
