#!/usr/bin/env python3
"""Guard lab (RX3-G1, research/REVIEW-EXTERNAL-3.md part G): python3 guardlab.py <port> <logfile>

  /fakeplay   a page that, WITHOUT any tap, tries to open a pop-up window at 0.3 s, 3 s, 8 s and 15 s after it loaded (window.open of /popad), and writes what it saw to <logfile>.
              With the pop-up guard in the page, window.open is refused (no GET /popad ever reaches this server); without it a window opens and the server logs "GET /popad".
  /popad      the page an ad would open; it only exists to be requested.
  /assist-parent  RX3-G3: a page with a cross-origin frame (the same server under another host name: 127.0.0.1) holding a "player", and a transparent layer over it that opens an ad when TAPPED.
              A real tap on the layer: the pop-up is refused, and the assist asks the frame (through the extension, never through a message the page could send) to start its video:
              the frame logs "child play event: the video STARTED" (a real tiny silent media element, the page listens for its play event). Without the assist the log has no such line.
  /assist-child   the frame's page (served for the host 127.0.0.1).
  /finderlab  RX3-V1: four real media elements the page itself describes differently (a muted looping hero, a player with controls, a player inside an ad box, a thumbnail-sized player); the app's Health lines
              "video info role=... counted=N of M" say what it decided. Ends with a DONE line once all four loaded their metadata. /clip1.wav .. /clip4.wav: the same 20 s silent audio under four addresses.
  /teaser-link  RX3-V2: a page with nothing but two LINKS to video files and no player: /teaser.mp4 (30 KB, a preview) and /bigfile.mp4 (700 KB, a real one); both answer a one-byte Range request with
              Content-Range. The app's Health lines "media verify asked=N" and "media verified length=... counted=N of M" show that only the big one ends up counted for the badge.
  /hls-lab    RX3-V5: a page that asks for three stream playlists (the segments are never requested): /live.m3u8 (no end list), /short.m3u8 (a 12 s clip) and /film.m3u8 (2 minutes with an advertising
              break marked by CUE-OUT / CUE-IN). The app's Health lines "media look live=.. length=..s advertising=..s in N breaks counted=N of M" show what the early look found.
  /playlab    RX3-S1: ONE real, audible 440 Hz tone (/tone.wav, 30 s) in a <video>; a tap anywhere starts it (the app refuses audible autoplay without a tap), the page pauses it after 6 s, plays it again 3 s later,
              pauses it after 6 s more and ends with DONE. The app's Health lines "neon video playing=true/false front=true" show the glow being held while it plays.
  /slowjump   RX3-G6: with NO tap at all, 5 s after it loaded the page sends its own tab to a host the block lists name (https://propellerads.com/). Before RX3-G6 a jump with no recent touch was always allowed;
              now the app stops it and shows the blocked icon (Health: "guard app-wall stopped a jump to propellerads.com").
  /layer-link RX3-G5: a transparent layer over the page: a tap on it makes the page click a hidden <a target=_blank> to another site (an ad). The app's new-window wall now needs a tap ON a link to exactly
              that address: Health "guard app-wall stopped a new window to ..." (or the page guard's own "chip-show open", whichever refused it first).
  /fake-layer RX3-G4: a <video> (each click on it is logged) and over it an INVISIBLE full-screen layer of the page itself whose click handler opens an ad window. Version 1 (alpha44) switched such a layer off before the
              press ("video-click" only); an independent review showed that unsafe (it disabled a page's own invisible controls), so since alpha45 the layer takes the first tap ("layer-click", the ad is refused) and the
              assist starts the video: see research/REVIEW_ALPHA44.md. Only an invisible FRAME of another site over a player is switched off now (test browser only, no lab page).
  /preroll-main RX3-V3: ONE <video> element plays a 15 s "ad" file (/ad15.wav); after 8 s of real playback the page gives the same element the 60 s "film" (/film60.wav). The app's Health lines "video info PLAYED el=N ..."
              show both played reports with the SAME element number and, after the second, the ad marked as an advertisement and not counted: "counted=1 of 2".
  /blob-lab   RX3-V4: a <video> whose source is a blob: address (the page fetched /blob-film.wav, 300 s of silence, and made the address itself), next to two stream playlists the page asked for:
              /blob-clip.m3u8 (45 s) and /blob-film.m3u8 (300 s). After the early look (about 3 s after the page settled) and 3 s of real playback the app's Health lines say "media look HLS ... length=300s",
              "video info-blob PLAYED el=1 ... length=300s ... matched=1 counted=1 of 2": the film is matched to the player, the 45 s clip next to it is not counted.
  /playerlab  RX3-G7: five mock video players on one page, driven by the page itself (no touch needed): an ordinary video (no ad words), a JW-style player (jw-flag-ads on its root), a Video.js-style player
              (vjs-ad-loading, then vjs-ad-playing), a Google-IMA-style host (the ad video inside .ima-ad-container) and a JW-style ad with a skip button (.jw-skip, .jw-skippable). Each ad is a REAL 12 s silent
              media element. The log says how long the ad took (about 12000 ms = not skipped, a few hundred = fast-forwarded), at what speed it ran, and which events the skip button got. Ends with a DONE line.

  /seekbar-lab YS1: three mock players over a REAL 300 s silent media file (/seek-film.wav, served with byte ranges, every request logged with its Range header). A: a YouTube-like red bar whose page IGNORES a tap
              (only a light "hover" line follows the finger, as the owner reported); B: the same bar whose page jumps the video itself on a tap (the control: Kestrel must not seek twice); C: a short volume
              slider (must never move the video); R: the REAL current YouTube bar markup (yt-progress-bar, role=slider drag container, 3 px rail at opacity .3, 'Hovered' line), the page ignores a tap and only paints the light line; under A's bar a thin strip with a name the script does not know (a tap on it only shows up in Diagnostics: "1 tap on a bar-like strip Kestrel did not know (last: div.mystery-strip)"). ?small uses the 2.4 MB film instead of /seek-big.wav (57.6 MB, 192 KB/s). The page logs every pointer / mouse / touch event of a tap (type, isTrusted, pointerType) and every seeking / seeked of each video with its time, and shows both
              times on screen. The log's "GET /seek-film.wav Range=bytes=N-" lines show where the engine reads from after a jump: nothing before the jumped-to place is asked for again.

Used as the FIRST page after a force-stop (a cold start from a link: the extension may not be active yet) and as a later page, to see whether the guard was there from the very first document.
Nothing here is part of the app. Reach it with `adb reverse tcp:<port> tcp:<port>` and http://localhost:<port>/fakeplay. The driver is tools/emu/firstpage_emu.py.
"""
import base64, http.server, struct, sys, time, urllib.parse


