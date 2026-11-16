'use strict';
// ANS52h (research/VIDEO_ACCELERATION.md section "ANS52h design"): the picture size cap for HLS pages. A site's player picks its picture size from the rungs of the stream's MASTER playlist, and on a tablet
// whose screen is 973 x 600 pixels the site's default is still the 1920 x 1080 rung (the owner's own diagnostics: a Boltdns film, 1920 x 1080 in a 600 x 947 view): decoded, copied and drawn at 1.9 times the
// pixels the glass can show, over a 5 Mbit/s hotspot. The extension answers the request for that playlist with the rungs above the cap taken out (background.js: webRequest.filterResponseData); the player
// never sees them and its own bitrate logic picks among the rest. This file is the pure part (the text rewrite), so it can be tested without a browser: web-tests/hlscap.test.js.
//
// The rules (never an error for the page: a text this file does not understand is handed back as it was, null):
//  - only a MASTER playlist (it has `#EXT-X-STREAM-INF`); a media playlist, a segment list, any other text: null;
//  - a rung is the `#EXT-X-STREAM-INF:` line and the address line right after it; it goes when its RESOLUTION's LONGER side is more than 2 % above the cap (a portrait film is judged by its long side too);
//  - a rung with no RESOLUTION (audio only, or a site that leaves it out) always stays, and so do the rungs of the SMALLEST size that has one: a master whose rungs are all above the cap loses all but those;
//  - `#EXT-X-I-FRAME-STREAM-INF:` lines (the picture strip a seek bar shows) above the cap go as well; everything else (EXT-X-MEDIA, the audio groups, comments, the order, the line endings) stays byte for byte;
//  - a stream-inf tag with no address line right after it is left alone.
// Conservative ES5 (it runs in the background page of the extension, but the same rule as every script of the extension).
(function (root) {
  var STREAM_INF = '#EXT-X-STREAM-INF:', FRAME_INF = '#EXT-X-I-FRAME-STREAM-INF:';
  var RES = /(?:^|[:,])RESOLUTION=(\d+)x(\d+)/;
  var SLACK = 1.02;

  function longSide(line) {
    var m = RES.exec(line);
    if (!m) return 0;
    var a = +m[1], b = +m[2];
    return (a > 0 && b > 0) ? Math.max(a, b) : 0;
  }

  /** @returns null (nothing to change) or { text, dropped: rungs taken out, kept: rungs with a picture left, frames: strips taken out } */
  function rewriteMaster(text, cap) {
    if (typeof text !== 'string' || !(cap > 0) || text.indexOf(STREAM_INF) < 0) return null;
    var eol = text.indexOf('\r\n') >= 0 ? '\r\n' : '\n';
    var lines = text.split(/\r\n|\n/);
    var rungs = [], i;
    for (i = 0; i < lines.length; i++) {
      var l = lines[i];
      if (l.indexOf(STREAM_INF) === 0) {
        var u = lines[i + 1];
        if (u !== undefined && u !== '' && u.charAt(0) !== '#') rungs.push({ at: i, n: 2, size: longSide(l), frame: false });
      } else if (l.indexOf(FRAME_INF) === 0) {
        rungs.push({ at: i, n: 1, size: longSide(l), frame: true });
      }
    }
    var limit = cap * SLACK, pictures = [], over = 0;
    for (i = 0; i < rungs.length; i++) if (!rungs[i].frame && rungs[i].size > 0) { pictures.push(rungs[i]); if (rungs[i].size > limit) over++; }
    var floor = 0;                                                     // every rung with a picture is too big: those of the SMALLEST size stay (all of them, so the player keeps its choice of bit rates)
    if (pictures.length > 0 && over === pictures.length) {
      floor = pictures[0].size;
      for (i = 1; i < pictures.length; i++) if (pictures[i].size < floor) floor = pictures[i].size;
    }
    var gone = {}, dropped = 0, frames = 0;
    for (i = 0; i < rungs.length; i++) {
      var r = rungs[i];
      if (r.size > limit && r.size > floor) {
        for (var k = 0; k < r.n; k++) gone[r.at + k] = true;
        if (r.frame) frames++; else dropped++;
      }
    }
    if (dropped === 0 && frames === 0) return null;
    var out = [];
    for (i = 0; i < lines.length; i++) if (!gone[i]) out.push(lines[i]);
    return { text: out.join(eol), dropped: dropped, kept: pictures.length - dropped, frames: frames };
  }

  var api = { rewriteMaster: rewriteMaster, longSide: longSide };
  root.KestrelHls = api;
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
})(typeof self !== 'undefined' ? self : this);
