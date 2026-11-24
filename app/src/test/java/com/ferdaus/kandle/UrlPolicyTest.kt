package com.ferdaus.kandle

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlPolicyTest {

    @Test
    fun `bundled pages are internal`() {
        assertTrue(UrlPolicy.isInternal("https://appassets.androidplatform.net/assets/kandle/index.html"))
        assertTrue(UrlPolicy.isInternal("https://appassets.androidplatform.net/assets/kandle/assets/index.js"))
    }

    @Test
    fun `external links are routed to the browser`() {
        assertTrue(UrlPolicy.isExternal("https://kernel-hunter.github.io/kandle/"))
        assertTrue(UrlPolicy.isExternal("https://github.com/Kernel-Hunter/kandle"))
        assertFalse(UrlPolicy.isExternal("mailto:someone@example.com"))
        assertFalse(UrlPolicy.isExternal("https://appassets.androidplatform.net/assets/kandle/index.html"))
        assertFalse(UrlPolicy.isExternal(null))
    }

    @Test
    fun `javascript and data urls are never opened externally`() {
        assertFalse(UrlPolicy.isExternal("javascript:alert(1)"))
        assertFalse(UrlPolicy.isExternal("data:text/html,hello"))
        assertFalse(UrlPolicy.isInternal("javascript:alert(1)"))
    }
}