def silent_wav(seconds=1):
    """Silence (8-bit mono, 8 kHz) of the given length as a data: address: a media element that REALLY plays in the engine, with no codec or file needed."""
    n = 8000 * seconds
    hdr = b"RIFF" + struct.pack("<I", 36 + n) + b"WAVEfmt " + struct.pack("<IHHIIHH", 16, 1, 1, 8000, 8000, 1, 8) + b"data" + struct.pack("<I", n)
    return "data:audio/wav;base64," + base64.b64encode(hdr + b"\x80" * n).decode()


WAV = silent_wav()

FAKEPLAY = b"""<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>fakeplay</title></head>
<body style="font:20px sans-serif;margin:16px"><p>fakeplay: tries to open a pop-up by script, with no tap, four times.</p>
<script>
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v) + '&t=' + Date.now()); x.send(); } catch (e) {} }
// the guard and the scriptlet library each leave a marker in the page world: is the guard in THIS document, from its very first script on? (RX3-G1)
L('first-script', 'guard in the page world: ' + (window.__kstGuard === 1 ? 'YES' : 'no') + ', scriptlet library: ' + (window.__kstSL === 1 ? 'YES' : 'no'));
L('load', 'fakeplay');
// an about:blank frame the page creates itself (the "iframe race": a script makes a frame and calls the frame's window.open before anything could have been injected into it)
try {
  var f = document.createElement('iframe'); f.style.cssText = 'width:10px;height:10px'; document.body.appendChild(f);
  var fw = f.contentWindow, early = fw.__kstGuard === 1;
  var fr = null; try { fr = fw.open('/popad?n=frame', '_blank'); } catch (e) {}
  L('blank-frame', 'guard marker in the frame right after it was created: ' + (early ? 'YES' : 'no') + ', its window.open returned ' + (fr ? 'a window object' : 'null'));
  setTimeout(function () { L('blank-frame-later', 'guard marker in the frame now: ' + (fw.__kstGuard === 1 ? 'YES' : 'no')); }, 2000);
} catch (e) { L('blank-frame', 'error ' + e); }
[300, 3000, 8000, 15000].forEach(function (ms, i) {
  setTimeout(function () {
    var w = null, err = '';
    try { w = window.open('/popad?n=' + (i + 1), '_blank'); } catch (e) { err = String(e); }
    L('attempt', (i + 1) + ' at ' + ms + ' ms: guard marker ' + (window.__kstGuard === 1 ? 'YES' : 'no') + ', window.open returned ' + (w ? 'a window object' : 'null') + (err ? ' error ' + err : ''));
  }, ms);
});
</script></body></html>
"""

ASSIST_PARENT = b"""<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>assist-parent</title></head>
<body style="margin:0;font:18px sans-serif"><div style="position:relative;width:100%;height:420px">
<iframe id="f" src="http://127.0.0.1:%PORT%/assist-child" style="position:absolute;left:0;top:0;width:100%;height:420px;border:0"></iframe>
<div id="layer" style="position:absolute;left:0;top:0;width:100%;height:420px" onclick="window.open('http://127.0.0.1:%PORT%/popad?n=layer','_blank')"></div></div>
<p>assist-parent: tap the dark box. The layer on top tries to open an ad; the frame's video should start instead.</p>
<script>
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v) + '&t=' + Date.now()); x.send(); } catch (e) {} }
L('load', 'assist-parent first-script guard=' + (window.__kstGuard === 1 ? 'YES' : 'no'));
document.getElementById('layer').addEventListener('click', function () { L('layer-click', 'the layer got the tap and called window.open'); }, true);
</script></body></html>
"""

ASSIST_CHILD = b"""<!doctype html><html><head><meta charset="utf-8"><title>assist-child</title></head>
<body style="margin:0;background:#111;color:#eee;font:18px sans-serif"><video id="v" src="%WAV%" style="width:100%;height:380px;background:#000" muted loop></video>
<script>
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v) + '&t=' + Date.now()); x.send(); } catch (e) {} }
// a real, tiny, silent media element: the assist's own play() call makes the engine fire a play event that THIS page can see (a stubbed play() could not be seen: the extension's calls go through the engine's own wrappers)
document.getElementById('v').addEventListener('play', function () { L('child', 'child play event: the video STARTED'); });
L('load', 'assist-child first-script guard=' + (window.__kstGuard === 1 ? 'YES' : 'no') + ' host=' + location.host);
</script></body></html>
"""

WAV12 = silent_wav(12)
WAV20 = silent_wav(20)

