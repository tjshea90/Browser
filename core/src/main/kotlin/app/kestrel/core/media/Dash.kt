package app.kestrel.core.media

import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * MPEG-DASH manifest (MPD) reader for static (on-demand), unencrypted streams: lists the representations (quality variants) and turns one of them into the same segment plan the HLS reader
 * produces ([Hls.Media]), so the stream engine downloads both the same way. Supports SegmentTemplate (number and time based, with SegmentTimeline), SegmentList, and a single-file
 * Representation (SegmentBase / plain BaseURL). Refuses dynamic (live) manifests, ContentProtection (DRM) and multi-period manifests. Design: research/VIDEO_DOWNLOADER.md section 5.
 */
object Dash {
    class Rep(
        val id: String,
        val bandwidth: Long,
        val width: Int,
        val height: Int,
        val codecs: String,
        val mimeType: String,
        val frameRate: Double,
        val audio: Boolean,
        val lang: String?,
        /** The segment plan of this representation (or null when it cannot be built). */
        val media: Hls.Media?,
    ) {
        /** "1080p", "720p60", "Audio 128 kbps". */
        val label: String get() = when {
            audio -> "Audio"
            height > 0 -> "${height}p" + (if (frameRate >= 50.0) frameRate.toInt().toString() else "")
            else -> "Video"
        }
        val videoCodec: String get() = when {
            codecs.startsWith("avc") -> "H.264"
            codecs.startsWith("hvc") || codecs.startsWith("hev") -> "H.265"
            codecs.startsWith("vp09") || codecs.startsWith("vp9") -> "VP9"
            codecs.startsWith("av01") -> "AV1"
            else -> ""
        }
        /** Bytes per second the manifest promises. */
        val bytesPerSecond: Long get() = bandwidth / 8
        /** This representation can sit in an MP4 (what Android's muxer can write next to AAC): H.264/H.265 video or AAC audio in an MP4 container. */
        val mp4Friendly: Boolean get() = mimeType.endsWith("mp4") && (audio && (codecs.isEmpty() || codecs.startsWith("mp4a")) || !audio && (videoCodec == "H.264" || videoCodec == "H.265" || codecs.isEmpty()))
    }

    class Manifest(val durationMs: Long, val reps: List<Rep>, val problem: Hls.Problem?) {
        val video: List<Rep> get() = reps.filter { !it.audio }.sortedWith(compareByDescending<Rep> { it.height }.thenByDescending { it.bandwidth })
        val audio: List<Rep> get() = reps.filter { it.audio }.sortedByDescending { it.bandwidth }
    }

