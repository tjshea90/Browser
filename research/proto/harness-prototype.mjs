// Research scratch: A/B page-load harness. Same pages, same browser, engine OFF vs ON (our JS port of Kestrel's matcher decides in page.route()).
// This is the shape of the CI measurement job (requests, bytes, DOMContentLoaded/load) - run here in Chromium because the sandbox has no Firefox.
// usage: node harness.mjs <default.kfs> <sites.txt> <out.json> [runs]
import { chromium } from '/opt/node-tools/node_modules/playwright/index.mjs';
import { readFileSync, writeFileSync } from 'node:fs';
import { Kfs } from './kfs.mjs';

const [kfsPath, sitesPath, outPath, runsArg] = process.argv.slice(2);
const runs = +(runsArg || 1);
const f = readFileSync(kfsPath);
const eng = Kfs.load(new Uint8Array(f.buffer, f.byteOffset, f.length));
const sites = readFileSync(sitesPath, 'utf8').split('\n').map(s => s.trim()).filter(Boolean);
const TYPE = { document: 2, stylesheet: 4, image: 16, media: 64, font: 32, script: 8, xhr: 128, fetch: 128, eventsource: 128, websocket: 256, ping: 512 };

async function once(browser, url, mode) {
  const ctx = await browser.newContext({ viewport: { width: 600, height: 973 }, userAgent: 'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36', locale: 'en-US' });
  const page = await ctx.newPage();
  const pageHost = new URL(url).hostname.toLowerCase();
  let blocked = 0, allowed = 0, bytes = 0, lastFinish = 0; const t0 = Date.now(); const pend = [];
  await page.route('**/*', route => {
    const r = route.request();
    if (r.resourceType() === 'document' && r.frame() === page.mainFrame()) return route.continue();
    if (mode === 'on') {
      const v = eng.check(r.url(), pageHost, TYPE[r.resourceType()] || 2048);
      if (v & 1) { blocked++; return route.abort('blockedbyclient'); }
    }
    allowed++;
    return route.continue();
  });
  page.on('requestfinished', r => { lastFinish = Date.now() - t0; pend.push(r.sizes().then(s => { bytes += (s.responseBodySize || 0) + (s.responseHeadersSize || 0); }).catch(() => {})); });
  let ok = true, dcl = -1, load = -1;
  try {
    await page.goto(url, { waitUntil: 'load', timeout: 40000 });
    await page.waitForTimeout(3000);
    [dcl, load] = await page.evaluate(() => { const n = performance.getEntriesByType('navigation')[0]; return n ? [Math.round(n.domContentLoadedEventEnd), Math.round(n.loadEventEnd)] : [-1, -1]; });
  } catch (e) { ok = false; }
  await Promise.all(pend);
  await ctx.close();
  return { url, mode, ok, blocked, requests: allowed + 1, bytesKB: Math.round(bytes / 1024), dcl, load, lastFinish };
}

const browser = await chromium.launch({ args: ['--no-sandbox'] });
const out = [];
for (const url of sites) {
  for (let i = 0; i < runs; i++) {
    for (const mode of (i % 2 === 0 ? ['off', 'on'] : ['on', 'off'])) {
      const r = await once(browser, url, mode); out.push(r);
      console.log(url.padEnd(40), mode.padEnd(3), r.ok ? 'ok  ' : 'FAIL', 'req=' + String(r.requests).padStart(5), 'blocked=' + String(r.blocked).padStart(4), 'KB=' + String(r.bytesKB).padStart(6), 'dcl=' + r.dcl, 'load=' + r.load, 'lastFinish=' + r.lastFinish);
    }
  }
}
await browser.close();
writeFileSync(outPath, JSON.stringify(out));
