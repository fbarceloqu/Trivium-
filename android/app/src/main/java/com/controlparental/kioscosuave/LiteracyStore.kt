package com.controlparental.kioscosuave

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Avance en curso de la etapa de alfabetización.
 *
 * Guarda solo lo que hace falta para RETOMAR tras cerrar la app: qué actividad
 * estaba en pantalla (con sus fichas en el mismo orden) y cuáles salieron hace
 * poco. Los aciertos de la etapa usan [StageProgressStore] y el dominio por
 * habilidad usa [MemoryStore], igual que el resto de Trivium.
 *
 * La selección a medias (sílabas tocadas sin confirmar) NO se guarda a
 * propósito: no es una respuesta, así que al volver se empieza esa actividad
 * limpia y sin que cuente como fallo.
 */
object LiteracyStore {
    /** Llave de [StageProgressStore] para el historial de la etapa. */
    const val PROGRESS_KEY = "literacy"

    private const val PREFS = "TriviumLiteracy"
    private const val KEY_CURRENT = "current"
    private const val KEY_RECENT = "recent"

    data class Current(val id: String, val choices: List<String>)

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun current(ctx: Context): Current? = try {
        prefs(ctx).getString(KEY_CURRENT, null)?.let { raw ->
            val o = JSONObject(raw)
            val arr = o.getJSONArray("choices")
            Current(o.getString("id"), List(arr.length()) { arr.getString(it) })
        }
    } catch (_: Exception) { null }

    /** null = no hay actividad pendiente (ya se calificó o se terminó la etapa). */
    fun saveCurrent(ctx: Context, id: String?, choices: List<String> = emptyList()) {
        val editor = prefs(ctx).edit()
        if (id == null) editor.remove(KEY_CURRENT)
        else editor.putString(KEY_CURRENT, JSONObject().apply {
            put("id", id)
            put("choices", JSONArray(choices))
        }.toString())
        editor.apply()
    }

    fun recent(ctx: Context): List<String> = try {
        val arr = JSONArray(prefs(ctx).getString(KEY_RECENT, "[]"))
        List(arr.length()) { arr.getString(it) }
    } catch (_: Exception) { emptyList() }

    fun saveRecent(ctx: Context, ids: List<String>) {
        prefs(ctx).edit().putString(KEY_RECENT, JSONArray(ids).toString()).apply()
    }
}
