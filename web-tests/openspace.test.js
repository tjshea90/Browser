'use strict';
// A long press on OPEN space (AY2, owner message 39: "make it so I can long press anywhere in the browser and it will show up under where I long pressed, but only if I am long pressing an open space. If I'm long
// pressing a link, give me the options for that link, not the navigation panel"). content.js (the isolated content script of the built-in extension) in real headless Chromium with a fake `browser.runtime`.
// The engine delivers a long press as a trusted `contextmenu` event on whatever is under the finger. Only when nothing there could have an action of its own, and the engine did not select a word, the page tells
// the app ({k:'os'}) that the press was on open space, and the app puts the mini panel under the finger. (Headless Chromium does not select a word on a right click: the test page does what the engine does.)
// Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const CONTENT = fs.readFileSync(path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'content.js'), 'utf8');
let browser;
before(async () => { browser = await chromium.launch(); });
after(async () => { await browser.close(); });

/** A page at https://site.example/ with content.js running against a fake extension API that records every message. */
async function open(body, pre) {
  const context = await browser.newContext();
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>t</title></head><body style="margin:0">${body}</body></html>` }));
  const page = await context.newPage();
  await page.addInitScript(() => {
    window.__msgs = [];
    window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m); return Promise.resolve(undefined); } } };
  });
  if (pre) await page.addInitScript(pre);
  await page.addInitScript({ content: CONTENT });
  await page.goto('https://site.example/');
  return page;
}
const opens = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'os'));
/** Long presses (right clicks) at every point, then one wait: the page answers 200 ms after each. */
const press = async (page, points) => { for (const [x, y] of points) await page.mouse.click(x, y, { button: 'right' }); await page.waitForTimeout(350); };
/** What the engine does on a long press on text: it selects the word under the finger. */
const selectWord = () => { document.addEventListener('contextmenu', (e) => { const t = document.getElementById('t'); if (!t || !t.contains(e.target)) return; const r = document.createRange(); r.selectNodeContents(t); const s = window.getSelection(); s.removeAllRanges(); s.addRange(r); }); };

test('a long press on blank space is reported once, with the address of the page and nothing else', async () => {
  const page = await open(`<div style="height:600px"></div>`);
  await press(page, [[120, 200]]);
  assert.deepEqual(await opens(page), [{ k: 'os', p: 'https://site.example/' }]);
});

test('a link, a picture, a button, a form control and a video are not open space', async () => {
  const page = await open(`
    <a href="https://x.example/" style="position:absolute;left:0;top:0;width:100px;height:40px;display:block">link</a>
    <img src="data:image/gif;base64,R0lGODlhAQABAAAAACw=" style="position:absolute;left:0;top:50px;width:100px;height:40px">
    <button style="position:absolute;left:0;top:100px;width:100px;height:40px">b</button>
    <input style="position:absolute;left:0;top:150px;width:100px;height:40px">
    <textarea style="position:absolute;left:0;top:200px;width:100px;height:40px"></textarea>
    <select style="position:absolute;left:0;top:250px;width:100px;height:40px"><option>o</option></select>
    <video src="https://cdn.example/v.mp4" style="position:absolute;left:0;top:300px;width:100px;height:40px"></video>`);
  await press(page, [20, 70, 120, 170, 220, 270, 320].map((y) => [50, y]));
  assert.deepEqual(await opens(page), []);
});

test('widgets by role, an onclick, a tab stop, an editable region and a pointer cursor are not open space either', async () => {
  const page = await open(`
    <div role="button" style="position:absolute;left:0;top:0;width:100px;height:40px"></div>
    <div onclick="1" style="position:absolute;left:0;top:50px;width:100px;height:40px"></div>
    <div tabindex="0" style="position:absolute;left:0;top:100px;width:100px;height:40px"></div>
    <div contenteditable="true" style="position:absolute;left:0;top:150px;width:100px;height:40px"></div>
    <div style="position:absolute;left:0;top:200px;width:100px;height:40px;cursor:pointer"></div>
    <div role="textbox" style="position:absolute;left:0;top:250px;width:100px;height:40px"></div>`);
  await press(page, [20, 70, 120, 170, 220, 270].map((y) => [50, y]));
  assert.deepEqual(await opens(page), []);
});

test('a wrapper that fills the whole screen (a tab stop or an onclick on the page root) does not make every spot "not open space"', async () => {
  const page = await open(`<div tabindex="0" onclick="1" style="position:absolute;left:0;top:0;width:100%;height:100%"></div>`);
  await press(page, [[300, 300]]);
  assert.equal((await opens(page)).length, 1);
});

test('a transparent layer over a link does not make the link open space (the whole stack under the finger is looked at)', async () => {
  const page = await open(`<a href="https://x.example/" style="position:absolute;left:0;top:0;width:200px;height:100px;display:block">link</a><div style="position:absolute;left:0;top:0;width:200px;height:100px"></div>`);
  await press(page, [[50, 50]]);
  assert.deepEqual(await opens(page), []);
});

test('a link NEXT to blank space does not stop a press on the blank space', async () => {
  const page = await open(`<a href="https://x.example/" style="position:absolute;left:0;top:0;width:100px;height:40px;display:block">link</a><div style="height:600px"></div>`);
  await press(page, [[300, 300]]);
  assert.equal((await opens(page)).length, 1);
});

test('a press on text is the engine\'s (it selects the word), not open space; blank space below it is', async () => {
  const page = await open(`<p id="t" style="margin:0;padding:20px;width:300px">some words of a paragraph</p><div style="height:600px"></div>`, selectWord);
  await press(page, [[50, 30]]);
  assert.deepEqual(await opens(page), [], 'the word is selected: the press belongs to the selection');
  await page.evaluate(() => window.getSelection().removeAllRanges());
  await press(page, [[100, 400]]);
  assert.equal((await opens(page)).length, 1, 'blank space below the text is open space');
});

test('a selection that was there already and stays does not hide a press on blank space (only a NEW selection means the engine selected a word)', async () => {
  const page = await open(`<p id="t" style="margin:0;padding:20px;width:300px">some words of a paragraph</p><div style="height:600px"></div>`, selectWord);
  await press(page, [[50, 30]]);
  assert.deepEqual(await opens(page), [], 'the word is selected');
  await press(page, [[100, 400]]);
  assert.equal((await opens(page)).length, 1, 'the word is still selected, and a press on blank space below is still open space');
});

test('a selection that the engine CLEARS because the press is elsewhere (found on the emulator) does not hide the press on blank space', async () => {
  const clearOnBlank = () => { document.addEventListener('contextmenu', (e) => { const t = document.getElementById('t'); if (t && t.contains(e.target)) return; setTimeout(() => window.getSelection().removeAllRanges(), 50); }); };
  const page = await open(`<p id="t" style="margin:0;padding:20px;width:300px">some words of a paragraph</p><div style="height:600px"></div>`, [selectWord, clearOnBlank].map((f) => '(' + f.toString() + ')()').join(';'));
  await press(page, [[50, 30]]);
  assert.deepEqual(await opens(page), [], 'the word is selected');
  await press(page, [[100, 400]]);
  assert.equal((await opens(page)).length, 1, 'the blank press cleared that selection, and it is still open space');
});

test('a press on the text that is selected already is still the selection\'s, even though nothing changes', async () => {
  const page = await open(`<p id="t" style="margin:0;padding:20px;width:300px">some words of a paragraph</p><div style="height:600px"></div>`, selectWord);
  await press(page, [[50, 30]]);
  await press(page, [[60, 30]]);
  assert.deepEqual(await opens(page), []);
});

test('a context menu event a page makes up itself is never believed', async () => {
  const page = await open(`<div style="height:600px"></div>`);
  await page.evaluate(() => document.body.dispatchEvent(new MouseEvent('contextmenu', { bubbles: true, cancelable: true, clientX: 50, clientY: 50 })));
  await page.waitForTimeout(350);
  assert.deepEqual(await opens(page), []);
});

test('the page\'s own context menu handler still runs, and an app that does not answer never breaks the page', async () => {
  const page = await open(`<div style="height:600px" oncontextmenu="window.__own=1"></div>`, () => { window.browser.runtime.sendMessage = () => Promise.reject(new Error('no app')); });
  await press(page, [[50, 50]]);
  assert.equal(await page.evaluate(() => window.__own), 1);
});
