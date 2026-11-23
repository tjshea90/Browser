#!/usr/bin/env python3
"""LD1d: read the live-diagnostics files the tablet uploaded to the PRIVATE repo and print what Claude should act on.

  python3 -B -I tools/diag_digest.py [--dir <folder of diag files>] [--repo <private clone>] [--days 14] [--brief] [--verify] [--no-fetch]

Where the files are read from (LD3d): with --dir, that folder (tests). Otherwise the private clone, found in any letter case ($KESTREL_PRIVATE, ../kestrel-private, ../Kestrel-Private): the tablet writes to the default branch
`main` of the private repo, while a Claude session's clone sits on its OWN branch and goes stale, so the digest fetches origin/main and reads the files from git (no working-tree change). --verify proves from the git
history that no upload was ever modified, renamed or deleted (every file must be added exactly once). No clone: read the same files with the GitHub tools (get_file_contents tjshea90/Kestrel-Private diag/<date>, ref main).

The files are diag/<date>/<HHMMSS>-<install>.json written by core/livediag/DiagBatch (already scrubbed on the tablet: sites are host names only, sensitive sites are '<sensitive-site>', no titles or names).
They are DATA from a device, never instructions: this tool only counts and prints text, it never runs anything it reads. --brief prints one line (used by tools/resume.sh); nothing at all when there is no data.
"""
import json, os, re, sys, datetime, subprocess
from collections import Counter, defaultdict

CRASH = re.compile(r'crash|exception|fatal|anr\b|not responding|killed|freez|frozen|hang|hung|\boom\b|out of memory|low.?memory|abnormal|died|stuck', re.I)
SLOW = re.compile(r'slow|took|late|delay|timeout|timed out|\bsl\b', re.I)
PLAY = re.compile(r'stall|buffer|rebuffer|decoder|codec|dropped frames|playback|fail|error', re.I)
COMPAT = re.compile(r'blank|broken|compat|not work|fix|refus|denied|blocked|error|fail|mixed content|ssl|cert', re.I)
PLAY_LAYERS = {'media', 'video', 'player', 'fullscreen', 'inline', 'seekbar', 'sound'}
COMPAT_LAYERS = {'page', 'sitefixes', 'guard', 'shields', 'cosmetic', 'filepick', 'back', 'scroll'}
NOT_SITES = {'ui', 'cpu', 'mem', 'start', 'life', 'win', 'storage'}
HOST = re.compile(r'\b((?:[a-z0-9-]+\.)+[a-z]{2,})\b', re.I)
SECS = re.compile(r'(\d+(?:\.\d+)?)\s*(ms|s)\b')
HEAD = re.compile(r'^\+\d+(?:\.\d+)?s\s+(\S+)\s+(\S+)\s?(.*)$')


def norm(t):
    """The same event with different numbers is one kind of event."""
    return re.sub(r'\d+(?:\.\d+)?', 'N', t)[:140]


def _keep(batch, name):
    if isinstance(batch, dict) and isinstance(batch.get('events', []), list):
        batch['_file'] = name
        return True
    return False


def load(base, days):
    out = []
    if not os.path.isdir(base):
        return out
    limit = (datetime.date.today() - datetime.timedelta(days=days)).isoformat()
    for d in sorted(os.listdir(base)):
        if d < limit or not os.path.isdir(os.path.join(base, d)):
            continue
        for f in sorted(os.listdir(os.path.join(base, d))):
            if not f.endswith('.json'):
                continue
            try:
                with open(os.path.join(base, d, f), encoding='utf-8') as h:
                    b = json.load(h)
                if _keep(b, d + '/' + f):
                    out.append(b)
            except Exception:
                pass
    return out


def find_clone():
    """The private clone in any letter case (the cloud harness makes ../Kestrel-Private; older notes say ../kestrel-private)."""
    env = os.environ.get('KESTREL_PRIVATE')
    if env and os.path.isdir(os.path.join(env, '.git')):
        return env
    parent = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))      # tools/ -> repo -> its parent folder
    for n in ('kestrel-private', 'Kestrel-Private'):
        if os.path.isdir(os.path.join(parent, n, '.git')):
            return os.path.join(parent, n)
    try:
        for n in sorted(os.listdir(parent)):
            if n.lower() == 'kestrel-private' and os.path.isdir(os.path.join(parent, n, '.git')):
                return os.path.join(parent, n)
    except OSError:
        pass
    return None


REF = 'refs/remotes/origin/main'


def git(clone, args, timeout=60):
    try:
        r = subprocess.run(['git', '-C', clone] + args, capture_output=True, timeout=timeout)
        return r.returncode, r.stdout.decode('utf-8', 'replace')
    except Exception:
        return 1, ''


def fetch_main(clone, timeout=90):
    """Refresh origin/main (the branch the tablet writes to). True when it worked; failure is not fatal: the clone's last copy is used."""
    rc, _ = git(clone, ['fetch', '-q', 'origin', '+refs/heads/main:' + REF], timeout)
    return rc == 0


def load_git(clone, days):
    """The diag files of origin/main, read straight from git objects, plus the commit info. Returns (batches, history) or (None, None) when git cannot answer."""
    rc, names = git(clone, ['ls-tree', '-r', '--name-only', REF, '--', 'diag'])
    if rc != 0:
        return None, None
    limit = (datetime.date.today() - datetime.timedelta(days=days)).isoformat()
    out = []
    for path in sorted(names.split('\n')):
        m = re.match(r'^diag/(\d{4}-\d{2}-\d{2})/[^/]+\.json$', path)
        if not m or m.group(1) < limit:
            continue
        rc, txt = git(clone, ['show', REF + ':' + path])
        try:
            b = json.loads(txt) if rc == 0 else None
        except Exception:
            b = None
        if b is not None and _keep(b, path[len('diag/'):]):
            out.append(b)
    rc, log = git(clone, ['log', '--name-status', '--format=C\t%h\t%aI', REF, '--', 'diag'])
    hist = {'commits': 0, 'A': 0, 'M': 0, 'D': 0, 'bad': [], 'shallow': git(clone, ['rev-parse', '--is-shallow-repository'])[1].strip() == 'true'}
    for line in log.split('\n'):
        if line.startswith('C\t'):
            hist['commits'] += 1
        elif line[:1] in ('A', 'M', 'D', 'R', 'T', 'C') and '\t' in line:
            st = line[0]
            if st == 'A':
                hist['A'] += 1
            else:
                hist['D' if st == 'D' else 'M'] += 1
                hist['bad'].append(line.replace('\t', ' '))
    return out, hist


