package com.controlparental.kioscosuave

import android.content.Context
import android.util.Log
import com.controlparental.kioscosuave.curriculum.ExerciseFormat
import com.controlparental.kioscosuave.curriculum.SkillState
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistencia de la memoria de aprendizaje.
 *
 * Guarda el estado por habilidad como JSON en SharedPreferences. Se eligió esto
 * en vez de Room a propósito: una tablet = un niño, y 47 habilidades ocupan
 * unos pocos KB, así que una base de datos con generación de código añadiría
 * dependencias y complejidad sin resolver ningún problema real. La política de
 * repaso vive aparte (LearningMemory), así que migrar a Room más adelante no
 * tocaría la lógica.
 *
 * Todo es tolerante a fallos: si el JSON está corrupto o cambió de forma, se
 * empieza de cero en vez de reventar. Perder el historial es molesto; dejar la
 * tablet bloqueada por una excepción, no.
 */
object MemoryStore {

    private const val TAG = "TriviumMemory"
    private const val PREFS = "trivium_memory"
    private const val KEY_SKILLS = "skills_v1"

    fun load(ctx: Context): Map<String, SkillState> {
        val raw = prefs(ctx).getString(KEY_SKILLS, null) ?: return emptyMap()
        return try {
            val root = JSONObject(raw)
            val out = HashMap<String, SkillState>()
            for (id in root.keys()) {
                out[id] = fromJson(id, root.getJSONObject(id))
            }
            out
        } catch (e: Exception) {
            Log.w(TAG, "Memoria ilegible, se reinicia: ${e.message}")
            emptyMap()
        }
    }

    fun save(ctx: Context, states: Map<String, SkillState>) {
        try {
            val root = JSONObject()
            states.forEach { (id, st) -> root.put(id, toJson(st)) }
            prefs(ctx).edit().putString(KEY_SKILLS, root.toString()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo guardar la memoria: ${e.message}")
        }
    }

    /** Borra el historial (para "empezar de nuevo" desde ajustes parentales). */
    fun clear(ctx: Context) {
        prefs(ctx).edit().remove(KEY_SKILLS).apply()
    }

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun toJson(s: SkillState) = JSONObject().apply {
        put("p", s.practices)
        put("c", s.correct)
        put("st", s.streak)
        put("l", s.lapses)
        put("last", s.lastPracticedAt)
        put("due", s.dueAt)
        put("iv", s.intervalDays.toDouble())
        put("e", s.ease.toDouble())
        put("rf", JSONArray(s.recentFormats.map { it.name }))
        put("re", JSONArray(s.recentErrors))
    }

    private fun fromJson(id: String, o: JSONObject): SkillState {
        // Un formato que ya no exista en el enum se descarta en silencio: el
        // historial sigue siendo válido aunque el temario evolucione.
        val formats = o.optJSONArray("rf").toStringList().mapNotNull { name ->
            runCatching { ExerciseFormat.valueOf(name) }.getOrNull()
        }
        return SkillState(
            skillId = id,
            practices = o.optInt("p"),
            correct = o.optInt("c"),
            streak = o.optInt("st"),
            lapses = o.optInt("l"),
            lastPracticedAt = o.optLong("last"),
            dueAt = o.optLong("due"),
            intervalDays = o.optDouble("iv", 0.0).toFloat(),
            ease = o.optDouble("e", 2.3).toFloat(),
            recentFormats = formats,
            recentErrors = o.optJSONArray("re").toStringList()
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotBlank() } }
    }
}
