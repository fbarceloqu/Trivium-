package com.controlparental.kioscosuave.curriculum

import java.text.Normalizer
import java.util.Locale
import kotlin.random.Random

/**
 * COMPRENSIÓN LECTORA DE SECUNDARIA
 * =================================
 *
 * Lecturas de 500–800 palabras con un cuestionario de opción múltiple. Sustituye
 * al resumen escrito: la calificación es objetiva y determinista, no depende de
 * evaluar texto libre ni de tener conexión.
 *
 * Kotlin puro (sin Android) para poder verificarlo en tools/harness.
 */

/** Qué se le pide al lector. Los ids se guardan en la memoria: NO renombrar. */
enum class ComprehensionSkill(val id: String, val label: String) {
    LITERAL("read.literal", "Comprensión literal"),
    MAIN_IDEA("read.main_idea", "Idea principal"),
    INFERENCE("read.inference", "Inferencia"),
    CAUSE_EFFECT("read.cause_effect", "Causa y consecuencia"),
    VOCABULARY("read.vocabulary", "Vocabulario en contexto"),
    CRITICAL("read.critical", "Interpretación y pensamiento crítico")
}

enum class ReadingTopic(val label: String) {
    SCIENCE("Ciencia"), HISTORY("Historia"), TECHNOLOGY("Tecnología"),
    NATURE("Naturaleza"), LITERATURE("Literatura"), DAILY_LIFE("Vida cotidiana")
}

data class LongQuestion(
    val skill: ComprehensionSkill,
    val question: String,
    /** Cuatro opciones; el orden en pantalla lo decide [LongReadingQuiz.order]. */
    val options: List<String>,
    val answer: String,
    /** Por qué es esa la respuesta. Se muestra al terminar el cuestionario. */
    val explanation: String,
    /** Fragmento LITERAL del texto que respalda la respuesta. */
    val evidence: String
)

data class LongReading(
    /** Identificador estable: es la llave del historial de lecturas. */
    val id: String,
    val topic: ReadingTopic,
    val title: String,
    val paragraphs: List<String>,
    val questions: List<LongQuestion>
) {
    val text: String get() = paragraphs.joinToString("\n\n")
    val wordCount: Int get() = paragraphs.sumOf { p -> p.split(Regex("\\s+")).count { it.any(Char::isLetterOrDigit) } }
}

// =====================================================================
//  VALIDACIÓN (antes de presentar la actividad)
// =====================================================================

object LongReadingValidator {
    const val MIN_WORDS = 500
    const val MAX_WORDS = 800
    val QUESTION_RANGE = 6..8

    private val forbidden = listOf("todas las anteriores", "ninguna de las anteriores", "a y b", "ambas son")

    fun normalize(s: String): String =
        Normalizer.normalize(s.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^a-z0-9ñ ]"), " ")
            .replace(Regex("\\s+"), " ").trim()

    fun errors(r: LongReading): List<String> = buildList {
        if (r.id.isBlank() || r.title.isBlank()) add("Sin identificador o título")
        if (r.wordCount !in MIN_WORDS..MAX_WORDS) add("Extensión ${r.wordCount} palabras (debe ser $MIN_WORDS–$MAX_WORDS)")
        if (r.paragraphs.size < 4) add("Menos de 4 párrafos")
        r.paragraphs.forEachIndexed { i, p ->
            val n = p.split(Regex("\\s+")).size
            if (n > 170) add("Párrafo ${i + 1} demasiado largo ($n palabras)")
            if (n < 25) add("Párrafo ${i + 1} demasiado corto ($n palabras)")
        }
        if (r.questions.size !in QUESTION_RANGE) add("${r.questions.size} preguntas (deben ser 6–8)")
        ComprehensionSkill.entries.filter { s -> r.questions.none { it.skill == s } }
            .forEach { add("Falta una pregunta de: ${it.label}") }
        if (r.questions.map { normalize(it.question) }.distinct().size != r.questions.size) add("Preguntas repetidas")

        val text = r.text
        var answerIsLongest = 0
        r.questions.forEachIndexed { i, q ->
            val n = "P${i + 1}"
            val norm = q.options.map(::normalize)
            if (q.question.isBlank() || q.explanation.isBlank()) add("$n: pregunta o explicación vacía")
            if (q.options.size != 4) add("$n: ${q.options.size} opciones en vez de 4")
            if (norm.any { it.isBlank() }) add("$n: opción vacía")
            if (norm.distinct().size != norm.size) add("$n: opciones repetidas o equivalentes")
            if (norm.count { it == normalize(q.answer) } != 1) add("$n: la clave no aparece exactamente una vez")
            if (norm.any { o -> forbidden.any { it in o } }) add("$n: opción comodín («todas las anteriores»)")
            // Una opción contenida en otra suele delatar cuál es la completa.
            norm.forEachIndexed { a, x -> norm.forEachIndexed { b, y -> if (a != b && x.length > 12 && x in y) add("$n: una opción contiene a otra") } }

            // La respuesta debe salir del texto, no del conocimiento previo.
            if (q.evidence.isBlank() || q.evidence !in text) add("$n: la evidencia no es un fragmento literal del texto")
            if (q.skill == ComprehensionSkill.VOCABULARY) {
                val word = Regex("«([^»]+)»").find(q.question)?.groupValues?.get(1)
                if (word == null) add("$n: la pregunta de vocabulario debe citar la palabra entre «»")
                else if (word !in q.evidence) add("$n: la palabra «$word» no está en la evidencia")
            }
            // Una respuesta copiada tal cual del texto se resuelve buscando, sin
            // comprender. Solo se tolera en preguntas literales.
            if (q.skill != ComprehensionSkill.LITERAL && q.answer.length > 25 && q.answer in text)
                add("$n: la clave es una copia literal del texto")

            // Ninguna opción debe delatarse por su tamaño.
            val lengths = q.options.map { it.length }.sorted()
            val a = q.answer.length
            if (lengths.size == 4) {
                if (a == lengths[3] && a > lengths[2] * 1.3f) add("$n: la clave es claramente la opción más larga")
                if (a == lengths[0] && a < lengths[1] * 0.7f) add("$n: la clave es claramente la opción más corta")
                if (lengths[3] > lengths[0] * 2.2f) add("$n: opciones de longitud muy desigual")
                if (a == lengths[3] && lengths[3] != lengths[2]) answerIsLongest++
            }
        }
        if (answerIsLongest > r.questions.size / 2) add("La clave es la opción más larga en $answerIsLongest preguntas: patrón adivinable")
    }
}

