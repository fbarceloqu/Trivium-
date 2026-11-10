package com.controlparental.kioscosuave

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Avance del cuestionario de lectura de secundaria.
 *
 * Un cuestionario de 7 preguntas sobre un texto de 550 palabras lleva un buen
 * rato: si la app se cierra a la mitad, el alumno debe volver a la MISMA
 * lectura, con las opciones en el mismo orden y sus respuestas puestas.
 *
 * También guarda qué lecturas ya se hicieron, para no repetirlas, y el último
 * resultado, para ajustar la extensión de la siguiente.
 */
object ReadingQuizStore {
    private const val PREFS = "TriviumReadingQuiz"
    private const val KEY_CURRENT = "current"
    private const val KEY_HISTORY = "history"
    private const val KEY_LAST_SCORE = "last_score"
    private const val MAX_HISTORY = 40

    data class Current(
        val id: String,
        /** Semilla del orden de las opciones en pantalla. */
        val seed: Long,
        /** Por pregunta: índice original de la opción elegida, o null. */
        val answers: List<Int?>,
        /** Preguntas ya acertadas en un envío anterior: no se vuelven a tocar. */
        val locked: Set<Int> = emptySet(),
        val submits: Int = 0,
        /** Respuestas del primer envío: son las que miden la comprensión. */
        val firstAnswers: List<Int?>? = null,
        /** true si lo último que vio el alumno fue el resultado de un envío. */
        val showingResult: Boolean = false
    )

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun JSONArray.toAnswers(): List<Int?> = List(length()) { i -> optInt(i, -1).takeIf { it >= 0 } }
    private fun List<Int?>.toJson() = JSONArray(map { it ?: -1 })

    fun current(ctx: Context): Current? = try {
        prefs(ctx).getString(KEY_CURRENT, null)?.let { raw ->
            val o = JSONObject(raw)
            val locked = o.optJSONArray("locked") ?: JSONArray()
            Current(
                id = o.getString("id"),
                seed = o.getLong("seed"),
                answers = o.getJSONArray("answers").toAnswers(),
                locked = (0 until locked.length()).map { locked.getInt(it) }.toSet(),
                submits = o.optInt("submits"),
                firstAnswers = o.optJSONArray("first")?.toAnswers(),
                showingResult = o.optBoolean("result")
            )
        }
    } catch (_: Exception) { null }

    fun saveCurrent(ctx: Context, c: Current?) {
        val editor = prefs(ctx).edit()
        if (c == null) editor.remove(KEY_CURRENT)
        else editor.putString(KEY_CURRENT, JSONObject().apply {
            put("id", c.id)
            put("seed", c.seed)
            put("answers", c.answers.toJson())
            put("locked", JSONArray(c.locked.sorted()))
            put("submits", c.submits)
            c.firstAnswers?.let { put("first", it.toJson()) }
            put("result", c.showingResult)
        }.toString())
        editor.apply()
    }

    /** Lecturas terminadas, de la más antigua a la más reciente. */
    fun history(ctx: Context): List<String> = try {
        val arr = JSONArray(prefs(ctx).getString(KEY_HISTORY, "[]"))
        List(arr.length()) { arr.getString(it) }
    } catch (_: Exception) { emptyList() }

    fun lastScore(ctx: Context): Int? =
        prefs(ctx).getInt(KEY_LAST_SCORE, -1).takeIf { it >= 0 }

    /** Cierra la lectura: pasa al historial y deja de estar "en curso". */
    fun finish(ctx: Context, id: String, firstScore: Int) {
        val history = (history(ctx) + id).takeLast(MAX_HISTORY)
        prefs(ctx).edit()
            .putString(KEY_HISTORY, JSONArray(history).toString())
            .putInt(KEY_LAST_SCORE, firstScore)
            .remove(KEY_CURRENT)
            .apply()
    }
}
