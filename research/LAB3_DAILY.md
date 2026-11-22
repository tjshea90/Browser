# LAB3: the daily-use lab and the report for a build (owner message 62, 2026-11-17)

Owner: "Set up a github runner which emulates my hardware and os as closely as possible and stimulate regular usage of the browser. Track and log any errors, crashes, security problems, slow loading or scrolling, page loading issues, speed issues, feature problems, video playback errors or problems, etc. also look for ways to improve the browser, especially speed. put the findings into a report for a build"

This is NOT a new lab. It extends the lab of SOAK1/LAB2 (`.github/workflows/emu-lab.yml`, `tools/emu/soak.py`, `tools/sitelab/soaklab.py`, research/EMU_LAB.md) with: a device profile, one new suite (`daily`), security/feature probes, and ONE build-keyed report.

## Start it
`mcp__github__actions_run_trigger` run_workflow, owner tjshea90, repo Browser, workflow `android.yml`, `ref` = the session branch, inputs `app=lab full_build=false lab_suite=daily lab_minutes=45 lab_net=full lab_profile=auto`. About 60-90 minutes on a free runner (the repo is public). `lab_net=hotspot` re-runs it on the owner's 5 Mbit/s link shape (an extra run, not the main one). Check progress with `actions_list list_workflow_jobs` (small); `list_workflow_runs` prints ~20 KB of release notes per call, so ask for `perPage=1` with `workflow_runs_filter {"branch": ...}`.

## What it matches of the tablet, and what it cannot (the report prints this table from the run's own `getprop`)
- Matched: Android 10 / API 29, 600x1024 window at 170 dpi, 2 GB RAM, 128 MB heap request, font scale 1.2 (driver), no Google services, 2 guest cores of the runner's 4.
- Asked for with a read-back: `ro.config.low_ram=true` (emulator `-prop`; the workflow starts the emulator again without it if it refuses, the report says which).
- NOT matched, no way to match: the CPU (x86 under KVM is several times faster than 4 x Cortex-A53), the GPU (SwiftShader, not Mali-G52), the video decoders (software), the Go edition (the SDK has no Android 10 Go image), 32-bit userland (a 32-bit x86 image exists, but Mozilla publishes NO `geckoview-x86` for GeckoView 160: lab run 43997001918 died at Gradle with "Could not find org.mozilla.geckoview:geckoview-x86:160.0.20261104181552").
- Consequence: absolute times and smoothness are emulator numbers. Behaviour (loads, plays, refuses, crashes) and the CHANGE between two builds are what count. The `/bench` page (a fixed amount of JS, sort, JSON, regex, DOM, canvas work) is timed at the start and the end; the report divides times by the runner's speed relative to the baseline's.

## The day (tools/emu/daily.py, class DailyMixin; suite `daily`)
Runs on the app's REAL defaults (the older suites switch press-to-play, Kestrel's own player and the turning mode off with debug commands; `daily` does not). 15 steps, each timed, each with the Health lines / console errors / UI stalls / scroll gaps of that step attached: cold start, home screen (accessibility tree), a typed search (must go to Google), a typed address without a scheme + a long article read with flings, 5 tabs and the tab grid, three Back presses, typing in a form field, a film pressed in a page, a film on a real site, a download then the Downloads screen, the menu's own screens, a private tab + a Shields on/off A/B on one heavy page, leaving the app / returning / screen off and on, 8 heavy tabs then Android's RUNNING_CRITICAL warning, then the security and feature probes.
Security / feature probes (rows pass / fail / info in events.jsonl as kind `sec` / `feat`): pop-up without a tap, camera + microphone, location, notifications, clipboard read, WebRTC local-address leak, referrer sent cross-site, third-party cookie (set in a frame of another site, then looked for in a first-party visit), a cross-site frame navigating the top page, `tel:` `sms:` `mailto:` `market:` `intent:` ... without a tap, a `file://` page running script, bad certificates (expired, self-signed, wrong host, untrusted root), `http://` upgrade, mixed content, Safe Browsing pages, an installer that pretends to be Kestrel, downloads nobody tapped, plus fingerprint-surface facts (info). Pages: `/sec`, `/sec/frame`, `/sec/read`, `/sec/echo`, `/sec/scheme`, `/sec/framebust`, `/bench` in tools/sitelab/soaklab.py (validated in headless Chromium; the in-page verdicts assume Gecko's policy).
The lab saves the native view tree of key screens (`ui-home.txt`, `ui-tab-grid.txt`, `ui-menu.txt`, `ui-settings.txt` ...) in the artifact: the NEXT run's taps are tuned from these (the first run drives the address bar, tab grid and menu by their accessibility names and records honestly what it could not find).

## The report (tools/emu/buildreport.py)
`BUILD-REPORT.md` + `build-report.json` in the artifact `build-report` (30 days) and on the run's summary page: build identity (versionName / versionCode / commit / run), verdict per area (RED / AMBER / GREEN / NOT MEASURED), the hardware table above, findings ranked with a "where to look" hint, the day step by step, page-load / security tables, the app's own Diagnostics lines, the change since the last report (`research/lab-baseline.json`, normalised by the bench page), the hand-written improvement section (`--notes`), what was not measured.
Local use on any downloaded artifact: `python3 -I tools/emu/buildreport.py --out <dir with events.jsonl> --repo-root . --suite daily`.
New baseline after a run is accepted: add `--write-baseline research/lab-baseline.json` (then commit). Reports kept for the owner go to `docs/handoff/LABREPORT_<versionName>.md`.

## Runs
| Run | What | Result |
| :- | :- | :- |
| 43997001918 | daily, first dispatch | failed at Gradle: no geckoview-x86 (see above); the profile now keeps x86_64 |
| 43997607118 | daily, second dispatch (x86_64, low-RAM prop attempt, font scale 1.2) | see TASKS LAB3e |
