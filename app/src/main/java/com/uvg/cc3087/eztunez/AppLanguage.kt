package com.uvg.cc3087.eztunez

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import java.util.Locale

enum class AppLanguage(val tag: String, @StringRes val labelRes: Int) {
    SYSTEM("", R.string.language_system),
    ENGLISH("en", R.string.language_english),
    SPANISH("es", R.string.language_spanish),
    RUSSIAN("ru", R.string.language_russian);

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull {
            it.tag == tag?.substringBefore('-')
        } ?: SYSTEM
    }
}

/** Reserve built-in names in every supported language, independent of the current UI. */
internal fun builtInPresetNames(context: Context): List<String> = AppLanguage.entries
    .filter { it != AppLanguage.SYSTEM }
    .flatMap { language ->
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language.tag))
        }
        val resources = context.createConfigurationContext(configuration).resources
        BuiltInTuningPresets.all.map { resources.getString(requireNotNull(it.nameRes)) }
    }
    .distinct()
