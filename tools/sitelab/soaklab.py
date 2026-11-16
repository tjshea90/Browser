#!/usr/bin/env python3
"""SOAK1 lab server (the GitHub emulator lab, tools/emu/soak.py): python3 -I tools/sitelab/soaklab.py <port> <outdir>

Serves, on one port (the emulator reaches it with `adb reverse tcp:<port> tcp:<port>` and http://localhost:<port>/):
  /                      index of everything below
  /page/text|images|iframes|form   synthetic pages of different weight (a long article, 40 small pictures, three frames, a form)
  /video/<name>[?src=URL]          a player page: <video> + a reporter that sends every media event and getVideoPlaybackQuality() to /report
                                   name = landscape | portrait | big (clips made with ffmpeg at start) ; ?src= plays a PUBLIC address instead
  /fs                              the full-screen lab (FS1): a wide video, buttons "full screen", "lock landscape", "unlock"; reports fullscreenchange / orientation
  /fsbox?kind=light|shadow&nobar=1&chrome=1 the full-screen BOX lab (FSB1 / ROT1, owner message 46): a player box (in a shadow root like finance.yahoo.com's, or plain) that goes full screen as a box, with a red strip on its bottom edge
  /dl/<n>mb.bin  /dl/slow.bin  /dl/page   downloads of known size (the last page links to them)
  /report?...            one line of JSON per call -> <outdir>/report.jsonl  (what the pages saw: the soak driver reads it)
  /health                "ok"
Environment: LAB_RATE_KBPS (clips and downloads are served at most this fast, KB/s) and LAB_DELAY_MS (a pause before every answer) shape the lab like the owner's hotspot; both default to 0 = off.
Nothing here is part of the app and nothing it writes belongs in the repository (keep <outdir> outside the repo). Clips need ffmpeg; without it the video pages show an error and say so.
"""
import http.server, json, os, shutil, socketserver, subprocess, sys, threading, time, urllib.parse

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8091
OUT = os.path.abspath(sys.argv[2]) if len(sys.argv) > 2 else os.path.abspath("soaklab-out")
os.makedirs(OUT, exist_ok=True)
REPORT = os.path.join(OUT, "report.jsonl")
LOCK = threading.Lock()
# The owner's hotspot (research/EMU_LAB.md): LAB_RATE_KBPS = how fast the lab serves its clips and downloads (0 = full speed), LAB_DELAY_MS = one round trip before each answer.
# The emulator's own network shaping does not touch what comes through `adb reverse`, so the lab server shapes its own answers (the workflow sets both when net = hotspot).
RATE = int(os.environ.get("LAB_RATE_KBPS", "0") or 0) * 1024
DELAY = int(os.environ.get("LAB_DELAY_MS", "0") or 0) / 1000.0


def pace(sent, t0, rate):
    """Sleep so that `sent` bytes since t0 never go faster than `rate` bytes a second (0 = no limit)."""
    if rate:
        ahead = sent / float(rate) - (time.time() - t0)
        if ahead > 0:
            time.sleep(ahead)
CLIPS = {
    "landscape": ("640x360", 24, "landscape.mp4"),
    "portrait": ("360x640", 24, "portrait.mp4"),
    "big": ("1280x720", 20, "big.mp4"),
}


def make_clips():
    if not shutil.which("ffmpeg"):
        print("soaklab: ffmpeg not found, no clips", flush=True)
        return
    for name, (size, secs, fn) in CLIPS.items():
        path = os.path.join(OUT, fn)
        if os.path.exists(path):
            continue
        w, h = size.split("x")
        # H.264 (the one codec this tablet decodes in hardware) + AAC, an on-screen counter so a frozen picture is visible in a screenshot
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-f", "lavfi", "-i", "testsrc2=duration=%d:size=%s:rate=25" % (secs, size),
                        "-f", "lavfi", "-i", "sine=frequency=440:duration=%d" % secs, "-c:v", "libx264", "-preset", "veryfast", "-profile:v", "main",
                        "-pix_fmt", "yuv420p", "-b:v", "1500k" if name == "big" else "600k", "-g", "50", "-c:a", "aac", "-b:a", "64k",
                        "-movflags", "+faststart", path], check=False)
        print("soaklab: clip %s %s" % (name, "ok" if os.path.exists(path) else "FAILED"), flush=True)
    tone = os.path.join(OUT, "tone.m4a")          # SND1: sound without a picture
    if not os.path.exists(tone):
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-f", "lavfi", "-i", "sine=frequency=440:duration=120", "-c:a", "aac", "-b:a", "64k", "-movflags", "+faststart", tone], check=False)
        print("soaklab: tone %s" % ("ok" if os.path.exists(tone) else "FAILED"), flush=True)


