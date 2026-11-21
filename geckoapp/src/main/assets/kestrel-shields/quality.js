'use strict';
// QL1 (owner message 2026-11-18: "When i put YouTube video in full screen, it doesn't let me choose the video quality. I want to be able to choose the video quality on as many sites and videos as possible").
// The isolated-world half of the quality choice (research/QL1_QUALITY.md). player.js (the page side of Kestrel's full-screen controls) asks this for the quality list of the film that is full screen and tells the
// app, which shows it behind the controls' Quality button; a pick comes back as a command and ends in set(). The page's OWN player does the switching, so the film goes on without a gap and without Kestrel's own
// player: it is the choice a site's gear menu would give, drawn by Kestrel.
//
// Families, in the order they are tried (the first that offers at least two rows wins):
//   yt     YouTube (desktop and mobile site): #movie_player / .html5-video-player answers getAvailableQualityLevels(), setPlaybackQualityRange(min, max), getPlaybackQuality().
//   jw     JW Player 8: jwplayer(id).getQualityLevels() / getCurrentQuality() / setCurrentQuality(i).
//   vjs    Video.js with videojs-contrib-quality-levels: player.qualityLevels() (each level has height and a writable `enabled`).
//   hls    hls.js: hls.levels, hls.currentLevel (-1 = automatic), hls.autoLevelEnabled.   (found through quality-main.js, which notes every `new Hls()` of a page that sets the global)
//   dash   dash.js: getBitrateInfoListFor('video'), setQualityFor('video', i), the ABR switch.   (same)
//   shaka  Shaka Player: getVariantTracks(), selectVariantTrack(track, clearBuffer), configure({abr: {enabled}}).   (same)
//   src    a <video> whose <source> elements carry a size / label (Plyr, Video.js and hand-made pages): the film moves to the other file at the same second.
// The page's objects are reached through `wrappedJSObject` (Firefox: the page's own view of an element or of window), numbers and strings only go in, only short strings and numbers are read out and the app
// checks them again. Nothing here runs unless player.js calls it (a film is full screen under Kestrel's controls). Conservative ES5; every call is in try/catch: a player that throws just offers no list.
window.KestrelQ = window.KestrelQ || (function () {
  var api = {};
  var MAXROWS = 12, MAXID = 24, MAXLABEL = 22;

  function W(x) { try { return (x && x.wrappedJSObject) ? x.wrappedJSObject : x; } catch (e) { return x; } }    // the page's own object (tests: the object itself)
  function into(o) { try { return typeof cloneInto === 'function' ? cloneInto(o, window) : o; } catch (e) { return o; } }   // an object that goes INTO a page function
  function isFn(f) { return typeof f === 'function'; }
  function str(x, n) { return String(x === undefined || x === null ? '' : x).replace(/[\u0000-\u001f<>]/g, ' ').replace(/\s+/g, ' ').replace(/^ | $/g, '').substring(0, n || MAXLABEL); }
  function closest(el, sel) { try { return el && el.closest ? el.closest(sel) : null; } catch (e) { return null; } }
  function row(id, label) { return { i: str(id, MAXID).replace(/[^A-Za-z0-9_.:\-]/g, '_'), l: str(label, MAXLABEL) }; }
  function numLabel(h, w, kbps) {
    if (h > 0) return h + 'p';
    if (w > 0) return w + 'w';
    if (kbps > 0) return Math.round(kbps) + ' kbit/s';
    return '';
  }
  function dedupe(list) {                                   // one row per id, at most MAXROWS
    var out = [], seen = {};
    for (var i = 0; i < list.length && out.length < MAXROWS; i++) {
      var r = list[i];
      if (!r || !r.i || seen[r.i]) continue;
      seen[r.i] = 1; out.push(r);
    }
    return out;
  }

  // ------------------------------------------------------------------------------------------ YouTube
  var YT_LABEL = { highres: '4320p+', hd4320: '4320p', hd2880: '2880p', hd2160: '2160p', hd1440: '1440p', hd1080: '1080p', hd720: '720p', large: '480p', medium: '360p', small: '240p', tiny: '144p', auto: 'Automatic' };
  var YT_ORDER = ['highres', 'hd4320', 'hd2880', 'hd2160', 'hd1440', 'hd1080', 'hd720', 'large', 'medium', 'small', 'tiny'];
  function ytPlayer(video) {
    var el = closest(video, '#movie_player') || closest(video, '.html5-video-player');
    if (!el) return null;
    var p = W(el);
    return (p && isFn(p.getAvailableQualityLevels) && isFn(p.setPlaybackQualityRange || p.setPlaybackQuality)) ? p : null;
  }
  function ytAdapter(video) {
    var p = ytPlayer(video);
    if (!p) return null;
    return {
      fam: 'yt',
      levels: function () {
        var ids = [], labels = {}, out = [];
        try { var a = p.getAvailableQualityLevels(); for (var i = 0; a && i < a.length && i < 20; i++) { var id = String(a[i]); if (id && id !== 'auto' && id.length <= MAXID) ids.push(id); } } catch (e) { return []; }
        try {                                                // the site's own labels (1080p60, 720p HDR ...) when it has them
          if (isFn(p.getAvailableQualityData)) { var d = p.getAvailableQualityData(); for (var j = 0; d && j < d.length && j < 20; j++) { var q = d[j]; if (q && q.quality && q.qualityLabel) labels[String(q.quality)] = String(q.qualityLabel); } }
        } catch (e2) { /* the table below */ }
        ids.sort(function (x, y) { var a1 = YT_ORDER.indexOf(x), b1 = YT_ORDER.indexOf(y); return (a1 < 0 ? 99 : a1) - (b1 < 0 ? 99 : b1); });
        out.push(row('auto', 'Automatic'));
        for (var k = 0; k < ids.length; k++) out.push(row(ids[k], labels[ids[k]] || YT_LABEL[ids[k]] || ids[k]));
        return dedupe(out);
      },
      cur: function () { try { return String(p.getPlaybackQuality()); } catch (e) { return ''; } },
      set: function (id) {
        try {
          if (id === 'auto') { if (isFn(p.setPlaybackQualityRange)) p.setPlaybackQualityRange('tiny', 'highres'); return true; }     // a range from the smallest to the biggest = the site chooses
          if (isFn(p.setPlaybackQualityRange)) p.setPlaybackQualityRange(id, id);
          if (isFn(p.setPlaybackQuality)) p.setPlaybackQuality(id);
          return true;
        } catch (e) { return false; }
      }
    };
  }

  // ------------------------------------------------------------------------------------------ JW Player
  function jwAdapter(video) {
    var root = closest(video, '.jwplayer');
    if (!root || !root.id) return null;
    var win = W(window), jw = null;
    try { if (win && isFn(win.jwplayer)) jw = win.jwplayer(root.id); } catch (e) { jw = null; }
    if (!jw || !isFn(jw.getQualityLevels) || !isFn(jw.setCurrentQuality)) return null;
    return {
      fam: 'jw',
      levels: function () {
        var out = [];
        try { var a = jw.getQualityLevels(); for (var i = 0; a && i < a.length && i < MAXROWS; i++) { var lb = a[i] && a[i].label ? String(a[i].label) : ''; out.push(row(String(i), /^auto$/i.test(lb) ? 'Automatic' : lb || ('level ' + i))); } } catch (e) { return []; }
        return dedupe(out);
      },
      cur: function () { try { return String(jw.getCurrentQuality()); } catch (e) { return ''; } },
      set: function (id) { try { var n = parseInt(id, 10); if (!(n >= 0 && n < 50)) return false; jw.setCurrentQuality(n); return true; } catch (e) { return false; } }
    };
  }

  // ------------------------------------------------------------------------------------------ Video.js (videojs-contrib-quality-levels)
  function vjsAdapter(video) {
    var root = closest(video, '.video-js');
    if (!root) return null;
    var win = W(window), pl = null;
    try {
      var vjs = win && win.videojs;
      if (vjs && root.id) pl = isFn(vjs.getPlayer) ? vjs.getPlayer(root.id) : (isFn(vjs) ? vjs(root.id) : null);
    } catch (e) { pl = null; }
    if (!pl || !isFn(pl.qualityLevels)) return null;
    var ql = null;
    try { ql = pl.qualityLevels(); } catch (e2) { ql = null; }
    if (!ql || !(ql.length > 0)) return null;
    function heights() { var a = [], i; for (i = 0; i < ql.length && i < 30; i++) a.push({ i: i, h: ql[i].height | 0, w: ql[i].width | 0, b: (ql[i].bitrate | 0) / 1000 }); return a; }
    return {
      fam: 'vjs',
      levels: function () {
        var h = heights(), seen = {}, out = [row('auto', 'Automatic')];
        h.sort(function (x, y) { return (y.h - x.h) || (y.b - x.b); });
        for (var i = 0; i < h.length; i++) { var lb = numLabel(h[i].h, h[i].w, h[i].b); if (!lb || seen[lb]) continue; seen[lb] = 1; out.push(row('h' + (h[i].h || Math.round(h[i].b)), lb)); }
        return dedupe(out);
      },
      cur: function () { try { var si = ql.selectedIndex; if (si >= 0 && si < ql.length) { var all = true, i; for (i = 0; i < ql.length; i++) if (!ql[i].enabled) all = false; if (all) return 'auto'; return 'h' + (ql[si].height | 0 || Math.round((ql[si].bitrate | 0) / 1000)); } } catch (e) { /* none */ } return ''; },
      set: function (id) {
        try {
          var i, want = -1;
          if (id !== 'auto') { var n = parseInt(String(id).substring(1), 10); for (i = 0; i < ql.length; i++) { if ((ql[i].height | 0) === n || Math.round((ql[i].bitrate | 0) / 1000) === n) want = i; } if (want < 0) return false; }
          for (i = 0; i < ql.length; i++) ql[i].enabled = (want < 0) || i === want;
          return true;
        } catch (e) { return false; }
      }
    };
  }

  // ------------------------------------------------------------------------------------------ hls.js, dash.js, Shaka (found through quality-main.js)
  function registry() { try { var r = W(window).__kstq1; return r && r.length ? r : []; } catch (e) { return []; } }
  function sameMedia(a, b) { try { return a === b || W(a) === W(b); } catch (e) { return false; } }

  function hlsAdapter(video) {
    var reg = registry(), h = null, i;
    for (i = reg.length - 1; i >= 0 && !h; i--) { try { if (reg[i].k === 'hls' && reg[i].o && sameMedia(reg[i].o.media, video) && reg[i].o.levels && reg[i].o.levels.length > 1) h = reg[i].o; } catch (e) { /* next */ } }
    if (!h) return null;
    return {
      fam: 'hls',
      levels: function () {
        var out = [row('auto', 'Automatic')], a = [];
        try { for (var j = 0; j < h.levels.length && j < 30; j++) a.push({ i: j, h: h.levels[j].height | 0, w: h.levels[j].width | 0, b: (h.levels[j].bitrate | 0) / 1000 }); } catch (e) { return []; }
        a.sort(function (x, y) { return (y.h - x.h) || (y.b - x.b); });
        var seen = {};
        for (var k = 0; k < a.length; k++) { var lb = numLabel(a[k].h, a[k].w, a[k].b); var key = lb + ':' + a[k].h; if (!lb || seen[key]) continue; seen[key] = 1; out.push(row('l' + a[k].i, lb)); }
        return dedupe(out);
      },
      cur: function () { try { if (h.autoLevelEnabled || h.currentLevel < 0) return 'auto'; return 'l' + h.currentLevel; } catch (e) { return ''; } },
      set: function (id) {
        try { if (id === 'auto') { h.currentLevel = -1; return true; } var n = parseInt(String(id).substring(1), 10); if (!(n >= 0 && n < h.levels.length)) return false; h.currentLevel = n; return true; } catch (e) { return false; }
      }
    };
  }

  function dashAdapter(video) {
    var reg = registry(), d = null, i;
    for (i = reg.length - 1; i >= 0 && !d; i--) {
      try { var o = reg[i].o; if (reg[i].k === 'dash' && o && isFn(o.getBitrateInfoListFor) && isFn(o.setQualityFor) && (!isFn(o.getVideoElement) || sameMedia(o.getVideoElement(), video))) { var l = o.getBitrateInfoListFor('video'); if (l && l.length > 1) d = o; } } catch (e) { /* next */ }
    }
    if (!d) return null;
    function setAbr(on) {
      try { if (isFn(d.updateSettings)) { d.updateSettings(into({ streaming: { abr: { autoSwitchBitrate: { video: !!on } } } })); return; } } catch (e) { /* the older call */ }
      try { if (isFn(d.setAutoSwitchQualityFor)) d.setAutoSwitchQualityFor('video', !!on); } catch (e2) { /* none */ }
    }
    return {
      fam: 'dash',
      levels: function () {
        var out = [row('auto', 'Automatic')], l;
        try { l = d.getBitrateInfoListFor('video'); } catch (e) { return []; }
        var a = [];
        for (var j = 0; l && j < l.length && j < 30; j++) a.push({ i: (l[j].qualityIndex !== undefined ? l[j].qualityIndex : j) | 0, h: l[j].height | 0, w: l[j].width | 0, b: (l[j].bitrate | 0) / 1000 });
        a.sort(function (x, y) { return (y.h - x.h) || (y.b - x.b); });
        var seen = {};
        for (var k = 0; k < a.length; k++) { var lb = numLabel(a[k].h, a[k].w, a[k].b); if (!lb || seen[lb]) continue; seen[lb] = 1; out.push(row('q' + a[k].i, lb)); }
        return dedupe(out);
      },
      cur: function () {
        try { var auto = isFn(d.getSettings) ? d.getSettings().streaming.abr.autoSwitchBitrate.video : (isFn(d.getAutoSwitchQualityFor) ? d.getAutoSwitchQualityFor('video') : false); if (auto) return 'auto'; return 'q' + d.getQualityFor('video'); } catch (e) { return ''; }
      },
      set: function (id) {
        try { if (id === 'auto') { setAbr(true); return true; } var n = parseInt(String(id).substring(1), 10); if (!(n >= 0 && n < 60)) return false; setAbr(false); d.setQualityFor('video', n); return true; } catch (e) { return false; }
      }
    };
  }

  function shakaAdapter(video) {
    var reg = registry(), s = null, i;
    for (i = reg.length - 1; i >= 0 && !s; i--) {
      try { var o = reg[i].o; if (reg[i].k === 'shaka' && o && isFn(o.getVariantTracks) && isFn(o.selectVariantTrack) && (!isFn(o.getMediaElement) || sameMedia(o.getMediaElement(), video))) { var t = o.getVariantTracks(); if (t && t.length > 1) s = o; } } catch (e) { /* next */ }
    }
    if (!s) return null;
    function tracks() { try { return s.getVariantTracks(); } catch (e) { return []; } }
    return {
      fam: 'shaka',
      levels: function () {
        var out = [row('auto', 'Automatic')], t = tracks(), best = {}, j;
        for (j = 0; j < t.length && j < 60; j++) { var h = t[j].height | 0, b = (t[j].bandwidth | 0) / 1000; var key = h || ('b' + Math.round(b)); if (!best[key] || b > best[key].b) best[key] = { h: h, w: t[j].width | 0, b: b }; }
        var keys = []; for (var kx in best) if (Object.prototype.hasOwnProperty.call(best, kx)) keys.push(kx);
        keys.sort(function (x, y) { return (best[y].h - best[x].h) || (best[y].b - best[x].b); });
        for (var k = 0; k < keys.length; k++) { var lb = numLabel(best[keys[k]].h, best[keys[k]].w, best[keys[k]].b); if (lb) out.push(row('v' + keys[k], lb)); }
        return dedupe(out);
      },
      cur: function () {
        try { var auto = true; try { auto = !!s.getConfiguration().abr.enabled; } catch (e0) { auto = true; } if (auto) return 'auto'; var t = tracks(); for (var j = 0; j < t.length; j++) if (t[j].active) return 'v' + ((t[j].height | 0) || ('b' + Math.round((t[j].bandwidth | 0) / 1000))); } catch (e) { /* none */ }
        return '';
      },
      set: function (id) {
        try {
          if (id === 'auto') { s.configure(into({ abr: { enabled: true } })); return true; }
          var want = String(id).substring(1), t = tracks(), pick = null, j;
          for (j = 0; j < t.length; j++) { var h = t[j].height | 0, key = h ? String(h) : 'b' + Math.round((t[j].bandwidth | 0) / 1000); if (key === want && (!pick || (t[j].bandwidth | 0) > (pick.bandwidth | 0))) pick = t[j]; }
          if (!pick) return false;
          s.configure(into({ abr: { enabled: false } }));
          s.selectVariantTrack(pick, true);
          return true;
        } catch (e) { return false; }
      }
    };
  }

  // ------------------------------------------------------------------------------------------ plain <source> lists
  function srcAdapter(video) {
    var list = [], nodes;
    try { nodes = video.querySelectorAll('source'); } catch (e) { return null; }
    for (var i = 0; nodes && i < nodes.length && i < 12; i++) {
      var n = nodes[i], u = n.getAttribute('src') || '';
      var lb = n.getAttribute('size') || n.getAttribute('data-res') || n.getAttribute('res') || n.getAttribute('data-quality') || n.getAttribute('label') || n.getAttribute('title') || '';
      lb = str(lb, 12);
      if (!u || !lb) continue;
      if (/^\d{3,4}$/.test(lb)) lb += 'p';
      list.push({ id: 's' + i, label: lb, src: n.src || u });
    }
    var seen = {}, uniq = [];
    for (var j = 0; j < list.length; j++) { if (seen[list[j].label]) continue; seen[list[j].label] = 1; uniq.push(list[j]); }
    if (uniq.length < 2) return null;
    return {
      fam: 'src',
      levels: function () {
        var out = [], a = uniq.slice();
        a.sort(function (x, y) { return (parseInt(y.label, 10) || 0) - (parseInt(x.label, 10) || 0); });
        for (var k = 0; k < a.length; k++) out.push(row(a[k].id, a[k].label));
        return dedupe(out);
      },
      cur: function () { try { var cs = video.currentSrc || video.src || ''; for (var k = 0; k < uniq.length; k++) if (uniq[k].src === cs) return uniq[k].id; } catch (e) { /* none */ } return ''; },
      set: function (id) {
        try {
          var pick = null, k;
          for (k = 0; k < uniq.length; k++) if (uniq[k].id === id) pick = uniq[k];
          if (!pick) return false;
          var t = video.currentTime || 0, was = !video.paused, rate = video.playbackRate || 1;
          var back = function () { try { video.removeEventListener('loadedmetadata', back); video.currentTime = t; video.playbackRate = rate; if (was) { var p = video.play(); if (p && p.catch) p.catch(function () { /* the page decides */ }); } } catch (e) { /* ignore */ } };
          video.addEventListener('loadedmetadata', back);
          video.src = pick.src;
          video.load();
          return true;
        } catch (e) { return false; }
      }
    };
  }

  var FAMILIES = [ytAdapter, jwAdapter, vjsAdapter, hlsAdapter, dashAdapter, shakaAdapter, srcAdapter];

  /** The adapter of the film [video] (or null): the first family that has at least two rows. */
  api.find = function (video) {
    if (!video) return null;
    for (var i = 0; i < FAMILIES.length; i++) {
      try {
        var a = FAMILIES[i](video);
        if (a) { var l = a.levels(); if (l && l.length >= 2) return a; }
      } catch (e) { /* the next family */ }
    }
    return null;
  };
  api.families = ['yt', 'jw', 'vjs', 'hls', 'dash', 'shaka', 'src'];
  return api;
})();
