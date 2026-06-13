package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.GardenViewModel
import com.example.ui.viewmodel.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class ThemeTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        // Clear shared preferences before each test to ensure a clean state
        val sharedPrefs = context.getSharedPreferences("floraflow_billing_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()
    }

    @Test
    fun testInitialThemeState() {
        val viewModel = GardenViewModel(context as Application)
        // Verify default theme settings
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
        assertNull(viewModel.isDarkTheme.value)
    }

    @Test
    fun testSetThemeModeLight() {
        val viewModel = GardenViewModel(context as Application)
        viewModel.setThemeMode(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
        assertEquals(false, viewModel.isDarkTheme.value)

        // Verify shared preferences storage
        val sharedPrefs = context.getSharedPreferences("floraflow_billing_prefs", Context.MODE_PRIVATE)
        assertEquals(ThemeMode.LIGHT.name, sharedPrefs.getString("theme_mode", null))
    }

    @Test
    fun testSetThemeModeDark() {
        val viewModel = GardenViewModel(context as Application)
        viewModel.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
        assertEquals(true, viewModel.isDarkTheme.value)

        // Verify shared preferences storage
        val sharedPrefs = context.getSharedPreferences("floraflow_billing_prefs", Context.MODE_PRIVATE)
        assertEquals(ThemeMode.DARK.name, sharedPrefs.getString("theme_mode", null))
    }

    @Test
    fun testSetThemeModeSystem() {
        val viewModel = GardenViewModel(context as Application)
        
        // Change theme to DARK first to verify it changes back to SYSTEM correctly
        viewModel.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
        assertEquals(true, viewModel.isDarkTheme.value)

        viewModel.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
        assertNull(viewModel.isDarkTheme.value)

        // Verify shared preferences storage
        val sharedPrefs = context.getSharedPreferences("floraflow_billing_prefs", Context.MODE_PRIVATE)
        assertEquals(ThemeMode.SYSTEM.name, sharedPrefs.getString("theme_mode", null))
    }

    @Test
    fun testThemeStatePersistence() {
        // 1. Write theme settings in one session
        var viewModel = GardenViewModel(context as Application)
        viewModel.setThemeMode(ThemeMode.DARK)

        // 2. Re-instantiate ViewModel and verify stored settings are restored
        viewModel = GardenViewModel(context as Application)
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
        assertEquals(true, viewModel.isDarkTheme.value)

        // 3. Switch back to LIGHT and verify restoration
        viewModel.setThemeMode(ThemeMode.LIGHT)
        viewModel = GardenViewModel(context as Application)
        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
        assertEquals(false, viewModel.isDarkTheme.value)
    }
}
