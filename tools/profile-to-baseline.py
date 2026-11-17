#!/usr/bin/env python3
"""LAB5: turn the ART profile the emulator lab collected into geckoapp/src/main/baseline-prof.txt.

  python3 -I tools/profile-to-baseline.py <classes-and-methods.txt> [out.txt]

The lab (.github/workflows/emu-lab.yml, step "Keep the ART profile of this run") ends every run with `killall -s SIGUSR1 <pkg>` (ART writes its in-memory JIT profile) and
`profman --dump-classes-and-methods --profile-file=/data/misc/profiles/cur/0/<pkg>/primary.prof --apk=<base.apk>`; that text is the input. Every line is either a class (`Lpkg/Name;`) or a
method with flags (`HSPLpkg/Name;->method(Lsig;)V`: H = hot, S = startup, P = post-startup), exactly the grammar of an AGP baseline profile rule file, so the job is only to keep the valid lines,
drop duplicates and sort them (a stable file: the diff of the next refresh shows what changed). No comment lines are written (profgen's parser does not promise to accept them).

The result is compiled by AGP into assets/dexopt/baseline.prof (and .profm) of the release APK (R8 rewrites the names to the obfuscated ones), installed by androidx.profileinstaller
(5 s after the first frame) and turned into machine code by Android's idle-and-charging dexopt or `cmd package compile -f -m speed-profile`. Collected from the DEBUG build of the same code (names are
the original, unobfuscated ones; a debuggable app still saves its profile): the dex content is the same, only BuildConfig.DEBUG differs.
"""
import re
import sys

RULE = re.compile(r'^(?:[HSP]{0,3}L[A-Za-z0-9_$/.\-]+;(?:->[A-Za-z0-9_$<>\-]+\([A-Za-z0-9_$/;\[\]\-]*\)[A-Za-z0-9_$/;\[\-]+)?)$')   # '-' also inside a signature: the engine's synthetic accessor classes are named Outer-IA
# classes of the tools around the app that never run in a release build
DROP = ('Lapp/kestrel/browser/diag/DebugProps',)


def convert(text):
    out = set()
    for raw in text.split('\n'):
        line = raw.strip()
        if not line or not RULE.match(line):
            continue
        body = line.lstrip('HSP')
        if body.startswith(DROP):
            continue
        out.add(line)
    return sorted(out)


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    rules = convert(open(sys.argv[1], encoding='utf-8', errors='replace').read())
    dest = sys.argv[2] if len(sys.argv) > 2 else 'geckoapp/src/main/baseline-prof.txt'
    with open(dest, 'w', encoding='ascii', newline='\n') as f:
        f.write('\n'.join(rules) + '\n')
    methods = sum(1 for r in rules if '->' in r)
    print('%s: %d rules (%d methods, %d classes)' % (dest, len(rules), methods, len(rules) - methods))
    return 0


if __name__ == '__main__':
    sys.exit(main())
