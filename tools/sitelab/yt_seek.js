'use strict';
// tools/sitelab/yt_seek.js [video id] [--kestrel] - what happens when a finger taps YouTube's seek bar, in plain headless Chromium with a touch screen (YS1, owner message 42).
// Needs the cloud sandbox's agent proxy (HTTPS_PROXY). Sends the tablet's Firefox-Android user agent to m.youtube.com. Logs which events the page gets for the tap and whether the video's currentTime moved.
// With --kestrel the REAL page scripts of the built-in extension are loaded the way the engine loads them: guard-main.js in the page (before any page script), content.js / youtube.js in the page too with a
// fake `browser.runtime` that answers like the app does (guard: on + skip; css: nothing). Chromium is not Gecko: this shows how the PAGE seeks and whether OUR scripts get in its way, not the engine's touch handling.
// Never puts a cookie, token or signed URL in its output.
const fs = require('fs');
const path = require('path');
let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const args = process.argv.slice(2);
const VIDEO = args.find((a) => !a.startsWith('--')) || 'dQw4w9WgXcQ';
const KESTREL = args.includes('--kestrel');
const DIR = path.join(__dirname, '..', '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields');
const read = (f) => fs.readFileSync(path.join(DIR, f), 'utf8');
const UA = 'Mozilla/5.0 (Android 10; Mobile; rv:160.0) Gecko/160.0 Firefox/160.0';

(async () => {
  const browser = await chromium.launch({ args: ['--autoplay-policy=no-user-gesture-required', '--mute-audio'], proxy: process.env.HTTPS_PROXY ? { server: process.env.HTTPS_PROXY } : undefined });
  const ctx = await browser.newContext({ userAgent: UA, viewport: { width: 564, height: 916 }, hasTouch: true, isMobile: true, deviceScaleFactor: 1.0625, locale: 'en-US' });
  await ctx.addCookies([{ name: 'SOCS', value: 'CAISNggDEitib3FfaWRlbnRpdHlmcm9udGVuZHVpc2VydmVyXzIwMjMwODI5LjA3X3AxGgJlbiACGgYIgJnLpwY', domain: '.youtube.com', path: '/' }]);
  const page = await ctx.newPage();
  const errors = [];
  page.on('pageerror', (e) => errors.push(String(e && e.message || e).slice(0, 120)));
  if (KESTREL) {
    await page.addInitScript({ content: read('guard-main.js') });
    await page.addInitScript(() => {
      window.__msgs = [];
      window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m && m.k); if (m && m.k === 'guard') return Promise.resolve({ on: true, top: location.hostname, skip: true }); return Promise.resolve(undefined); }, onMessage: { addListener() {} } } };
    });
    await page.addInitScript({ content: read('content.js') });
    await page.addInitScript({ content: read('youtube.js') });
  }
  await page.addInitScript(() => {
    window.__ev = [];
    const t0 = performance.now();
    const names = ['touchstart', 'touchend', 'touchcancel', 'pointerdown', 'pointerup', 'pointercancel', 'mousedown', 'mouseup', 'click'];
    const desc = (e) => { const n = e.target; return n && n.tagName ? (n.tagName.toLowerCase() + '.' + String(n.className && n.className.baseVal !== undefined ? n.className.baseVal : n.className || '').split(/\s+/).slice(0, 2).join('.')) : '?'; };
    for (const n of names) window.addEventListener(n, (e) => { if (window.__log) window.__ev.push([Math.round(performance.now() - t0), n, desc(e), Math.round((e.changedTouches ? e.changedTouches[0].clientX : e.clientX) || 0), Math.round((e.changedTouches ? e.changedTouches[0].clientY : e.clientY) || 0), e.isTrusted ? 1 : 0, e.defaultPrevented ? 'P' : '']); }, true);
    window.addEventListener('mousemove', () => {}, true);
  });
  const out = { mode: KESTREL ? 'WITH the real Kestrel page scripts' : 'plain page (nothing injected)', video: VIDEO };
  try {
    await page.goto('https://m.youtube.com/watch?v=' + VIDEO, { waitUntil: 'domcontentloaded', timeout: 45000 });
    await page.waitForFunction(() => { const v = document.querySelector('video'); return v && v.readyState >= 2 && v.duration > 30; }, null, { timeout: 40000 }).catch(() => {});
    await page.waitForTimeout(4000);
    out.start = await page.evaluate(() => { const v = document.querySelector('video'); return v ? { t: Math.round(v.currentTime * 10) / 10, dur: Math.round(v.duration), paused: v.paused, seeking: v.seeking } : null; });
    const vr = await page.evaluate(() => { const v = document.querySelector('video'); const r = v.getBoundingClientRect(); return { x: r.left + r.width / 2, y: r.top + r.height / 2, w: r.width, h: r.height }; });
    // a tap on the picture shows the controls
    await page.touchscreen.tap(vr.x, vr.y);
    await page.waitForTimeout(1500);
    out.bars = await page.evaluate(() => {
      const list = [];
      for (const el of document.querySelectorAll('[class*="progress" i], [class*="seek" i], [class*="scrub" i], [role="slider"]')) {
        const r = el.getBoundingClientRect();
        if (r.width < 40 || r.height <= 0 || r.height > 80) continue;
        list.push({ tag: el.tagName.toLowerCase(), cls: String(el.className && el.className.baseVal !== undefined ? el.className.baseVal : el.className).slice(0, 70), role: el.getAttribute('role') || '', x: Math.round(r.left), y: Math.round(r.top), w: Math.round(r.width), h: Math.round(r.height) });
      }
      return list.slice(0, 14);
    });
    // the widest bar near the bottom of the picture is the seek bar
    const bar = out.bars.filter((b) => b.w > 200).sort((a, b) => b.w - a.w)[0];
    if (bar) {
      const x = bar.x + bar.w * 0.7, y = bar.y + bar.h / 2;
      await page.evaluate(() => { window.__ev.length = 0; window.__log = true; });
      const before = await page.evaluate(() => document.querySelector('video').currentTime);
      await page.touchscreen.tap(x, y);
      await page.waitForTimeout(2500);
      const after = await page.evaluate(() => ({ t: document.querySelector('video').currentTime, seeking: document.querySelector('video').seeking }));
      out.tap = { at: [Math.round(x), Math.round(y)], bar: bar.cls, before: Math.round(before * 10) / 10, after: Math.round(after.t * 10) / 10, expectedAbout: Math.round(out.start.dur * 0.7), jumped: Math.abs(after.t - out.start.dur * 0.7) < out.start.dur * 0.1 };
      out.events = await page.evaluate(() => window.__ev.slice(0, 40));
    } else out.note = 'no seek bar found';
    if (KESTREL) out.kestrelMessages = await page.evaluate(() => Array.from(new Set(window.__msgs)).slice(0, 12));
  } catch (e) { out.error = String(e && e.message || e).slice(0, 200); }
  out.pageErrors = errors.slice(0, 5);
  console.log(JSON.stringify(out, null, 1));
  await browser.close();
})();
