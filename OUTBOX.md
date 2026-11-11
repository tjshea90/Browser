# OUTBOX - what Claude told the owner (the last reply of every turn), newest last. Written by tools/capture_outbox.sh on the Stop hook; key-shaped strings are redacted and long replies cut in the middle. The mirror of INBOX.md.

## 2026-11-11T03:24:17Z
> Both APKs now build, sign and publish on GitHub, and your save system from Launcher and Portfolio is running in Browser. I haven't run either APK on your tablet.
>
> **Downloads**
>
> Kestrel G, the Firefox-engine build (pre-release):
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.2.0-alpha2
>
> Kestrel, the WebView build:
> https://github.com/tjshea90/Browser/releases/tag/kestrel-v0.1.1
>
> Open the page on the tablet and tap the `.apk`. Both are signed with the same key as the APKs I sent before, so they should install over them.
>
> **How the GitHub build works now**
> - The `Build APK` workflow tests, builds, signs and checks the certificate. It then creates the Release, with the APK and its sha256 attached.
> - The first full run took about 5 minutes for both apps.
> - You can start it yourself from the Actions tab (Run workflow). Pick `gecko`, `web` or `both`, tick `full_build`, and add a note.
> - It never runs on ordinary pushes, only when started manually or when a `kestrel-v*` / `kestrel-g-v*` tag is pushed. This follows your Launcher rule.
> - GitHub creates the release tag itself, because tag pushes from the Claude sandbox are refused.
> - I won't send APKs as chat attachments anymore. Releases are the delivery channel.
>
> **Save system**
> - It follows your Launcher and Portfolio setup: every tool call is autosaved and pushed, `ckpt.sh` records "did / next", and `resume.sh` briefs a cold start. `ship.sh` and `record-release.sh` handle releases.
> - It also saves what you type and what I reply (`INBOX.md`, `OUTBOX.md`), as in your other repos. This repo is public, so those are visible to anyone.
> - The hooks only start in the next session. I wrote your two messages from this session into `INBOX.md` by hand.
> - `TASKS.md` is the job list, `HANDOFF.md` is the page for a session with no context, and `BUILDLOG.md` records every release.
> - I left out the compaction-veto guards, since that plugin isn't in this environment.
>
> **Things to know**
> - Kestrel G alpha2 has the same code as alpha1, rebuilt by GitHub.
> - Kestrel 0.1.1 is 0.1.0 plus an internal refactor. The unit tests pass, but I haven't re-run it on an emulator.
> - The signing key is still the public dev key in the repo, so anyone could sign an APK that updates yours. A private key from repo secrets is supported but needs your say-so, because the pinned certificate would have to change.
> - The default branch is the current session branch, and `tools/MIRROR_MAIN` is off, so nothing goes to `main`. A later session starts from the latest work unless it works on its own new branch. Making `main` the default branch and mirroring to it would fix that, but that's your call.
>
> Next, please install Kestrel G alpha2 over alpha1 and tell me whether it installs in place. Then tell me how claude.ai, Google, speed and memory feel on the tablet. Until you do, I'm holding the next big step, the cosmetic ad-hiding index.
<!-- sha:aeed195790b6 -->
