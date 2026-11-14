'use strict';
// tools/sitelab/yt_probe.js <video id> - what does YouTube ITSELF answer for a watch page in plain headless Chromium (no Shields, no extension)?
// Prints the page's own ytInitialPlayerResponse.playabilityStatus, the number of ad placements and the visible text, for a mobile and a desktop user agent.
// Needs the cloud sandbox's agent proxy (HTTPS_PROXY; its CA is already in the browser trust store). Chromium is not Gecko: this shows the SERVERS' answers and which data exists, not the tablet's engine.
// Origin: RX4 (owner screenshot of a Movies & TV listing), research/YOUTUBE_ADS.md section 2. Never put a cookie, token or signed URL in its output.
let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const VIDEO = process.argv[2] || 'pQ73wo2JMb8';
const CASES = [
  { name: 'mobile site, Firefox-Android user agent (what the tablet sends)', host: 'm.youtube.com', mobile: true,
    ua: 'Mozilla/5.0 (Android 10; Mobile; rv:160.0) Gecko/160.0 Firefox/160.0', viewport: { width: 564, height: 916 } },
  { name: 'mobile site, Chrome-Android user agent', host: 'm.youtube.com', mobile: true,
    ua: 'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36', viewport: { width: 564, height: 916 } },
  { name: 'desktop site, Firefox-on-Linux user agent', host: 'www.youtube.com', mobile: false,
    ua: 'Mozilla/5.0 (X11; Linux x86_64; rv:160.0) Gecko/20100101 Firefox/160.0', viewport: { width: 1200, height: 800 } },
];

(async () => {
  const browser = await chromium.launch({ proxy: process.env.HTTPS_PROXY ? { server: process.env.HTTPS_PROXY } : undefined });
  for (const c of CASES) {
    const ctx = await browser.newContext({ userAgent: c.ua, viewport: c.viewport, hasTouch: c.mobile, isMobile: false, locale: 'en-US' });
    const page = await ctx.newPage();
    let status = '';
    page.on('response', (r) => { if (r.url().includes('/watch?v=' + VIDEO) && !status) status = String(r.status()); });
    const out = { case: c.name, http: '', playability: null, text: '', adPlacements: null };
    try {
      await page.goto('https://' + c.host + '/watch?v=' + VIDEO, { waitUntil: 'domcontentloaded', timeout: 40000 });
      await page.waitForTimeout(7000);
      out.http = status;
      out.playability = await page.evaluate(() => {
        const r = window.ytInitialPlayerResponse;
        if (!r) return 'no ytInitialPlayerResponse';
        const ps = r.playabilityStatus || {};
        const er = (ps.errorScreen && ps.errorScreen.playerErrorMessageRenderer) || {};
        const txt = (x) => (x && (x.simpleText || (x.runs || []).map((y) => y.text).join(''))) || '';
        return { status: ps.status, reason: ps.reason, errReason: txt(er.reason), errSub: txt(er.subreason), hasStreamingData: !!r.streamingData };
      });
      out.adPlacements = await page.evaluate(() => { const r = window.ytInitialPlayerResponse; return r ? (r.adPlacements ? r.adPlacements.length : 0) : null; });
      out.text = (await page.evaluate(() => document.body ? document.body.innerText : '')).replace(/\s+/g, ' ').slice(0, 420);
      out.title = await page.title();
    } catch (e) { out.error = String(e && e.message || e).slice(0, 300); }
    console.log(JSON.stringify(out, null, 1));
    await ctx.close();
  }
  await browser.close();
})();
