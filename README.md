# Kandle for Android

Native Android shell for [Kandle](https://github.com/Kernel-Hunter/kandle), the free open-source charting app
for crypto, stocks and forex. The chart app itself is Kandle's own MIT-licensed web build, bundled inside the APK
and served locally — no remote code, no CDN, no account.

This is an **unofficial wrapper**. It is not affiliated with or endorsed by the Kandle project or Karim Masmoudi.

## What is inside

- Kandle 0.11.0 web build (upstream commit `02eb2bc`), built with `vite build --base=./` and bundled in
  `app/src/main/assets/kandle/`.
- `WebViewAssetLoader` serves the bundle from `https://appassets.androidplatform.net/assets/kandle/`, so the page
  has a real HTTPS origin (CORS for exchange APIs works) and local storage persists between launches.
- Native shell: splash theme, own launcher icon, file picker for CSV import, download handling, external links in
  the browser, back navigation, keep-screen-on, and a notification bridge.
- `native-bridge.js` replaces the web `Notification` constructor with a call into `AndroidNative.notify`, so Kandle
  alerts become real Android notifications (channel `kandle_alerts`, POST_NOTIFICATIONS on Android 13+).

## What does not work on Android

- The **MCP agent bridge** needs Node.js on `127.0.0.1`; it is not included. Everything else in Kandle — 36
  indicators, drawing tools, scripts, strategy backtests, alerts, replay, layouts, watchlist, CSV import, share
  links — runs inside the WebView.
- Browser service-worker behaviour is not guaranteed; the app relies on WebView storage.
- The web build is pinned. Updating it is a deliberate step (see below), not an in-app download.

## Build

```bash
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # URL policy tests
./gradlew assembleRelease        # needs the signing env vars
```

Signing:

```bash
export KANDLE_KEYSTORE=/path/to/kandle-release.jks
export KANDLE_STORE_PASSWORD=...
export KANDLE_KEY_ALIAS=kandle
export KANDLE_KEY_PASSWORD=...
```

CI builds debug and signed release APKs and uploads them as artifacts when the `KANDLE_*` repository secrets exist.

## Updating the bundled Kandle build

```bash
git clone --depth 1 https://github.com/Kernel-Hunter/kandle.git /tmp/kandle
cd /tmp/kandle
corepack enable && pnpm install --frozen-lockfile
pnpm exec vite build --base=./
rm -rf <repo>/app/src/main/assets/kandle
mkdir -p <repo>/app/src/main/assets/kandle
cp -r dist/* <repo>/app/src/main/assets/kandle/
cp LICENSE <repo>/app/src/main/assets/kandle/KANDLE_LICENSE.txt
# re-insert the bridge script into index.html before </head>
sed -i 's#</head>#<script src="native-bridge.js"></script></head>#' \
  <repo>/app/src/main/assets/kandle/index.html
```

Record the upstream commit in `docs/ATTRIBUTION.md` and in `AGENT_LOG.md`.

## Licence

Kandle is MIT licensed, Copyright (c) 2026 Karim Masmoudi. The full text ships in
`app/src/main/assets/kandle/KANDLE_LICENSE.txt` and is reproduced in `docs/ATTRIBUTION.md`. The Android shell in
this repository is MIT licensed as well; see `LICENSE`.
