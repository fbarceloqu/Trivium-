package com.controlparental.kioscosuave

import java.text.Normalizer
import kotlin.random.Random

data class MathQuestion(
    val question: String,
    val options: List<String>,
    val answer: String,
    val explanation: String
)

data class EnglishExercise(
    val instruction: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String
)

data class ReadingPassage(
    val title: String,
    val text: String
)

data class SummaryResult(
    val approved: Boolean,
    val score: Int,
    val feedback: String,
    val suggestions: String
)

/**
 * Motor de retos 100% offline (v1 standalone). Equivale a los generadores del
 * simulador web y a los fallbacks locales del backend, portados a Kotlin.
 */
object ChallengeEngine {

    // ---------------- MATEMÁTICAS ----------------
    fun generateMath(difficulty: Difficulty): MathQuestion = when (difficulty) {
        Difficulty.EASY -> {
            val n1 = Random.nextInt(3, 15)
            val n2 = Random.nextInt(2, 11)
            val ans = n1 + n2
            MathQuestion(
                question = "¿Cuánto es $n1 + $n2?",
                options = distinctOptions(ans, listOf(ans + 2, ans - 3, ans * 2)),
                answer = ans.toString(),
                explanation = "Suma directa: $n1 + $n2 = $ans."
            )
        }
        Difficulty.MEDIUM -> {
            val n1 = Random.nextInt(4, 12)
            val n2 = Random.nextInt(3, 10)
            val ans = n1 * n2
            MathQuestion(
                question = "¿Cuánto es $n1 × $n2?",
                options = distinctOptions(ans, listOf(ans + 4, ans - 6, ans + 10)),
                answer = ans.toString(),
                explanation = "Multiplicando $n1 por $n2 resulta en $ans."
            )
        }
        Difficulty.HARD -> {
            val x = Random.nextInt(3, 10)
            val coeff = Random.nextInt(2, 5)
            val constant = Random.nextInt(1, 11)
            val rightSide = coeff * x + constant
            MathQuestion(
                question = "Resuelve para X:  ${coeff}X + $constant = $rightSide",
                options = distinctOptions(x, listOf(x + 2, x - 1, x * 2)),
                answer = x.toString(),
                explanation = "Restamos $constant: ${coeff}X = ${rightSide - constant}. Dividimos entre $coeff: X = $x."
            )
        }
    }

    private fun distinctOptions(correct: Int, distractors: List<Int>): List<String> {
        val set = LinkedHashSet<Int>()
        set.add(correct)
        distractors.forEach { if (it != correct && it >= 0) set.add(it) }
        var filler = correct + 1
        while (set.size < 4) { if (filler != correct) set.add(filler); filler++ }
        return set.toList().shuffled().map { it.toString() }
    }

    // ---------------- INGLÉS (Past Tense) ----------------
    private val englishBank = listOf(
        EnglishExercise(
            "Elige la forma correcta del verbo 'run' en pasado.",
            "Yesterday, Liam ______ to school because he was late.",
            listOf("runned", "ran", "runs", "running"), "ran",
            "El pasado de 'run' (correr) es irregular: 'ran'."
        ),
        EnglishExercise(
            "Elige la forma correcta en pasado del verbo 'go'.",
            "Last weekend, we ______ to the beach with our dog.",
            listOf("goed", "goes", "went", "going"), "went",
            "El pasado de 'go' (ir) es irregular: 'went'."
        ),
        EnglishExercise(
            "Identifica el verbo en pasado simple de la oración.",
            "Which word is in the past tense? 'She watched a movie.'",
            listOf("She", "watched", "movie", "a"), "watched",
            "'watched' es el pasado regular de 'watch' (mirar)."
        ),
        EnglishExercise(
            "Traduce usando el tiempo pasado.",
            "Nosotros comimos manzanas ayer.",
            listOf(
                "We ate apples yesterday", "We eat apples yesterday",
                "We eaten apples yesterday", "We eated apples yesterday"
            ), "We ate apples yesterday",
            "'eat' es irregular: su pasado es 'ate'. 'ayer' = 'yesterday'."
        ),
        EnglishExercise(
            "Completa con el pasado de 'see'.",
            "I ______ a beautiful bird this morning.",
            listOf("seed", "saw", "seen", "sees"), "saw",
            "El pasado de 'see' (ver) es irregular: 'saw'."
        )
    )

    fun randomEnglish(): EnglishExercise =
        englishBank.random().let { it.copy(options = it.options.shuffled()) }

    // ---------------- LECTURA ----------------
    private val readingBank = listOf(
        ReadingPassage(
            "El pingüino emperador",
            "El pingüino emperador es la especie de pingüino más grande del mundo. Vive en la fría Antártida. A pesar de ser un ave, no puede volar, pero es un nadador excepcional que caza peces en el océano helado."
        ),
        ReadingPassage(
            "Las abejas y las flores",
            "Las abejas son insectos trabajadores que vuelan de flor en flor recolectando néctar para hacer miel. Al hacer esto, transportan el polen de las flores, lo cual ayuda a que crezcan nuevas plantas y frutos."
        ),
        ReadingPassage(
            "El misterio de la Luna",
            "La Luna es el único satélite natural de la Tierra. No tiene luz propia, sino que refleja la luz del Sol. Tarda aproximadamente 28 días en dar una vuelta completa alrededor de nuestro planeta."
        )
    )

    fun randomReading(): ReadingPassage = readingBank.random()

    // ---------------- EVALUACIÓN DE RESUMEN (heurística local) ----------------
    /**
     * Evaluación offline del resumen. Normaliza acentos/puntuación y exige
     * coincidencia de palabras clave. Fail-closed: nunca aprueba texto vacío,
     * demasiado corto o sin relación con la lectura.
     */
    fun evaluateSummary(readingText: String, userSummary: String): SummaryResult {
        val cleanSummary = normalize(userSummary)
        val words = cleanSummary.split(Regex("\\s+")).filter { it.isNotBlank() }

        if (words.size < 12) {
            return SummaryResult(
                approved = false,
                score = 40,
                feedback = "Tu resumen es un poco corto para evaluar tu comprensión.",
                suggestions = "Escribe al menos un par de oraciones describiendo de qué trata la lectura."
            )
        }

        val summarySet = words.toHashSet()
        val keywords = normalize(readingText)
            .split(Regex("\\s+"))
            .filter { it.length > 5 }
        val matches = keywords.count { summarySet.contains(it) }

        return if (matches >= 2) {
            SummaryResult(
                approved = true,
                score = minOf(65 + matches * 8, 100),
                feedback = "¡Buen trabajo! Tu resumen demuestra que entendiste las ideas clave de la lectura.",
                suggestions = "Excelente esfuerzo de redacción autónoma."
            )
        } else {
            SummaryResult(
                approved = false,
                score = 55,
                feedback = "Escribiste un buen texto, pero intenta incluir más ideas de la lectura.",
                suggestions = "Relee el texto y menciona de qué animal o tema se está hablando."
            )
        }
    }

    private fun normalize(s: String): String {
        val noAccents = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return noAccents.replace(Regex("[^a-z0-9ñ\\s]"), " ")
    }
}
