# PDL1 - downloads a page makes itself (Proton Drive, blob: files)

Owner, 2026-11-28 (verbatim): "I tried to download from my proton drive but it gave me this error. It downloaded fine in brave".
The screenshot: the Downloads screen, two failed rows of a zip file from drive.proton.me, each "The site sent a web page instead of the file. The link may
have expired, or the site wants you signed in." (Retry / Delete), and one finished apk. Kestrel G (Firefox engine).

## 1. What the screenshot proves, and what is only read from the code

- The failure is `Reason.BAD_CONTENT` (core `Engine`: an html content type, or an html first block, where the file name says zip). It is NOT `BAD_URL`
  ("This address cannot be downloaded", what a blob: address gave before this change). So the address was http(s) and the download manager's OWN request
  to it returned a web page with status 200.
- How Kestrel G downloads: GeckoView calls `TabDelegates.onExternalResponse(session, response)` with the page's response (address, headers, a body stream).
  Before PDL1 the body was closed unread and the download manager (core `Engine`, fetcher kind 1 = `GeckoFetcher`) asked the SAME address again through
  `GeckoWebExecutor`. That second request is a plain network request: it never goes through the page's service worker.
- Proton Drive (NOT proven here, read from knowledge of its web client, no Proton account in the sandbox): the page downloads the encrypted blocks, decrypts them in a
  script and pushes the clear bytes into a service worker, which answers a navigation to its own address (`/sw/<id>`-like) with `Content-Disposition: attachment`.
  That address exists only inside the engine. Asked on the network it returns the site's single-page app shell (a 200 web page): exactly the BAD_CONTENT row.
  Brave (Chromium) saves the engine's own stream, so it works there.
- The same family: `blob:` files (the page builds the file in memory), `data:` files, one-time links (the engine's own request used the link up), answers to a form POST.

## 2. What GeckoView gives us (and the limits)

- `WebResponse.body` is an `InputStream` that stays suspended until it is read (no data is lost or buffered while nobody reads). `setReadTimeoutMillis` applies.
- Its Java-side queue has NO back-pressure (the reason `GeckoFetcher` asks for 4 MB windows): a reader that is slower than the sender piles data up in memory.
  So the engine's own stream is never throttled (no speed limit, no "yield to pages") and cannot be paused.
- NOT proven: what the stream does when the page's stream fails half way (an IOException, or a clean end?). The design therefore treats an end clearly short of the
  stated length as a cut (section 3.5).

## 3. The design (core `Handed.kt`, tests `HandedTest`, Gecko wiring `net/HandedOver.kt`)

1. `HandedStreams` (registry, at most 4 entries, 3 minutes, every unclaimed entry is closed = the engine's side is cancelled): `TabDelegates.onExternalResponse` OFFERS
   the body instead of closing it. A declined question, a quiet refusal, a private-tab "no", the phone taking the download over and a timer (`Handed.MAX_AGE_MS` + 5 s) all let it go.
2. Which fetcher the download starts with (`HandedOver.kindFor`): `Handed.cannotBeAskedAgain` = no web address (blob:, data:) or NO `Date` header (every web server states the
   date; a response a service worker made up has only the headers its script set) -> `FETCHER_ONE_SHOT` (5): the engine's stream is the only source, no second request,
   the phone is skipped, the download jumps the waiting line (`startNow`). Otherwise `FETCHER_HANDED` (4): the site is asked first as before (several connections, pause, resume,
   the phone) and the engine's stream is the FALLBACK.
3. `HandedFetcher` (kind 4): the ordinary answer is used when it is a 2xx that is not a web page (content type, then a 512-byte look at the first bytes unless the engine's
   own answer was a web page too) and the engine's stream is then closed; otherwise (error status, web page, FetchException other than a safety refusal) the engine's stream is used.
   A safety refusal (certificate, https to http, address on the own network, malformed) is never worked around.
4. The engine marks the download `FETCHER_ONE_SHOT` (stored with the first checkpoint) as soon as it started from a one-shot response: ranges 0, no validators, the sink is not sized
   by the stated length, the segment is open ended (the file ends where the stream ends, a stated length too small cannot truncate it), no speed limit, the stall watchdog waits
   8 times longer (a page decrypts block by block), a break of the stream fails at once as `Reason.PAGE_FILE` (no retry loop), and the half file is removed.
5. `HandedResponse.Guard`: a stream that ends more than 2 % short of its STATED length is a cut, not a finished file (the page was closed, the engine killed). Up to 2 % short
   is accepted (a site may state the stored size), longer than stated is read to the end.
6. After a restart or a retry the stream is gone: the kind-5 download fails with `Reason.PAGE_FILE` ("The page made this file itself and its stream stopped, so it cannot be
   continued. Go back to the page and download it again."), the half file is removed, only Delete is offered (Retry could only fail again). The site is never asked.
7. A tab whose page handed a stream over is not put to sleep while a kind-5 download runs (`BrowserActivity.feedsDownload`: tab hibernation by the live-tab budget and by the
   memory callback): closing the page would cut the file.
8. Counters for the digest (no address, no site): `download.handed.offered` (a stream was kept), `.ordinary` (the site's own answer was the file, the stream was closed), `.used`
   (the engine's stream was served), `.gone` (a kind-5 download found no stream). Read them with `python3 -B -I tools/diag_digest.py` once the owner has tried a Proton download.

## 4. What was and was NOT run

- Run: `HandedTest` (17 tests, real `Engine` with fakes: a service-worker-like stream behind a web page, an ordinary link, a one-time link, a web page labelled octet-stream,
  a safety refusal, a blob: file, a cut stream, a stream short of its stated length, a stated length too small or slightly off, a very low speed limit, a restart, the registry),
  the full `:core:test`, the Robolectric app tests and the compile of both app modules.
- NOT run: anything with a real Proton Drive account or a real Firefox engine (none in the sandbox); whether Proton's service worker response really has no `Date` (if it has,
  kind 4 still saves it through the fallback, only the phone offload and one wasted request differ); GeckoView's behaviour when a page stream fails half way; the tablet itself.
- The real proof is the owner trying the same zip again on the tablet (Kestrel G). Kestrel (the WebView build) is NOT changed: Android's download listener hands over only an
  address, never the page's stream, so a service-worker download cannot be saved there (use Kestrel G for such sites).

## 5. Ideas left out (cheap to add if the tablet shows a need)

- A progress bar from the stated length is already shown (the length is used for display only); a page that states none shows "N MB downloaded".
- Keep-ready service while a kind-5 download runs and the app is away (the download service already keeps the process; the engine's page process is separate).