def page_audible(kind):
    """SND1 (owner message 45): a page that starts SOUND when its big button is tapped (a real tap is the user gesture the engine wants), as a <video> or as an <audio>, and reports every media event
    and the page's visibility to /report so the driver can see what happened when the app was left."""
    el = ("<video id=v src='/clip/landscape.mp4' loop playsinline style='width:100%;max-height:35vh;background:#000'></video>" if kind == "video"
          else "<audio id=v src='/clip/tone.m4a' loop></audio>")
    return (("<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'><title>sound lab %s</title>"
             "<body style='margin:0;font:16px sans-serif'><h3 style='margin:10px'>sound lab %s</h3>"
             "<button id=go style='display:block;width:90%%;height:120px;margin:20px auto;font-size:28px'>Play with sound</button>%s"
             "<script>var v=document.getElementById('v');function send(ev){try{new Image().src='/report?kind=snd&ev='+ev+'&paused='+(v.paused?1:0)+'&ct='+v.currentTime.toFixed(1)+'&vis='+document.visibilityState+'&t='+Date.now();}catch(e){}}"
             "['play','playing','pause','ended','volumechange','emptied'].forEach(function(e){v.addEventListener(e,function(){send(e);});});"
             "document.addEventListener('visibilitychange',function(){send('vis');});"
             "document.getElementById('go').onclick=function(){v.muted=false;v.volume=1;var r=v.play();if(r&&r.then){r.then(function(){send('started');},function(){send('rejected');});}};"
             "setInterval(function(){send('tick');},2000);send('page');</script>") % (kind, kind, el)).encode()


LOREM = ("Kestrel is a small private browser made for one tablet. It blocks advertisements and trackers, keeps the pages light, and tries hard to stay out of the way. "
         "A page like this one exists only to be scrolled, measured and forgotten: it has headings, paragraphs, a list, a table and a few links, but no script of its own. ")


def page_text():
    parts = ["<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'><title>Soak text page</title>",
             "<body style='font:17px/1.55 sans-serif;margin:0 14px;background:#fff;color:#111'><h1>A long article</h1>"]
    for i in range(1, 41):
        parts.append("<h2>Section %d</h2><p>%s</p><p>%s</p>" % (i, LOREM * 3, LOREM * 2))
        if i % 8 == 0:
            parts.append("<table border=1 cellpadding=6><tr><th>a<th>b<th>c</tr>" + "".join("<tr><td>%d<td>%d<td>%d</tr>" % (i, j, i * j) for j in range(1, 8)) + "</table>")
        if i % 10 == 0:
            parts.append("<ul>" + "".join("<li><a href='/page/images?from=%d'>link %d</a></li>" % (i, j) for j in range(8)) + "</ul>")
    parts.append("<p id=end>the end</p>")
    return "".join(parts).encode()


def page_images():
    parts = ["<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'><title>Soak images page</title>",
             "<body style='margin:0;background:#222;color:#eee;font:16px sans-serif'><h1 style='margin:10px'>40 pictures</h1>"]
    for i in range(40):
        parts.append("<img loading=lazy width=280 height=210 style='margin:6px' src='/img/%d.svg' alt='picture %d'>" % (i, i))
    return "".join(parts).encode()


def svg(n):
    hue = (n * 47) % 360
    return ("<svg xmlns='http://www.w3.org/2000/svg' width='280' height='210'><defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'>"
            "<stop offset='0' stop-color='hsl(%d,70%%,45%%)'/><stop offset='1' stop-color='hsl(%d,70%%,20%%)'/></linearGradient></defs>"
            "<rect width='280' height='210' fill='url(#g)'/><text x='16' y='120' font-size='56' fill='white'>%d</text></svg>" % (hue, (hue + 90) % 360, n)).encode()


def page_iframes():
    return (b"<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'><title>Soak frames page</title>"
            b"<body style='margin:0;font:16px sans-serif'><h1 style='margin:8px'>Three frames</h1>"
            b"<iframe src='/page/text' style='width:100%;height:240px'></iframe><iframe src='/page/images' style='width:100%;height:240px'></iframe>"
            b"<iframe src='/page/form' style='width:100%;height:240px'></iframe></body>")


