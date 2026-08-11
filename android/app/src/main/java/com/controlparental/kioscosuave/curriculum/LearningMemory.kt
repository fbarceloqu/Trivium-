package com.controlparental.kioscosuave.curriculum

import kotlin.math.abs
import kotlin.random.Random

/**
 * MEMORIA DE APRENDIZAJE  (Fase 2)
 * ================================
 *
 * Kotlin PURO, sin Android ni Compose: toda la política de repaso vive aquí
 * para poder simularla y verificarla fuera del dispositivo. La persistencia
 * (SharedPreferences + JSON) y el enganche con la UI son capas aparte.
 *
 * POR QUÉ EXISTE
 * --------------
 * Hasta ahora el progreso se guardaba por MATERIA: `{correct: 7, attempts: 10}`.
 * Con ese dato es imposible saber QUÉ necesita practicar el niño. En los
 * exámenes reales se vio el costo: el mismo error de media/moda/mediana
 * apareció en noviembre y otra vez en marzo, cuatro meses sin que nadie lo
 * detectara ni lo reforzara.
 *
 * LA MEMORIA NO ES SOLO ANTI-REPETICIÓN
 * -------------------------------------
 * No sirve únicamente para no repetir el mismo ejercicio. Decide:
 *   QUÉ practicar   -> lo vencido y lo que más se falla
 *   CUÁNDO          -> intervalo creciente si hay dominio, inmediato si se falla
 *   CON QUÉ FORMATO -> el concepto se repite, la presentación cambia
 *   HASTA DÓNDE     -> recordar → comprender → aplicar → razonar
 *
 * La repetición NO se elimina: se espacia y se varía. Lo que se evita es
 * "misma pregunta + mismo formato + todos los días".
 */

/** Qué tan asentado está un conocimiento. */
enum class Mastery { NUEVO, EN_PROGRESO, DOMINADO }

/** Por qué el planificador eligió una habilidad (útil para el panel de padres). */
enum class Reason {
    /** Tocaba repasarlo: el intervalo venció. */
    REPASO,

    /** Se está fallando: vuelve antes de lo que tocaría. */
    REFUERZO,

    /** Conocimiento nuevo, con sus prerrequisitos ya vistos. */
    NUEVO
}

/**
 * Lo que la app sabe sobre UNA habilidad de UN niño.
 *
 * [dueAt] es el corazón de la repetición espaciada: el momento a partir del
 * cual conviene volver a preguntarlo.
 */
data class SkillState(
    val skillId: String,
    val practices: Int = 0,
    val correct: Int = 0,
    /** Aciertos consecutivos. Se reinicia al fallar. */
    val streak: Int = 0,
    /** Veces que se falló DESPUÉS de haberlo dominado (olvido real). */
    val lapses: Int = 0,
    val lastPracticedAt: Long = 0L,
    val dueAt: Long = 0L,
    val intervalDays: Float = 0f,
    /** Facilidad estimada; crece con los aciertos y baja con los fallos. */
    val ease: Float = LearningMemory.EASE_START,
    /** Últimos formatos usados, para no repetir la misma presentación. */
    val recentFormats: List<ExerciseFormat> = emptyList(),
    /** Últimas respuestas incorrectas elegidas: dicen QUÉ error se comete. */
    val recentErrors: List<String> = emptyList()
) {
    val accuracy: Float get() = if (practices == 0) 0f else correct.toFloat() / practices

    val mastery: Mastery
        get() = when {
            practices == 0 -> Mastery.NUEVO
            streak >= 3 && accuracy >= 0.8f && intervalDays >= 7f -> Mastery.DOMINADO
            else -> Mastery.EN_PROGRESO
        }
}

/** Una entrada del plan del día. */
data class PlannedExercise(
    val skillId: String,
    val format: ExerciseFormat,
    val reason: Reason
)

object LearningMemory {

    const val DAY_MS = 86_400_000L
    const val EASE_START = 2.3f

    private const val EASE_MAX = 3.0f
    private const val EASE_MIN = 1.4f
    private const val EASE_UP = 0.08f
    private const val EASE_DOWN = 0.25f
    private const val MAX_INTERVAL_DAYS = 60f
    private const val MAX_RECENT = 4

