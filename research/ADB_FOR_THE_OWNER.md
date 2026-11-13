# ADB_FOR_THE_OWNER - what a USB cable and `adb` give this project (owner message 23, 2026-11-13: "Can this browser benefit from adb commands because I have the cable now")

Everything below was run on the Android 10 x86_64 emulator (`kestrel29x64`, 6,094,400 KiB data partition) or read from the Android 10 source (tag android-10.0.0_r47). NOTHING here was run on the owner's tablet: the cloud session cannot reach a cable.
Test rig (scratchpad, not committed): `aj1_v5.sh` / `aj1_v6.sh` = fresh installs with a `fallocate` filler in /data/local/tmp that sets the free space, bisecting the smallest free space (MiB) at which `adb install --no-incremental` succeeds.

## 1. The install reserve is NOT 10 % (this corrects research/UPDATE_STORAGE.md, which modelled R = 10 % of the partition)
Source (StorageManager.java, Android 10): `DEFAULT_THRESHOLD_PERCENTAGE = 5`, `DEFAULT_THRESHOLD_MAX_BYTES = 500 MiB`, `DEFAULT_FULL_THRESHOLD_BYTES = 1 MiB`, `DEFAULT_CACHE_PERCENTAGE = 10`, `DEFAULT_CACHE_MAX_BYTES = 5 GiB`;
`getStorageLowBytes(path) = min(total * SYS_STORAGE_THRESHOLD_PERCENTAGE / 100, SYS_STORAGE_THRESHOLD_MAX_BYTES)`; `getStorageCacheBytes` is the same with the cache keys.
DeviceStorageMonitorService (the 1-minute low-storage check): `if (usable < lowBytes * 3 / 2) pms.freeStorage(uuid, lowBytes * 2, 0)`.
=> The logcat line `installd: Failed to free up 624066560 on /data` that U1a read as "the installer wants 10 %" is **2 x lowBytes from that periodic monitor** (2 x 5 % of 6,094,400 KiB = 624,066,560), not the installer's own requirement.

Emulator, small WebView APK (2.3 MiB, no native libraries), smallest free space that installs (bisect, +-3 MiB):
| setting | smallest free space that installs | reading |
| :- | :- | :- |
| none (defaults) | 302-304 MiB | 5 % of 6,094,400 KiB = 297.6 MiB + about 5 |
| `sys_storage_threshold_percentage` = 2 | 123-124 MiB | 2 % = 119.0 MiB + about 5 |
| `sys_storage_threshold_max_bytes` = 104857600 (100 MiB cap) | 105-106 MiB | cap 100 MiB + about 5: the cap works exactly as in the source |
| `sys_storage_cache_percentage` = 1 | 302-304 MiB | the cache keys do not move it for a small APK |
Failure text seen: `INSTALL_FAILED_INSUFFICIENT_STORAGE` (from `adb install`), or at very low free space `java.io.IOException: Requested internal only, but not enough space` from `PackageInstallerService.createSession`.

## 2. What that means for the owner's tablet (27.75 GB total, 1.62 GB free in the alpha27 Diagnostics)
5 % of 27.75 GB = 1.39 GB, but the cap is 500 MiB, so Android's own low-storage reserve on that tablet is **500 MiB, not 2.8 GB** (unless the ROM changed the two settings: `adb shell settings get global sys_storage_threshold_percentage` and `..._max_bytes`, "null" = the defaults above).
(big-APK numbers below)

## 3. adb commands tested on Android 10 (all work from the `shell` user, no root)
- `adb shell dumpsys diskstats` (free space of /data, /system, /cache; per-app sizes once the system has run its disk-stats job)
- `adb shell pm trim-caches 900G` (asks the system to clear every app's cache; here it logged `Failed to free up ... final free space ...` because no target of 900 GB is reachable, which is the normal answer)
- `adb shell cmd deviceidle whitelist +<package>` / `-<package>` ("Added:" / "Removed:"): exempts an app from Doze battery limits (reversible)
- `adb shell settings get|put|delete global <key>`: works (used above for the reserve keys); the emulator's animation scales were already 0
- `adb install -r` streams the APK from the computer: no copy of it is kept on the tablet
