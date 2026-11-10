import com.controlparental.kioscosuave.curriculum.*
import kotlin.random.Random
import kotlin.system.exitProcess

/**
 * Alfabetización de 1º: contenido, calificación y progresión por dominio.
 * Termina con código de error si algo falla (a diferencia de arneses antiguos).
 */
private var fallas = 0
private fun check(cond: Boolean, msg: String) {
    if (!cond) { println("  FALLA: $msg"); fallas++ }
}

private const val DAY = LearningMemory.DAY_MS
private val ORDER = LiteracyCatalog.skills.map { it.id }

/** Índices de ficha que forman la respuesta correcta. */
private fun solve(a: LiteracyActivity): List<Int> {
    if (!a.buildsWord) return listOf(a.choices.indexOf(a.answer))
    val free = a.choices.indices.toMutableList()
    return a.syllables.map { s -> free.first { a.choices[it] == s }.also { free.remove(it) } }
}

/** Una respuesta incorrecta pero completa (lo que un niño podría confirmar). */
private fun miss(a: LiteracyActivity): List<Int> =
    if (a.buildsWord) solve(a).reversed().let { r -> if (a.correct(r)) solve(a).drop(1) + (a.choices.indices - solve(a).toSet()).first() else r }
    else listOf(a.choices.indices.first { a.choices[it] != a.answer })

