# BUILDLOG - every shipped release, in order

Written ONLY by `tools/record-release.sh`, after GitHub Actions has built, signed and published a version - never by hand, never
before that run is green. `ship.sh` and `.github/workflows/android.yml` gate the next `versionCode` against this file, separately
for each app (`web` = Kestrel on the system WebView, `gecko` = Kestrel G on Firefox's engine): a line here is a claim that a release
EXISTS and is installable. The line format is `| tag | app | code N | time | note`.

The first two lines were built in the cloud sandbox BEFORE the move to GitHub Actions (2026-11-11) and delivered to the owner as chat
attachments; they are recorded so the next version codes are gated correctly. Both carry the same pinned certificate
(`signing/CERT_SHA256`), so GitHub-built releases install over them.

| kestrel-v0.1.0 | web | code 1 | 2026-11-11T01:47Z | built in the sandbox, delivered as a chat attachment (sha256 9f06059b52097017c28fa5791e5f85e790d822ce9d3419769ce3fab9d8a34704); Google default search, no-Google-services adaptations, hotspot fixes, 22 review findings fixed
| kestrel-g-v0.2.0-gecko-alpha1 | gecko | code 100 | 2026-11-11T02:45Z | built in the sandbox, delivered as a chat attachment (sha256 ecd42ae8d8e0885e77b56346e775ee7764a4cdabc4115611d3174ddbe57288d9); first Firefox-engine build, smoke-tested on an Android 10 x86_64 emulator only
| kestrel-v0.1.1 | web | code 2 | 2026-11-11T03:23Z | first build by GitHub Actions (run 42951742468): 0.1.0 plus an internal refactor so both builds share code; unit tests pass, not re-run on an emulator
| kestrel-g-v0.2.0-alpha2 | gecko | code 101 | 2026-11-11T03:23Z | first Kestrel G built by GitHub Actions (run 42951742468); same code as alpha1
| kestrel-g-v0.2.0-alpha3 | gecko | code 102 | 2026-11-11T04:53Z | Alpha 3 (run 42961586675): typing-freeze fixes, no animations, Diagnostics + Refresh screen, tuned engine settings, security pins; not yet run on the real tablet
| kestrel-g-v0.2.0-alpha4 | gecko | code 103 | 2026-11-11T06:48Z | Alpha 4 (run 42973747344, green): bottom dock, black Home screen, private/regular tabs separated, tab grid with live previews, bottom menu/Shields sheets, in-app updates; verified on the Android 10 emulator, not yet on the real tablet
| kestrel-g-v0.2.0-alpha5 | gecko | code 104 | 2026-11-11T08:50Z | alpha5: blue neon REAPER theme (black glossy + neon blue, ghost-flame Home, subtle pulsing glow with a Settings switch), full download manager (pause/resume, parallel segments, notifications, own Downloads screen), faster Shields/IO threads, rotation and dock fixes
| kestrel-g-v0.2.0-alpha6 | gecko | code 105 | 2026-11-11T16:16Z | alpha6: a page that Android frees to save memory now comes back where you were (dimmed preview instead of a white screen, scroll position kept); the decoded-image cache is capped at about 95 MB (it could grow to about 475 MB and set off memory-flush stutter); new Settings > Scroll feel and Diagnostics > Scroll test (measures smoothness), GPU info in Diagnostics; the neon glow and page previews stay out of the way while you scroll
| kestrel-g-v0.2.0-alpha7 | gecko | code 106 | 2026-11-11T17:58Z | alpha7: the in-app update now says exactly how much space to free (fixes the stuck 'insufficient storage' message) and retries from the file it already downloaded; download cards show 'MB of MB'; the neon glow visibly pulses (even with system animations off) incl. a ring around the address bar; pull down to refresh a page; scrolling: Smoother is the new default (paints about twice the screen around your finger, kept longer, shorter touch wait) + Diagnostics > Compare 3 feels
| kestrel-g-v0.2.0-alpha8 | gecko | code 107 | 2026-11-11T18:51Z | alpha8: finds why updates would not install over the old version - Android only finishes an install while a tenth of the tablet's storage stays free afterwards (about 1.4 GB free on a 16 GB tablet), which uninstalling first quietly fixed. The Updates screen now tells you the exact amount to free before it downloads anything, offers to delete old Kestrel installers from Downloads, Diagnostics shows the storage numbers
