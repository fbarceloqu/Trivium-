import com.controlparental.kioscosuave.curriculum.Curriculum
import com.controlparental.kioscosuave.curriculum.Sec1MathGenerator

/**
 * Arnés temporal: genera muchas veces cada habilidad × formato y verifica las
 * invariantes que romperían el ejercicio para el niño.
 */
fun main() {
    var total = 0
    val fallas = mutableListOf<String>()

    for (skill in Sec1MathGenerator.implemented) {
        for (format in skill.formats) {
            repeat(300) {
                val q = Sec1MathGenerator.generate(skill, format)
                if (q == null) {
                    fallas += "${skill.id}/$format -> generate() devolvio null"
                    return@repeat
                }
                total++

                if (q.answer !in q.options) {
                    fallas += "${skill.id}/$format -> la respuesta '${q.answer}' NO esta " +
                        "entre las opciones ${q.options} | pregunta: ${q.question.take(90)}"
                }
                if (q.options.size != 4) {
                    fallas += "${skill.id}/$format -> ${q.options.size} opciones en vez de 4: ${q.options}"
                }
                if (q.options.distinct().size != q.options.size) {
                    fallas += "${skill.id}/$format -> opciones repetidas: ${q.options}"
                }
                if (q.question.isBlank()) fallas += "${skill.id}/$format -> pregunta vacia"
                if (q.steps.isEmpty()) fallas += "${skill.id}/$format -> sin pasos de solucion"
                if (q.skillId != skill.id) fallas += "${skill.id}/$format -> skillId mal etiquetado: ${q.skillId}"
            }
        }
    }

    println("Ejercicios generados: $total")
    println("Habilidades con generador: ${Sec1MathGenerator.implemented.size}")
    println("Habilidades pendientes:   ${Sec1MathGenerator.pending.size}")

    val unicas = fallas.distinct()
    if (unicas.isEmpty()) {
        println("OK: ninguna invariante rota.")
    } else {
        println("\nFALLAS (${fallas.size} totales, ${unicas.size} distintas):")
        unicas.take(40).forEach { println("  - $it") }
    }

    // Muestra de contenido real: un ejercicio por habilidad.
    println("\n=================== MUESTRA ===================")
    for (skill in Sec1MathGenerator.implemented) {
        val q = Sec1MathGenerator.generate(skill) ?: continue
        println("\n[${skill.topic}] ${skill.label}")
        println("  ${q.question.replace("\n", "\n  ")}")
        println("  Opciones: ${q.options.joinToString("  |  ")}")
        println("  Correcta: ${q.answer}   (formato ${q.format})")
        q.steps.forEach { println("    · $it") }
    }

    // Cobertura del temario declarado.
    val porTema = Curriculum.topics(
        com.controlparental.kioscosuave.GradeLevel.SECUNDARIA,
        com.controlparental.kioscosuave.curriculum.Subject.MATH
    )
    println("\nCobertura por tema:")
    porTema.forEach { (tema, skills) ->
        val listos = skills.count { it.id in Sec1MathGenerator.implementedIds }
        println("  ${if (listos == skills.size) "OK " else "   "} $tema: $listos/${skills.size}")
    }
}