def page_form():
    return (b"<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'><title>Soak form page</title>"
            b"<body style='font:18px sans-serif;margin:12px'><form action='/report' method='get'><input type=hidden name=kind value=form>"
            b"<p><label>Name <input name=name></label></p><p><label>Email <input type=email name=email></label></p>"
            b"<p><select name=pick><option>one<option>two<option>three</select></p><p><textarea name=msg rows=4></textarea></p><button>Send</button></form></body>")


REPORTER = """
<script>
(function () {
  var q = new URLSearchParams(location.search), name = %(name)s + (q.get('id') ? '#' + q.get('id') : '');
  function send(ev, extra) {
    var v = document.getElementById('v'), o = { kind: 'video', name: name, ev: ev, t: Date.now() };
    try { o.ct = v.currentTime.toFixed(2); o.rs = v.readyState; o.ns = v.networkState; o.w = v.videoWidth; o.h = v.videoHeight; o.paused = v.paused ? 1 : 0;
          if (v.error) o.err = v.error.code + ':' + (v.error.message || '');
          if (v.getVideoPlaybackQuality) { var c = v.getVideoPlaybackQuality(); o.total = c.totalVideoFrames; o.dropped = c.droppedVideoFrames; o.corrupt = c.corruptedVideoFrames; } } catch (e) { o.exc = String(e); }
    if (extra) for (var k in extra) o[k] = extra[k];
    try { new Image().src = '/report?' + Object.keys(o).map(function (k) { return k + '=' + encodeURIComponent(o[k]); }).join('&'); } catch (e) {}
  }
  var v = document.getElementById('v');
  ['loadstart', 'loadedmetadata', 'canplay', 'playing', 'pause', 'waiting', 'stalled', 'error', 'ended', 'seeked'].forEach(function (e) { v.addEventListener(e, function () { send(e); }); });
  var n = 0; setInterval(function () { if (!v.paused) { n++; send('tick'); } }, 5000);
  send('page');
  var p = v.play(); if (p && p.catch) p.catch(function (e) { send('playrejected', { why: String(e).slice(0, 60) }); });
})();
</script>"""


def page_video(name, src):
    reporter = (REPORTER % {"name": json.dumps(name)})
    return (("<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'><title>Soak video %s</title>"
             "<body style='margin:0;background:#000;color:#9f9;font:14px monospace'>"
             "<video id=v src='%s' muted autoplay playsinline loop controls style='width:100%%;max-height:70vh;background:#000'></video>"
             "<p style='margin:8px'>%s</p>%s") % (name, src, name, reporter)).encode()


