package com.controlparental.kioscosuave.curriculum

import java.text.Normalizer
import kotlin.math.max
import kotlin.random.Random

/**
 * GUÍAS Y MATERIAL DE ESTUDIO
 * ===========================
 *
 * Kotlin puro, verificable fuera del dispositivo.
 *
 * El padre sube una guía; Trivium la convierte en PRIORIDAD, no en un
 * cuestionario. La guía dice QUÉ es importante ahora; el desempeño del niño
 * sigue diciendo CUÁNTO lo domina. Los dos datos se combinan, ninguno sustituye
 * al otro.
 *
 * DECISIÓN CLAVE: una guía no crea conocimientos nuevos si ya existen.
 * "Repasar tabla del 7" no genera un concepto paralelo: se engancha con la
 * habilidad que ya está en el temario y solo cambia su prioridad y su fecha de
 * repaso. Así la memoria del alumno se acumula en vez de fragmentarse.
 */

enum class GuideMode {
    /** Hay examen con fecha: la prioridad escala conforme se acerca. */
    EXAM_PREP,

    /** "Esto es lo que está viendo ahora": entra al repaso normal, sin urgencia. */
    LEARNING
}

/**
 * Material subido por un padre, ya procesado a habilidades del temario.
 *
 * [skillIds] son habilidades EXISTENTES con las que se emparejó el material.
 * [unmatchedTopics] son temas detectados que el temario todavía no cubre: se
 * conservan a propósito, para poder decirle al padre qué quedó fuera en vez de
 * fingir que se cubrió todo.
 */
data class StudyGuide(
    val id: String,
    val title: String,
    val subject: Subject = Subject.MATH,
    val mode: GuideMode,
    /** Fecha del examen en epoch ms. Solo aplica a EXAM_PREP. */
    val examAt: Long? = null,
    val skillIds: List<String> = emptyList(),
    val unmatchedTopics: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val paused: Boolean = false
)

/** Resultado de emparejar los temas de una guía con el temario existente. */
data class GuideMatch(
    val skillIds: List<String>,
    val unmatchedTopics: List<String>
)

object GuidePolicy {

    /**
     * Cuánto pesa una guía frente al repaso normal. Es un MULTIPLICADOR de
     * prioridad, no una orden: aunque haya examen, el resto del temario sigue
     * apareciendo (ver [plan]).
     */
    private const val BOOST_LEARNING = 1.6f
    private const val BOOST_BASE_EXAM = 2.0f

    /** Cupo máximo de la sesión que puede acaparar el material de guías. */
    private const val MAX_GUIDE_SHARE = 0.75f

    /**
     * Prioridad de una guía en este momento.
     *
     * En EXAM_PREP la urgencia escala al acercarse la fecha, siguiendo el plan
     * acordado: a 14 días introducir y medir, a 7 reforzar lo flojo, a 3
     * priorizar lo no dominado, la víspera repaso general.
     *
     * DESPUÉS DEL EXAMEN la guía NO desaparece: baja al peso de LEARNING y
     * sigue formando parte del repaso acumulativo. Lo estudiado no deja de
     * importar porque el examen ya pasó.
     */
    fun priority(guide: StudyGuide, now: Long): Float {
        if (guide.paused) return 0f
        if (guide.mode == GuideMode.LEARNING) return BOOST_LEARNING

        val examAt = guide.examAt ?: return BOOST_LEARNING
        val dias = (examAt - now).toFloat() / LearningMemory.DAY_MS

        return when {
            dias < 0f -> BOOST_LEARNING          // ya pasó: repaso acumulativo
            dias <= 1f -> BOOST_BASE_EXAM * 2.2f // víspera
            dias <= 3f -> BOOST_BASE_EXAM * 1.9f
            dias <= 7f -> BOOST_BASE_EXAM * 1.5f
            dias <= 14f -> BOOST_BASE_EXAM * 1.2f
            else -> BOOST_BASE_EXAM
        }
    }

    /** Días que faltan para el examen (negativo si ya pasó, null si no hay). */
    fun daysToExam(guide: StudyGuide, now: Long): Int? =
        guide.examAt?.let { ((it - now) / LearningMemory.DAY_MS).toInt() }

    // =================================================================
    //  EMPAREJAR EL MATERIAL CON EL TEMARIO
    // =================================================================

    /** Palabras vacías que no aportan al emparejamiento. */
    private val STOP = setOf(
        "de", "del", "la", "el", "los", "las", "y", "o", "en", "con", "un", "una",
        "para", "por", "al", "a", "que", "se", "su", "sus", "resolver", "repasar",
        "estudiar", "identificar", "problemas", "ejercicios", "tema", "temas"
    )

    private fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")

    private fun tokens(s: String): Set<String> =
        normalize(s).split(Regex("[^a-z0-9]+"))
            .filter { it.length > 2 && it !in STOP }
            .toSet()

    /**
     * Empareja los temas detectados en la guía con habilidades EXISTENTES.
     *
     * Puntúa por solapamiento de palabras contra la etiqueta y el tema de cada
     * habilidad. Se exige coincidencia real (al menos dos palabras, o una muy
     * distintiva) para no enganchar cualquier cosa: preferimos reportar un tema
     * como no cubierto antes que asignarlo a la habilidad equivocada y ponerse
     * a ejercitar algo que el niño no tiene que estudiar.
     */
    fun match(extractedTopics: List<String>, catalog: List<Skill>): GuideMatch {
        val matched = LinkedHashSet<String>()
        val unmatched = mutableListOf<String>()

        val index = catalog.map { it to (tokens(it.label) + tokens(it.topic)) }

        for (topic in extractedTopics) {
            val t = tokens(topic)
            if (t.isEmpty()) { unmatched += topic; continue }

            val scored = index.map { (skill, words) -> skill to t.count { it in words } }
            val best = scored.maxByOrNull { it.second }

            if (best != null && best.second >= 2) {
                matched += best.first.id
                // Habilidades igual de buenas también entran: "fracciones"
                // legítimamente toca varias.
                scored.filter { it.second == best.second }.forEach { matched += it.first.id }
            } else if (best != null && best.second == 1 && t.size == 1) {
                matched += best.first.id
            } else {
                unmatched += topic
            }
        }
        return GuideMatch(matched.toList(), unmatched.distinct())
    }