PLAYERLAB = b"""<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>playerlab</title></head>
<body style="margin:8px;font:16px sans-serif">
<p>playerlab: mock players, driven by this page (RX3-G7).</p>
<div id="plain" class="post video-wrapper"><video id="plainv" style="width:200px;height:60px;background:#000" muted loop></video></div>
<div id="jw" class="jwplayer jw-reset jw-skin-seven jw-state-playing" style="position:relative;width:300px;height:80px"><div class="jw-media jw-reset"><video id="jwv" class="jw-video jw-reset" style="width:300px;height:80px;background:#000" muted></video></div></div>
<div id="vjs" class="video-js vjs-default-skin" style="position:relative;width:300px;height:80px"><video id="vjsv" class="vjs-tech" style="width:300px;height:80px;background:#000" muted></video></div>
<div id="ima" style="position:relative;width:300px;height:80px"><video id="imac" style="width:300px;height:80px;background:#000" muted loop></video><div class="ima-ad-container" style="position:absolute;left:0;top:0;width:300px;height:80px"><video id="imaad" style="width:300px;height:80px" muted></video></div></div>
<div id="jws" class="jwplayer jw-reset" style="position:relative;width:300px;height:80px"><div class="jw-media jw-reset"><video id="jwsv" class="jw-video jw-reset" style="width:300px;height:80px;background:#000" muted></video></div></div>
<script>
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v) + '&t=' + Date.now()); x.send(); } catch (e) {} }
var MAIN = '%WAV20%', AD = '%WAV12%';
function until(fn, ms) { return new Promise(function (res) { var t0 = Date.now(); (function p() { var r = false; try { r = fn(); } catch (e) {} if (r || Date.now() - t0 > ms) res(!!r); else setTimeout(p, 120); })(); }); }
function sleep(ms) { return new Promise(function (r) { setTimeout(r, ms); }); }
function startFilm(v) { v.muted = true; v.loop = true; v.src = MAIN; return v.play().catch(function (e) { L('error', 'film play refused: ' + e); }); }
function sampler(v) { var t0 = Date.now(), s = { n: 0, fast: 0, first: -1 }; var id = setInterval(function () { s.n++; if (v.playbackRate === 16) { s.fast++; if (s.first < 0) s.first = Date.now() - t0; } }, 100); return { stop: function () { clearInterval(id); return s; } }; }
// the ad takes the SAME element (state-class players) or its own element (hosted players); [ended] is set by the ad element's own ended event
function runAd(name, v, before, during, afterAd) {
  var t0 = Date.now(), ended = false, sm = sampler(v);
  return Promise.resolve(before && before()).then(function () {
    during && during();
    v.loop = false; v.src = AD; v.muted = true;
    v.addEventListener('ended', function f() { ended = true; v.removeEventListener('ended', f); });
    return v.play().catch(function (e) { L('error', name + ' ad play refused: ' + e); });
  }).then(function () { return until(function () { return ended; }, 30000); }).then(function (ok) {
    var took = Date.now() - t0, s = sm.stop();
    L(name, 'ad ' + (ok ? 'ended after ' + took + ' ms' : 'did NOT end within 30 s') + ' (12000 ms = normal speed, not skipped); samples ' + s.n + ', at speed 16 in ' + s.fast + (s.first >= 0 ? ', first fast sample after ' + s.first + ' ms' : ', never fast'));
    afterAd && afterAd();
  });
}
function plain() {
  var v = document.getElementById('plainv'), sm = sampler(v);
  return startFilm(v).then(function () { return sleep(3500); }).then(function () { var s = sm.stop(); L('plain', 'ordinary video, no ad words: samples ' + s.n + ', at speed 16 in ' + s.fast + ' (must be 0), playing=' + (!v.paused)); });
}
function jw() {
  var r = document.getElementById('jw'), v = document.getElementById('jwv');
  return startFilm(v).then(function () { return sleep(1500); }).then(function () {
    return runAd('jw', v, null, function () { r.classList.add('jw-flag-ads'); }, function () { r.classList.remove('jw-flag-ads'); });
  });
}
function warm() {   // the first question to the app (may the skipper run?) can take long on the emulator: one ad on a hidden player gets it answered before the measured ones
  var r = document.getElementById('jw'), v = document.getElementById('jwv');
  return startFilm(v).then(function () { return sleep(500); }).then(function () {
    return runAd('warm-up', v, null, function () { r.classList.add('jw-flag-ads'); }, function () { r.classList.remove('jw-flag-ads'); });
  });
}
function vjs() {
  var r = document.getElementById('vjs'), v = document.getElementById('vjsv'), sm;
  return startFilm(v).then(function () { return sleep(1500); }).then(function () {
    r.classList.add('vjs-ad-loading'); sm = sampler(v);
    return sleep(1200);
  }).then(function () {
    var s = sm.stop(); L('vjs-loading', 'waiting for the ad (vjs-ad-loading only): samples ' + s.n + ', at speed 16 in ' + s.fast + ' (must be 0)');
    return runAd('vjs', v, null, function () { r.classList.remove('vjs-ad-loading'); r.classList.add('vjs-ad-playing'); }, function () { r.classList.remove('vjs-ad-playing'); });
  });
}
function ima() {
  var c = document.getElementById('imac'), a = document.getElementById('imaad'), sm = sampler(c);
  return startFilm(c).then(function () { return sleep(1500); }).then(function () {
    return runAd('ima', a, null, null, function () { var s = sm.stop(); L('ima-film', 'the film video beside the ad container: samples ' + s.n + ', at speed 16 in ' + s.fast + ' (must be 0), playing=' + (!c.paused)); });
  });
}
function jwskip() {
  var r = document.getElementById('jws'), v = document.getElementById('jwsv'), seen = [];
  return startFilm(v).then(function () { return sleep(1500); }).then(function () {
    r.classList.add('jw-flag-ads');
    Object.defineProperty(v, 'duration', { configurable: true, get: function () { return 400; } });   // a 'long' ad: it is not moved to its end, so the skip button has to be used
    v.loop = true; v.src = AD; v.muted = true;
    var b = document.createElement('div'); b.className = 'jw-skip jw-reset'; b.style.cssText = 'position:absolute;right:4px;bottom:4px;width:80px;height:30px;background:#333';
    ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'].forEach(function (n) { b.addEventListener(n, function (e) { if (b.classList.contains('jw-skippable')) seen.push(n + (e.isTrusted ? '(trusted)' : '')); }); });
    var pressed = false;
    b.addEventListener('click', function () { if (b.classList.contains('jw-skippable')) { pressed = true; r.classList.remove('jw-flag-ads'); v.pause(); } });   // a real player ends the ad here
    r.appendChild(b);
    var t0 = Date.now();
    return v.play().catch(function () {}).then(function () { return sleep(1500); }).then(function () {
      var before = pressed;
      b.classList.add('jw-skippable');
      return until(function () { return pressed; }, 20000).then(function (ok) {
        L('jw-skip', 'skip button pressed ' + (ok ? 'by the script ' + (Date.now() - t0) + ' ms after the ad began' : 'NEVER within 20 s') + '; pressed before it was skippable: ' + before + '; events it got once skippable: ' + seen.join(' '));
      });
    });
  });
}
L('load', 'playerlab');
plain().then(warm).then(jw).then(vjs).then(ima).then(jwskip).then(function () { L('DONE', 'all five scenarios finished'); }, function (e) { L('error', 'lab: ' + e); L('DONE', 'with an error'); });
</script></body></html>
"""