def page_fs(lock=""):
    """The full-screen lab (FS1). The buttons sit at fixed fractions of the page height so a script can tap them without reading the screen:
    full screen at 25.5 %, lock landscape 44.5 %, unlock 63.5 %, exit 82.5 % (tools/emu/soak.py FS_GO ...). ?lock=portrait|landscape makes the page lock the screen by itself the moment it is full screen."""
    reporter = """
<script>
(function () {
  var v = document.getElementById('v'), autolock = %s;
  function send(ev, extra) {
    var o = { kind: 'fs', ev: ev, t: Date.now(), fs: document.fullscreenElement ? 1 : 0, iw: innerWidth, ih: innerHeight };
    try { o.ot = screen.orientation.type; o.oa = screen.orientation.angle; } catch (e) { o.ot = 'none'; }
    try { o.vw = v.videoWidth; o.vh = v.videoHeight; o.paused = v.paused ? 1 : 0; o.ct = v.currentTime.toFixed(1); } catch (e) {}
    if (extra) for (var k in extra) o[k] = extra[k];
    try { new Image().src = '/report?' + Object.keys(o).map(function (k) { return k + '=' + encodeURIComponent(o[k]); }).join('&'); } catch (e) {}
  }
  function lock(kind, ev) {
    try { screen.orientation.lock(kind).then(function () { send(ev + '-ok'); }, function (e) { send(ev + '-rejected', { why: String(e).slice(0, 80) }); }); } catch (e) { send(ev + '-threw', { why: String(e).slice(0, 80) }); }
  }
  document.addEventListener('fullscreenchange', function () {
    send('fullscreenchange');
    if (autolock && document.fullscreenElement) lock(autolock, 'autolock');
    if (autolock && !document.fullscreenElement) { try { screen.orientation.unlock(); } catch (e) {} }
  });
  window.addEventListener('resize', function () { send('resize'); });
  try { screen.orientation.addEventListener('change', function () { send('orientationchange'); }); } catch (e) {}
  document.addEventListener('click', function (e) { send('page-click', { tg: (e.target && (e.target.id || e.target.tagName)) || '' }); }, true);   // VP1: a tap on the picture must NOT reach the page while Kestrel's controls are on
  document.getElementById('go').onclick = function () { send('click-go'); var p = v.requestFullscreen ? v.requestFullscreen() : null; if (p && p.catch) p.catch(function (e) { send('fs-rejected', { why: String(e).slice(0, 60) }); }); };
  document.getElementById('lock').onclick = function () { send('click-lock'); lock('landscape', 'lock'); };
  document.getElementById('unlock').onclick = function () { send('click-unlock'); try { screen.orientation.unlock(); send('unlock-ok'); } catch (e) { send('unlock-threw'); } };
  document.getElementById('exit').onclick = function () { send('click-exit'); try { document.exitFullscreen(); } catch (e) {} };
  v.play().catch(function () {});
  send('page');
})();
</script>""" % json.dumps(lock)
    btn = "display:block;width:92%;margin:1vh auto;height:17vh;font-size:26px"
    return ("<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'><title>Full-screen lab</title>"
            "<body style='margin:0;background:#112;color:#eee;font:20px sans-serif;height:100vh;overflow:hidden'>"
            "<video id=v src='/clip/landscape.mp4' muted autoplay playsinline loop style='display:block;width:100%;height:16vh;object-fit:contain;background:#000'></video>"
            "<button id=go style='" + btn + "'>Go full screen</button><button id=lock style='" + btn + "'>Lock landscape</button>"
            "<button id=unlock style='" + btn + "'>Unlock</button><button id=exit style='" + btn + "'>Exit full screen</button>" + reporter).encode()


