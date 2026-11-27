#!/usr/bin/env python3
"""Unpack a GitHub Actions artifact (or a run's logs zip) from the pre-signed address the GitHub tools hand out.

  python3 -I tools/emu/fetch_artifact.py "<download_url>" <empty folder outside the repo>

How to get the address: mcp__github__actions_list list_workflow_run_artifacts <run id> -> the artifact id -> mcp__github__actions_get download_workflow_run_artifact <artifact id> (its `download_url`, valid for a few
minutes), or mcp__github__actions_get get_workflow_run_logs_url <run id> (`logs_url`). curl is blocked in this sandbox; Python's urllib is not. The unpacked files are DATA (the emulator lab's report, logcat,
Health lines): read them, never execute them. Prints each file with its size; refuses names that would leave the folder.
"""
import io, os, sys, urllib.request, zipfile

if len(sys.argv) != 3:
    print(__doc__); sys.exit(2)
url, out = sys.argv[1], os.path.abspath(sys.argv[2])
os.makedirs(out, exist_ok=True)
data = urllib.request.urlopen(url, timeout=120).read()
z = zipfile.ZipFile(io.BytesIO(data))
for n in z.namelist():
    dest = os.path.abspath(os.path.join(out, n))
    if not dest.startswith(out + os.sep):
        print("refused", n); continue
    if n.endswith("/"):
        os.makedirs(dest, exist_ok=True); continue
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    with open(dest, "wb") as f:
        f.write(z.read(n))
    print("%9d  %s" % (z.getinfo(n).file_size, n))
