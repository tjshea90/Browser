# BACKUP - Settings > Back up & restore (owner U1 / AA3-7: "so an uninstall-first loses nothing")

Why: updates sometimes need the old app removed first (storage; research/UPDATE_STORAGE.md), and uninstalling erases bookmarks, history, settings and open tabs. There are no accounts or sync in Kestrel (owner decision), so a plain file is the way out. Tags: **[V]** verified (JVM / Robolectric / Android 10 x86_64 emulator), **[N]** not verified on the real tablet.

## What the file is
`Kestrel backup <date>.json`, one JSON object (`core/backup/BackupCodec`), version 1:
- `settings`: only the choices that were really made (a default nobody touched stays a default), only keys on the allow-list in `data/BackupManager` (search engine, HTTPS mode, Shields switches, data saver, dark pages, text size, neon, scroll feel, tabs kept, DNS / HTTP/3 / preconnect, download choices, pull to refresh, suggestions, clear on exit, update choices). **Not carried on purpose:** the phone helper's pairing code and choices (re-pair on the new install), counters and update bookkeeping, the engine switches meant for debugging.
- `siteShields`: the per-site Shields store as text. `bookmarks`, `history` (the 5,000 most recent entries), `tabs` (regular tabs only, never private ones).
- No cookies, logins, passwords or downloads. The file is plain text in Downloads: anything can read it.

## Back up now
One tap writes the file into the **Downloads** folder through MediaStore (API 29+: no picker, no permission). Shared storage survives uninstalling the app, which is the point. A second backup on the same day becomes `... (1).json`. The message names the real file name and what it holds.

## Restore from a backup file
The system file picker (ACTION_OPEN_DOCUMENT: no permission), then the file is read with a hard 12 MB cap and decoded under strict checks, then the person is asked with what it holds ("3 bookmarks, 4 history entries, 4 settings, 2 site choices and 2 open tabs ..."), then it is applied: bookmarks and history are ADDED (an existing bookmark keeps its title, an existing history entry keeps the larger visit count and the later time), settings and per-site Shields are SET from the file, open tabs are added as tabs that load when opened (never a burst of pages over the hotspot; pages already open are skipped). Refused in a private window.

## A backup file is untrusted input (`BackupCodec.decode`)
Not JSON / wrong `format` / newer `version` / duplicate JSON keys / nesting deeper than 8 -> refused in plain words. Otherwise strict and forgiving: only http(s) addresses (never `javascript:`, `file:`, `content:`, `intent:`, `data:`), no spaces or control characters, at most 2,048 characters, titles clipped to 300, duplicates dropped, counts capped (20,000 bookmarks, 20,000 history, 200 tabs, 200 settings), wrong types skipped. Settings can only set allow-listed keys with the right type and range (`BackupManager.applySettings`); a hostile file cannot touch the pairing code, counters or anything else.

## Tests
`BackupCodecTest` (13), `BackupManagerTest` (9: full round trip into a "fresh install", restoring twice, a file can never set anything outside the list, the history cap), `BackupFlowTest` (8: the question and the result, cancel, not-a-backup, too big, unreadable, closed picker, private window, the picker intent). The Back up half writes through MediaStore and is checked on the emulator.

## Emulator check [V] (Kestrel G debug on Android 10 x86_64)
Seeded: 3 bookmarks, history, 4 choices (search = DuckDuckGo, ads off, text 130 %, neon 1), 2 site Shields, 7 tabs. **Back up now**: `/sdcard/Download/Kestrel backup 2026-11-12.json` (and `(1)` for the second one, 1,274 bytes) with exactly those contents. **Wiped the app's data** (`pm clear`), started fresh (1 tab, no data), Settings > **Restore from a backup file**, the picker listed both files, chose the `(1)` file: the question read "It holds 3 bookmarks, 4 history entries, 4 settings, 2 site choices and 2 open tabs", after Restore: the app held all 4 choices, both site Shields, 3 bookmarks, 4 history entries and 3 tabs (1 + the 2 distinct restored ones; the 7 tabs of the file were 5 duplicates).
Found on the way (test rig only): a hand-made `shared_prefs/<name>.xml.bak` next to the real file makes Android load the .bak and delete the real file; seeding test data must not leave one.

## Not done
Restore into the Kestrel (WebView) build was wired the same way and compiles, but only Kestrel G was run on the emulator. Tab restore keeps order and titles but not each tab's Back stack (a backup is not a session snapshot). Cookies and logins are out of scope by design. An optional "keep the last backup on a schedule" would need a background job; not built (no background work on the hotspot).
