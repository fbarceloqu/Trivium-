import com.controlparental.kioscosuave.curriculum.*
import kotlin.random.Random

private var fallas = 0
private fun check(c: Boolean, m: String) { if (!c) { println("  FALLA: $m"); fallas++ } }
private const val DAY = LearningMemory.DAY_MS

fun main() {
    val catalogo = Curriculum.sec1Math
    val rnd = Random(7)

    // =============================================================
    // 1. Emparejar la guia con el temario SIN duplicar conceptos
    // =============================================================
    println("### 1. Temas de la guia -> habilidades existentes ###\n")
    val temas = listOf(
        "Repasar fracciones equivalentes",
        "Comparar fracciones",
        "Angulos complementarios y suplementarios",
        "Media, moda y mediana",
        "Fotosintesis de las plantas"          // no existe en el temario de mate
    )
    val m = GuidePolicy.match(temas, catalogo)
    println("  habilidades emparejadas: ${m.skillIds.size}")
    m.skillIds.forEach { println("    - $it") }
    println("  temas NO cubiertos: ${m.unmatchedTopics}")

    check(m.skillIds.contains("sec1.frac.equivalentes"), "no emparejo fracciones equivalentes")
    check(m.skillIds.contains("sec1.frac.comparar"), "no emparejo comparar fracciones")
    check(m.skillIds.any { it.startsWith("sec1.ang.") }, "no emparejo angulos")
    check(m.skillIds.any { it.startsWith("sec1.est.") }, "no emparejo estadistica")
    check(m.unmatchedTopics.contains("Fotosintesis de las plantas"),
        "deberia reportar como NO cubierto lo que el temario no tiene")
    check(m.skillIds.size == m.skillIds.distinct().size, "hay habilidades duplicadas")
    check(m.skillIds.all { Curriculum.byId(it) != null },
        "emparejo con una habilidad que no existe (creo un concepto nuevo)")

    // =============================================================
    // 2. La urgencia escala conforme se acerca el examen
    // =============================================================
    println("\n### 2. Cuenta regresiva del examen ###\n")
    val ahora = 100L * DAY
    fun guiaCon(dias: Int) = StudyGuide(
        id = "g1", title = "Guia de examen", mode = GuideMode.EXAM_PREP,
        examAt = ahora + dias * DAY, skillIds = m.skillIds
    )
    val curva = listOf(20, 14, 7, 3, 1, 0, -2).map { it to GuidePolicy.priority(guiaCon(it), ahora) }
    curva.forEach { (d, p) -> println("    faltan ${"%3d".format(d)} dias -> prioridad ${"%.2f".format(p)}") }

    for (i in 1 until 5) {
        check(curva[i].second >= curva[i - 1].second,
            "la prioridad no subio al acercarse el examen (${curva[i-1].first} -> ${curva[i].first})")
    }
    val despues = GuidePolicy.priority(guiaCon(-2), ahora)
    check(despues > 0f, "tras el examen la guia desaparecio; deberia quedar como repaso acumulativo")
    check(despues < curva.first { it.first == 1 }.second,
        "tras el examen deberia pesar MENOS que la vispera")

    val pausada = guiaCon(3).copy(paused = true)
    check(GuidePolicy.priority(pausada, ahora) == 0f, "una guia pausada no deberia pesar")

    // =============================================================
    // 3. La guia sube prioridad pero NO monopoliza la sesion
    // =============================================================
    println("\n### 3. Equilibrio: guia + repaso acumulativo ###\n")
    val estados = HashMap<String, SkillState>()
    // El alumno ya domina bastante temario general.
    catalogo.take(20).forEach { s ->
        var st = SkillState(s.id); var t = 0L
        repeat(4) { st = LearningMemory.review(st, true, s.formats.first(), now = t); t = st.dueAt }
        estados[s.id] = st
    }
    val plan = GuidePolicy.plan(catalogo, estados, listOf(guiaCon(3)), ahora, 8, rnd)
    val deGuia = plan.count { it.skillId in m.skillIds }
    println("  ejercicios de la guia: $deGuia de ${plan.size}")
    check(plan.size == 8, "el plan trajo ${plan.size} en vez de 8")
    check(plan.map { it.skillId }.distinct().size == plan.size, "hay habilidades repetidas")
    check(deGuia > 0, "la guia no influyo en el plan")
    check(deGuia < plan.size, "la guia monopolizo la sesion; debe quedar sitio para repaso acumulativo")

    // =============================================================
    // 4. Aparecer en la guia NO implica dominio: hay que medir
    // =============================================================
    println("\n### 4. La guia marca prioridad, el desempeno marca dominio ###\n")
    val sinPracticar = m.skillIds.filter { estados[it] == null }
    val planNuevo = GuidePolicy.plan(catalogo, estados, listOf(guiaCon(10)), ahora, 8, rnd)
    val medidos = planNuevo.count { it.skillId in sinPracticar }
    println("  habilidades de la guia sin medir: ${sinPracticar.size}")
    println("  de esas, incluidas en el plan:    $medidos")
    check(medidos > 0, "no esta midiendo las habilidades de la guia que nunca se han practicado")

    // =============================================================
    // 5. Lo flojo de la guia pesa mas que lo dominado
    // =============================================================
    println("\n### 5. Dentro de la guia manda lo que peor va ###\n")
    val flojo = "sec1.frac.comparar"
    val fuerte = "sec1.frac.equivalentes"
    var stFlojo = SkillState(flojo); var stFuerte = SkillState(fuerte); var t = 0L
    repeat(6) {
        stFlojo = LearningMemory.review(stFlojo, it % 3 == 0, ExerciseFormat.DIRECTO, now = t)
        stFuerte = LearningMemory.review(stFuerte, true, ExerciseFormat.DIRECTO, now = t)
        t += DAY
    }
    estados[flojo] = stFlojo; estados[fuerte] = stFuerte
    println("  $flojo  -> ${"%.0f".format(stFlojo.accuracy * 100)}%")
    println("  $fuerte -> ${"%.0f".format(stFuerte.accuracy * 100)}%")

    var vecesFlojo = 0; var vecesFuerte = 0
    repeat(30) { d ->
        val p = GuidePolicy.plan(catalogo, estados, listOf(guiaCon(10)), ahora + d * DAY, 8, Random(d))
        if (p.any { it.skillId == flojo }) vecesFlojo++
        if (p.any { it.skillId == fuerte }) vecesFuerte++
    }
    println("  apariciones en 30 planes: flojo=$vecesFlojo  fuerte=$vecesFuerte")
    check(vecesFlojo >= vecesFuerte, "el tema flojo deberia practicarse al menos tanto como el fuerte")

    // =============================================================
    // 6. Modo AGREGAR AL APRENDIZAJE: sin urgencia de examen
    // =============================================================
    println("\n### 6. Los dos modos ###\n")
    val aprendizaje = StudyGuide("g2", "Tema de la semana", mode = GuideMode.LEARNING, skillIds = m.skillIds)
    val pExamen = GuidePolicy.priority(guiaCon(3), ahora)
    val pAprend = GuidePolicy.priority(aprendizaje, ahora)
    println("  PREPARAR EXAMEN (3 dias): ${"%.2f".format(pExamen)}")
    println("  AGREGAR AL APRENDIZAJE:   ${"%.2f".format(pAprend)}")
    check(pAprend > 1f, "el material sin examen deberia influir algo")
    check(pExamen > pAprend, "un examen inminente deberia pesar mas que material sin fecha")

    // =============================================================
    // 7. Resumen en lenguaje llano
    // =============================================================
    println("\n### 7. Resumen para el padre ###\n")
    println("  \"" + GuidePolicy.summary("Gretel", guiaCon(5), catalogo, estados, ahora) + "\"")
    val resumen = GuidePolicy.summary("Gretel", guiaCon(5), catalogo, estados, ahora)
    check(resumen.contains("Gretel"), "el resumen deberia nombrar al nino")
    check(resumen.contains("%"), "el resumen deberia decir como va")
    check(!resumen.contains("sec1."), "el resumen no deberia filtrar identificadores tecnicos")

    println("\n" + "=".repeat(66))
    if (fallas == 0) println("OK: la politica de guias se comporta como se espera.")
    else println("FALLAS: $fallas")
}
