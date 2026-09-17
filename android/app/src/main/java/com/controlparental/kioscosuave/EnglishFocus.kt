package com.controlparental.kioscosuave

import android.content.Context
import java.text.Normalizer
import org.json.JSONArray
import org.json.JSONObject

/**
 * Refuerzo temporal de Inglés enviado desde una guía del panel de padres.
 *
 * Por ahora reconoce el formato "Spelling B" / "Ortografía letra B". El
 * resultado se conserva localmente: una vez descargado, el niño puede seguir
 * practicando aunque se quede sin red. Una guía pausada o eliminada se limpia
 * al siguiente sincronizado exitoso.
 */
data class EnglishFocus(val letter: Char, val correctTarget: Int = 30) {
    val title: String get() = "Spelling ${letter.uppercaseChar()}"
}

object EnglishFocusStore {
    private const val PREFS = "TriviumEnglishFocus"
    private const val KEY_LETTER = "spelling_letter"
    private const val KEY_TARGET = "correct_target"

    fun load(ctx: Context): EnglishFocus? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LETTER, null)
            ?.singleOrNull()
            ?.takeIf(Char::isLetter)
            ?.let { EnglishFocus(it, ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_TARGET, 30).coerceIn(10, 100)) }

    fun save(ctx: Context, focus: EnglishFocus?) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            if (focus == null) {
                remove(KEY_LETTER); remove(KEY_TARGET)
            } else {
                putString(KEY_LETTER, focus.letter.uppercaseChar().toString())
                putInt(KEY_TARGET, focus.correctTarget.coerceIn(10, 100))
            }
        }.apply()
    }

    /** Extrae "Spelling B", "deletrear B" u "ortografía letra B". */
    fun detect(lines: Iterable<String>, correctTarget: Int = 30): EnglishFocus? {
        val pattern = Regex(
            """\b(?:spelling|deletrear|ortografia)\s*(?:de\s+la\s+)?(?:letra\s+)?([a-z])\b"""
        )
        return lines.asSequence()
            .map(::normalize)
            .mapNotNull { pattern.find(it)?.groupValues?.getOrNull(1)?.singleOrNull() }
            .firstOrNull()
            ?.let { EnglishFocus(it, correctTarget.coerceIn(10, 100)) }
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
}

/** Un reactivo tal como lo contestó el alumno; se muestra solo al padre. */
data class StageAttempt(
    val question: String,
    val selected: String,
    val answer: String,
    val correct: Boolean,
    val explanation: String
)

/**
 * Progreso acumulado de un refuerzo activo. Guarda el resultado y el reactivo
 * durante la sesión para poder entregarle al padre un resumen educativo. Así
 * 30 aciertos puede completarse en varios ratos y un reinicio inesperado no
 * obliga a empezar de cero.
 */
object StageProgressStore {
    private const val PREFS = "TriviumStageProgress"

    fun load(ctx: Context, key: String): List<Boolean> =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key, "")
            .orEmpty()
            .mapNotNull { when (it) { '1' -> true; '0' -> false; else -> null } }

    fun save(ctx: Context, key: String, history: List<Boolean>) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(key, history.joinToString("") { if (it) "1" else "0" })
            .apply()
    }

    fun loadAttempts(ctx: Context, key: String): List<StageAttempt> = try {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("$key:attempts", "[]") ?: "[]"
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                add(StageAttempt(
                    question = o.optString("question"),
                    selected = o.optString("selected"),
                    answer = o.optString("answer"),
                    correct = o.optBoolean("correct"),
                    explanation = o.optString("explanation")
                ))
            }
        }
    } catch (_: Exception) { emptyList() }

    fun saveAttempts(ctx: Context, key: String, attempts: List<StageAttempt>) {
        val array = JSONArray()
        attempts.forEach { a -> array.put(JSONObject().apply {
            put("question", a.question)
            put("selected", a.selected)
            put("answer", a.answer)
            put("correct", a.correct)
            put("explanation", a.explanation)
        }) }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("$key:attempts", array.toString()).apply()
    }

    fun clear(ctx: Context, key: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(key).remove("$key:attempts").apply()
    }
}
