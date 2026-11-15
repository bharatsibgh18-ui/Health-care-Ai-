package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

val LocalAppLanguage = compositionLocalOf { "English" }

fun trLang(language: String, en: String, gu: String, hi: String): String {
    return when {
        language.equals("Gujarati", ignoreCase = true) -> gu
        language.equals("Hindi", ignoreCase = true) -> hi
        else -> en
    }
}

@Composable
fun tr(en: String, gu: String, hi: String): String {
    return trLang(LocalAppLanguage.current, en, gu, hi)
}
