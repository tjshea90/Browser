'use strict';
// hlscap.js (ANS52h): the pure rewrite of an HLS master playlist that takes the rungs above the picture size cap out. Plain node, no browser. Run: cd web-tests && npm test
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { rewriteMaster, longSide } = require('../geckoapp/src/main/assets/kestrel-shields/hlscap.js');

// the shape of a real ladder (Brightcove / Boltdns style): audio group, five rungs, picture strips
const MASTER = [
  '#EXTM3U', '#EXT-X-VERSION:5', '#EXT-X-INDEPENDENT-SEGMENTS',
  '#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aud",NAME="English",DEFAULT=YES,AUTOSELECT=YES,URI="audio/en.m3u8"',
  '#EXT-X-STREAM-INF:BANDWIDTH=6000000,AVERAGE-BANDWIDTH=5200000,RESOLUTION=1920x1080,CODECS="avc1.640028,mp4a.40.2",FRAME-RATE=29.970,AUDIO="aud"', 'v1080.m3u8?x=1',
  '#EXT-X-STREAM-INF:BANDWIDTH=3500000,AVERAGE-BANDWIDTH=3000000,RESOLUTION=1280x720,CODECS="avc1.64001f,mp4a.40.2",FRAME-RATE=29.970,AUDIO="aud"', 'v720.m3u8?x=1',
  '#EXT-X-STREAM-INF:BANDWIDTH=2000000,RESOLUTION=960x540,CODECS="avc1.4d401f,mp4a.40.2",AUDIO="aud"', 'v540.m3u8?x=1',
  '#EXT-X-STREAM-INF:BANDWIDTH=1000000,RESOLUTION=640x360,CODECS="avc1.4d401e,mp4a.40.2",AUDIO="aud"', 'v360.m3u8?x=1',
  '#EXT-X-STREAM-INF:BANDWIDTH=140000,CODECS="mp4a.40.2",AUDIO="aud"', 'audio-only.m3u8',
  '#EXT-X-I-FRAME-STREAM-INF:BANDWIDTH=600000,RESOLUTION=1920x1080,CODECS="avc1.640028",URI="i1080.m3u8"',
  '#EXT-X-I-FRAME-STREAM-INF:BANDWIDTH=200000,RESOLUTION=640x360,CODECS="avc1.4d401e",URI="i360.m3u8"',
  '',
];
const text = (lines, eol) => lines.join(eol || '\n');
const without = (lines, drop) => lines.filter((l) => !drop.some((d) => l === d || l.indexOf(d) === 0));

test('Automatic on the tablet (1313): the 1080p rung and its picture strip go, everything else stays byte for byte', () => {
  const r = rewriteMaster(text(MASTER), 1313);
  assert.deepEqual([r.dropped, r.kept, r.frames], [1, 3, 1]);
  const expect = MASTER.filter((l, i) => !(l.indexOf('RESOLUTION=1920x1080') > 0) && l !== 'v1080.m3u8?x=1');
  assert.equal(r.text, text(expect));
  assert.ok(r.text.indexOf('audio-only.m3u8') > 0, 'a rung without a picture stays');
  assert.ok(r.text.indexOf('#EXT-X-MEDIA:TYPE=AUDIO') > 0, 'the audio group stays');
  assert.ok(r.text.endsWith('\n') || r.text.endsWith('i360.m3u8"'), 'the end of the text is as it was');
});

test('the named caps: 720p keeps 1280 wide, 480p keeps only 640 and under, 1080p keeps everything', () => {
  const kept = (cap) => { const r = rewriteMaster(text(MASTER), cap); return r ? r.text.split('\n').filter((l) => /^v\d+\.m3u8/.test(l)) : ['(unchanged)']; };
  assert.deepEqual(kept(1280), ['v720.m3u8?x=1', 'v540.m3u8?x=1', 'v360.m3u8?x=1']);
  assert.deepEqual(kept(854), ['v360.m3u8?x=1'], '960 is more than 2 % above 854');
  assert.deepEqual(kept(1920), ['(unchanged)']);
  assert.equal(rewriteMaster(text(MASTER), 1920), null);
  assert.deepEqual(kept(870), ['v540.m3u8?x=1', 'v360.m3u8?x=1'], '960 is within the 2 % slack of 940');
});

