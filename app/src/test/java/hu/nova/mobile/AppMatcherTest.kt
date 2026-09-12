package hu.nova.mobile

import hu.nova.mobile.apps.AppMatcher
import hu.nova.mobile.domain.model.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppMatcherTest {

    private val apps = listOf(
        AppInfo("com.google.android.youtube", "YouTube", listOf("youtube", "yt", "videók")),
        AppInfo("com.android.chrome", "Chrome", listOf("chrome", "böngésző", "internet")),
        AppInfo("com.android.settings", "Settings", listOf("beállítások", "settings"))
    )

    @Test
    fun `exact alias match resolves correctly`() {
        val match = AppMatcher.findBestMatch("yt", apps)
        assertEquals("com.google.android.youtube", match?.packageName)
    }

    @Test
    fun `hungarian alias with accents matches`() {
        val match = AppMatcher.findBestMatch("böngésző", apps)
        assertEquals("com.android.chrome", match?.packageName)
    }

    @Test
    fun `minor typo still fuzzy matches within threshold`() {
        val match = AppMatcher.findBestMatch("chrom", apps)
        assertEquals("com.android.chrome", match?.packageName)
    }

    @Test
    fun `unrelated query does not match anything`() {
        val match = AppMatcher.findBestMatch("xyznonexistentapp123", apps)
        assertNull(match)
    }
}
