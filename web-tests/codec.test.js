'use strict';
// codec-main.js (the page-side video format policy, ANS52g) in real headless Chromium. The three questions a player asks are replaced BEFORE the script is installed by stand-ins that always say yes, so
// "the engine answered" (yes) and "the policy answered" (no) are told apart exactly, whatever codecs this Chromium build has. The script is plain DOM code that behaves the same in Gecko; what only the real
// engine can show (world MAIN registration in the manifest, the real MediaSource) is checked on the emulator lab. Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const SHIELDS = path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields');
const CODEC = fs.readFileSync(path.join(SHIELDS, 'codec-main.js'), 'utf8');
let browser;
before(async () => { browser = await chromium.launch(); });
after(async () => { await browser.close(); });

// the tablet's real limits (research/VIDEO_ACCELERATION.md): H.264 and H.265 2048x1088 at 522000 macroblocks a second (1080p about 64), VP9 2048x1088 at 244800 (1080p30), no hardware AV1
const TABLET = '2048x1088@522000|2048x1088@522000|2048x1088@244800|-';
const P = (mode, fps30, lims) => `${mode}${fps30 ? 1 : 0}|${lims || TABLET}`;

/** A page at https://movie.example/ with stand-ins for the engine's three answers, then the script under test, then (when [policy] is given) the policy the content script would hand over. */
async function open(policy) {
  const context = await browser.newContext();
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', body: '<!doctype html><html><head><title>movie</title></head><body>video page</body></html>' }));
  const page = await context.newPage();
  await page.addInitScript(() => {
    window.__engine = [];                                                     // what the engine (stand-in) was asked
    const def = (obj, name, fn) => Object.defineProperty(obj, name, { value: fn, writable: true, enumerable: true, configurable: true });
    def(MediaSource, 'isTypeSupported', function isTypeSupported(t) { window.__engine.push(['mse', String(t)]); return true; });
    def(HTMLMediaElement.prototype, 'canPlayType', function canPlayType(t) { window.__engine.push(['can', String(t)]); return 'probably'; });
    def(MediaCapabilities.prototype, 'decodingInfo', function decodingInfo(c) { window.__engine.push(['mc', JSON.stringify(c)]); return Promise.resolve({ supported: true, smooth: true, powerEfficient: false }); });
    window.__reports = [];
    document.addEventListener('__kst_codec_rep_v1', (e) => window.__reports.push(String(e.detail)), true);
    window.__ready = 0;
    document.addEventListener('__kst_codec_ready_v1', () => window.__ready++, true);
  });
  await page.addInitScript({ content: CODEC });
  if (policy !== undefined) await page.addInitScript((p) => { document.dispatchEvent(new CustomEvent('__kst_codec_cfg_v1', { detail: p })); }, policy);
  await page.goto('https://movie.example/');
  return page;
}
const mse = (page, t) => page.evaluate((x) => MediaSource.isTypeSupported(x), t);
const can = (page, t) => page.evaluate((x) => document.createElement('video').canPlayType(x), t);
const mc = (page, video) => page.evaluate((v) => navigator.mediaCapabilities.decodingInfo({ type: 'media-source', video: v }), video);

const vp9 = (w, h, f) => `video/webm; codecs="vp09.00.40.08"` + (w ? `; width=${w}; height=${h}` : '') + (f ? `; framerate=${f}` : '');
const avc = (w, h, f) => `video/mp4; codecs="avc1.640028"` + (w ? `; width=${w}; height=${h}` : '') + (f ? `; framerate=${f}` : '');

test('no policy yet: every answer is the engine\'s own, nothing is refused', async () => {
  const page = await open();
  assert.equal(await page.evaluate(() => window.__ready), 1, 'the script announces itself so the content script can hand over the policy');
  for (const t of [vp9(3840, 2160, 60), avc(3840, 2160, 60), 'video/webm; codecs="vp9"', 'video/mp4; codecs="av01.0.05M.08"']) assert.equal(await mse(page, t), true, t);
  assert.equal(await can(page, vp9(3840, 2160, 60)), 'probably');
  assert.equal((await mc(page, { contentType: vp9(0), width: 3840, height: 2160, framerate: 60 })).supported, true);
});