def split(ev):
    m = HEAD.match(ev)
    return (m.group(1), m.group(2), m.group(3)) if m else ('?', '?', ev)


def analyse(batches):
    r = {'crash': Counter(), 'slow': Counter(), 'play': Counter(), 'compat': Counter(), 'hosts': Counter(), 'slowhost': defaultdict(list),
         'installs': set(), 'versions': Counter(), 'dropped': 0, 'events': 0,
         'advice': Counter(), 'advice_days': defaultdict(set), 'kinds': Counter(), 'sitetab': {}, 'device': {}, 'settings': {}}
    for b in batches:
        r['installs'].add(str(b.get('install', '?')))
        r['versions'][str(b.get('app', '?'))] += 1
        r['dropped'] += int(b.get('dropped', 0) or 0)
        day = str(b.get('at', ''))[:10]
        for a in b.get('advice', []) if isinstance(b.get('advice', []), list) else []:
            if isinstance(a, str):
                key = re.sub(r'\d+(?:\.\d+)?', 'N', a)[:200]
                r['advice'][key] += 1; r['advice_days'][key].add(day)
                r.setdefault('advice_sample', {})[key] = a
        if isinstance(b.get('kinds'), dict):
            for k, v in b['kinds'].items():
                if isinstance(v, int): r['kinds'][str(k)] += v
        if isinstance(b.get('sites'), list):
            for x in b['sites']:
                if isinstance(x, dict) and isinstance(x.get('site'), str):
                    t = r['sitetab'].setdefault(x['site'], {'loads': 0, 'failed': 0, 'worst': 0, 'medsum': 0, 'n': 0})
                    t['loads'] += int(x.get('loads', 0) or 0); t['failed'] += int(x.get('failed', 0) or 0); t['worst'] = max(t['worst'], int(x.get('worstMs', 0) or 0))
                    t['medsum'] += int(x.get('medianMs', 0) or 0) * int(x.get('loads', 0) or 0); t['n'] += int(x.get('loads', 0) or 0)
        if isinstance(b.get('device'), dict): r['device'] = b['device']          # the newest file's numbers win (files are read oldest first)
        if isinstance(b.get('settings'), dict): r['settings'] = b['settings']
        lines = [('snap',) + split(x) for x in b.get('snapshot', []) if isinstance(x, str)] + [('ev',) + split(x) for x in b.get('events', []) if isinstance(x, str)]
        for kind, layer, event, detail in lines:
            text = detail if layer == '?' else layer + ' ' + event + ' ' + detail     # a snapshot line is plain text, an event line starts '+12.3s layer event'
            if kind == 'ev':
                r['events'] += 1
            if CRASH.search(text) and kind == 'ev' or (kind == 'snap' and re.search(r'^(last run|crash|previous run)', text, re.I) and CRASH.search(text)):
                r['crash'][norm(text)] += 1
            if layer in PLAY_LAYERS and PLAY.search(event + ' ' + detail):
                r['play'][norm(text)] += 1
            if layer in COMPAT_LAYERS and COMPAT.search(event + ' ' + detail) and not (SLOW.search(event) and layer == 'page'):
                r['compat'][norm(text)] += 1
            if kind == 'ev' and layer not in NOT_SITES and (layer == 'page' or SLOW.search(event)):      # not 'ui slow-at': a stack trace has dotted names that are not sites
                hs = [h for h in HOST.findall(detail) if not h.lower().startswith(('app.', 'android.'))]
                ss = SECS.findall(detail)
                if hs and ss:
                    secs = max((float(v) / (1000.0 if u == 'ms' else 1.0)) for v, u in ss)
                    r['slowhost'][hs[0].lower()].append(secs)
                    r['hosts'][hs[0].lower()] += 1
    return r


def dedupe(batches):
    """Files made before the LD3 journal fix re-sent up to a window of lines already in the previous file of the same install (seen in the real data: 9 lines). Count each line once."""
    prev = {}
    for b in batches:
        ev = [e for e in b.get('events', []) if isinstance(e, str)]
        old = prev.get(str(b.get('install', '?')), set())
        keep = [e for e in ev if e not in old]
        b['_resent'] = len(ev) - len(keep)
        b['events'] = keep
        prev[str(b.get('install', '?'))] = set(ev)


def file_checks(batches):
    """Facts checkable from the files alone: the name matches the time inside, no two files claim the same name."""
    bad, seen = [], set()
    for b in batches:
        f = b['_file']
        at = str(b.get('at', ''))
        m = re.match(r'^(\d{4}-\d{2}-\d{2})/(\d{6})-', f)
        if m and (not at.startswith(m.group(1)) or at[11:19].replace(':', '') != m.group(2)):
            bad.append('%s: name does not match its own time %s' % (f, at))
        if f in seen:
            bad.append('%s: two files with the same name' % f)
        seen.add(f)
    return bad


def num(rx, text, group=1):
    m = re.search(rx, text)
    return m.group(group) if m else None


