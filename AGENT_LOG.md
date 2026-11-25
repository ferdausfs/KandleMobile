# Agent log

Newest first. Never delete an entry.

---

## 2026-11-25 — native chrome v1 + audit fixes

- **Agent / tool:** OpenCode Android agent; `dev-server` for Gradle, git, craft-anything and headless Chromium.
  Three review agents ran in parallel: hallmark-designer (native chrome spec), pr-review-toolkit-code-reviewer
  (Kotlin/WebView audit), explore (Kandle DOM/`window.kandle` API map).
- **What changed:** ember-on-slate native palette with `values-night`; Kandle `data-theme` observer drives native
  bars, status/nav bar icons and pull-to-refresh colours; edge-to-edge insets (status spacer + nav-bar padding);
  transparent active-indicator pill replaced by a 20×3dp accent tick; bottom nav is now Watchlist · Chart ·
  Explore · Scripts · Menu and uses `window.kandle.watchlist.toggle` / the Script editor button instead of double
  key dispatch.
- **P0 fixes:** `onRenderProcessGone` recreates the WebView instead of killing the process; `onDestroy` removes the
  JS bridge and destroys the view; error dialog guarded against finishing activities.
- **P1 fixes:** removed duplicate tab toggles; notification permission is reported truthfully to the web layer
  (`canNotify` + shim); camera/mic requests denied; file chooser honours `acceptTypes` and multi-select; external
  URL scheme validation (`http/https` only, plus mailto/tel); `allowBackup=false`; tab reselect handler; mailto/tel
  routing; clear-cache no longer wipes watchlists/layouts; R8 rule broadened to all `@JavascriptInterface` methods.
- **Verification:** `assembleDebug` + `testDebugUnitTest` + `assembleRelease` green; CI green on `main` and the
  release tag; craft-anything `android-package-show` (deep): 4 permissions, backup off, cleartext off, v2 signed,
  15 assets, 1 dex, no native libs, 3 components (launcher + AndroidX profile installer exported, both standard).
- **Limits:** server has no `/dev/kvm` and no ADB device, so the native chrome was not exercised on a device; the
  bundled web layer is covered by the headless-Chromium tests in the upstream project and by manual DOM probes.
- **Release:** `v0.11.0-android.4` — `kandle-android4.apk`.

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
