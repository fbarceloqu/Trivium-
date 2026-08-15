package com.controlparental.kioscosuave.curriculum

import java.text.Normalizer

/**
 * VALIDADOR DE LECTURAS GENERADAS POR IA  (Fase 4)
 * ================================================
 *
 * Kotlin puro, verificable fuera del dispositivo.
 *
 * POR QUÉ HACE FALTA
 * ------------------
 * En matemáticas la app calcula la respuesta y la IA solo reescribe el
 * enunciado, así que una alucinación no puede producir una respuesta mala.
 * En LECTURA no existe esa red: el texto entero viene del modelo. Si inventa
 * una fecha de la Independencia o pregunta algo que el texto no responde, el
 * niño estudia algo falso o se frustra sin motivo.
 *
 * Este validador no puede comprobar que la historia sea CIERTA —eso requiere
 * una fuente— pero sí puede exigir que el paquete sea COHERENTE consigo mismo,
 * que es donde fallan los modelos en la práctica:
 *
 *   · cada pregunta debe poder responderse CON EL TEXTO
 *   · la respuesta correcta debe aparecer en la lectura
 *   · los distractores NO deben aparecer como respuesta válida en el texto
 *   · nada de meta-texto, instrucciones filtradas ni inyección
 *
 * Fail-closed: ante la duda se rechaza y se usa una lectura del banco local.
 */

/** Una pregunta de comprensión sobre la lectura. */
data class ReadingQuestion(
    val question: String,
    val options: List<String>,
    val answer: String
)

/** Lectura completa generada por IA, con sus preguntas. */
data class GeneratedReading(
    val title: String,
    val text: String,
    val questions: List<ReadingQuestion>,
    /** Tema pedido, p. ej. "Independencia de México". */
    val topic: String
)

object ReadingValidator {

    private const val MIN_TEXT = 320
    private const val MAX_TEXT = 2200
    private const val MIN_QUESTIONS = 2
    private const val MAX_QUESTIONS = 6

    /**
     * SIN ACENTOS a propósito: se comparan contra el texto ya normalizado.
     * Con "aquí tienes" acentuado, una respuesta del modelo escrita sin tildes
     * se colaba entera — la detección dependía de cómo le diera por acentuar.
     */
    private val FORBIDDEN = listOf(
        "como ia", "como modelo", "lo siento", "no puedo",
        "ignora", "instrucciones anteriores", "system", "assistant",
        "aqui tienes", "espero que", "```"
    )

    /** Palabras vacías que no sirven para comprobar si algo está en el texto. */
    private val STOP = setOf(
        "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "al",
        "y", "o", "en", "con", "por", "para", "que", "se", "su", "sus", "es",
        "son", "fue", "era", "como", "mas", "pero", "esta", "este", "esto"
    )

    private fun norm(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")

    private fun words(s: String): List<String> =
        norm(s).split(Regex("[^a-z0-9]+")).filter { it.length > 2 && it !in STOP }

    /**
     * Cuánto de [phrase] aparece en [text], de 0 a 1.
     *
     * No se exige coincidencia literal: la respuesta puede estar redactada de
     * otra forma. Se mide qué proporción de sus palabras con significado
     * aparecen en la lectura.
     */
    fun coverage(phrase: String, text: String): Float {
        val w = words(phrase)
        if (w.isEmpty()) return 0f
        val body = words(text).toSet()
        return w.count { it in body }.toFloat() / w.size
    }

    fun validate(r: GeneratedReading): Verdict {
        // --- la lectura ---
        val text = r.text.trim()
        if (r.title.isBlank()) return Verdict.Rejected("sin título")
        if (text.length < MIN_TEXT) return Verdict.Rejected("lectura demasiado corta (${text.length})")
        if (text.length > MAX_TEXT) return Verdict.Rejected("lectura demasiado larga (${text.length})")

        val lower = norm(text)
        FORBIDDEN.firstOrNull { lower.contains(it) }?.let {
            return Verdict.Rejected("meta-texto en la lectura: «$it»")
        }
        // Debe hablar del tema pedido, no de lo que al modelo le apeteció.
        if (coverage(r.topic, text) < 0.5f) {
            return Verdict.Rejected("la lectura no trata el tema pedido («${r.topic}»)")
        }

        // --- las preguntas ---
        if (r.questions.size < MIN_QUESTIONS) return Verdict.Rejected("faltan preguntas (${r.questions.size})")
        if (r.questions.size > MAX_QUESTIONS) return Verdict.Rejected("demasiadas preguntas (${r.questions.size})")

        r.questions.forEachIndexed { i, q ->
            val n = i + 1
            if (!q.question.contains('?')) return Verdict.Rejected("P$n no es una pregunta")
            if (!q.question.contains('¿')) return Verdict.Rejected("P$n no parece español")
            if (q.options.size != 4) return Verdict.Rejected("P$n tiene ${q.options.size} opciones, deben ser 4")
            if (q.options.distinct().size != 4) return Verdict.Rejected("P$n tiene opciones repetidas")
            if (q.answer !in q.options) return Verdict.Rejected("P$n: la respuesta no está entre las opciones")

            norm(q.question).let { ql ->
                FORBIDDEN.firstOrNull { ql.contains(it) }?.let {
                    return Verdict.Rejected("P$n contiene meta-texto: «$it»")
                }
            }

            // LA COMPROBACIÓN CLAVE: la respuesta correcta tiene que estar en la
            // lectura. Si no, la pregunta es imposible de responder leyendo, que
            // es exactamente lo que frustra a un niño y no enseña nada.
            if (coverage(q.answer, text) < 0.6f) {
                return Verdict.Rejected("P$n: la respuesta «${q.answer}» no se puede deducir del texto")
            }

            // Y al revés: si un distractor también está plenamente en el texto,
            // la pregunta tiene dos respuestas defendibles.
            val ambiguo = q.options.firstOrNull {
                it != q.answer && coverage(it, text) >= 0.95f && words(it).size >= 2
            }
            if (ambiguo != null) {
                return Verdict.Rejected("P$n: el distractor «$ambiguo» también aparece en el texto")
            }
        }

        return Verdict.Ok(text)
    }
}
