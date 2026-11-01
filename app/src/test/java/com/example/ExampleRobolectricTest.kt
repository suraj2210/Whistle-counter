package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.DefaultPresets
import com.example.model.DetectorSettings
import com.example.model.SensitivityLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Whistle Counter", appName)
    }

    @Test
    fun `verify default presets contain essential cooking recipes`() {
        val presets = DefaultPresets.list
        assertTrue("Presets should have at least 5 recipes", presets.size >= 5)
        val rice = presets.firstOrNull { it.id == "white_rice" }
        assertEquals("White Rice / Basmati", rice?.name)
        assertEquals(3, rice?.defaultWhistles)
    }

    @Test
    fun `verify detector settings defaults`() {
        val settings = DetectorSettings()
        assertEquals(SensitivityLevel.MEDIUM, settings.sensitivity)
        assertEquals(15, settings.cooldownSec)
        assertTrue(settings.voiceAnnouncementsEnabled)
        assertTrue(settings.vibrationEnabled)
    }
}
