import com.controlparental.kioscosuave.curriculum.*
import kotlin.random.Random
import kotlin.system.exitProcess

/**
 * Lecturas largas de secundaria: contenido, calificación determinista,
 * selección de lecturas y registro en la memoria.
 * Termina con código de error si algo falla.
 */
private var fallas = 0
private fun check(cond: Boolean, msg: String) {
    if (!cond) { println("  FALLA: $msg"); fallas++ }
}

fun main() {
    val bank = LongReadingBank.readings

    // =============================================================
    println("### 1. Contenido del banco ###\n")
    bank.forEach { r ->
        val errs = LongReadingValidator.errors(r)
        println("  ${r.id}: ${r.wordCount} palabras, ${r.paragraphs.size} párrafos, ${r.questions.size} preguntas" +
            if (errs.isEmpty()) "" else "  <-- ${errs.size} problemas")
        errs.forEach { check(false, "${r.id}: $it") }
    }
    check(bank.map { it.id }.distinct().size == bank.size, "ids de lectura repetidos")
    check(bank.map { it.topic }.toSet() == ReadingTopic.entries.toSet(), "no están cubiertos todos los temas")
    check(ComprehensionSkill.entries.map { it.id }.distinct().size == ComprehensionSkill.entries.size, "ids de habilidad repetidos")
    // La primera opción es la clave en el banco: en pantalla NO debe quedarse ahí.
    bank.forEach { r -> r.questions.forEach { q -> check(q.options.first() == q.answer, "${r.id}: convención del banco rota") } }

    // =============================================================
    println("\n### 2. El validador rechaza lo que debe ###\n")
    run {
        val ok = bank.first()
        fun bad(name: String, r: LongReading, expect: String) {
            val e = LongReadingValidator.errors(r)
            check(e.any { expect in it }, "no detectó: $name (dio $e)")
        }
        fun withQ(i: Int, f: (LongQuestion) -> LongQuestion) =
            ok.copy(questions = ok.questions.toMutableList().also { it[i] = f(it[i]) })
        bad("texto corto", ok.copy(paragraphs = ok.paragraphs.take(3)), "Extensión")
        bad("texto largo", ok.copy(paragraphs = ok.paragraphs + ok.paragraphs), "Extensión")
        bad("pocas preguntas", ok.copy(questions = ok.questions.take(5)), "preguntas")
        bad("tipo ausente", ok.copy(questions = ok.questions.filter { it.skill != ComprehensionSkill.CRITICAL } + ok.questions.first()), "Falta una pregunta")
        bad("dos claves", withQ(0) { it.copy(options = listOf(it.answer, it.answer.uppercase(), it.options[2], it.options[3])) }, "repetidas")
        bad("equivalentes con acento", withQ(0) { it.copy(options = it.options.take(3) + (it.options[1].replace("a", "á"))) }, "repetidas")
        bad("clave ausente", withQ(0) { it.copy(answer = "otra cosa") }, "clave")
        bad("tres opciones", withQ(0) { it.copy(options = it.options.take(3)) }, "opciones")
        bad("evidencia inventada", withQ(0) { it.copy(evidence = "esto no aparece en la lectura") }, "evidencia")
        bad("comodín", withQ(0) { it.copy(options = it.options.take(3) + "Todas las anteriores") }, "comodín")
        bad("clave delatada por larga", withQ(1) { val a = it.answer + " y además lo hace de una manera muy completa"; it.copy(answer = a, options = listOf(a) + it.options.drop(1)) }, "más larga")
        bad("clave copiada del texto", withQ(1) { val a = "Dormir, entonces, no interrumpe el estudio; lo completa."; it.copy(answer = a, options = listOf(a) + it.options.drop(1)) }, "copia literal")
        bad("vocabulario sin palabra", withQ(4) { it.copy(question = "¿Qué significa la palabra?") }, "vocabulario")
    }

    // =============================================================
    println("### 3. Calificación determinista y umbral de 80 % ###\n")
    bank.forEach { r ->
        val n = r.questions.size
        val all = r.questions.map { 0 }                 // la clave es la opción 0 del banco
        check(LongReadingQuiz.grade(r, all).let { it.correct == n && it.score == 100 && it.passed }, "${r.id}: todo correcto no da 100")
        check(LongReadingQuiz.grade(r, r.questions.map { 1 }).let { it.correct == 0 && !it.passed }, "${r.id}: todo mal no da 0")
        check(LongReadingQuiz.grade(r, List(n) { null }).correct == 0, "${r.id}: sin responder cuenta aciertos")
        check(LongReadingQuiz.grade(r, all) == LongReadingQuiz.grade(r, all), "${r.id}: la calificación no es repetible")
        // Umbral aprobado por el padre: 5/6, 6/7 o 7/8.
        val need = when (n) { 6 -> 5; 7 -> 6; else -> 7 }
        fun withWrong(k: Int) = List(n) { if (it < k) 1 else 0 }
        check(LongReadingQuiz.grade(r, withWrong(n - need)).passed, "${r.id}: $need/$n debería aprobar")
        check(!LongReadingQuiz.grade(r, withWrong(n - need + 1)).passed, "${r.id}: ${need - 1}/$n no debería aprobar")
        val res = LongReadingQuiz.grade(r, withWrong(2))
        check(res.weakSkills(r) == r.questions.take(2).map { it.skill }.distinct(), "${r.id}: no identifica las habilidades falladas")
    }

    // =============================================================
    println("### 4. Orden en pantalla ###\n")
    run {
        val pos = IntArray(4)
        bank.forEach { r ->
            check(LongReadingQuiz.order(r, 77L) == LongReadingQuiz.order(r, 77L), "${r.id}: la misma semilla da otro orden (no se podría retomar)")
            (1L..60L).forEach { seed ->
                val order = LongReadingQuiz.order(r, seed * 7919)
                check(order.size == r.questions.size && order.all { it.sorted() == listOf(0, 1, 2, 3) }, "${r.id}: orden inválido")
                order.forEach { pos[it.indexOf(0)]++ }
            }
        }
        val total = pos.sum()
        println("  posición de la clave en pantalla: ${pos.map { "%.0f%%".format(it * 100.0 / total) }}")
        pos.forEach { check(it > total / 5, "la clave cae poco en alguna posición: ${pos.toList()}") }
    }

    // =============================================================
    println("\n### 5. Selección: sin repetir, tema variado, extensión según desempeño ###\n")
    run {
        val rnd = Random(2026)
        val history = mutableListOf<String>()
        var last: Int? = null
        repeat(60) {
            val r = LongReadingQuiz.pick(bank, history, last, rnd)
            check(r.id !in history.takeLast(bank.size - 1), "repitió ${r.id} antes de agotar el banco")
            history.lastOrNull()?.let { prev -> check(LongReadingBank.byId(prev)!!.topic != r.topic, "dos lecturas seguidas del mismo tema") }
            history += r.id
            last = listOf(60, 85, 100, 71).random(rnd)
        }
        check(history.take(bank.size).toSet().size == bank.size, "no recorrió todo el banco antes de repetir")
        val words = bank.map { it.wordCount }.sorted()
        val median = words[words.size / 2]
        val easy = (1..200).map { LongReadingQuiz.pick(bank, emptyList(), 50, rnd).wordCount }
        val hard = (1..200).map { LongReadingQuiz.pick(bank, emptyList(), 100, rnd).wordCount }
        check(easy.average() < hard.average(), "tras un mal resultado no ofrece lecturas más cortas")
        check(easy.all { it <= median } && hard.all { it >= words[words.size / 2 - 1] }, "la extensión no sigue al desempeño")
        check(LongReadingQuiz.pick(bank.take(1), listOf(bank.first().id), 100, rnd) == bank.first(), "con una sola lectura debe seguir funcionando")
    }

    // =============================================================
    println("### 6. Memoria: habilidades que requieren refuerzo ###\n")
    run {
        val r = bank.first()
        val math = SkillState("sec1.est.moda", practices = 9, correct = 7, streak = 2, intervalDays = 3f, dueAt = 123L)
        // Falla solo las preguntas de inferencia.
        val answers = r.questions.map { if (it.skill == ComprehensionSkill.INFERENCE) 2 else 0 }
        val after = LongReadingQuiz.remember(mapOf(math.skillId to math), r, answers, now = 1_000L)
        check(after[math.skillId] == math, "se alteró la memoria de matemáticas")
        val inf = after.getValue(ComprehensionSkill.INFERENCE.id)
        check(inf.correct == 0 && inf.practices == r.questions.count { it.skill == ComprehensionSkill.INFERENCE }, "inferencia mal contabilizada")
        check(inf.recentErrors.isNotEmpty() && inf.dueAt == 1_000L, "lo fallado debe quedar vencido y con su error anotado")
        ComprehensionSkill.entries.filter { it != ComprehensionSkill.INFERENCE }.forEach { s ->
            val st = after.getValue(s.id)
            check(st.correct == st.practices && st.dueAt > 1_000L, "${s.id}: un acierto debe espaciarse")
        }
        check(after.keys == setOf(math.skillId) + ComprehensionSkill.entries.map { it.id }, "habilidades inesperadas en la memoria")
        // Sin responder cuenta como fallo, sin "error elegido".
        val blank = LongReadingQuiz.remember(emptyMap(), r, List(r.questions.size) { null }, 5L)
        check(blank.values.all { it.correct == 0 && it.recentErrors.isEmpty() }, "una pregunta sin responder no es un acierto")
    }

    println(if (fallas == 0) "\nPASS: lecturas largas (contenido, validador, calificación, orden, selección y memoria)."
            else "\n$fallas FALLAS")
    if (fallas != 0) exitProcess(1)
}