FINDERLAB = b"""<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>finderlab</title></head>
<body style="margin:8px;font:16px sans-serif">
<p>finderlab: four media elements, each described differently by the page itself.</p>
<video id="hero" src="/clip1.wav" muted loop autoplay preload="auto" style="width:300px;height:100px;background:#222"></video>
<video id="film" src="/clip2.wav" controls preload="auto" style="width:300px;height:140px;background:#222"></video>
<div class="sidebar ad-slot"><video id="ad" src="/clip3.wav" controls preload="auto" style="width:300px;height:120px;background:#522"></video></div>
<video id="tiny" src="/clip4.wav" controls preload="auto" style="width:120px;height:60px;background:#225"></video>
<script>
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v) + '&t=' + Date.now()); x.send(); } catch (e) {} }
var ids = ['hero', 'film', 'ad', 'tiny'], got = 0;
ids.forEach(function (id) {
  var v = document.getElementById(id);
  function done() { got++; L('metadata', id + ' loaded (' + got + ' of 4)'); if (got === 4) setTimeout(function () { L('DONE', 'all four loaded their metadata'); }, 6000); }
  if (v.readyState >= 1) done(); else v.addEventListener('loadedmetadata', done, { once: true });
});
L('load', 'finderlab');
</script></body></html>
"""

TEASERLINK = b"""<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>teaser-link</title></head>
<body style="margin:8px;font:18px sans-serif"><p>teaser-link: links only, nothing plays.</p>
<p><a href="/teaser.mp4">watch the teaser</a></p><p><a href="/bigfile.mp4">the big file</a></p>
<script>try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=load&v=teaser-link'); x.send(); } catch (e) {}</script></body></html>
"""
SIZES = {"/teaser.mp4": 30000, "/bigfile.mp4": 700000}

HLSLAB = b"""<!doctype html><html><head><meta charset="utf-8"><title>hls-lab</title></head>
<body style="margin:8px;font:18px sans-serif"><p>hls-lab: asks for three playlists.</p>
<script>
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v)); x.send(); } catch (e) {} }
L('load', 'hls-lab');
['/live.m3u8', '/short.m3u8', '/film.m3u8'].forEach(function (u, i) { setTimeout(function () { fetch(u).then(function (r) { return r.text(); }).then(function (t) { L('got', u + ' ' + t.length + ' bytes'); }); }, 300 * i); });
setTimeout(function () { L('DONE', 'asked for all three'); }, 6000);
</script></body></html>
"""

BLOBLAB = b"""<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>blob-lab</title></head>
<body style="margin:8px;font:18px sans-serif"><p>blob-lab: a player on a blob: address, two playlists.</p>
<video id="v" muted controls style="width:300px;height:170px"></video>
<script>
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v)); x.send(); } catch (e) {} }
L('load', 'blob-lab');
(async function () {
  try {
    await (await fetch('/blob-clip.m3u8')).text(); L('got', 'clip playlist (45 s)');
    await (await fetch('/blob-film.m3u8')).text(); L('got', 'film playlist (300 s)');
    var b = await (await fetch('/blob-film.wav')).blob();
    var v = document.getElementById('v');
    v.addEventListener('playing', function () { L('playing', 'blob element: ' + Math.round(v.duration) + ' s'); });
    v.src = URL.createObjectURL(b); v.muted = true;
    var p = v.play(); if (p && p.catch) p.catch(function (e) { L('play-refused', String(e)); });
    setTimeout(function () { L('DONE', 'blob player playing ' + Math.round(v.currentTime) + ' s of ' + Math.round(v.duration)); }, 9000);
  } catch (e) { L('error', String(e)); }
})();
</script></body></html>
"""

def hls_playlist(kind):
    head = "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:6\n#EXT-X-MEDIA-SEQUENCE:0\n"
    if kind == "live":
        return head + "".join("#EXTINF:6.0,\n/seg/live%d.ts\n" % i for i in range(4))
    if kind == "blobclip":
        return head + "#EXT-X-PLAYLIST-TYPE:VOD\n" + "".join("#EXTINF:5.0,\n/seg/blobclip%d.ts\n" % i for i in range(9)) + "#EXT-X-ENDLIST\n"
    if kind == "blobfilm":
        return head + "#EXT-X-PLAYLIST-TYPE:VOD\n" + "".join("#EXTINF:6.0,\n/seg/blobfilm%d.ts\n" % i for i in range(50)) + "#EXT-X-ENDLIST\n"
    if kind == "short":
        return head + "#EXT-X-PLAYLIST-TYPE:VOD\n" + "".join("#EXTINF:6.0,\n/seg/short%d.ts\n" % i for i in range(2)) + "#EXT-X-ENDLIST\n"
    out = head + "#EXT-X-PLAYLIST-TYPE:VOD\n"
    for i in range(20):
        if i == 10: out += "#EXT-X-CUE-OUT:DURATION=12\n"
        if i == 12: out += "#EXT-X-CUE-IN\n"
        out += "#EXTINF:6.0,\n/seg/film%d.ts\n" % i
    return out + "#EXT-X-ENDLIST\n"

def tone_wav(seconds=30):
    """A real tone (440 Hz, 8-bit mono, 8 kHz, about 30 % of full scale): unlike silence the engine counts it as audible, so the media session of the page really starts."""
    import math
    n = 8000 * seconds
    hdr = b"RIFF" + struct.pack("<I", 36 + n) + b"WAVEfmt " + struct.pack("<IHHIIHH", 16, 1, 1, 8000, 8000, 1, 8) + b"data" + struct.pack("<I", n)
    return hdr + bytes(128 + int(40 * math.sin(2 * math.pi * 440 * i / 8000)) for i in range(n))


TONE = tone_wav(30)

PLAYLAB = b"""<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>playlab</title></head>
<body style="margin:0;font:18px sans-serif"><video id="v" src="/tone.wav" style="width:300px;height:80px;background:#222" preload="auto"></video>
<div id="go" style="position:fixed;left:0;top:0;right:0;bottom:0;background:rgba(0,160,0,.35);font-size:28px;padding:40px">TAP ANYWHERE to start the tone</div>
<script>
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v) + '&t=' + Date.now()); x.send(); } catch (e) {} }
var v = document.getElementById('v'), go = document.getElementById('go');
['play', 'pause', 'ended'].forEach(function (n) { v.addEventListener(n, function () { L('media', n); }); });
L('load', 'playlab');
go.addEventListener('click', function () {
  go.style.display = 'none';
  v.play().then(function () { L('started', 'by the tap'); }, function (e) { L('refused', String(e)); });
  setTimeout(function () { v.pause(); }, 6000);
  setTimeout(function () { v.play().then(function () { L('started', 'again'); }, function (e) { L('refused-again', String(e)); }); }, 9000);
  setTimeout(function () { v.pause(); }, 15000);
  setTimeout(function () { L('DONE', 'play, pause, play, pause'); }, 18000);
});
</script></body></html>
"""

