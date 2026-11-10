package com.vibecode.minitodo.ui.theme

import com.vibecode.minitodo.domain.ThemeMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {

    @Test
    fun `system mode follows system setting`() {
        assertFalse(resolveDarkTheme(ThemeMode.SYSTEM, systemDark = false))
        assertTrue(resolveDarkTheme(ThemeMode.SYSTEM, systemDark = true))
    }

    @Test
    fun `light mode ignores system dark theme`() {
        assertFalse(resolveDarkTheme(ThemeMode.LIGHT, systemDark = true))
    }

    @Test
    fun `dark mode ignores system light theme`() {
        assertTrue(resolveDarkTheme(ThemeMode.DARK, systemDark = false))
    }
}
