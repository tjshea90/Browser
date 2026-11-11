'use strict';
// Compares the JS engine with the Kotlin engine on the REAL bundled lists. Not part of `npm test` (needs the files written by the Kotlin side):
//   KESTREL_REAL_DIFF=/some/empty/dir ./gradlew :core:test --tests '*RealListsExtImageTest*'
//   node web-tests/real-diff.js /some/empty/dir
const fs = require('node:fs');
const path = require('node:path');
const E = require('../geckoapp/src/main/assets/kestrel-shields/engine.js');

const dir = process.argv[2];
if (!dir) { console.error('usage: node real-diff.js <dir written by RealListsExtImageTest>'); process.exit(2); }
const b = fs.readFileSync(path.join(dir, 'real.kex'));
const t0 = process.hrtime.bigint();
const eng = E.create(b.buffer.slice(b.byteOffset, b.byteOffset + b.byteLength));
console.log(`image ${b.length} bytes loaded+verified+warmed in ${(Number(process.hrtime.bigint() - t0) / 1e6).toFixed(1)} ms:`, JSON.stringify({ hosts: eng.hostRules, exceptions: eng.hostExceptions, patterns: eng.patternRules, tokens: eng.tokenRules, grams: eng.gramRules, tokenless: eng.tokenlessRules }));
const rows = fs.readFileSync(path.join(dir, 'real.corpus.tsv'), 'utf8').split('\n').filter(Boolean).map((l) => l.split('\t'));
const counts = {}, bad = [];
let totalNs = 0n, maxNs = 0n, maxUrl = '';
for (const [url, type, page, want] of rows) {
  const a = process.hrtime.bigint();
  const got = eng.check(url, page === '' ? null : page, Number(type));
  const d = process.hrtime.bigint() - a;
  totalNs += d; if (d > maxNs) { maxNs = d; maxUrl = url; }
  counts[got] = (counts[got] || 0) + 1;
  if (got !== Number(want)) bad.push(`${url.slice(0, 160)}  type=${type} page=${page || '-'} want ${want} got ${got}`);
}
console.log(`${rows.length} requests, verdict mix ${JSON.stringify(counts)}`);
console.log(`average ${(Number(totalNs) / rows.length / 1000).toFixed(2)} us per check, slowest ${(Number(maxNs) / 1000).toFixed(0)} us (${maxUrl.slice(0, 80)})`);
if (bad.length) { console.log(`MISMATCHES: ${bad.length}`); console.log(bad.slice(0, 15).join('\n')); process.exit(1); }
console.log('0 mismatches: the JS engine equals the Kotlin engine on the real lists');
