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
