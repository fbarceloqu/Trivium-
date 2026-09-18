package com.controlparental.kioscosuave

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.controlparental.kioscosuave.curriculum.NarrativeRequest
import com.controlparental.kioscosuave.curriculum.NarrativeValidator
import com.controlparental.kioscosuave.curriculum.Verdict
import org.json.JSONArray
import org.json.JSONObject

/**
 * ONLINE-FIRST: contexto generado por IA sobre matemáticas deterministas.
 * (Fase 3)
 *
 * CÓMO FUNCIONA
 * -------------
 * La app genera el ejercicio COMPLETO —números, opciones y respuesta— con sus
 * generadores deterministas. A la IA solo se le pide que reescriba el ENUNCIADO
 * con otro contexto, para que el niño no vea siempre la misma redacción.
 *
 * La IA nunca ve ni produce la respuesta correcta. Todo lo que devuelve pasa
 * por [NarrativeValidator], que rechaza cualquier texto que cambie un número,
 * invente datos, regale la respuesta o se salga del papel. Si algo no cuadra,
 * se usa el enunciado original: el peor caso es una redacción menos variada,
 * nunca un ejercicio mal calificado.
 *
 * POR LOTES, NO POR EJERCICIO
 * ---------------------------
 * Una sola llamada trae varias narrativas. Un niño no espera 8 segundos por
     * pregunta: la sesión arranca con contenido local y las narrativas llegan de
 * fondo para las preguntas siguientes. Además, una llamada por ejercicio
 * agotaría la cuota gratuita en pocos días con tres tablets.
 *
 * EL BANCO CRECE
 * --------------
 * Cada narrativa validada se guarda en [NarrativeBank]. El modo sin conexión
 * tira de ahí primero, así que cuanto más se use la app con internet, mejor se
 * pone la experiencia sin internet.
 */
object AiNarrator {

    private const val TAG = "TriviumNarrator"
    private val main = Handler(Looper.getMainLooper())

    /** Cuántos enunciados se piden en una sola llamada. */
    const val BATCH = 6

    /**
     * ¿Hay una red utilizable? Se comprueba antes de intentar la llamada para
     * no gastar el timeout completo cuando es obvio que no hay conexión.
     *
     * NET_CAPABILITY_VALIDATED distingue una red que de verdad sale a internet
     * de un wifi conectado pero sin salida (el caso típico en una escuela).
     */
    fun isOnline(ctx: Context): Boolean = try {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        caps != null &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo consultar la red: ${e.message}")
        false
    }

    /**
     * Pide narrativas para [requests] y devuelve solo las que pasan validación,
     * indexadas por skillId. Mapa vacío si no hay red o todo se
     * rechazó: el llamador simplemente sigue con sus enunciados originales.
     */
    fun narrate(
        ctx: Context,
        requests: List<NarrativeRequest>,
        onResult: (Map<String, String>) -> Unit
    ) {
        if (requests.isEmpty() || !isOnline(ctx)) {
            main.post { onResult(emptyMap()) }
            return
        }

        TriviumAiApi.generateNarratives(ctx, requests) { raw ->
            val result = try {
                if (raw == null) emptyMap() else parseAndValidate(ctx, requests, raw)
            } catch (e: Exception) {
                Log.w(TAG, "Narración inválida, se usan los enunciados locales: ${e.message}")
                emptyMap()
            }
            main.post { onResult(result) }
        }
    }

    // -----------------------------------------------------------------

    private fun parseAndValidate(
        ctx: Context,
        requests: List<NarrativeRequest>,
        raw: String
    ): Map<String, String> {
        val arr = JSONArray(raw)
        val out = HashMap<String, String>()
        var rechazadas = 0

        for (k in 0 until arr.length()) {
            val o = arr.optJSONObject(k) ?: continue
            val idx = o.optInt("i", -1)
            val req = requests.getOrNull(idx) ?: continue

            when (val v = NarrativeValidator.validate(req, o.optString("q"))) {
                is Verdict.Ok -> {
                    out[req.skillId] = v.text
                    NarrativeBank.remember(ctx, req.skillId, v.text)
                }
                is Verdict.Rejected -> {
                    rechazadas++
                    Log.i(TAG, "Narrativa rechazada (${req.skillId}): ${v.reason}")
                }
            }
        }
        Log.i(TAG, "Narrativas: ${out.size} aceptadas, $rechazadas rechazadas")
        return out
    }
}

/**
 * Banco de enunciados validados. Es lo que hace que el modo sin conexión mejore
 * con el uso: lo que se generó online queda disponible después sin red.
 *
 * Se guarda poco a propósito (unos pocos por habilidad): el objetivo es variar
 * la redacción, no acumular. Un tope evita que SharedPreferences engorde.
 */
object NarrativeBank {

    private const val PREFS = "trivium_narrativas"
    private const val KEY = "bank_v1"
    private const val MAX_POR_HABILIDAD = 5

    fun remember(ctx: Context, skillId: String, text: String) {
        try {
            val root = load(ctx)
            val list = root.optJSONArray(skillId) ?: JSONArray()
            // Sin duplicados y con tope: se conserva lo más reciente.
            val actuales = (0 until list.length()).map { list.getString(it) }
            if (text in actuales) return
            val nuevas = (listOf(text) + actuales).take(MAX_POR_HABILIDAD)
            root.put(skillId, JSONArray(nuevas))
            prefs(ctx).edit().putString(KEY, root.toString()).apply()
        } catch (e: Exception) {
            Log.w("TriviumBank", "No se pudo guardar la narrativa: ${e.message}")
        }
    }

    /** Enunciados guardados para una habilidad (los usa el modo sin conexión). */
    fun forSkill(ctx: Context, skillId: String): List<String> = try {
        val list = load(ctx).optJSONArray(skillId) ?: JSONArray()
        (0 until list.length()).map { list.getString(it) }
    } catch (e: Exception) {
        emptyList()
    }

    private fun load(ctx: Context): JSONObject =
        runCatching { JSONObject(prefs(ctx).getString(KEY, "{}") ?: "{}") }
            .getOrElse { JSONObject() }

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