test('Automatic: VP9 passes up to what the chip declares (1080p30) and is refused above it, by size and by pixel rate', async () => {
  const page = await open(P(0, false));
  const yes = [vp9(), vp9(1920, 1080, 30), vp9(1920, 1080, 29.97), vp9(1280, 720, 60), vp9(1280, 720), vp9(1920, 1080), vp9(1080, 1920, 30), vp9(640, 360, 60), vp9(0, 0, 60), 'video/webm; codecs="vp9"', 'video/webm'];
  const no = [vp9(1920, 1080, 60), vp9(2560, 1440, 30), vp9(3840, 2160, 30), vp9(3840, 2160, 60), vp9(1080, 1920, 60), vp9(2048, 1152, 30), 'Video/WebM; Codecs="VP9"; Width=3840; Height=2160'];
  for (const t of yes) assert.equal(await mse(page, t), true, 'should pass: ' + t);
  for (const t of no) assert.equal(await mse(page, t), false, 'should be refused: ' + t);
});

test('Automatic: H.264 passes up to its own, larger limit; a codec with no hardware decoder is left to the engine', async () => {
  const page = await open(P(0, false));
  for (const t of [avc(), avc(1920, 1080, 30), avc(1920, 1080, 60), avc(1280, 720, 60), 'video/mp4; codecs="avc1.4d401f, mp4a.40.2"; width=1920; height=1080; framerate=30']) assert.equal(await mse(page, t), true, t);
  for (const t of [avc(3840, 2160, 30), avc(2560, 1440, 60), avc(3840, 2160, 60)]) assert.equal(await mse(page, t), false, t);
  // AV1 has "-": the engine's own preferences decide (here: the stand-in says yes)
  for (const t of ['video/mp4; codecs="av01.0.05M.08"; width=1920; height=1080; framerate=60', 'video/webm; codecs="av01.0.08M.10"; width=3840; height=2160']) assert.equal(await mse(page, t), true, t);
  // audio and containers without a video codec are never touched
  for (const t of ['audio/mp4; codecs="mp4a.40.2"', 'audio/webm; codecs="opus"', 'audio/mpeg', 'application/x-mpegURL', 'video/mp4']) assert.equal(await mse(page, t), true, t);
});

test('H.264 only: every other video codec is refused, also with no size, and H.264 keeps its hardware limit', async () => {
  const page = await open(P(1, false));
  for (const t of ['video/webm; codecs="vp9"', vp9(1280, 720, 30), 'video/mp4; codecs="hvc1.1.6.L93.B0"', 'video/mp4; codecs="hev1.1.6.L93.B0"', 'video/mp4; codecs="av01.0.05M.08"', 'video/webm; codecs="vp8"', 'video/webm; codecs="vp8, vorbis"', 'video/webm', 'video/mp4; codecs="dvh1.05.06"']) assert.equal(await mse(page, t), false, 'should be refused: ' + t);
  for (const t of [avc(), avc(1920, 1080, 60), 'video/mp4', 'audio/webm; codecs="opus"', 'audio/mp4; codecs="mp4a.40.2"']) assert.equal(await mse(page, t), true, 'should pass: ' + t);
  assert.equal(await mse(page, avc(3840, 2160, 30)), false);
});

test('Everything: nothing is refused for its codec or size', async () => {
  const page = await open(P(2, false));
  for (const t of [vp9(3840, 2160, 60), avc(3840, 2160, 60), 'video/mp4; codecs="av01.0.05M.08"', 'video/webm', 'video/mp4; codecs="hvc1.1.6.L93.B0"']) assert.equal(await mse(page, t), true, t);
});

test('the 30 frames a second switch refuses a stated rate above 30 in every mode, and nothing without a stated rate', async () => {
  for (const mode of [0, 1, 2]) {
    const page = await open(P(mode, true));
    assert.equal(await mse(page, avc(1280, 720, 60)), false, 'mode ' + mode + ' 60 fps');
    assert.equal(await mse(page, avc(0, 0, 50)), false, 'mode ' + mode + ' 50 fps with no size');
    assert.equal(await mse(page, avc(1280, 720, 30)), true, 'mode ' + mode + ' 30 fps');
    assert.equal(await mse(page, avc(1280, 720, 29.97)), true);
    assert.equal(await mse(page, avc(1920, 1080)), true, 'mode ' + mode + ' no rate stated');
    assert.equal(await mse(page, 'audio/mp4; codecs="mp4a.40.2"; framerate=60'), true, 'audio has no frame rate to cap');
  }
});

