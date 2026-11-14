'use strict';
// Kestrel's request-blocking engine for the built-in extension: a line-by-line port of core/.../filter/FilterSet.kt (check), Psl.kt (siteOf) and UrlParts.hostOf,
// reading the little-endian KEX1 image the app writes (core/.../filter/ExtImage.kt). Same lists, same verdicts as the Kotlin engine - web-tests/engine.test.js proves it
// against fixtures the Kotlin side regenerates (core/.../ExtImageTest.kt). Everything is synchronous and allocation-light: the webRequest listener calls check() for every request.
// Design and message protocol: research/D1-DESIGN.md.
var KestrelEngine = (function () {
  var MAGIC = 0x3158454B, VERSION = 1, SECTIONS = 21;
  var S = { HOST_HASH: 0, HOST_FLAGS: 1, EXC_HASH: 2, EXC_FLAGS: 3, PAT_BYTES: 4, PAT_OFF: 5, PAT_META: 6, PAT_DOM: 7, DOM_POOL: 8, TAB_KEY: 9, TAB_START: 10, TAB_LEN: 11, RULE_IDS: 12,
    GRAM_KEY: 13, GRAM_START: 14, GRAM_LEN: 15, GRAM_IDS: 16, NO_TOKEN: 17, PSL_RULES: 18, PSL_WILD: 19, PSL_EXC: 20 };

  // Bits (FilterSet.kt `Bits`) and request types (RequestType.kt)
  var TYPES = 0x1FFF, PARTY_SHIFT = 13, B_IMPORTANT = 1 << 15, B_EXCEPTION = 1 << 16, CAT_SHIFT = 17, LEFT_HOST = 1 << 19, LEFT_ANCHOR = 1 << 20, RIGHT_ANCHOR = 1 << 21,
      REDIRECT_SHIFT = 22, B_REDIRECT_ONLY = 1 << 27; // AW1c: 5 bits name the stand-in resource of a $redirect= rule, bit 27 marks a $redirect-rule= (picks the stand-in for something else's block, blocks nothing itself)
  var T = { DOCUMENT: 1, SUBDOCUMENT: 2, STYLESHEET: 4, SCRIPT: 8, IMAGE: 16, FONT: 32, MEDIA: 64, XHR: 128, WEBSOCKET: 256, PING: 512, OBJECT: 1024, OTHER: 2048 };
  var MIN_TOKEN = 3, MAX_TOKEN_LEN = 64, MAX_GRAM_EVALS = 3000, MAX_URL = 2048, MAX_HOSTPORT = 300, MAX_USERINFO = 256;
  var STAR = 42, CARET = 94;
  var CHARS = []; for (var ci = 0; ci < 256; ci++) CHARS.push(String.fromCharCode(ci));

  /** webRequest resource type -> filter-engine request type (ShieldsBridge.requestType). */
  function typeMask(t) {
    switch (t) {
      case 'script': return T.SCRIPT;
      case 'stylesheet': return T.STYLESHEET;
      case 'image': case 'imageset': return T.IMAGE;
      case 'media': return T.MEDIA;
      case 'font': return T.FONT;
      case 'xmlhttprequest': case 'fetch': return T.XHR;
      case 'websocket': return T.WEBSOCKET;
      case 'sub_frame': return T.SUBDOCUMENT;
      case 'object': case 'object_subrequest': return T.OBJECT;
      case 'ping': case 'beacon': return T.PING;
      default: return T.OTHER;
    }
  }

  // ---------------------------------------------------------------- 64-bit hashing on 32-bit limbs (Hash.kt: FNV-1a + murmur3 finaliser)
  var mHi = 0, mLo = 0;
  /** (ah:al) * (bh:bl) mod 2^64 into mHi / mLo (both unsigned). */
  function mul64(ah, al, bh, bl) {
    var a0 = al & 0xFFFF, a1 = al >>> 16, b0 = bl & 0xFFFF, b1 = bl >>> 16;
    var p00 = a0 * b0, p01 = a0 * b1, p10 = a1 * b0, p11 = a1 * b1;
    var mid = p01 + p10 + (p00 >>> 16);
    mLo = (((mid & 0xFFFF) << 16) | (p00 & 0xFFFF)) >>> 0;
    mHi = (p11 + Math.floor(mid / 65536) + Math.imul(ah, bl) + Math.imul(al, bh)) >>> 0;
  }
  var hHi = 0, hLo = 0; // result of h64 (signed int32 each)
  function h64(s, from, to) {
    var hi = 0xcbf29ce4 | 0, lo = 0x84222325 | 0;
    for (var i = from; i < to; i++) {
      lo ^= s.charCodeAt(i);
      // h * 0x100000001B3 = (h << 40) + h * 0x1B3
      var loU = lo >>> 0, hiU = hi >>> 0;
      var p = loU * 435;
      var nhi = (hiU * 435 + Math.floor(p / 4294967296) + ((loU & 0xFFFFFF) << 8)) >>> 0;
      lo = p >>> 0; hi = nhi;
    }
    lo ^= (hi >>> 1); lo >>>= 0; // h ^= h >>> 33
    mul64(hi, lo, 0xff51afd7, 0xed558ccd); hi = mHi; lo = mLo;
    lo = (lo ^ (hi >>> 1)) >>> 0;
    mul64(hi, lo, 0xc4ceb9fe, 0x1a85ec53); hi = mHi; lo = mLo;
    lo = (lo ^ (hi >>> 1)) >>> 0;
    hHi = hi | 0; hLo = lo | 0;
  }
  function slot(key, mask) { var h = Math.imul(key, -1640531527); h ^= h >>> 16; return h & mask; }
  function gramOf(s, i) { return ((s.charCodeAt(i) & 0xFF) << 24) | ((s.charCodeAt(i + 1) & 0xFF) << 16) | ((s.charCodeAt(i + 2) & 0xFF) << 8) | (s.charCodeAt(i + 3) & 0xFF); }
  /** First index of (hi, lo) in a sorted table of 64-bit values stored as [lo, hi] int pairs (signed order: hi signed, then lo unsigned), or -1. */
  function firstIndexOf(a, n, hi, lo) {
    var l = 0, h = n - 1, found = -1, ulo = lo >>> 0;
    while (l <= h) {
      var mid = (l + h) >>> 1;
      var vhi = a[2 * mid + 1], vlo = a[2 * mid] >>> 0;
      if (vhi < hi || (vhi === hi && vlo < ulo)) l = mid + 1;
      else { if (vhi === hi && vlo === ulo) found = mid; h = mid - 1; }
    }
    return found;
  }

  function isAlnum(c) { return (c >= 97 && c <= 122) || (c >= 48 && c <= 57); }
  function isSeparator(c) { return !(isAlnum(c) || c === 95 || c === 45 || c === 46 || c === 37); } // anything except letters, digits and _ - . %
  function allDigits(s, from, to) { if (from >= to) return true; for (var i = from; i < to; i++) { var c = s.charCodeAt(i); if (c < 48 || c > 57) return false; } return true; }
  function isIpv4(h) {
    var parts = 0, value = -1;
    for (var k = 0; k <= h.length; k++) {
      var c = k < h.length ? h.charCodeAt(k) : 46;
      if (c === 46) { if (value < 0 || value > 255) return false; parts++; value = -1; }
      else if (c >= 48 && c <= 57) { value = (value < 0 ? 0 : value) * 10 + (c - 48); if (value > 255) return false; }
      else return false;
    }
    return parts === 4;
  }

  /** UrlParts.hostOf: lower-case host of an absolute URL without port / userinfo / trailing dots, or null. */
  function hostOf(url) {
    var n = url.length, i = 0, c;
    while (i < n) {
      c = url.charCodeAt(i);
      var ok = (c >= 97 && c <= 122) || (c >= 65 && c <= 90) || (i > 0 && ((c >= 48 && c <= 57) || c === 43 || c === 45 || c === 46));
      if (!ok) break;
      i++;
    }
    if (i === 0 || url.substr(i, 3) !== '://') return null;
    var authStart = i + 3, j = authStart;
    while (j < n) { c = url.charCodeAt(j); if (c === 47 || c === 63 || c === 35) break; j++; }
    var authority = url.substring(authStart, j);
    var at = authority.lastIndexOf('@');
    var hostPort = at >= 0 ? authority.substring(at + 1) : authority;
    var host;
    if (hostPort.charCodeAt(0) === 91) {
      var close = hostPort.indexOf(']');
      if (close < 0) return null;
      host = hostPort.substring(1, close);
      var rest = hostPort.substring(close + 1);
      if (rest.length > 0) { if (rest.charCodeAt(0) !== 58) return null; if (parsePort(rest, 1) === null) return null; }
    } else {
      var colon = hostPort.lastIndexOf(':');
      if (colon >= 0) { if (parsePort(hostPort, colon + 1) === null) return null; host = hostPort.substring(0, colon); }
      else host = hostPort;
      if (host.indexOf(':') >= 0) return null;
    }
    host = host.toLowerCase();
    var end = host.length;
    while (end > 0 && host.charCodeAt(end - 1) === 46) end--;
    if (end !== host.length) host = host.substring(0, end);
    return host.length === 0 ? null : host;
  }
  function parsePort(s, from) {
    if (from >= s.length) return -1;
    var v = 0;
    for (var k = from; k < s.length; k++) { var d = s.charCodeAt(k) - 48; if (d < 0 || d > 9) return null; v = v * 10 + d; if (v > 65535) return null; }
    return v;
  }

  // ---------------------------------------------------------------- the engine over one image
  /** @param buf ArrayBuffer holding a complete KEX1 image. Throws (and the caller keeps the old engine) when it is damaged. */
  function create(buf) {
    // (toString, not instanceof: a buffer from another realm - a worker, a structured clone - must load too)
    if (Object.prototype.toString.call(buf) !== '[object ArrayBuffer]' || buf.byteLength < 16 + SECTIONS * 8 + 4 || (buf.byteLength & 3) !== 0) throw new Error('image size');
    var all = new Int32Array(buf);
    if (all[0] !== MAGIC) throw new Error('image magic');
    if (all[1] !== VERSION) throw new Error('image version');
    if (all[3] !== SECTIONS) throw new Error('image sections');
    var sum = 0, lastWord = all.length - 1;
    for (var wi = 0; wi < lastWord; wi++) sum = (sum + all[wi]) | 0;
    if (sum !== all[lastWord]) throw new Error('image checksum');
    var end = 16 + SECTIONS * 8, bytes = buf.byteLength - 4;
    function span(id) {
      var off = all[4 + id * 2], cnt = all[5 + id * 2];
      if (off !== end || cnt < 0) throw new Error('image layout ' + id);
      var width = (id === S.PAT_BYTES) ? ((cnt + 3) & ~3) : ((id === S.HOST_HASH || id === S.EXC_HASH || id === S.DOM_POOL || id >= S.PSL_RULES) ? cnt * 8 : cnt * 4);
      end = off + width;
      if (end > bytes) throw new Error('image overrun ' + id);
      return off;
    }
    function ints(id) { var o = span(id); return new Int32Array(buf, o, all[5 + id * 2]); }
    function pairs(id) { var o = span(id); return new Int32Array(buf, o, all[5 + id * 2] * 2); }
    var hostHash = pairs(S.HOST_HASH), hostFlags = ints(S.HOST_FLAGS), excHash = pairs(S.EXC_HASH), excFlags = ints(S.EXC_FLAGS);
    var patOffBytes = span(S.PAT_BYTES), patCount = all[5 + S.PAT_BYTES * 2];
    var patBytes = new Uint8Array(buf, patOffBytes, patCount);
    var patOff = ints(S.PAT_OFF), meta = ints(S.PAT_META), dom = ints(S.PAT_DOM), domPool = pairs(S.DOM_POOL);
    var tabKey = ints(S.TAB_KEY), tabStart = ints(S.TAB_START), tabLen = ints(S.TAB_LEN), ruleIds = ints(S.RULE_IDS);
    var gramKey = ints(S.GRAM_KEY), gramStart = ints(S.GRAM_START), gramLen = ints(S.GRAM_LEN), gramIds = ints(S.GRAM_IDS), noToken = ints(S.NO_TOKEN);
    var pslRules = pairs(S.PSL_RULES), pslWild = pairs(S.PSL_WILD), pslExc = pairs(S.PSL_EXC);
    if (end !== bytes) throw new Error('image trailer');
    var nHost = hostHash.length >> 1, nExc = excHash.length >> 1, nRules = meta.length;
    var nPslRules = pslRules.length >> 1, nPslWild = pslWild.length >> 1, nPslExc = pslExc.length >> 1;
    if (patOff.length !== nRules + 1 || dom.length !== nRules || hostFlags.length !== nHost || excFlags.length !== nExc) throw new Error('image tables');
    if (tabKey.length < 16 || (tabKey.length & (tabKey.length - 1)) !== 0 || gramKey.length < 16 || (gramKey.length & (gramKey.length - 1)) !== 0) throw new Error('image index');

    // ---- Public Suffix List (Psl.kt)
    var siteCache = new Map();
    function siteOf(host) {
      var hit = siteCache.get(host);
      if (hit !== undefined) return hit;
      var r = computeSite(host);
      if (siteCache.size >= 256) siteCache.clear();
      siteCache.set(host, r);
      return r;
    }
    function computeSite(host) {
      if (host.length === 0) return null;
      if (host.indexOf(':') >= 0 || isIpv4(host)) return host;
      var n = host.length, starts = [0];
      for (var i = 0; i < n; i++) if (host.charCodeAt(i) === 46) starts.push(i + 1);
      var labels = starts.length;
      if (labels === 1) return host;
      var suffixLabels = 1; // default rule "*": the last label is the public suffix
      for (var k = 0; k < labels; k++) {
        h64(host, starts[k], n);
        if (firstIndexOf(pslExc, nPslExc, hHi, hLo) >= 0) { suffixLabels = labels - k - 1; break; }
        if (firstIndexOf(pslRules, nPslRules, hHi, hLo) >= 0) { suffixLabels = labels - k; break; }
        if (k + 1 < labels) {
          h64(host, starts[k + 1], n);
          if (firstIndexOf(pslWild, nPslWild, hHi, hLo) >= 0) { suffixLabels = labels - k; break; }
        }
      }
      if (suffixLabels >= labels) return null; // the host IS a public suffix
      return host.substring(starts[labels - suffixLabels - 1]);
    }
    function isSameSite(a, b) {
      if (a === b) return true;
      var sa = siteOf(a); if (sa === null) return false;
      var sb = siteOf(b); if (sb === null) return false;
      return sa === sb;
    }

    // ---- per-check context (one check at a time: JS is single-threaded and check() never re-enters)
    var cUrl = '', cHost = '', cHostStart = 0, cHostEnd = 0, cPageHost = null, cTypeMask = 0, cThird = false;
    var cBlocked = false, cImportant = false, cExcepted = false, cCategory = 0, cEvals = 0, cPageHashes = null, cRedirect = 0, cRedirectOnly = 0;
    var seen = new Int32Array(64), nSeen = 0;

    var lastHashHost = null, lastHashes = null; // every request of a page names the same page host: its suffix hashes are computed once, not once per request
    function pageHashes() {
      var a = cPageHashes;
      if (a === null) {
        var h = cPageHost;
        if (h && h === lastHashHost) a = lastHashes;
        else {
          a = [];
          if (h) {
            var start = 0;
            for (;;) {
              h64(h, start, h.length); a.push(hHi, hLo);
              var dot = h.indexOf('.', start);
              if (dot < 0) break;
              start = dot + 1;
            }
          }
          if (h) { lastHashHost = h; lastHashes = a; }
        }
        cPageHashes = a;
      }
      return a;
    }
    function pageHas(a, hi, lo) { for (var k = 0; k < a.length; k += 2) if (a[k] === hi && a[k + 1] === lo) return true; return false; }

    function makeCtx(rawUrl, pageHost, mask) {
      var low = rawUrl.length >= 8 ? rawUrl.substr(0, 8).toLowerCase() : rawUrl.toLowerCase();
      var schemeLen;
      if (low.startsWith('https://')) schemeLen = 8;
      else if (low.startsWith('http://')) schemeLen = 7;
      else if (low.startsWith('wss://')) schemeLen = 6;
      else if (low.startsWith('ws://')) schemeLen = 5;
      else return false; // data:, blob:, about:, file: ... never reach the network
      var s = rawUrl, c;
      if (s.length > MAX_URL) {
        // the cap bounds the work but must not let the sender choose WHAT is inspected: locate the authority on the full URL and drop an absurd userinfo first
        var authEnd0 = schemeLen;
        while (authEnd0 < s.length) { c = s.charCodeAt(authEnd0); if (c === 47 || c === 63 || c === 35) break; authEnd0++; }
        var at0 = s.lastIndexOf('@', authEnd0 - 1);
        var hostPart = at0 >= schemeLen ? authEnd0 - (at0 + 1) : authEnd0 - schemeLen;
        if (hostPart > MAX_HOSTPORT) return false;
        if (at0 >= schemeLen && at0 - schemeLen > MAX_USERINFO) s = s.substring(0, schemeLen) + s.substring(at0 + 1);
        if (s.length > MAX_URL) s = s.substring(0, MAX_URL);
      }
      s = s.toLowerCase();
      var hs = schemeLen, authEnd = hs;
      while (authEnd < s.length) { c = s.charCodeAt(authEnd); if (c === 47 || c === 63 || c === 35) break; authEnd++; }
      var at = s.lastIndexOf('@', authEnd - 1);
      if (at >= hs) hs = at + 1;
      var he = authEnd;
      if (hs < authEnd && s.charCodeAt(hs) === 91) {
        var rb = s.indexOf(']', hs);
        if (rb >= hs && rb < authEnd) he = rb + 1;
      } else {
        var colon = s.lastIndexOf(':', authEnd - 1);
        if (colon > hs && colon < authEnd && allDigits(s, colon + 1, authEnd)) he = colon;
      }
      if (he <= hs) return false;
      var trimmed = he; // FQDN trailing dots ("ads.example.com.") must not dodge anchored patterns
      while (trimmed > hs && s.charCodeAt(trimmed - 1) === 46) trimmed--;
      if (trimmed !== he) { s = s.substring(0, trimmed) + s.substring(he); he = trimmed; }
      cUrl = s; cHost = s.substring(hs, he); cHostStart = hs; cHostEnd = he; cPageHost = pageHost; cTypeMask = mask;
      cThird = mask !== T.DOCUMENT && pageHost !== null && pageHost !== undefined && !isSameSite(cHost, pageHost);
      cBlocked = false; cImportant = false; cExcepted = false; cCategory = 0; cEvals = 0; cPageHashes = null; cRedirect = 0; cRedirectOnly = 0;
      return true;
    }

    function flagsApply(f) {
      if ((f & TYPES & cTypeMask) === 0) return false;
      var party = (f >>> PARTY_SHIFT) & 3;
      if (party === 1 && !cThird) return false;
      if (party === 2 && cThird) return false;
      return true;
    }

    function scanHosts(hashes, flags, exception) {
      var n = hashes.length >> 1;
      if (n === 0) return;
      var h = cHost, start = 0;
      for (;;) {
        h64(h, start, h.length);
        var idx = firstIndexOf(hashes, n, hHi, hLo);
        if (idx >= 0) {
          while (idx < n && hashes[2 * idx + 1] === hHi && hashes[2 * idx] === hLo) {
            var f = flags[idx];
            if (flagsApply(f)) {
              if (exception) { cExcepted = true; return; }
              var rdh = (f >>> REDIRECT_SHIFT) & 31;
              if (rdh !== 0 && cRedirect === 0) cRedirect = rdh;
              cBlocked = true;
              cCategory = (f >>> CAT_SHIFT) & 3;
              if ((f & B_IMPORTANT) !== 0) { cImportant = true; return; }
            }
            idx++;
          }
        }
        var dot = h.indexOf('.', start);
        if (dot < 0) break;
        start = dot + 1; // walk down to the bare TLD too: lists do contain TLD rules such as ||zip^
      }
    }

    function scanBucket(keys, starts, lens, ids, key) {
      var mask = keys.length - 1, sl = slot(key, mask);
      for (;;) {
        var len = lens[sl];
        if (len === 0) return;
        if (keys[sl] === key) {
          var st = starts[sl], stop = st + len;
          for (var k = st; k < stop; k++) { evalRule(ids[k]); if (cImportant) return; }
          return;
        }
        sl = (sl + 1) & mask;
      }
    }

    function scanPatterns() {
      if (nRules === 0) return;
      var url = cUrl, n = url.length, i = 0;
      nSeen = 0;
      while (i < n) {
        if (isAlnum(url.charCodeAt(i))) {
          var j = i + 1;
          while (j < n && isAlnum(url.charCodeAt(j))) j++;
          var len = j - i;
          if (len >= MIN_TOKEN && len <= MAX_TOKEN_LEN) {
            h64(url, i, j);
            var key = hHi ^ hLo, dup = false;
            for (var q = 0; q < nSeen; q++) if (seen[q] === key) { dup = true; break; }
            if (!dup) { // each DISTINCT token's bucket is scanned once per URL (a crafted URL repeating one hot token must not rescan it)
              if (nSeen === seen.length) { var bigger = new Int32Array(nSeen * 2); bigger.set(seen); seen = bigger; }
              seen[nSeen++] = key;
              scanBucket(tabKey, tabStart, tabLen, ruleIds, key);
              if (cImportant) return;
            }
          }
          i = j;
        } else i++;
      }
      if (gramIds.length > 0 && n >= 4) {
        var start = cEvals; // rules without a usable token: bounded by work, not by a set (grams repeat freely in ordinary URLs)
        for (var g = 0; g <= n - 4; g++) {
          scanBucket(gramKey, gramStart, gramLen, gramIds, gramOf(url, g));
          if (cImportant) return;
          if (cEvals - start > MAX_GRAM_EVALS) break;
        }
      }
      for (var z = 0; z < noToken.length; z++) { evalRule(noToken[z]); if (cImportant) return; }
    }

    function domainOk(off) {
      var nInc = domPool[2 * off], nExcl = domPool[2 * (off + 1)];
      var ph = pageHashes(), k, p;
      if (nInc > 0) {
        var ok = false;
        for (k = 0; k < nInc; k++) { p = 2 * (off + 2 + k); if (pageHas(ph, domPool[p + 1], domPool[p])) { ok = true; break; } }
        if (!ok) return false;
      }
      for (k = 0; k < nExcl; k++) { p = 2 * (off + 2 + nInc + k); if (pageHas(ph, domPool[p + 1], domPool[p])) return false; }
      return true;
    }

    function glob(ps, pe, start, rightAnchor) {
      var url = cUrl, n = url.length, p = ps, u = start, starP = -1, starU = -1;
      for (;;) {
        if (p === pe) {
          if (!rightAnchor || u === n) return true;
          if (starP < 0) return false;
          starU++;
          if (starU > n) return false;
          u = starU; p = starP + 1;
          continue;
        }
        var pc = patBytes[p];
        if (pc === STAR) { starP = p; starU = u; p++; continue; }
        if (pc === CARET) {
          if (u === n) { p++; continue; }
          if (isSeparator(url.charCodeAt(u))) { p++; u++; continue; }
        } else if (u < n && url.charCodeAt(u) === pc) { p++; u++; continue; }
        if (starP < 0) return false;
        starU++;
        if (starU > n) return false;
        u = starU; p = starP + 1;
      }
    }

    function matchPattern(id, w) {
      var s = patOff[id], e = patOff[id + 1];
      if (s === e) return true; // options-only rule
      var url = cUrl, right = (w & RIGHT_ANCHOR) !== 0, p;
      if ((w & LEFT_HOST) !== 0) {
        p = cHostStart;
        var he = cHostEnd;
        for (;;) {
          if (glob(s, e, p, right)) return true;
          var dot = url.indexOf('.', p);
          if (dot < 0 || dot + 1 >= he) return false;
          p = dot + 1;
        }
      }
      if ((w & LEFT_ANCHOR) !== 0) return glob(s, e, 0, right);
      var first = patBytes[s];
      if (first !== STAR && first !== CARET) {
        var ch = CHARS[first];
        p = url.indexOf(ch);
        while (p >= 0) { if (glob(s, e, p, right)) return true; p = url.indexOf(ch, p + 1); }
        return false;
      }
      for (p = 0; p <= url.length; p++) if (glob(s, e, p, right)) return true;
      return false;
    }

    function evalRule(id) {
      cEvals++;
      var w = meta[id];
      if ((w & TYPES & cTypeMask) === 0) return;
      var party = (w >>> PARTY_SHIFT) & 3;
      if (party === 1 && !cThird) return;
      if (party === 2 && cThird) return;
      var isExc = (w & B_EXCEPTION) !== 0;
      var rd = (w >>> REDIRECT_SHIFT) & 31, redirOnly = (w & B_REDIRECT_ONLY) !== 0;
      if (isExc) { if (cExcepted) return; }
      else if (redirOnly) { if (cRedirectOnly !== 0) return; }
      else if (cBlocked && (w & B_IMPORTANT) === 0 && (rd === 0 || cRedirect !== 0)) return; // already blocked: only a rule that can still name the stand-in is worth looking at
      var d = dom[id];
      if (d !== 0 && !domainOk(d - 1)) return;
      if (!matchPattern(id, w)) return;
      if (isExc) cExcepted = true;
      else if (redirOnly) cRedirectOnly = rd;
      else {
        if (rd !== 0 && cRedirect === 0) cRedirect = rd;
        if (!cBlocked || (w & B_IMPORTANT) !== 0) cCategory = (w >>> CAT_SHIFT) & 3;
        cBlocked = true;
        if ((w & B_IMPORTANT) !== 0) cImportant = true;
      }
    }

    /**
     * Verdict word exactly as FilterSet.check: 0 none; 1 block (| category << 2); 2 excepted; 16 = $important; bits 8-12 = the stand-in resource to answer a block with (AW1c, 0 = just cancel).
     * @param url full request URL; @param pageHost lower-case host of the top-level page or null; @param mask request-type bits (typeMask())
     */
    function check(url, pageHost, mask) {
      if (!makeCtx(url, pageHost, mask)) return 0;
      scanHosts(hostHash, hostFlags, false);
      if (!cImportant) scanPatterns();
      if (!cBlocked) return 0;
      var stand = (cRedirect !== 0 ? cRedirect : cRedirectOnly) << 8;
      if (cImportant) return 1 | 16 | (cCategory << 2) | stand;
      if (!cExcepted) scanHosts(excHash, excFlags, true);
      return cExcepted ? 2 : (1 | (cCategory << 2) | stand);
    }

    /** Runs a few dozen representative checks so the script engine has compiled every path before the first real request (a cold first check is slow on a small CPU). */
    function warm() {
      var urls = ['https://ads.example.com/banner/1.gif', 'https://cdn.example.org/a/b/c/d.js?x=1&y=2', 'https://www.google-analytics.com/collect?v=1&tid=UA-1', 'https://static.site.co.uk/img/logo.png',
        'wss://sock.example/s', 'http://127.0.0.1:8080/x', 'https://a.b.c.d.example.com/' + 'seg/'.repeat(30) + 'file.js', 'https://user@host.example:8443/p/q.html#f'];
      var types = [T.SCRIPT, T.IMAGE, T.XHR, T.STYLESHEET, T.SUBDOCUMENT, T.OTHER];
      for (var r = 0; r < 8; r++) for (var u = 0; u < urls.length; u++) check(urls[u], (r & 1) ? 'news.site.co.uk' : 'example.com', types[(r + u) % types.length]);
    }
    warm();

    return {
      check: check, siteOf: siteOf, isSameSite: isSameSite,
      generation: all[2], bytes: buf.byteLength, hostRules: nHost, hostExceptions: nExc, patternRules: nRules, tokenRules: ruleIds.length, gramRules: gramIds.length, tokenlessRules: noToken.length,
    };
  }

  return { create: create, hostOf: hostOf, typeMask: typeMask, h64: function (s) { h64(s, 0, s.length); return [hHi, hLo]; }, MAGIC: MAGIC, T: T };
})();
if (typeof module !== 'undefined' && module.exports) module.exports = KestrelEngine;
