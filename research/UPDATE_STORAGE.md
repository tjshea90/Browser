# UPDATE_STORAGE - why an update (or a fresh install) of Kestrel G needs about a gigabyte of free space (owner report U1, 2026-11-11)

> **CORRECTION (2026-11-13, alpha31; measured on the Android 10 emulator and read from the Android 10 source: `research/ADB_FOR_THE_OWNER.md`).** The "10 % of the partition" reserve below is WRONG. The logcat line `installd: Failed to free up 624066560` is the periodic storage monitor (DeviceStorageMonitorService) asking for 2 x the low-storage threshold (2 x 5 %), not the installer's own need. The installer's reserve is `StorageManager.getStorageLowBytes` = min(5 % of the storage, 500 MiB) (settings `sys_storage_threshold_percentage` / `sys_storage_threshold_max_bytes`), and an install needs free space >= reserve + 2 x (package + unpacked libraries) (small app 302-304 MiB, Gecko 95.5/187.4 MiB APK 861-868 MiB on a 5.8 GiB partition; 2 % moved both down by the same 178 MiB; the cache keys moved nothing; 20 % stayed at the 500 MiB cap). On the owner's 27.75 GB tablet the reserve is 500 MiB, so the update needs about 1 GB, not 3.1 GB, and 1.62 GB free should already fit. The 2 x APK slope below was right; the 10 % and the +32 MiB were not. Kept below as the historical record.

Owner after alpha7: "The newer versions never install over the old ones. I always have to uninstall the old one first. I don't think free space was the issue because I installed it from github fine."

## What is NOT the cause
- **Signing certificate.** Every GitHub Release (kestrel-v0.1.1, alpha2 ... alpha7) carries the same certificate, SHA-256 `641aca0b...52b`, verified by CI on the exact APK (`tools/verify-apk.sh`, `signing/CERT_SHA256`). A mismatch would also say INSTALL_FAILED_UPDATE_INCOMPATIBLE, not INSUFFICIENT_STORAGE.
- **versionCode.** 101 (alpha2) ... 106 (alpha7), strictly increasing (the workflow gates it).

## What it is: Android's install-time storage rule, which is much stricter than "APK size"
Measured on the Android 10 x86_64 emulator (`adb install -r`, i.e. the same PackageInstaller path; data partition 6,094,400 KiB; debug APK 116 MiB with 187.4 MiB of native libraries; filler file in /data/local/tmp to set the free space):

| free space BEFORE the install | in-place update | fresh install (after uninstall) |
| :- | :- | :- |
| 1500 / 1150 / 1082 / 1050 MB | OK | (1857 MB: OK) |
| **1000 MB** | **INSTALL_FAILED_INSUFFICIENT_STORAGE** | **same failure** |
| 850 / 700 / 600 MB | fails | fails |

