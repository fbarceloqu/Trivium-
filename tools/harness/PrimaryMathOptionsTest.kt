import com.controlparental.kioscosuave.ChallengeEngine
import com.controlparental.kioscosuave.Difficulty
import com.controlparental.kioscosuave.curriculum.MathOptionValidator
import kotlin.system.exitProcess

/**
 * Distractores de matemáticas de 1º de primaria (STARTER) y primaria
 * (EASY / MEDIUM): la misma revisión que MathOptionsTest hace en secundaria.
 * Con el argumento "audit" solo informa.
 */
fun main(args: Array<String>) {
    val auditOnly = args.contains("audit")
    var bad = 0
    var total = 0
    for (d in listOf(Difficulty.STARTER, Difficulty.EASY, Difficulty.MEDIUM)) {
        val byRule = HashMap<String, Int>()
        val samples = LinkedHashMap<String, String>()
        var n = 0
        var b = 0
        repeat(6000) {
            val q = ChallengeEngine.generateMath(d)
            n++
            // Invariantes básicas, además de la calidad de los distractores.
            // Las preguntas de sí/no tienen dos opciones por naturaleza.
            if (q.options.toSet() == setOf("yes", "no") || q.options.toSet() == setOf("sí", "no")) return@repeat
            val errs = MathOptionValidator.errors(q.question, q.options, q.answer)
            if (errs.isNotEmpty()) {
                b++
                errs.forEach { e ->
                    val rule = e.substringBefore(":").replace(Regex("«.*"), "").trim()
                    byRule.merge(rule, 1, Int::plus)
                    samples.putIfAbsent(rule, "${q.question.replace("\n", " ").take(90)} -> ${q.options} (correcta ${q.answer})")
                }
            }
        }
        total += n; bad += b
        println("### $d: $n ejercicios · con defecto: $b (${"%.1f".format(b * 100.0 / n)}%)")
        byRule.entries.sortedByDescending { it.value }.forEach { println("  %6d  %s\n          %s".format(it.value, it.key, samples[it.key])) }
        println()
    }
    if (bad == 0) println("PASS: opciones de matemáticas de 1º y primaria ($total ejercicios).")
    else println("$bad de $total ejercicios con opciones defectuosas")
    if (bad != 0 && !auditOnly) exitProcess(1)
}
