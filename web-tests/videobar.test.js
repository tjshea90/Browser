'use strict';
// VC1 / LP1 (owner message 44, 2026-11-15; research/VIDEO_CONTROLS.md): "I played a video on finance.yahoo.com in Kestrel and the player had no controls at all ... Anywhere I can start a video in the browser,
// I should be able to control it ... the download options should come up for videos when I long press them instead of the floating notification panel".
// vfind.js + videobar.js (+ content.js for the long press) in real headless Chromium with a touch screen against a fake extension API (videobar-lib.js). The Yahoo player is rebuilt from what a probe of the
// live page showed: a web component (open shadow root) with a pointer-events-less control layer whose bar is 5 px / 0 px tall while an ad plays.
// Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { launch, close, open, msgs, msgsAll, barInfo, tapAt, partCentre, filmTime, touch, VIDEO, LEN } = require('./videobar-lib');

before(launch);
after(close);

const CENTRE = [220, 220];                       // the middle of the 400 x 240 picture at (20, 100)
const wait = (page, ms) => page.waitForTimeout(ms);

/** The Yahoo-like player: a host div with an OPEN shadow root: the film, and a control layer without pointer events whose bar is 5 px tall with a play button of size 0 (ad phase). */
const SHADOW = (controls) => `<div id="host" style="position:absolute;left:20px;top:100px;width:400px;height:240px"></div><script>
  var root = document.getElementById('host').attachShadow({ mode: 'open' });
  root.innerHTML = '<div style="position:relative;width:400px;height:240px;background:#000"><video id="v" src="https://cdn.example/film.wav" preload="auto" muted style="width:400px;height:240px"></video>'
    + '<div class="shaka-controls-container" style="position:absolute;left:0;top:0;right:0;bottom:0;pointer-events:none">${controls || ''}</div></div>';
  window.__deepV = function () { return root.getElementById('v'); };
</script>`;
const ZERO_BAR = '<div class="shaka-bottom-controls" style="position:absolute;left:0;right:0;bottom:0;height:5px;pointer-events:auto"><button class="shaka-play-button" aria-label="Play" style="display:none;width:0;height:0"></button></div>';
/** The page's own bar, visible: a play button and a seek slider over the bottom of the picture. */
const OWN_BAR = '<div style="position:absolute;left:20px;top:300px;width:400px;height:40px"><button class="player-play-button" aria-label="Play" style="position:absolute;left:0;top:0;width:36px;height:36px"></button><div class="seek-bar" role="slider" aria-label="Seek" aria-valuemin="0" aria-valuemax="60" aria-valuenow="0" tabindex="0" style="position:absolute;left:44px;top:12px;width:340px;height:12px;background:#888"></div></div>';

test('VC1: a video with no controls: a tap brings up Kestrel\'s bar over the bottom of the picture, and the app is told once', async () => {
  const page = await open(VIDEO());
  assert.equal(await barInfo(page), null, 'no bar before the tap');
  await tapAt(page, ...CENTRE); await wait(page, 350);
  const b = await barInfo(page);
  assert.ok(b, 'the bar is there');
  assert.ok(b.width >= 200 && b.height === 44, 'size ' + b.width + 'x' + b.height);
  assert.ok(b.top > 100 + 240 - 80 && b.top + b.height <= 100 + 240, 'over the bottom of the picture: top ' + b.top);
  assert.equal(b.host, 'html', 'on the document element');
  const shown = await msgs(page, 'vbk');
  assert.deepEqual(shown.map((m) => m.e), ['shown']);
  await page.context().close();
});

test('VC1: the video\'s own controls attribute, or a visible bar of the page, means no bar of ours (and the app counts "the page had its own")', async () => {
  let page = await open(VIDEO('controls'));
  await tapAt(page, ...CENTRE); await wait(page, 350);
  assert.equal(await barInfo(page), null, 'native controls');
  assert.deepEqual((await msgs(page, 'vbk')).map((m) => m.e), ['own']);
  await page.context().close();
  page = await open(VIDEO() + OWN_BAR);
  await tapAt(page, 220, 160); await wait(page, 350);
  assert.equal(await barInfo(page), null, 'a play button and a slider of the page over the picture');
  assert.deepEqual((await msgs(page, 'vbk')).map((m) => m.e), ['own']);
  await page.context().close();
});

