// Research scratch code (not part of the repo): a JS port of Kestrel's FilterSet.check() that reads the EXISTING big-endian .kfs snapshot into typed arrays.
// Purpose: measure load time, memory and per-request cost of "engine in JS inside the extension" and diff its verdicts against the Kotlin engine.
const BLOCK = 1, EXCEPTED = 2, IMPORTANT = 16;
const TYPES = 0x1FFF, PARTY_SHIFT = 13, IMP_BIT = 1 << 15, EXC_BIT = 1 << 16, CAT_SHIFT = 17, LEFT_HOST = 1 << 19, LEFT_ANCHOR = 1 << 20, RIGHT_ANCHOR = 1 << 21;
const STAR = 42, CARET = 94, MIN_TOKEN = 3, MAX_TOKEN_LEN = 64, MAX_GRAM_EVALS = 3000, DOCUMENT = 1;
const M1H = 0xff51afd7, M1L = 0xed558ccd, M2H = 0xc4ceb9fe, M2L = 0x1a85ec53;

let HH = 0, HL = 0; // result registers of h64 (unsigned 32-bit halves)
function mulhi32(a, b) {
  const a0 = a & 0xffff, a1 = a >>> 16, b0 = b & 0xffff, b1 = b >>> 16;
  const t0 = a0 * b0;
  const t1 = a1 * b0 + (t0 >>> 16);
  const t2 = a0 * b1 + (t1 & 0xffff);
  return (a1 * b1 + (t1 >>> 16) + (t2 >>> 16)) >>> 0;
}
function h64(s, from, to) {
  let hi = 0xcbf29ce4, lo = 0x84222325;
  for (let i = from; i < to; i++) {
    lo = (lo ^ s.charCodeAt(i)) >>> 0;
    const t = lo * 0x1b3;
    const nl = t >>> 0;
    const carry = (t - nl) / 4294967296;
    hi = (carry + hi * 0x1b3 + lo * 0x100) >>> 0;
    lo = nl;
  }
  lo = (lo ^ (hi >>> 1)) >>> 0;
  let nlo = Math.imul(lo, M1L) >>> 0;
  hi = (mulhi32(lo, M1L) + Math.imul(hi, M1L) + Math.imul(lo, M1H)) >>> 0; lo = nlo;
  lo = (lo ^ (hi >>> 1)) >>> 0;
  nlo = Math.imul(lo, M2L) >>> 0;
  hi = (mulhi32(lo, M2L) + Math.imul(hi, M2L) + Math.imul(lo, M2H)) >>> 0; lo = nlo;
  lo = (lo ^ (hi >>> 1)) >>> 0;
  HH = hi; HL = lo;
}
function token32(s, from, to) { h64(s, from, to); return (HL ^ HH) | 0; }
function slotOf(key, mask) { let h = Math.imul(key, -1640531527); h ^= h >>> 16; return h & mask; }
function gram(s, i) { return ((s.charCodeAt(i) & 0xFF) << 24) | ((s.charCodeAt(i + 1) & 0xFF) << 16) | ((s.charCodeAt(i + 2) & 0xFF) << 8) | (s.charCodeAt(i + 3) & 0xFF); }
function isAlnum(c) { return (c >= 97 && c <= 122) || (c >= 48 && c <= 57); }
function isSep(c) { return !(isAlnum(c) || c === 95 || c === 45 || c === 46 || c === 37); }

// sorted-array lower bound over (hi signed, lo unsigned) pairs
function firstIndexOf(hiA, loA, qh, ql) {
  let lo = 0, hi = hiA.length - 1, found = -1;
  qh |= 0;
  while (lo <= hi) {
    const mid = (lo + hi) >>> 1;
    const h = hiA[mid];
    let less;
    if (h !== qh) less = h < qh; else less = loA[mid] < ql;
    if (less) lo = mid + 1; else { if (h === qh && loA[mid] === ql) found = mid; hi = mid - 1; }
  }
  return found;
}

export function site(host) {
  const parts = host.split('.');
  if (parts.length < 2) return host;
  return parts.slice(-2).join('.');
}
let isSameSite = (a, b) => a === b || site(a) === site(b);
export function setSameSite(fn) { isSameSite = fn; }

