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
    private val names = listOf("María", "Luis", "Ana", "Pedro", "Sofía", "Diego", "Lucía", "Mateo")
    private val things = listOf("manzanas", "canicas", "lápices", "galletas", "stickers", "monedas")

    /**
     * Genera un problema de matemáticas mezclando OPERACIONES directas y
     * SITUACIONES (problemas de contexto). [exclude] evita repetir el mismo
     * enunciado al avanzar al siguiente ejercicio.
     */
    fun generateMath(difficulty: Difficulty, exclude: String? = null): MathQuestion {
        val generators: List<() -> MathQuestion> = when (difficulty) {
            Difficulty.EASY ->
                listOf(::opAddition, ::opSubtraction, ::wordAddition, ::wordSubtraction)
            Difficulty.MEDIUM ->
                listOf(::opMultiplication, ::opDivision, ::wordMultiplication, ::wordDivision)
            Difficulty.HARD ->
                listOf(::opLinear, ::wordLinear, ::wordPurchase, ::wordPercentage)
        }
        var q = generators.random().invoke()
        var tries = 0
        while (exclude != null && q.question == exclude && tries < 6) {
            q = generators.random().invoke()
            tries++
        }
        return q
    }

    // --- Operaciones directas ---
    private fun opAddition(): MathQuestion {
        val a = Random.nextInt(3, 15); val b = Random.nextInt(2, 11); val ans = a + b
        return MathQuestion("¿Cuánto es $a + $b?",
            distinctOptions(ans, listOf(ans + 2, ans - 3, ans + 5)), ans.toString(),
            "Suma directa: $a + $b = $ans.")
    }

    private fun opSubtraction(): MathQuestion {
        val a = Random.nextInt(8, 20); val b = Random.nextInt(2, a); val ans = a - b
        return MathQuestion("¿Cuánto es $a − $b?",
            distinctOptions(ans, listOf(ans + 2, ans + 1, ans + 4)), ans.toString(),
            "Resta directa: $a − $b = $ans.")
    }

    private fun opMultiplication(): MathQuestion {
        val a = Random.nextInt(4, 12); val b = Random.nextInt(3, 10); val ans = a * b
        return MathQuestion("¿Cuánto es $a × $b?",
            distinctOptions(ans, listOf(ans + 4, ans - 6, ans + 10)), ans.toString(),
            "Multiplicando $a por $b resulta en $ans.")
    }

    private fun opDivision(): MathQuestion {
        val b = Random.nextInt(2, 10); val ans = Random.nextInt(2, 10); val a = b * ans
        return MathQuestion("¿Cuánto es $a ÷ $b?",
            distinctOptions(ans, listOf(ans + 1, ans + 2, ans - 1)), ans.toString(),
            "$a ÷ $b = $ans.")
    }

    private fun opLinear(): MathQuestion {
        val x = Random.nextInt(3, 10); val coeff = Random.nextInt(2, 5); val c = Random.nextInt(1, 11)
        val right = coeff * x + c
        return MathQuestion("Resuelve para X:  ${coeff}X + $c = $right",
            distinctOptions(x, listOf(x + 2, x - 1, x + 3)), x.toString(),
            "Restamos $c: ${coeff}X = ${right - c}. Dividimos entre $coeff: X = $x.")
    }

    // --- Situaciones (problemas de contexto) ---
    private fun wordAddition(): MathQuestion {
        val n = names.random(); val o = things.random()
        val a = Random.nextInt(3, 15); val b = Random.nextInt(2, 10); val ans = a + b
        return MathQuestion(
            "$n tenía $a $o y consiguió $b más. ¿Cuántas $o tiene ahora?",
            distinctOptions(ans, listOf(ans + 2, ans - 3, ans + 4)), ans.toString(),
            "Hay que sumar: $a + $b = $ans.")
    }

    private fun wordSubtraction(): MathQuestion {
        val n = names.random(); val o = things.random()
        val a = Random.nextInt(8, 20); val b = Random.nextInt(2, a); val ans = a - b
        return MathQuestion(
            "$n tenía $a $o y regaló $b. ¿Cuántas $o le quedan?",
            distinctOptions(ans, listOf(ans + 2, ans + 1, ans + 3)), ans.toString(),
            "Hay que restar: $a − $b = $ans.")
    }

    private fun wordMultiplication(): MathQuestion {
        val o = things.random(); val boxes = Random.nextInt(3, 9); val per = Random.nextInt(3, 9)
        val ans = boxes * per
        return MathQuestion(
            "Cada caja trae $per $o. Si hay $boxes cajas, ¿cuántas $o hay en total?",
            distinctOptions(ans, listOf(ans + boxes, ans - per, ans + 5)), ans.toString(),
            "Multiplicamos: $boxes × $per = $ans.")
    }

    private fun wordDivision(): MathQuestion {
        val o = things.random(); val per = Random.nextInt(2, 9); val kids = Random.nextInt(2, 8)
        val total = per * kids; val ans = per
        return MathQuestion(
            "Se reparten $total $o en partes iguales entre $kids niños. ¿Cuántas le tocan a cada uno?",
            distinctOptions(ans, listOf(ans + 1, ans + 2, ans - 1)), ans.toString(),
            "Dividimos: $total ÷ $kids = $ans.")
    }

    private fun wordLinear(): MathQuestion {
        val x = Random.nextInt(2, 11); val m = Random.nextInt(2, 5); val b = Random.nextInt(1, 10)
        val result = m * x + b
        return MathQuestion(
            "Pienso un número, lo multiplico por $m y le sumo $b; obtengo $result. ¿Qué número pensé?",
            distinctOptions(x, listOf(x + 1, x + 2, x - 1)), x.toString(),
            "Planteamos ${m}·n + $b = $result → n = ($result − $b) ÷ $m = $x.")
    }

    private fun wordPurchase(): MathQuestion {
        val n = names.random(); val count = Random.nextInt(2, 6)
        val unit = Random.nextInt(8, 20); val ship = Random.nextInt(5, 15)
        val total = count * unit + ship; val ans = unit
        return MathQuestion(
            "$n compró $count cuadernos y pagó \$$total en total, incluidos \$$ship de envío. ¿Cuánto costó cada cuaderno?",
            distinctOptions(ans, listOf(ans + 2, ans - 1, ans + 4)), ans.toString(),
            "Quitamos el envío y dividimos: (\$$total − \$$ship) ÷ $count = \$$ans.")
    }

    private fun wordPercentage(): MathQuestion {
        val price = Random.nextInt(1, 11) * 20
        val disc = listOf(10, 20, 25, 50).random()
        val off = price * disc / 100; val ans = price - off
        return MathQuestion(
            "Un artículo de \$$price tiene $disc% de descuento. ¿Cuánto pagas al final?",
            distinctOptions(ans, listOf(ans + 5, ans - 5, price)), ans.toString(),
            "$disc% de \$$price = \$$off; entonces \$$price − \$$off = \$$ans.")
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

    fun randomEnglish(exclude: String? = null): EnglishExercise {
        val pool = englishBank.filter { it.question != exclude }.ifEmpty { englishBank }
        return pool.random().let { it.copy(options = it.options.shuffled()) }
    }

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
