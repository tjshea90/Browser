# Checkpoint ladder & cold-resume protocol

Assume Claude can be cut off at ANY moment (usage limit, worker restart, container reclaimed). Work is safe only
once it is **pushed to origin**. Every level below ends with a push; the higher levels also leave a better map.

| Level | What it does | Trigger | Run by | Command |
|---|---|---|---|---|
| **L0 autosave** | `wip(autosave)` commit + push | automatic: after Write/Edit/Bash (>=120 s apart), on Stop, on PreCompact; SessionStart recovers a dirty tree | hooks | `scripts/autosave.sh [--force]` |
| **L1 step** | refresh STATE checkpoint block, journal line, commit, push, **verify origin has it** | every finished unit; at least every ~15 min of work | Claude | `scripts/checkpoint.sh "summary"` |
| **L2 milestone** | L1 + commit subject `checkpoint(milestone <name>): ...` (STATE phase table + RESUME HERE must be updated first) | end of each phase | Claude | `scripts/checkpoint.sh --milestone p3 "summary"` |
| **L3 release** | L2 + commit subject `release vX.Y.Z: ...`; requires `verify.sh full` green and APK in `dist/` | shippable build | Claude | `scripts/checkpoint.sh --release 0.1.0 "summary"` |

**No git tags:** this environment's git proxy answers HTTP 403 to any non-branch push, and tags would not survive a container reset. Find
milestones with `git log --grep='^checkpoint(milestone' --grep='^release v' --oneline` (resume.sh prints them).

`checkpoint.sh` prints `CHECKPOINT OK ...` only after `git ls-remote` shows origin at the local HEAD. Anything else
(`LOCAL-ONLY`, `WARN`) means the work is NOT safe — retry `git push -u origin <branch>` before continuing.

## Rules
1. **Announce before you start**: add one line under STATE.md "IN PROGRESS" (what, which files, how you will know it is done).
2. **Evidence when done**: move it to "Done" with the command/test that proves it.
3. **RESUME HERE must always be executable by a stranger**: update it whenever the next step changes.
4. L2/L3 must be green (`scripts/verify.sh quick|full`). L0/L1 may be red — record it under "Known issues".
5. Never force-push, never rewrite pushed history, never push main/master, only the assigned branch. No PR unless asked.
6. Low on usage? Run an L1 checkpoint immediately with an accurate RESUME HERE, then continue.
7. Secrets: `.gitignore` + the secret-guard in `lib.sh` keep key material out. The PUBLIC dev key in `signing/dev/` is intentional.

## Cold resume (new session)
1. SessionStart hook prints the report (or `bash scripts/resume.sh`): branch sync, recovered work, last commits, RESUME HERE.
2. Read `docs/handoff/STATE.md` (kept < 150 lines) and the tail of `JOURNAL.md`.
3. `git log --oneline -15`; compare "IN PROGRESS" with `git status` / the last `wip(autosave)` diffs. Finish it or revert it deliberately.
4. `bash scripts/verify.sh quick` to learn the baseline. 5. Continue with Next action #1. Do not redo "Done" items.

## Branch / push facts
- Assigned branch for this project right now: `ccr-a57c163e-mtq8js` (origin = github.com/tjshea90/Browser). Scripts push the *current* branch.
- Push retries: 2 s, 4 s, 8 s, 16 s backoff (`kst_push`). If the network is down, commits still accumulate locally; the next successful push sends all.