SLOWJUMP = b"""<!doctype html><html><head><meta charset="utf-8"><title>slowjump</title></head><body style="font:18px sans-serif;margin:12px"><p>slowjump: no tap, 5 s, then this page sends its own tab to an ad host.</p>
<script>
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v) + '&t=' + Date.now()); x.send(); } catch (e) {} }
L('load', 'slowjump');
setTimeout(function () { L('jump', 'location = https://propellerads.com/'); location = 'https://propellerads.com/'; }, 5000);
setTimeout(function () { L('still-here', 'the page is still showing after 9 s (the jump was refused)'); }, 9000);
setTimeout(function () { L('DONE', 'slowjump'); }, 10000);
</script></body></html>
"""

LAYERLINK = b"""<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>layer-link</title></head><body style="font:18px sans-serif;margin:12px"><p>layer-link: tap anywhere.</p>
<div id="layer" style="position:fixed;left:0;top:0;right:0;bottom:0;background:rgba(200,0,0,.2)"></div>
<script>
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v) + '&t=' + Date.now()); x.send(); } catch (e) {} }
L('load', 'layer-link');
document.getElementById('layer').addEventListener('click', function () {
  L('layer-click', 'the layer got the tap and clicks a hidden target=_blank link to another site');
  var a = document.createElement('a'); a.href = 'http://127.0.0.1:%PORT%/popad?n=layer-link'; a.target = '_blank'; a.style.display = 'none'; document.body.appendChild(a); a.click();
});
setTimeout(function () { L('DONE', 'layer-link'); }, 14000);
</script></body></html>
"""

FAKELAYER = b"""<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>fake-layer</title></head>
<body style="margin:0;font:18px sans-serif"><video id="v" style="position:absolute;left:10px;top:60px;width:300px;height:200px;background:#246" src="/tone.wav" preload="auto"></video>
<div id="layer" style="position:fixed;left:0;top:0;width:100%;height:100%;opacity:0"></div>
<script>
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v) + '&t=' + Date.now()); x.send(); } catch (e) {} }
var v = document.getElementById('v'), layer = document.getElementById('layer');
v.addEventListener('click', function () { L('video-click', 'the video got the click'); v.play().catch(function () {}); });
layer.addEventListener('click', function () { L('layer-click', 'the invisible layer got the click and opens an ad'); window.open('http://127.0.0.1:%PORT%/popad?n=fake-layer', '_blank'); });
L('load', 'fake-layer');
setTimeout(function () { L('DONE', 'fake-layer'); }, 14000);
</script></body></html>
"""

PREROLL = b"""<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>preroll-main</title></head>
<body style="margin:8px;font:18px sans-serif"><video id="v" src="/ad15.wav" muted autoplay controls style="width:300px;height:170px;background:#223"></video>
<script>
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v) + '&t=' + Date.now()); x.send(); } catch (e) {} }
var v = document.getElementById('v');
v.addEventListener('playing', function () { L('playing', v.currentSrc.split('/').pop()); });
L('load', 'preroll-main');
var switched = false;
v.addEventListener('timeupdate', function () {      // tied to the real playback, not to a clock: the emulator stalls for seconds at a time
  if (switched || v.currentTime < 8) return;
  switched = true; L('switch', 'the same element gets the film after ' + v.currentTime.toFixed(1) + ' s of the ad');
  v.src = '/film60.wav'; v.muted = true; v.play().catch(function (e) { L('refused', String(e)); });
  setTimeout(function () { L('DONE', 'preroll-main'); }, 9000);
});
</script></body></html>
"""

POPAD = b"""<!doctype html><meta charset=utf-8><title>an ad</title><body style="font:20px sans-serif;margin:24px">popad: this is the ad page. If you can see it, the pop-up was NOT stopped.</body>"""


