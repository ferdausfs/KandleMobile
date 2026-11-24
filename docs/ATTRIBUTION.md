# Attribution

## Kandle

This app bundles an unmodified build of [Kandle](https://github.com/Kernel-Hunter/kandle) 0.11.0
(upstream commit `02eb2bc`), built with `vite build --base=./`.

```
MIT License

Copyright (c) 2026 Karim Masmoudi

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

The full upstream licence also ships inside the APK at
`app/src/main/assets/kandle/KANDLE_LICENSE.txt`, together with
`third-party-notices.txt` from the Kandle build.

## Modifications

- The web bundle is served through `WebViewAssetLoader` instead of GitHub Pages.
- `native-bridge.js` is appended to the page to route web notifications to Android.
- No Kandle source code is modified beyond that injection; the upstream build output is otherwise byte-for-byte.

## Trade marks

Kandle is the name of the upstream project. This Android wrapper uses the name descriptively, states that it is
unofficial, and does not use the upstream logo or claim endorsement.