def page_fsbox(kind, nobar, chrome=False):
    """FSB1 / ROT1 / FSC1 (owner message 46): a PLAYER BOX goes full screen (not the bare video), as finance.yahoo.com's player does: kind=shadow puts the box inside a web component's open shadow root (the full-screen
    element is then the host), kind=light is a plain box. The box's bottom edge carries a 6 px RED strip, so a screenshot shows whether the bottom of the full-screen page is hidden under a system bar
    (the strip is gone) or visible (red rows). The clip has sound (a media session opens, so the app can pause it while the screen turns). nobar=1: the box has no controls of its own (Kestrel's bar can show).
    The report rows (/report, kind=fsb) carry the window size and the strip's position. chrome=1 (VP2): the box also has a bar named like YouTube's player chrome (class ytp-chrome-bottom, bright magenta); the rows
    say its computed display, so the lab sees that Kestrel's controls put the page's own controls away while they own the film and give them back."""
    own = "" if nobar else ('<div id="bar" style="position:absolute;left:0;right:0;bottom:6px;height:44px;background:rgba(0,0,0,.7)"><button class="play-button" aria-label="Play or pause" style="width:44px;height:44px">|&gt;</button>'
                            '<input type="range" aria-label="Seek" value="0" style="width:60%"></div>')
    ytc = '<div id="ytchrome" class="ytp-chrome-bottom" style="position:absolute;left:0;right:0;bottom:30px;height:36px;background:#c0c;color:#fff;display:block">page chrome</div>' if chrome else ""
    box = ('<div id="box" style="position:relative;width:100%;height:300px;background:#000"><video id="v" src="/clip/landscape.mp4" loop playsinline style="width:100%;height:100%;object-fit:contain"></video>' + own + ytc +
           '<div id="mark" style="position:absolute;left:0;right:0;bottom:0;height:6px;background:#f00"></div></div>')
    css = "#box:fullscreen,:host(:fullscreen) #box{width:100vw !important;height:100vh !important}"
    if kind == "shadow":
        build = ("var host=document.getElementById('slot');var root=host.attachShadow({mode:'open'});root.innerHTML=`<style>@CSS@</style>@BOX@`;"
                 "var q=function(i){return root.getElementById(i)};var fsEl=host;")
    else:
        build = ("document.getElementById('slot').innerHTML=`@BOX@`;var st=document.createElement('style');st.textContent=`@CSS@`;document.head.appendChild(st);"
                 "var q=function(i){return document.getElementById(i)};var fsEl=q('box');")
    build = build.replace("@CSS@", css).replace("@BOX@", box)
    page = """<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'><title>fsbox @KIND@</title>
<body style='margin:0;background:#112;color:#eee;font:20px sans-serif'><h3 style='margin:10px'>fsbox @KIND@</h3>
<button id=go style='position:fixed;left:4%;top:60px;width:92%;height:90px;font-size:26px;z-index:5'>Play and go full screen</button>
<div id=slot style='position:absolute;left:0;top:170px;width:100%;height:300px'></div>
<script>(function () {
  @BUILD@
  function send(ev, extra) {
    var o = { kind: 'fsb', ev: ev, t: Date.now(), fs: document.fullscreenElement ? 1 : 0, iw: innerWidth, ih: innerHeight, dpr: window.devicePixelRatio };
    try { var m = q('mark').getBoundingClientRect(); o.mb = Math.round(m.bottom); o.bw = Math.round(m.width); o.bh = Math.round(q('box').getBoundingClientRect().height); } catch (e) {}
    try { var v = q('v'); o.paused = v.paused ? 1 : 0; o.ct = v.currentTime.toFixed(1); } catch (e) {}
    try { var yc = q('ytchrome'); if (yc) o.chrome = getComputedStyle(yc).display; } catch (e) {}
    try { o.ot = screen.orientation.type; } catch (e) {}
    if (extra) for (var k in extra) o[k] = extra[k];
    try { new Image().src = '/report?' + Object.keys(o).map(function (k) { return k + '=' + encodeURIComponent(o[k]); }).join('&'); } catch (e) {}
  }
  document.addEventListener('fullscreenchange', function () { send('fullscreenchange'); });
  ['seeked', 'pause', 'play', 'volumechange'].forEach(function (ev) { try { q('v').addEventListener(ev, function () { send('v-' + ev); }); } catch (e) {} });   // VP1: the lab sees what Kestrel's controls did to the film
  window.addEventListener('resize', function () { send('resize'); });
  try { screen.orientation.addEventListener('change', function () { send('orientationchange'); }); } catch (e) {}
  document.getElementById('go').onclick = function () {
    send('click-go');
    try { q('v').play().catch(function () {}); } catch (e) {}
    var p = fsEl.requestFullscreen ? fsEl.requestFullscreen() : null;
    if (p && p.catch) p.catch(function (e) { send('fs-rejected', { why: String(e).slice(0, 60) }); });
  };
  send('page');
})();</script>"""
    return page.replace("@KIND@", kind).replace("@BUILD@", build).encode()


def page_dlguard(kind):
    """RX4-X2: four ways a page starts a download. auto-* = by itself 2.5 s after the load (a timer), tap-* = the person taps the big button; *-link clicks a hidden link, *-nav sets location."""
    nav = kind.endswith("nav")
    trigger = ("location.href='/dl/1mb.bin';" if nav else
               "var a=document.createElement('a');a.href='/dl/1mb.bin';a.setAttribute('download','lab-guard.bin');document.body.appendChild(a);a.click();")
    head = "<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'><title>Guard %s</title><body style='font:22px sans-serif;margin:14px'><h1>%s</h1>" % (kind, kind)
    if kind.startswith("auto"):
        return (head + "<p>a timer starts the download 2.5 s after the page loaded</p><script>setTimeout(function(){%s}, 2500);</script>" % trigger).encode()
    return (head + "<button id=go style='display:block;width:92%%;height:30vh;margin:30vh auto 0;font-size:28px' onclick=\"%s\">Download</button>" % trigger.replace('"', "'")).encode()