SEEKLAB = b"""<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>seekbar-lab</title>
<style>
body{margin:8px;font:15px sans-serif}
.pl{position:relative;width:340px;height:190px;background:#111;margin:0 0 6px 0}
.pl video{position:absolute;left:0;top:0;width:340px;height:190px}
.ch{position:absolute;left:0;right:0;bottom:0;height:46px}
.bar{position:absolute;left:10px;right:10px;bottom:12px;height:26px;touch-action:none}
.rail{position:absolute;left:0;right:0;top:11px;height:5px;background:rgba(255,255,255,.3)}
.hov{position:absolute;left:0;top:11px;height:5px;width:0;background:rgba(255,255,255,.7)}
.red{position:absolute;left:0;top:11px;height:5px;width:0;background:#f00}
.dot{position:absolute;top:6px;left:0;width:14px;height:14px;margin-left:-7px;border-radius:7px;background:#f00}
.vol{position:absolute;right:10px;top:10px;width:70px;height:12px;background:#999}
.t{font:14px monospace;margin:0 0 18px 0}
</style></head><body>
<p>seekbar-lab: tap the red bar of A, then B. C is a volume slider.</p>
<div id="pa" class="pl html5-video-player"><video id="va" preload="auto" muted playsinline></video>
 <div class="ch ytp-chrome-bottom"><div id="ba" class="bar ytp-progress-bar" role="slider" aria-label="Seek slider" aria-valuemin="0" aria-valuemax="300" aria-valuenow="0" tabindex="0">
  <div class="rail"></div><div class="hov ytp-hover-progress"></div><div class="red ytp-play-progress"></div><div class="dot ytp-scrubber-container"></div></div></div>
 <div id="volc" class="vol" role="slider" aria-label="Volume" aria-valuemin="0" aria-valuemax="100" aria-valuenow="50"></div>
 <div id="mys" class="mystery-strip" style="position:absolute;left:10px;right:10px;bottom:1px;height:9px;background:#444"></div></div>
<p class="t" id="ta">A: ...</p>
<div id="pb" class="pl html5-video-player"><video id="vb" preload="auto" muted playsinline></video>
 <div class="ch ytp-chrome-bottom"><div id="bb" class="bar ytp-progress-bar" role="slider" aria-label="Seek slider" aria-valuemin="0" aria-valuemax="300" aria-valuenow="0" tabindex="0">
  <div class="rail"></div><div class="hov ytp-hover-progress"></div><div class="red ytp-play-progress"></div><div class="dot ytp-scrubber-container"></div></div></div></div>
<p class="t" id="tb">B: ...</p>
<div id="pr" class="pl html5-video-player"><video id="vr" preload="auto" muted playsinline></video>
 <div class="player-controls-content" style="position:absolute;left:0;top:0;width:340px;height:190px">
  <yt-progress-bar class="ytPlayerProgressBarHost watch-page-progress-bar" style="display:block;position:absolute;left:20px;top:130px;width:300px;height:44px">
   <div id="br" class="ytPlayerProgressBarDragContainer" role="slider" aria-label="Seek slider" aria-valuemin="0" aria-valuemax="100" aria-valuenow="0" tabindex="0" style="display:block;position:static;height:44px;touch-action:none">
    <div class="ytPlayerProgressBarProgressBar" style="position:relative;height:0">
     <yt-progress-bar-line class="ytProgressBarLineHost" style="display:block;position:absolute;left:0;top:21px;width:300px;height:3px"><div id="lr" class="ytProgressBarLineProgressBarLine" style="position:relative;height:3px">
      <div class="ytProgressBarLineProgressBarBackground" style="position:absolute;left:0;top:0;width:100%;height:3px;background:#fff;opacity:.3"></div>
      <div class="ytProgressBarLineProgressBarLoaded" style="position:absolute;left:0;top:0;width:0;height:3px;background:#fff;opacity:.6"></div>
      <div id="hr" class="ytProgressBarLineProgressBarHovered" style="position:absolute;left:0;top:0;width:0;height:3px;background:#fff;opacity:.7"></div>
      <div id="pl" class="ytProgressBarLineProgressBarPlayed" style="position:absolute;left:0;top:0;width:0;height:3px;background:#f03"></div></div></yt-progress-bar-line>
     <yt-progress-bar-playhead class="ytProgressBarPlayheadHost" id="ph" style="display:block;position:absolute;left:-6px;top:16px;width:12px;height:12px"><div class="ytProgressBarPlayheadProgressBarPlayheadDot" style="width:12px;height:12px;border-radius:6px;background:#f03"></div></yt-progress-bar-playhead>
    </div></div></yt-progress-bar></div></div>
<p class="t" id="tr">R: ...</p>
<script>
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v)); x.send(); } catch (e) {} }
L('load', 'seekbar-lab ontouchstart=' + ('ontouchstart' in window) + ' maxTouchPoints=' + navigator.maxTouchPoints + ' PointerEvent=' + (typeof PointerEvent));
function frac(bar, x) { var r = bar.getBoundingClientRect(); return Math.max(0, Math.min(1, (x - r.left) / r.width)); }
function wire(id, bid, tid, name, jumps) {
  var v = document.getElementById(id), bar = document.getElementById(bid), t = document.getElementById(tid);
  var hov = bar.querySelector('.hov'), red = bar.querySelector('.red'), dot = bar.querySelector('.dot');
  function paint() { var d = v.duration || 300, w = (bar.getBoundingClientRect().width) * (v.currentTime / d); red.style.width = w + 'px'; dot.style.left = w + 'px'; t.textContent = name + ': ' + v.currentTime.toFixed(1) + ' s of ' + Math.round(d) + (v.paused ? ' (paused)' : ' (playing)'); }
  v.addEventListener('timeupdate', paint); v.addEventListener('seeked', paint); v.addEventListener('loadedmetadata', function () { L('meta', name + ' duration=' + v.duration); paint(); });
  v.addEventListener('seeking', function () { L('seeking', name + ' to ' + v.currentTime.toFixed(1)); });
  v.addEventListener('seeked', function () { L('seeked', name + ' at ' + v.currentTime.toFixed(1)); });
  v.addEventListener('play', function () { L('play', name); });
  // the hover line (what the owner saw: a light line up to the place that was pressed)
  function hover(e) { var x = (e.touches && e.touches[0]) ? e.touches[0].clientX : e.clientX; hov.style.width = (frac(bar, x) * bar.getBoundingClientRect().width) + 'px'; }
  ['mousemove', 'pointermove', 'touchstart', 'touchmove'].forEach(function (n) { bar.addEventListener(n, hover, { passive: true }); });
  ['pointerdown', 'pointerup', 'pointercancel', 'mousedown', 'mouseup', 'click', 'touchstart', 'touchend', 'touchcancel', 'contextmenu'].forEach(function (n) {
    bar.addEventListener(n, function (e) { var pt = (e.changedTouches && e.changedTouches[0]) ? e.changedTouches[0] : e; L('ev', name + ' ' + n + (e.pointerType ? ' pointerType=' + e.pointerType : '') + ' trusted=' + e.isTrusted + ' x=' + Math.round(pt.clientX) + ' y=' + Math.round(pt.clientY)); });
  });
  if (jumps) bar.addEventListener('pointerup', function (e) { var f = frac(bar, e.clientX); v.currentTime = f * v.duration; L('page-seek', name + ' to ' + (f * v.duration).toFixed(1)); });
  var lastBuf = '';
  function buf() { var b = v.buffered, o = []; for (var i = 0; i < b.length; i++) o.push(b.start(i).toFixed(0) + '-' + b.end(i).toFixed(0)); var t = o.join(' '); if (t !== lastBuf) { lastBuf = t; L('buffered', name + ' at ' + v.currentTime.toFixed(0) + ' s holds: ' + (t || 'nothing')); } }   // what the ENGINE holds (seconds): the proof of "nothing before the new place"
  setInterval(buf, 1000);
  setInterval(paint, 500);
  function rect() { var r = bar.getBoundingClientRect(); L('rect', name + ' left=' + Math.round(r.left) + ' top=' + Math.round(r.top) + ' w=' + Math.round(r.width) + ' h=' + Math.round(r.height) + ' inner=' + innerWidth + 'x' + innerHeight + ' dpr=' + devicePixelRatio + ' moz=' + window.mozInnerScreenX + ',' + window.mozInnerScreenY); }
  setTimeout(rect, 1500);
  window['rect' + name] = rect;
}
var N = Date.now();
var F = (location.search.indexOf('small') >= 0) ? '/seek-film.wav' : '/seek-big.wav';
document.getElementById('va').src = F + '?p=A&n=' + N; document.getElementById('vb').src = F + '?p=B&n=' + N;   // a new address per load: a repeated one is served from the engine's media cache without asking the server
wire('va', 'ba', 'ta', 'A', false);
wire('vb', 'bb', 'tb', 'B', true);
['pointerdown', 'pointerup', 'click'].forEach(function (n) { document.getElementById('volc').addEventListener(n, function (e) { L('ev', 'C ' + n + ' x=' + Math.round(e.clientX) + ' y=' + Math.round(e.clientY)); }); });   // the volume slider: its taps are only logged, they must never move a video
// R: the REAL current YouTube player bar (markup of the live embed, build 20261114): the page paints only its light "Hovered" line under the finger and does NOT seek on a tap (the owner's case)
(function () {
  var v = document.getElementById('vr'), bar = document.getElementById('br'), hov = document.getElementById('hr'), line = document.getElementById('lr'), t = document.getElementById('tr'), pl = document.getElementById('pl'), ph = document.getElementById('ph');
  v.src = '/seek-film.wav?p=R&n=' + N;
  function paint() { var w = bar.getBoundingClientRect().width * (v.currentTime / (v.duration || 300)); pl.style.width = w + 'px'; ph.style.left = (w - 6) + 'px'; t.textContent = 'R: ' + v.currentTime.toFixed(1) + ' s of ' + Math.round(v.duration || 300) + (v.paused ? ' (paused)' : ' (playing)'); }
  setInterval(paint, 500);
  ['pointerdown', 'pointermove'].forEach(function (n) { bar.addEventListener(n, function (e) { hov.style.width = (e.clientX - bar.getBoundingClientRect().left) + 'px'; }, { passive: true }); });
  ['pointerdown', 'pointerup', 'pointercancel', 'mousedown', 'mouseup', 'click', 'touchstart', 'touchend', 'touchcancel', 'contextmenu'].forEach(function (n) {
    bar.addEventListener(n, function (e) { var pt = (e.changedTouches && e.changedTouches[0]) ? e.changedTouches[0] : e; L('ev', 'R ' + n + (e.pointerType ? ' pointerType=' + e.pointerType : '') + ' x=' + Math.round(pt.clientX) + ' y=' + Math.round(pt.clientY)); });
  });
  v.addEventListener('seeking', function () { L('seeking', 'R to ' + v.currentTime.toFixed(1)); });
  v.addEventListener('seeked', function () { L('seeked', 'R at ' + v.currentTime.toFixed(1)); });
  setTimeout(function () { var r = line.getBoundingClientRect(), b = bar.getBoundingClientRect(); L('rect', 'R rail left=' + Math.round(b.left) + ' w=' + Math.round(b.width) + ' railTop=' + Math.round(r.top) + ' railH=' + Math.round(r.height) + ' barTop=' + Math.round(b.top) + ' barH=' + Math.round(b.height)); }, 1500);
  v.play().catch(function (e) { L('play-refused', 'R ' + String(e)); });
})();
document.getElementById('va').play().catch(function (e) { L('play-refused', String(e)); });   // A and B play (muted): the jump must keep playing
document.getElementById('vb').play().catch(function (e) { L('play-refused', String(e)); });
</script></body></html>
"""

