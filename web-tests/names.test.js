'use strict';
// NM1 (owner message 2026-11-18: "i downloaded ozark season 1 episode 1, but it named it just ozark. Then ... episode 2, it also named it just ozark"): names.js, which tells the app what the TOP page says about
// WHICH video is on (the show, the active episode of its list, the season, that episode's title), in real headless Chromium against pages built like the ones episode sites use (a list whose active item has a class,
// data attributes, a select for the season, JSON-LD). What a real site's markup is could not be read from the sandbox: these shapes are the common ones.
// Run: cd web-tests && npm test
const fs = require('node:fs');
const path = require('node:path');
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { launch, close, open } = require('./videobar-lib');

before(launch);
after(close);

const NAMES = fs.readFileSync(path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'names.js'), 'utf8');
const FAST = '<script>window.__kstNMcfg = { first: 50, second: 120, after1: 60, after2: 150 };</script>';

async function show(html, o) {
  const page = await open(FAST + html, Object.assign({ cfg: {}, wait: false }, o || {}));
  await page.addScriptTag({ content: NAMES });
  return page;
}
const collect = (page) => page.evaluate(() => window.KestrelNM.collect());
const sentNm = (page) => page.evaluate(() => window.__msgs.filter((m) => m && m.k === 'nm'));

const LIST = (act) => `<h1>Ozark</h1><select id="season"><option>Season 1</option><option selected>Season 2</option></select>
  <ul class="episodes"><li class="episode-item ${act === 1 ? 'active' : ''}"><a>Episode 1: Sugarwood</a></li>
  <li class="episode-item ${act === 2 ? 'active' : ''}"><a>Episode 2: Blue Cat</a></li><li class="episode-item ${act === 3 ? 'current' : ''}"><a>Episode 3: My Dripping Sleep</a></li></ul>`;

test('NM1: the active item of an episode list, the heading and the season select give show, season, episode and its title', async () => {
  const page = await show(LIST(2));
  assert.deepEqual(await collect(page), { sh: 'Ozark', s: 2, e: 2, et: 'Blue Cat' });
  await page.context().close();
});

test('NM1: another item becomes active (the person pressed episode 3): the next report names it, and only a change is sent', async () => {
  const page = await show(LIST(1));
  await page.waitForFunction(() => window.__msgs.some((m) => m && m.k === 'nm'), null, { timeout: 5000 });
  assert.equal((await sentNm(page))[0].e, 1);
  await page.evaluate(() => { document.querySelectorAll('.episode-item').forEach((li, i) => li.classList.toggle('active', i === 2)); });
  await page.mouse.click(10, 10);                                    // a real click: the script looks again 0.6 s and 2 s later
  await page.waitForFunction(() => window.__msgs.filter((m) => m && m.k === 'nm').length >= 2, null, { timeout: 5000 });
  const all = await sentNm(page);
  assert.equal(all[all.length - 1].e, 3); assert.equal(all[all.length - 1].et, 'My Dripping Sleep');
  const n = all.length;
  await page.waitForTimeout(500);
  assert.equal((await sentNm(page)).length, n, 'the same state is not sent again');
  await page.context().close();
});

test('NM1: data attributes and aria-current work too (data-season / data-episode, no words at all)', async () => {
  const page = await show('<h1>Better Call Saul</h1><div class="eps"><button data-season="1" data-episode="1">1</button><button data-season="1" data-episode="2" aria-current="true">2</button></div>');
  assert.deepEqual(await collect(page), { sh: 'Better Call Saul', s: 1, e: 2, et: '' });
  await page.context().close();
});

test('NM1: "S1 E2" and "1x02" style labels, and "2. Title" number-title items', async () => {
  const a = await show('<h1>Dark</h1><ul><li class="ep selected">S1:E4 Lies</li></ul>');
  assert.equal((await collect(a)).e, 4); assert.equal((await collect(a)).s, 1);
  await a.context().close();
  const b = await show('<h1>Show X</h1><ul class="episodes"><li class="active">7. The Reckoning</li></ul>');
  const r = await collect(b);
  assert.equal(r.e, 7); assert.equal(r.et, 'The Reckoning');
  await b.context().close();
});

test('NM1: JSON-LD of a TVEpisode names the show, season, episode and title when the page has no list', async () => {
  const ld = JSON.stringify({ '@context': 'https://schema.org', '@type': 'TVEpisode', name: 'Sugarwood', episodeNumber: 1, partOfSeason: { '@type': 'TVSeason', seasonNumber: 1 }, partOfSeries: { '@type': 'TVSeries', name: 'Ozark' } });
  const page = await show(`<script type="application/ld+json">${ld}</script><p>nothing else</p>`);
  assert.deepEqual(await collect(page), { sh: 'Ozark', s: 1, e: 1, et: 'Sugarwood' });
  await page.context().close();
});

test('NM1: a film page (no episode anywhere) sends nothing; a show heading alone is not enough to send an episode', async () => {
  const page = await show('<h1>Dune: Part Two</h1><p>A film.</p>');
  await page.waitForTimeout(400);
  assert.equal((await sentNm(page)).length, 1, 'the heading is told (the app ignores a name with no episode)');
  assert.equal((await sentNm(page))[0].e, 0);
  await page.context().close();
});

test('NM1: it runs in the top page only, never in a frame, and a page that says nothing sends nothing', async () => {
  const page = await show('<p>no heading, no list</p>');
  await page.waitForTimeout(400);
  assert.equal((await sentNm(page)).length, 0);
  await page.context().close();
});

// EM1i (2026-11-19): the REAL doraby title page, rendered in headless Chromium, builds its episode list like this (a javascript: link with data-episode_id, the number and the title in two spans, only
// spaces between them) and marks the shown episode with class "active". The first version of names.js named the episode but left its title empty.
const DORABY = (act) => `<center><h1 itemprop="name" class="name">Ozark </h1></center>
  <ul class="episodes">${[['Sugarwood', 1], ['Blue Cat', 2], ['My Dripping Sleep', 3]].map(([t, n]) => `<li><a href="javascript:void(0)" data-episode_id="${n}" onclick="infoEpisodio(69740,1,${n})" class="${act === n ? 'active' : ''}"><span class="num">Episode ${n}</span>&nbsp;&nbsp;<span>${t}</span></a></li>`).join('')}</ul>`;

test('EM1i: the real doraby episode list (number and title in two spans, no separator) gives the episode AND its title', async () => {
  const page = await show(DORABY(2));
  assert.deepEqual(await collect(page), { sh: 'Ozark', s: 1, e: 2, et: 'Blue Cat' });
  await page.context().close();
});

test('EM1i: filler after the episode number is never taken as a title', async () => {
  for (const label of ['Episode 2 of 10', 'Episode 2 (2017)', 'Episode 2 season 1', 'Episode 2 45 min']) {
    const page = await show(`<h1>Ozark</h1><ul class="episodes"><li class="episode-item active"><a>${label}</a></li></ul>`);
    const c = await collect(page);
    assert.equal(c.e, 2, label);
    assert.equal(c.et, '', label);
    await page.context().close();
  }
});
