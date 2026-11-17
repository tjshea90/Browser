#!/usr/bin/env python3
"""LAB7a: the baseline-profile variants the lab compares (and the one alpha66 may ship).

  python3 -I tools/emu/profvariant.py exercised|all IN.txt OUT.txt
  python3 -I tools/emu/profvariant.py --selftest

Android 10 compiles ONLY the methods marked HOT (H) when it installs an app with a profile (`speed-profile`, art/dex2oat ShouldCompileBasedOnProfile). The lab's profile marks about 2.5k of the 15k methods of the
app's dex as hot; the rest of the methods the lab ran carry only P (post start-up) or S (start-up) flags, which decide the ORDER of the compiled code, not whether it is compiled.

  exercised  every method line gets H in front (P -> HP, S -> HSP): everything the lab ever ran is compiled at install; the flags that order the code stay
  all        the file unchanged plus ONE wildcard rule `HL**;->**(**)**`: Android Gradle plugin's expandReleaseArtProfileWildcards expands it over every class, R8 rewrites the result to the final dex, and every
             method of the app is hot. A method named by several rules gets the union of their flags (measured: 2118 HSP, 2765 HP, 10113 H of 14996), so the start-up and post-start-up groups stay laid out first.
Standard library only; repository text stays ASCII.
"""
import re
import sys

ORDER = "HSP"
WILDCARD = "HL**;->**(**)**"


def promote(line):
    """A method line with the H flag added (flags kept); every other line unchanged."""
    if "->" not in line:
        return line
    m = re.match(r"^([HSP]*)(L.*)$", line)
    if not m:
        return line
    flags = set(m.group(1)) | {"H"}
    return "".join(f for f in ORDER if f in flags) + m.group(2)


def convert(text, mode):
    if mode == "exercised":
        return "\n".join(promote(ln) for ln in text.split("\n"))
    if mode == "all":
        body = text if text.endswith("\n") else text + "\n"
        return body + WILDCARD + "\n"
    raise ValueError("mode must be exercised or all, not %r" % (mode,))


def selftest():
    assert promote("PLfoo/Bar;->baz(I)V") == "HPLfoo/Bar;->baz(I)V"
    assert promote("SPLfoo/Bar;->baz(I)V") == "HSPLfoo/Bar;->baz(I)V"
    assert promote("HSPLfoo/Bar;->baz(I)V") == "HSPLfoo/Bar;->baz(I)V"
    assert promote("Lfoo/Bar;") == "Lfoo/Bar;"                        # a class line
    assert promote("") == ""
    assert promote("PLfoo/Bar;-><init>()V") == "HPLfoo/Bar;-><init>()V"
    assert convert("PLa;->b()V\nLa;\n", "exercised") == "HPLa;->b()V\nLa;\n"
    assert convert("PLa;->b()V\n", "all") == "PLa;->b()V\nHL**;->**(**)**\n"
    assert convert("PLa;->b()V", "all") == "PLa;->b()V\nHL**;->**(**)**\n"
    try:
        convert("", "x")
        raise SystemExit("bad mode accepted")
    except ValueError:
        pass
    print("profvariant selftest ok")


def main(argv):
    if argv[1:2] == ["--selftest"]:
        selftest()
        return 0
    if len(argv) != 4:
        print(__doc__)
        return 2
    with open(argv[2], "r", encoding="ascii") as f:
        text = f.read()
    out = convert(text, argv[1])
    with open(argv[3], "w", encoding="ascii", newline="\n") as f:
        f.write(out)
    print("%s: %d lines in, %d lines out" % (argv[1], text.count("\n"), out.count("\n")))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
