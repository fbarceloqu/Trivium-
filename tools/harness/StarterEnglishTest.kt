import com.controlparental.kioscosuave.ChallengeEngine
import com.controlparental.kioscosuave.MathQuestion

/** Run with the pure Kotlin engine, without Android or a speech engine. */
fun main() {
    repeat(100) {
        for (word in listOf("blue", "bee", "Blue", "butterfly")) {
            val bank = com.controlparental.kioscosuave.SpellingLetters.bank(word)
            check(bank.size == word.length + 3)
            word.toSet().forEach { letter -> check(bank.count { it == letter } >= word.count { it == letter }) }
        }
    }
    check(com.controlparental.kioscosuave.SpellingLetters.answer(listOf('b', 'l', 'u', 'e')) == "B-L-U-E")
    check(com.controlparental.kioscosuave.SpellingLetters.answer(listOf('b', 'e', 'l', 'u')) != "B-L-U-E")
    val letters = com.controlparental.kioscosuave.SpellingLetters
    check(letters.reviewSpeech("blue") == "bee, el, you, ee, blue.")
    check(letters.reviewSpeech("cat") == "see, ay, tee, cat.")
    check(letters.reviewSpeech("dog") == "dee, oh, jee, dog.")
    check(letters.reviewSpeech("bee") == "bee, ee, ee, bee.")
    check(letters.reviewSpeech("fish") == "eff, eye, ess, aitch, fish.")
    check(letters.reviewSpeech("Blue") == "bee, el, you, ee, Blue.")
    check(('a'..'z').all { letters.letterName(it).length > 1 })
    check(letters.letterName('A') == "ay")

    check(ChallengeEngine.optionSpeech("🧥") == "jacket")
    check(ChallengeEngine.optionSpeech("👮") == "police officer")
    check(ChallengeEngine.optionSpeech("🔵") == "blue circle")
    check(ChallengeEngine.optionSpeech("3 + 1 = 4") == "3 plus 1 equals 4")
    check(ChallengeEngine.optionSpeech("B") == "B")
    check(ChallengeEngine.optionSpeech("no") == "no")
    val spanish = Regex("[¿¡áéíóúñ]|\\b(Cuenta|Ejemplo|Hay|Quedan|Pronunciación)\\b")
    val generators = listOf("countObjects", "addObjects", "subObjects", "whichSumShown",
        "moreOrLess", "numberSequence", "patternNext", "biggerNumber")
    for (name in generators) {
        val method = ChallengeEngine::class.java.getDeclaredMethod(name).apply { isAccessible = true }
        repeat(200) {
            val q = method.invoke(ChallengeEngine) as MathQuestion
            check(q.answer in q.options)
            check(q.options.distinct().size == q.options.size)
            val text = (listOf(q.question, q.example.title) + q.steps + q.example.lines + q.options).joinToString("\n")
            check(!spanish.containsMatchIn(text)) { "$name contains Spanish: $text" }
            if (name == "addObjects" || name == "whichSumShown") check(" + " in q.question)
            if (name == "subObjects") check(" − " in q.question)
            if (name == "moreOrLess") {
                check(q.question.startsWith("Are there "))
                check(q.options == listOf("yes", "no"))
                val rows = q.question.lines().filter { it.isNotBlank() }.drop(1)
                val counts = rows.map { it.trim().split(" ").size }
                val yes = if (q.question.startsWith("Are there more")) counts[0] > counts[1]
                    else counts[0] < counts[1]
                check(q.answer == if (yes) "yes" else "no")
            }
        }
    }
    val naming = ChallengeEngine::class.java.getDeclaredMethod("comparisonName", String::class.java)
        .apply { isAccessible = true }
    check(naming.invoke(ChallengeEngine, "🦆") == "ducks")
    check(naming.invoke(ChallengeEngine, "🎸") == "guitars")
    check(naming.invoke(ChallengeEngine, "🐭") == "mice")
    check(naming.invoke(ChallengeEngine, "🦋") == "butterflies")
    repeat(500) {
        for (q in listOf(ChallengeEngine.randomEnglish(starter = true),
            ChallengeEngine.randomEnglish(starter = true, spellingLetter = 'B'),
            ChallengeEngine.randomEnglish(starter = true, spellingWords = listOf("sun", "top", "bee")))) {
            check(q.correctAnswer in q.options)
            check(!spanish.containsMatchIn("${q.instruction} ${q.question} ${q.explanation}"))
        }
        val olderSpelling = ChallengeEngine.randomEnglish(spellingLetter = 'B')
        check(spanish.containsMatchIn(olderSpelling.question) || olderSpelling.instruction.startsWith("Completa"))
    }
    val olderMath = ChallengeEngine::class.java.getDeclaredMethod("opAddition").apply { isAccessible = true }
    check((olderMath.invoke(ChallengeEngine) as MathQuestion).question.startsWith("¿Cuánto es"))
    println("PASS: 1,600 starter math and 1,500 English/spelling exercises; Spanish spelling preserved.")
}