    /** Umbral por debajo del cual una habilidad se considera "en problemas". */
    private const val WEAK_ACCURACY = 0.70f

    // =================================================================
    //  REGISTRAR UNA RESPUESTA
    // =================================================================

    /**
     * Actualiza el estado tras responder.
     *
     * Al ACERTAR el intervalo crece (1 día, luego × ease, con tope de 60):
     * el conocimiento se consolida y deja de ocupar sitio en el reto diario.
     *
     * Al FALLAR el intervalo se va a cero, así que vuelve enseguida — pero
     * [recentFormats] garantiza que vuelva con OTRA presentación, que es la
     * forma útil de reintentar: si alguien no entiende "7 × 8", repetirle
     * "7 × 8" no ayuda; mostrarle "7+7+7…" sí.
     *
     * [wrongChoice] es la opción incorrecta que eligió. Se guarda porque los
     * distractores están diseñados como errores típicos concretos: saber cuál
     * marcó dice QUÉ está confundiendo, no solo que falló.
     */
    fun review(
        state: SkillState,
        correct: Boolean,
        format: ExerciseFormat,
        wrongChoice: String? = null,
        now: Long
    ): SkillState {
        val eraDominado = state.mastery == Mastery.DOMINADO

        val ease = if (correct) (state.ease + EASE_UP).coerceAtMost(EASE_MAX)
        else (state.ease - EASE_DOWN).coerceAtLeast(EASE_MIN)

        val interval = when {
            !correct -> 0f                       // vuelve en esta misma sesión
            state.intervalDays < 1f -> 1f        // primer acierto: mañana
            else -> (state.intervalDays * ease).coerceAtMost(MAX_INTERVAL_DAYS)
        }

        return state.copy(
            practices = state.practices + 1,
            correct = state.correct + if (correct) 1 else 0,
            streak = if (correct) state.streak + 1 else 0,
            lapses = state.lapses + if (!correct && eraDominado) 1 else 0,
            lastPracticedAt = now,
            dueAt = now + (interval * DAY_MS).toLong(),
            intervalDays = interval,
            ease = ease,
            recentFormats = (listOf(format) + state.recentFormats).distinct().take(MAX_RECENT),
            recentErrors = if (wrongChoice != null)
                (listOf(wrongChoice) + state.recentErrors).take(MAX_RECENT)
            else state.recentErrors
        )
    }

    // =================================================================
    //  ELEGIR EL FORMATO
    // =================================================================

    /**
     * Con qué presentación pedir esta habilidad.
     *
     * Dos criterios, en este orden:
     * 1. NO repetir un formato reciente (aunque el concepto sí se repita).
     * 2. Acercarse al nivel cognitivo que corresponde al dominio actual:
     *    recordar → comprender → aplicar → razonar. Un conocimiento dominado
     *    deja de pedirse en su forma más básica.
     */
    fun pickFormat(skill: Skill, state: SkillState?, rnd: Random = Random): ExerciseFormat {
        val available = skill.formats
        if (available.size == 1) return available.first()

        val levels = available.map { it.cognitiveLevel }.sorted()
        val target = when (state?.mastery ?: Mastery.NUEVO) {
            Mastery.NUEVO -> levels.first()
            Mastery.EN_PROGRESO -> levels[levels.size / 2]
            Mastery.DOMINADO -> levels.last()
        }
        val recent = state?.recentFormats ?: emptyList()

        // Se puntúa cada formato y gana el de menor penalización.
        //
        // El castigo es GRADUAL, no binario: lo que de verdad hay que evitar es
        // repetir el formato INMEDIATAMENTE anterior. Penalizar por igual todo
        // lo "reciente" hacía que en una habilidad con solo dos formatos ambos
        // quedaran vetados en cuanto se usaran una vez, y la alternancia se
        // perdía. Así, con dos formatos se alterna estricto y con tres o más
        // se rota.
        val scored = available.map { f ->
            val penal = when {
                f == recent.firstOrNull() -> 100
                f in recent -> 10
                else -> 0
            }
            f to (penal + abs(f.cognitiveLevel - target))
        }
        val best = scored.minOf { it.second }
        return scored.filter { it.second == best }.random(rnd).first
    }

    // =================================================================
    //  PLANIFICAR EL RETO DEL DÍA
    // =================================================================

