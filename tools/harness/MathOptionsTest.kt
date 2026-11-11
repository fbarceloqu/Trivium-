import com.controlparental.kioscosuave.curriculum.MathOptionValidator
import com.controlparental.kioscosuave.curriculum.Sec1MathGenerator
import kotlin.system.exitProcess

/**
 * Auditoría de distractores: genera muchas veces cada habilidad × formato de
 * matemáticas de secundaria y cuenta en cuántos ejercicios la respuesta se
 * puede adivinar sin resolver (magnitud, forma, cantidades imposibles,
 * opciones equivalentes).
 *
 * Con el argumento "audit" solo informa; sin él, falla si queda algún defecto.
 */
private var fallas = 0
private fun check(cond: Boolean, msg: String) {
    if (!cond) { println("  FALLA: $msg"); fallas++ }
}

fun main(args: Array<String>) {
    val auditOnly = args.contains("audit")
    val V = MathOptionValidator

    // =============================================================
    println("### 1. El validador reconoce los defectos conocidos ###\n")
    fun has(q: String, o: List<String>, a: String, expect: String) =
        check(V.errors(q, o, a).any { expect in it }, "no detectó «$expect» en $o (dio ${V.errors(q, o, a)})")
    fun clean(q: String, o: List<String>, a: String) =
        check(V.errors(q, o, a).isEmpty(), "falso positivo en $o: ${V.errors(q, o, a)}")

    // El caso que reportó el padre.
    has("Si el 15% de una cantidad es 36, ¿cuál es la cantidad?", listOf("240", "5.4", "30.6", "51"), "240", "mucho mayor")
    has("Vendió el 15% de las galletas y fueron 36 galletas. ¿Cuántas galletas tenía la caja?",
        listOf("240", "5.4", "30.6", "51"), "240", "cantidad imposible")
    has("¿Cuánto es?", listOf("240", "5.4", "30.6", "51.5"), "240", "única entera")
    has("¿Cuánto es?", listOf("0.5", "0.50", "0.25", "0.75"), "0.5", "equivalentes")
    has("¿Cuánto es?", listOf("4", "40", "48", "52"), "4", "mucho menor")
    // Clasificar ejemplares: ni el tamaño ni dos incorrectas equivalentes delatan nada.
    clean("¿Cuál de los siguientes números es primo?", listOf("3", "12", "25", "21"), "3")
    clean("¿Cuál de los siguientes es un número irracional?", listOf("3/4", "0.75", "0.5", "π"), "π")
    has("¿Cuál de las siguientes fracciones es equivalente a 1/2?", listOf("2/4", "4/8", "1/3", "3/4"), "2/4", "equivalentes")
    clean("¿A qué porcentaje equivale la fracción 1/5?", listOf("0.2%", "80%", "20%", "5%"), "20%")
    has("¿Cuánto es?", listOf("1/2", "2/4", "1/3", "3/4"), "1/2", "equivalentes")
    has("¿Cuánto es?", listOf("0.5", "1/2", "1/3", "3/4"), "0.5", "equivalentes")
    has("¿Cuánto es?", listOf("12", "12", "14", "16"), "12", "repetidas")
    has("¿Cuánto es?", listOf("12%", "15", "18", "20"), "12%", "única con su unidad")
    has("¿Cuántos alumnos aprobaron?", listOf("28", "-12", "32", "30"), "28", "cantidad imposible")
    has("¿Cuál es?", listOf("Un número que se puede escribir como fracción de dos enteros", "Par", "Primo", "Entero"),
        "Un número que se puede escribir como fracción de dos enteros", "más larga")
    clean("Si el 15% de una cantidad es 36, ¿cuál es la cantidad?", listOf("240", "204", "276", "540"), "240")
    clean("¿Cuánto mide el ángulo?", listOf("35°", "55°", "145°", "125°"), "35°")
    clean("¿Cuánto pagó?", listOf("$128", "$32", "$192", "$140"), "$128")
    clean("¿Cuánto es?", listOf("0.5", "50%", "5", "0.05"), "0.5")       // unidades distintas: no son equivalentes
    clean("¿Cuánto es -3 + 5?", listOf("2", "-2", "8", "-8"), "2")
    check(V.asksForCount("¿Cuántos alumnos hay en total?") && !V.asksForCount("¿Cuántos metros mide?") &&
        !V.asksForCount("¿Cuánto cuesta el artículo?"), "detección de cosas contables")
    check(V.parse("$12.50")!!.value == 12.5 && V.parse("3/4")!!.value == 0.75 && V.parse("45%")!!.shape == "#%" &&
        V.parse("3 y 5") == null && V.parse("2x + 3") == null, "lectura de opciones")

    // =============================================================
    println("\n### 2. Auditoría: generador sin revisar vs. lo que recibe el alumno ###\n")
    fun audit(label: String, show: Boolean, make: (com.controlparental.kioscosuave.curriculum.Skill, com.controlparental.kioscosuave.curriculum.ExerciseFormat) -> com.controlparental.kioscosuave.MathQuestion?): Int {
        var total = 0
        var bad = 0
        val byRule = HashMap<String, Int>()
        val bySkill = LinkedHashMap<String, IntArray>()       // [total, malos]
        val samples = LinkedHashMap<String, String>()
        for (skill in Sec1MathGenerator.implemented) for (format in skill.formats) {
            val key = "${skill.id}/$format"
            val acc = bySkill.getOrPut(key) { IntArray(2) }
            repeat(400) {
                val q = make(skill, format) ?: return@repeat
                total++; acc[0]++
                val errs = V.errors(q.question, q.options, q.answer)
                if (errs.isNotEmpty()) {
                    bad++; acc[1]++
                    errs.forEach { e ->
                        val rule = e.substringBefore(":").replace(Regex("«.*"), "").trim()
                        byRule.merge(rule, 1, Int::plus)
                        samples.putIfAbsent("$key | $rule", "${q.question.replace("\n", " ").take(110)} -> ${q.options} (correcta ${q.answer})")
                    }
                }
            }
        }
        println("  [$label] ejercicios: $total · con algún defecto: $bad (${"%.1f".format(bad * 100.0 / total)}%)")
        byRule.entries.sortedByDescending { it.value }.forEach { println("  %8d  %s".format(it.value, it.key)) }
        if (show) {
            val worst = bySkill.filterValues { it[1] > 0 }.entries.sortedByDescending { it.value[1].toDouble() / it.value[0] }
            println("\n  habilidad/formato con defectos: ${worst.size} de ${bySkill.size}")
            worst.forEach { println("  %5.1f%%  %s".format(it.value[1] * 100.0 / it.value[0], it.key)) }
            println("\n  ejemplos:")
            samples.entries.take(40).forEach { println("   · ${it.key}\n       ${it.value}") }
        }
        println()
        return bad
    }
    audit("sin revisar", auditOnly) { s, f -> Sec1MathGenerator.raw(s, f) }
    val bad = audit("entregado ", true) { s, f -> Sec1MathGenerator.generate(s, f) }

    // =============================================================
    println("### 3. La reparación no altera el ejercicio ###\n")
    var repaired = 0
    var keptOriginals = 0
    val demo = ArrayList<String>()
    for (skill in Sec1MathGenerator.implemented) for (format in skill.formats) repeat(150) {
        val q = Sec1MathGenerator.raw(skill, format) ?: return@repeat
        val before = V.errors(q.question, q.options, q.answer)
        val fixed = com.controlparental.kioscosuave.curriculum.MathOptionRepair.repair(q.question, q.options, q.answer)
        check(fixed.size == 4 && fixed.distinct().size == 4, "${skill.id}: la reparación no deja 4 opciones distintas: $fixed")
        check(fixed.count { it == q.answer } == 1, "${skill.id}: la reparación perdió o duplicó la respuesta")
        if (before.isEmpty()) check(fixed == q.options, "${skill.id}: se tocó un ejercicio que estaba bien")
        else if (fixed != q.options) {
            repaired++
            keptOriginals += fixed.count { it in q.options && it != q.answer }
            if (demo.size < 8 && demo.none { it.startsWith(skill.id) })
                demo += "${skill.id}: ${q.question.replace("\n", " ").take(90)}\n       antes   ${q.options}\n       después $fixed  (correcta ${q.answer})"
        }
    }
    demo.forEach { println("   · $it") }
    if (repaired > 0) println("\n  reparados: $repaired · distractores originales conservados por ejercicio: ${"%.2f".format(keptOriginals.toDouble() / repaired)} de 3")
    // El caso que reportó el padre, de principio a fin.
    val galletas = "Vendió el 15% de las galletas de una caja y fueron 36 galletas. ¿Cuántas galletas tenía la caja?"
    val fixed = com.controlparental.kioscosuave.curriculum.MathOptionRepair.repair(galletas, listOf("240", "5.4", "30.6", "51"), "240")
    println("  caso reportado: [240, 5.4, 30.6, 51] -> $fixed")
    check(V.errors(galletas, fixed, "240").isEmpty() && "240" in fixed, "el caso reportado no queda reparado")

    // =============================================================
    println("\n### 4. Las pistas de la pizarra nunca contienen la respuesta ###\n")
    val H = com.controlparental.kioscosuave.curriculum.MathHints
    var withHints = 0
    var hintTotal = 0
    for (skill in Sec1MathGenerator.implemented) for (format in skill.formats) repeat(120) {
        val q = Sec1MathGenerator.generate(skill, format) ?: return@repeat
        val hints = H.safe(q.steps, q.answer)
        hintTotal++
        if (hints.isNotEmpty()) withHints++
        check(hints.size <= H.MAX, "${skill.id}: demasiadas pistas")
        check(hints.size < q.steps.size || q.steps.isEmpty(), "${skill.id}: las pistas incluyen el procedimiento completo")
        hints.forEach { h -> check(!H.reveals(h, q.answer), "${skill.id}: la pista «$h» contiene la respuesta ${q.answer}") }
    }
    println("  ejercicios con al menos una pista: ${"%.0f".format(withHints * 100.0 / hintTotal)}%")
    check(H.reveals("168 ÷ 14 = 12", "12") && H.reveals("Pagas $170", "$170") && H.reveals("Es el 25%", "25%"), "no reconoce la respuesta dentro de un paso")
    check(!H.reveals("El total es 120", "12") && !H.reveals("3.12 × 2", "12") && !H.reveals("12.5 por pieza", "12"), "confunde 12 con 120, 3.12 o 12.5")
    check(H.safe(listOf("20% = 0.20", "80 × 0.20 = 16"), "16") == listOf("20% = 0.20"), "debe dar el planteamiento y cortar en el resultado")
    check(H.safe(listOf("168 ÷ 14 = 12", "Comprueba"), "12").isEmpty(), "si el primer paso ya da el resultado, no hay pista")
    check(H.safe(listOf("a", "b"), "zz").size == 1, "nunca se entrega el procedimiento entero")

    if (!auditOnly) check(bad == 0, "$bad ejercicios entregados tienen opciones defectuosas")
    println(if (fallas == 0) "\nPASS: opciones de matemáticas." else "\n$fallas FALLAS")
    if (fallas != 0) exitProcess(1)
}
