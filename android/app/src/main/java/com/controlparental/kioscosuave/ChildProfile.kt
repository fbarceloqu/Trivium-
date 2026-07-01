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
 * Cada etapa se aprueba con PRECISIÓN >= 80% medida sobre una VENTANA MÓVIL de
 * las últimas N respuestas (no sobre todo el historial). Así el niño puede
 * remontar respondiendo bien, pero quien adivina al azar nunca llega al 80%.
 * Una sola oportunidad por pregunta: fallar cuenta dentro de la ventana.
 */
data class ChallengeConfig(
    val difficulty: Difficulty,
    val mathWindow: Int,    // ventana de las últimas N respuestas de mate
    val englishWindow: Int  // ventana de las últimas N respuestas de inglés
)

data class ChildProfile(
    val name: String,
    val grade: GradeLevel
) {
    val config: ChallengeConfig
        get() = when (grade) {
            GradeLevel.PRIMARIA -> ChallengeConfig(
                difficulty = Difficulty.EASY,
                mathWindow = 5,   // 4 de las últimas 5 (80%)
                englishWindow = 5
            )
            GradeLevel.SECUNDARIA -> ChallengeConfig(
                difficulty = Difficulty.HARD,
                mathWindow = 10,  // 8 de las últimas 10 (80%)
                englishWindow = 5
            )
        }
}