test('VC1: controls that are transparent, hidden or of size zero are no controls (the Yahoo ad phase): the bar shows', async () => {
  let page = await open(VIDEO() + OWN_BAR.replace('left:20px;top:300px', 'left:20px;top:300px;opacity:0'));
  await tapAt(page, 220, 160); await wait(page, 350);
  assert.ok(await barInfo(page), 'opacity 0');
  await page.context().close();
  page = await open(VIDEO() + OWN_BAR.replace('left:20px;top:300px', 'left:20px;top:300px;display:none'));
  await tapAt(page, 220, 160); await wait(page, 350);
  assert.ok(await barInfo(page), 'display none');
  await page.context().close();
});

test('VC1: Settings "Always" shows the bar although the page has controls; "Off" shows nothing and sends nothing', async () => {
  let page = await open(VIDEO() + OWN_BAR, { cfg: { vba: true } });
  await tapAt(page, 220, 160); await wait(page, 350);
  assert.ok(await barInfo(page), 'always');
  await page.context().close();
  page = await open(VIDEO(), { cfg: { vb: false } });
  await tapAt(page, ...CENTRE); await wait(page, 350);
  assert.equal(await barInfo(page), null, 'off');
  assert.deepEqual(await msgs(page, 'vbk'), [], 'nothing reported');
  await page.context().close();
});

test('VC1 (the owner\'s page): a video inside a web component\'s shadow root, with an invisible control layer: a tap on the player brings up the bar for THAT video', async () => {
  const page = await open(SHADOW(ZERO_BAR));
  assert.equal(await page.evaluate(() => document.querySelectorAll('video').length), 0, 'the document itself sees no video');
  await tapAt(page, ...CENTRE); await wait(page, 350);
  const b = await barInfo(page);
  assert.ok(b, 'the bar is there');
  assert.equal(await page.evaluate(() => window.KestrelVB.dbg().v === window.__deepV()), true, 'it controls the video inside the shadow root');
  const play = await partCentre(page, 'play');
  await tapAt(page, play.x, play.y); await wait(page, 300);
  assert.equal((await barInfo(page)).paused, false, 'the play button plays it');
  await page.context().close();
});

test('VC1: a video under a layer that takes the taps (pointer-events: none on the film) also gets the bar', async () => {
  const page = await open(VIDEO().replace('style="', 'style="pointer-events:none;') + '<div style="position:absolute;left:20px;top:100px;width:400px;height:240px"></div>');
  await tapAt(page, ...CENTRE); await wait(page, 350);
  assert.ok(await barInfo(page));
  await page.context().close();
});

test('VC1: the bar works: play / pause, mute, a tap on the track jumps there, a drag follows the finger, the time labels follow', async () => {
  const page = await open(VIDEO());
  await tapAt(page, ...CENTRE); await wait(page, 350);
  let play = await partCentre(page, 'play');
  await tapAt(page, play.x, play.y); await wait(page, 400);
  assert.equal((await barInfo(page)).paused, false, 'plays');
  play = await partCentre(page, 'play');
  await tapAt(page, play.x, play.y); await wait(page, 300);
  assert.equal((await barInfo(page)).paused, true, 'pauses');
  const mute = await partCentre(page, 'mute');
  await tapAt(page, mute.x, mute.y); await wait(page, 200);
  assert.equal((await barInfo(page)).muted, false, 'unmuted');
  const tr = await partCentre(page, 'track');
  await tapAt(page, tr.left + tr.width * 0.5, tr.y); await wait(page, 400);
  assert.ok(Math.abs(await filmTime(page) - LEN * 0.5) < 3, 'jumped to the middle, at ' + await filmTime(page));
  const b0 = await barInfo(page);
  assert.equal(b0.dur, '1:00', 'the length shown: ' + b0.dur);
  await touch(page, tr.left + tr.width * 0.5, tr.y, { toX: tr.left + tr.width * 0.25, toY: tr.y }); await wait(page, 400);
  assert.ok(Math.abs(await filmTime(page) - LEN * 0.25) < 3, 'the drag ended at a quarter, at ' + await filmTime(page));
  assert.deepEqual([...new Set((await msgs(page, 'vbk')).filter((m) => m.e === 'act').map((m) => m.a))].sort(), ['mute', 'pause', 'play', 'seek']);
  await page.context().close();
});

