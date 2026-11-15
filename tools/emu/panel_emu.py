#!/usr/bin/env python3
"""Real touches for the floating mini panel on the Android emulator (owner message 41: close by a long press above it, a large X while it is carried).

glide_emu.py writes a whole gesture in one go (down, hold, move, lift); here the finger can stay DOWN between steps so a screenshot can be taken while the panel is being carried.

  python3 -I tools/emu/panel_emu.py longpress X Y [ms]      press and hold (screen pixels), lift
  python3 -I tools/emu/panel_emu.py tap X Y [ms]            a tap of ms (default 60) by raw events (a slow tap: 450)
  python3 -I tools/emu/panel_emu.py carry X0 Y0 X1 Y1 OUT.png [LIFT]   finger down at (X0,Y0), still for 800 ms, dragged to (X1,Y1), screenshot while it is still down, then lift only if LIFT is given
  python3 -I tools/emu/panel_emu.py shot OUT.png            screenshot (keep it OUT of the repository: the repo is public)

Nothing here is part of the app. Needs `adb root` (the emulator allows it).
"""
import os, struct, subprocess, sys, tempfile, time
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import glide_emu as g

def _push_and_play(chunks, pause=0.04):
    path, mx, my = g.touch_device()
    blob = b"".join(chunks)
    with tempfile.TemporaryDirectory() as td:
        f = os.path.join(td, "pe.bin"); open(f, "wb").write(blob); g.adb("push", f, "/data/local/tmp/pe.bin")
    cmds = []; at = 0
    for c in chunks:
        n = len(c) // 24
        cmds.append("dd if=/data/local/tmp/pe.bin of=%s bs=24 skip=%d count=%d 2>/dev/null; sleep %s" % (path, at, n, pause))
        at += n
    cmds.append("rm -f /data/local/tmp/pe.bin")
    g.shell("; ".join(cmds))

def _raw(x, y):
    path, mx, my = g.touch_device(); sw, sh = g.screen_size()
    return int(round(x / sw * mx)), int(round(y / sh * my))

def down(x, y, track=1):
    rx, ry = _raw(x, y)
    return g._ev(3, 0x2f, 0) + g._ev(3, 0x39, track) + g._ev(3, 0x35, rx) + g._ev(3, 0x36, ry) + g._ev(3, 0x3a, 100) + g._ev(0, 0, 0)

def move(x, y):
    rx, ry = _raw(x, y)
    return g._ev(3, 0x35, rx) + g._ev(3, 0x36, ry) + g._ev(0, 0, 0)

def up():
    return g._ev(3, 0x39, -1) + g._ev(0, 0, 0)

def line(x0, y0, x1, y1, step=8.0):
    return g.line(x0, y0, x1, y1, step)

def main(a):
    cmd = a[0]
    if cmd == "longpress":
        g.longpress(int(a[1]), int(a[2]), int(a[3]) if len(a) > 3 else 800)
    elif cmd == "tap":
        x, y, ms = int(a[1]), int(a[2]), int(a[3]) if len(a) > 3 else 60
        _push_and_play([down(x, y)]); time.sleep(ms / 1000.0); _push_and_play([up()])
    elif cmd == "carry":
        x0, y0, x1, y1, out = int(a[1]), int(a[2]), int(a[3]), int(a[4]), a[5]
        _push_and_play([down(x0, y0)]); time.sleep(0.9)                      # the long press: the panel is picked up
        pts = line(x0, y0, x1, y1)
        _push_and_play([move(px, py) for px, py in pts[1:]], pause=0.05)
        time.sleep(0.8); g.shot(out)                                         # the finger is still down: the X is on screen
        if len(a) > 6: _push_and_play([up()])
    elif cmd == "up":
        _push_and_play([up()])
    elif cmd == "shot":
        g.shot(a[1])
    else:
        print(__doc__); sys.exit(2)

if __name__ == "__main__":
    main(sys.argv[1:])
