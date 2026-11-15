// VC1 / LP1 (owner message 44, research/VIDEO_CONTROLS.md): finds the videos that the page's own DOM hides from `document.getElementsByTagName('video')` and from `elementsFromPoint`: videos inside
// shadow roots (finance.yahoo.com's player is a web component: its <video> is not in the document's element list and the point test returns only the shadow HOST), and videos under a layer with
// pointer-events: none. It is published as window.KestrelVF (the content scripts of one extension share the window's expandos in a frame), so the file only has to be listed before the files that use it (content.js, videobar.js); a second load keeps the first.
// Everything here is read-only, conservative ES5, bounded (a scan stops after a few thousand elements) and does nothing until another script asks.
window.KestrelVF = window.KestrelVF || (function () {
  var MIN_W = 80, MIN_H = 45;      // a video smaller than this (in CSS pixels) is a thumbnail or a decoration: no bar, no menu
  var SCAN = 1500;                 // elements looked at below the elements under a point (a press), more for a whole-document scan

  function rootOf(el) {            // Firefox gives content scripts the root of a CLOSED shadow tree too
    try { return el.openOrClosedShadowRoot || el.shadowRoot || null; } catch (e) { return null; }
  }

  // every element with this tag name inside `root`, also inside the shadow roots below it (four levels, [budget.n] elements looked at)
  function collect(root, out, budget, depth, tag) {
    try {
      var list = root.querySelectorAll(tag), i;   // (a ShadowRoot has no getElementsByTagName)
      for (i = 0; i < list.length; i++) out.push(list[i]);
      if (depth >= 4) return;
      var all = root.querySelectorAll('*');
      for (i = 0; i < all.length && budget.n > 0; i++) {
        budget.n--;
        var sr = rootOf(all[i]);
        if (sr) collect(sr, out, budget, depth + 1, tag);
      }
    } catch (e) { /* never break the page */ }
  }

  function unique(list) {
    var out = [];
    for (var i = 0; i < list.length; i++) if (out.indexOf(list[i]) < 0) out.push(list[i]);
    return out;
  }

  // the picture's rectangle when the element is shown at a size worth a bar, else null
  function rect(el) {
    try {
      var cs = window.getComputedStyle(el);
      if (cs.display === 'none' || cs.visibility === 'hidden') return null;
      var r = el.getBoundingClientRect();
      if (r.width < MIN_W || r.height < MIN_H) return null;
      if (r.bottom < 0 || r.right < 0 || r.top > window.innerHeight || r.left > window.innerWidth) return null;
      return r;
    } catch (e) { return null; }
  }

  function playing(v) { return !v.paused && !v.ended; }
  function area(r) { return r.width * r.height; }

  // the best video under a point (client coordinates) or null: playing first, then the larger one
  function at(x, y, limit) {
    var vids = [];
    try {
      var els = document.elementsFromPoint ? document.elementsFromPoint(x, y) : [];
      var budget = { n: limit || SCAN };
      for (var i = 0; i < els.length && i < 24; i++) {
        if (els[i].tagName === 'VIDEO') vids.push(els[i]);
        var sr = rootOf(els[i]);
        if (sr) collect(sr, vids, budget, 1, 'video');
      }
      var light = document.getElementsByTagName('video');
      for (var k = 0; k < light.length && k < 60; k++) vids.push(light[k]);
    } catch (e) { /* never break the page */ }
    vids = unique(vids);
    var best = null, bestR = null;
    for (var m = 0; m < vids.length; m++) {
      var r = rect(vids[m]);
      if (!r || x < r.left || x > r.right || y < r.top || y > r.bottom) continue;
      if (!best || (playing(vids[m]) ? 1 : 0) > (playing(best) ? 1 : 0) || ((playing(vids[m]) ? 1 : 0) === (playing(best) ? 1 : 0) && area(r) > area(bestR))) { best = vids[m]; bestR = r; }
    }
    return best;
  }

  // every video of the document, also inside shadow roots (a whole-document scan: only for rare questions, never per touch)
  function all(tag) {
    var out = [];
    collect(document, out, { n: 20000 }, 0, tag || 'video');
    return unique(out);
  }

  // a video that a FRAME of that size lies over (a tap or long press landed inside a frame: ad overlays such as Google's IMA cover the player and take every touch). The frame itself is not known to the
  // frame's own script (it cannot see its element), only its size, so the match is by size.
  function covered(w, h) {
    try {
      var vids = all('video'), best = null, bestR = null;
      if (!vids.length) return null;
      var frames = all('iframe');
      for (var i = 0; i < frames.length; i++) {
        var fr = frames[i].getBoundingClientRect();
        if (Math.abs(fr.width - w) > 4 || Math.abs(fr.height - h) > 4 || fr.width < 60 || fr.height < 40) continue;
        for (var j = 0; j < vids.length; j++) {
          var vr = rect(vids[j]);
          if (!vr) continue;
          var ox = Math.min(fr.right, vr.right) - Math.max(fr.left, vr.left), oy = Math.min(fr.bottom, vr.bottom) - Math.max(fr.top, vr.top);
          if (ox <= 0 || oy <= 0 || ox * oy < 0.4 * Math.min(area(fr), area(vr))) continue;
          if (!best || (playing(vids[j]) ? 1 : 0) > (playing(best) ? 1 : 0) || ((playing(vids[j]) ? 1 : 0) === (playing(best) ? 1 : 0) && area(vr) > area(bestR))) { best = vids[j]; bestR = vr; }
        }
      }
      return best;
    } catch (e) { return null; }
  }

  // the person long pressed this video: tell the app, which shows its Save video sheet (the finder's list, Copy video address, or the line that says why nothing was found). The video's own address goes
  // along only when it is a web address (a blob: address names nothing the app could fetch).
  function menu(v) {
    try {
      var src = v.currentSrc || v.src || '';
      browser.runtime.sendMessage({ k: 'pop', d: 'vmenu|' + (src.indexOf('http') === 0 ? src : ''), p: location.href }).catch(function () {});
    } catch (e) { /* nobody listens */ }
  }

  // LAB2 (owner message 45): the page-health probe of the emulator lab. Only a DEBUG build asks for it (the guard answer carries lab: true); two and a half seconds after the page has loaded it counts what the
  // page really has: text, pictures (and broken ones), frames, videos, size, nodes, requests. Numbers only. A page that loaded but has no text, only broken pictures or overflows the screen is "not loading correctly".
  var probed = false;
  function probe() {
    if (probed || window.top !== window) return;
    probed = true;
    function go() {
      setTimeout(function () {
        try {
          var d = document, b = d.body, imgs = d.images, ib = 0, iv = 0, i, vd = 0, vp = 0, vids = all('video');
          for (i = 0; i < imgs.length && i < 600; i++) {
            if (imgs[i].complete && imgs[i].naturalWidth === 0 && (imgs[i].currentSrc || imgs[i].src)) ib++;
            else if (imgs[i].naturalWidth > 20) iv++;
          }
          vd = vids.length; for (i = 0; i < vids.length; i++) if (playing(vids[i])) vp++;
          var el = d.documentElement, rs = 0;
          try { rs = performance.getEntriesByType('resource').length; } catch (e1) { /* no timing */ }
          browser.runtime.sendMessage({ k: 'probe', h: location.hostname, t: b ? (b.innerText || '').length : 0, im: imgs.length, ib: ib, iv: iv, fr: d.getElementsByTagName('iframe').length, vd: vd, vp: vp,
            hh: el ? el.scrollHeight : 0, ww: el ? el.scrollWidth : 0, iw: window.innerWidth, sc: d.scripts.length, nd: d.getElementsByTagName('*').length, rs: rs, rd: d.readyState }).catch(function () {});
        } catch (e) { /* never break the page */ }
      }, 2500);
    }
    if (document.readyState === 'complete') go(); else window.addEventListener('load', go);
  }

  return { at: at, all: all, covered: covered, rect: rect, rootOf: rootOf, playing: playing, menu: menu, probe: probe };
})();
