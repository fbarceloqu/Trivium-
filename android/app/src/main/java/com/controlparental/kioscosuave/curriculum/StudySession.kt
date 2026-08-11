package com.controlparental.kioscosuave.curriculum

import com.controlparental.kioscosuave.MathQuestion
import kotlin.random.Random

/**
 * Una sesión de estudio: convierte el plan de [LearningMemory] en la secuencia
 * concreta de ejercicios que ve el niño, y va registrando lo que responde.
 *
 * Kotlin puro (sin Android) para poder simularla. La persistencia la aporta
 * quien la crea, pasando [states] y guardando [snapshot] al terminar.
 *
 * La cola se rellena sola: cuando se agota se vuelve a planificar con el estado
 * YA ACTUALIZADO, así una habilidad fallada hace un momento puede reaparecer en
 * la misma sesión —con otro formato— tal como pide la política de repaso.
 */
class StudySession(
    private val catalog: List<Skill>,
    initialStates: Map<String, SkillState>,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val batchSize: Int = 6,
    private val rnd: Random = Random
) {
    private val states: MutableMap<String, SkillState> = HashMap(initialStates)
    private val queue: ArrayDeque<PlannedExercise> = ArrayDeque()

    /** Estado actual de la memoria, para persistirlo. */
    fun snapshot(): Map<String, SkillState> = states.toMap()

    fun stateOf(skillId: String): SkillState? = states[skillId]

    /** Siguiente ejercicio planificado. Nunca devuelve null si hay temario. */
    fun nextPlanned(): PlannedExercise? {
        if (queue.isEmpty()) refill()
        return queue.removeFirstOrNull()
    }

    /**
     * Siguiente ejercicio ya generado. Si la habilidad elegida todavía no tiene
     * generador, se salta y se intenta con la siguiente (hasta agotar la cola),
     * de modo que declarar una habilidad antes de escribir su generador nunca
     * rompe la sesión.
     */
    fun nextQuestion(): MathQuestion? {
        repeat(catalog.size.coerceAtLeast(1)) {
            val planned = nextPlanned() ?: return null
            val q = Sec1MathGenerator.generate(planned.skillId, planned.format)
            if (q != null) return q
        }
        return null
    }

    /** Registra la respuesta y devuelve el estado actualizado de esa habilidad. */
    fun record(
        skillId: String,
        format: ExerciseFormat,
        correct: Boolean,
        wrongChoice: String? = null
    ): SkillState {
        val prev = states[skillId] ?: SkillState(skillId)
        val updated = LearningMemory.review(prev, correct, format, wrongChoice, now())
        states[skillId] = updated
        // Si se falló, la habilidad queda vencida: se replanifica para que
        // pueda volver dentro de esta misma sesión.
        if (!correct) queue.clear()
        return updated
    }

    private fun refill() {
        queue.addAll(LearningMemory.plan(catalog, states, now(), batchSize, rnd))
    }
}