fun main() {
    val bank = LiteracyCatalog.activities

    // =============================================================
    println("### 1. Contenido del banco ###\n")
    bank.forEach { a -> LiteracyValidator.errors(a).forEach { check(false, "${a.id}: $it") } }
    check(bank.map { it.id }.distinct().size == bank.size, "hay identificadores repetidos")
    ORDER.forEach { id -> check(bank.count { it.skillId == id } >= 6, "$id tiene menos de 6 actividades") }
    LiteracyCatalog.skills.forEach { s ->
        check(s.prerequisites.all { it in ORDER }, "${s.id}: prerrequisito inexistente")
        check(s.prerequisites.all { ORDER.indexOf(it) < ORDER.indexOf(s.id) }, "${s.id}: prerrequisito posterior")
    }
    // Ninguna opción delata la respuesta por ser mucho más larga o corta.
    bank.filter { !it.buildsWord }.forEach { a ->
        val others = a.choices.filter { it != a.answer }.map { it.length }
        check(a.answer.length <= others.max() + 4 && a.answer.length >= others.min() - 4,
            "${a.id}: la respuesta destaca por longitud")
    }
    // Las palabras parecidas deben ser parecidas de verdad (misma longitud ±2).
    bank.filter { it.kind == LiteracyKind.WORD_IMAGE }.forEach { a ->
        check(a.choices.all { kotlin.math.abs(it.length - a.answer.length) <= 2 }, "${a.id}: distractor de otra longitud")
    }
    println("  ${bank.size} actividades · ${bank.groupingBy { it.kind }.eachCount()}")

    // =============================================================
    println("\n### 2. Calificación determinista ###\n")
    bank.forEach { a ->
        val ok = solve(a)
        check(a.canSubmit(ok) && a.correct(ok), "${a.id}: la respuesta correcta no se acepta")
        check(!a.correct(miss(a)), "${a.id}: se acepta una respuesta incorrecta")
        check(!a.canSubmit(emptyList()), "${a.id}: se puede enviar sin responder")
        if (a.buildsWord) {
            check(!a.canSubmit(ok.dropLast(1)), "${a.id}: se puede enviar una palabra incompleta")
            check(!a.canSubmit(ok + ok.first()), "${a.id}: se puede usar dos veces la misma ficha")
        }
    }
    val papa = bank.first { it.id == "build.raton" }
    check(LiteracyActivity.normalize("ratón") != LiteracyActivity.normalize("raton"), "el acento no debe ignorarse")
    check(papa.answer == "RATÓN", "ratón conserva su acento")

    // =============================================================
    println("### 3. Variedad: orden de fichas y repeticiones ###\n")
    run {
        val s = LiteracySession(emptyMap(), rnd = Random(7))
        val seen = mutableListOf<LiteracyActivity>()
        repeat(60) { val a = s.next(); seen += a; s.record(a, true) }
        check(seen.zipWithNext().none { (x, y) -> x.id == y.id }, "la misma actividad salió dos veces seguidas")
        val builds = seen.filter { it.buildsWord }
        check(builds.none { it.choices.take(it.syllables.size) == it.syllables }, "las sílabas aparecieron ya ordenadas")
        val soloCasa = bank.filter { it.id == "build.casa" }
        val casa = (1..40).map { LiteracySession(emptyMap(), rnd = Random(it), bank = soloCasa).next().choices }
        check(casa.distinct().size >= 8, "CASA casi siempre muestra las fichas en el mismo orden")
        val shared = Random(2026)   // semillas pequeñas consecutivas están correlacionadas
        val pos = bank.filter { !it.buildsWord }.flatMap { a -> (1..20).map { LiteracySession(emptyMap(), rnd = shared, bank = listOf(a.copy(skillId = LiteracyCatalog.TWO))).next().let { r -> r.choices.indexOf(r.answer) } } }
        (0..3).forEach { i -> check(pos.count { it == i } > pos.size / 6, "la respuesta correcta casi nunca cae en la posición $i") }
        seen.forEach { a -> check(LiteracyValidator.errors(a).isEmpty(), "${a.id}: deja de ser válida al barajar") }
    }

    // =============================================================
    println("### 4. Progresión por dominio, no por cantidad ###\n")
    run {
        // Alumno que acierta todo: avanza etapa por etapa, en orden.
        var t = 0L
        val s = LiteracySession(emptyMap(), now = { t }, rnd = Random(1))
        val firstSeen = LinkedHashMap<String, Int>()
        var n = 0
        repeat(12) {                       // 12 días de 10 actividades
            repeat(10) { val a = s.next(); firstSeen.putIfAbsent(a.skillId, n++); s.record(a, true) }
            t += DAY
        }
        println("  alumno que acierta: primera vez de cada habilidad -> ${firstSeen.map { "${it.key.substringAfter("lit.")}@${it.value}" }}")
        check(firstSeen.keys.containsAll(ORDER), "un alumno que acierta todo debe llegar a los cuentos")
        check(firstSeen.keys.toList().let { k -> LiteracyCatalog.skills.all { sk -> sk.prerequisites.all { p -> k.indexOf(p) < k.indexOf(sk.id) } } }, "se saltó un prerrequisito")
        check(firstSeen.getValue(LiteracyCatalog.THREE) >= LiteracySession.READY_PRACTICES, "pasó a tres sílabas sin practicar dos")
        check(firstSeen.getValue(LiteracyCatalog.DETAIL) >= 4 * LiteracySession.READY_PRACTICES, "llegó a cuentos demasiado pronto")

        // Alumno que adivina (50%): muchas actividades, pero no debe llegar a cuentos.
        var t2 = 0L
        val g = LiteracySession(emptyMap(), now = { t2 }, rnd = Random(2))
        val coin = Random(99)
        val seen = HashSet<String>()
        repeat(6) { repeat(10) { val a = g.next(); seen += a.skillId; g.record(a, coin.nextBoolean(), "x") }; t2 += DAY }
        println("  alumno al 50% tras 60 actividades: ${seen.map { it.substringAfter("lit.") }}")
        check(LiteracyCatalog.DETAIL !in seen && LiteracyCatalog.SEQUENCE !in seen, "60 actividades al azar no deben abrir los cuentos")

        // Alumno que falla siempre: se queda en la primera etapa.
        val f = LiteracySession(emptyMap(), rnd = Random(3))
        val only = HashSet<String>()
        repeat(40) { val a = f.next(); only += a.skillId; f.record(a, false, "x") }
        check(only == setOf(LiteracyCatalog.TWO), "quien falla siempre no debe salir de dos sílabas")
    }

    // =============================================================
    println("\n### 5. Refuerzo y repaso espaciado ###\n")
    run {
        var t = 0L
        val s = LiteracySession(emptyMap(), now = { t }, rnd = Random(5))
        repeat(12) { repeat(10) { s.record(s.next(), true) }; t += DAY }
        // Con todo asentado, falla la comprensión de oraciones: debe volver enseguida.
        val before = s.unlocked().map { it.id }
        val target = bank.first { it.id == "sentence.cat.where" }
        s.record(target, false, "en la cama")
        val nextTen = (1..10).map { s.next().also { a -> s.record(a, true) } }
        check(nextTen.take(3).any { it.skillId == LiteracyCatalog.WHERE }, "lo fallado debe reaparecer de inmediato")
        check(nextTen.first { it.skillId == LiteracyCatalog.WHERE }.id != target.id, "debe volver con OTRA oración")
        check(s.stateOf(LiteracyCatalog.WHERE)!!.recentErrors.contains("en la cama"), "no se registró el error concreto")
        check(s.unlocked().map { it.id } == before, "fallar un repaso no debe cerrar lo ya abierto")
        // Lo dominado deja de ocupar la sesión hasta que vence.
        val mastered = s.stateOf(LiteracyCatalog.TWO)!!
        check(mastered.dueAt > t, "dos sílabas dominadas deberían estar espaciadas")
        val today = (1..20).map { s.next().also { a -> s.record(a, true) }.skillId }
        check(today.count { it == LiteracyCatalog.TWO } <= 2, "lo dominado y no vencido no debe monopolizar la sesión")
        // El audio de apoyo se retira al asentarse la habilidad.
        check(!s.listeningSupport(LiteracyCatalog.DETAIL), "el audio debe retirarse al dominar")
        check(LiteracySession(emptyMap()).listeningSupport(LiteracyCatalog.WHERE), "al principio se permite escuchar")
    }

    // =============================================================
    println("### 6. No se pierde el progreso ###\n")
    run {
        // Memoria previa de otra materia: debe salir intacta.
        val math = SkillState("sec1.est.moda", practices = 9, correct = 7, streak = 2, intervalDays = 3f, dueAt = 123L)
        val s = LiteracySession(mapOf(math.skillId to math), rnd = Random(11))
        repeat(7) { s.record(s.next(), true) }
        check(s.snapshot()[math.skillId] == math, "se alteró la memoria de matemáticas")

        // Cerrar y abrir: misma actividad, mismas fichas, sin registrar nada.
        val a = s.next()
        val reopened = LiteracySession(s.snapshot(), s.recentIds(), rnd = Random(12))
        val back = reopened.restore(a.id, a.choices)
        check(back == a, "al reabrir no se recuperó la misma actividad")
        check(reopened.snapshot() == s.snapshot(), "reabrir cambió la memoria")
        check(reopened.restore("no.existe", a.choices) == null, "una actividad desconocida debe descartarse")
        check(reopened.restore(a.id, a.choices.drop(1)) == null, "un orden de fichas corrupto debe descartarse")
        check(LiteracySession(emptyMap()).restore("story.ball.0", bank.first { it.id == "story.ball.0" }.choices) == null,
            "no se debe restaurar una etapa que aún no está abierta")
        // Las actividades recientes se respetan tras reabrir.
        val after = (1..5).map { reopened.next().also { x -> reopened.record(x, true) }.id }
        check(a.id !in after.take(2) || bank.count { it.skillId == a.skillId } <= 2, "repitió la actividad recién vista tras reabrir")
    }

    println(if (fallas == 0) "\nPASS: alfabetización (contenido, calificación, variedad, progresión, refuerzo y persistencia)."
            else "\n$fallas FALLAS")
    if (fallas != 0) exitProcess(1)
}
