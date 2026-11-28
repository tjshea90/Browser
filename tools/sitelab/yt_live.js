'use strict';
// RX3-Y3: a LIVE check of the real scriptlet library with the real YouTube list lines against the real site, in plain headless Chromium (the sandbox's agent proxy; see tools/sitelab/yt_probe.js).
//   node tools/sitelab/yt_live.js --ua mobile|desktop --video <id> [--payload <file>] [--wait 12]
// Without --payload: the BASELINE (nothing injected). With --payload <file> (made by YoutubePayloadDump, see its header): geckoapp/.../scriptlets-main.js is injected before any page script and the file's
// calls are handed to it exactly the way content.js does. In both cases a probe records what the page's own fetch / XHR calls RECEIVE (counts of ad slot entries in the answers of the player and feed
// requests), the page's own player response (playability, ad placements), page errors, and whether the video really moves on. Chromium is not Gecko: this shows that the rules and the library work on the
// real site's data and do not break playback; it cannot show how the tablet's engine behaves. A signed-out visitor is usually served no real ads, so "ads removed" can only be seen where the site still sends
// ad slot entries. Nothing is kept: no cookie, no address with a query, no body is printed.
const fs = require('fs');
const path = require('path');
let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const args = process.argv.slice(2);
const opt = (n, d) => { const i = args.indexOf('--' + n); return i >= 0 && i + 1 < args.length ? args[i + 1] : d; };
const MOBILE = (opt('ua', 'mobile') === 'mobile');
const VIDEO = opt('video', 'dQw4w9WgXcQ');
const PAYLOAD = opt('payload', '');
const WAIT = parseInt(opt('wait', '12'), 10) * 1000;
const LIB = fs.readFileSync(path.join(__dirname, '..', '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'scriptlets-main.js'), 'utf8');
const UA = MOBILE
  ? 'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36'
  : 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36';

(async () => {
  const browser = await chromium.launch({ args: ['--autoplay-policy=no-user-gesture-required'], proxy: process.env.HTTPS_PROXY ? { server: process.env.HTTPS_PROXY } : undefined });
  const ctx = await browser.newContext({ userAgent: UA, viewport: MOBILE ? { width: 564, height: 916 } : { width: 1200, height: 800 }, hasTouch: MOBILE, locale: 'en-US' });
  await ctx.addCookies([{ name: 'SOCS', value: 'CAISNggDEitib3FfaWRlbnRpdHlmcm9udGVuZHVpc2VydmVyXzIwMjMwODI5LjA3X3AxGgJlbiACGgYIgJnLpwY', domain: '.youtube.com', path: '/' }]);
  const page = await ctx.newPage();
  const errors = [];
  page.on('pageerror', (e) => errors.push(String(e && e.message || e).slice(0, 140)));
  if (PAYLOAD) {
    const payload = fs.readFileSync(PAYLOAD, 'utf8');
    await page.addInitScript({ content: LIB });
    await page.addInitScript((p) => { window.__n = []; document.addEventListener('__kst_sl_n_v1', (e) => window.__n.push(String(e.detail)), true); document.dispatchEvent(new CustomEvent('__kst_sl_v1', { detail: p })); }, payload);
  }
  // the probe goes AFTER the library: it wraps what the library left, so it sees what the page's own code receives
  await page.addInitScript(() => {
    window.__probe = { answers: [], errors: 0 };
    const KEY = /youtubei\/v1\/|get_watch|[?&]pbj=1/;
    const count = (t, w) => { let n = 0, i = 0; while ((i = t.indexOf(w, i)) >= 0) { n++; i += w.length; } return n; };
    const note = (url, text) => {
      try {
        const u = new URL(url, location.href);
        if (!KEY.test(u.pathname + u.search)) return;
        window.__probe.answers.push({ path: u.pathname.slice(0, 40), bytes: text.length, adSlotRenderer: count(text, 'adSlotRenderer'), adPlacements: count(text, '"adPlacements"'), playerAds: count(text, '"playerAds"') });
      } catch (e) { window.__probe.errors++; }
    };
    const f = window.fetch;
    window.fetch = function () {
      const p = f.apply(this, arguments);
      try { const u = typeof arguments[0] === 'string' ? arguments[0] : (arguments[0] && arguments[0].url); p.then((r) => { try { r.clone().text().then((t) => note(u, t), () => {}); } catch (e) { window.__probe.errors++; } }, () => {}); } catch (e) { window.__probe.errors++; }
      return p;
    };
    const open = XMLHttpRequest.prototype.open;
    XMLHttpRequest.prototype.open = function (m, u) { this.__u = u; this.addEventListener('load', () => { try { if (this.responseType === '' || this.responseType === 'text') note(this.__u, this.responseText); } catch (e) { window.__probe.errors++; } }); return open.apply(this, arguments); };
  });
  const out = { mode: PAYLOAD ? 'WITH the real library + ' + path.basename(PAYLOAD) : 'BASELINE (nothing injected)', site: MOBILE ? 'm.youtube.com' : 'www.youtube.com', video: VIDEO };
  try {
    await page.goto('https://' + (MOBILE ? 'm' : 'www') + '.youtube.com/watch?v=' + VIDEO, { waitUntil: 'domcontentloaded', timeout: 45000 });
    await page.waitForTimeout(WAIT);
    const t1 = await page.evaluate(() => { const v = document.querySelector('video'); return v ? v.currentTime : -1; });
    await page.waitForTimeout(3000);
    Object.assign(out, await page.evaluate(() => {
      const v = document.querySelector('video'), r = window.ytInitialPlayerResponse;
      const a = window.__probe.answers;
      return {
        playability: r && r.playabilityStatus ? r.playabilityStatus.status : 'no player response',
        initialAdPlacements: r && r.adPlacements ? r.adPlacements.length : 0,
        video: v ? { playing: !v.paused, t: Math.round(v.currentTime * 10) / 10, readyState: v.readyState } : null,
        answersSeen: a.length, adSlotRendererInAnswers: a.reduce((n, x) => n + x.adSlotRenderer, 0), adPlacementsKeysInAnswers: a.reduce((n, x) => n + x.adPlacements, 0),
        answers: a.slice(0, 8), probeErrors: window.__probe.errors, libraryReports: window.__n || [],
      };
    }));
    out.videoAdvanced = out.video && t1 >= 0 ? out.video.t > t1 : false;
  } catch (e) { out.error = String(e && e.message || e).slice(0, 160); }
  out.pageErrors = errors.slice(0, 6);
  console.log(JSON.stringify(out, null, 1));
  await browser.close();
})();
