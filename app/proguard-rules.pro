# Keep the JS bridge methods used by the bundled Kandle page.
-keepclassmembers class com.ferdaus.kandle.NativeBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
