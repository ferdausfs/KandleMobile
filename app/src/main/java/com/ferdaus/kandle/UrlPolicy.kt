package com.ferdaus.kandle

/** URL policy kept pure so it can be unit-tested without Android. */
object UrlPolicy {
    const val HOST = "appassets.androidplatform.net"
    const val BASE = "https://$HOST/assets/kandle/"

    fun isInternal(url: String?): Boolean = url != null && url.startsWith("https://$HOST/")

    fun isExternal(url: String?): Boolean =
        url != null && (url.startsWith("http://") || url.startsWith("https://")) && !isInternal(url)
}
