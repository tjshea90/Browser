// "This site is broken in Kestrel": make a local plain-http copy of one page that the REAL Gecko build can open on the emulator (which cannot validate https certificates).
//   node tools/sitelab/mirror.js https://example.org/ <outdir> [skip-regex ...]      (needs Playwright, preinstalled globally in the sandbox; reads the page through the sandbox proxy)
//   python3 tools/sitelab/serve.py <outdir> 8001 [/_cdn/host.example,...]            then on the emulator open http://10.0.2.2:8001/
// Every file the page loads is saved (the page's own host at its own path, other hosts under /_cdn/<host>/), absolute and protocol-relative links are pointed at the copy, and the hosts named in
// skip-regex arguments (the ad / tracker hosts Kestrel blocks anyway) become an unreachable address. serve.py can make a host FAIL (404) to imitate an unreachable CDN. The copy is untrusted data:
// keep <outdir> OUTSIDE the repository.
const fs = require('fs'), path = require('path');
let chromium; try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }
const [url, out, ...skipArgs] = process.argv.slice(2);
if (!url || !out) { console.error('usage: node mirror.js <url> <outdir> [skip-regex ...]'); process.exit(2); }
const SKIP = skipArgs.map((s) => new RegExp(s));
const home = new URL(url).hostname;
const localPath = (u) => { const x = new URL(u); const p = decodeURIComponent(x.pathname); return x.hostname === home ? (p.endsWith('/') ? p + 'index.html' : p) : '/_cdn/' + x.hostname + p; };
function rewrite(text, hosts) {
  for (const h of hosts) {
    const to = h === home ? '/' : '/_cdn/' + h + '/';
    text = text.replace(new RegExp('https?://' + h.replace(/\./g, '\\.') + '/', 'g'), to).replace(new RegExp('(?<![:\\w])//' + h.replace(/\./g, '\\.') + '/', 'g'), to);
  }
  return text;
}
(async () => {
  const browser = await chromium.launch();
  const page = await (await browser.newContext({ viewport: { width: 600, height: 973 } })).newPage();
  const seen = new Map(); // local path -> { body, type }
  const hosts = new Set([home]);
  page.on('response', async (r) => {
    const u = r.url();
    if (r.status() !== 200 || !/^https:/.test(u) || SKIP.some((s) => s.test(u))) return;
    try { seen.set(localPath(u), { body: await r.body(), type: r.headers()['content-type'] || '' }); hosts.add(new URL(u).hostname); } catch (e) { /* redirects, aborted */ }
  });
  await page.goto(url, { waitUntil: 'load', timeout: 60000 });
  await page.waitForTimeout(5000);
  await page.evaluate(async () => { for (let y = 0; y < document.body.scrollHeight; y += 500) { window.scrollTo(0, y); await new Promise((r) => setTimeout(r, 250)); } }); // lazy pictures
  await page.waitForTimeout(3000);
  for (const [lp, f] of seen) {
    const file = path.join(out, lp); fs.mkdirSync(path.dirname(file), { recursive: true });
    if (/css|javascript|html|json/.test(f.type) || /\.(css|js|html)$/.test(lp)) {
      let t = rewrite(f.body.toString('utf8'), hosts);
      for (const s of SKIP) t = t.replace(new RegExp('https?://[^"\'\\s)]*' + s.source + '[^"\'\\s)]*', 'g'), 'http://127.0.0.1:9/blocked');
      fs.writeFileSync(file, t);
    } else fs.writeFileSync(file, f.body);
  }
  console.log(seen.size + ' files from ' + hosts.size + ' hosts in ' + out);
  await browser.close();
})();
