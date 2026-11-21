'use strict';
// RVW5: the NAMING half measured on REAL pages. For each page: what a file name could be built from - document.title (what the app uses today), og:title, twitter:title, the JSON-LD names
// (VideoObject / Movie / TVEpisode / Clip / Article), the first h1, og:site_name - printed side by side so a rule (research/REVIEW_2026-11-23_RVW5.md) rests on evidence, not on a guess.
// A measuring tool, not a test: needs the internet, the sites change, reads nothing from the tablet, writes only to stdout (or --json file).
//   usage: NODE_PATH=$(npm root -g) node tools/sitelab/realnames.js [--json out.json] url [url ...]
const fs = require('fs');
const { chromium } = require('playwright');

async function names(browser, url) {
  const ctx = await browser.newContext({ viewport: { width: 1024, height: 700 }, userAgent: 'Mozilla/5.0 (Android 10; Mobile; rv:160.0) Gecko/160.0 Firefox/160.0' });
  const page = await ctx.newPage();
  const out = { url };
  try {
    const resp = await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 25000 });
    out.status = resp ? resp.status() : 0;
    await page.waitForTimeout(2500);
    out.final = page.url();
    out.data = await page.evaluate(() => {
      const meta = (sel) => { const e = document.querySelector(sel); return e ? String(e.getAttribute('content') || '').trim() : ''; };
      const ld = [];
      document.querySelectorAll('script[type="application/ld+json"]').forEach((s) => {
        if (ld.length >= 12) return;
        let j; try { j = JSON.parse(s.textContent || ''); } catch (e) { return; }
        const arr = Array.isArray(j) ? j : (j && j['@graph'] ? j['@graph'] : [j]);
        arr.slice(0, 20).forEach((o) => { if (o && typeof o === 'object' && o.name) ld.push(String(o['@type'] || '?') + ': ' + String(o.name).slice(0, 160)); else if (o && typeof o === 'object' && o.headline) ld.push(String(o['@type'] || '?') + ' headline: ' + String(o.headline).slice(0, 160)); });
      });
      const h1 = document.querySelector('h1');
      return {
        title: document.title,
        og: meta('meta[property="og:title"]'), tw: meta('meta[name="twitter:title"]'), site: meta('meta[property="og:site_name"]'),
        ogtype: meta('meta[property="og:type"]'), ld, h1: h1 ? String(h1.textContent || '').replace(/\s+/g, ' ').trim().slice(0, 160) : '',
        videos: document.getElementsByTagName('video').length, iframes: document.getElementsByTagName('iframe').length,
      };
    });
  } catch (e) { out.error = String(e && e.message || e).split('\n')[0]; }
  await ctx.close();
  return out;
}

(async () => {
  const args = process.argv.slice(2);
  let json = '';
  const urls = [];
  for (let i = 0; i < args.length; i++) { if (args[i] === '--json') json = args[++i]; else urls.push(args[i]); }
  const browser = await chromium.launch();
  const all = [];
  for (const u of urls) {
    const r = await names(browser, u);
    all.push(r);
    if (r.error) { console.log('## ' + u + '\n   ERROR ' + r.error); continue; }
    const d = r.data;
    console.log('## ' + u + ' (' + r.status + ')\n   title : ' + d.title + '\n   og    : ' + d.og + '\n   tw    : ' + d.tw + '\n   h1    : ' + d.h1 + '\n   site  : ' + d.site + '  ogtype: ' + d.ogtype + '  video:' + d.videos + ' iframe:' + d.iframes + '\n   ld    : ' + (d.ld.join(' || ') || '-'));
  }
  await browser.close();
  if (json) fs.writeFileSync(json, JSON.stringify(all, null, 1));
})();
