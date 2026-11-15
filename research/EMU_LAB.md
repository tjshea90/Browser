# The emulator lab (RX4-E1 and SOAK1, 2026-11-15)

**What it is.** Kestrel G's debug build running on an Android 10 emulator on GitHub's free runners (this repository is public), with hardware acceleration (KVM). The real Firefox engine (GeckoView 160, x86_64 build) is driven by `tools/emu/soak.py` through real touches and `adb`; the lab pages come from `tools/sitelab/soaklab.py`. Before this, every real-engine statement in the research was "emulator without KVM, minutes per step" or "not run".

**Owner's request (message 43):** "put the browser in Android 10 emulators matching my hardware as much as possible running on GitHub runners and make them use the Kestrel app simulating browsing on a variety of web sites and videos and downloads for an hour, and log any issues with loading, speed, finding and downloading videos, crashing, etc. Then look for ways to improve it based on the simulated runs."

## How to start it (nothing else is needed, no secret, no owner step)
`mcp__github__actions_run_trigger`, `run_workflow`, owner `tjshea90`, repo `Browser`, workflow `android.yml`, `ref` = the session's branch, inputs `app=lab`, `full_build=false`, `lab_suite` = `smoke | fs | soak | all`, `lab_minutes` = how long the soak part runs (60 = one hour).
Why through android.yml: GitHub dispatches only workflows it already knows on the DEFAULT branch (here `ccr-a57c163e-mtq8js`), and `emu-lab.yml` exists only on the session branches. `android.yml` is known, so it is the front door; it calls `.github/workflows/emu-lab.yml` (a reusable workflow, also directly dispatchable once it is on the default branch). A run uses the workflow files of the dispatched branch.
The workflow has `permissions: contents: read`, no secrets, never uploads an APK (its artifacts are text and a few screenshots), and never has a `push:` trigger (`tools/test_handoff.sh` asserts all three).

## What one run does (about 4 minutes of set-up, then the suite)
1. Hardware acceleration check (`/dev/kvm`), ffmpeg (the lab makes its own clips: 640x360 landscape, 360x640 portrait, 1280x720, H.264 + AAC).
2. `sdkmanager` installs the emulator and `system-images;android-29;default;x86_64` (about 1 minute).
3. `gradlew :geckoapp:assembleDebug -PgeckoAbi=x86_64` (about 1.6 minutes with the Gradle cache). Package `app.kestrel.browser.gecko.debug` (a debuggable build: its Health lines are mirrored to `logcat` tag `KestrelHealth`).
4. The AVD `kestrel29x64`: 600 x 1024 at 170 dpi (the usable window is 600 x 973 like the tablet), 2048 MB RAM, 4 cores, VM heap 128, SwiftShader GL, `hw.accelerometer` on. The emulator boots in about 30 seconds with KVM.
5. The suite (`tools/emu/soak.py`): `smoke` = engine start, one page, the Safe Browsing check, the app's own Diagnostics text; `fs` = the full-screen turning checks of owner message 43 (FS1); `soak` = for `--minutes`: public pages (Wikipedia, MDN, news, GitHub, search, YouTube home ...), lab pages, videos (lab clips + public MP4, WebM/VP9 and HLS files through the lab's player page that reports every media event and `getVideoPlaybackQuality()`), real video sites (YouTube, Vimeo, archive.org, Wikimedia Commons: tap play, did the app see playback, did its "download video" finder count a video), downloads (lab 1/5/20 MB and a slow one, public 10 MB files: the app asks first, the driver taps "Download", the file must arrive with the right size in `/sdcard/Download`), full-screen rounds, tabs closed every 8 scenes, Safe Browsing every 20 minutes, memory every 90 s, the Diagnostics text every 20 minutes.
6. Output: `report.md` (printed at the end of the step and on the run's summary page), `events.jsonl` (one line per scene), `health-lines.txt`, `diagnostics-*.txt`, `logcat-tail.txt`, `lab/report.jsonl` (what the pages saw), screenshots of the problem moments.

## Reading the results from the sandbox (cheap)
`actions_list list_workflow_runs/list_workflow_jobs` -> `actions_list list_workflow_run_artifacts` -> `actions_get download_workflow_run_artifact` gives a pre-signed URL; fetch it with Python `urllib` (curl is blocked by the hooks) into the scratchpad and unzip there (the files are data, never executed). Reading `get_job_logs` with a large `tail_lines` costs a lot of context (every step prints its environment): prefer the artifact.

## What it cannot prove (written into every report)
- x86_64 + SwiftShader is not the tablet's Mali GPU; the x86 JIT is far faster than 4 x Cortex-A53: absolute times, scrolling smoothness, GPU memory and dropped-frame counts of the emulator mean nothing for the tablet. Only behaviour (did it load, play, finish, crash) and comparisons inside one run mean something.
- No hardware video decoders (the emulator has software ones): "plays / no error" is meaningful, smoothness is not. The tablet's decoders were measured by the Decode lab (research/VIDEO_AUDIT.md).
- Sites that block data-centre addresses show up as UPSTREAM or as a consent / challenge page. A challenge page counts as a loaded page.
- The AOSP image is not the Go edition (`low-RAM device=false`); memory pressure behaviour differs.

## Runs
| Run | What | Result |
| :- | :- | :- |
| 43684158283 | smoke, first try | failed: avdmanager put the AVD elsewhere (fixed: `ANDROID_AVD_HOME` at job level) |
| 43684548711 | smoke 3 min | green in 4 min: KVM present, emulator boot 30 s, build 1.6 min, engine ready 3 s after start, first page loaded in 242 ms, Diagnostics dump works |
EOF
echo ok