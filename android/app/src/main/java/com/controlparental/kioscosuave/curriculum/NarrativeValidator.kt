package com.controlparental.kioscosuave.curriculum

/**
 * VALIDADOR DE NARRATIVA  (Fase 3, online-first)
 * ==============================================
 *
 * Kotlin puro, verificable fuera del dispositivo.
 *
 * EL PRINCIPIO
 * ------------
 * La IA NUNCA produce la respuesta correcta. La app genera el ejercicio
 * completo de forma determinista (números y clave incluidos) y la IA solo
 * REESCRIBE el enunciado para darle un contexto distinto y evitar la monotonía.
 *
 *   App:  "¿Cuánto es el 20% de 160?"   respuesta 32
 *   IA:   "Una camisa de $160 tiene 20% de descuento. ¿Cuánto te ahorras?"
 *   App:  verifica y sigue usando SU respuesta, 32
 *
 * Con esto, una alucinación del modelo puede como mucho producir un enunciado
 * raro —que este validador rechaza— pero jamás una respuesta equivocada.
 *
 * QUÉ SE COMPRUEBA
 * ----------------
 * Fail-closed: ante cualquier duda se rechaza y se usa el enunciado original.
 * Un texto raro se nota; una respuesta mal calificada mina la confianza del
 * niño en la app.
 */

/** Enunciado determinista que la IA debe reescribir sin alterar los datos. */
data class NarrativeRequest(
    val skillId: String,
    val original: String,
    /** La respuesta correcta; no debe aparecer regalada en el enunciado. */
    val answer: String
) {
    /** Números que el enunciado reescrito DEBE conservar. */
    val requiredNumbers: List<String> = NUMBER.findAll(original).map { it.value }.distinct().toList()

    companion object {
        val NUMBER = Regex("""\d+(?:[.,]\d+)?""")
    }
}

sealed interface Verdict {
    data class Ok(val text: String) : Verdict
    data class Rejected(val reason: String) : Verdict
}

object NarrativeValidator {

    private const val MIN_LEN = 20
    private const val MAX_LEN = 320

    /**
     * Frases que delatan que el modelo se salió de su papel: se filtró el
     * razonamiento, intentó dar la respuesta, o alguien intentó inyectar
     * instrucciones. Cualquiera de ellas invalida el texto.
     */
    private val FORBIDDEN = listOf(
        "respuesta correcta", "la respuesta es", "solución:", "resultado:",
        "como ia", "como modelo", "no puedo", "lo siento",
        "ignora", "instrucciones anteriores", "system", "assistant",
        "```", "<", ">"
    )

    fun validate(req: NarrativeRequest, candidate: String?): Verdict {
        val text = candidate?.trim().orEmpty()

        if (text.isBlank()) return Verdict.Rejected("vacío")
        if (text.length < MIN_LEN) return Verdict.Rejected("demasiado corto (${text.length})")
        if (text.length > MAX_LEN) return Verdict.Rejected("demasiado largo (${text.length})")

        val lower = text.lowercase()
        FORBIDDEN.firstOrNull { lower.contains(it) }?.let {
            return Verdict.Rejected("contiene texto prohibido: «$it»")
        }

        // Debe seguir siendo una PREGUNTA, no una explicación.
        if (!text.contains('?')) return Verdict.Rejected("no es una pregunta")

        // Debe estar en español: se exige la apertura de interrogación, que es
        // la marca más fiable y difícil de producir por accidente en inglés.
        if (!text.contains('¿')) return Verdict.Rejected("no parece español (falta ¿)")

        // Todos los datos numéricos del original deben seguir presentes: si la
        // IA cambió un número, la respuesta que calculó la app ya no aplica.
        val presentes = NarrativeRequest.NUMBER.findAll(text).map { it.value }.toSet()
        val faltantes = req.requiredNumbers.filterNot { it in presentes }
        if (faltantes.isNotEmpty()) {
            return Verdict.Rejected("faltan datos del original: ${faltantes.joinToString()}")
        }

        // No debe aparecer un número que el original no tenía: sería un dato
        // inventado que cambia el problema.
        val extras = presentes.filterNot { it in req.requiredNumbers }
        if (extras.isNotEmpty()) {
            return Verdict.Rejected("datos inventados: ${extras.joinToString()}")
        }

        // No debe regalar la respuesta. Solo aplica si la respuesta no era ya
        // uno de los datos del enunciado (en sucesiones puede coincidir).
        val ans = req.answer.trim()
        if (ans.isNotEmpty() && ans !in req.requiredNumbers && lower.contains(ans.lowercase())) {
            return Verdict.Rejected("regala la respuesta («$ans»)")
        }

        return Verdict.Ok(text)
    }
}
