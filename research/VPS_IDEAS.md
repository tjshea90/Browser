# VPS1: what an 8 GB / 2 vCPU VPS can and cannot do for Kestrel and for Claude (2026-11-26)
Owner, verbatim: "I now have a vps with 8gb ram and 2 high performance vcpus. How can this help with the kestrel project and claude". Nothing here is built yet: the owner picks (VPS1b).

## Facts this rests on (all from this repo / this session)
- Every Claude cloud session starts in an empty container. This session's had NO Android SDK (the environment setup script did not install it), so ship.sh's local app and gecko gates could not run; Gradle was rate limited by Maven Central (HTTP 429) and offline mode had no cache; the container restarted once and killed a background watcher.
- GitHub builds and tests (test-jvm about 12 min, test-js about 5 min, build-gecko about 3 min, whole run about 15 min), and the emulator labs run there too (x86 emulator with KVM, 2 guest cores, software graphics: research/LAB3_DAILY.md).
- The tablet's diagnostics arrive as JSON files in the private repo; Claude runs tools/diag_digest.py by hand each time the owner asks.
- Open question O11 (research/PROMPT_AUDIT.md): one scheduled job that compiles the ad-block lists so the tablet fetches one file instead of seven.
- The tablet's hotspot is capped at about 5 Mbit/s; LinkMeter's Fast Wi-Fi (a VPN, so it holds the tablet's one VPN slot) lifts the cap.

## Ranked ideas
1. Claude Code ON the VPS as a persistent dev box (biggest win for Claude). Install the Android SDK, JDK 21 and a Gradle cache once; keep the repo checked out; run `claude` in tmux over SSH, or `claude remote-control` in the repo folder so the session shows up in the Claude app. Saves: the missing-SDK problem, the 429s, cold Gradle starts (minutes per session), lost background watchers when a container restarts. Claude usage is the same as today (same login); the saving is wall-clock time and fewer failed attempts. Needs: the owner's own login on the VPS, a normal user (not root), a firewall that allows only SSH.
2. Nightly diagnostics job (biggest saving of Claude usage). A cron job on the VPS pulls the private repo, runs tools/diag_digest.py and tools/optimizer_digest.py, writes a one-page summary file, and compares it with yesterday's. Claude is only started when something got worse (drawing mode failing, crash count up, a new slow site). Today Claude spends a turn reading the digest every time. Needs: a token for the private repo kept on the VPS only.
3. Daily filter-list compile and mirror (O11). The VPS fetches the licensed lists, compiles the .kfs file with the existing Gradle tool, signs it, and publishes one small file. The tablet downloads one file over the hotspot instead of seven and skips the on-device compile. Needs: a signature check in the app (the app must refuse an unsigned list: do not skip this), and a small HTTPS endpoint or a GitHub Release asset.
4. Self-hosted GitHub Actions runner. Removes the minutes limit but NOT the time: a 2 vCPU box is slower than GitHub's runners for a Gradle build. The repo is PUBLIC: a self-hosted runner on a public repo must never run pull-request code (only manual dispatch, which is how the workflow already works). Only worth it if the minutes really run out.
5. Remote Gradle build cache or Maven mirror for CI and Claude sessions. Makes test-jvm faster after the first run. Moderate setup; do it after 1.
6. Exit server for the tablet's traffic (WireGuard or similar). Might replace LinkMeter, but it is unproven that it lifts the cap (depends on how the carrier detects tethering), it adds a hop, and the tablet still has only one VPN slot. A test (one speed test with and without) decides; not a code job.
7. Real-tablet testing from the VPS (adb over a private network). The biggest prize (every release today says NOT RUN on the tablet) but the hardest: Android 10 has no wireless pairing, so adb over the network needs a USB step after every reboot, the tablet's VPN slot is taken, and an open adb port is full control of the device. Park it.

## What does NOT help
- Reproducing the cut-off pictures and text: the emulator has no Mali graphics chip, on any host. Only the tablet can.
- Emulator speed: a 2 vCPU VPS with or without KVM is no faster than the GitHub lab runners.
- Speeding up pages on the tablet: the limits are the tablet's four slow cores and the 5 Mbit/s link.

## Safety rules for any of it
- No secrets in chat: keys and tokens go to the VPS or the environment settings, never into a message (INBOX and OUTBOX copy chat text into a repo).
- Dedicated non-root user, SSH keys only, firewall closed except SSH, automatic security updates.
- The VPS never holds the signing key for releases (GitHub does); it never gets the owner's photos or personal data.
- The browser itself stays free of accounts, sync and telemetry: the VPS serves the owner's tools, not the app's code path, except idea 3's signed list.