def page_vc(kind, ad):
    """VC1 / LP1 (owner message 44): a clip with no controls of the page's own (naked), inside a web component's shadow root like finance.yahoo.com's player (shadow; ad=1 lays a frame over it, as Google's
    IMA overlay does), and with a visible button and slider of its own (own)."""
    clip = "/clip/landscape.mp4"
    head = "<!doctype html><html><head><meta name=viewport content='width=device-width,initial-scale=1'><title>vc %s</title></head><body style='margin:0;background:#fff;font:16px sans-serif'><h3 style='margin:10px'>vc %s</h3>" % (kind, kind)
    if kind == "own":
        body = ("<div style='position:relative;width:100%;max-width:560px;margin:8px auto'><video id=v src='__CLIP__' autoplay muted loop playsinline style='display:block;width:100%;background:#222'></video>"
                "<div style='position:absolute;left:0;right:0;bottom:0;height:44px;background:rgba(0,0,0,.6)'><button class='play-button' aria-label='Play or pause' style='width:44px;height:44px'>|&gt;</button>"
                "<input type=range aria-label='Seek' value=0 style='width:60%'></div></div>")
    elif kind == "shadow":
        body = ("<div style='position:relative;width:100%;max-width:560px;margin:8px auto'><div id=host style='width:100%;height:315px'></div>"
                + ("<iframe src='/blank' style='position:absolute;left:0;top:0;width:100%;height:280px;border:0'></iframe>" if ad else "")  # a real address: the extension's scripts do not run in about:srcdoc / about:blank frames
                + "</div><script>var root=document.getElementById('host').attachShadow({mode:'open'});root.innerHTML='<div style=\"position:relative;width:100%;height:315px;background:#000\">"
                "<video id=v src=\"__CLIP__\" autoplay muted loop playsinline style=\"width:100%;height:100%\"></video><div class=\"shaka-controls-container\" style=\"position:absolute;left:0;top:0;right:0;bottom:0;pointer-events:none\">"
                "<div class=\"shaka-bottom-controls\" style=\"position:absolute;left:0;right:0;bottom:0;height:5px;pointer-events:auto\"></div></div></div>';</script>")
    else:
        body = "<video id=v src='__CLIP__' autoplay muted loop playsinline style='display:block;width:100%;max-width:560px;margin:8px auto;background:#222'></video>"
    return (head + body.replace("__CLIP__", clip) + "<p style='margin:10px'>The clip plays by itself (muted).</p></body></html>").encode()


# ANS52g (owner message 52): the video format policy on the real engine. Each row is a question a player asks: the type with the hints a site puts into it (width / height / framerate), as `MediaSource.isTypeSupported`,
# `canPlayType` and (rows that have a size) `navigator.mediaCapabilities.decodingInfo` see it.
FMT_ROWS = [
    ("vp9-720p30", 'video/webm; codecs="vp09.00.30.08"', 1280, 720, 30), ("vp9-720p60", 'video/webm; codecs="vp09.00.30.08"', 1280, 720, 60),
    ("vp9-1080p30", 'video/webm; codecs="vp09.00.40.08"', 1920, 1080, 30), ("vp9-1080p60", 'video/webm; codecs="vp09.00.40.08"', 1920, 1080, 60),
    ("vp9-1440p30", 'video/webm; codecs="vp09.00.50.08"', 2560, 1440, 30), ("vp9-4k30", 'video/webm; codecs="vp09.00.51.08"', 3840, 2160, 30),
    ("vp9-bare", 'video/webm; codecs="vp9"', 0, 0, 0),
    ("avc-720p30", 'video/mp4; codecs="avc1.4d401f"', 1280, 720, 30), ("avc-720p60", 'video/mp4; codecs="avc1.4d4020"', 1280, 720, 60),
    ("avc-1080p60", 'video/mp4; codecs="avc1.640028"', 1920, 1080, 60), ("avc-4k30", 'video/mp4; codecs="avc1.640033"', 3840, 2160, 30),
    ("avc-bare", 'video/mp4; codecs="avc1.640028"', 0, 0, 0),
    ("audio-aac", 'audio/mp4; codecs="mp4a.40.2"', 0, 0, 0),
]


