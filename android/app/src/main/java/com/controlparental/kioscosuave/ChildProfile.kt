package com.controlparental.kioscosuave

/**
 * Nivel escolar del menor. Determina automáticamente la dificultad y las metas
 * de los retos (decisión: "dificultad automática por nivel escolar").
 */
enum class GradeLevel(val label: String) {
    PRIMARIA("Primaria"),
    SECUNDARIA("Secundaria");

    companion object {
        fun fromName(name: String?): GradeLevel =
            entries.firstOrNull { it.name == name } ?: PRIMARIA
    }
}

enum class Difficulty { EASY, MEDIUM, HARD }

/**
 * Configuración de retos derivada del nivel escolar.
 * Mapeo v1: Primaria -> fácil y pocas repeticiones; Secundaria -> difícil.
 * Un mismo dispositivo = un solo niño (una tablet por niño).
 */
/**
 * Cada etapa se aprueba con PRECISIÓN >= 80% (aciertos ÷ intentos) tras un
 * mínimo de preguntas. Fallar penaliza (una sola oportunidad por pregunta),
 * lo que evita adivinar presionando opciones al azar.
 */
data class ChallengeConfig(
    val difficulty: Difficulty,
    val mathTotal: Int,    // mínimo de preguntas de matemáticas
    val englishTotal: Int  // mínimo de preguntas de inglés
)

data class ChildProfile(
    val name: String,
    val grade: GradeLevel
) {
    val config: ChallengeConfig
        get() = when (grade) {
            GradeLevel.PRIMARIA -> ChallengeConfig(
                difficulty = Difficulty.EASY,
                mathTotal = 5,
                englishTotal = 3
            )
            GradeLevel.SECUNDARIA -> ChallengeConfig(
                difficulty = Difficulty.HARD,
                mathTotal = 10,
                englishTotal = 5
            )
        }
}
