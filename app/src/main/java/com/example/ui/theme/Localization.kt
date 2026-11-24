package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.example.ui.theme.i18n.GlobalLanguagesDictionary
import com.example.ui.theme.i18n.IndianLanguagesDictionary
import java.util.Locale

enum class LanguageGroup {
    INDIAN,
    GLOBAL
}

data class AppLanguageInfo(
    val id: String,
    val nativeName: String,
    val englishName: String,
    val localeTag: String,
    val speechTag: String,
    val group: LanguageGroup,
    val changedToast: String,
    val refreshedToast: String,
    val isRtl: Boolean = false
) {
    val testTagKey: String
        get() = "lang_option_${id.lowercase(Locale.ROOT)}"

    fun toLocale(): Locale = Locale.forLanguageTag(localeTag)
}

/**
 * Central registry of all 21 supported app languages (3 original + 18 newly added):
 * - 12 Indian languages: Gujarati, Hindi, Marathi, Bengali, Tamil, Telugu, Kannada, Malayalam, Punjabi, Urdu, Odia, Assamese
 * - 9 Global languages: English, Spanish, French, German, Arabic, Portuguese, Russian, Japanese, Chinese
 */
object AppLanguages {
    val all: List<AppLanguageInfo> = listOf(
        // Original 3 languages
        AppLanguageInfo(
            id = "English",
            nativeName = "English",
            englishName = "English",
            localeTag = "en",
            speechTag = "en-IN",
            group = LanguageGroup.GLOBAL,
            changedToast = "Language changed to English",
            refreshedToast = "Language and app refreshed successfully!"
        ),
        AppLanguageInfo(
            id = "Gujarati",
            nativeName = "ગુજરાતી",
            englishName = "Gujarati",
            localeTag = "gu",
            speechTag = "gu-IN",
            group = LanguageGroup.INDIAN,
            changedToast = "ભાષા બદલાઈને ગુજરાતી થઈ ગઈ",
            refreshedToast = "ભાષા અને એપ્લિકેશન સફળતાપૂર્વક રિફ્રેશ થઈ ગઈ છે!"
        ),
        AppLanguageInfo(
            id = "Hindi",
            nativeName = "हिन्दी",
            englishName = "Hindi",
            localeTag = "hi",
            speechTag = "hi-IN",
            group = LanguageGroup.INDIAN,
            changedToast = "भाषा बदलकर हिन्दी हो गई",
            refreshedToast = "भाषा और ऐप सफलतापूर्वक रीफ्रेश हो गए!"
        ),

        // 10 Newly Added Indian Languages
        AppLanguageInfo(
            id = "Marathi",
            nativeName = "मराठी",
            englishName = "Marathi",
            localeTag = "mr",
            speechTag = "mr-IN",
            group = LanguageGroup.INDIAN,
            changedToast = "भाषा मराठीवर बदलली आहे",
            refreshedToast = "ॲप व भाषा यशस्वीरित्या रीफ्रेश झाले!"
        ),
        AppLanguageInfo(
            id = "Bengali",
            nativeName = "বাংলা",
            englishName = "Bengali",
            localeTag = "bn",
            speechTag = "bn-IN",
            group = LanguageGroup.INDIAN,
            changedToast = "ভাষা বাংলায় পরিবর্তিত হয়েছে",
            refreshedToast = "অ্যাপ ও ভাষা সফলভাবে রিফ্রেশ হয়েছে!"
        ),
        AppLanguageInfo(
            id = "Tamil",
            nativeName = "தமிழ்",
            englishName = "Tamil",
            localeTag = "ta",
            speechTag = "ta-IN",
            group = LanguageGroup.INDIAN,
            changedToast = "மொழி தமிழுக்கு மாற்றப்பட்டது",
            refreshedToast = "செயலி & மொழி வெற்றிகரமாக புதுப்பிக்கப்பட்டது!"
        ),
        AppLanguageInfo(
            id = "Telugu",
            nativeName = "తెలుగు",
            englishName = "Telugu",
            localeTag = "te",
            speechTag = "te-IN",
            group = LanguageGroup.INDIAN,
            changedToast = "భాష తెలుగుకు మార్చబడింది",
            refreshedToast = "యాప్ & భాష విజయవంతంగా రిఫ్రెష్ అయ్యాయి!"
        ),
        AppLanguageInfo(
            id = "Kannada",
            nativeName = "ಕನ್ನಡ",
            englishName = "Kannada",
            localeTag = "kn",
            speechTag = "kn-IN",
            group = LanguageGroup.INDIAN,
            changedToast = "ಭಾಷೆಯನ್ನು ಕನ್ನಡಕ್ಕೆ ಬದಲಾಯಿಸಲಾಗಿದೆ",
            refreshedToast = "ಆ್ಯಪ್ ಮತ್ತು ಭಾಷೆ ಯಶಸ್ವಿಯಾಗಿ ರಿಫ್ರೆಶ್ ಆಗಿವೆ!"
        ),
        AppLanguageInfo(
            id = "Malayalam",
            nativeName = "മലയാളം",
            englishName = "Malayalam",
            localeTag = "ml",
            speechTag = "ml-IN",
            group = LanguageGroup.INDIAN,
            changedToast = "ഭാഷ മലയാളത്തിലേക്ക് മാറ്റി",
            refreshedToast = "ആപ്പും ഭാഷയും വിജയകരമായി പുതുക്കി!"
        ),
        AppLanguageInfo(
            id = "Punjabi",
            nativeName = "ਪੰਜਾਬੀ",
            englishName = "Punjabi",
            localeTag = "pa",
            speechTag = "pa-IN",
            group = LanguageGroup.INDIAN,
            changedToast = "ਭਾਸ਼ਾ ਪੰਜਾਬੀ ਵਿੱਚ ਬਦਲ ਗਈ",
            refreshedToast = "ਐਪ ਅਤੇ ਭਾਸ਼ਾ ਸਫਲਤਾਪੂਰਵਕ ਰੀਫ੍ਰੈਸ਼ ਹੋ ਗਈ!"
        ),
        AppLanguageInfo(
            id = "Urdu",
            nativeName = "اردو",
            englishName = "Urdu",
            localeTag = "ur",
            speechTag = "ur-IN",
            group = LanguageGroup.INDIAN,
            changedToast = "زبان اردو میں تبدیل ہو گئی",
            refreshedToast = "ایپ اور زبان کامیابی سے ریفریش ہو گئی!",
            isRtl = true
        ),
        AppLanguageInfo(
            id = "Odia",
            nativeName = "ଓଡ଼ିଆ",
            englishName = "Odia",
            localeTag = "or",
            speechTag = "or-IN",
            group = LanguageGroup.INDIAN,
            changedToast = "ଭାଷା ଓଡ଼ିଆକୁ ବଦଳିଗଲା",
            refreshedToast = "ଆପ୍ ଏବଂ ଭାଷା ସଫଳତାର ସହ ରିଫ୍ରେଶ୍ ହେଲା!"
        ),
        AppLanguageInfo(
            id = "Assamese",
            nativeName = "অসমীয়া",
            englishName = "Assamese",
            localeTag = "as",
            speechTag = "as-IN",
            group = LanguageGroup.INDIAN,
            changedToast = "ভাষা অসমীয়ালৈ সলনি হ'ল",
            refreshedToast = "এপ আৰু ভাষা সফলতাৰে ৰিফ্ৰেছ হ'ল!"
        ),

        // 8 Newly Added Global Languages
        AppLanguageInfo(
            id = "Spanish",
            nativeName = "Español",
            englishName = "Spanish",
            localeTag = "es",
            speechTag = "es-ES",
            group = LanguageGroup.GLOBAL,
            changedToast = "Idioma cambiado a Español",
            refreshedToast = "¡App e idioma actualizados correctamente!"
        ),
        AppLanguageInfo(
            id = "French",
            nativeName = "Français",
            englishName = "French",
            localeTag = "fr",
            speechTag = "fr-FR",
            group = LanguageGroup.GLOBAL,
            changedToast = "Langue modifiée en Français",
            refreshedToast = "Application et langue actualisées !"
        ),
        AppLanguageInfo(
            id = "German",
            nativeName = "Deutsch",
            englishName = "German",
            localeTag = "de",
            speechTag = "de-DE",
            group = LanguageGroup.GLOBAL,
            changedToast = "Sprache auf Deutsch geändert",
            refreshedToast = "App & Sprache erfolgreich aktualisiert!"
        ),
        AppLanguageInfo(
            id = "Arabic",
            nativeName = "العربية",
            englishName = "Arabic",
            localeTag = "ar",
            speechTag = "ar-SA",
            group = LanguageGroup.GLOBAL,
            changedToast = "تم تغيير اللغة إلى العربية",
            refreshedToast = "تم تحديث التطبيق واللغة بنجاح!",
            isRtl = true
        ),
        AppLanguageInfo(
            id = "Portuguese",
            nativeName = "Português",
            englishName = "Portuguese",
            localeTag = "pt",
            speechTag = "pt-BR",
            group = LanguageGroup.GLOBAL,
            changedToast = "Idioma alterado para Português",
            refreshedToast = "App e idioma atualizados com sucesso!"
        ),
        AppLanguageInfo(
            id = "Russian",
            nativeName = "Русский",
            englishName = "Russian",
            localeTag = "ru",
            speechTag = "ru-RU",
            group = LanguageGroup.GLOBAL,
            changedToast = "Язык изменён на Русский",
            refreshedToast = "Приложение и язык успешно обновлены!"
        ),
        AppLanguageInfo(
            id = "Japanese",
            nativeName = "日本語",
            englishName = "Japanese",
            localeTag = "ja",
            speechTag = "ja-JP",
            group = LanguageGroup.GLOBAL,
            changedToast = "言語を日本語に変更しました",
            refreshedToast = "アプリと言語を更新しました！"
        ),
        AppLanguageInfo(
            id = "Chinese",
            nativeName = "中文",
            englishName = "Chinese (Simplified)",
            localeTag = "zh-CN",
            speechTag = "zh-CN",
            group = LanguageGroup.GLOBAL,
            changedToast = "语言已更改为简体中文",
            refreshedToast = "应用与语言已成功刷新！"
        )
    )