test('VC1: the bar goes away by itself after a few seconds, a slide (a scroll) is no tap, and a thumbnail-sized video never gets one', async () => {
  const page = await open(VIDEO());
  await touch(page, 220, 260, { toX: 220, toY: 180 }); await wait(page, 350);
  assert.equal(await barInfo(page), null, 'a slide is not a tap');
  await tapAt(page, ...CENTRE); await wait(page, 350);
  assert.ok(await barInfo(page));
  await wait(page, 1500);
  assert.equal(await barInfo(page), null, 'hidden again (a paused film keeps it 1.7 s in the test, 8 s in the app)');
  await page.evaluate(() => { const v = document.getElementById('v'); v.style.width = '60px'; v.style.height = '34px'; });
  await tapAt(page, 40, 117); await wait(page, 350);
  assert.equal(await barInfo(page), null, 'a thumbnail');
  await page.context().close();
});

test('VC1: a FRAME laid over the player (an ad overlay takes every touch): the frame relays the tap, the page above finds the video by the frame\'s size and shows the bar', async () => {
  const page = await open(SHADOW(ZERO_BAR) + '<iframe id="ad" srcdoc="<body style=\'margin:0;background:transparent\'></body>" style="position:absolute;left:20px;top:100px;width:400px;height:210px;border:0"></iframe>');
  await page.waitForTimeout(500);
  await tapAt(page, 220, 200); await wait(page, 450);
  assert.ok(await barInfo(page), 'the bar is there although the tap went into the frame');
  assert.equal(await page.evaluate(() => window.KestrelVB.dbg().v === window.__deepV()), true);
  await page.context().close();
});

test('LP1: a long press on a video inside a shadow root, or under a pointer-events-less layer, is the video menu and never open space', async () => {
  const page = await open(SHADOW(ZERO_BAR) + VIDEO().replace('id="v"', 'id="v2"').replace('left:20px;top:100px', 'left:20px;top:400px').replace('style="', 'style="pointer-events:none;') + '<div style="position:absolute;left:20px;top:400px;width:400px;height:240px"></div>', { content: true });
  await page.mouse.click(220, 220, { button: 'right' }); await wait(page, 350);
  await page.mouse.click(220, 520, { button: 'right' }); await wait(page, 350);
  const pops = await msgs(page, 'pop');
  assert.equal(pops.length, 2, 'two video menus: ' + JSON.stringify(pops));
  assert.ok(pops.every((m) => m.d === 'vmenu|https://cdn.example/film.wav'));
  assert.deepEqual(await msgs(page, 'os'), [], 'no open space');
  await page.mouse.click(250, 700, { button: 'right' }); await wait(page, 350);
  assert.equal((await msgs(page, 'os')).length, 1, 'a blank spot still opens the panel');
  await page.context().close();
});

test('LP1: a long press inside a FRAME that lies over a video: the top frame says it is a video: the video menu (sent by the frame), and the frame does not report open space', async () => {
  const page = await open(SHADOW(ZERO_BAR) + '<iframe id="ad" srcdoc="<body style=\'margin:0;background:transparent\'></body>" style="position:absolute;left:20px;top:100px;width:400px;height:210px;border:0"></iframe>', { content: true });
  await page.waitForTimeout(500);
  await page.mouse.click(220, 200, { button: 'right' }); await wait(page, 700);
  const pops = await msgsAll(page, 'pop');
  assert.equal(pops.length, 1, 'the video menu: ' + JSON.stringify(pops));
  assert.equal(pops[0].d, 'vmenu|https://cdn.example/film.wav');
  const inFrame = await page.frames()[1].evaluate(() => window.__msgs.filter((m) => m.k === 'os'));
  assert.deepEqual(inFrame, [], 'the frame sent no open-space message');
  // a frame that covers no video still reports open space
  await page.evaluate(() => { const f = document.getElementById('ad'); f.style.top = '500px'; });
  await wait(page, 200);
  await page.mouse.click(220, 560, { button: 'right' }); await wait(page, 900);
  assert.equal((await page.frames()[1].evaluate(() => window.__msgs.filter((m) => m.k === 'os'))).length, 1);
  await page.context().close();
});

