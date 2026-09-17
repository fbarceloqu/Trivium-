package com.controlparental.kioscosuave

import android.content.Context
import java.text.Normalizer

/**
 * Refuerzo temporal de Inglés enviado desde una guía del panel de padres.
 *
 * Por ahora reconoce el formato "Spelling B" / "Ortografía letra B". El
 * resultado se conserva localmente: una vez descargado, el niño puede seguir
 * practicando aunque se quede sin red. Una guía pausada o eliminada se limpia
 * al siguiente sincronizado exitoso.
 */
data class EnglishFocus(val letter: Char) {
    val title: String get() = "Spelling ${letter.uppercaseChar()}"
}

object EnglishFocusStore {
    private const val PREFS = "TriviumEnglishFocus"
    private const val KEY_LETTER = "spelling_letter"

    fun load(ctx: Context): EnglishFocus? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LETTER, null)
            ?.singleOrNull()
            ?.takeIf(Char::isLetter)
            ?.let(::EnglishFocus)

    fun save(ctx: Context, focus: EnglishFocus?) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            if (focus == null) remove(KEY_LETTER)
            else putString(KEY_LETTER, focus.letter.uppercaseChar().toString())
        }.apply()
    }

    /** Extrae "Spelling B", "deletrear B" u "ortografía letra B". */
    fun detect(lines: Iterable<String>): EnglishFocus? {
        val pattern = Regex(
            """\b(?:spelling|deletrear|ortografia)\s*(?:de\s+la\s+)?(?:letra\s+)?([a-z])\b"""
        )
        return lines.asSequence()
            .map(::normalize)
            .mapNotNull { pattern.find(it)?.groupValues?.getOrNull(1)?.singleOrNull() }
            .firstOrNull()
            ?.let(::EnglishFocus)
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
}
