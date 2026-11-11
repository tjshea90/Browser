'use strict';
// The extension's JS engine against the Kotlin engine. The fixtures (web-tests/fixtures/mini.*) are written by core/.../filter/ExtImageTest.kt from the REAL matcher, so every line below is
// "Kotlin said X": the JS port must say X as well. If this fails after a change to FilterSet/Psl/Hash/ExtImage, regenerate the fixtures on the Kotlin side first
// (KESTREL_REGEN=1 ./gradlew :core:test --tests '*ExtImageTest*') and then fix whichever engine is wrong. Run: cd web-tests && npm test
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const E = require('../geckoapp/src/main/assets/kestrel-shields/engine.js');

const FX = path.join(__dirname, 'fixtures');
const lines = (name) => fs.readFileSync(path.join(FX, name), 'utf8').split('\n').filter((l) => l.length > 0);
const imageBytes = () => { const b = fs.readFileSync(path.join(FX, 'mini.kex')); return b.buffer.slice(b.byteOffset, b.byteOffset + b.byteLength); };
const NULL = '\u0001null';

test('hash: FNV-1a + murmur finaliser on 32-bit limbs equals the Kotlin 64-bit hash', () => {
  const rows = lines('mini.hash.tsv');
  assert.ok(rows.length > 300);
  let bad = [];
  for (const r of rows) {
    const [s, hi, lo] = r.split('\t');
    const got = E.h64(s);
    if (got[0] !== Number(hi) || got[1] !== Number(lo)) bad.push(`${JSON.stringify(s)}: want ${hi},${lo} got ${got}`);
  }
  assert.deepEqual(bad.slice(0, 5), [], `${bad.length} of ${rows.length} hashes differ`);
});

test('hostOf equals UrlParts.hostOf', () => {
  const rows = lines('mini.host.tsv');
  for (const r of rows) {
    const [url, want] = r.split('\t');
    assert.equal(E.hostOf(url), want === NULL ? null : want, url);
  }
});

test('the image loads and reports what the Kotlin side wrote', () => {
  const eng = E.create(imageBytes());
  assert.equal(eng.generation, 1);
  assert.ok(eng.hostRules > 0 && eng.patternRules > 0 && eng.tokenRules > 0 && eng.gramRules > 0 && eng.tokenlessRules > 0, JSON.stringify(eng));
});

test('Public Suffix List: siteOf and isSameSite equal Psl', () => {
  const eng = E.create(imageBytes());
  let n = 0;
  for (const r of lines('mini.psl.tsv')) {
    const f = r.split('\t');
    if (f[0] === 'same') assert.equal(eng.isSameSite(f[1], f[2]) ? 1 : 0, Number(f[3]), `same ${f[1]} ${f[2]}`);
    else assert.equal(eng.siteOf(f[0]), f[1] === NULL ? null : f[1], `siteOf ${JSON.stringify(f[0])}`);
    n++;
  }
  assert.ok(n > 100);
});

test('verdicts: every request in the corpus gets the Kotlin verdict (block, category, $important, exception)', () => {
  const eng = E.create(imageBytes());
  const rows = lines('mini.corpus.tsv');
  assert.ok(rows.length > 2000);
  const bad = [];
  const counts = {};
  for (const r of rows) {
    const [url, type, page, want] = r.split('\t');
    const got = eng.check(url, page === '' ? null : page, Number(type));
    counts[got] = (counts[got] || 0) + 1;
    if (got !== Number(want)) bad.push(`${url.length > 140 ? url.slice(0, 140) + '...(' + url.length + ')' : url}  type=${type} page=${page || '-'}  want ${want} got ${got}`);
  }
  assert.deepEqual(bad.slice(0, 8), [], `${bad.length} of ${rows.length} verdicts differ`);
  assert.ok(Object.keys(counts).length >= 6, 'the corpus must exercise several verdict kinds: ' + JSON.stringify(counts));
});

test('a damaged or truncated image is refused, never half-loaded', () => {
  const good = imageBytes();
  assert.doesNotThrow(() => E.create(good));
  const flip = (at) => { const c = good.slice(0); new Uint8Array(c)[at] ^= 0x55; return c; };
  assert.throws(() => E.create(flip(100)), /checksum|layout|magic|sections/);
  assert.throws(() => E.create(flip(good.byteLength >> 1)), /checksum/);
  assert.throws(() => E.create(flip(0)), /magic/);
  assert.throws(() => E.create(good.slice(0, good.byteLength - 8)), /checksum|layout|overrun|trailer/);
  assert.throws(() => E.create(good.slice(0, 20)), /size/);
  assert.throws(() => E.create(new ArrayBuffer(good.byteLength)), /magic/);
});

test('request types map like ShieldsBridge.requestType', () => {
  const T = E.T;
  const want = { script: T.SCRIPT, stylesheet: T.STYLESHEET, image: T.IMAGE, imageset: T.IMAGE, media: T.MEDIA, font: T.FONT, xmlhttprequest: T.XHR, fetch: T.XHR, websocket: T.WEBSOCKET,
    sub_frame: T.SUBDOCUMENT, object: T.OBJECT, object_subrequest: T.OBJECT, ping: T.PING, beacon: T.PING, other: T.OTHER, csp_report: T.OTHER, speculative: T.OTHER, xslt: T.OTHER };
  for (const k of Object.keys(want)) assert.equal(E.typeMask(k), want[k], k);
});

test('hostile URLs cost bounded work (a hot token repeated 330 times, a 5000 character userinfo, a 100 kB URL)', () => {
  const eng = E.create(imageBytes());
  const cases = [
    'https://x.net/?' + 'event/'.repeat(330) + 'pixel.gif',
    'https://' + 'j'.repeat(5000) + '@ads.example.com/x.js',
    'https://x.net/' + 'a/'.repeat(50000) + 'banner/1',
    'https://x.net/' + 'q*z*y'.repeat(20000),
  ];
  for (const u of cases) {
    const t0 = process.hrtime.bigint();
    eng.check(u, 'example.com', E.T.SCRIPT);
    const ms = Number(process.hrtime.bigint() - t0) / 1e6;
    assert.ok(ms < 60, `${u.slice(0, 40)}... took ${ms} ms`);
  }
});

test('speed (informational): average microseconds per check over the corpus', () => {
  const eng = E.create(imageBytes());
  const rows = lines('mini.corpus.tsv').map((r) => r.split('\t')).filter((f) => f[0].length < 600);
  for (let i = 0; i < 3; i++) for (const [u, t, p] of rows) eng.check(u, p === '' ? null : p, Number(t)); // warm up
  const t0 = process.hrtime.bigint();
  for (let i = 0; i < 5; i++) for (const [u, t, p] of rows) eng.check(u, p === '' ? null : p, Number(t));
  const us = Number(process.hrtime.bigint() - t0) / 1e3 / (rows.length * 5);
  console.log(`# engine.js: ${us.toFixed(2)} us per check on this machine (x86 V8; the tablet will be several times slower)`);
  assert.ok(us < 200);
});
