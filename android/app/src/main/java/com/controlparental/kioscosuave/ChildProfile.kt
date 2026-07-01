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
data class ChallengeConfig(
    val difficulty: Difficulty,
    val mathTotal: Int,           // tamaño del bloque de matemáticas
    val mathRequiredCorrect: Int, // aciertos necesarios para aprobar (≈ 80% del bloque)
    val englishTarget: Int,
    val readingTarget: Int
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
                mathRequiredCorrect = 4, // 80% de 5
                englishTarget = 2,
                readingTarget = 1
            )
            GradeLevel.SECUNDARIA -> ChallengeConfig(
                difficulty = Difficulty.HARD,
                mathTotal = 10,
                mathRequiredCorrect = 8, // 80% de 10
                englishTarget = 3,
                readingTarget = 1
            )
        }
}