    val indianLanguages: List<AppLanguageInfo> = all.filter { it.group == LanguageGroup.INDIAN }
    val globalLanguages: List<AppLanguageInfo> = all.filter { it.group == LanguageGroup.GLOBAL }

    fun findByName(name: String?): AppLanguageInfo {
        if (name.isNullOrBlank()) return all.first()
        val trimmed = name.trim()
        return all.firstOrNull {
            it.id.equals(trimmed, ignoreCase = true) ||
                it.localeTag.equals(trimmed, ignoreCase = true) ||
                it.nativeName.equals(trimmed, ignoreCase = true) ||
                it.englishName.equals(trimmed, ignoreCase = true)
        } ?: all.first()
    }

    fun localeFor(name: String?): Locale = findByName(name).toLocale()

    fun speechTagFor(name: String?): String = findByName(name).speechTag

    fun isRtl(name: String?): Boolean = findByName(name).isRtl
}

val LocalAppLanguage = compositionLocalOf { "English" }

fun trLang(language: String, en: String, gu: String, hi: String): String {
    val resolved = AppLanguages.findByName(language)
    return when (resolved.id) {
        "Gujarati" -> gu
        "Hindi" -> hi
        "English" -> en
        else -> com.example.ui.theme.i18n.ExtendedLanguagesDictionary.translate(resolved.id, en)
            ?: IndianLanguagesDictionary.translate(resolved.id, en)
            ?: GlobalLanguagesDictionary.translate(resolved.id, en)
            ?: en
    }
}

@Composable
fun tr(en: String, gu: String, hi: String): String {
    return trLang(LocalAppLanguage.current, en, gu, hi)
}
