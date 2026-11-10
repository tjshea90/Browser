# Target hardware and what it forces on the design

Source: `docs/hardware/hardware_.txt` (supplied by the owner) plus the owner's statement that the tablet reaches the
internet **only through a phone hotspot**.

| Fact | Derived constraint |
|---|---|
| MediaTek MT8168B, Mali-G52 MC2 | Low-end quad-core in-order CPU class (public specs, not in the file). Keep main-thread work tiny, no startup JIT storms, ship a baseline profile if possible, keep hardware acceleration on. |
| Android 10 **Go** (API 29), "modified" build | `minSdk 29`. Anything newer must be behind `Build.VERSION.SDK_INT` guards (the same APK should still run on Android 16). Go = aggressive low-memory killer. |
| `ro.config.low_ram=true`, memory class **128 MB** | Per-app Java heap cap 128 MB; no `largeHeap`; keep our own heap < ~25 MB. 1 live WebView (+1 for an opener), hibernate the rest. Handle `onRenderProcessGone`, `onTrimMemory`. |
| `sys_mssi_t_32_ago` system image | Likely 32-bit userspace (armeabi-v7a). Ship **no native code** so ABI does not matter. |
| Security patch 2021-07-05, kernel 4.14.141 | OS is years behind on patches; the app cannot fix that. WebView version unknown and may be old -> detect, show in About, warn on NTP. |
| `ro.crypto.state=unencrypted` | App data at rest is unencrypted: persist as little sensitive data as possible, `allowBackup=false`, private window leaves nothing, recommend a screen lock. |
| 600x973 @170 dpi, font scale 1.2 | ~564x916 dp portrait tablet (below `sw600dp`), mdpi resource bucket: vector icons, `sp` text, >=48 dp targets, honour font scale. |
| `getMaxSupportedUsers=1` | No multi-user handling needed. |
| Stock launcher Quickstep (Android 10) | Gesture navigation may be on: avoid edge-swipe gestures in our own UI. |
| **Internet only via phone hotspot** (owner) | Metered, variable latency, may drop or have "no internet"; carrier sits in the path. -> data-frugal defaults, cache-first when offline, never reload background tabs at start, HTTPS-first (cleartext can be tampered with), confirm large downloads, data-used counter, no background traffic except a tiny weekly conditional list refresh, helpful "hotspot has no internet" error page. |

Unknown (read them from the device later via the in-app About page): total RAM, WebView provider/version, Google Play Services
presence (affects Safe Browsing), free storage, true ABI.