// =====================================================================
//  CUESTIONARIO: orden en pantalla y calificación
// =====================================================================

object LongReadingQuiz {
    /** Se aprueba con el mismo 80% que el resto de las etapas. */
    const val PASS_SCORE = 80

    data class Result(
        val correct: Int,
        val total: Int,
        /** Por pregunta, en el orden de la lectura. */
        val perQuestion: List<Boolean>
    ) {
        val score: Int get() = if (total == 0) 0 else correct * 100 / total
        val passed: Boolean get() = score >= PASS_SCORE

        /**
         * Falló MÁS de la mitad: no tiene sentido corregir pregunta por
         * pregunta (sería aprobar por descarte); toca leer otro texto.
         * Con la mitad exacta (3 de 6) todavía se permite corregir.
         */
        val mustChangeReading: Boolean get() = (total - correct) * 2 > total
        /** Tipos de pregunta fallados: lo que conviene reforzar. */
        fun weakSkills(r: LongReading): List<ComprehensionSkill> =
            r.questions.filterIndexed { i, _ -> !perQuestion[i] }.map { it.skill }.distinct()
    }

    /**
     * Orden de las opciones de cada pregunta. Sale de una semilla que se guarda,
     * así al reabrir la app las opciones están donde el alumno las dejó.
     * Devuelve, por pregunta, los índices originales en el orden de pantalla.
     */
    fun order(reading: LongReading, seed: Long): List<List<Int>> {
        val rnd = Random(seed)
        return reading.questions.map { q -> q.options.indices.shuffled(rnd) }
    }

    /** [answers]: por pregunta, el índice ORIGINAL de la opción elegida (o null). */
    fun grade(reading: LongReading, answers: List<Int?>): Result {
        val per = reading.questions.mapIndexed { i, q ->
            answers.getOrNull(i)?.let { q.options.getOrNull(it) == q.answer } ?: false
        }
        return Result(per.count { it }, per.size, per)
    }

    /**
     * Lleva el resultado del PRIMER intento a la memoria de aprendizaje, una
     * práctica por pregunta. Solo el primero: corregir después con el texto
     * delante es aprender, pero no mide lo que se entendió al leer.
     */
    fun remember(
        states: Map<String, SkillState>,
        reading: LongReading,
        answers: List<Int?>,
        now: Long
    ): Map<String, SkillState> {
        val out = HashMap(states)
        reading.questions.forEachIndexed { i, q ->
            val chosen = answers.getOrNull(i)?.let { q.options.getOrNull(it) }
            val ok = chosen == q.answer
            val prev = out[q.skill.id] ?: SkillState(q.skill.id)
            out[q.skill.id] = LearningMemory.review(prev, ok, ExerciseFormat.CONTEXTO, if (ok) null else chosen, now)
        }
        return out
    }

    /**
     * Elige la siguiente lectura: nunca una de las últimas leídas, de un tema
     * distinto al anterior si se puede, y más larga si la anterior salió bien.
     *
     * [history] son los ids ya leídos, del más antiguo al más reciente.
     */
    fun pick(
        bank: List<LongReading>,
        history: List<String>,
        lastScore: Int?,
        rnd: Random = Random
    ): LongReading {
        require(bank.isNotEmpty())
        val byId = bank.associateBy { it.id }
        // Cuanto más grande el banco, más se tarda en repetir una lectura.
        val recent = history.takeLast((bank.size - 1).coerceAtLeast(0)).toSet()
        val fresh = bank.filter { it.id !in recent }.ifEmpty {
            bank.filter { it.id != history.lastOrNull() }.ifEmpty { bank }
        }
        val lastTopic = history.lastOrNull()?.let { byId[it]?.topic }
        val otherTopic = fresh.filter { it.topic != lastTopic }.ifEmpty { fresh }
        // Extensión según desempeño: con buen resultado, la mitad más larga;
        // si costó trabajo, la mitad más corta. Sin historial, cualquiera.
        val sorted = otherTopic.sortedBy { it.wordCount }
        val half = (sorted.size + 1) / 2
        val pool = when {
            lastScore == null || sorted.size < 2 -> sorted
            lastScore >= 85 -> sorted.takeLast(half)
            lastScore < PASS_SCORE -> sorted.take(half)
            else -> sorted
        }
        return pool.random(rnd)
    }
}