test('VC1: full screen: a bare film gets the engine\'s own controls for as long as it lasts; a film in a box goes full screen as the box, with Kestrel\'s bar inside it', async () => {
  let page = await open(VIDEO());
  await tapAt(page, ...CENTRE); await wait(page, 350);
  let full = await partCentre(page, 'full');
  await tapAt(page, full.x, full.y); await wait(page, 700);
  assert.deepEqual(await page.evaluate(() => ({ fs: !!document.fullscreenElement, ctl: document.getElementById('v').controls })), { fs: true, ctl: true });
  await page.evaluate(() => document.exitFullscreen()); await wait(page, 600);
  assert.equal(await page.evaluate(() => document.getElementById('v').controls), false, 'back to what the page had');
  await page.context().close();
  page = await open('<div id="box" style="position:absolute;left:20px;top:100px;width:400px;height:240px"><video id="v" src="https://cdn.example/film.wav" preload="auto" muted style="width:400px;height:240px"></video></div>');
  await tapAt(page, ...CENTRE); await wait(page, 350);
  full = await partCentre(page, 'full');
  await tapAt(page, full.x, full.y); await wait(page, 800);
  assert.equal(await page.evaluate(() => document.fullscreenElement && document.fullscreenElement.id), 'box');
  assert.equal((await barInfo(page)).host, 'other', 'the bar moved into the full-screen box');
  await page.context().close();
});

test('VC1: a tap on a film whose own controls were showing, and which hide on that very tap (YouTube), does not bring up Kestrel\'s bar as they fade', async () => {
  const page = await open(VIDEO() + OWN_BAR);
  await page.evaluate(() => { document.addEventListener('pointerup', () => { const b = document.querySelector('.seek-bar').parentNode; b.style.opacity = b.style.opacity === '0' ? '1' : '0'; }, true); });
  await wait(page, 1500);                                           // the app's answer is there (it is asked once the page is idle)
  await tapAt(page, 220, 160); await wait(page, 400);               // the page's bar was showing and fades out
  assert.equal(await page.evaluate(() => document.querySelector('.seek-bar').parentNode.style.opacity), '0', 'the page hid its bar');
  assert.equal(await barInfo(page), null, 'no bar of ours');
  await tapAt(page, 220, 160); await wait(page, 400);               // the page's bar comes back
  assert.equal(await barInfo(page), null);
  assert.deepEqual((await msgs(page, 'vbk')).map((m) => m.e), ['own', 'own']);
  await page.context().close();
});

test('LAB2: the lab-only page-health probe counts text, pictures (and broken ones), frames and videos once the page has loaded, and sends numbers only', async () => {
  const page = await open(VIDEO() + '<p>Hello, this page has some text in it.</p><img id="ok" src="data:image/gif;base64,R0lGODlhAQABAAAAACw=" width="40" height="40"><img id="bad" src="https://cdn.example/missing.png" width="40" height="40"><iframe src="about:blank" width="50" height="50"></iframe>');
  await page.evaluate(() => window.KestrelVF.probe());
  await wait(page, 3200);
  const m = (await msgs(page, 'probe'))[0];
  assert.ok(m, 'one probe message');
  assert.ok(m.t >= 30 && m.im === 2 && m.ib >= 1 && m.fr === 1 && m.vd === 1 && m.nd > 5 && m.hh > 100 && m.rd === 'complete', JSON.stringify(m));
  assert.deepEqual(Object.keys(m).sort(), ['fr', 'h', 'hh', 'ib', 'im', 'iv', 'iw', 'k', 'nd', 'rd', 'rs', 'sc', 't', 'vd', 'vp', 'ww'].sort(), 'numbers and a host name only');
  await page.evaluate(() => window.KestrelVF.probe());
  await wait(page, 300);
  assert.equal((await msgs(page, 'probe')).length, 1, 'only once per page');
  await page.context().close();
});

// ---------------------------------------------------------------------------------------------------------------- the second independent review (RW1 .. RW11, research/REVIEW_ALPHA48.md addendum)
const AD_FRAME = (top) => `<iframe class="ad" srcdoc="<body style='margin:0;background:transparent'></body>" style="position:absolute;left:20px;top:${top}px;width:400px;height:210px;border:0"></iframe>`;