test('canPlayType answers the empty string where isTypeSupported says no, and the engine otherwise', async () => {
  const page = await open(P(1, false));
  assert.equal(await can(page, 'video/webm; codecs="vp9"'), '');
  assert.equal(await can(page, vp9(1920, 1080, 60)), '');
  assert.equal(await can(page, avc(1920, 1080, 30)), 'probably');
  assert.equal(await can(page, 'video/mp4'), 'probably');
  const a = await open(P(0, false));
  assert.equal(await can(a, vp9(3840, 2160, 30)), '');
  assert.equal(await can(a, vp9(1920, 1080, 30)), 'probably');
});

test('mediaCapabilities.decodingInfo: refused configurations get supported:false, the rest go to the engine unchanged', async () => {
  const page = await open(P(0, false));
  const no = await mc(page, { contentType: vp9(0), width: 1920, height: 1080, framerate: 60, bitrate: 6e6 });
  assert.deepEqual(no, { supported: false, smooth: false, powerEfficient: false });
  assert.equal((await mc(page, { contentType: vp9(0), width: 1920, height: 1080, framerate: 30, bitrate: 3e6 })).supported, true);
  assert.equal((await mc(page, { contentType: vp9(0), width: 1920, height: 1080, framerate: '30000/1001' })).supported, true, 'a fraction such as 30000/1001 is read');
  assert.equal((await mc(page, { contentType: vp9(0), width: 1920, height: 1080, framerate: '60000/1001' })).supported, false, '59.94 fps is above the limit');
  assert.equal((await mc(page, { contentType: vp9(0), width: 3840, height: 2160, framerate: 30 })).supported, false);
  assert.equal((await mc(page, { contentType: vp9(0) })).supported, true, 'no size, no rate: never refused');
  assert.equal((await mc(page, { contentType: 'video/mp4; codecs="avc1.640028"', width: 1920, height: 1080, framerate: 60 })).supported, true);
  // a call the policy has no opinion on reaches the engine with exactly what the page sent
  const sent = await page.evaluate(() => window.__engine.filter((x) => x[0] === 'mc').map((x) => JSON.parse(x[1]).video.contentType));
  assert.equal(sent.length, 5);
  const audioOnly = await page.evaluate(() => navigator.mediaCapabilities.decodingInfo({ type: 'file', audio: { contentType: 'audio/mp4; codecs="mp4a.40.2"' } }));
  assert.equal(audioOnly.supported, true);
  const h = await open(P(1, false));
  assert.equal((await mc(h, { contentType: 'video/webm; codecs="vp9"' })).supported, false, 'H.264 only refuses VP9 in mediaCapabilities too');
});

test('the refused call is answered at once and never reaches the engine; the allowed one does, unchanged', async () => {
  const page = await open(P(0, false));
  await mse(page, vp9(3840, 2160, 60));
  await mse(page, avc(1920, 1080, 30));
  const seen = await page.evaluate(() => window.__engine.slice());
  assert.deepEqual(seen, [['mse', avc(1920, 1080, 30)]]);
});

test('a policy that is nonsense, or forged by the page, cannot break anything; an empty one switches the policy off', async () => {
  const page = await open(P(0, false));
  assert.equal(await mse(page, vp9(3840, 2160, 60)), false);
  await page.evaluate(() => { for (const bad of ['garbage', '9x|a|b|c|d', '00|1|2|3', '00|2048x1088@1|2048x1088@1|2048x1088@1|-|extra', 42, null, '00|' + 'x'.repeat(500)]) document.dispatchEvent(new CustomEvent('__kst_codec_cfg_v1', { detail: bad })); });
  assert.equal(await mse(page, vp9(3840, 2160, 60)), false, 'nonsense keeps the policy that was told before');
  await page.evaluate(() => document.dispatchEvent(new CustomEvent('__kst_codec_cfg_v1', { detail: '' })));
  assert.equal(await mse(page, vp9(3840, 2160, 60)), true, 'an empty policy means none');
  await page.evaluate((p) => document.dispatchEvent(new CustomEvent('__kst_codec_cfg_v1', { detail: p })), P(1, false));
  assert.equal(await mse(page, 'video/webm; codecs="vp9"'), false, 'a new policy replaces the old one at once (the cache is emptied)');
});

