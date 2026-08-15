import com.controlparental.kioscosuave.curriculum.*
import kotlin.random.Random

private var fallas = 0
private fun check(cond: Boolean, msg: String) {
    if (!cond) { println("  FALLA: $msg"); fallas++ }
}

private const val DAY = LearningMemory.DAY_MS

fun main() {
    val catalogo = Curriculum.sec1Math
    val rnd = Random(42)   // determinista: la prueba debe ser reproducible

    // =============================================================
    // 1. Un conocimiento acertado siempre debe espaciarse
    // =============================================================
    println("### 1. Intervalos al acertar de forma sostenida ###\n")
    var s = SkillState("sec1.est.moda")
    var t = 0L
    val intervalos = mutableListOf<Float>()
    repeat(6) {
        s = LearningMemory.review(s, correct = true, format = ExerciseFormat.DIRECTO, now = t)
        intervalos += s.intervalDays
        t = s.dueAt
    }
    println("  dias entre repasos: ${intervalos.joinToString(", ") { "%.1f".format(it) }}")
    println("  dominio final: ${s.mastery}  (racha ${s.streak}, precision ${"%.0f".format(s.accuracy * 100)}%)")
    for (i in 1 until intervalos.size) {
        check(intervalos[i] >= intervalos[i - 1], "el intervalo se encogio pese a acertar")
    }
    check(s.mastery == Mastery.DOMINADO, "6 aciertos seguidos deberian dar DOMINADO")

    // =============================================================
    // 2. Al fallar debe volver de inmediato y con otro formato
    // =============================================================
    println("\n### 2. Al fallar vuelve enseguida y cambia de presentacion ###\n")
    var f = SkillState("sec1.ang.complementario")
    f = LearningMemory.review(f, true, ExerciseFormat.DIRECTO, now = 0L)
    val antesDeFallar = f.intervalDays
    f = LearningMemory.review(f, false, ExerciseFormat.DIRECTO, wrongChoice = "95°", now = DAY)
    println("  intervalo antes de fallar: ${antesDeFallar} dias")
    println("  intervalo tras fallar:     ${f.intervalDays} dias  (vence ya: ${f.dueAt <= DAY})")
    println("  error registrado:          ${f.recentErrors}")
    check(f.intervalDays == 0f, "tras fallar deberia volver en la misma sesion")
    check(f.dueAt <= DAY, "tras fallar deberia quedar vencido")
    check(f.streak == 0, "la racha deberia reiniciarse al fallar")
    check(f.recentErrors.contains("95°"), "deberia registrar QUE opcion incorrecta eligio")

    val skillAng = Curriculum.byId("sec1.ang.complementario")!!
    val siguiente = LearningMemory.pickFormat(skillAng, f, rnd)
    println("  formato anterior: DIRECTO -> siguiente: $siguiente")
    check(siguiente != ExerciseFormat.DIRECTO,
        "tras fallar en DIRECTO no deberia repetirse el mismo formato")

    // =============================================================
    // 3. El formato sube de nivel cognitivo conforme hay dominio
    // =============================================================
    println("\n### 3. Progresion recordar -> comprender -> aplicar -> razonar ###\n")
    val skillPct = Curriculum.byId("sec1.pct.de_cantidad")!!
    val nivelNuevo = LearningMemory.pickFormat(skillPct, null, rnd).cognitiveLevel
    var dom = SkillState(skillPct.id)
    var tt = 0L
    repeat(6) { dom = LearningMemory.review(dom, true, ExerciseFormat.DIRECTO, now = tt); tt = dom.dueAt }
    val nivelDominado = LearningMemory.pickFormat(skillPct, dom, rnd).cognitiveLevel
    println("  nivel cuando es NUEVO:     $nivelNuevo")
    println("  nivel cuando es DOMINADO:  $nivelDominado")
    check(nivelDominado >= nivelNuevo, "al dominar deberia pedirse un formato mas exigente")

    // =============================================================
    // 4. Simulacion de 40 dias con un alumno realista
    // =============================================================
    println("\n### 4. Simulacion: 40 dias, 8 ejercicios diarios ###\n")
    val estados = HashMap<String, SkillState>()
    // El alumno domina la mitad de los temas y arrastra estos cuatro, que son
    // justo los que se fallaron en los examenes reales.
    val flojas = setOf(
        "sec1.pct.inverso", "sec1.est.identificar",
        "sec1.ang.complementario", "sec1.prob.factorial"
    )

    var vistasDistintas = 0
    var repeticionesFormatoSeguidas = 0
    val conteoPorSkill = HashMap<String, Int>()

    for (dia in 0 until 40) {
        val ahora = dia * DAY
        val plan = LearningMemory.plan(catalogo, estados, ahora, count = 8, rnd = rnd)

        check(plan.size == 8, "dia $dia: el plan trajo ${plan.size} ejercicios en vez de 8")
        check(plan.map { it.skillId }.toSet().size == plan.size,
            "dia $dia: la misma habilidad aparece dos veces en la sesion")

        for (p in plan) {
            val prev = estados[p.skillId]
            if (prev == null) vistasDistintas++
            // Solo cuenta como falla si HABIA otro formato disponible: una
            // habilidad con un unico formato no puede alternar, y ahi la
            // variedad la aporta el generador (numeros y contexto distintos).
            val alternativas = Curriculum.byId(p.skillId)!!.formats.size
            if (prev != null && alternativas > 1 && prev.recentFormats.firstOrNull() == p.format) {
                repeticionesFormatoSeguidas++
            }
            conteoPorSkill[p.skillId] = (conteoPorSkill[p.skillId] ?: 0) + 1

            // El alumno acierta el 85% salvo en sus temas flojos (40%).
            val prob = if (p.skillId in flojas) 0.40 else 0.85
            val acierta = rnd.nextDouble() < prob
            estados[p.skillId] = LearningMemory.review(
                prev ?: SkillState(p.skillId),
                correct = acierta,
                format = p.format,
                wrongChoice = if (!acierta) "opcion-mala" else null,
                now = ahora
            )
        }
    }

    val dominadas = estados.values.count { it.mastery == Mastery.DOMINADO }
    val enProgreso = estados.values.count { it.mastery == Mastery.EN_PROGRESO }
    println("  habilidades tocadas:      ${estados.size} de ${catalogo.size}")
    println("  dominadas:                $dominadas")
    println("  en progreso:              $enProgreso")
    println("  formato repetido pudiendo evitarse: $repeticionesFormatoSeguidas veces")

    check(estados.size >= 30, "en 40 dias deberia haber cubierto buena parte del temario")
    check(dominadas > 0, "nadie llego a DOMINADO en 40 dias")
    check(repeticionesFormatoSeguidas == 0,
        "se repitio el mismo formato dos veces seguidas ($repeticionesFormatoSeguidas)")

    // Lo flojo debe practicarse MAS que lo dominado: ese es todo el punto.
    val mediaFlojas = flojas.mapNotNull { conteoPorSkill[it] }.average()
    val mediaResto = conteoPorSkill.filterKeys { it !in flojas }.values.average()
    println("  practicas medias temas flojos:  ${"%.1f".format(mediaFlojas)}")
    println("  practicas medias temas normales:${"%.1f".format(mediaResto)}")
    check(mediaFlojas > mediaResto,
        "los temas que se fallan deberian practicarse MAS que los demas")

    // =============================================================
    // 5. Prerrequisitos
    // =============================================================
    println("\n### 5. Prerrequisitos ###\n")
    val vacio = emptyMap<String, SkillState>()
    val primerPlan = LearningMemory.plan(catalogo, vacio, 0L, count = 8, rnd = rnd)
    val conPrereq = primerPlan.count { p ->
        Curriculum.byId(p.skillId)!!.prerequisites.isNotEmpty()
    }
    println("  en la primera sesion, habilidades con prerrequisito sin cumplir: $conPrereq")
    // El relleno puede meter alguna al final; lo que no debe pasar es que la
    // sesion inicial este DOMINADA por habilidades bloqueadas.
    check(conPrereq <= 2, "la primera sesion trae demasiadas habilidades con prerrequisitos")

    // =============================================================
    // 6. Resumen para el panel de padres
    // =============================================================
    println("\n### 6. Resumen para el panel de padres ###\n")
    val debiles = LearningMemory.weakest(catalogo, estados, limit = 5)
    println("  Lo que mas le cuesta:")
    debiles.forEach { (sk, st) ->
        println("    - ${sk.topic}: ${sk.label}  (${"%.0f".format(st.accuracy * 100)}%, ${st.practices} practicas)")
    }
    check(debiles.isNotEmpty(), "deberia identificar habilidades debiles")
    check(debiles.any { it.first.id in flojas },
        "los temas flojos deberian aparecer entre los mas debiles")

    val porTema = LearningMemory.masteryByTopic(catalogo, estados)
    check(porTema.size == 13, "deberia resumir los 13 temas, resumio ${porTema.size}")

    println("\n" + "=".repeat(70))
    if (fallas == 0) println("OK: la memoria de aprendizaje se comporta como se espera.")
    else println("FALLAS: $fallas")
}
