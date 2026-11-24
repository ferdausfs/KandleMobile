# Agent log

Newest first. Never delete an entry.

---

## 2026-11-24 — KandleMobile bootstrap

- **Agent / tool:** OpenCode Android agent; `dev-server` for upstream build, Gradle, git.
- **Feature:** native Android wrapper for the Kandle web app.
- **What changed:** new repo `ferdausfs/KandleMobile`; fresh Kotlin/Android project (no code reused from earlier
  projects); upstream Kandle 0.11.0 cloned at `02eb2bc`, built with `vite build --base=./` (1.4 MB dist) and
  bundled in `app/src/main/assets/kandle/`; `WebViewAssetLoader` serves it from
  `https://appassets.androidplatform.net/assets/kandle/`; native shell with splash theme, own icon, file picker
  for CSV, downloads, external links, back handling, keep-screen-on, and a `Notification` shim that turns Kandle
  alerts into Android notifications; CI workflow with debug + signed release APKs; MIT attribution files.
- **Touched files:** `app/**`, `gradle/**`, `.github/workflows/android.yml`, `docs/ATTRIBUTION.md`, `README.md`.
- **Verification:** `./gradlew assembleDebug testDebugUnitTest` (UrlPolicy tests) and `assembleRelease` on the
  server; APK asset list checked to contain the full Kandle bundle.
- **Commit:** see `git log`.
- **Pending:** install test on the owner's device; optional native additions (in-app About screen, share target).
- **Notes:** MCP bridge is intentionally absent (needs Node). The bundled build is pinned; update steps are in
  `README.md`.
