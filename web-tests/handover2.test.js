'use strict';
// PERF9 (owner 2026-11-22: "I pressed play again and it did the loading animation for about 1 second then stopped and it never played"): the press-to-play gate asks Kestrel's player first for a tap that hits no control of a held script player. Own file (handover.test.js is near the 60 s limit); same harness. The page half of the hand-over in real headless Chromium. gate-main.js (world MAIN, no extension API) asks, content.js (isolated copy with a fake
// extension API) checks and relays the question as a `vhand` message, the "background page" (this test) answers {h: 1} (Kestrel's own player took the film) or {h: 0}. Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const SHIELDS = path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields');
const read = (f) => fs.readFileSync(path.join(SHIELDS, f), 'utf8');
const VFIND = read('vfind.js'), CONTENT = read('content.js'), GATE = read('gate-main.js');
// 5 seconds of silence as a WAV file (8 kHz, 8 bit, mono): the smallest thing a <video> element really plays in headless Chromium, so that play, pause and play again behave like a real film
const WAV = (() => { const rate = 8000, n = rate * 5, b = Buffer.alloc(44 + n, 128); b.write('RIFF', 0); b.writeUInt32LE(36 + n, 4); b.write('WAVEfmt ', 8); b.writeUInt32LE(16, 16); b.writeUInt16LE(1, 20); b.writeUInt16LE(1, 22); b.writeUInt32LE(rate, 24); b.writeUInt32LE(rate, 28); b.writeUInt16LE(1, 32); b.writeUInt16LE(8, 34); b.write('data', 36); b.writeUInt32LE(n, 40); return b; })();
let browser;
before(async () => { browser = await chromium.launch({ args: ['--autoplay-policy=no-user-gesture-required'] }); });
after(async () => { await browser.close(); });

/**
 * [html]: the body. [guard]: what the background page answers to `guard` (gt: the rule is on, ho: the app may take films over). [handAnswer]: 'yes' | 'no' | 'never' | 'late' (a yes after 1.2 s) for `vhand`.
 * window.__nativePlay: the engine's own play(), kept before the gate replaces it (the way the engine's own controls start a film: not through the page-visible play()).
 */
async function open(html, guard, handAnswer) {
  const context = await browser.newContext({ hasTouch: false });
  await context.route('https://**/*', (r) => {
    const u = new URL(r.request().url());
    if (u.pathname.endsWith('.mp4')) return r.fulfill({ status: 200, contentType: 'video/mp4', body: Buffer.alloc(2000) });
    if (u.pathname.endsWith('.wav')) return r.fulfill({ status: 200, contentType: 'audio/wav', body: WAV });         // (a film the engine can really play: silence, 5 s; a <video> plays it like any file)
    return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>t</title></head><body style="margin:0">${html}</body></html>` });
  });
  const page = await context.newPage();
  await page.addInitScript(({ g, h }) => {
    window.__msgs = []; window.__pauses = 0; window.__nativePlay = HTMLMediaElement.prototype.play;
    const op = HTMLMediaElement.prototype.pause;
    HTMLMediaElement.prototype.pause = function pause() { window.__pauses++; return op.apply(this, arguments); };   // (the gate keeps the original it finds: this one)
    window.browser = { runtime: {
      sendMessage: (m) => {
        window.__msgs.push(m);
        if (m && m.k === 'guard') return Promise.resolve(g);
        if (m && m.k === 'vhand') {
          if (h === 'yes') return Promise.resolve({ h: 1 });
          if (h === 'no') return Promise.resolve({ h: 0 });
          if (h === 'late') return new Promise((res) => setTimeout(() => res({ h: 1 }), 1200));
          return new Promise(() => {});
        }
        return Promise.resolve(undefined);
      },
      onMessage: { addListener() {} },
    } };
  }, { g: guard, h: handAnswer });
  await page.addInitScript({ content: VFIND });
  await page.addInitScript({ content: CONTENT });
  // the gate lives in the page's world and must not see the extension API
  await page.addInitScript({ content: `(function(){var b=window.browser;try{delete window.browser;}catch(e){window.browser=undefined;}${GATE};window.browser=b;})();` });
  await page.goto('https://site.example/');
  return page;
}
const GUARD_HO = { on: true, top: 'site.example', gt: true, ho: true };
const tapOn = async (page, sel) => { const b = await page.locator(sel).boundingBox(); await page.mouse.click(b.x + b.width / 2, b.y + b.height / 2); };
const settle = (page, ms) => page.waitForTimeout(ms || 300);
const vhand = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'vhand'));

test('PERF9: a tap on the picture of a held SCRIPT player that hit no control asks the app first (a plain mark, the film itself); a no opens the page\'s own player; a control, a loop and a film with a file source do not ask', async () => {
  const html = '<div id="player" style="position:relative;width:320px;height:180px"><video id="v" style="display:block;width:320px;height:180px"></video><svg id="mark" width="24" height="24" style="position:absolute;left:148px;top:78px"><circle cx="12" cy="12" r="10"/></svg>'
    + '<button id="mute" style="position:absolute;left:8px;top:8px;width:70px;height:40px">Mute</button></div>'
    + '<script>window.ms = new MediaSource(); window.opened = 0; ms.addEventListener("sourceopen", function () { window.opened++; }); document.getElementById("v").src = URL.createObjectURL(ms);</script>';
  const yes = await open(html, GUARD_HO, 'yes');
  await settle(yes, 400);
  await tapOn(yes, '#mark'); await settle(yes, 600);
  const q = await vhand(yes);
  assert.equal(q.length, 1, 'the tap on a plain mark is a press: asked'); assert.equal(q[0].y, 'press');
  assert.equal(await yes.evaluate(() => window.opened), 0, 'a yes: the page\'s source stays held');
  const no = await open(html, GUARD_HO, 'no');
  await settle(no, 400);
  await tapOn(no, '#v'); await settle(no, 900);
  assert.equal((await vhand(no)).length, 1, 'a tap on the film itself asks too');
  assert.equal(await no.evaluate(() => window.opened), 1, 'a no: the page\'s own player opens');
  const ctl = await open(html, GUARD_HO, 'yes');
  await settle(ctl, 400);
  await tapOn(ctl, '#mute'); await settle(ctl, 600);
  assert.equal((await vhand(ctl)).length, 0, 'Mute is a control: nothing is asked');
});
