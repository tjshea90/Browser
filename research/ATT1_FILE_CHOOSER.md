# ATT1: "when i tried to add an attachment in kestrel browser it just got stuck and did nothing" (owner, 2026-11-21, Google Gemini website)

STATUS: first fix shipped in alpha79, NOT proven on the tablet. Which step stops there is unknown; the new Diagnostics line "File chooser (attach a file on a page): ..." will say.

## Path of an attach button in Kestrel G
page `<input type=file>` -> GeckoView `PromptDelegate.onFilePrompt` (TabDelegates) -> `BrowserActivity.pickFiles` -> `startActivityForResult(GET_CONTENT chooser)` -> `onActivityResult(REQ_FILE)` -> `prompt.confirm(activity, uris)`. GeckoView 160's FilePrompt has type SINGLE / MULTIPLE / FOLDER, a `capture` hint (camera) and `mimeTypes` (MIME types or `.ext`).

## Found (code reading, with the real lists probed)
- Silent failure: with no app that can choose a file, or any exception, `pickFiles` completed the prompt with "dismiss" and said nothing; the page waits or just does nothing. Nothing was logged either, and the lab never had a file input.
- The filter of the picker kept only `accept` entries that contain a slash: `image/*,.pdf,.docx` became "pictures only" (documents hidden).
- A FOLDER prompt (`webkitdirectory`) was answered with a file chooser (the page then got a file where it wanted a folder).
- The manifest declared no GET_CONTENT / OPEN_DOCUMENT in `<queries>`: on Android 11+ the check "does any app handle this" would always say no.
- Gemini's upload / picker addresses (push.clients6.google.com/upload, content-push.googleapis.com/upload, docs.google.com/picker, apis.google.com, lh3.googleusercontent.com ...) were evaluated against the REAL bundled default.kfs as page gemini.google.com: all allowed (only play.google.com/log telemetry is blocked). So the Shields lists are not what stops it.

## Changed (alpha79)
core `prompt/FilePickRules.mimeTypes` (extensions mapped, anything not understood = no filter), `FilePickStats` (Diagnostics line), BrowserActivity `pickFiles` (GET_CONTENT, else OPEN_DOCUMENT, else a toast "No app on this tablet can pick a file" and the page is answered), `refuseFolder`, Health lines `filepick asked / opened / no-handler / open-failed / result`, manifest queries, tests `FilePickRulesTest`.

## Not known (the next paste decides)
Whether the chooser opened, whether the tablet has any app that chooses files (Settings > Back up & restore > Restore uses the same kind of chooser), whether Android ended Kestrel while the chooser was open (2 GB tablet: Diagnostics "Last run"), and Gemini's own menu steps (a menu first, then "Upload files").
