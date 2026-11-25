# Keep the JS bridge methods used by the bundled Kandle page.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