def page_formats(tag):
    """The questions are asked twice (1.5 s and 5 s after the page started: the policy reaches the page a few tens of milliseconds after its first script, a late one shows as a difference) and each pass is sent to
    /report (kind=fmt, d = name:isTypeSupported:canPlayType:decodingInfo, one per row; mc 'x' = no such question for the row, 'e' = the engine threw)."""
    return (("""<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'><title>Soak formats</title>
<body style='font:14px monospace;margin:10px'><h3>formats %(tag)s</h3><pre id=o>waiting</pre>
<script>
(function () {
  var ROWS = %(rows)s, TAG = %(tag_js)s;
  function ask(pass) {
    var out = [], pend = [];
    ROWS.forEach(function (r) {
      var type = r[1] + (r[2] ? '; width=' + r[2] + '; height=' + r[3] : '') + (r[4] ? '; framerate=' + r[4] : ''), row = { n: r[0], mse: 'x', can: 'x', mc: 'x' };
      try { row.mse = MediaSource.isTypeSupported(type) ? '1' : '0'; } catch (e) { row.mse = 'e'; }
      try { row.can = document.createElement(r[1].indexOf('audio/') === 0 ? 'audio' : 'video').canPlayType(type) || '-'; } catch (e) { row.can = 'e'; }
      if (r[2]) {
        try {
          pend.push(navigator.mediaCapabilities.decodingInfo({ type: 'file', video: { contentType: r[1], width: r[2], height: r[3], framerate: r[4], bitrate: 2000000 } })
            .then(function (x) { row.mc = x && x.supported ? '1' : '0'; }, function () { row.mc = 'e'; }));
        } catch (e) { row.mc = 'e'; }
      }
      out.push(row);
    });
    Promise.all(pend).then(function () { finish(pass, out); }, function () { finish(pass, out); });
  }
  function finish(pass, out) {
    var d = out.map(function (r) { return r.n + ':' + r.mse + ':' + r.can.replace(/[^a-z-]/g, '') + ':' + r.mc; }).join(',');
    document.getElementById('o').textContent = TAG + ' ' + pass + '\\n' + out.map(function (r) { return r.n + '  mse ' + r.mse + '  can ' + r.can + '  mc ' + r.mc; }).join('\\n');
    try { new Image().src = '/report?kind=fmt&tag=' + encodeURIComponent(TAG) + '&pass=' + pass + '&d=' + encodeURIComponent(d) + '&t=' + Date.now(); } catch (e) {}
  }
  setTimeout(function () { ask('a'); }, 1500);
  setTimeout(function () { ask('b'); }, 5000);
})();
</script>""") % {"tag": tag.replace("<", "").replace("&", "")[:30], "tag_js": json.dumps(tag[:30]), "rows": json.dumps([list(r) for r in FMT_ROWS])}).encode()


def page_dl():
    links = "".join("<p><a href='/dl/%dmb.bin'>%d MB file</a></p>" % (n, n) for n in (1, 5, 20)) + "<p><a href='/dl/slow.bin'>5 MB, slowly</a></p>"
    return ("<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'><title>Soak downloads</title><body style='font:20px sans-serif;margin:14px'><h1>Downloads</h1>" + links).encode()


def index():
    rows = ["/page/text", "/page/images", "/page/iframes", "/page/form", "/video/landscape", "/video/portrait", "/video/big", "/formats?tag=index", "/fs", "/fsbox?kind=shadow", "/dl/page", "/dl/1mb.bin", "/dl/5mb.bin", "/dl/20mb.bin", "/dl/slow.bin"]
    return ("<!doctype html><meta charset=utf-8><title>Soak lab</title><body style='font:18px sans-serif'><h1>Soak lab</h1>" + "".join("<p><a href='%s'>%s</a></p>" % (r, r) for r in rows)).encode()


def blob(n, seed=7):
    # deterministic bytes the driver can check by size alone
    line = bytes((seed + i) % 251 for i in range(1024))
    return line, n


