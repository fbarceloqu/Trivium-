package com.controlparental.kioscosuave.ui

import com.controlparental.kioscosuave.SpellingLetters

/** Presentation only: answers still go through MultipleChoiceStage's single submission. */
internal class SpellingAdventureRound(
    val word: String,
    question: String,
    options: List<String>,
    val building: Boolean
) {
    val missingLetter = question.startsWith("_")
    private val choosingWord = !building && options.any { it.length > 1 }
    val prompt = when {
        building -> "Build the word with your team."
        missingLetter -> "Find the missing letter."
        choosingWord -> "Which word starts with ${word.first().uppercaseChar()}?"
        else -> "Which letter starts the word?"
    }
    val speech = when {
        choosingWord -> "Find a word that starts with ${SpellingLetters.letterName(word.first())}."
        else -> "$prompt $word."
    }
    // Tile indices, not letters, are identities: BEE has two independently usable Es.
    val choices = if (building) SpellingLetters.bank(word).map(Char::toString) else options
    val incompleteWord: String? = question.takeIf { missingLetter }

    fun selection(indices: List<Int>): String =
        if (building) SpellingLetters.answer(indices.map { choices[it].single() })
        else indices.lastOrNull()?.let { choices[it] }.orEmpty()

    fun slots(indices: List<Int>): List<String> = when {
        building -> List(word.length) { indices.getOrNull(it)?.let { index -> choices[index] } ?: "_" }
        missingLetter -> incompleteWord!!.map { if (it == '_') indices.lastOrNull()?.let { index -> choices[index] } ?: "_" else it.toString() }
        else -> emptyList()
    }
}