test('every rung with a picture above the cap: the smallest one stays, so the film still plays', () => {
  const big = ['#EXTM3U', '#EXT-X-STREAM-INF:BANDWIDTH=6000000,RESOLUTION=1920x1080', 'a.m3u8', '#EXT-X-STREAM-INF:BANDWIDTH=3500000,RESOLUTION=1280x720', 'b.m3u8', '#EXT-X-STREAM-INF:BANDWIDTH=300000,CODECS="mp4a.40.2"', 'audio.m3u8', ''];
  const r = rewriteMaster(text(big), 640);
  assert.deepEqual([r.dropped, r.kept], [1, 1]);
  assert.equal(r.text, text(without(big, ['#EXT-X-STREAM-INF:BANDWIDTH=6000000', 'a.m3u8'])));
  // one rung only: nothing to take out
  assert.equal(rewriteMaster(text(['#EXTM3U', '#EXT-X-STREAM-INF:BANDWIDTH=6000000,RESOLUTION=1920x1080', 'only.m3u8', '']), 640), null);
  // rungs of equal size above the cap: one of them stays
  const twin = ['#EXTM3U', '#EXT-X-STREAM-INF:BANDWIDTH=6000000,RESOLUTION=1920x1080', 'a.m3u8', '#EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080', 'b.m3u8'];
  assert.equal(rewriteMaster(text(twin), 640), null, 'both are the smallest: nothing dropped except what is bigger');
});

test('a portrait film is judged by its longer side, a rung with no picture size is never dropped', () => {
  const port = ['#EXTM3U', '#EXT-X-STREAM-INF:BANDWIDTH=6000000,RESOLUTION=1080x1920', 'p1080.m3u8', '#EXT-X-STREAM-INF:BANDWIDTH=3000000,RESOLUTION=720x1280', 'p720.m3u8', '#EXT-X-STREAM-INF:BANDWIDTH=900000', 'nosize.m3u8', ''];
  const r = rewriteMaster(text(port), 1313);
  assert.equal(r.text, text(['#EXTM3U', '#EXT-X-STREAM-INF:BANDWIDTH=3000000,RESOLUTION=720x1280', 'p720.m3u8', '#EXT-X-STREAM-INF:BANDWIDTH=900000', 'nosize.m3u8', '']));
  assert.equal(longSide('#EXT-X-STREAM-INF:RESOLUTION=1080x1920'), 1920);
  assert.equal(longSide('#EXT-X-STREAM-INF:BANDWIDTH=1,RESOLUTION=640x360'), 640);
  assert.equal(longSide('#EXT-X-STREAM-INF:BANDWIDTH=1'), 0);
  assert.equal(longSide('#EXT-X-STREAM-INF:NAME="RESOLUTION=1x1"'), 0, 'inside a quoted value is no attribute');
});

test('Windows line endings and a missing final newline come out as they went in', () => {
  const crlf = rewriteMaster(text(MASTER, '\r\n'), 1313);
  assert.ok(crlf.text.indexOf('\r\n') > 0 && !/[^\r]\n/.test(crlf.text), 'every line break is still CRLF');
  const noEnd = MASTER.slice(0, -1);
  const r = rewriteMaster(text(noEnd), 1313);
  assert.ok(!r.text.endsWith('\n'));
});

test('not a master playlist, no cap, not text: null, the original goes through', () => {
  const media = ['#EXTM3U', '#EXT-X-VERSION:3', '#EXT-X-TARGETDURATION:6', '#EXTINF:6.0,', 'seg1.ts', '#EXTINF:6.0,', 'seg2.ts', '#EXT-X-ENDLIST', ''];
  assert.equal(rewriteMaster(text(media), 640), null);
  assert.equal(rewriteMaster('', 640), null);
  assert.equal(rewriteMaster(text(MASTER), 0), null);
  assert.equal(rewriteMaster(text(MASTER), -5), null);
  assert.equal(rewriteMaster(text(MASTER), NaN), null);
  assert.equal(rewriteMaster(null, 640), null);
  assert.equal(rewriteMaster(undefined, 640), null);
  assert.equal(rewriteMaster(12, 640), null);
  assert.equal(rewriteMaster('{"not":"a playlist"}', 640), null);
});

test('a stream tag with no address right behind it is left alone, and so is what comes after', () => {
  const odd = ['#EXTM3U', '#EXT-X-STREAM-INF:BANDWIDTH=6000000,RESOLUTION=1920x1080', '# a comment where the address should be', 'late.m3u8', '#EXT-X-STREAM-INF:BANDWIDTH=3000000,RESOLUTION=1280x720', 'ok720.m3u8', '#EXT-X-STREAM-INF:BANDWIDTH=9000000,RESOLUTION=3840x2160', ''];
  const r = rewriteMaster(text(odd), 1313);
  assert.equal(r, null, 'the 1080 tag has no address line (a comment follows) and the 4K tag is the last line: neither can be taken out cleanly');
});

test('rewriting twice changes nothing more; a big ladder is fast', () => {
  const once = rewriteMaster(text(MASTER), 1313);
  assert.equal(rewriteMaster(once.text, 1313), null);
  const ladder = ['#EXTM3U'];
  for (let i = 0; i < 3000; i++) ladder.push('#EXT-X-STREAM-INF:BANDWIDTH=' + (1000 + i) + ',RESOLUTION=' + (i % 2 ? '1920x1080' : '640x360'), 'r' + i + '.m3u8');
  const t0 = Date.now();
  const r = rewriteMaster(text(ladder), 1313);
  assert.equal(r.dropped, 1500);
  assert.ok(Date.now() - t0 < 500, 'took ' + (Date.now() - t0) + ' ms');
});
