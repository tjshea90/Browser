# UPDATE_STORAGE - why an update (or a fresh install) of Kestrel G needs about a gigabyte of free space (owner report U1, 2026-11-11)

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
Model that fits the numbers: **free before >= 10 % of the partition + staged APK + unpacked native libraries + optimised code** = 624 + 116 + 187 + ~100 = about 1030 MB for the emulator's debug build.
For the real release APK (92.6 MB, 131.7 MB of arm32 libraries because the APK stores them compressed and `extractNativeLibs=true`, minified dex so little dexopt output) that is **10 % of the tablet's partition + about 270 MB**, plus another 92.6 MB if our own downloaded copy is still on the disk.
On a 16 GB-class tablet (about 11 GB data partition) that is roughly 1.1 GB + 0.27 GB = **about 1.4 GB free**; the owner "made 900 MB" and fell short.

## Why "uninstall first" works
The uninstall frees the old install (about 250 MB) and its data and caches (up to 0.7 GB: the Gecko disk cache alone is 512 MB), so free space jumps by roughly a gigabyte and the SAME APK now clears the bar. Installing "fine from GitHub" was always after an uninstall.
In-place and fresh need the same free space BEFORE the install (the old version's bytes are already counted as used); uninstalling changes the "before".

## What the updater does about it (alpha8)
- `UpdateLogic.storagePlan`: needed free = **10 % of the partition (the installer's reserve) + unpacked libraries + staged APK + 64 MB margin**; the free space it compares with is Android's "free + caches the system may clear" (`StorageStatsManager.getFreeBytes`).
  The old model assumed a reserve of at most 500 MB, so it told the owner "it fits" at about 870 MB free and Android then refused.
- The Updates sheet says the real numbers ("Android keeps 1.1 GB free, the update needs 0.3 GB, you have 0.9 GB: free 0.5 GB more") and offers the quick wins: delete Kestrel installers it downloaded earlier (one per release, 92 MB each), Android's Storage screen.
- Diagnostics has a Storage line (free, installer-usable, reserve, this app's package / libraries / data / cache).
- A failure still raises the bar by 150 MB over what was free at the time ("floor"), so a wrong estimate converges.
- Not done (decided against): `extractNativeLibs=false` (GeckoView needs the libraries on disk; the APK would also grow by 62 MB on a metered link).

## Open
- Measure the release build (R8, x86_64) to replace the 64 MB margin for dexopt output by a number.
- Ask the owner for Settings > Storage (total, free) to confirm the 10 % figure on the tablet, and the exact error of a manual in-place install.
