package com.controlparental.kioscosuave

import java.text.Normalizer
import kotlin.random.Random

/** Ejemplo resuelto (para el botón de ayuda 💡). */
data class WorkedExample(val title: String, val lines: List<String>)

data class MathQuestion(
    val question: String,
    val options: List<String>,
    val answer: String,
    val steps: List<String>,      // procedimiento paso a paso de ESTA pregunta
    val example: WorkedExample    // ejemplo resuelto (otro caso) para la ayuda
)

data class EnglishExercise(
    val instruction: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String,
    val phonetic: String          // pronunciación del verbo clave
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
 * Motor de retos 100% offline (v1 standalone). Genera operaciones y problemas
 * de contexto con procedimiento paso a paso, ejercicios de inglés con fonética
 * y lecturas (más amplias en secundaria).
 */
object ChallengeEngine {

    private val names = listOf("María", "Luis", "Ana", "Pedro", "Sofía", "Diego", "Lucía", "Mateo")
    private val things = listOf("manzanas", "canicas", "lápices", "galletas", "stickers", "monedas")

    // ---------- Ejemplos resueltos para el botón de ayuda ----------
    // Ejemplos para OPERACIONES DIRECTAS (mismo formato "¿Cuánto es A+B?").
    private val EX_ADD_DIRECT = WorkedExample(
        "Ejemplo resuelto: suma",
        listOf("¿Cuánto es 7 + 5?", "Sumamos:  7 + 5 = 12", "Respuesta: 12")
    )
    private val EX_SUB_DIRECT = WorkedExample(
        "Ejemplo resuelto: resta",
        listOf("¿Cuánto es 15 − 6?", "Restamos:  15 − 6 = 9", "Respuesta: 9")
    )
    private val EX_MUL_DIRECT = WorkedExample(
        "Ejemplo resuelto: multiplicación",
        listOf("¿Cuánto es 6 × 4?", "Multiplicamos:  6 × 4 = 24", "Respuesta: 24")
    )
    private val EX_DIV_DIRECT = WorkedExample(
        "Ejemplo resuelto: división",
        listOf("¿Cuánto es 24 ÷ 6?", "Dividimos:  24 ÷ 6 = 4", "Respuesta: 4")
    )

    // Ejemplos para PROBLEMAS DE CONTEXTO (mismo formato de historia/situación).
    private val EX_ADD = WorkedExample(
        "Ejemplo resuelto: suma",
        listOf("María tenía 7 y consiguió 5 más.", "Sumamos:  7 + 5 = 12", "Respuesta: 12")
    )
    private val EX_SUB = WorkedExample(
        "Ejemplo resuelto: resta",
        listOf("Luis tenía 15 y regaló 6.", "Restamos:  15 − 6 = 9", "Respuesta: 9")
    )
    private val EX_MUL = WorkedExample(
        "Ejemplo resuelto: multiplicación",
        listOf("6 cajas con 4 lápices cada una.", "Multiplicamos:  6 × 4 = 24", "Respuesta: 24")
    )
    private val EX_DIV = WorkedExample(
        "Ejemplo resuelto: división",
        listOf("Repartir 24 dulces entre 6 niños.", "Dividimos:  24 ÷ 6 = 4", "A cada uno le tocan 4")
    )
    // Ejemplo para ECUACIÓN DIRECTA ("Resuelve para X: ...").
    private val EX_LINEAR = WorkedExample(
        "Ejemplo resuelto: despejar X",
        listOf(
            "Resuelve:  4X + 8 = 44",
            "1) El 8 está sumando → pasa restando:",
            "     4X = 44 − 8  →  4X = 36",
            "2) El 4 está multiplicando → pasa dividiendo:",
            "     X = 36 ÷ 4",
            "Respuesta:  X = 9"
        )
    )
    // Ejemplo para "PIENSO UN NÚMERO..." (mismo formato que wordLinear).
    private val EX_WORD_NUMBER = WorkedExample(
        "Ejemplo resuelto: pienso un número",
        listOf(
            "Pienso un número, lo multiplico por 3 y le sumo 4;",
            "obtengo 19. ¿Qué número pensé?",
            "Sea n el número:  3·n + 4 = 19",
            "1) El 4 está sumando → pasa restando:",
            "     3·n = 19 − 4 = 15",
            "2) El 3 está multiplicando → pasa dividiendo:",
            "     n = 15 ÷ 3",
            "Respuesta:  n = 5"
        )
    )
    private val EX_PURCHASE = WorkedExample(
        "Ejemplo resuelto: precio por unidad",
        listOf(
            "Compró 3 libretas y pagó \$62, con \$14 de envío.",
            "1) Quita el envío:  62 − 14 = 48",
            "2) Divide entre la cantidad:  48 ÷ 3 = 16",
            "Cada libreta cuesta \$16"
        )
    )
    private val EX_PERCENT = WorkedExample(
        "Ejemplo resuelto: descuento",
        listOf(
            "Artículo de \$80 con 25% de descuento.",
            "1) Descuento:  80 × 25 ÷ 100 = 20",
            "2) Resta al precio:  80 − 20 = 60",
            "Pagas \$60"
        )
    )

    /** Guía de ayuda de inglés (mini-gramática general por tiempos). */
    val englishHelp = WorkedExample(
        "Mini-guía de inglés",
        listOf(
            "PASADO: verbo + -ed (watched) o irregular (go→went, eat→ate).",
            "     'Yesterday I went home.'",
            "FUTURO: will + verbo, o 'be going to'.",
            "     'Tomorrow I will play.' / 'I am going to study.'",
            "PRESENTE: con he/she/it el verbo lleva -s.",
            "     'She goes to school.' / '¿Do you like...?'",
            "PISTAS: yesterday=pasado, tomorrow/next=futuro,",
            "     every day=presente."
        )
    )

    // ---------------- MATEMÁTICAS ----------------
    fun generateMath(difficulty: Difficulty, exclude: String? = null): MathQuestion {
        val generators: List<() -> MathQuestion> = when (difficulty) {
            Difficulty.STARTER ->
                listOf(
                    ::countObjects, ::addObjects, ::subObjects, ::whichSumShown,
                    ::moreOrLess, ::numberSequence, ::patternNext, ::biggerNumber
                )
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

    // --- Preescolar / 1º: contar objetos con dibujos ---
    private val countEmojis = listOf("🍎", "⭐", "🐶", "🎈", "🚗", "🌸", "⚽", "🐟")

    private val EX_COUNT = WorkedExample(
        "Ejemplo: contar",
        listOf(
            "¿Cuántas hay?  🍎 🍎 🍎",
            "Cuenta una por una con el dedo:",
            "1... 2... 3",
            "¡Son 3!"
        )
    )

    private fun countObjects(): MathQuestion {
        val n = Random.nextInt(2, 10)
        val e = countEmojis.random()
        val row = List(n) { e }.joinToString(" ")
        return MathQuestion(
            "¿Cuántos hay?\n\n$row",
            distinctOptions(n, listOf(n + 1, n - 1, n + 2)), n.toString(),
            listOf("Cuenta uno por uno con el dedo:", row, "¡Son $n!"), EX_COUNT)
    }

    private fun addObjects(): MathQuestion {
        val a = Random.nextInt(1, 5); val b = Random.nextInt(1, 5); val ans = a + b
        val e = countEmojis.random()
        val rowA = List(a) { e }.joinToString(" ")
        val rowB = List(b) { e }.joinToString(" ")
        return MathQuestion(
            "$rowA  y  $rowB\n\n¿Cuántos hay en total?",
            distinctOptions(ans, listOf(ans + 1, ans - 1, ans + 2)), ans.toString(),
            listOf("Junta los dos grupos y cuenta todos:", "$rowA  $rowB", "¡Son $ans!"), EX_COUNT)
    }

    private val EX_RESTA = WorkedExample(
        "Ejemplo: restar contando hacia atrás",
        listOf(
            "Resta:  4 − 1 = ?",
            "Puedes contar hacia atrás para restar.",
            "Empieza desde el 4 y cuenta 1 hacia atrás:",
            "4... 3",
            "¡La respuesta es 3!"
        )
    )

    /** Resta visual: había N, se van B, ¿cuántos quedan? (contar hacia atrás). */
    private fun subObjects(): MathQuestion {
        val a = Random.nextInt(3, 10); val b = Random.nextInt(1, minOf(a, 4))
        val ans = a - b
        val e = countEmojis.random()
        val row = List(a) { e }.joinToString(" ")
        val backwards = (a downTo ans).joinToString("... ")
        return MathQuestion(
            "Hay $a. Se van $b.\n\n$row\n\n¿Cuántos quedan?",
            distinctOptions(ans, listOf(ans + 1, ans - 1, ans + 2)), ans.toString(),
            listOf(
                "Cuenta hacia atrás desde $a, $b ${if (b == 1) "vez" else "veces"}:",
                backwards,
                "¡Quedan $ans!"
            ), EX_RESTA)
    }

    private val EX_QUE_SUMA = WorkedExample(
        "Ejemplo: ¿qué suma muestra el dibujo?",
        listOf(
            "🔵 🔵    🟠",
            "Cuenta los azules: 2. Cuenta los naranjas: 1.",
            "2 + 1 = 3",
            "La suma correcta es «2 + 1 = 3»."
        )
    )

    /** Como IXL: muestra dos grupos y las opciones son ecuaciones completas. */
    private fun whichSumShown(): MathQuestion {
        val a = Random.nextInt(2, 6); val b = Random.nextInt(1, 5)
        val rowA = List(a) { "🔵" }.joinToString(" ")
        val rowB = List(b) { "🟠" }.joinToString(" ")
        val correct = "$a + $b = ${a + b}"
        val opts = LinkedHashSet<String>()
        opts.add(correct)
        opts.add("$a + ${b + 1} = ${a + b + 1}")
        opts.add("${a + 1} + $b = ${a + b + 1}")
        opts.add("$a + $b = ${a + b + 1}")
        return MathQuestion(
            "¿Qué suma muestra este dibujo?\n\n$rowA   $rowB",
            opts.toList().shuffled(), correct,
            listOf(
                "Cuenta los azules: $a. Cuenta los naranjas: $b.",
                "$a + $b = ${a + b}"
            ), EX_QUE_SUMA)
    }

    private val EX_COMPARA = WorkedExample(
        "Ejemplo: ¿hay más?",
        listOf(
            "¿Hay más 🍎 que 🍌?",
            "🍎 🍎 🍎",
            "🍌 🍌",
            "Cuenta cada fila: 3 manzanas y 2 plátanos.",
            "3 es más que 2 → la respuesta es «sí»."
        )
    )

    /** Comparación sí/no: ¿hay más/menos X que Y? (dos filas de dibujos). */
    private fun moreOrLess(): MathQuestion {
        val e1 = countEmojis.random()
        var e2 = countEmojis.random()
        while (e2 == e1) e2 = countEmojis.random()
        val n1 = Random.nextInt(1, 6); var n2 = Random.nextInt(1, 6)
        while (n2 == n1) n2 = Random.nextInt(1, 6)
        val askMore = Random.nextBoolean()
        val word = if (askMore) "más" else "menos"
        val answerYes = if (askMore) n1 > n2 else n1 < n2
        val row1 = List(n1) { e1 }.joinToString(" ")
        val row2 = List(n2) { e2 }.joinToString(" ")
        return MathQuestion(
            "¿Hay $word $e1 que $e2?\n\n$row1\n$row2",
            listOf("sí", "no"), if (answerYes) "sí" else "no",
            listOf(
                "Cuenta cada fila: $n1 $e1 y $n2 $e2.",
                "$n1 ${if (n1 > n2) "es más que" else "es menos que"} $n2 → «${if (answerYes) "sí" else "no"}»."
            ), EX_COMPARA)
    }

    /** Secuencia numérica con hueco: 3, 4, _, 6. */
    private fun numberSequence(): MathQuestion {
        val start = Random.nextInt(1, 7)
        val blankAt = Random.nextInt(1, 3) // posición 1 o 2 de 4
        val nums = (start until start + 4).toList()
        val shown = nums.mapIndexed { i, n -> if (i == blankAt) "_" else n.toString() }
        val ans = nums[blankAt]
        return MathQuestion(
            "¿Qué número falta?\n\n${shown.joinToString(",  ")}",
            distinctOptions(ans, listOf(ans + 1, ans - 1, ans + 2)), ans.toString(),
            listOf(
                "Cuenta en orden: ${nums.joinToString(", ")}.",
                "El número que falta es $ans."
            ),
            WorkedExample("Ejemplo: el número que falta",
                listOf("1, 2, _, 4", "Cuenta: 1, 2, 3, 4...", "¡Falta el 3!"))
        )
    }

    /** Patrón AB: 🔴 🔵 🔴 🔵 🔴 _ ¿qué sigue? */
    private fun patternNext(): MathQuestion {
        val pool = listOf("🔴", "🔵", "🟡", "🟢", "⭐", "🌸", "⚽", "🍎").shuffled()
        val a = pool[0]; val b = pool[1]
        val seq = listOf(a, b, a, b, a)
        val ans = b
        val options = (listOf(a, b) + pool.drop(2).take(2)).shuffled()
        return MathQuestion(
            "¿Qué sigue en el patrón?\n\n${seq.joinToString("  ")}  _",
            options, ans,
            listOf(
                "El patrón se repite: $a $b $a $b...",
                "Después de $a sigue $b."
            ),
            WorkedExample("Ejemplo: patrones",
                listOf("🔴 🔵 🔴 🔵 🔴 _", "Se repite rojo, azul, rojo, azul...", "Después del 🔴 sigue el 🔵."))
        )
    }

    private fun biggerNumber(): MathQuestion {
        val nums = (1..9).shuffled().take(4)
        val ans = nums.max()
        return MathQuestion(
            "¿Cuál número es el MÁS GRANDE?",
            nums.map { it.toString() }.shuffled(), ans.toString(),
            listOf("De estos números, el más grande es $ans."),
            WorkedExample("Ejemplo: el más grande",
                listOf("Entre 2, 5 y 3...", "el más grande es 5,", "porque 5 tiene más que 2 y que 3."))
        )
    }

    // --- Operaciones directas ---
    private fun opAddition(): MathQuestion {
        val a = Random.nextInt(3, 15); val b = Random.nextInt(2, 11); val ans = a + b
        return MathQuestion("¿Cuánto es $a + $b?",
            distinctOptions(ans, listOf(ans + 2, ans - 3, ans + 5)), ans.toString(),
            listOf("Sumamos las cantidades:", "$a + $b = $ans"), EX_ADD_DIRECT)
    }

    private fun opSubtraction(): MathQuestion {
        val a = Random.nextInt(8, 20); val b = Random.nextInt(2, a); val ans = a - b
        return MathQuestion("¿Cuánto es $a − $b?",
            distinctOptions(ans, listOf(ans + 2, ans + 1, ans + 4)), ans.toString(),
            listOf("Restamos:", "$a − $b = $ans"), EX_SUB_DIRECT)
    }

    private fun opMultiplication(): MathQuestion {
        val a = Random.nextInt(4, 12); val b = Random.nextInt(3, 10); val ans = a * b
        return MathQuestion("¿Cuánto es $a × $b?",
            distinctOptions(ans, listOf(ans + 4, ans - 6, ans + 10)), ans.toString(),
            listOf("Multiplicamos:", "$a × $b = $ans"), EX_MUL_DIRECT)
    }

    private fun opDivision(): MathQuestion {
        val b = Random.nextInt(2, 10); val ans = Random.nextInt(2, 10); val a = b * ans
        return MathQuestion("¿Cuánto es $a ÷ $b?",
            distinctOptions(ans, listOf(ans + 1, ans + 2, ans - 1)), ans.toString(),
            listOf("Dividimos:", "$a ÷ $b = $ans"), EX_DIV_DIRECT)
    }

    private fun opLinear(): MathQuestion {
        val x = Random.nextInt(3, 10); val coeff = Random.nextInt(2, 5); val c = Random.nextInt(1, 11)
        val right = coeff * x + c
        return MathQuestion("Resuelve para X:  ${coeff}X + $c = $right",
            distinctOptions(x, listOf(x + 2, x - 1, x + 3)), x.toString(),
            listOf(
                "${coeff}X + $c = $right",
                "1) El $c está sumando → pasa restando:",
                "     ${coeff}X = $right − $c = ${right - c}",
                "2) El $coeff está multiplicando → pasa dividiendo:",
                "     X = ${right - c} ÷ $coeff",
                "X = $x"
            ), EX_LINEAR)
    }

    // --- Situaciones (problemas de contexto) ---
    private fun wordAddition(): MathQuestion {
        val n = names.random(); val o = things.random()
        val a = Random.nextInt(3, 15); val b = Random.nextInt(2, 10); val ans = a + b
        return MathQuestion(
            "$n tenía $a $o y consiguió $b más. ¿Cuántas $o tiene ahora?",
            distinctOptions(ans, listOf(ans + 2, ans - 3, ans + 4)), ans.toString(),
            listOf("Hay que sumar lo que tenía y lo que consiguió:", "$a + $b = $ans"), EX_ADD)
    }

    private fun wordSubtraction(): MathQuestion {
        val n = names.random(); val o = things.random()
        val a = Random.nextInt(8, 20); val b = Random.nextInt(2, a); val ans = a - b
        return MathQuestion(
            "$n tenía $a $o y regaló $b. ¿Cuántas $o le quedan?",
            distinctOptions(ans, listOf(ans + 2, ans + 1, ans + 3)), ans.toString(),
            listOf("Hay que restar lo que regaló:", "$a − $b = $ans"), EX_SUB)
    }

    private fun wordMultiplication(): MathQuestion {
        val o = things.random(); val boxes = Random.nextInt(3, 9); val per = Random.nextInt(3, 9)
        val ans = boxes * per
        return MathQuestion(
            "Cada caja trae $per $o. Si hay $boxes cajas, ¿cuántas $o hay en total?",
            distinctOptions(ans, listOf(ans + boxes, ans - per, ans + 5)), ans.toString(),
            listOf("Multiplicamos cajas por lo de cada caja:", "$boxes × $per = $ans"), EX_MUL)
    }

    private fun wordDivision(): MathQuestion {
        val o = things.random(); val per = Random.nextInt(2, 9); val kids = Random.nextInt(2, 8)
        val total = per * kids; val ans = per
        return MathQuestion(
            "Se reparten $total $o en partes iguales entre $kids niños. ¿Cuántas le tocan a cada uno?",
            distinctOptions(ans, listOf(ans + 1, ans + 2, ans - 1)), ans.toString(),
            listOf("Dividimos el total entre los niños:", "$total ÷ $kids = $ans"), EX_DIV)
    }

    private fun wordLinear(): MathQuestion {
        val x = Random.nextInt(2, 11); val m = Random.nextInt(2, 5); val b = Random.nextInt(1, 10)
        val result = m * x + b
        return MathQuestion(
            "Pienso un número, lo multiplico por $m y le sumo $b; obtengo $result. ¿Qué número pensé?",
            distinctOptions(x, listOf(x + 1, x + 2, x - 1)), x.toString(),
            listOf(
                "Sea n el número:  ${m}·n + $b = $result",
                "1) El $b está sumando → pasa restando:",
                "     ${m}·n = $result − $b = ${result - b}",
                "2) El $m está multiplicando → pasa dividiendo:",
                "     n = ${result - b} ÷ $m",
                "n = $x"
            ), EX_WORD_NUMBER)
    }

    private fun wordPurchase(): MathQuestion {
        val n = names.random(); val count = Random.nextInt(2, 6)
        val unit = Random.nextInt(8, 20); val ship = Random.nextInt(5, 15)
        val total = count * unit + ship; val ans = unit
        return MathQuestion(
            "$n compró $count cuadernos y pagó \$$total en total, incluidos \$$ship de envío. ¿Cuánto costó cada cuaderno?",
            distinctOptions(ans, listOf(ans + 2, ans - 1, ans + 4)), ans.toString(),
            listOf(
                "Total = \$$total, envío = \$$ship, cantidad = $count",
                "1) Quita el envío:  $total − $ship = ${total - ship}",
                "2) Divide entre la cantidad:  ${total - ship} ÷ $count = $unit",
                "Cada cuaderno cuesta \$$unit"
            ), EX_PURCHASE)
    }

    private fun wordPercentage(): MathQuestion {
        val price = Random.nextInt(1, 11) * 20
        val disc = listOf(10, 20, 25, 50).random()
        val off = price * disc / 100; val ans = price - off
        return MathQuestion(
            "Un artículo de \$$price tiene $disc% de descuento. ¿Cuánto pagas al final?",
            distinctOptions(ans, listOf(ans + 5, ans - 5, price)), ans.toString(),
            listOf(
                "Precio = \$$price, descuento = $disc%",
                "1) Calcula el descuento:  $price × $disc ÷ 100 = \$$off",
                "2) Resta al precio:  $price − $off = \$$ans",
                "Pagas \$$ans"
            ), EX_PERCENT)
    }

    private fun distinctOptions(correct: Int, distractors: List<Int>): List<String> {
        val set = LinkedHashSet<Int>()
        set.add(correct)
        distractors.forEach { if (it != correct && it >= 0) set.add(it) }
        var filler = correct + 1
        while (set.size < 4) { if (filler != correct) set.add(filler); filler++ }
        return set.toList().shuffled().map { it.toString() }
    }

    // ---------------- INGLÉS (currículo rotativo estilo Duolingo) ----------------
    // Cada día toca UNA unidad distinta (rotación determinística por fecha):
    // pasado, futuro, irregulares, presente, y días temáticos de vocabulario.
    data class EnglishUnit(val title: String, val bank: List<EnglishExercise>)

    private val englishUnits = listOf(
        EnglishUnit("Viaje al pasado (Past Simple)", listOf(
            EnglishExercise(
                "Elige la forma correcta del verbo 'run' en pasado.",
                "Yesterday, Liam ______ to school because he was late.",
                listOf("runned", "ran", "runs", "running"), "ran",
                "El pasado de 'run' (correr) es irregular: 'ran'.",
                "ran = /ræn/ («ran»)"
            ),
            EnglishExercise(
                "Elige la forma correcta en pasado del verbo 'go'.",
                "Last weekend, we ______ to the beach with our dog.",
                listOf("goed", "goes", "went", "going"), "went",
                "El pasado de 'go' (ir) es irregular: 'went'.",
                "went = /wɛnt/ («uént»)"
            ),
            EnglishExercise(
                "Identifica el verbo en pasado simple de la oración.",
                "Which word is in the past tense? 'She watched a movie.'",
                listOf("She", "watched", "movie", "a"), "watched",
                "'watched' es el pasado regular de 'watch' (mirar).",
                "watched = /wɒtʃt/ («uótcht»)"
            ),
            EnglishExercise(
                "Traduce usando el tiempo pasado.",
                "Nosotros comimos manzanas ayer.",
                listOf(
                    "We ate apples yesterday", "We eat apples yesterday",
                    "We eaten apples yesterday", "We eated apples yesterday"
                ), "We ate apples yesterday",
                "'eat' es irregular: su pasado es 'ate'. 'ayer' = 'yesterday'.",
                "ate = /eɪt/ («éit»)"
            ),
            EnglishExercise(
                "Completa con el pasado de 'see'.",
                "I ______ a beautiful bird this morning.",
                listOf("seed", "saw", "seen", "sees"), "saw",
                "El pasado de 'see' (ver) es irregular: 'saw'.",
                "saw = /sɔː/ («so»)"
            )
        )),

        EnglishUnit("El futuro (will / going to)", listOf(
            EnglishExercise(
                "Completa la frase sobre el futuro.",
                "Tomorrow, I ______ visit my grandma.",
                listOf("will", "did", "was", "am"), "will",
                "'will' + verbo expresa futuro: 'I will visit' = visitaré.",
                "will = /wɪl/ («uíl»)"
            ),
            EnglishExercise(
                "Completa con 'going to'.",
                "She is ______ to study tonight.",
                listOf("going", "goes", "went", "go"), "going",
                "'be going to' = plan futuro: 'is going to study' = va a estudiar.",
                "going = /ˈgoʊɪŋ/ («góuing»)"
            ),
            EnglishExercise(
                "Traduce al inglés (futuro).",
                "Yo comeré pizza mañana.",
                listOf(
                    "I will eat pizza tomorrow", "I ate pizza tomorrow",
                    "I eat pizza yesterday", "I eating pizza tomorrow"
                ), "I will eat pizza tomorrow",
                "Futuro con 'will' + verbo base: will eat. 'mañana' = 'tomorrow'.",
                "tomorrow = /təˈmɒroʊ/ («tumórrou»)"
            ),
            EnglishExercise(
                "Completa la frase sobre el futuro.",
                "They ______ play soccer next Saturday.",
                listOf("will", "played", "was", "were"), "will",
                "'next Saturday' (el próximo sábado) pide futuro: 'will play'.",
                "will play = /wɪl pleɪ/ («uíl pléi»)"
            ),
            EnglishExercise(
                "¿Cuál frase habla del FUTURO?",
                "Choose the sentence about the future.",
                listOf(
                    "We will travel next year", "We traveled last year",
                    "We travel every year", "We were traveling"
                ), "We will travel next year",
                "'will travel' + 'next year' = viajaremos el próximo año.",
                "travel = /ˈtrævəl/ («trável»)"
            )
        )),

        EnglishUnit("Verbos irregulares", listOf(
            EnglishExercise(
                "¿Cuál es el pasado de 'buy' (comprar)?",
                "Yesterday, mom ______ tortillas at the market.",
                listOf("buyed", "bought", "buys", "buying"), "bought",
                "'buy' es irregular: su pasado es 'bought'.",
                "bought = /bɔːt/ («bot»)"
            ),
            EnglishExercise(
                "¿Cuál es el pasado de 'make' (hacer)?",
                "He ______ a beautiful drawing in class.",
                listOf("maked", "made", "makes", "making"), "made",
                "'make' es irregular: su pasado es 'made'.",
                "made = /meɪd/ («méid»)"
            ),
            EnglishExercise(
                "¿Cuál es el pasado de 'have' (tener)?",
                "We ______ a great time at the party.",
                listOf("haved", "had", "has", "having"), "had",
                "'have' es irregular: su pasado es 'had'.",
                "had = /hæd/ («jad»)"
            ),
            EnglishExercise(
                "¿Cuál es el pasado de 'take' (tomar/llevar)?",
                "She ______ the bus to school this morning.",
                listOf("taked", "took", "takes", "taking"), "took",
                "'take' es irregular: su pasado es 'took'.",
                "took = /tʊk/ («tuk»)"
            ),
            EnglishExercise(
                "¿Cuál es el pasado de 'come' (venir)?",
                "My cousins ______ to visit us last month.",
                listOf("comed", "came", "comes", "coming"), "came",
                "'come' es irregular: su pasado es 'came'.",
                "came = /keɪm/ («kéim»)"
            )
        )),

        EnglishUnit("El presente (Present Simple)", listOf(
            EnglishExercise(
                "Completa en presente (tercera persona).",
                "She ______ to school every day.",
                listOf("goes", "go", "went", "going"), "goes",
                "Con he/she/it el verbo lleva -s/-es: 'she goes'.",
                "goes = /goʊz/ («góus»)"
            ),
            EnglishExercise(
                "Completa en presente.",
                "I ______ breakfast at seven o'clock.",
                listOf("have", "has", "had", "having"), "have",
                "Con 'I' se usa 'have' (has es solo para he/she/it).",
                "have = /hæv/ («jav»)"
            ),
            EnglishExercise(
                "Completa la pregunta.",
                "______ you like apples?",
                listOf("Do", "Does", "Did", "Is"), "Do",
                "Preguntas en presente con you/we/they usan 'Do'.",
                "do = /duː/ («du»)"
            ),
            EnglishExercise(
                "Traduce al inglés (presente).",
                "Nosotros vivimos en México.",
                listOf(
                    "We live in Mexico", "We lived in Mexico",
                    "We living in Mexico", "We lives in Mexico"
                ), "We live in Mexico",
                "Presente simple: 'we live'. Sin -s porque no es he/she/it.",
                "live = /lɪv/ («liv»)"
            ),
            EnglishExercise(
                "Completa en presente (tercera persona).",
                "My brother ______ soccer on Sundays.",
                listOf("plays", "play", "played", "playing"), "plays",
                "Con 'my brother' (él) el verbo lleva -s: 'plays'.",
                "plays = /pleɪz/ («pléis»)"
            )
        )),

        EnglishUnit("Un día de escuela (vocabulario)", listOf(
            EnglishExercise(
                "¿Cómo se dice 'mochila' en inglés?",
                "I carry my books in my ______.",
                listOf("backpack", "pencil", "desk", "lunch"), "backpack",
                "'backpack' = mochila.",
                "backpack = /ˈbækpæk/ («bákpak»)"
            ),
            EnglishExercise(
                "¿Quién enseña la clase?",
                "The ______ teaches the class.",
                listOf("teacher", "student", "doctor", "driver"), "teacher",
                "'teacher' = maestro/maestra.",
                "teacher = /ˈtiːtʃər/ («tícher»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'cuaderno' en inglés?",
                "I write my homework in my ______.",
                listOf("notebook", "window", "chair", "door"), "notebook",
                "'notebook' = cuaderno.",
                "notebook = /ˈnoʊtbʊk/ («nóutbuk»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'recreo' en inglés?",
                "We play with our friends at ______.",
                listOf("recess", "homework", "test", "class"), "recess",
                "'recess' = recreo.",
                "recess = /ˈriːsɛs/ («ríses»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'tarea' en inglés?",
                "I do my ______ after school.",
                listOf("homework", "breakfast", "shower", "game"), "homework",
                "'homework' = tarea.",
                "homework = /ˈhoʊmwɜːrk/ («jóumuerk»)"
            )
        )),

        EnglishUnit("De viaje (vocabulario)", listOf(
            EnglishExercise(
                "¿En qué volamos a otro país?",
                "We fly in an ______.",
                listOf("airplane", "car", "bicycle", "boat"), "airplane",
                "'airplane' = avión.",
                "airplane = /ˈɛərpleɪn/ («érplein»)"
            ),
            EnglishExercise(
                "¿Dónde dormimos en un viaje?",
                "We sleep in a ______.",
                listOf("hotel", "school", "kitchen", "garden"), "hotel",
                "'hotel' = hotel (¡se escribe igual!).",
                "hotel = /hoʊˈtɛl/ («joutél»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'playa' en inglés?",
                "We swim at the ______.",
                listOf("beach", "mountain", "city", "store"), "beach",
                "'beach' = playa.",
                "beach = /biːtʃ/ («bich»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'maleta' en inglés?",
                "I pack my clothes in my ______.",
                listOf("suitcase", "wallet", "hat", "shoe"), "suitcase",
                "'suitcase' = maleta.",
                "suitcase = /ˈsuːtkeɪs/ («sútkeis»)"
            ),
            EnglishExercise(
                "¿Qué necesitas para subir al avión?",
                "Show your ______ to get on the plane.",
                listOf("ticket", "toy", "sandwich", "pillow"), "ticket",
                "'ticket' = boleto.",
                "ticket = /ˈtɪkɪt/ («tíket»)"
            )
        )),

        EnglishUnit("La familia (vocabulario)", listOf(
            EnglishExercise(
                "¿Cómo se dice 'hermano' en inglés?",
                "My ______ plays video games with me.",
                listOf("brother", "sister", "father", "uncle"), "brother",
                "'brother' = hermano.",
                "brother = /ˈbrʌðər/ («bróder»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'abuela' en inglés?",
                "My ______ makes delicious cookies.",
                listOf("grandmother", "teacher", "cousin", "aunt"), "grandmother",
                "'grandmother' = abuela.",
                "grandmother = /ˈgrænmʌðər/ («gránmader»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'primo' en inglés?",
                "My ______ lives in another city.",
                listOf("cousin", "brother", "nephew", "son"), "cousin",
                "'cousin' = primo o prima.",
                "cousin = /ˈkʌzən/ («kásen»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'padres' en inglés?",
                "My ______ take care of me.",
                listOf("parents", "friends", "teachers", "neighbors"), "parents",
                "'parents' = padres (papá y mamá).",
                "parents = /ˈpɛərənts/ («pérents»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'tía' en inglés?",
                "My ______ is my mom's sister.",
                listOf("aunt", "uncle", "grandma", "sister"), "aunt",
                "'aunt' = tía.",
                "aunt = /ænt/ («ant»)"
            )
        )),

        EnglishUnit("La comida (vocabulario)", listOf(
            EnglishExercise(
                "¿Cómo se dice 'desayuno' en inglés?",
                "I eat ______ in the morning.",
                listOf("breakfast", "dinner", "lunch", "snack"), "breakfast",
                "'breakfast' = desayuno.",
                "breakfast = /ˈbrɛkfəst/ («brékfast»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'pollo' en inglés?",
                "We had rice and ______ for lunch.",
                listOf("chicken", "beef", "fish", "cheese"), "chicken",
                "'chicken' = pollo.",
                "chicken = /ˈtʃɪkɪn/ («chíken»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'arroz' en inglés?",
                "My favorite food is ______ with beans.",
                listOf("rice", "bread", "soup", "salad"), "rice",
                "'rice' = arroz.",
                "rice = /raɪs/ («ráis»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'jugo' en inglés?",
                "I drink orange ______ every morning.",
                listOf("juice", "milk", "water", "soda"), "juice",
                "'juice' = jugo.",
                "juice = /dʒuːs/ («llus»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'verduras' en inglés?",
                "Eat your ______ to grow strong!",
                listOf("vegetables", "candies", "cookies", "chips"), "vegetables",
                "'vegetables' = verduras.",
                "vegetables = /ˈvɛdʒtəbəlz/ («véchtabols»)"
            )
        ))
    )

    /** Índice de la unidad del día (rotación determinística por fecha). */
    private fun todaysUnitIndex(): Int {
        val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        return dayOfYear % englishUnits.size
    }

    /** Título de la lección de inglés de HOY (para mostrar en la UI). */
    fun todaysEnglishUnitTitle(): String = englishUnits[todaysUnitIndex()].title

    // Vocabulario con dibujos para Preescolar/1º: (emoji, palabra, fonética, español)
    private data class VocabItem(val emoji: String, val word: String, val phon: String, val es: String)
    private val starterVocab = listOf(
        VocabItem("🐶", "dog", "/dɒg/ («dog»)", "perro"),
        VocabItem("🐱", "cat", "/kæt/ («cat»)", "gato"),
        VocabItem("🍎", "apple", "/ˈæpəl/ («ápol»)", "manzana"),
        VocabItem("☀️", "sun", "/sʌn/ («san»)", "sol"),
        VocabItem("🏠", "house", "/haʊs/ («jaus»)", "casa"),
        VocabItem("🐟", "fish", "/fɪʃ/ («fish»)", "pez"),
        VocabItem("🐦", "bird", "/bɜːrd/ («berd»)", "pájaro"),
        VocabItem("🥛", "milk", "/mɪlk/ («milk»)", "leche"),
        VocabItem("⚽", "ball", "/bɔːl/ («bol»)", "pelota"),
        VocabItem("🌙", "moon", "/muːn/ («mun»)", "luna"),
        VocabItem("💧", "water", "/ˈwɔːtər/ («uóter»)", "agua"),
        VocabItem("📖", "book", "/bʊk/ («buk»)", "libro"),
        // Partes de la cara (hoja "Listen, repeat and trace")
        VocabItem("😊", "face", "/feɪs/ («féis»)", "cara"),
        VocabItem("👁️", "eye", "/aɪ/ («ái»)", "ojo"),
        VocabItem("👃", "nose", "/noʊz/ («nóus»)", "nariz"),
        VocabItem("👄", "mouth", "/maʊθ/ («máuz»)", "boca"),
        VocabItem("👂", "ear", "/ɪr/ («íer»)", "oreja"),
        VocabItem("✋", "hand", "/hænd/ («jand»)", "mano")
    )

    /** Guía de ayuda de inglés para los más pequeños. */
    val starterEnglishHelp = WorkedExample(
        "Palabras en inglés",
        listOf(
            "Mira el dibujo y di su nombre en inglés:",
            "🐶 dog («dog»)   🐱 cat («cat»)",
            "🍎 apple («ápol»)   ☀️ sun («san»)",
            "Repite la palabra en voz alta 3 veces. ¡Así se aprende!"
        )
    )

    /** Ejercicio de vocabulario con dibujo: emoji→palabra o palabra→emoji. */
    private fun starterEnglish(): EnglishExercise {
        val item = starterVocab.random()
        val others = starterVocab.filter { it != item }.shuffled().take(3)
        return if (Random.nextBoolean()) {
            EnglishExercise(
                "Mira el dibujo. ¿Cómo se dice en inglés?",
                item.emoji,
                (others.map { it.word } + item.word).shuffled(),
                item.word,
                "${item.emoji} es '${item.word}' en inglés (${item.es}). Dilo en voz alta: ${item.word}!",
                "${item.word} = ${item.phon}"
            )
        } else {
            EnglishExercise(
                "¿Cuál dibujo es '${item.word}'?",
                item.word,
                (others.map { it.emoji } + item.emoji).shuffled(),
                item.emoji,
                "'${item.word}' significa ${item.es}: ${item.emoji}. Repite: ${item.word}!",
                "${item.word} = ${item.phon}"
            )
        }
    }

    fun randomEnglish(starter: Boolean = false, exclude: String? = null): EnglishExercise {
        if (starter) {
            var ex = starterEnglish()
            var tries = 0
            while (exclude != null && ex.question == exclude && tries < 6) {
                ex = starterEnglish(); tries++
            }
            return ex
        }
        // Primaria/Secundaria: se practica la UNIDAD DEL DÍA (rotación por fecha).
        val bank = englishUnits[todaysUnitIndex()].bank
        val pool = bank.filter { it.question != exclude }.ifEmpty { bank }
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

    // Lecturas más amplias para secundaria (comprensión de textos largos).
    private val readingAdvancedBank = listOf(
        ReadingPassage(
            "La fotosíntesis",
            "La fotosíntesis es el proceso por el cual las plantas, las algas y algunas bacterias transforman la energía de la luz solar en energía química. Usando el dióxido de carbono del aire y el agua del suelo, producen glucosa, que les sirve de alimento, y liberan oxígeno como subproducto. Este oxígeno es esencial para la respiración de casi todos los seres vivos. Además, la glucosa producida es la base de las cadenas alimenticias: los animales que comen plantas obtienen de ellas la energía que originalmente vino del Sol. Por eso se dice que la fotosíntesis sostiene la vida en la Tierra."
        ),
        ReadingPassage(
            "La Revolución Industrial",
            "La Revolución Industrial comenzó en Inglaterra a finales del siglo XVIII y transformó profundamente la sociedad. La invención de la máquina de vapor permitió mecanizar la producción, que antes se hacía a mano en talleres pequeños. Surgieron grandes fábricas y las ciudades crecieron rápidamente cuando muchas personas dejaron el campo para trabajar en ellas. Aunque aumentó la producción de bienes y aparecieron nuevos inventos, también trajo problemas como largas jornadas laborales, trabajo infantil y contaminación. Estos cambios sentaron las bases del mundo industrial y tecnológico en el que vivimos hoy."
        ),
        ReadingPassage(
            "El ciclo del agua",
            "El ciclo del agua describe el movimiento continuo del agua en la Tierra. El calor del Sol evapora el agua de los océanos, ríos y lagos, convirtiéndola en vapor que sube a la atmósfera. Allí el vapor se enfría y se condensa formando las nubes. Cuando las gotas se vuelven demasiado pesadas, caen como lluvia, nieve o granizo en un proceso llamado precipitación. Parte de esa agua regresa a los ríos y mares, y otra parte se filtra en el suelo formando aguas subterráneas. Así, el mismo agua se recicla una y otra vez desde hace millones de años."
        )
    )

    fun randomReading(advanced: Boolean = false): ReadingPassage =
        if (advanced) readingAdvancedBank.random() else readingBank.random()

    // --- Mini-lecturas para Preescolar/1º: una oración + pregunta de opción ---
    data class ReadingQuiz(
        val sentence: String,
        val question: String,
        val options: List<String>,
        val answer: String
    )

    private val readingQuizBank = listOf(
        ReadingQuiz("El gato bebe leche.", "¿Qué bebe el gato?",
            listOf("leche", "agua", "jugo", "pan"), "leche"),
        ReadingQuiz("El sol es amarillo.", "¿De qué color es el sol?",
            listOf("amarillo", "azul", "verde", "rojo"), "amarillo"),
        ReadingQuiz("Ana tiene un globo rojo.", "¿Qué tiene Ana?",
            listOf("un globo", "un perro", "una pelota", "un pan"), "un globo"),
        ReadingQuiz("El perro corre en el parque.", "¿Dónde corre el perro?",
            listOf("en el parque", "en la casa", "en la escuela", "en el mar"), "en el parque"),
        ReadingQuiz("Mamá compra pan.", "¿Qué compra mamá?",
            listOf("pan", "leche", "fruta", "queso"), "pan"),
        ReadingQuiz("El pez nada en el agua.", "¿Dónde nada el pez?",
            listOf("en el agua", "en la arena", "en el cielo", "en la mesa"), "en el agua"),
        ReadingQuiz("Luis juega con la pelota.", "¿Con qué juega Luis?",
            listOf("la pelota", "el carro", "la muñeca", "el libro"), "la pelota"),
        ReadingQuiz("La luna sale de noche.", "¿Cuándo sale la luna?",
            listOf("de noche", "de día", "en la tarde", "en verano"), "de noche"),

        // --- Completar la vocal que falta (conciencia fonológica) ---
        ReadingQuiz("🐢  T_RTUGA", "¿Qué vocal falta?",
            listOf("O", "A", "E", "U"), "O"),
        ReadingQuiz("🐰  CON_JO", "¿Qué vocal falta?",
            listOf("E", "A", "O", "I"), "E"),
        ReadingQuiz("🐔  G_LLINA", "¿Qué vocal falta?",
            listOf("A", "E", "O", "U"), "A"),
        ReadingQuiz("🐱  GAT_", "¿Qué vocal falta?",
            listOf("O", "A", "E", "I"), "O"),
        ReadingQuiz("🍎  MANZAN_", "¿Qué vocal falta?",
            listOf("A", "O", "E", "U"), "A"),

        // --- Completar la sílaba que falta ---
        ReadingQuiz("🍎  man_na", "¿Qué sílaba falta?",
            listOf("za", "ta", "pa", "sa"), "za"),
        ReadingQuiz("🥄  cu_ra", "¿Qué sílaba falta?",
            listOf("cha", "ta", "ra", "ma"), "cha"),
        ReadingQuiz("☂️  pa_guas", "¿Qué sílaba falta?",
            listOf("ra", "va", "za", "ta"), "ra"),
        ReadingQuiz("🍌  plá_no", "¿Qué sílaba falta?",
            listOf("ta", "sa", "ma", "pa"), "ta"),
        ReadingQuiz("👻  fantas_", "¿Qué sílaba falta?",
            listOf("ma", "za", "ra", "cha"), "ma"),
        ReadingQuiz("👟  za_tillas", "¿Qué sílaba falta?",
            listOf("pa", "va", "ta", "sa"), "pa"),
        ReadingQuiz("🦋  maripo_", "¿Qué sílaba falta?",
            listOf("sa", "za", "ma", "ra"), "sa"),

        // --- Mini-lecturas con varias preguntas (el texto se repite por pregunta) ---
        ReadingQuiz("La jirafa tiene un cuello muy largo. Es amarilla con manchas de color café.",
            "¿Cómo tiene el cuello la jirafa?",
            listOf("largo", "corto", "gordo", "azul"), "largo"),
        ReadingQuiz("La jirafa tiene un cuello muy largo. Es amarilla con manchas de color café.",
            "¿De qué color son sus manchas?",
            listOf("café", "amarillas", "rojas", "verdes"), "café"),
        ReadingQuiz("El elefante es muy grande. Tiene una trompa larga y orejas grandes. Le gusta bañarse en el río.",
            "¿Qué tiene largo el elefante?",
            listOf("la trompa", "la cola", "las patas", "el pelo"), "la trompa"),
        ReadingQuiz("El elefante es muy grande. Tiene una trompa larga y orejas grandes. Le gusta bañarse en el río.",
            "¿Dónde le gusta bañarse?",
            listOf("en el río", "en el mar", "en la casa", "en la escuela"), "en el río")
    )

    val readingQuizHelp = WorkedExample(
        "Cómo leer la oración",
        listOf(
            "1) Lee despacio, palabra por palabra.",
            "2) Puedes leerla en voz alta.",
            "Ejemplo: «El gato bebe leche.»",
            "Pregunta: ¿Qué bebe el gato? → leche 🥛"
        )
    )

    fun randomReadingQuiz(exclude: String? = null): ReadingQuiz {
        // exclude llega como la PREGUNTA del quiz anterior (Quiz.question en la UI).
        val pool = readingQuizBank.filter { it.question != exclude }.ifEmpty { readingQuizBank }
        return pool.random().let { it.copy(options = it.options.shuffled()) }
    }

    // ---------------- EVALUACIÓN DE RESUMEN (heurística local) ----------------
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
                suggestions = "Relee el texto y menciona de qué tema principal se está hablando."
            )
        }
    }

    private fun normalize(s: String): String {
        val noAccents = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return noAccents.replace(Regex("[^a-z0-9ñ\\s]"), " ")
    }
}