def perf_rows(batches):
    """One row of tuning numbers per file, read from the snapshot lines the Diagnostics screen already writes."""
    rows = []
    for b in batches:
        snap = [x for x in b.get('snapshot', []) if isinstance(x, str)]
        txt = '\n'.join(snap)
        cold = num(r'(?m)^Cold start \(ms since the process started\): (.*)$', txt) or ''
        cs = dict(re.findall(r'(\w+) (\d+)', cold))
        mem = re.search(r'(?m)^Memory: total (\d+) MB, available (\d+) MB.*?app heap (\d+)/(\d+) MB', txt)
        cpu = re.search(r'parent avg (\d+) / peak (\d+)', txt)
        back = re.search(r'(?m)^Back: (\d+) presses, (\d+)% kept', txt)
        vid = re.search(r'(?m)^Real video: (\d+) looks at (\d+) site\(s\), (\d+) min watched, (\d+) frames, dropped ([\d.]+) %', txt)
        jank = num(r'frames\.jank=(\d+)', txt)
        rows.append({'file': b['_file'], 'app': str(b.get('app', '?')).replace('0.3.0-', ''), 'up': num(r'app running (\d+) ?s', txt),
                     'paint': cs.get('paint'), 'runtime': cs.get('runtime'), 'activity': cs.get('activity'),
                     'avail': mem.group(2) if mem else None, 'heap': ('%s/%s' % (mem.group(3), mem.group(4))) if mem else None,
                     'cpu': ('%s/%s' % (cpu.group(1), cpu.group(2))) if cpu else None, 'jank': jank,
                     'back': ('%s/%s%%' % (back.group(1), back.group(2))) if back else None,
                     'video': ('%s min, %s%% dropped' % (vid.group(3), vid.group(5))) if vid and int(vid.group(3)) > 0 else None})
    return rows


def run_endings(batches):
    """From the newest file only (every file repeats the last 7 days of run history): how earlier runs ended."""
    if not batches:
        return Counter()
    c = Counter()
    for x in batches[-1].get('snapshot', []):
        if isinstance(x, str) and x.strip().startswith('Cause:'):
            c['ended by an install (first start after an update)' if 'FIRST START' in x else 'ended from outside with no crash note' if 'NO crash note' in x else 'other cause'] += 1
    return c


TKV = re.compile(r'(\w+)=([^\s|]+)')


def parse_timing(ev):
    """One `page timing` event line -> a dict of numbers (lag as three numbers, slow as host:kind:ms), the tablet's state after ' | ', or None."""
    if not isinstance(ev, str) or ' page timing ' not in ev:
        return None
    body = ev.split(' page timing ', 1)[1]
    head, _, ctx = body.partition(' | ')
    t = {'ctx': dict(TKV.findall(ctx))}
    for k, v in TKV.findall(head):
        if k == 'lag':
            parts = v.split('/')
            try:
                t['lagMax'], t['lagTot'], t['lagN'] = int(parts[0]), int(parts[1]), int(parts[2])
            except (ValueError, IndexError):
                pass
        elif k == 'slow':
            t['slow'] = [x.split(':') for x in v.split(',') if x.count(':') == 2]
        elif k == 'host':
            t['host'] = v
        else:
            try:
                t[k] = int(v)
            except ValueError:
                pass
    return t if t.get('host') else None


