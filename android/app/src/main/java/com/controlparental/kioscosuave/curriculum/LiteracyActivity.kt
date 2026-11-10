package com.controlparental.kioscosuave.curriculum

import java.text.Normalizer
import java.util.Locale

enum class LiteracyKind(val label: String) {
    SYLLABLES("Formar palabras"), WORD_IMAGE("Palabras y dibujos"),
    SENTENCE("Leer oraciones"), STORY("Comprender cuentos")
}

/** Educational content, independent of Compose, network and the existing question contracts. */
data class LiteracyActivity(
    val id: String,
    val skillId: String,
    val kind: LiteracyKind,
    val question: String,
    val choices: List<String>,
    val answer: String,
    val explanation: String,
    val imageKey: String,
    val imageFallback: String,
    val passage: String = "",
    val evidence: String = "",
    val syllables: List<String> = emptyList()
) {
    val format: ExerciseFormat get() = when (kind) {
        LiteracyKind.SYLLABLES, LiteracyKind.WORD_IMAGE -> ExerciseFormat.DIRECTO
        LiteracyKind.SENTENCE -> ExerciseFormat.CONTEXTO
        LiteracyKind.STORY -> ExerciseFormat.RAZONAMIENTO
    }
    val buildsWord: Boolean get() = kind == LiteracyKind.SYLLABLES
    fun canSubmit(selection: List<Int>): Boolean =
        selection.distinct().size == selection.size && selection.all { it in choices.indices } &&
            selection.size == if (buildsWord) syllables.size else 1

    fun response(selection: List<Int>): String = selection.joinToString(if (buildsWord) "" else " ") { choices[it] }
    fun correct(selection: List<Int>): Boolean = canSubmit(selection) && normalize(response(selection)) == normalize(answer)

    companion object {
        // Accents are meaningful in Spanish; do not collapse PAPÁ and PAPA.
        fun normalize(text: String): String = Normalizer.normalize(text.trim(), Normalizer.Form.NFC).uppercase(Locale.ROOT)
    }
}

object LiteracyValidator {
    fun errors(a: LiteracyActivity): List<String> = buildList {
        if (a.id.isBlank() || a.skillId !in LiteracyCatalog.skills.map { it.id }) add("Identidad inválida")
        if (a.question.isBlank() || a.answer.isBlank() || a.explanation.isBlank()) add("Contenido incompleto")
        if (a.imageKey.isBlank() || a.imageFallback.isBlank()) add("Falta ilustración local")
        if (a.choices.any { it.isBlank() }) add("Opción vacía")
        if (a.buildsWord) {
            if (a.syllables.size !in 2..3 || a.syllables.joinToString("") != a.answer) add("Segmentación inválida")
            if (!a.syllables.all { s -> a.choices.count { it == s } >= a.syllables.count { it == s } }) add("Faltan fichas")
            // Different physical copies of the same syllable are allowed; different words are not.
            if (a.choices.size < a.syllables.size + 2) add("Faltan distractores")
            fun words(prefix: List<Int>) {
                if (prefix.size == a.syllables.size) {
                    if (a.correct(prefix) && prefix.map { a.choices[it] } != a.syllables) add("Segmentación ambigua")
                } else a.choices.indices.filter { it !in prefix }.forEach { words(prefix + it) }
            }
            if (a.choices.size <= 8) words(emptyList()) else add("Demasiadas fichas")
        } else {
            if (a.choices.size != 4 || a.choices.map(LiteracyActivity::normalize).distinct().size != 4) add("Opciones repetidas o incompletas")
            if (a.choices.count { LiteracyActivity.normalize(it) == LiteracyActivity.normalize(a.answer) } != 1) add("Clave no única")
        }
        if (a.kind == LiteracyKind.SENTENCE || a.kind == LiteracyKind.STORY) {
            if (a.evidence.isBlank() || !a.passage.contains(a.evidence)) add("Evidencia ausente del texto")
            if (!a.evidence.contains(a.answer, ignoreCase = true)) add("La evidencia no respalda la clave")
        }
        if (a.kind == LiteracyKind.STORY && a.passage.split(Regex("[.!?]+")).count { it.isNotBlank() } !in 3..5) add("El cuento debe tener 3–5 oraciones")
    }
}