test('RW1: a decorative film under a link, a button or a picture: the link\'s long press and the button\'s tap are THEIRS (no video menu, no bar); a bare spot of the layer still is the film\'s', async () => {
  const DECOR = '<div style="position:absolute;left:20px;top:100px;width:400px;height:240px"><a id="lnk" href="https://other.example/x" style="position:absolute;left:20px;top:20px;width:150px;height:40px;background:#ccc">Read the story</a>'
    + '<button id="menu" style="position:absolute;left:200px;top:20px;width:100px;height:40px">Menu</button></div>';
  const page = await open(VIDEO().replace('style="', 'style="pointer-events:none;') + DECOR, { content: true });
  await page.mouse.click(115, 140, { button: 'right' }); await wait(page, 350);     // the link
  assert.deepEqual(await msgs(page, 'pop'), [], 'a long press on the link gave no video menu');
  assert.deepEqual(await msgs(page, 'os'), [], 'and no panel either: the link has its own menu');
  await tapAt(page, 270, 140); await wait(page, 350);                              // the "Menu" button
  assert.equal(await barInfo(page), null, 'a tap on a button of the page is not a tap on the film');
  await wait(page, 1100);                                                          // (a tap within a second of a long press is that press's release: RW5)
  await tapAt(page, 300, 300); await wait(page, 350);                              // an empty spot of the layer over the film
  assert.ok(await barInfo(page), 'a bare spot of the layer still brings up the bar');
  await page.mouse.click(300, 200, { button: 'right' }); await wait(page, 350);   // (above the bar, which covers the bottom of the picture)
  assert.equal((await msgs(page, 'pop')).filter((m) => m.d.startsWith('vmenu|')).length, 1, 'and its long press is the video menu');
  await page.context().close();
});

test('RW2: a full-screen box that is a web component\'s host: Kestrel\'s bar goes into its shadow tree and is really drawn (it was added to the host\'s light DOM and had no size)', async () => {
  const page = await open(SHADOW(ZERO_BAR));
  await tapAt(page, ...CENTRE); await wait(page, 350);
  const full = await partCentre(page, 'full');
  await tapAt(page, full.x, full.y); await wait(page, 900);
  assert.equal(await page.evaluate(() => document.fullscreenElement && document.fullscreenElement.id), 'host', 'the host went full screen');
  const b = await barInfo(page);
  assert.ok(b, 'the bar is still there');
  assert.equal(b.host, 'other', 'in the shadow tree');
  assert.ok(b.width >= 200 && b.height === 44, 'drawn with a size: ' + b.width + 'x' + b.height);
  await page.context().close();
});

test('RW3: the page\'s own controls are found next to the film, not among the first 1500 elements of the document (1700 links in front of a player with its own buttons)', async () => {
  const LINKS = Array.from({ length: 1700 }, (_, i) => `<a href="#l${i}">l${i}</a>`).join(' ');
  const PLAYER = '<div id="box" style="position:absolute;left:0;top:0;width:500px;height:420px">' + VIDEO() + OWN_BAR + '</div>';
  const page = await open(LINKS + PLAYER);
  await tapAt(page, 220, 160); await wait(page, 350);
  assert.equal(await barInfo(page), null, 'no bar of ours over the page\'s own controls');
  assert.deepEqual((await msgs(page, 'vbk')).map((m) => m.e), ['own']);
  await page.context().close();
});

test('RW4: the panel and the Save sheet never both open: an answer that comes after the panel was told changes nothing', async () => {
  const page = await open(SHADOW(ZERO_BAR) + AD_FRAME(100), { content: true });
  await page.waitForTimeout(500);
  await page.evaluate(() => { window.__ftDelay = 1300; });                         // a busy page: the top frame answers after 1.3 s
  await page.mouse.click(220, 200, { button: 'right' }); await wait(page, 2200);
  assert.equal((await msgsAll(page, 'os')).length, 1, 'the panel was told after 700 ms of silence');
  assert.deepEqual(await msgsAll(page, 'pop'), [], 'and the late "it is a video" answer sent no menu');
  await page.context().close();
});

test('RW5: a press released after the engine\'s long press is no tap (the bar must not come up behind the video menu), a quick tap right after one is none either', async () => {
  let page = await open(VIDEO());
  await touch(page, ...CENTRE, { holdMs: 520 }); await wait(page, 350);
  assert.equal(await barInfo(page), null, 'a 520 ms press is no tap');
  await tapAt(page, ...CENTRE); await wait(page, 350);
  assert.ok(await barInfo(page), 'a quick tap is');
  await page.context().close();
  page = await open(VIDEO(), { content: true });
  await page.mouse.click(...CENTRE, { button: 'right' });                          // the engine's long press: the video menu
  await tapAt(page, ...CENTRE); await wait(page, 350);
  assert.equal(await barInfo(page), null, 'the release of that press is no tap');
  await wait(page, 1100);
  await tapAt(page, ...CENTRE); await wait(page, 350);
  assert.ok(await barInfo(page), 'a second later a tap is a tap again');
  await page.context().close();
});

