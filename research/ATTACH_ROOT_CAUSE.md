# ATTACH_ROOT_CAUSE: "Photos still won't attach to claude website" (owner, 2026-11-21 IMG1 / ATT1, again 2026-11-22 PERF10, again 2026-11-23 PERF14)
Found 2026-11-23 from the alpha96 uploads (page-side probe) and the engine's own bytecode. Public repo: no file names, no private data here.

## What the uploads said (alpha96, two real attaches on claude.ai)
- Android side fine: `filepick picked type=image/png kb=119 size=600x1024 decodes=1`: the picker gave a readable 119 KB PNG.
- Page side broken: `filepick page-got host=claude.ai n=1 type=image/png kb=119 bytes=other decode=failed canvas=- reencode=-`. The page's `File` has the right type and size, but its first bytes cannot be read (`bytes=other`: the FileReader failed) and the picture cannot be decoded. A page that cannot read the bytes of its File cannot upload or process it: that is the site's "can't process images" (and the earlier Gemini attach that "just got stuck").
- alpha96 also logged `shields ext-version installed=1.0.197 wanted=1.0.197`: the probe code IS the running code, so the silence of alpha95 was the probe's own waiting (the `page-seen` beacon now arrives at once).

## Why (read from GeckoView 160's own classes with javap)
`GeckoSession.PromptDelegate.FilePrompt.confirm(Context, Uri[])` does NOT copy a picked file. It calls `getFile(context, uri)`, which turns a `content://` address into a **raw file path** (`DocumentsContract` document ids such as `primary:Pictures/...` become `/storage/emulated/0/Pictures/...`, other providers are asked for their `_data` column), puts the PATH strings in the prompt's result (`files`), and the engine opens those paths itself. On Android 10 and later an app may read a shared-storage file by raw path only with the storage permission (and on Android 11 and later only for media or with "All files access"); Kestrel G declared no storage permission at all. So the engine got a path it was not allowed to open: the File exists (name, type, size come from the path's metadata) but every read fails.
This is also why the owner's own guess ("give the browser all files permission") was right in the symptom: the app was missing exactly that.

## The fix (alpha97): never hand the engine a raw path
BrowserActivity copies every picked file, with the permission the picker GRANTED to the app for exactly that file, into the app's own private cache (`cache/picks/<id>/<safe name>`), and answers the prompt with `file://` addresses of the copies. The engine opens a path inside the app's own directory, which it may always read. No storage permission, no "All files access", works the same on Android 10 and 16, and also for documents (PDF, Word) that a storage permission would not have opened under scoped storage.
- Name: the picker's display name, cleaned (no slashes, no control characters, at most 80 characters), with an extension taken from the file type when the name has none (the engine takes the page's content type from the extension).
- Space: refused when the cache has less than the file's size plus 64 MB free; a hard cap of 1.5 GB per file.
- Lifetime: the page reads the copy when the person presses send (minutes later), so copies stay for 6 hours at most; all of them are removed at every start; copies made while a private tab was in front live in `picks-private` and are removed when the last private tab closes (PV1: nothing from a private tab is kept).
- The storage permission declared in alpha95 and the Settings row that opened Android's page for it are removed again: not needed, one permission less.
- Evidence for the next upload: `filepick copied kb= ms=` (or `copy-failed <reason>`), then `filepick page-got ... decode=ok canvas=ok reencode=ok`.
