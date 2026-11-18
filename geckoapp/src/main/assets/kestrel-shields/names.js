'use strict';
// NM1 (owner message 2026-11-18: "i downloaded ozark season 1 episode 1, but it named it just ozark. Then when i went to download episode 2, it also named it just ozark ... deduce what each video should be named
// based on different clues such as the site or link name or if i press episode 2 on the site"): the page side of the naming. A series page often has ONE title and ONE address for every episode (doraby's "Ozark"), and
// the episode that is on is only the ACTIVE item of its episode list. This reads, in the TOP page only, what the page says about which video is on: the show's name (JSON-LD, the heading, og:title), the active
// episode of its list (class active / current / selected / aria-current, data-episode / data-season, "Episode 2", "S1 E2", "2. Title"), the season (a select, an active season tab) and that episode's own title.
// It sends a few numbers and two short strings (`nm`) when they change: once the page has settled, and again 0.6 s and 2 s after each real click (the person pressed an episode). The app joins them with the addresses of
// the player's frames (core EpisodeNames.fromClues). Nothing is sent for a page that says nothing; no address, no page text beyond those strings; strings are cut to 80 characters. Conservative ES5, passive listeners.
(function () {
  try {
    if (window !== window.top || window.__kstNM) return;
    if (typeof browser === 'undefined' || !browser || !browser.runtime) return;
    window.__kstNM = 1;
    var C = (window.__kstNMcfg && typeof window.__kstNMcfg === 'object') ? window.__kstNMcfg : {};
    var FIRST = C.first || 2500, SECOND = C.second || 6000, AFTER1 = C.after1 || 600, AFTER2 = C.after2 || 2000, MAXNODES = 500;
    var last = '', pending = [];

    var ACTIVE = /(^|[\s_-])(active|current|selected|playing|now-?playing|watching|on|checked|is-?active)($|[\s_-])/i;
    function cls(el) { var c = el.className; if (c && c.baseVal !== undefined) c = c.baseVal; return String(c || ''); }
    function txt(el) { return String(el.textContent || '').replace(/\s+/g, ' ').trim().slice(0, 160); }
    function attr(el, n) { try { var v = el.getAttribute(n); return v === null ? '' : String(v); } catch (e) { return ''; } }
    function isActive(el) {
      try {
        var a = attr(el, 'aria-current'); if (a && a !== 'false') return true;
        if (attr(el, 'aria-selected') === 'true' || attr(el, 'data-active') === 'true' || attr(el, 'data-selected') === 'true') return true;
        if (el.tagName === 'OPTION') return !!el.selected;
        return ACTIVE.test(cls(el));
      } catch (e) { return false; }
    }
    function num(v, hi) { v = parseInt(v, 10); return (v >= 1 && v <= hi) ? v : 0; }
    function clean(s) { return String(s || '').replace(/[\u0000-\u001f<>]/g, ' ').replace(/\s+/g, ' ').replace(/^[\s\-:.|]+|[\s\-:.|]+$/g, '').slice(0, 80); }

    var SE = /(?:^|[^a-z])s(?:eason)?\s*\.?\s*(\d{1,3})\s*[:.\-_ ,]*\s*e(?:p(?:isode)?)?\s*\.?\s*(\d{1,4})(?![0-9])/i;
    var EP = /(?:^|[^a-z])(?:episode|ep|eps|e)\s*\.?\s*#?(\d{1,4})(?![0-9])/i;
    var NUMTITLE = /^\s*(\d{1,4})\s*[.:)\-–]\s+(.{2,80})$/;
    var SEASON = /(?:^|[^a-z])(?:season|series|s)\s*\.?\s*#?(\d{1,3})(?![0-9])/i;

    // one candidate element -> {s, e, t} or null
    function episodeOf(el) {
      var s = num(attr(el, 'data-season') || attr(el, 'data-season-number') || attr(el, 'data-seasonnumber'), 199);
      var e = num(attr(el, 'data-episode') || attr(el, 'data-ep') || attr(el, 'data-episode-number') || attr(el, 'data-epnum') || attr(el, 'data-number'), 9999);
      var text = txt(el), m, t = '';
      if (!e) {
        if ((m = SE.exec(text))) { s = s || num(m[1], 199); e = num(m[2], 9999); }
        else if ((m = EP.exec(text))) { e = num(m[1], 9999); }
        else if ((m = NUMTITLE.exec(text))) { e = num(m[1], 9999); t = m[2]; }
      }
      if (!e) return null;
      if (!t) { var mt = /(?:episode|ep)\s*\.?\s*\d{1,4}\s*[:.\-–|]\s*(.{2,80})$/i.exec(text); if (mt) t = mt[1]; else { var tt = attr(el, 'title') || attr(el, 'data-title'); if (tt && !EP.test(tt)) t = tt; } }
      return { s: s, e: e, t: clean(t) };
    }

    function listItems() {
      var out = [];
      try {
        var nodes = document.querySelectorAll('[data-episode],[data-ep],[data-episode-number],[class~="ep"],[class*="episode" i],[class*="-ep" i],[class*="ep-" i],[class*="eps" i],[id*="episode" i],[aria-current],option:checked');
        for (var i = 0; i < nodes.length && i < MAXNODES; i++) out.push(nodes[i]);
      } catch (e) { /* none */ }
      return out;
    }

    function activeEpisode() {
      var items = listItems(), best = null, i;
      for (i = 0; i < items.length; i++) {
        var el = items[i];
        if (!isActive(el)) { var anc = el.closest ? el.closest('li, a, button, [role="tab"], [role="option"]') : null; if (!anc || !isActive(anc)) continue; }
        var r = episodeOf(el);
        if (r && (!best || (r.t && !best.t))) best = r;
        if (best && best.t) break;
      }
      return best;
    }

    function activeSeason() {
      try {
        var sels = document.querySelectorAll('select');
        for (var i = 0; i < sels.length && i < 20; i++) {
          var sl = sels[i], nm = (attr(sl, 'name') + ' ' + attr(sl, 'id') + ' ' + cls(sl)).toLowerCase();
          if (nm.indexOf('season') < 0 && !/season/i.test(txt(sl.options && sl.selectedIndex >= 0 ? sl.options[sl.selectedIndex] : sl))) continue;
          var o = sl.options && sl.options[sl.selectedIndex], m = o ? (SEASON.exec(txt(o)) || /^\s*(\d{1,3})\s*$/.exec(txt(o))) : null;
          if (m) return num(m[1], 199);
        }
        var nodes = document.querySelectorAll('[class*="season" i],[data-season],[id*="season" i]');
        for (var j = 0; j < nodes.length && j < 200; j++) {
          var el = nodes[j];
          if (!isActive(el)) continue;
          var ds = num(attr(el, 'data-season') || attr(el, 'data-season-number'), 199); if (ds) return ds;
          var mm = SEASON.exec(txt(el)) || /^\s*(\d{1,3})\s*$/.exec(txt(el)); if (mm) return num(mm[1], 199);
        }
      } catch (e) { /* none */ }
      return 0;
    }

    function jsonld() {
      var out = { sh: '', s: 0, e: 0, et: '' };
      try {
        var sc = document.querySelectorAll('script[type="application/ld+json"]');
        for (var i = 0; i < sc.length && i < 6; i++) {
          var t = sc[i].textContent || ''; if (t.length > 40000) continue;
          var j; try { j = JSON.parse(t); } catch (e) { continue; }
          var arr = Array.isArray(j) ? j : (j && j['@graph'] ? j['@graph'] : [j]);
          for (var k = 0; k < arr.length && k < 20; k++) {
            var o = arr[k]; if (!o || typeof o !== 'object') continue;
            var ty = String(o['@type'] || '');
            if (/TVEpisode/i.test(ty)) { out.et = clean(o.name); out.e = num(o.episodeNumber, 9999); if (o.partOfSeason) out.s = num(o.partOfSeason.seasonNumber, 199) || out.s; if (o.partOfSeries) out.sh = clean(o.partOfSeries.name) || out.sh; }
            else if (/TVSeries|TVShow/i.test(ty)) out.sh = out.sh || clean(o.name);
            else if (/TVSeason/i.test(ty)) { out.s = out.s || num(o.seasonNumber, 199); if (o.partOfSeries) out.sh = out.sh || clean(o.partOfSeries.name); }
            else if (/Movie/i.test(ty)) out.sh = out.sh || clean(o.name);
          }
        }
      } catch (e) { /* none */ }
      return out;
    }

    function heading() {
      try {
        var h = document.querySelector('h1');
        var t = h ? clean(txt(h)) : '';
        if (t && t.length >= 2 && !EP.test(t) && !SE.test(t)) return t;
        var og = document.querySelector('meta[property="og:title"]');
        var v = og ? clean(attr(og, 'content')) : '';
        if (v && !EP.test(v) && !SE.test(v)) return v;
      } catch (e) { /* none */ }
      return '';
    }

    function collect() {
      var j = jsonld(), a = activeEpisode();
      var out = { sh: j.sh || heading(), s: 0, e: 0, et: '' };
      if (a) { out.e = a.e; out.s = a.s; out.et = a.t; }
      else if (j.e) { out.e = j.e; out.s = j.s; out.et = j.et; }
      if (out.e && !out.s) out.s = activeSeason() || j.s;
      if (!out.e) { out.s = 0; out.et = ''; }
      return out;
    }

    function report() {
      try {
        if (document.hidden) return;
        var c = collect(), sig = c.sh + '|' + c.s + '|' + c.e + '|' + c.et;
        if (sig === last) return;
        last = sig;
        if (!c.sh && !c.e) return;
        browser.runtime.sendMessage({ k: 'nm', sh: c.sh, s: c.s, e: c.e, et: c.et }).catch(function () {});
      } catch (e) { /* never break the page */ }
    }
    function soon(ms) { try { pending.push(setTimeout(function () { report(); }, ms)); if (pending.length > 20) pending.shift(); } catch (e) { /* ignore */ } }
    window.addEventListener('click', function (e) { if (e && e.isTrusted) { soon(AFTER1); soon(AFTER2); } }, { capture: true, passive: true });
    soon(FIRST); soon(SECOND);
    window.KestrelNM = { collect: collect, report: report };
  } catch (e) { /* nothing here may break a page */ }
})();