test('RW6: two frames of one size: nobody can say which one was touched: no bar, no video menu (the panel instead)', async () => {
  const page = await open(SHADOW(ZERO_BAR) + AD_FRAME(100) + AD_FRAME(500), { content: true });
  await page.waitForTimeout(500);
  await tapAt(page, 220, 560); await wait(page, 450);
  assert.equal(await barInfo(page), null, 'a tap in the unrelated frame gives the other frame\'s film no bar');
  await page.mouse.click(220, 560, { button: 'right' }); await wait(page, 900);
  assert.deepEqual((await msgsAll(page, 'pop')).filter((m) => m.d.startsWith('vmenu|')), [], 'no video menu');
  assert.equal((await msgsAll(page, 'os')).length, 1, 'the panel');
  await page.context().close();
});

test('RW7: a page cannot switch the menus off by naming a layer "the bar", and a script\'s own pointer events cannot cancel the person\'s taps', async () => {
  let page = await open(VIDEO() + '<div data-kestrel-bar="1" style="position:absolute;left:20px;top:100px;width:400px;height:240px"></div>', { content: true });
  await page.mouse.click(...CENTRE, { button: 'right' }); await wait(page, 350);
  assert.equal((await msgs(page, 'pop')).length, 1, 'the video menu opens although the layer wears the old marker');
  await page.context().close();
  page = await open(VIDEO());
  const c = await page.context().newCDPSession(page);
  await c.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [{ x: CENTRE[0], y: CENTRE[1] }] });
  await page.evaluate(() => { window.dispatchEvent(new PointerEvent('pointercancel', { pointerId: 1 })); window.dispatchEvent(new PointerEvent('pointerdown', { pointerId: 77, clientX: 5, clientY: 5 })); });
  await c.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
  await wait(page, 350);
  assert.ok(await barInfo(page), 'the real tap still counted');
  await page.context().close();
});

test('RW9: a tap on a second film while the first film\'s bar is up: the bar moves to the second', async () => {
  const page = await open(VIDEO() + VIDEO().replace('id="v"', 'id="v2"').replace('top:100px', 'top:400px'));
  await tapAt(page, ...CENTRE); await wait(page, 300);
  assert.equal(await page.evaluate(() => window.KestrelVB.dbg().v.id), 'v');
  await tapAt(page, 220, 520); await wait(page, 300);
  assert.equal(await page.evaluate(() => window.KestrelVB.dbg() && window.KestrelVB.dbg().v.id), 'v2');
  await page.context().close();
});

test('RW10: controls that were there at the first look and fade out before the second look: no bar of ours (the first verdict stands)', async () => {
  const page = await open(VIDEO() + OWN_BAR);
  await page.evaluate(() => { document.addEventListener('pointerup', () => { setTimeout(() => { document.querySelector('.seek-bar').parentNode.style.opacity = '0'; }, 250); }, true); });
  await wait(page, 1500);
  await tapAt(page, 220, 160); await wait(page, 900);
  assert.equal(await barInfo(page), null);
  assert.deepEqual((await msgs(page, 'vbk')).map((m) => m.e), ['own']);
  await page.context().close();
});

test('RW11: an answer of the app that did not come is not kept as "off": the next look asks again', async () => {
  const page = await open(VIDEO());
  await page.evaluate(() => { window.__guardFails = 1; });
  await tapAt(page, ...CENTRE); await wait(page, 350);
  assert.equal(await barInfo(page), null, 'no answer, no bar');
  await wait(page, 1500);                                                           // the prefetch asks again
  await tapAt(page, ...CENTRE); await wait(page, 350);
  assert.ok(await barInfo(page), 'the second answer arrived and was kept');
  await page.context().close();
});

test('FSC1: a page that takes a bare film full screen by itself gets the engine\'s own controls for as long as it lasts', async () => {
  const page = await open(VIDEO());
  await tapAt(page, ...CENTRE); await wait(page, 350);                              // (the tap is the page's user gesture; the bar knows the Settings answer now)
  await page.evaluate(() => document.getElementById('v').requestFullscreen()); await wait(page, 700);
  assert.deepEqual(await page.evaluate(() => ({ fs: !!document.fullscreenElement, ctl: document.getElementById('v').controls })), { fs: true, ctl: true });
  assert.equal(await barInfo(page), null, 'nothing of ours is drawn over a bare film');
  await page.evaluate(() => document.exitFullscreen()); await wait(page, 600);
  assert.equal(await page.evaluate(() => document.getElementById('v').controls), false, 'back to what the page had');
  await page.context().close();
});