_SEEKWAV = []
def seek_wav():
    if not _SEEKWAV:
        _SEEKWAV.append(base64.b64decode(silent_wav(300).split(",", 1)[1]))
    return _SEEKWAV[0]


BIGRATE, BIGSECS = 192000, 300                       # 48 kHz, 16 bit, stereo: 192 KB/s, so the 300 s film is 57.6 MB and the engine's read-ahead limit (60 s = 11.5 MB) matters
BIGLEN = 44 + BIGRATE * BIGSECS
BIGHDR = b"RIFF" + struct.pack("<I", BIGLEN - 8) + b"WAVEfmt " + struct.pack("<IHHIIHH", 16, 1, 2, 48000, BIGRATE, 4, 16) + b"data" + struct.pack("<I", BIGRATE * BIGSECS)


def stream_big(h, lo, hi, who, rng):
    """Writes bytes lo..hi of the big film in 64 KB chunks and logs how many the engine took before it closed the connection (or that it took them all)."""
    sent = 0
    try:
        pos = lo
        while pos <= hi:
            n = min(65536, hi - pos + 1)
            chunk = (BIGHDR[pos:44] if pos < 44 else b"") + b"\x00" * max(0, n - max(0, 44 - pos))
            h.wfile.write(chunk[:n]); sent += n; pos += n
        h._write("   player=%s Range=%s: all %d bytes sent (the engine read to the end of this request)" % (who, rng, sent))
    except (BrokenPipeError, ConnectionResetError):
        h._write("   player=%s Range=%s: the engine closed the connection after %d bytes = %.1f s of film from offset %d" % (who, rng, sent, sent / float(BIGRATE), lo))


