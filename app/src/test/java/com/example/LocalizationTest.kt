package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.AppLanguages
import com.example.ui.theme.trLang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `verify 21 languages are registered including 18 newly added languages`() {
        assertEquals(21, AppLanguages.all.size)
        assertEquals(21, AppLanguages.all.map { it.id }.toSet().size)
        assertEquals(21, AppLanguages.all.map { it.localeTag }.toSet().size)
        assertEquals(12, AppLanguages.indianLanguages.size)
        assertEquals(9, AppLanguages.globalLanguages.size)

        // Verify RTL configuration for Arabic and Urdu
        assertTrue(AppLanguages.isRtl("Arabic"))
        assertTrue(AppLanguages.isRtl("Urdu"))
        assertFalse(AppLanguages.isRtl("English"))
        assertFalse(AppLanguages.isRtl("Gujarati"))
        assertFalse(AppLanguages.isRtl("Japanese"))

        // Verify speech tags and trLang translations across all 18 new languages
        val newLanguages = AppLanguages.all.filter { it.id !in setOf("English", "Gujarati", "Hindi") }
        assertEquals(18, newLanguages.size)
        newLanguages.forEach { info ->
            assertTrue("Speech tag should not be blank for ${info.id}", info.speechTag.isNotBlank())
            val translatedHello = trLang(info.id, "Hello", "નમસ્તે", "नमस्ते")
            assertNotEquals("trLang Hello should translate for ${info.id}", "Hello", translatedHello)
            val translatedSelect = trLang(info.id, "Select Language", "ભાષા પસંદ કરો", "भाषा चुनें")
            assertNotEquals("trLang Select Language should translate for ${info.id}", "Select Language", translatedSelect)
        }
    }

    @Test
    fun `verify all 18 new language string resource bundles exist and differ from english`() {
        val en = ApplicationProvider.getApplicationContext<Context>()
        val newLanguages = AppLanguages.all.filter { it.id !in setOf("English", "Gujarati", "Hindi") }
        assertEquals(18, newLanguages.size)

        val keysToCheck = listOf(
            R.string.welcome_screen_title,
            R.string.nav_water,
            R.string.nav_emergency,
            R.string.emergency_protocol_title,
            R.string.medicine_reminder_title,
            R.string.step_counter_title,
            R.string.nearby_map_header_title,
            R.string.report_scanner_title,
            R.string.btn_grant_all_permissions
        )

        newLanguages.forEach { langInfo ->
            val localized = localizedContext(langInfo.localeTag)
            keysToCheck.forEach { resId ->
                val localizedText = localized.getString(resId)
                val enText = en.getString(resId)
                assertTrue(
                    "${langInfo.id} (${langInfo.localeTag}) string should not be blank for $resId",
                    localizedText.isNotBlank()
                )
                assertNotEquals(
                    "${langInfo.id} (${langInfo.localeTag}) string should differ from English for $resId",
                    enText,
                    localizedText
                )
            }
        }
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

    @Test
    fun `verify all 18 added languages translate splash upgrade download and hub phrases`() {
        val newLanguages = AppLanguages.all.filter { it.id !in setOf("English", "Gujarati", "Hindi") }
        assertEquals(18, newLanguages.size)

        val phrases = listOf(
            "Download Upgrade",
            "App Upgrade Available",
            "Publish Public Upgrade",
            "Loading Smart Clinical AI...",
            "Ultra Hub",
            "Developer Control & Upgrades",
            "Users (5)"
        )

        newLanguages.forEach { langInfo ->
            phrases.forEach { enPhrase ->
                val translated = trLang(langInfo.id, enPhrase, enPhrase, enPhrase)
                assertTrue("Translated phrase should not be blank for ${langInfo.id}", translated.isNotBlank())
                assertNotEquals(
                    "Phrase '$enPhrase' should be translated for ${langInfo.id}",
                    enPhrase,
                    translated
                )
            }
        }
    }
}
