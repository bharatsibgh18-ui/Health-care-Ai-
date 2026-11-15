package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.trLang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalizationTest {

    private fun localizedContext(languageTag: String): Context {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag(languageTag))
        return base.createConfigurationContext(config)
    }

    @Test
    fun `verify trLang helper switches cleanly between English Gujarati and Hindi`() {
        assertEquals("Hello", trLang("English", "Hello", "નમસ્તે", "नमस्ते"))
        assertEquals("નમસ્તે", trLang("Gujarati", "Hello", "નમસ્તે", "नमस्ते"))
        assertEquals("नमस्ते", trLang("Hindi", "Hello", "નમસ્તે", "नमस्ते"))
        assertEquals("નમસ્તે", trLang("gujarati", "Hello", "નમસ્તે", "नमस्ते"))
        assertEquals("नमस्ते", trLang("hindi", "Hello", "નમસ્તે", "नमस्ते"))
        assertEquals("Hello", trLang("Other", "Hello", "નમસ્તે", "नमस्ते"))
    }

    @Test
    fun `verify gujarati string resources are complete and localized`() {
        val en = ApplicationProvider.getApplicationContext<Context>()
        val gu = localizedContext("gu")

        assertEquals("હેલ્થકેર સહાયકમાં આપનું સ્વાગત છે", gu.getString(R.string.welcome_screen_title))

        val keysToCheck = listOf(
            R.string.nav_triage,
            R.string.nav_water,
            R.string.nav_emergency,
            R.string.nav_history,
            R.string.nav_profile,
            R.string.scan_pdf_btn,
            R.string.emergency_protocol_title,
            R.string.medicine_reminder_title,
            R.string.step_counter_title,
            R.string.nearby_map_header_title,
            R.string.report_scanner_title
        )

        keysToCheck.forEach { resId ->
            val guText = gu.getString(resId)
            val enText = en.getString(resId)
            assertTrue("Gujarati string should not be blank for $resId", guText.isNotBlank())
            assertNotEquals("Gujarati string should differ from English for $resId", enText, guText)
        }
    }

    @Test
    fun `verify hindi string resources exist and differ from english fallback`() {
        val en = ApplicationProvider.getApplicationContext<Context>()
        val hi = localizedContext("hi")

        assertEquals("हेल्थकेयर सहायक में आपका स्वागत है", hi.getString(R.string.welcome_screen_title))

        val keysToCheck = listOf(
            R.string.app_name,
            R.string.nav_triage,
            R.string.nav_water,
            R.string.nav_emergency,
            R.string.nav_history,
            R.string.nav_profile,
            R.string.scan_pdf_btn,
            R.string.emergency_protocol_title,
            R.string.medicine_reminder_title,
            R.string.step_counter_title,
            R.string.nearby_map_header_title,
            R.string.report_scanner_title,
            R.string.btn_grant_all_permissions
        )

        keysToCheck.forEach { resId ->
            val hiText = hi.getString(resId)
            val enText = en.getString(resId)
            assertTrue("Hindi string should not be blank for $resId", hiText.isNotBlank())
            assertNotEquals("Hindi string should differ from English for $resId", enText, hiText)
        }
    }
}