class H(http.server.BaseHTTPRequestHandler):
    logfile = None

    def _send(self, code, body=b"", ctype="text/html; charset=utf-8"):
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def _write(self, line):
        with open(self.logfile, "a", encoding="utf-8") as f:
            f.write("%s %s\n" % (time.strftime("%H:%M:%S"), line))

    def do_GET(self):
        u = urllib.parse.urlparse(self.path)
        q = urllib.parse.parse_qs(u.query)
        if u.path == "/fakeplay":
            self._write("GET /fakeplay")
            return self._send(200, FAKEPLAY)
        if u.path == "/playerlab":
            self._write("GET /playerlab")
            return self._send(200, PLAYERLAB.replace(b"%WAV12%", WAV12.encode()).replace(b"%WAV20%", WAV20.encode()))
        if u.path == "/finderlab":
            self._write("GET /finderlab")
            return self._send(200, FINDERLAB)
        if u.path.startswith("/clip") and u.path.endswith(".wav"):
            self._write("GET " + u.path)
            return self._send(200, base64.b64decode(WAV20.split(",", 1)[1]), "audio/wav")
        if u.path == "/teaser-link":
            self._write("GET /teaser-link")
            return self._send(200, TEASERLINK)
        if u.path in SIZES:
            total = SIZES[u.path]
            rng = self.headers.get("Range", "")
            self._write("GET %s Range=%s" % (u.path, rng or "-"))
            if rng.startswith("bytes="):
                a, _, b = rng[6:].partition("-")
                lo = int(a or 0); hi = min(int(b) if b else total - 1, total - 1)
                body = b"\x00" * (hi - lo + 1)
                self.send_response(206); self.send_header("Content-Type", "video/mp4"); self.send_header("Content-Range", "bytes %d-%d/%d" % (lo, hi, total))
                self.send_header("Content-Length", str(len(body))); self.send_header("Accept-Ranges", "bytes"); self.end_headers(); self.wfile.write(body); return
            return self._send(200, b"\x00" * total, "video/mp4")
        if u.path == "/hls-lab":
            self._write("GET /hls-lab")
            return self._send(200, HLSLAB)
        if u.path in ("/live.m3u8", "/short.m3u8", "/film.m3u8"):
            self._write("GET " + u.path)
            return self._send(200, hls_playlist(u.path[1:].split(".")[0]).encode(), "application/vnd.apple.mpegurl")
        if u.path == "/blob-lab":
            self._write("GET /blob-lab")
            return self._send(200, BLOBLAB)
        if u.path in ("/blob-clip.m3u8", "/blob-film.m3u8"):
            self._write("GET " + u.path)
            return self._send(200, hls_playlist("blobclip" if "clip" in u.path else "blobfilm").encode(), "application/vnd.apple.mpegurl")
        if u.path == "/blob-film.wav":
            self._write("GET /blob-film.wav")
            return self._send(200, base64.b64decode(silent_wav(300).split(",", 1)[1]), "audio/wav")
        if u.path == "/seekbar-lab":
            self._write("GET /seekbar-lab")
            return self._send(200, SEEKLAB)
        if u.path == "/seek-big.wav":
            rng = self.headers.get("Range", ""); who = q.get("p", ["?"])[0]
            self._write("GET /seek-big.wav player=%s Range=%s" % (who, rng or "-"))
            lo, hi = 0, BIGLEN - 1
            if rng.startswith("bytes="):
                a, _, b = rng[6:].partition("-")
                lo = int(a or 0); hi = min(int(b) if b else BIGLEN - 1, BIGLEN - 1)
                self.send_response(206); self.send_header("Content-Range", "bytes %d-%d/%d" % (lo, hi, BIGLEN))
            else:
                self.send_response(200)
            self.send_header("Content-Type", "audio/wav"); self.send_header("Content-Length", str(hi - lo + 1)); self.send_header("Accept-Ranges", "bytes"); self.send_header("Cache-Control", "no-store"); self.end_headers()
            stream_big(self, lo, hi, who, rng or "-")
            return
        if u.path == "/seek-film.wav":
            body = seek_wav(); total = len(body); rng = self.headers.get("Range", "")
            self._write("GET /seek-film.wav player=%s Range=%s" % (q.get("p", ["?"])[0], rng or "-"))
            try:
                if rng.startswith("bytes="):
                    a, _, b = rng[6:].partition("-")
                    lo = int(a or 0); hi = min(int(b) if b else total - 1, total - 1)
                    part = body[lo:hi + 1]
                    self.send_response(206); self.send_header("Content-Type", "audio/wav"); self.send_header("Content-Range", "bytes %d-%d/%d" % (lo, hi, total))
                    self.send_header("Content-Length", str(len(part))); self.send_header("Accept-Ranges", "bytes"); self.send_header("Cache-Control", "no-store"); self.end_headers(); self.wfile.write(part)
                else:
                    self.send_response(200); self.send_header("Content-Type", "audio/wav"); self.send_header("Content-Length", str(total)); self.send_header("Accept-Ranges", "bytes"); self.send_header("Cache-Control", "no-store"); self.end_headers(); self.wfile.write(body)
            except (BrokenPipeError, ConnectionResetError):
                self._write("   (the engine closed the connection: it moved on)")
            return
        if u.path == "/playlab":
            self._write("GET /playlab")
            return self._send(200, PLAYLAB)
        if u.path == "/tone.wav":
            self._write("GET /tone.wav")
            return self._send(200, TONE, "audio/wav")
        if u.path == "/slowjump":
            self._write("GET /slowjump")
            return self._send(200, SLOWJUMP)
        if u.path == "/layer-link":
            self._write("GET /layer-link")
            return self._send(200, LAYERLINK.replace(b"%PORT%", str(self.server.server_address[1]).encode()))
        if u.path == "/fake-layer":
            self._write("GET /fake-layer")
            return self._send(200, FAKELAYER.replace(b"%PORT%", str(self.server.server_address[1]).encode()))
        if u.path == "/preroll-main":
            self._write("GET /preroll-main")
            return self._send(200, PREROLL)
        if u.path in ("/ad15.wav", "/film60.wav"):
            self._write("GET " + u.path)
            return self._send(200, base64.b64decode(silent_wav(15 if u.path == "/ad15.wav" else 60).split(",", 1)[1]), "audio/wav")
        if u.path == "/popad":
            self._write("GET /popad %s   <- the pop-up was NOT stopped" % q.get("n", ["?"])[0])
            return self._send(200, POPAD)
        if u.path == "/assist-parent":
            self._write("GET /assist-parent")
            return self._send(200, ASSIST_PARENT.replace(b"%PORT%", str(self.server.server_address[1]).encode()))
        if u.path == "/assist-child":
            self._write("GET /assist-child (host %s)" % self.headers.get("Host", "?"))
            return self._send(200, ASSIST_CHILD.replace(b"%PORT%", str(self.server.server_address[1]).encode()).replace(b"%WAV%", WAV.encode()))
        if u.path == "/clip.mp4":
            return self._send(404)
        if u.path == "/log":
            self._write("%s %s" % (q.get("kind", ["?"])[0], q.get("v", [""])[0]))
            return self._send(204)
        self._send(404)

    def log_message(self, *a):
        pass


if __name__ == "__main__":
    port, H.logfile = int(sys.argv[1]), sys.argv[2]
    open(H.logfile, "a").close()
    http.server.ThreadingHTTPServer(("0.0.0.0", port), H).serve_forever()