export class Kfs {
  static load(buf) {
    const t0 = performance.now();
    const dv = new DataView(buf.buffer, buf.byteOffset, buf.byteLength);
    let p = 0;
    const i32 = () => { const v = dv.getInt32(p); p += 4; return v; };
    i32(); i32(); p += 8; const nl = i32();
    for (let i = 0; i < nl; i++) { const len = dv.getUint16(p); p += 2 + len; }
    i32(); // cosmeticSite
    const longs = () => { const n = i32(); const hi = new Int32Array(n), lo = new Uint32Array(n); for (let i = 0; i < n; i++) { hi[i] = dv.getInt32(p); lo[i] = dv.getUint32(p + 4); p += 8; } return { hi, lo, n }; };
    const ints = () => { const n = i32(); const a = new Int32Array(n); for (let i = 0; i < n; i++) { a[i] = dv.getInt32(p); p += 4; } return a; };
    const bytes = () => { const n = i32(); const a = new Uint8Array(n); a.set(buf.subarray(p, p + n)); p += n; return a; };
    const e = new Kfs();
    e.hosts = longs(); e.hostFlags = ints();
    e.exc = longs(); e.excFlags = ints();
    e.site = longs(); e.siteFlags = ints();
    e.count = i32();
    e.patBytes = bytes(); e.patOff = ints(); e.meta = ints(); e.dom = ints(); e.domPool = longs();
    e.tabKey = ints(); e.tabStart = ints(); e.tabLen = ints(); e.ruleIds = ints();
    e.gramKey = ints(); e.gramStart = ints(); e.gramLen = ints(); e.gramRuleIds = ints(); e.noToken = ints();
    e.loadMs = performance.now() - t0;
    e.bytesRead = p; // cosmetic tables intentionally not loaded in this experiment
    return e;
  }

  check(rawUrl, pageHost, typeMask) {
    const c = this.makeCtx(rawUrl, pageHost, typeMask);
    if (c === null) return 0;
    this.scanHosts(this.hosts, this.hostFlags, c, false);
    if (!c.important) this.scanPatterns(c);
    if (!c.blocked) return 0;
    if (c.important) return BLOCK | IMPORTANT | (c.category << 2);
    if (!c.excepted) this.scanHosts(this.exc, this.excFlags, c, true);
    return c.excepted ? EXCEPTED : BLOCK | (c.category << 2);
  }

  makeCtx(raw, pageHost, typeMask) {
    let sl;
    const l = raw.length >= 8 ? raw.substring(0, 8).toLowerCase() : raw.toLowerCase();
    if (l === 'https://') sl = 8; else if (l.startsWith('http://')) sl = 7; else if (l.startsWith('wss://')) sl = 6; else if (l.startsWith('ws://')) sl = 5; else return null;
    let s = raw.length > 2048 ? raw.substring(0, 2048) : raw;
    s = s.toLowerCase();
    let hs = sl, authEnd = hs;
    while (authEnd < s.length) { const ch = s.charCodeAt(authEnd); if (ch === 47 || ch === 63 || ch === 35) break; authEnd++; }
    const at = s.lastIndexOf('@', authEnd - 1);
    if (at >= hs) hs = at + 1;
    let he = authEnd;
    if (hs < authEnd && s.charCodeAt(hs) === 91) { const rb = s.indexOf(']', hs); if (rb >= hs && rb < authEnd) he = rb + 1; }
    else { const colon = s.lastIndexOf(':', authEnd - 1); if (colon > hs && colon < authEnd) { let dig = true; for (let k = colon + 1; k < authEnd; k++) { const ch = s.charCodeAt(k); if (ch < 48 || ch > 57) { dig = false; break; } } if (dig) he = colon; } }
    if (he <= hs) return null;
    let trimmed = he;
    while (trimmed > hs && s.charCodeAt(trimmed - 1) === 46) trimmed--;
    if (trimmed !== he) { s = s.substring(0, trimmed) + s.substring(he); he = trimmed; }
    const host = s.substring(hs, he);
    const third = typeMask !== DOCUMENT && pageHost != null && !isSameSite(host, pageHost);
    return { url: s, host, hs, he, pageHost, typeMask, third, blocked: false, important: false, excepted: false, category: 0, evals: 0, ph: null };
  }

  pageHashes(c) {
    if (c.ph) return c.ph;
    const hosts = c.pageHost; const out = [];
    if (hosts) { let start = 0; for (;;) { h64(hosts, start, hosts.length); out.push(HH | 0, HL); const d = hosts.indexOf('.', start); if (d < 0) break; start = d + 1; } }
    return (c.ph = out);
  }

  scanHosts(t, flags, c, exception) {
    if (t.n === 0) return;
    const h = c.host; let start = 0;
    for (;;) {
      h64(h, start, h.length);
      let idx = firstIndexOf(t.hi, t.lo, HH, HL);
      if (idx >= 0) {
        const qh = HH | 0, ql = HL;
        while (idx < t.n && t.hi[idx] === qh && t.lo[idx] === ql) {
          const f = flags[idx];
          if (this.flagsApply(f, c)) {
            if (exception) { c.excepted = true; return; }
            c.blocked = true; c.category = (f >>> CAT_SHIFT) & 3;
            if ((f & IMP_BIT) !== 0) { c.important = true; return; }
          }
          idx++;
        }
      }
      const dot = h.indexOf('.', start);
      if (dot < 0) break;
      start = dot + 1;
    }
  }
  flagsApply(f, c) {
    if ((f & TYPES & c.typeMask) === 0) return false;
    const party = (f >>> PARTY_SHIFT) & 3;
    if (party === 1 && !c.third) return false;
    if (party === 2 && c.third) return false;
    return true;
  }