    /**
     * Arma la lista de ejercicios de la sesión.
     *
     * Mezcla deliberada: ~60% repaso vencido, ~30% refuerzo de lo que se falla
     * y ~10% conocimiento nuevo. Si un cubo se queda corto, los otros lo
     * rellenan, así que la sesión siempre sale completa.
     *
     * Una habilidad NUEVA solo entra si sus prerrequisitos ya se practicaron:
     * no tiene sentido pedir "comparar fracciones" antes de haber visto
     * "fracciones equivalentes".
     */
    fun plan(
        catalog: List<Skill>,
        states: Map<String, SkillState>,
        now: Long,
        count: Int,
        rnd: Random = Random
    ): List<PlannedExercise> {
        if (count <= 0 || catalog.isEmpty()) return emptyList()

        val vistas = catalog.filter { states[it.id] != null }
        val nuevas = catalog.filter { states[it.id] == null }

        // 1) REPASO: lo vencido, empezando por lo más atrasado.
        val repaso = vistas
            .filter { (states[it.id]?.dueAt ?: 0L) <= now }
            .sortedBy { states[it.id]?.dueAt ?: 0L }

        // 2) REFUERZO: lo que se falla, aunque todavía no venza.
        val refuerzo = vistas
            .filter {
                val s = states[it.id]!!
                s.mastery == Mastery.EN_PROGRESO && s.accuracy < WEAK_ACCURACY
            }
            .sortedBy { states[it.id]!!.accuracy }

        // 3) NUEVO: con prerrequisitos ya practicados.
        val frescas = nuevas.filter { skill ->
            skill.prerequisites.all { p -> states[p] != null }
        }

        val objetivo = listOf(
            Triple(repaso, Reason.REPASO, cuota(count, 0.6f)),
            Triple(refuerzo, Reason.REFUERZO, cuota(count, 0.3f)),
            Triple(frescas, Reason.NUEVO, count)   // el resto
        )

        val elegidas = LinkedHashMap<String, Reason>()
        for ((pool, reason, cupo) in objetivo) {
            var puestas = 0
            for (skill in pool) {
                if (elegidas.size >= count || puestas >= cupo) break
                if (elegidas.putIfAbsent(skill.id, reason) == null) puestas++
            }
        }

        // Relleno: si con los tres cubos no se llenó la sesión (por ejemplo al
        // principio, cuando casi nada está vencido), se completa con lo menos
        // practicado recientemente.
        if (elegidas.size < count) {
            val resto = catalog
                .filter { it.id !in elegidas }
                .sortedBy { states[it.id]?.lastPracticedAt ?: 0L }
            for (skill in resto) {
                if (elegidas.size >= count) break
                val r = if (states[skill.id] == null) Reason.NUEVO else Reason.REPASO
                elegidas[skill.id] = r
            }
        }

        val porId = catalog.associateBy { it.id }
        return elegidas.map { (id, reason) ->
            val skill = porId.getValue(id)
            PlannedExercise(id, pickFormat(skill, states[id], rnd), reason)
        }
    }

    private fun cuota(total: Int, fraccion: Float): Int =
        kotlin.math.max(1, kotlin.math.ceil(total * fraccion).toInt())

    // =================================================================
    //  RESUMEN PARA EL PANEL DE PADRES
    // =================================================================

    /** Cuántas habilidades hay en cada nivel de dominio, por tema. */
    fun masteryByTopic(
        catalog: List<Skill>,
        states: Map<String, SkillState>
    ): Map<String, Map<Mastery, Int>> =
        catalog.groupBy { it.topic }.mapValues { (_, skills) ->
            skills.groupingBy { states[it.id]?.mastery ?: Mastery.NUEVO }.eachCount()
        }

    /** Las habilidades que peor van, para mostrarlas al padre primero. */
    fun weakest(
        catalog: List<Skill>,
        states: Map<String, SkillState>,
        limit: Int = 5
    ): List<Pair<Skill, SkillState>> =
        catalog.mapNotNull { s -> states[s.id]?.let { s to it } }
            .filter { it.second.practices >= 2 && it.second.accuracy < WEAK_ACCURACY }
            .sortedBy { it.second.accuracy }
            .take(limit)
}