class H(http.server.BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"
    server_version = "soaklab"

    def log_message(self, *a):
        pass

    def _send(self, body, ctype="text/html; charset=utf-8", code=200, extra=None):
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        for k, v in (extra or {}).items():
            self.send_header(k, v)
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(body)

    def _file(self, path, ctype):
        """A file with Range support (a <video> asks for ranges)."""
        if not os.path.exists(path):
            return self._send(b"no such clip (ffmpeg missing?)", "text/plain", 404)
        size = os.path.getsize(path)
        rng = self.headers.get("Range")
        start, end, code = 0, size - 1, 200
        if rng and rng.startswith("bytes="):
            a, _, b = rng[6:].partition("-")
            start = int(a) if a else 0
            end = int(b) if b else size - 1
            end = min(end, size - 1)
            code = 206
        n = end - start + 1
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Content-Length", str(n))
        if code == 206:
            self.send_header("Content-Range", "bytes %d-%d/%d" % (start, end, size))
        self.end_headers()
        if self.command == "HEAD":
            return
        sent, t0 = 0, time.time()
        with open(path, "rb") as f:
            f.seek(start)
            left = n
            while left > 0:
                chunk = f.read(min(16384 if RATE else 65536, left))
                if not chunk:
                    break
                self.wfile.write(chunk)
                left -= len(chunk)
                sent += len(chunk)
                pace(sent, t0, RATE)

    def do_HEAD(self):
        self.do_GET()

    def do_GET(self):
        u = urllib.parse.urlparse(self.path)
        p, q = u.path, urllib.parse.parse_qs(u.query)
        try:
            if DELAY and p not in ("/health", "/report"):
                time.sleep(DELAY)
            if p == "/":
                return self._send(index())
            if p == "/health":
                return self._send(b"ok", "text/plain")
            if p == "/page/text":
                return self._send(page_text())
            if p == "/page/images":
                return self._send(page_images())
            if p == "/page/iframes":
                return self._send(page_iframes())
            if p == "/page/form":
                return self._send(page_form())
            if p.startswith("/img/") and p.endswith(".svg"):
                return self._send(svg(int(p[5:-4] or 0)), "image/svg+xml")
            if p == "/formats":
                return self._send(page_formats((q.get("tag") or ["x"])[0]))
            if p == "/video/audible":
                return self._send(page_audible((q.get("kind") or ["video"])[0]))
            if p == "/video/vc":
                return self._send(page_vc((q.get("kind") or ["naked"])[0], (q.get("ad") or [""])[0] == "1"))
            if p.startswith("/video/"):
                name = p[7:]
                src = (q.get("src") or [""])[0] or "/clip/%s.mp4" % name
                return self._send(page_video(name, src.replace("'", "%27")))
            if p.startswith("/clip/") and p.endswith(".mp4"):
                name = p[6:-4]
                if name in CLIPS:
                    return self._file(os.path.join(OUT, CLIPS[name][2]), "video/mp4")
                return self._send(b"unknown clip", "text/plain", 404)
            if p == "/clip/tone.m4a":
                return self._file(os.path.join(OUT, "tone.m4a"), "audio/mp4")
            if p == "/blank":
                return self._send(b"<!doctype html><html><body style=\"margin:0;background:transparent\"></body></html>")
            if p == "/fs":
                return self._send(page_fs((q.get("lock") or [""])[0]))
            if p == "/fsbox":
                return self._send(page_fsbox((q.get("kind") or ["light"])[0], (q.get("nobar") or [""])[0] == "1", (q.get("chrome") or [""])[0] == "1"))
            if p == "/dl/page":
                return self._send(page_dl())
            if p == "/dl/guard":
                return self._send(page_dlguard((q.get("kind") or ["auto-link"])[0]))
            if p.startswith("/dl/") and p.endswith("mb.bin"):
                return self._download(int(p[4:-6]) * 1024 * 1024, "kestrel-lab-%smb.bin" % p[4:-6], 0)
            if p == "/dl/slow.bin":
                return self._download(5 * 1024 * 1024, "kestrel-lab-slow.bin", 400 * 1024)
            if p == "/report":
                row = {k: v[0] for k, v in q.items()}
                row["srv"] = round(time.time(), 2)
                with LOCK:
                    with open(REPORT, "a") as f:
                        f.write(json.dumps(row) + "\n")
                return self._send(b"", "image/gif", 204)
            return self._send(b"not found", "text/plain", 404)
        except (BrokenPipeError, ConnectionResetError):
            pass

    def _download(self, n, name, rate):
        line, _ = blob(n)
        self.send_response(200)
        self.send_header("Content-Type", "application/octet-stream")
        self.send_header("Content-Length", str(n))
        self.send_header("Content-Disposition", 'attachment; filename="%s"' % name)
        self.end_headers()
        if self.command == "HEAD":
            return
        sent = 0
        t0 = time.time()
        eff = min(rate, RATE) if (rate and RATE) else (rate or RATE)   # the slow download stays slower than the shaped link, never faster
        while sent < n:
            k = min(len(line) * 16, n - sent)
            self.wfile.write((line * 16)[:k])
            sent += k
            pace(sent, t0, eff)


class Server(socketserver.ThreadingMixIn, http.server.HTTPServer):
    daemon_threads = True
    allow_reuse_address = True
    request_queue_size = 64


if __name__ == "__main__":
    make_clips()
    print("soaklab: http://localhost:%d/ (out %s)" % (PORT, OUT), flush=True)
    Server(("0.0.0.0", PORT), H).serve_forever()
