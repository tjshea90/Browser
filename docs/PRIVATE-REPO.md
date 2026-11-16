# Making the source repo private (PRIV1) - owner guide

Idea: the SOURCE repo (tjshea90/Browser) becomes private. Every release is ALSO published to a small PUBLIC repo that holds only
the APKs (tjshea90/Kestrel-Releases). The app checks for updates there, and your install links point there. Easier and less secure
on purpose: one token, no extra keys.

## Do this (about 10 minutes, in this order)

1. Create the public releases repo: github.com > New repository > name `Kestrel-Releases`, **Public**, tick "Add a README file" (it needs one commit), Create.
2. Make a token: github.com > your picture > Settings > Developer settings > Personal access tokens > Tokens (classic) > Generate new token (classic).
   Name `kestrel-releases`, expiration "No expiration", tick only the box `repo`, Generate, COPY the token (shown once).
   (Fine-grained alternative: only repo Kestrel-Releases, permission Contents: Read and write.)
3. Give it to the build: repo tjshea90/Browser > Settings > Secrets and variables > Actions > New repository secret.
   Name exactly `RELEASES_TOKEN`, value = the token. Save.
4. Tell Claude "token is set". Claude ships alpha62 (it lands in BOTH repos). This is the bridge: your installed alpha61 still looks at the old repo, which is still public.
5. On the tablet: update to alpha62 inside the app (Updates screen) as always. From then on the app looks at Kestrel-Releases.
   Check Updates says "up to date" or lists the next version without errors.
6. ONLY NOW make the source private: tjshea90/Browser > Settings > General > Danger zone > Change visibility > Make private.
7. Check Claude still reaches it: start/continue a Claude session; if it says it cannot reach the repo, install the Claude GitHub app on it
   (github.com/apps/claude/installations/select_target) and give it access to tjshea90/Browser.

## Afterwards
- Release links you get from Claude point to github.com/tjshea90/Kestrel-Releases/releases/tag/... (public, no login needed).
- A private repo on the free plan has 2,000 free Actions minutes a month: a release build is about 10, a full video lab run about 25. Run the lab before releases only.
- If an update ever stops showing: the token expired or was deleted. Make a new one (step 2) and replace the secret (step 3).
- Not fixed by this: the old releases in the source repo (alpha1-alpha61) stay there, private. Downloads of them need your GitHub login.
