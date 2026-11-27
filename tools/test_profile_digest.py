#!/usr/bin/env python3
"""Self-test of tools/profile_digest.py on synthetic Gecko profiles in both layouts the format has had (tables as schema + rows, tables as columns). Exit 0 = every check passed.
No real profile existed when this was written: the checks prove the reader's logic, not the layout of a real file (`profile_digest.py --shape FILE` shows that)."""
import gzip
import importlib.util
import json
import os
import random
import re
import sys

here = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location('profile_digest', os.path.join(here, 'profile_digest.py'))
pd = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pd)


def make(layout, seed=1):
    r = random.Random(seed)
    cats = [{'name': n, 'color': 'grey', 'subcategories': ['Other']} for n in ['Idle', 'Other', 'Layout', 'JavaScript', 'GC / CC', 'Network', 'Graphics', 'DOM']]
    strings = ['(root)', 'nsThread::ProcessNextEvent', 'foo (https://assets-proxy.example.com/a/index-AbC.js:10:5)', 'bar (https://assets-proxy.example.com/a/chunk-9.js:3:1)', 'PresShell::DoReflow', 'JS::GC', 'Idle']
    frames = [(0, 1), (1, 1), (2, 3), (3, 3), (4, 2), (5, 4), (6, 0)]
    stacks = [(None, 0), (0, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6)]
    samples = []
    t = 0.0
    for i in range(4000):
        t += 1.0
        busy = (i // 1000) in (0, 1, 3)
        pick = r.choices([2, 3, 4, 5], weights=[50, 25, 15, 10])[0] if busy else 6
        samples.append((pick, t, 1000 if busy else 0))

    def schema(names, rows):
        return {'schema': {n: i for i, n in enumerate(names)}, 'data': [list(x) for x in rows]}

    def cols(names, rows):
        d = {n: [x[i] for x in rows] for i, n in enumerate(names)}
        d['length'] = len(rows)
        return d
    mk = schema if layout == 'schema' else cols
    th = {'name': 'GeckoMain', 'isMainThread': True, 'processType': 'tab', 'processName': 'Content Process', 'pid': '123', 'tid': 123,
          'samples': mk(['stack', 'time', 'threadCPUDelta'], samples), 'stackTable': mk(['prefix', 'frame'], stacks), 'frameTable': mk(['location', 'category'], frames)}
    th['stringTable' if layout == 'schema' else 'stringArray'] = strings
    parent = dict(th)
    parent.update({'processType': 'default', 'processName': 'Parent Process', 'samples': mk(['stack', 'time', 'threadCPUDelta'], [(6, float(i), 0) for i in range(3000)])})
    return {'meta': {'version': 30, 'product': 'GeckoView', 'interval': 1, 'categories': cats}, 'threads': [th, parent], 'processes': []}


fails = []


def check(name, cond):
    if not cond:
        fails.append(name)
        print('  FAIL  ' + name)


for layout in ('schema', 'cols'):
    raw = gzip.compress(json.dumps(make(layout)).encode())
    prof = pd.read_profile(raw)
    out = pd.report(prof)
    check(layout + ': gzip is read', prof['meta']['product'] == 'GeckoView')
    check(layout + ': the page process is named with its categories', 'Content Process' in out and 'JavaScript 56%' in out)
    check(layout + ': the idle parent is idle', 'Parent Process' in out and 'Idle 100%' in out)
    check(layout + ': the busiest main thread is the page process', 'THE BUSIEST MAIN THREAD: GeckoMain of Content Process' in out)
    check(layout + ': the script file with the most samples is named', 'assets-proxy.example.com/index-AbC.js' in out and '36.5%' in out)
    check(layout + ': the busy seconds show the idle second', '2:0%' in out and '0:100%' in out)
    check(layout + ': the label frames are listed', 'PresShell::DoReflow' in out and 'JS::GC' in out)
    check(layout + ': --shape names the layout', ('stringTable' if layout == 'schema' else 'stringArray') in pd.report(prof, only_shape=True))
nested = {'meta': {'product': 'GeckoView', 'categories': make('schema')['meta']['categories']}, 'threads': [], 'processes': [make('schema')]}
check('child-process profiles nested under processes are read', 'Content Process' in pd.report(nested))
check('plain (not gzipped) JSON is read', pd.read_profile(json.dumps(make('cols')).encode())['meta']['version'] == 30)
check('a script frame is told apart from a label frame', pd.script_of('foo (https://h.example/a/b.js:1:2)') == ('h.example/b.js', 'foo') and pd.script_of('PresShell::DoReflow') is None)
# the processed layout stores gaps between samples (timeDeltas) and states the unit of the CPU column
proc = make('cols')
th0 = proc['threads'][0]
th0['samples']['timeDeltas'] = [1.0] * th0['samples']['length']
del th0['samples']['time']
proc['meta']['sampleUnits'] = {'threadCPUDelta': 'us'}
rep = pd.report(proc)
check('timeDeltas become times (4 s of samples) and a stated unit gives the share of a core', 'over   4.0 s, cpu 3.0 s = 75% of one core' in rep)
# interval markers are stored three ways: one row with both times (phase 1) or a START row (phase 2, endTime 0) and an END row (phase 3, startTime 0) that must be joined; network markers are phase-1 rows with a payload
for layout in ('schema', 'cols'):
    mp = make(layout)
    mth = mp['threads'][0]
    strs = mth['stringTable' if layout == 'schema' else 'stringArray']
    def sid(x):
        strs.append(x)
        return len(strs) - 1
    awake, dom, l1, l2, l3 = sid('Awake'), sid('DOMEvent'), sid('Load 1: https://assets-proxy.example.com/a/one.js'), sid('Load 2: https://assets-proxy.example.com/a/two.js'), sid('Load 3: https://other.example.org/x.png')
    net = lambda i, cache, n, u, st='STATUS_STOP': {'type': 'Network', 'id': i, 'status': st, 'cache': cache, 'count': n, 'URI': u}
    mrows = [
        (awake, 0, 5.0, 3, 0, None),                                        # an END whose start was before the buffer began: no length, left out
        (awake, 10.0, 0, 2, 0, None), (awake, 0, 14.0, 3, 0, None),         # 4 ms
        (awake, 20.0, 0, 2, 0, None), (awake, 0, 30.0, 3, 0, None),         # 10 ms
        (dom, 40.0, 0, 2, 0, {'type': 'DOMEvent', 'id': 7}), (dom, 41.0, 0, 2, 0, {'type': 'DOMEvent', 'id': 8}),
        (dom, 0, 45.0, 3, 0, {'type': 'DOMEvent', 'id': 8}), (dom, 0, 60.0, 3, 0, {'type': 'DOMEvent', 'id': 7}),     # two overlapping ones told apart by id: 4 ms and 20 ms
        (awake, 3990.0, 0, 2, 0, None),                                     # a START still open at the end: left out
        (l1, 100.0, 900.0, 1, 6, net(1, 'Hit', 1000, 'https://assets-proxy.example.com/a/one.js')),
        (l2, 200.0, 1200.0, 1, 6, net(2, 'Missed', 3000, 'https://assets-proxy.example.com/a/two.js')),
        (l3, 2100.0, 2150.0, 1, 6, net(3, 'Hit', 7, 'https://other.example.org/x.png', 'STATUS_CANCEL')),
    ]
    sc = ['name', 'startTime', 'endTime', 'phase', 'category', 'data']
    mth['markers'] = ({'schema': {n: i for i, n in enumerate(sc)}, 'data': [list(x) for x in mrows]} if layout == 'schema'
                      else dict({n: [x[i] for x in mrows] for i, n in enumerate(sc)}, length=len(mrows)))
    ivs = pd.marker_intervals(pd.Thread(mth, mp, 'Content Process'))
    check(layout + ': start and end rows are joined (5 intervals: 2 awake, 2 overlapping DOM events, ...)', sorted(round(b - a) for n, a, b, d in ivs if n in ('Awake', 'DOMEvent')) == [4, 4, 10, 20])
    mrep = pd.report(mp)
    check(layout + ': time inside an interval kind is totalled with its count and longest', re.search(r'\b14 ms\s+2 x\s+longest\s+10 ms\s+Awake', mrep) is not None)
    check(layout + ': network markers are summed (3 requests, bytes, cache, ends)', 'network: 3 requests, 4007 bytes, cache Hit 2 Missed 1, ends STOP 2 CANCEL 1' in mrep)
    check(layout + ': hosts are counted', 'assets-proxy.example.com 2' in mrep and 'other.example.org 1' in mrep)
    check(layout + ': opened and ended per 2 s are listed', '0:2/2 2:1/1' in mrep and 'opened / ended per 2 s' in mrep)
print('profile_digest self-test: %s' % ('OK' if not fails else '%d FAILED' % len(fails)))
sys.exit(1 if fails else 0)
