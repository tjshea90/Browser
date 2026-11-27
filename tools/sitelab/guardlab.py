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
  /fake-layer RX3-G4: a <video> (each click on it is logged) and over it an INVISIBLE full-screen layer whose click handler opens an ad window. One real tap on the video: with the guard's fake-layer switch-off
              the video gets the click and the layer none ("video-click" only); without it the layer takes the first tap ("layer-click", the ad is refused) and the video plays on the second try.
  /playerlab  RX3-G7: five mock video players on one page, driven by the page itself (no touch needed): an ordinary video (no ad words), a JW-style player (jw-flag-ads on its root), a Video.js-style player
              (vjs-ad-loading, then vjs-ad-playing), a Google-IMA-style host (the ad video inside .ima-ad-container) and a JW-style ad with a skip button (.jw-skip, .jw-skippable). Each ad is a REAL 12 s silent
              media element. The log says how long the ad took (about 12000 ms = not skipped, a few hundred = fast-forwarded), at what speed it ran, and which events the skip button got. Ends with a DONE line.

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

def hls_playlist(kind):
    head = "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:6\n#EXT-X-MEDIA-SEQUENCE:0\n"
    if kind == "live":
        return head + "".join("#EXTINF:6.0,\n/seg/live%d.ts\n" % i for i in range(4))
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

POPAD = b"""<!doctype html><meta charset=utf-8><title>an ad</title><body style="font:20px sans-serif;margin:24px">popad: this is the ad page. If you can see it, the pop-up was NOT stopped.</body>"""


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