    // =================================================================
    //  PLANIFICAR CON GUÍAS
    // =================================================================

    /**
     * Reto del día combinando el temario normal con las guías activas.
     *
     * Las guías SUBEN la prioridad de sus habilidades, no monopolizan la
     * sesión: [MAX_GUIDE_SHARE] reserva siempre una parte para el repaso
     * acumulativo. Abandonar lo ya dominado para volcarse en el examen es
     * justamente cómo se olvida lo aprendido.
     *
     * Dentro del material de la guía se atiende primero lo que peor va: una
     * habilidad al 58% pesa mucho más que una al 92%, que solo necesita un
     * repaso de mantenimiento.
     */
    fun plan(
        catalog: List<Skill>,
        states: Map<String, SkillState>,
        guides: List<StudyGuide>,
        now: Long,
        count: Int,
        rnd: Random = Random
    ): List<PlannedExercise> {
        val activas = guides.filter { !it.paused }
        if (activas.isEmpty() || count <= 0) {
            return LearningMemory.plan(catalog, states, now, count, rnd)
        }

        // Prioridad acumulada por habilidad: si varias guías la mencionan, pesa más.
        val peso = HashMap<String, Float>()
        for (g in activas) {
            val p = priority(g, now)
            for (id in g.skillIds) peso[id] = (peso[id] ?: 0f) + p
        }

        val cupoGuia = max(1, (count * MAX_GUIDE_SHARE).toInt())
        val porId = catalog.associateBy { it.id }

        // Orden dentro de la guía: primero lo urgente Y flojo.
        val deGuia = peso.keys
            .mapNotNull { porId[it] }
            .sortedByDescending { skill ->
                val st = states[skill.id]
                val dominio = st?.accuracy ?: 0f
                val vencido = if (st == null || st.dueAt <= now) 1f else 0f
                // Peso de guía × (1 - dominio) prioriza lo flojo, y lo vencido
                // sube. Una habilidad nueva de la guía (dominio 0) entra alto:
                // aparecer en una guía NO implica dominarla, hay que medirla.
                (peso[skill.id] ?: 0f) * (1.3f - dominio) + vencido
            }
            .take(cupoGuia)

        val elegidas = LinkedHashMap<String, Reason>()
        for (skill in deGuia) {
            val st = states[skill.id]
            elegidas[skill.id] = when {
                st == null -> Reason.NUEVO
                st.accuracy < 0.7f -> Reason.REFUERZO
                else -> Reason.REPASO
            }
        }

        // El resto de la sesión sigue la política normal: repaso acumulativo,
        // para no abandonar lo que ya se domina mientras dura la guía.
        if (elegidas.size < count) {
            val faltan = count - elegidas.size
            val normal = LearningMemory.plan(catalog, states, now, faltan + elegidas.size, rnd)
            for (p in normal) {
                if (elegidas.size >= count) break
                elegidas.putIfAbsent(p.skillId, p.reason)
            }
        }

        return elegidas.map { (id, reason) ->
            val skill = porId.getValue(id)
            PlannedExercise(id, LearningMemory.pickFormat(skill, states[id], rnd), reason)
        }
    }

    // =================================================================
    //  RESUMEN PARA EL PADRE
    // =================================================================

    /** Dominio por habilidad de una guía, para la tarjeta del panel. */
    fun progress(guide: StudyGuide, catalog: List<Skill>, states: Map<String, SkillState>):
        List<Triple<Skill, Float, Mastery>> {
        val porId = catalog.associateBy { it.id }
        return guide.skillIds.mapNotNull { porId[it] }.map { skill ->
            val st = states[skill.id]
            Triple(skill, st?.accuracy ?: 0f, st?.mastery ?: Mastery.NUEVO)
        }
    }

    /**
     * Frase en lenguaje llano para el panel. El padre no debería tener que
     * interpretar porcentajes.
     */
    fun summary(
        childName: String,
        guide: StudyGuide,
        catalog: List<Skill>,
        states: Map<String, SkillState>,
        now: Long
    ): String {
        val avance = progress(guide, catalog, states)
        if (avance.isEmpty()) return "Todavía no hay práctica registrada de «${guide.title}»."

        val practicadas = avance.filter { states[it.first.id]?.practices ?: 0 > 0 }
        if (practicadas.isEmpty()) {
            return "$childName aún no empieza «${guide.title}». Trivium lo irá incluyendo en sus retos diarios."
        }

        val flojo = practicadas.minByOrNull { it.second }!!
        val pct = (flojo.second * 100).toInt()
        val dias = daysToExam(guide, now)

        val cuando = when {
            dias == null -> ""
            dias < 0 -> " El examen ya pasó, pero el tema sigue en su repaso."
            dias == 0 -> " El examen es hoy."
            dias == 1 -> " El examen es mañana."
            else -> " Faltan $dias días para el examen."
        }

        return "$childName necesita reforzar ${flojo.first.label.lowercase()} " +
            "(va en $pct%). Trivium seguirá trabajando ese tema y lo volverá a evaluar.$cuando"
    }
}