  scanPatterns(c) {
    if (this.count === 0) return;
    const url = c.url, n = url.length;
    const seen = [];
    let i = 0;
    while (i < n) {
      if (isAlnum(url.charCodeAt(i))) {
        let j = i + 1;
        while (j < n && isAlnum(url.charCodeAt(j))) j++;
        const len = j - i;
        if (len >= MIN_TOKEN && len <= MAX_TOKEN_LEN) {
          const key = token32(url, i, j);
          let dup = false;
          for (let k = 0; k < seen.length; k++) if (seen[k] === key) { dup = true; break; }
          if (!dup) { seen.push(key); this.scanBucket(this.tabKey, this.tabStart, this.tabLen, this.ruleIds, key, c); if (c.important) return; }
        }
        i = j;
      } else i++;
    }
    if (this.gramRuleIds.length > 0 && n >= 4) {
      const start = c.evals;
      for (let g = 0; g <= n - 4; g++) {
        this.scanBucket(this.gramKey, this.gramStart, this.gramLen, this.gramRuleIds, gram(url, g), c);
        if (c.important) return;
        if (c.evals - start > MAX_GRAM_EVALS) break;
      }
    }
    const nt = this.noToken;
    for (let k = 0; k < nt.length; k++) { this.evalRule(nt[k], c); if (c.important) return; }
  }
  scanBucket(keys, starts, lens, ids, key, c) {
    const mask = keys.length - 1; let slot = slotOf(key, mask);
    for (;;) {
      const len = lens[slot];
      if (len === 0) return;
      if (keys[slot] === key) { const st = starts[slot]; for (let k = st; k < st + len; k++) { this.evalRule(ids[k], c); if (c.important) return; } return; }
      slot = (slot + 1) & mask;
    }
  }
  evalRule(id, c) {
    c.evals++;
    const w = this.meta[id];
    if ((w & TYPES & c.typeMask) === 0) return;
    const party = (w >>> PARTY_SHIFT) & 3;
    if (party === 1 && !c.third) return;
    if (party === 2 && c.third) return;
    const isExc = (w & EXC_BIT) !== 0;
    if (isExc) { if (c.excepted) return; } else if (c.blocked && (w & IMP_BIT) === 0) return;
    const d = this.dom[id];
    if (d !== 0 && !this.domainOk(d - 1, c)) return;
    if (!this.matchPattern(id, w, c)) return;
    if (isExc) c.excepted = true;
    else { c.blocked = true; c.category = (w >>> CAT_SHIFT) & 3; if ((w & IMP_BIT) !== 0) c.important = true; }
  }
  domainOk(off, c) {
    const pool = this.domPool;
    const nInc = pool.lo[off], nExc = pool.lo[off + 1];
    const ph = this.pageHashes(c);
    const has = (qh, ql) => { for (let k = 0; k < ph.length; k += 2) if (ph[k] === qh && ph[k + 1] === ql) return true; return false; };
    if (nInc > 0) { let ok = false; for (let k = 0; k < nInc; k++) if (has(pool.hi[off + 2 + k], pool.lo[off + 2 + k])) { ok = true; break; } if (!ok) return false; }
    for (let k = 0; k < nExc; k++) if (has(pool.hi[off + 2 + nInc + k], pool.lo[off + 2 + nInc + k])) return false;
    return true;
  }
  matchPattern(id, w, c) {
    const s = this.patOff[id], e = this.patOff[id + 1];
    if (s === e) return true;
    const url = c.url, right = (w & RIGHT_ANCHOR) !== 0, pb = this.patBytes;
    if ((w & LEFT_HOST) !== 0) {
      let p = c.hs; const he = c.he;
      for (;;) { if (glob(pb, s, e, url, p, right)) return true; const dot = url.indexOf('.', p); if (dot < 0 || dot + 1 >= he) return false; p = dot + 1; }
    }
    if ((w & LEFT_ANCHOR) !== 0) return glob(pb, s, e, url, 0, right);
    const first = pb[s];
    if (first !== STAR && first !== CARET) {
      const ch = String.fromCharCode(first);
      let p = url.indexOf(ch);
      while (p >= 0) { if (glob(pb, s, e, url, p, right)) return true; p = url.indexOf(ch, p + 1); }
      return false;
    }
    for (let p = 0; p <= url.length; p++) if (glob(pb, s, e, url, p, right)) return true;
    return false;
  }
}

function glob(pat, ps, pe, url, start, right) {
  const n = url.length;
  let p = ps, u = start, starP = -1, starU = -1;
  for (;;) {
    if (p === pe) {
      if (!right || u === n) return true;
      if (starP < 0) return false;
      starU++; if (starU > n) return false; u = starU; p = starP + 1; continue;
    }
    const pc = pat[p];
    if (pc === STAR) { starP = p; starU = u; p++; continue; }
    if (pc === CARET) {
      if (u === n) { p++; continue; }
      if (isSep(url.charCodeAt(u))) { p++; u++; continue; }
    } else if (u < n && url.charCodeAt(u) === pc) { p++; u++; continue; }
    if (starP < 0) return false;
    starU++; if (starU > n) return false; u = starU; p = starP + 1;
  }
}
