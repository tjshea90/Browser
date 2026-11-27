#!/usr/bin/env python3
"""Self-test of tools/profile_digest.py on synthetic Gecko profiles in both layouts the format has had (tables as schema + rows, tables as columns). Exit 0 = every check passed.
No real profile existed when this was written: the checks prove the reader's logic, not the layout of a real file (`profile_digest.py --shape FILE` shows that)."""
import gzip
import importlib.util
import json
import os
import random
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
print('profile_digest self-test: %s' % ('OK' if not fails else '%d FAILED' % len(fails)))
sys.exit(1 if fails else 0)
