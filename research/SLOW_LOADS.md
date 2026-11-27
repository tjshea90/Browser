# LD4: why claude.ai and YouTube load slowly on the tablet, and what the live diagnostics must record to prove it (2026-11-21)

Owner, verbatim: "Make sure the live diagnostics is comprehensive and tells claude a lot of information about how the app is running so claude can optimize the code as much as possible. Claude.Ai and YouTube load slow, even on fast wifi".
Evidence = the 4 real uploads in tjshea90/Kestrel-Private diag/2026-11-21 (alpha81 + alpha83, read with tools/diag_digest.py), the code, and the earlier notes. NOTHING here was measured on the tablet by Claude: every number is the tablet's own, quoted.

## 1. What the tablet measured (quoted)
| Fact | Number | Source |
| :- | :- | :- |
| claude.ai tab revived after the Home screen: first paint / done | 14.7 s / 28.5 s (onPageStart to FirstContentfulPaint / PageStop) | file 151240, `page loaded host=claude.ai paint=14722ms done=28523ms blocked=1` |
| claude.ai other loads | 31 s average, 35 s worst over 3 loads | digest "Slow page loads" |
| m.youtube.com | first paint 4.9 to 6.2 s, done 11 s (first start after an install) up to 28 s | files 081049, 085654 |
| gemini.google.com | paint 1.3 s, done 12.3 s | file 151240 |
| requests during one claude.ai VISIT (about 4 minutes, not necessarily one load) | 1,044 in the page report; the Blocker line counted 1,228 requests, 91 % from cache (1,115) | file 072850 |
| Shields judging, 46 min of claude.ai + YouTube | 2,423 requests judged, average 1.24 ms, slowest 53 ms, 279 over 2 ms | file 085654 Blocker line |
| Extension listeners (parent process main thread), same 46 min | req 2,425 calls = 3.95 s held (1.63 ms per call); rcv 0.30 s; snd 0.10 s; done 0.20 s; err 0.06 s | same line |
| CPU samples during loads (parent process, % of one core) | gemini load: `Cache2 I/O 31, Gecko 28, Socket Thread 27` (71 % total); claude.ai load: `Gecko 89, RenderThread 44, HeapTaskDaemon 12` (136 %); +125 s and +1930 s: `Classifier Upda 95` / `90` | `cpu busy` events |
| memory | available 356 to 1167 MB; trims level 20 every few minutes; `lowRamDevice=1` | `mem trim` events, device facts |
| link | Fast Wi-Fi profile through LinkMeter's tunnel (VPN active), HTTP/3 and IPv6 off, DNS by the phone; `downKbps=1048576` is only what the tunnel claims | snapshot "Network:" |
| UI thread stalls at start | `ui slow` 750 to 890 ms inside Paint.getRunAdvance / drawText (the app's own text), `ui late-beat` up to 1.3 s | events |

## 2. What this does and does NOT show
- A load that is 91 % from cache is not waiting on the hotspot: the "even on fast wifi" complaint fits a load that is bound by the tablet's CPU, not by the link. (The one-session cache ratio is from the alpha81 file; the 28 s claude.ai load itself has no per-request breakdown.)
- The blocking `onBeforeRequest` listener costs about 1.6 ms per request on this tablet, so a 1,000-request page pays about 1.6 s of parent-thread time for Shields (plus about 0.3 s for the other four listeners). That is real but about 5 to 7 % of a 28 s load: it is NOT the main cause. The judge already has the earlier fixes (gram-pair bitmap, per-host memo of the no-token rules): no change without tablet numbers.
- The Neon glow is not the cause: `NeonRules.busy` holds every glow still while a page loads or a video plays (diagnostics line "holding still").
- `Classifier Upda` at 90 to 95 % of a core is Gecko's URL-classifier table update (the engine's own strict tracking protection stays on by design, SECURITY.md); it showed twice in 46 minutes, 125 s and 1,930 s after the start. Whether it overlaps a page load is not known yet.
- NOT KNOWN (no data yet): how much of a slow load is the page's own JavaScript (the content process 'tab'), how much is waiting for the network, how many requests are slow and to which hosts, the protocol mix, whether the time is before the first byte (DNS / connect / TLS through the tunnel), and whether Shields on/off changes it for the same site. These are exactly what LD4b records.

## 3. Ranked hypotheses (to be decided by the new numbers, not by guessing)
1. CPU-bound page JavaScript on 4 x Cortex-A53 (claude.ai ships megabytes of script and 1,000+ requests): predicted `lag` (event-loop lateness) over 40 % of the load and requests mostly cached. Fix space: none in Settings; maybe fewer competing threads (Classifier Update, Cache2 I/O) or JIT/GC prefs, each by A/B.
2. Time before the first byte through the LinkMeter tunnel (tunnel CPU, TLS handshakes with HTTP/3 off): predicted `ttfb` or `con` large for the document and for the slowest requests. Fix space: warm connections (already `engine warm`), Load pages through the phone.
3. A few very slow third-party requests that keep `load` from firing (YouTube never "finishes"): predicted `r3` > 0 and slow hosts that are ad / analytics names; first paint (`fcp`) is what the person feels, not `done`.
4. Shields cost on big pages: predicted `rn x 1.6 ms` of the load; A/B by the per-site Shields switch, recorded as `shields=on|off` in every timing line.
5. Disk cache contention (`Cache2 I/O` 31 %): predicted many cached requests and high `rsum` on cached entries.

## 4. What LD4b records (one line per page load, never for a private tab, hosts only, sensitive sites never named)
`page timing host=... nt dns con tls ttfb fcp lcp dcl load rn rc kb r1 r3 rsum h2 h1 h3 js css img xhr lag=max/total/ticks nodes scr fr slow=host:kind:ms,... | shields net avail cpu tabs`: separates network (dns, con, ttfb, slow requests, r1/r3), page JavaScript (lag, nodes, scr), Shields (rn, shields) and the tablet's state (avail, cpu, tabs). Counts and milliseconds only; no path, query, title or text.

## 5. What shipped in alpha85 against this list, and what comes next
- Shipped: the instrument (section 4), the digest's verdict per site, the advisor rules. Nothing in the engine's settings was changed: with no measurement of WHERE the 28 s go, any such change would be a guess, and every earlier guess is already in gecko-prefs.txt.
- How to read the first answer: after one day of normal use, `python3 -B -I tools/diag_digest.py` > LOAD ANALYSIS. Verdict CPU-bound -> hypothesis 1 (look at what competes: the parent's `Gecko` thread, extension plumbing, Classifier Update); NETWORK-bound -> hypothesis 2 or 3 (first byte vs slow requests, and which hosts); SHIELDS-heavy or a large Shields A/B gap -> hypothesis 4. The owner can make the A/B himself: load claude.ai three times with Shields up, switch Shields off for the site, load it three times.
- Next steps by verdict (each its own TASKS row when it applies): CPU-bound with a saturated parent main thread -> a Network-lab switch that drops the extension's three bookkeeping listeners (completed, error, send-headers) to measure the plumbing; CPU-bound in the content process -> per-scriptlet cost counters; NETWORK-bound before the first byte -> the tunnel (LinkMeter) and warm connections; slow third-party requests -> name them in the lists.

