package com.controlparental.kioscosuave.ui

/** Real engine rounds enter the scene and produce the exact answers expected by grading. */
fun main() {
    val engine = com.controlparental.kioscosuave.ChallengeEngine
    repeat(300) {
        for (word in listOf("bee", "cat", "Blue", "butterfly")) {
            val round = SpellingAdventureRound(word, "How do you spell '$word'?", emptyList(), true)
            check(word !in round.prompt) { "The prompt must not reveal the spelling" }
            check(round.slots(emptyList()) == List(word.length) { "_" })
            val remaining = round.choices.indices.toMutableList()
            val indices = word.map { letter ->
                remaining.first { round.choices[it] == letter.toString() }.also { remaining.remove(it) }
            }
            check(indices.distinct().size == word.length)
            check(round.selection(indices) == word.uppercase().toList().joinToString("-"))
            check(round.slots(indices).joinToString("") == word)
            check(round.selection(indices.dropLast(1)).split("-").size < word.length)
        }
        val exercise = engine.randomEnglish(starter = true, spellingWords = listOf("bee", "cat", "fish"))
        val word = exercise.visualWord!!
        val building = exercise.correctAnswer == word.uppercase().toList().joinToString("-")
        val round = SpellingAdventureRound(word, exercise.question, exercise.options, building)
        if (!building) {
            val index = round.choices.indexOf(exercise.correctAnswer)
            check(index >= 0)
            check(round.selection(listOf(index)) == exercise.correctAnswer)
            check(round.selection(emptyList()).isEmpty())
            if (exercise.question.startsWith("_")) {
                check(round.slots(emptyList()).joinToString("") == exercise.question)
                check(round.slots(listOf(index)).joinToString("").equals(word, ignoreCase = true))
            }
        }
    }
    val bee = SpellingAdventureRound("bee", "_ee", listOf("B", "C", "D", "E"), false)
    check(bee.prompt == "Find the missing letter.")
    check(bee.slots(listOf(1)).joinToString("") == "Cee") // No correction until the parent grades.
    val words = SpellingAdventureRound("cat", "Which word starts with C?", listOf("cat", "dog"), false)
    check(words.speech == "Find a word that starts with see.")
    check("cat" !in words.prompt && "cat" !in words.speech)
    println("PASS: 1,200 spelling builds (including repeated letters), 300 engine rounds, prompts and corrections.")
}
