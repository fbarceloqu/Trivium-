import com.controlparental.kioscosuave.ChallengeEngine
import com.controlparental.kioscosuave.EnglishGuides
import kotlin.system.exitProcess

/**
 * Correspondencia tema – guía – ejercicio en inglés.
 * El defecto que motivó esta prueba: una guía de tiempos verbales aparecía
 * como ayuda de un ejercicio de comparativos y superlativos.
 */
private var fallas = 0
private fun check(cond: Boolean, msg: String) {
    if (!cond) { println("  FALLA: $msg"); fallas++ }
}

/** Palabras que una guía DEBE mencionar para ser del tema de su unidad. */
private val KEYWORDS = mapOf(
    "Viaje al pasado (Past Simple)" to listOf("-ed", "did", "yesterday"),
    "El futuro (will / going to)" to listOf("will", "going to", "tomorrow"),
    "Verbos irregulares" to listOf("irregular", "-ed"),
    "El presente (Present Simple)" to listOf("does", "-s", "every day"),
    "Un día de escuela (vocabulario)" to listOf("classroom", "pencil"),
    "De viaje (vocabulario)" to listOf("airport", "passport"),
    "La familia (vocabulario)" to listOf("nephew", "niece", "grand-"),
    "La comida (vocabulario)" to listOf("lunch", "drink", "eat"),
    "Comparativos y superlativos" to listOf("than", "-er", "-est", "most"),
    "Verbos modales" to listOf("can", "must", "should"),
    "Primer condicional" to listOf("if", "will", "presente"),
    "Verbos frasales" to listOf("partícula", "turn on", "up"),
    "Voz pasiva básica" to listOf("participio", "was", "by"),
    "Preguntas con Wh- y do/does/did" to listOf("where", "when", "does"),
    "Estilo indirecto (reported speech)" to listOf("said", "would", "was"),
    "Conectores (because, although, however, so that)" to listOf("because", "although", "however", "so that"),
    "Vocabulario intermedio (tecnología, medio ambiente, profesiones)" to listOf("-tion", "-er")
)