    /** @return null when [text] is not an MPD. */
    fun parse(text: String, mpdUrl: String): Manifest? {
        val root = try {
            val f = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            try { f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) } catch (e: Exception) { /* not every parser knows it; none of them fetches external entities by default here */ }
            f.isNamespaceAware = false
            f.newDocumentBuilder().parse(org.xml.sax.InputSource(java.io.StringReader(text))).documentElement
        } catch (e: Exception) { return null }
        if (root.tagName.substringAfter(':') != "MPD") return null
        val isLive = root.getAttribute("type").equals("dynamic", true)
        val total = parseDuration(root.getAttribute("mediaPresentationDuration"))
        val mpdBase = baseOf(root, Hls.resolve(mpdUrl, ""), mpdUrl)
        val periods = children(root, "Period")
        if (periods.isEmpty()) return null
        var problem: Hls.Problem? = null
        if (isLive) problem = Hls.Problem.LIVE
        else if (periods.size > 1) problem = Hls.Problem.UNSUPPORTED
        val period = periods[0]
        val periodMs = parseDuration(period.getAttribute("duration")).takeIf { it > 0 } ?: total
        val periodBase = baseOf(period, mpdBase, mpdUrl)
        val reps = ArrayList<Rep>()
        for (aset in children(period, "AdaptationSet")) {
            val asBase = baseOf(aset, periodBase, mpdUrl)
            if (children(aset, "ContentProtection").isNotEmpty()) problem = problem ?: Hls.Problem.DRM
            for (r in children(aset, "Representation")) {
                if (children(r, "ContentProtection").isNotEmpty()) problem = problem ?: Hls.Problem.DRM
                val mime = r.getAttribute("mimeType").ifEmpty { aset.getAttribute("mimeType") }
                val ct = aset.getAttribute("contentType").ifEmpty { mime.substringBefore('/') }
                val codecs = r.getAttribute("codecs").ifEmpty { aset.getAttribute("codecs") }
                val audio = ct == "audio" || (ct.isEmpty() && mime.startsWith("audio"))
                if (ct != "audio" && ct != "video" && !mime.startsWith("video") && !mime.startsWith("audio")) continue // subtitles, thumbnails
                val rBase = baseOf(r, asBase, mpdUrl)
                val id = r.getAttribute("id")
                val bw = r.getAttribute("bandwidth").toLongOrNull() ?: 0L
                val media = try { plan(aset, r, id, bw, rBase, periodMs, mime, problem) } catch (e: Exception) { null }
                reps.add(Rep(id, bw, r.getAttribute("width").toIntOrNull() ?: aset.getAttribute("width").toIntOrNull() ?: 0, r.getAttribute("height").toIntOrNull() ?: aset.getAttribute("height").toIntOrNull() ?: 0,
                    codecs, mime, frameRate(r.getAttribute("frameRate").ifEmpty { aset.getAttribute("frameRate") }), audio, aset.getAttribute("lang").takeIf { it.isNotEmpty() }, media))
            }
        }
        return Manifest(periodMs, reps, problem)
    }

    // ------------------------------------------------------------------------------------------ one representation's segments

    private fun plan(aset: Element, rep: Element, id: String, bandwidth: Long, baseUrl: String, periodMs: Long, mime: String, problem: Hls.Problem?): Hls.Media? {
        val st = child(rep, "SegmentTemplate") ?: child(aset, "SegmentTemplate")
        val sl = child(rep, "SegmentList") ?: child(aset, "SegmentList")
        val sb = child(rep, "SegmentBase") ?: child(aset, "SegmentBase")
        val format = if (mime.contains("webm")) "webm" else "mp4"
        val segs = ArrayList<Hls.Seg>()
        var init: Hls.Init? = null
        when {
            st != null -> {
                val timescale = st.getAttribute("timescale").toLongOrNull() ?: 1L
                val startNumber = st.getAttribute("startNumber").toLongOrNull() ?: 1L
                val mediaT = st.getAttribute("media")
                val initT = st.getAttribute("initialization")
                if (initT.isNotEmpty()) init = Hls.Init(Hls.resolve(baseUrl, fill(initT, id, bandwidth, 0, 0)), -1, -1)
                val timeline = child(st, "SegmentTimeline")
                var number = startNumber
                if (timeline != null) {
                    var t = 0L
                    var first = true
                    for (s in children(timeline, "S")) {
                        val st0 = s.getAttribute("t").toLongOrNull()
                        if (st0 != null) t = st0 else if (first) t = 0
                        first = false
                        val d = s.getAttribute("d").toLongOrNull() ?: continue
                        val r = s.getAttribute("r").toIntOrNull() ?: 0
                        val repeats = if (r >= 0) r else maxOf(0L, ((periodMs * timescale / 1000 - t) / maxOf(d, 1)).toInt() - 1).toInt()
                        for (i in 0..repeats) {
                            segs.add(Hls.Seg(Hls.resolve(baseUrl, fill(mediaT, id, bandwidth, number, t)), d * 1000 / timescale, -1, -1, -1, number, false))
                            t += d; number++
                            if (segs.size > MAX_SEGMENTS) return null
                        }
                    }
                } else {
                    val d = st.getAttribute("duration").toLongOrNull() ?: return null
                    val count = if (periodMs > 0) (((periodMs * timescale / 1000) + d - 1) / d).toInt() else return null
                    if (count > MAX_SEGMENTS) return null
                    for (i in 0 until count) segs.add(Hls.Seg(Hls.resolve(baseUrl, fill(mediaT, id, bandwidth, number, i * d)), d * 1000 / timescale, -1, -1, -1, number++, false))
                }
            }
            sl != null -> {
                val timescale = sl.getAttribute("timescale").toLongOrNull() ?: 1L
                val d = sl.getAttribute("duration").toLongOrNull() ?: 0L
                child(sl, "Initialization")?.let { i -> init = rangeInit(i, baseUrl) }
                var n = 0L
                for (u in children(sl, "SegmentURL")) {
                    val media = u.getAttribute("media").ifEmpty { baseUrl }
                    val (from, len) = range(u.getAttribute("mediaRange"))
                    segs.add(Hls.Seg(Hls.resolve(baseUrl, media), if (timescale > 0) d * 1000 / timescale else 0, from, len, -1, n++, false))
                    if (segs.size > MAX_SEGMENTS) return null
                }
            }
            else -> {
                // one file: the whole Representation is its BaseURL (an initialisation range, if any, is part of the file)
                if (child(rep, "BaseURL") == null && child(aset, "BaseURL") == null && sb == null) return null
                segs.add(Hls.Seg(baseUrl, periodMs, -1, -1, -1, 0, false))
            }
        }
        if (segs.isEmpty()) return null
        return Hls.Media(segs, emptyList(), init, true, 0, 0, problem, format)
    }

    private fun rangeInit(e: Element, baseUrl: String): Hls.Init {
        val (from, len) = range(e.getAttribute("range"))
        return Hls.Init(Hls.resolve(baseUrl, e.getAttribute("sourceURL").ifEmpty { baseUrl }), from, len)
    }

    /** "0-862" -> (0, 863); "" -> (-1, -1). */
    private fun range(r: String): Pair<Long, Long> {
        if (r.isEmpty()) return -1L to -1L
        val a = r.substringBefore('-').toLongOrNull() ?: return -1L to -1L
        val b = r.substringAfter('-', "").toLongOrNull() ?: return -1L to -1L
        return a to (b - a + 1)
    }

    /** $RepresentationID$, $Bandwidth$, $Number$, $Time$ (with an optional printf width, "$Number%05d$") and "$$". */
    internal fun fill(template: String, id: String, bandwidth: Long, number: Long, time: Long): String {
        val sb = StringBuilder()
        var i = 0
        while (i < template.length) {
            val c = template[i]
            if (c != '$') { sb.append(c); i++; continue }
            val end = template.indexOf('$', i + 1)
            if (end < 0) { sb.append(template.substring(i)); break }
            val tok = template.substring(i + 1, end)
            if (tok.isEmpty()) sb.append('$')
            else {
                val name = tok.substringBefore('%')
                val fmt = if (tok.contains('%')) tok.substringAfter('%') else ""
                val v: String = when (name) {
                    "RepresentationID" -> id
                    "Bandwidth" -> pad(bandwidth, fmt)
                    "Number" -> pad(number, fmt)
                    "Time" -> pad(time, fmt)
                    else -> "$$tok$"
                }
                sb.append(v)
            }
            i = end + 1
        }
        return sb.toString()
    }

    private fun pad(v: Long, fmt: String): String {
        val m = Regex("""0?(\d+)d""").matchEntire(fmt) ?: return v.toString()
        return v.toString().padStart(m.groupValues[1].toInt(), '0')
    }

    // ------------------------------------------------------------------------------------------ small helpers

    private const val MAX_SEGMENTS = 20_000

    /** ISO 8601 duration ("PT1H2M3.5S", "P0DT12M") in milliseconds; 0 when absent or unreadable. */
    fun parseDuration(s: String): Long {
        val m = Regex("""P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?)?""").matchEntire(s.trim()) ?: return 0L
        val d = m.groupValues[1].toLongOrNull() ?: 0; val h = m.groupValues[2].toLongOrNull() ?: 0; val min = m.groupValues[3].toLongOrNull() ?: 0; val sec = m.groupValues[4].toDoubleOrNull() ?: 0.0
        return ((((d * 24 + h) * 60 + min) * 60) * 1000 + (sec * 1000).toLong())
    }

    private fun frameRate(s: String): Double { if (s.isEmpty()) return 0.0; val n = s.substringBefore('/').toDoubleOrNull() ?: return 0.0; val d = s.substringAfter('/', "1").toDoubleOrNull() ?: 1.0; return if (d > 0) n / d else n }

    private fun baseOf(e: Element, parent: String, mpdUrl: String): String {
        val b = child(e, "BaseURL")?.textContent?.trim().orEmpty()
        return if (b.isEmpty()) parent else Hls.resolve(parent.ifEmpty { mpdUrl }, b)
    }

    private fun children(e: Element, name: String): List<Element> {
        val out = ArrayList<Element>()
        var n: Node? = e.firstChild
        while (n != null) { if (n is Element && n.tagName.substringAfter(':') == name) out.add(n); n = n.nextSibling }
        return out
    }

    private fun child(e: Element, name: String): Element? = children(e, name).firstOrNull()
}
