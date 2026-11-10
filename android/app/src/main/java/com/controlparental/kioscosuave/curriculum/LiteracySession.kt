package com.controlparental.kioscosuave.curriculum

import kotlin.random.Random

/**
 * Sesión de alfabetización: decide QUÉ actividad toca según la memoria de
 * aprendizaje, sin tocar las reglas del planificador.
 *
 * Es un ADAPTADOR delante de [LearningMemory]: filtra qué habilidades puede ver
 * el planificador y deja que él ordene (repaso vencido, refuerzo, nuevo). Hace
 * falta porque el planificador solo exige que un prerrequisito se haya
 * PRACTICADO, y su relleno puede traer cualquier habilidad del catálogo; para
 * aprender a leer eso no basta: no se pasa de sílabas a cuentos por haber
 * visto una sílaba, sino por haberlas dominado.
 *
 * Kotlin puro (sin Android) para poder simularla en tools/harness.
 */
class LiteracySession(
    initialStates: Map<String, SkillState>,
    recentIds: List<String> = emptyList(),
    private val now: () -> Long = { System.currentTimeMillis() },
    private val rnd: Random = Random,
    private val bank: List<LiteracyActivity> = LiteracyCatalog.activities
) {
    private val states: MutableMap<String, SkillState> = HashMap(initialStates)
    private val recent: ArrayDeque<String> = ArrayDeque(recentIds.takeLast(RECENT))
    private val queue: ArrayDeque<String> = ArrayDeque()

    /** Memoria completa (incluye las habilidades de otras materias, intactas). */
    fun snapshot(): Map<String, SkillState> = states.toMap()

    fun stateOf(skillId: String): SkillState? = states[skillId]

    fun recentIds(): List<String> = recent.toList()

    /**
     * Habilidades cuyo paso previo ya está asentado. La primera siempre lo está.
     *
     * Lo ya abierto NO se vuelve a cerrar: fallar un repaso de sílabas hace que
     * las sílabas vuelvan antes, pero no le quita al niño los cuentos que ya
     * estaba leyendo.
     */
    fun unlocked(): List<Skill> =
        LiteracyCatalog.skills.filter { s ->
            states[s.id] != null || s.prerequisites.all { ready(states[it]) }
        }

    /**
     * Mientras una habilidad de lectura es nueva se permite ESCUCHAR el texto;
     * después solo se lee. Así el audio acompaña al principio y se retira solo,
     * sin que nadie tenga que configurarlo.
     */
    fun listeningSupport(skillId: String): Boolean = !ready(states[skillId])

    fun next(): LiteracyActivity {
        if (queue.isEmpty()) refill()
        val skillId = queue.removeFirstOrNull() ?: LiteracyCatalog.TWO
        val pool = bank.filter { it.skillId == skillId }.ifEmpty { bank }
        // Cuanto más grande el banco, más se tarda en repetir una actividad.
        val avoid = recent.takeLast((pool.size - 1).coerceIn(0, RECENT)).toSet()
        val pick = pool.filter { it.id !in avoid }.ifEmpty { pool }.random(rnd)
        remember(pick.id)
        return present(pick)
    }

    /** Recupera la actividad que estaba en pantalla, con sus fichas en el mismo orden. */
    fun restore(id: String, choices: List<String>): LiteracyActivity? {
        val original = bank.firstOrNull { it.id == id } ?: return null
        // Si el banco cambió en una actualización, el orden guardado ya no vale.
        if (choices.sorted() != original.choices.sorted()) return null
        if (original.skillId !in unlocked().map { it.id }) return null
        return original.copy(choices = choices)
    }

    /** Registra el resultado CONFIRMADO (no cada toque) y devuelve el estado nuevo. */
    fun record(activity: LiteracyActivity, correct: Boolean, wrongChoice: String? = null): SkillState {
        val prev = states[activity.skillId] ?: SkillState(activity.skillId)
        val updated = LearningMemory.review(prev, correct, activity.format, wrongChoice, now())
        states[activity.skillId] = updated
        // Tras un fallo (o al desbloquearse algo nuevo) se replanifica.
        queue.clear()
        return updated
    }

    private fun remember(id: String) {
        recent.remove(id)
        recent.addLast(id)
        while (recent.size > RECENT) recent.removeFirst()
    }

    private fun present(a: LiteracyActivity): LiteracyActivity {
        var order = a.choices.shuffled(rnd)
        // Las fichas no deben salir ya ordenadas: se resolvería sin leer.
        if (a.buildsWord) repeat(6) {
            if (order.take(a.syllables.size) == a.syllables) order = a.choices.shuffled(rnd)
        }
        return a.copy(choices = order)
    }

    private fun refill() {
        val open = unlocked()
        val t = now()
        // Lo que de verdad toca: lo vencido, lo que se falla y la frontera
        // (habilidades abiertas que aún no están asentadas).
        val needed = open.filter { s ->
            val st = states[s.id]
            st == null || !ready(st) || st.dueAt <= t
        }
        // Si todo está asentado y nada vence hoy, se repasa lo más avanzado
        // en vez de volver a las sílabas.
        val catalog = needed.ifEmpty { open.takeLast(3) }
        queue.addAll(LearningMemory.plan(catalog, states, t, BATCH, rnd).map { it.skillId })
    }

    companion object {
        private const val RECENT = 12
        private const val BATCH = 4

        /** Práctica mínima antes de dar algo por asentado. */
        const val READY_PRACTICES = 5
        const val READY_STREAK = 3
        const val READY_ACCURACY = 0.8f

        /**
         * "Asentado": suficiente práctica, racha reciente y precisión global.
         * Usa solo datos que la memoria ya guarda, así que no cambia el motor.
         */
        fun ready(s: SkillState?): Boolean =
            s != null && s.practices >= READY_PRACTICES &&
                s.streak >= READY_STREAK && s.accuracy >= READY_ACCURACY
    }
}