fun main() {
    val units = ChallengeEngine.englishUnits(advanced = true)
    val basic = ChallengeEngine.englishUnits(advanced = false)

    // =============================================================
    println("### 1. Cada unidad tiene su propia guía ###\n")
    check(basic.all { it in units } && basic.size < units.size, "primaria debe ser un subconjunto de secundaria")
    check(units.map { it.title }.distinct().size == units.size, "títulos de unidad repetidos")
    units.forEach { u ->
        val g = EnglishGuides.forUnit(u.title)
        check(g != null, "«${u.title}» no tiene guía propia")
        if (g == null) return@forEach
        check(g.title != ChallengeEngine.englishHelp.title, "«${u.title}» usa la guía general")
        check(g.lines.size in 6..11, "«${u.title}»: guía de ${g.lines.size} renglones (debe caber en el diálogo)")
        check(g.lines.all { it.length <= 70 }, "«${u.title}»: renglón demasiado largo: ${g.lines.filter { it.length > 70 }}")
        val text = (g.title + " " + g.lines.joinToString(" ")).lowercase()
        (KEYWORDS[u.title] ?: emptyList()).forEach { k ->
            check(k.lowercase() in text, "«${u.title}»: la guía no menciona «$k»")
        }
        check(KEYWORDS.containsKey(u.title), "«${u.title}»: falta definir sus palabras clave en la prueba")
    }
    check(EnglishGuides.unitTitles.all { t -> units.any { it.title == t } },
        "hay guías huérfanas (el título de la unidad cambió): ${EnglishGuides.unitTitles - units.map { it.title }.toSet()}")
    val guides = units.mapNotNull { EnglishGuides.forUnit(it.title) }
    check(guides.map { it.title }.distinct().size == guides.size, "dos unidades comparten título de guía")
    check(guides.map { it.lines }.distinct().size == guides.size, "dos unidades comparten el mismo contenido")
    println("  ${units.size} unidades · ${guides.size} guías propias")

    // =============================================================
    println("\n### 1b. Calidad de los bancos ###\n")
    units.forEach { u ->
        // La etapa pide 10 respuestas: con menos ejercicios se repiten y se
        // puede aprobar de memoria.
        check(u.bank.size >= 10, "«${u.title}» tiene solo ${u.bank.size} ejercicios")
        check(u.bank.map { it.question }.distinct().size == u.bank.size, "«${u.title}»: preguntas repetidas")
        u.bank.forEach { ex ->
            val o = ex.options
            check(o.size == 4 && o.distinct().size == 4, "«${ex.question.take(40)}»: no tiene 4 opciones distintas")
            check(o.count { it == ex.correctAnswer } == 1, "«${ex.question.take(40)}»: la respuesta no aparece una sola vez")
            check(o.map { it.lowercase() }.distinct().size == 4, "«${ex.question.take(40)}»: opciones que solo difieren en mayúsculas")
            check(ex.explanation.isNotBlank() && ex.phonetic.isNotBlank(), "«${ex.question.take(40)}»: sin explicación o pronunciación")
            // La respuesta no debe delatarse por ser mucho más larga que el resto.
            val others = o.filter { it != ex.correctAnswer }.map { it.length }
            check(ex.correctAnswer.length <= others.max() * 2 + 3, "«${ex.question.take(40)}»: la respuesta destaca por longitud")
            // Una oración con hueco tiene exactamente un hueco.
            if ("______" in ex.question) check(ex.question.split("______").size == 2, "«${ex.question.take(40)}»: más de un hueco")
        }
    }
    println("  ${units.sumOf { it.bank.size }} ejercicios en total · mínimo por unidad: ${units.minOf { it.bank.size }}")

    // =============================================================
    println("\n### 2. Cada ejercicio recibe la guía de SU tema ###\n")
    var exercises = 0
    units.forEach { u ->
        val expected = EnglishGuides.forUnit(u.title)
        u.bank.forEach { ex ->
            exercises++
            check(ChallengeEngine.englishHelpFor(ex) == expected,
                "«${ex.question.take(50)}» (${u.title}) recibe la guía «${ChallengeEngine.englishHelpFor(ex).title}»")
            // Las opciones se barajan al presentar: la guía no debe depender del orden.
            check(ChallengeEngine.englishHelpFor(ex.copy(options = ex.options.reversed())) == expected,
                "la guía cambia al barajar las opciones")
        }
    }
    // El caso reportado.
    val comparatives = units.first { it.title == "Comparativos y superlativos" }
    comparatives.bank.forEach { ex ->
        val g = ChallengeEngine.englishHelpFor(ex)
        val text = g.lines.joinToString(" ")
        check("comparativos" in g.title.lowercase(), "un ejercicio de comparativos recibe «${g.title}»")
        check("PASADO" !in text && "FUTURO" !in text, "la guía de comparativos habla de tiempos verbales")
    }
    // Lo que realmente sale en la sesión, tal como lo pide la pantalla.
    repeat(400) {
        val advanced = it % 2 == 0
        val ex = ChallengeEngine.randomEnglish(advanced = advanced)
        val today = ChallengeEngine.todaysEnglishUnitTitle(advanced)
        check(ChallengeEngine.englishHelpFor(ex) == EnglishGuides.forUnit(today),
            "la lección de hoy es «$today» pero la guía es «${ChallengeEngine.englishHelpFor(ex).title}»")
    }
    // Spelling (refuerzos): guía de deletreo, no de gramática.
    repeat(100) {
        val ex = ChallengeEngine.randomEnglish(spellingWords = listOf("sun", "sand", "sea", "summer"))
        check(ChallengeEngine.englishHelpFor(ex) == EnglishGuides.spelling, "un ejercicio de spelling recibe «${ChallengeEngine.englishHelpFor(ex).title}»")
    }
    val sp = EnglishGuides.spelling.lines.joinToString(" ")
    check("NOMBRE" in sp && "SONIDO" in sp, "la guía de deletreo debe distinguir nombre y sonido de la letra")
    println("  $exercises ejercicios comprobados")

    // =============================================================
    println("\n### 3. La guía ayuda, pero no entrega la respuesta ###\n")
    units.forEach { u ->
        val g = EnglishGuides.forUnit(u.title) ?: return@forEach
        val text = g.lines.joinToString(" ").lowercase()
        u.bank.forEach { ex ->
            if (ex.question.contains("______")) {
                val solved = ex.question.replace("______", ex.correctAnswer).lowercase().trim('.', '?', '!', ' ')
                check(solved !in text, "«${u.title}»: la guía contiene la oración resuelta «$solved»")
            }
            // Una traducción completa tampoco debe aparecer tal cual.
            if (ex.correctAnswer.split(" ").size >= 4)
                check(ex.correctAnswer.lowercase() !in text, "«${u.title}»: la guía contiene la respuesta «${ex.correctAnswer}»")
        }
        // En vocabulario la respuesta ES la palabra: la guía no debe listarla.
        if ("vocabulario" in u.title.lowercase()) u.bank.forEach { ex ->
            val word = ex.correctAnswer.lowercase()
            check(!Regex("\\b${Regex.escape(word)}\\b").containsMatchIn(text),
                "«${u.title}»: la guía incluye la palabra que se pregunta («$word»)")
        }
    }

    println(if (fallas == 0) "\nPASS: guías de inglés (una por unidad, emparejadas con su ejercicio, sin revelar respuestas)."
            else "\n$fallas FALLAS")
    if (fallas != 0) exitProcess(1)
}