`logcat` on every failure: `installd: Failed to free up 624066560 on /data; final free space ...` and 624,066,560 B is **exactly 10 % of the data partition** (6,094,400 KiB x 1024 / 10). So the installer will not finish unless, AFTER the install has taken its own space, at least 10 % of the partition is still free
(it first tries to clear every app's cache, then gives up). `sys_storage_cache_percentage = 5` did not move the threshold, so it is not that setting.
FINER MEASUREMENTS (fresh installs of an x86_64 RELEASE build, fallocate fillers, `adb install --no-incremental`; MiB; reserve R = 595.2 MiB): release APK A = 95.5 MiB with L = 187.4 MiB of libraries passes at 1010, fails at 1000;
the same APK PADDED to A = 155.5 MiB (60 MiB of incompressible data added to the zip and re-signed) passes at 1130, fails at 1115. So the threshold rises **2 MiB per MiB of APK**: **T = R + 2A + L + ~32 MiB** (the APK is held twice - the installer's staged copy and the installed one - then the unpacked libraries and the optimised code).
Release: 595.2 + 191 + 187.4 + 32 = 1005.6 (measured 1000-1010); padded: 595.2 + 311 + 187.4 + 32 = 1125.6 (measured 1115-1130); debug build (A = 116): predicted 1046 (measured 1000-1050).
For the real arm32 release APK (A = 92.6 MiB, L = 131.7 MiB) that is **T = R + 365 MiB** with our own downloaded copy still on the disk (R + 272 MiB once it is deleted after the installer took its own: that frees one APK).
On a 16 GB-class tablet (about 11 GB data partition, R about 1.1 GB) that is **about 1.4-1.5 GB free**; the owner "made 900 MB" and fell short.

## Why "uninstall first" works
The uninstall frees the old install (about 250 MB) and its data and caches (up to 0.7 GB: the Gecko disk cache alone is 512 MB), so free space jumps by roughly a gigabyte and the SAME APK now clears the bar. Installing "fine from GitHub" was always after an uninstall.
In-place and fresh need the same free space BEFORE the install (the old version's bytes are already counted as used); uninstalling changes the "before".

## What the updater does about it (alpha8)
- `UpdateLogic.storagePlan`: needed free = **10 % of the partition (the installer's reserve) + 2 x APK + unpacked libraries + 48 MB margin** (minus one APK once our own copy is deleted); the free space it compares with is Android's "free + caches the system may clear" (`StorageStatsManager.getFreeBytes`). (alpha8 still had one APK and a 64 MB margin: about 90 MB too optimistic; fixed in alpha9.)
  The old model assumed a reserve of at most 500 MB, so it told the owner "it fits" at about 870 MB free and Android then refused.
- The Updates sheet says the real numbers ("Android keeps 1.1 GB free, the update needs 0.3 GB, you have 0.9 GB: free 0.5 GB more") and offers the quick wins: delete Kestrel installers it downloaded earlier (one per release, 92 MB each), Android's Storage screen.
- Diagnostics has a Storage line (free, installer-usable, reserve, this app's package / libraries / data / cache).
- A failure still raises the bar by 150 MB over what was free at the time ("floor"), so a wrong estimate converges.
- Not done (decided against): `extractNativeLibs=false` (GeckoView needs the libraries on disk; the APK would also grow by 62 MB on a metered link).

## Open
- (done) the optimised-code margin measured 31 MiB for the release build.
- Ask the owner for Settings > Storage (total, free) to confirm the 10 % figure on the tablet, and the exact error of a manual in-place install.

## 2026-11-13: the owner's own tablet numbers (alpha27 Diagnostics, message 21) and what the Updates sheet now does
- The tablet: 1.62 GB free of 27.75 GB, the app holds package 94.4 + unpacked libraries 131.7 + data 62.3 + cache 228.1 MB. The reserve (a tenth) is 2.78 GB, so an in-place update of the 94.4 MiB alpha28 needs about 3.1 GB free (reserve 2.78 + staged copy 0.09 + installed copy 0.09 + libraries 0.13 + about 0.05 compiled code): **about 1.5 GB short**. The rule itself is MEASURED on the emulator (above) and consistent with the owner's long-standing "newer versions never install over the old ones"; the tablet's own installer is the judge, and the first refusal prints Android's number.
- "Install the other way" (uninstall first) hands back the old install (package + libraries + data + cache = about 0.52 GB, `StorageStatsManager.queryStatsForPackage` for the app itself needs no permission) and a fresh install needs the same ~3.1 GB, so the owner's tablet would still be **about 1.1 GB short** after the uninstall: the old sheet offered that path as "always worked", which would have left the tablet with NO Kestrel G and Android still refusing the saved file. `UpdateLogic.otherWayPlan` (core, tested with the owner's real numbers) now decides; the sheet only offers the other way when it fits and otherwise says how far short it would still be.
- New in the NEEDS_SPACE sheet: honest numbers (free now, reserve, what the install needs, how much more), "Clear cache (N MB)" (engine caches: `StorageController.clearData(ALL_CACHES)`), "Delete old installers", the three biggest downloads with a button to the Downloads screen, a Back up & restore reminder before an uninstall. New in the AVAILABLE / READY sheets: a heads-up BEFORE the download when the tablet is short (a hint only: the owner's alpha18 rule stands, the installer is always tried) so nobody spends 99 MB of hotspot data on a file that is then refused. Diagnostics' Storage line ends with the same verdict for an update of the installed size.
- Found while testing: the updater measured the partition through its own `files/updates` folder, which only exists after the first download; a missing folder answers 0 bytes total and free, so on a fresh install the reserve was 0 and the estimate said "fits". It now measures `filesDir`.
- Real Android 10 behaviour seen on the emulator filled to ~800 MB free: `PackageInstaller.Session.openWrite(name, 0, size)` was NOT refused for a 98 MB file (the allocation check does not include the reserve); a random file then failed at commit with "not a valid app" (parse comes before the storage rule), and `adb install -r` of the 129 MB debug APK at ~710 MB free failed (the reserve rule applies at install). So a refusal only shows up for a REAL APK, after the download: the heads-up before the download is the only place the owner can still save the data.
