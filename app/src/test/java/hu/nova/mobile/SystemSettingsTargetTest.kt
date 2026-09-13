package hu.nova.mobile

import hu.nova.mobile.apps.SystemSettingsTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemSettingsTargetTest {

    @Test
    fun `hungarian wifi alias resolves`() {
        assertEquals(SystemSettingsTarget.WIFI, SystemSettingsTarget.matchByAlias("nyisd meg a wifi beállításokat"))
    }

    @Test
    fun `english bluetooth alias resolves`() {
        assertEquals(SystemSettingsTarget.BLUETOOTH, SystemSettingsTarget.matchByAlias("open bluetooth settings"))
    }

    @Test
    fun `unrelated text resolves to null`() {
        assertNull(SystemSettingsTarget.matchByAlias("kérek egy csésze kávét"))
    }
}