test('the wrappers look like the engine\'s own functions and keep their properties', async () => {
  const page = await open(P(0, false));
  const r = await page.evaluate(() => {
    const d = Object.getOwnPropertyDescriptor(MediaSource, 'isTypeSupported');
    return {
      name: MediaSource.isTypeSupported.name, len: MediaSource.isTypeSupported.length, str: String(MediaSource.isTypeSupported),
      can: String(HTMLMediaElement.prototype.canPlayType), dec: String(MediaCapabilities.prototype.decodingInfo),
      w: d.writable, e: d.enumerable, c: d.configurable,
      marker: Object.keys(window).indexOf('__kstCodec'), thrown: (() => { try { MediaSource.isTypeSupported(); return 'none'; } catch (x) { return 'threw'; } })(),
      objectArg: MediaSource.isTypeSupported({ toString() { return 'video/webm; codecs="vp9"; width=3840; height=2160'; } }),
    };
  });
  assert.equal(r.name, 'isTypeSupported');
  assert.equal(r.len, 1);
  assert.match(r.str, /\[native code\]/);
  assert.match(r.can, /\[native code\]/);
  assert.match(r.dec, /\[native code\]/);
  assert.deepEqual([r.w, r.e, r.c], [true, true, true]);
  assert.equal(r.marker, -1, 'the one marker the script leaves is not enumerable');
  assert.equal(r.objectArg, true, 'a question that is not a string goes to the engine untouched');
});

test('what was refused is counted and reported once, in numbers and one short word, never an address', async () => {
  const page = await open(P(0, false));
  for (let i = 0; i < 5; i++) await mse(page, vp9(3840, 2160, 60));
  await mse(page, vp9(1920, 1080, 60));
  await mse(page, avc(1920, 1080, 30));
  assert.deepEqual(await page.evaluate(() => window.__reports), [], 'nothing is sent at once');
  await page.waitForFunction(() => window.__reports.length > 0, null, { timeout: 5000 });
  const rep = await page.evaluate(() => window.__reports);
  assert.equal(rep.length, 1);
  const [asked, size, mode, fps, word] = rep[0].split('|');
  assert.equal(Number(asked), 7);
  assert.equal(Number(size), 6);
  assert.equal(Number(mode), 0);
  assert.equal(Number(fps), 0);
  assert.equal(word, 'vp9 1920x1080@60');
  assert.ok(!/https?:|movie\.example/.test(rep[0]));
  const h = await open(P(1, true));
  await mse(h, 'video/webm; codecs="vp9"'); await mse(h, avc(1280, 720, 60));
  await h.waitForFunction(() => window.__reports.length > 0, null, { timeout: 5000 });
  const [a2, s2, m2, f2, w2] = (await h.evaluate(() => window.__reports))[0].split('|');
  assert.deepEqual([Number(a2), Number(s2), Number(m2), Number(f2)], [2, 0, 1, 1]);
  assert.equal(w2, 'avc 1280x720@60');
});

test('a hundred identical questions are one parse (the verdict is remembered) and the answers stay right', async () => {
  const page = await open(P(0, false));
  const r = await page.evaluate((t) => { const out = []; for (let i = 0; i < 2000; i++) out.push(MediaSource.isTypeSupported(t)); return out.every((x) => x === false); }, vp9(3840, 2160, 60));
  assert.equal(r, true);
});

test('the script does nothing where the extension API exists (an accidental copy in the isolated world)', async () => {
  const context = await browser.newContext();
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', body: '<!doctype html><title>x</title>' }));
  const page = await context.newPage();
  await page.addInitScript(() => { window.browser = { runtime: {} }; window.__ms = MediaSource.isTypeSupported; });
  await page.addInitScript({ content: CODEC });
  await page.goto('https://movie.example/');
  assert.equal(await page.evaluate(() => MediaSource.isTypeSupported === window.__ms), true);
});

test('a frame gets its own copy (about:blank frames the page makes included)', async () => {
  const page = await open(P(1, false));
  const r = await page.evaluate(() => {
    const f = document.createElement('iframe'); document.body.appendChild(f);
    return typeof f.contentWindow.MediaSource.isTypeSupported;
  });
  assert.equal(r, 'function');
});