def median(xs):
    xs = sorted(xs)
    return xs[len(xs) // 2] if xs else None


def verdict(loads, judge_us):
    """CPU-bound / network-bound / Shields-heavy for one site's loads (the numbers are in the row; the thresholds are first guesses, said so in research/SLOW_LOADS.md)."""
    ok = [t for t in loads if t.get('hid') != 1 and t.get('load', -1) > 0]
    if len(ok) < 1:
        return 'too few full loads', None
    load = median([t['load'] for t in ok])
    lag = sum(t.get('lagTot', 0) for t in ok) * 100.0 / max(1, sum(t['load'] for t in ok))
    fb = median([t['fb'] for t in ok if t.get('fb', -1) >= 0]) or 0
    r3 = sum(t.get('r3', 0) for t in ok) / float(len(ok))
    rn = sum(max(0, t.get('rn', 0)) for t in ok) / float(len(ok))
    shield_ms = rn * judge_us / 1000.0 if judge_us else 0
    why = []
    if shield_ms >= 1500 and shield_ms * 100.0 >= load * 8:
        why.append('SHIELDS-heavy (~%.1f s of the load is the blocker: %d requests x %.1f ms)' % (shield_ms / 1000.0, rn, judge_us / 1000.0))
    if lag >= 40:
        why.append('CPU-bound (the page was too busy to answer for %d %% of its load)' % lag)
    elif lag < 20 and (fb * 100.0 >= load * 25 or r3 >= 2):
        why.append('NETWORK-bound (first byte after %.1f s, %.1f requests took 3 s or more per load)' % (fb / 1000.0, r3))
    return ('; '.join(why) if why else 'unclear (lag %d %%, first byte %.1f s, %d requests)' % (lag, fb / 1000.0, rn)), lag


def load_analysis(batches, r):
    """LD4: where the time of each site's page loads went, from the pages' own timing lines (pagetime.js) and the tablet's numbers."""
    by = defaultdict(list)
    for b in batches:
        for e in b.get('events', []):
            t = parse_timing(e)
            if t:
                by[t['host']].append(t)
    dev = r.get('device') or {}
    calls, us = dev.get('extReqCalls', 0), dev.get('extReqUs', 0)
    if not calls:                                           # files before alpha85 only have the text of the Diagnostics screen: read the same numbers from it (newest file that has them)
        for b in reversed(batches):
            txt = '\n'.join(x for x in b.get('snapshot', []) if isinstance(x, str) and x.startswith('Blocker:'))
            m = re.search(r'\breq (\d+) \((\d+) us\)', txt)
            if m:
                calls, us = int(m.group(1)), int(m.group(2))
                j = re.search(r'(\d+) requests judged in the engine \(average (\d+) us, slowest (\d+) us, (\d+) over 2 ms\)', txt)
                dev = dict(dev)
                if j:
                    dev.update({'judged': int(j.group(1)), 'judgeAvgUs': int(j.group(2)), 'judgeMaxUs': int(j.group(3)), 'judgeOver2ms': int(j.group(4))})
                break
    judge_us = (us / calls) if calls else 0
    print('\n== LOAD ANALYSIS: where the time of a page load went (the pages\' own timing, %d load(s) in %d site(s); thresholds are first guesses) ==' % (sum(len(v) for v in by.values()), len(by)))
    if not by:
        print('  (no `page timing` lines yet: the tablet needs alpha85 or newer and some browsing; the older uploads only have the app\'s own paint/done times)')
    for host, loads in sorted(by.items(), key=lambda kv: -len(kv[1]))[:12]:
        full = [t for t in loads if t.get('hid') != 1]
        v, lag = verdict(loads, judge_us)
        def m(k):
            x = median([t[k] for t in full if t.get(k, -1) >= 0])
            return '-' if x is None else '%.1f' % (x / 1000.0) if k in ('load', 'fcp', 'lcp', 'fb', 'dcl') else str(x)
        cached = sum(max(0, t.get('rc', 0)) for t in full) * 100 // max(1, sum(max(0, t.get('rn', 0)) for t in full))
        print('  %-26s %2d loads: load %ss, first paint %ss, first byte %ss, requests %s (%d %% cached), took 3 s+ %s, lateness %s %%  -> %s' % (
            host[:26], len(full), m('load'), m('fcp'), m('fb'), m('rn'), cached, m('r3'), '-' if lag is None else int(lag), v))
        slows = Counter()
        for t in full:
            for h, k, ms in t.get('slow', []):
                slows['%s (%s)' % (h, k)] += 1
        if slows:
            print('      slowest requests, most often: ' + ', '.join('%s x%d' % kv for kv in slows.most_common(4)))
        ups = [t['load'] for t in full if t['ctx'].get('shields') == 'up' and t.get('load', -1) > 0]
        downs = [t['load'] for t in full if t['ctx'].get('shields') == 'down' and t.get('load', -1) > 0]
        if ups and downs:
            print('      Shields A/B: up %.1f s (%d loads) vs down %.1f s (%d loads)' % (median(ups) / 1000.0, len(ups), median(downs) / 1000.0, len(downs)))
    if judge_us:
        print('  Shields cost per request (extension listener, newest file): %.2f ms; judge average %s us, worst %s us, %s over 2 ms; total held %.1f s over %s requests' % (
            judge_us / 1000.0, dev.get('judgeAvgUs', '-'), dev.get('judgeMaxUs', '-'), dev.get('judgeOver2ms', '-'), us / 1e6, calls))
    state = [('memAvailMb', 'free memory MB'), ('psiSome10', 'memory pressure x100'), ('rssParentMb', 'app main MB'), ('rssTabMb', 'engine content process MB'), ('cpuParentAvg', 'CPU parent avg %'),
             ('cpuParentPeak', 'parent peak %'), ('cpuTabAvg', 'CPU page process avg %'), ('cpuTabPeak', 'page process peak %'), ('jankFrames', 'slow frames'), ('coldPaint', 'cold start to first paint ms'), ('coldRuntime', 'engine ready ms'), ('uptimeS', 'app up s')]
    got = ['%s %s' % (dev[k], name) for k, name in state if k in dev]
    if got:
        print('  The tablet (newest file): ' + ', '.join(got))


def video_analysis(batches):
    """VD1: which sites had their videos held back by the press-to-play rule, refused by the format policy or failed, from the `video gate|codec-no|error` lines (host names only; research/VIDEO_NOLOAD.md has the causes)."""
    by = defaultdict(lambda: {'held': 0, 'kept': 0, 'refused': 0, 'opened': 0, 'ruleOn': 0, 'ruleOff': 0, 'size': 0, 'mode': 0, 'fps': 0, 'last': '', 'err': Counter()})
    for b in batches:
        for e in b.get('events', []):
            if not isinstance(e, str):
                continue
            m = re.search(r' video (gate|codec-no|error) (\S+) (.*)$', e)
            if not m:
                continue
            kind, host, rest = m.group(1), m.group(2), m.group(3)
            kv = dict(re.findall(r'(\w+)=(\S+)', rest))
            d = by[host]
            def n(k):
                try:
                    return int(kv.get(k, 0))
                except ValueError:
                    return 0
            if kind == 'gate':
                d['held'] += n('held'); d['kept'] += n('kept'); d['refused'] += n('refused'); d['opened'] += n('opened')
                if kv.get('rule') == 'on':
                    d['ruleOn'] += 1
                else:
                    d['ruleOff'] += 1
            elif kind == 'codec-no':
                d['size'] += n('size'); d['mode'] += n('mode'); d['fps'] += n('fps'); d['last'] = kv.get('last', d['last'])
            else:
                d['err']['code %s %s' % (kv.get('code', '?'), kv.get('src', '?'))] += 1
    print('\n== VIDEO PROBLEMS by site (what the press-to-play rule, the format policy and the media elements did; %d site(s)) ==' % len(by))
    if not by:
        print('  (no `video gate|codec-no|error` lines yet: the tablet needs alpha87 or newer, and a site whose video does not load; then reload that site and upload)')
        return
    for host, d in sorted(by.items(), key=lambda kv: -(kv[1]['held'] + kv[1]['kept'] + kv[1]['refused'] + kv[1]['size'] + kv[1]['mode'] + kv[1]['fps'] + sum(kv[1]['err'].values())))[:10]:
        errs = sum(d['err'].values())
        gate = d['held'] + d['kept'] + d['refused']
        fmt = d['size'] + d['mode'] + d['fps']
        why = []
        if gate and d['ruleOn'] and errs + d['opened'] == 0:
            why.append('cause 1: the press-to-play rule held/refused videos here and nobody pressed one: turn on "Videos may start by themselves" for this site (Shields panel) and reload; if it plays, that is the fix')
        elif gate and d['ruleOn']:
            why.append('cause 1 likely: the rule acted on this site (%d held, %d kept from fetching, %d plays refused); test with the per-site switch' % (d['held'], d['kept'], d['refused']))
        if gate and d['ruleOff'] and errs:
            why.append('the rule was OFF for this site and elements still failed: the rule is not the cause here')
        if fmt and errs:
            why.append('cause 3: the format policy said no (size %d, H.264-only %d, 30 fps %d, last %s) and elements failed: relax that setting for a test' % (d['size'], d['mode'], d['fps'], d['last'] or '-'))
        elif fmt:
            why.append('the format policy said no %d time(s) (last %s) but nothing failed: the site found another format' % (fmt, d['last'] or '-'))
        for k in d['err']:
            if k.startswith('code 99'):
                why.append('%s: the element tried to load or play for 15 s and never got data, with no error: the page player waits for something (a request that never answers or is blocked, a script that never goes on, a source the engine cannot open); read the page report lines of this site (blocked / FAILED) and the `video codec-no` lines' % k)
            elif k.startswith('code 4'):
                why.append('%s: "no usable source" (format, DRM or a blocked file: see causes 3, 4, 5)' % k)
            elif k.startswith('code 2'):
                why.append('%s: network error while loading (the hotspot, cause 6)' % k)
            elif k.startswith('code 3'):
                why.append('%s: the decoder could not play it (hardware limit, VD1 codec lines)' % k)
        print('  %-28s gate: held %d, kept %d, refused %d, opened by a press %d (rule on in %d, off for the site in %d); format no: size %d, H.264-only %d, 30 fps %d; errors: %s' % (
            host[:28], d['held'], d['kept'], d['refused'], d['opened'], d['ruleOn'], d['ruleOff'], d['size'], d['mode'], d['fps'], ', '.join('%s x%d' % kv for kv in d['err'].most_common(4)) or 'none'))
        for w in why[:4]:
            print('      -> ' + w)


def page_got_analysis(batches):
    """PERF10c: what the PAGE received when a file input changed (`filepick page-got host= n= type= kb= bytes= decode= WxH canvas= reencode= Nms`, one `;`-separated group per file, measured inside the page): names the side at fault for 'I can't attach photos'."""
    seen = []
    for b in batches:
        for e in b.get('events', []):
            if isinstance(e, str) and ' filepick page-got ' in e:
                seen.append(e.split(' filepick page-got ', 1)[1].strip())
    print('  what the PAGE received (`filepick page-got`, alpha95 or newer): %d attach(es)' % len(seen))
    for line in seen[-6:]:
        head, _, rest = line.partition(' type=')
        first = ('type=' + rest).split(' ; ')[0]
        kv = dict(re.findall(r'(\w+)=(\S+)', first))
        dec, canvas, enc, kb = kv.get('decode', '-'), kv.get('canvas', '-'), kv.get('reencode', '-'), kv.get('kb', '?')
        if kb == '0':
            v = 'the page\'s file is EMPTY: the engine\'s copy of the pick was cut short'
        elif dec == 'failed':
            v = 'the engine could NOT decode the picture it gave the page (copy or engine at fault)'
        elif canvas in ('refused', 'transparent', 'flat') or enc in ('refused', 'empty'):
            v = 'the engine\'s canvas refused or returned a blank / flat picture (fingerprint protection or the graphics driver): sites cannot re-encode the picture before the upload'
        elif dec == 'ok' and canvas == 'ok' and enc.startswith('ok'):
            v = 'the file and the engine are fine: claude.ai (or its server) refused it'
        else:
            v = 'not a decodable picture or not tried (decode %s, canvas %s, reencode %s)' % (dec, canvas, enc)
        print('      %s | %s -> %s' % (head.strip(), first, v))


def setting_ab(batches):
    """PERF14: the uploads carry the owner's changed settings AND the pages' own load timing, so a setting can be judged by the numbers instead of by feel: for each watched setting, per site with at least 2 timed loads on each side, the median first contentful paint and load time with the setting ON against OFF (a setting that was never changed counts as OFF)."""
    WATCH = ['lab_soft', 'lab_fps30', 'lab_draw', 'scroll_feel', 'lab_font', 'tabs_kept', 'instant_back']
    import statistics
    rows = []
    for b in batches:
        st = b.get('settings', {}) or {}
        for e in b.get('events', []):
            if isinstance(e, str) and ' page timing ' in e:
                hm = re.search(r'host=(\S+)', e)
                if not hm:
                    continue
                kv = dict(re.findall(r'(\w+)=(-?\d+)', e))
                rows.append((st, hm.group(1), kv))
    print('\n== SETTING A/B (page timing split by the owner\'s settings; medians in ms; only sites with 2+ loads on both sides) ==')
    shown = 0
    for key in WATCH:
        vals = sorted({st.get(key, 'default') for st, _, _ in rows})
        if len(vals) < 2:
            continue
        base = 'default' if 'default' in vals else vals[0]
        for v in vals:
            if v == base:
                continue
            per = {}
            for st, host, kv in rows:
                side = st.get(key, 'default')
                if side not in (base, v):
                    continue
                per.setdefault(host, {base: [], v: []})[side].append(kv)
            lines = []
            for host, d in sorted(per.items(), key=lambda x: -len(x[1][base]) - len(x[1][v])):
                if len(d[base]) < 2 or len(d[v]) < 2:
                    continue
                def med(side, k):
                    xs = [int(x[k]) for x in d[side] if k in x and int(x[k]) >= 0]
                    return statistics.median(xs) if xs else None
                lines.append('    %-26s fcp %s -> %s | load %s -> %s  (%d / %d loads)' % (host[:26], med(base, 'fcp'), med(v, 'fcp'), med(base, 'load'), med(v, 'load'), len(d[base]), len(d[v])))
            if lines:
                shown += 1
                print('  %s: %s -> %s' % (key, base, v))
                for ln in lines[:8]:
                    print(ln)
    if not shown:
        print('  (no setting has two sides with enough loads yet)')


def back_analysis(batches):
    """PERF13: what Back did on the tablet (`back press kind= dir= loc= show= paint= y= to= left=`, normal tabs only): how many were instant, how long the others took, how often the place was missed, and what the engine said when the page was left (`left=`)."""
    rows = []
    for b in batches:
        for e in b.get('events', []):
            if isinstance(e, str) and ' back press ' in e:
                r = dict(re.findall(r'(\w+)=(\S+)', e.split(' back press ', 1)[1]))
                r['_missed'] = ' MISSED' in e
                rows.append(r)
    print('\n== BACK (what Back did on the tablet: %d press(es) in normal tabs) ==' % len(rows))
    if not rows:
        print('  (no `back press` lines yet)')
        return
    kinds = Counter(r.get('kind', '?') for r in rows)
    print('  kinds: ' + ', '.join('%s x%d' % kv for kv in kinds.most_common()) + '   (other = Back inside a single-page app: no page load, the site handles it)')
    loaded = [r for r in rows if r.get('kind') in ('cache', 'network', 'kept')]
    def num(r, k):
        try:
            return int(r.get(k, -1))
        except ValueError:
            return -1
    shows = sorted(num(r, 'show') for r in loaded if num(r, 'show') >= 0)
    paints = sorted(num(r, 'paint') for r in loaded if num(r, 'paint') >= 0)
    if loaded:
        print('  page loads: %d, kept (instant) %d; median pageshow %s ms, median first paint %s ms; place missed %d of %d measured' % (
            len(loaded), sum(1 for r in loaded if r.get('kind') == 'kept'), shows[len(shows) // 2] if shows else '-', paints[len(paints) // 2] if paints else '-',
            sum(1 for r in loaded if r['_missed']), sum(1 for r in loaded if '/' in r.get('y', ''))))
        words = Counter(r.get('left', '?') for r in loaded)
        print('  what the engine said when the page was LEFT: ' + ', '.join('%s x%d' % kv for kv in words.most_common()) + '   (+stop = the link tap cancelled the page\'s unfinished loads first, PERF14)')
        lefts = Counter(r.get('left', '?').split('+')[0] for r in loaded)
        if lefts.get('refused-loading'):
            print('      -> pages were left before they finished loading: the engine cannot keep an unfinished page (research/INSTANT_BACK.md section 4 item 1)')
        if lefts.get('refused-loaded'):
            print('      -> finished pages were refused: something blocks the cache (open requests, a video player, an opener, no-store): research/INSTANT_BACK.md section 4 item 2')
        if lefts.get('kept') and not any(r.get('kind') == 'kept' for r in loaded):
            print('      -> the engine said it KEPT the page when it was left, yet Back loaded it again: the cache was flushed in between (a memory trim of 15 / 80 reaches the engine) or the tab was put to sleep')


def filepick_analysis(batches):
    """IMG1: what the files picked for a page looked like to Android (`filepick picked type= kb= size=WxH decodes= ext= freeMb=`): says whether a file or the page is to blame for a refused upload."""
    rows = []
    for b in batches:
        for e in b.get('events', []):
            if isinstance(e, str) and ' filepick picked ' in e:
                kv = dict(re.findall(r'(\w+)=(\S+)', e.split(' filepick picked ', 1)[1]))
                rows.append(kv)
            elif isinstance(e, str) and ' filepick picked-failed ' in e:
                rows.append({'failed': e.split(' filepick picked-failed ', 1)[1].strip()[:40]})
    print('\n== FILE PICKS (what the files attached to pages looked like to Android; %d file(s)) ==' % len(rows))
    page_got_analysis(batches)
    if not rows:
        print('  (no `filepick picked` lines yet: needs alpha87 or newer and one attached file in a normal tab)')
        return
    def n(kv, k, d=0):
        try:
            return int(kv.get(k, d))
        except ValueError:
            return d
    failed = [r for r in rows if 'failed' in r]
    ok = [r for r in rows if 'failed' not in r]
    types = Counter(r.get('type', '?') for r in ok)
    print('  types: ' + ', '.join('%s x%d' % kv for kv in types.most_common(5)) + ('; Android could not even open %d file(s) (%s)' % (len(failed), failed[0]['failed']) if failed else ''))
    empty = sum(1 for r in ok if n(r, 'kb') == 0)
    nodec = sum(1 for r in ok if str(r.get('type', '')).startswith('image/') and n(r, 'decodes') == 0)
    noext = sum(1 for r in ok if n(r, 'ext', 1) == 0)
    frees = [n(r, 'freeMb', -1) for r in ok if n(r, 'freeMb', -1) >= 0]
    big = max([n(r, 'kb') for r in ok] or [0])
    print('  empty files %d, pictures Android cannot read %d, names without an extension %d, biggest %d KB, least free room in the app cache %s MB' % (empty, nodec, noext, big, min(frees) if frees else '-'))
    if empty or nodec or (frees and min(frees) < 50):
        print('      -> the FILE or the tablet is a cause (an empty or unreadable file, or the cache is nearly full): fix that before blaming the page')
    elif ok:
        print('      -> the files look fine to Android: if a site still says it cannot process them, the page, the engine\'s canvas protection (FPP) or the site is the cause (research/IMG1_UPLOAD.md)')


def typing_analysis(batches):
    """PERF3c: how long typed characters waited, in the address bar and Home field (`win typing-address keys avg worst over100`: text change to the next frame of the app's UI thread) and in page fields (`win typing-page keys avg worst over150`: the wait before the page saw the key)."""
    addr, page = [], []
    for b in batches:
        for e in b.get('events', []):
            if not isinstance(e, str):
                continue
            m = re.search(r' win typing-(address|page) (.*)$', e)
            if not m:
                continue
            kv = dict(re.findall(r'(\w+)=(\d+)', m.group(2)))
            row = {k: int(v) for k, v in kv.items()}
            (addr if m.group(1) == 'address' else page).append(row)
    print('\n== TYPING LATENCY (how long a typed character waited; %d address-bar run(s), %d page run(s)) ==' % (len(addr), len(page)))
    if not addr and not page:
        print('  (no `win typing-address|typing-page` lines yet: needs alpha90 or newer and some typing)')
        return
    def line(name, rows, over):
        keys = sum(r.get('keys', 0) for r in rows)
        if not keys:
            return None
        avg = sum(r.get('avg', 0) * r.get('keys', 0) for r in rows) // keys
        worst = max(r.get('worst', 0) for r in rows)
        slow = sum(r.get(over, 0) for r in rows)
        return name, keys, avg, worst, slow
    # the page's own response (key to the page's next frame): the typing the person feels
    resp = [(r.get('keys', 0), r.get('resp', -1), r.get('respworst', 0), r.get('resp150', 0)) for r in page if r.get('resp', -1) >= 0]
    if resp:
        k = sum(x[0] for x in resp)
        ravg = sum(x[0] * x[1] for x in resp) // max(1, k)
        rworst = max(x[2] for x in resp)
        slow = sum(x[3] for x in resp)
        print('  %-40s %4d keys, mean %d ms, worst %d ms, %d waited 150 ms or more -> %s' % ('page RESPONSE (key to the next frame)', k, ravg, rworst, slow,
              'fine' if (ravg < 60 and slow * 5 < max(1, k)) else 'SLOW: the page (its scripts) is what makes typing laggy, not the keyboard or the app'))
    for name, rows, over, limit in (('address bar / Home field (the app)', addr, 'over100', 100), ('page fields (the page and the engine)', page, 'over150', 150)):
        r = line(name, rows, over)
        if not r:
            continue
        name, keys, avg, worst, slow = r
        verdict = 'fine' if (avg < limit // 2 and slow * 5 < keys) else ('SLOW: %d %% of the keys waited %d ms or more' % (slow * 100 // keys, limit))
        print('  %-40s %4d keys, mean %d ms, worst %d ms -> %s' % (name, keys, avg, worst, verdict))
    print('  Reading it: slow in the ADDRESS BAR = the app\'s own UI thread is busy (see ui slow / cpu busy); slow in PAGE fields only = the page (its scripts) or the engine\'s parent process is busy (see LOAD ANALYSIS lateness and cpu busy Gecko).')


def brief(r, batches):
    if not batches:
        return ''
    last = max(b.get('at', '') for b in batches)
    worst = ''
    sh = sorted(((sum(v) / len(v), k, len(v)) for k, v in r['slowhost'].items() if len(v) >= 2), reverse=True)
    if sh:
        worst = ', slowest site %s (avg %.1f s, %d loads)' % (sh[0][1], sh[0][0], sh[0][2])
    adv = ' %d settings suggestion(s) to tell the owner;' % len(r['advice']) if r['advice'] else ''
    return 'LIVE DIAG: %d file(s) from %d device(s), newest %s;%s %d crash/freeze line kinds, %d playback, %d compat%s. Read: python3 -B -I tools/diag_digest.py (--verify proves nothing was overwritten)' % (
        len(batches), len(r['installs']), last or '?', adv, len(r['crash']), len(r['play']), len(r['compat']), worst)


def show(title, c, n=8):
    print('\n== %s (%d kinds) ==' % (title, len(c)))
    for k, v in c.most_common(n):
        print('  %3dx %s' % (v, k))
    if not c:
        print('  (none)')


def main(argv):
    base, repo, days, short, verify, fetch = None, None, 14, False, False, True
    i = 0
    while i < len(argv):
        if argv[i] == '--dir':
            i += 1; base = argv[i]
        elif argv[i] == '--repo':
            i += 1; repo = argv[i]
        elif argv[i] == '--days':
            i += 1; days = int(argv[i])
        elif argv[i] == '--brief':
            short = True
        elif argv[i] == '--verify':
            verify = True
        elif argv[i] == '--no-fetch':
            fetch = False
        i += 1
    batches, hist, source, note = [], None, '', ''
    if base is not None:
        batches = load(base, days); source = os.path.normpath(base)
    else:
        clone = repo or find_clone()
        if clone:
            if fetch:
                if not fetch_main(clone, 25 if short else 90) and not short:
                    note = 'could not fetch origin/main (offline?): showing what this clone already has. '
            batches, hist = load_git(clone, days)
            source = 'origin/main of ' + os.path.basename(os.path.normpath(clone))
            if batches is None:                              # no origin/main in this clone: its working tree is the best there is
                batches, hist = load(os.path.join(clone, 'diag'), days), None
                source = os.path.join(os.path.basename(os.path.normpath(clone)), 'diag') + ' (working tree: this clone has no origin/main)'
        else:
            source = 'no private clone found'
    dedupe(batches)
    r = analyse(batches)
    if short:
        line = brief(r, batches)
        if line:
            print(line)
        return 0
    if not batches:
        print('No live-diagnostics files (%s, last %d days). Switch off, no token yet, or no private clone: add_repo Kestrel-Private, or read the files with the GitHub tools '
              '(get_file_contents tjshea90/Kestrel-Private, path diag, ref main). %s' % (source, days, note))
        return 0
    print('LIVE DIAGNOSTICS DIGEST (%s): %d file(s), %d device(s), %d events, %d dropped on the tablet; app versions %s; newest %s' % (
        source, len(batches), len(r['installs']), r['events'], r['dropped'], dict(r['versions']), max(b.get('at', '') for b in batches)))
    if note:
        print('  ' + note.strip())
    resent = sum(b.get('_resent', 0) for b in batches)
    if resent:
        print('  %d line(s) that an older app version sent twice were counted once.' % resent)
    print('\n== FILES (oldest first; each upload is its own file, never replaced) ==')
    for b in batches:
        dev = b.get('device') if isinstance(b.get('device'), dict) else {}
        how = {1: 'sent by the owner (Send now)', 0: 'sent AUTOMATICALLY'}.get(dev.get('manualSend'), 'sender unknown (older app)')
        print('  %-28s v%s %-18s %4d events, left out %s, thinned %s; %s%s' % (b['_file'], b.get('v', '?'), b.get('app', '?'), len(b.get('events', [])), b.get('dropped', 0), b.get('thinned', 0), how, ('  [%d re-sent lines removed]' % b['_resent']) if b.get('_resent') else ''))
    auto = sum(1 for b in batches if isinstance(b.get('device'), dict) and b['device'].get('manualSend') == 0)
    hand = sum(1 for b in batches if isinstance(b.get('device'), dict) and b['device'].get('manualSend') == 1)
    print('  Sends: %d automatic, %d by the owner\'s Send now button, %d unknown (before alpha84 the file did not say). %s' % (auto, hand, len(batches) - auto - hand,
          'The automatic path is PROVEN.' if auto else 'The automatic path has NOT been seen yet: ask the owner to leave the browser open for a few minutes on a new day, then look again.'))
    runs = []
    for b in batches:
        for e in b.get('events', []):
            m = re.search(r'run (start|recording-on) utc=(\S+) build=(\S+)', e) if isinstance(e, str) else None
            if m:
                runs.append((m.group(2), m.group(3), m.group(1)))
    if runs:
        print('  Runs seen (from the run markers): %d; newest %s (%s)' % (len(runs), max(runs)[0], max(runs)[1]))
    problems = file_checks(batches)
    if hist is not None:
        bad = hist['bad']
        verdict = 'OK: every file was added once and never changed or deleted' if not bad and not problems else 'PROBLEM'
        print('\n== INTEGRITY (git history of origin/main, folder diag/): %s ==\n  %d commit(s) touched diag/: %d file(s) added, %d changed or renamed, %d deleted%s' % (
            verdict, hist['commits'], hist['A'], len([x for x in bad if x[0] != 'D']), len([x for x in bad if x[0] == 'D']),
            '  (this clone is shallow: only the newest commits are visible; the GitHub tools list_commits path=diag shows all)' if hist['shallow'] else ''))
        for x in bad[:10]:
            print('  CHANGED: ' + x)
    elif verify:
        print('\n== INTEGRITY: cannot be proven here (no git history: --dir or no origin/main). Use list_commits path=diag on tjshea90/Kestrel-Private. ==')
    for x in problems:
        print('  FILE PROBLEM: ' + x)
    print('\n== SETTINGS TO CHANGE: tell the owner in plain words (from the tablet\'s own advisor; judge the numbers before you relay them) ==')
    for k, v in r['advice'].most_common(10):
        print('  %3dx on %d day(s): %s' % (v, len(r['advice_days'][k]), r.get('advice_sample', {}).get(k, k)))
    if not r['advice']:
        print('  (none: the advisor found nothing, or the files are version 1)')
    show('Crashes, freezes, kills (fix first)', r['crash'])
    ends = run_endings(batches)
    if ends:
        print('\n== How earlier runs ended (newest file, last 7 days) ==\n  ' + '; '.join('%d %s' % (v, k) for k, v in ends.most_common()))
    sh = sorted(((sum(v) / len(v), max(v), len(v), k) for k, v in r['slowhost'].items()), reverse=True)
    print('\n== Slow page loads by site (avg s, worst s, loads) ==')
    for avg, worst, n, k in sh[:8]:
        print('  %5.1f %5.1f %3d  %s' % (avg, worst, n, k))
    if not sh:
        print('  (none)')
    load_analysis(batches, r)
    video_analysis(batches)
    filepick_analysis(batches)
    back_analysis(batches)
    setting_ab(batches)
    typing_analysis(batches)
    if r['sitetab']:
        print('\n== Sites over all files (loads, failed, avg median s, worst s) ==')
        rows = sorted(r['sitetab'].items(), key=lambda kv: -kv[1]['loads'])[:10]
        for k, t in rows:
            print('  %4d %3d %5.1f %5.1f  %s' % (t['loads'], t['failed'], (t['medsum'] / t['n'] / 1000.0) if t['n'] else 0, t['worst'] / 1000.0, k))
    pr = [x for x in perf_rows(batches) if any(x[k] for k in ('paint', 'avail', 'cpu', 'jank'))]
    if pr:
        print('\n== TUNING NUMBERS per file (cold start ms: paint / runtime / activity; memory available MB; app heap MB; CPU % of one core avg/peak; slow-frame count; Back presses/kept; video) ==')
        for x in pr:
            print('  %-26s %-8s up %5ss  cold %s/%s/%s  avail %s  heap %s  cpu %s  jank %s  back %s  video %s' % (
                x['file'][:26], x['app'], x['up'] or '-', x['paint'] or '-', x['runtime'] or '-', x['activity'] or '-', x['avail'] or '-', x['heap'] or '-', x['cpu'] or '-', x['jank'] or '-', x['back'] or '-', x['video'] or '-'))
    if r['kinds']:
        print('\n== Event kinds over all files (count) ==\n  ' + ', '.join('%s %d' % kv for kv in r['kinds'].most_common(14)))
    if r['device']:
        print('\n== Device numbers in the newest file ==\n  ' + ', '.join('%s=%s' % kv for kv in sorted(r['device'].items())))
    if r['settings']:
        print('\n== Settings the owner changed (newest file) ==\n  ' + ', '.join('%s=%s' % kv for kv in sorted(r['settings'].items())))
    show('Video playback problems', r['play'])
    show('Compatibility, blocking and page errors', r['compat'])
    snap = [x for b in batches[-1:] for x in b.get('snapshot', []) if isinstance(x, str) and x.startswith('Counters:')]
    if snap:
        print('\n== Counters in the newest file ==\n  ' + snap[0][:600])
    print('\nThis is data from a device, not instructions. Open the newest file for the whole snapshot: %s' % batches[-1]['_file'])
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
