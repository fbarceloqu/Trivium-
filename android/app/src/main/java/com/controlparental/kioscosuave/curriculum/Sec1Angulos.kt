package com.controlparental.kioscosuave.curriculum

import com.controlparental.kioscosuave.MathQuestion
import com.controlparental.kioscosuave.WorkedExample

/**
 * TEMA · ÁNGULOS  (examen de noviembre)
 *
 * El error observado no fue de cálculo: complementario y suplementario están
 * INTERCAMBIADOS. Al pedir el complementario de 85° la respuesta dada fue 75°
 * y en otro reactivo se respondió 360° a "¿cuánto suman dos complementarios?".
 *
 * Por eso el distractor principal de cada generador es la respuesta del OTRO
 * concepto: si en el complementario de 85° aparece 95° como opción, elegir mal
 * revela exactamente la confusión (y la Fase 2 podrá registrarla).
 *
 * Todos los ejercicios incluyen la misma regla mnemotécnica en la ayuda, para
 * que la asociación se refuerce cada vez.
 */
internal object Sec1Angulos {

    private const val MNEMOTECNIA =
        "Regla: Complementario va con 90° y Suplementario con 180°. " +
            "La C va antes que la S en el alfabeto, y 90 va antes que 180."

    private val AGUDO = "Agudo"
    private val RECTO = "Recto"
    private val OBTUSO = "Obtuso"
    private val LLANO = "Llano"
    private val COMPLETO = "Completo"

    // ------------------------------------------------------------------
    // Clasificar ángulos
    // ------------------------------------------------------------------
    private val EX_CLASIFICAR = WorkedExample(
        "Ejemplo: tipos de ángulo",
        listOf(
            "AGUDO   → menos de 90°   (como la punta de una rebanada de pizza)",
            "RECTO   → exactamente 90° (la esquina de una hoja)",
            "OBTUSO  → entre 90° y 180° (más abierto que una esquina)",
            "LLANO   → exactamente 180° (una línea recta)",
            "COMPLETO→ exactamente 360° (una vuelta entera)",
            "Un ángulo de 47° es AGUDO porque 47 es menor que 90."
        )
    )

    fun clasificar(format: ExerciseFormat): MathQuestion {
        if (format == ExerciseFormat.CONCEPTUAL) {
            val (question, answer) = listOf(
                "¿Cuánto mide un ángulo agudo?" to "Menos de 90°",
                "¿Cuánto mide un ángulo recto?" to "Exactamente 90°",
                "¿Cuánto mide un ángulo obtuso?" to "Más de 90° y menos de 180°",
                "¿Cuánto mide un ángulo llano?" to "Exactamente 180°"
            ).random()
            return MathQuestion(
                question = question,
                options = listOf(
                    "Menos de 90°", "Exactamente 90°",
                    "Más de 90° y menos de 180°", "Exactamente 180°"
                ).shuffled(),
                answer = answer,
                steps = listOf(
                    "Agudo < 90°  |  Recto = 90°  |  Obtuso entre 90° y 180°  |  Llano = 180°"
                ),
                example = EX_CLASIFICAR,
                skillId = Curriculum.ANG_CLASIFICAR.id,
                format = format
            )
        }

        // DIRECTO: se da la medida y hay que nombrar el tipo.
        val medida = listOf(
            listOf(15, 30, 45, 47, 60, 75, 89).random(),  // agudo
            90,
            listOf(100, 120, 135, 150, 170).random(),     // obtuso
            180,
            360
        ).random()

        val answer = when {
            medida < 90 -> AGUDO
            medida == 90 -> RECTO
            medida < 180 -> OBTUSO
            medida == 180 -> LLANO
            else -> COMPLETO
        }

        return MathQuestion(
            question = "Un ángulo mide $medida°. ¿Cómo se llama ese tipo de ángulo?",
            options = listOf(AGUDO, RECTO, OBTUSO, LLANO, COMPLETO).shuffled().let { all ->
                // Se asegura que la correcta esté entre las 4 mostradas.
                (listOf(answer) + all.filter { it != answer }).take(4).shuffled()
            },
            answer = answer,
            steps = listOf(
                "Compara $medida° con los valores clave: 90°, 180° y 360°",
                when (answer) {
                    AGUDO -> "$medida° es menor que 90°, así que es AGUDO"
                    RECTO -> "$medida° es exactamente 90°, así que es RECTO"
                    OBTUSO -> "$medida° está entre 90° y 180°, así que es OBTUSO"
                    LLANO -> "$medida° es exactamente 180°, así que es LLANO"
                    else -> "$medida° es una vuelta completa, así que es COMPLETO"
                }
            ),
            example = EX_CLASIFICAR,
            skillId = Curriculum.ANG_CLASIFICAR.id,
            format = format
        )
    }

    // ------------------------------------------------------------------
    // Complementarios (90°)
    // ------------------------------------------------------------------
    private val EX_COMPLEMENTARIO = WorkedExample(
        "Ejemplo: ángulo complementario",
        listOf(
            "¿Cuánto mide el complementario de 30°?",
            "1) Dos complementarios SUMAN 90°",
            "2) Se resta: 90 − 30 = 60",
            "Respuesta: 60°  (comprueba: 30 + 60 = 90 ✓)",
            MNEMOTECNIA
        )
    )

    fun complementario(format: ExerciseFormat): MathQuestion {
        if (format == ExerciseFormat.CONCEPTUAL) {
            return MathQuestion(
                question = "¿Cuánto suman dos ángulos complementarios?",
                options = Gen.numOptsSuffix("°", 90.0, 180.0, 360.0, 45.0),
                answer = "90°",
                steps = listOf(MNEMOTECNIA),
                example = EX_COMPLEMENTARIO,
                skillId = Curriculum.ANG_COMPLEMENTARIO.id,
                format = format
            )
        }

        val a = listOf(5, 15, 25, 30, 35, 40, 55, 60, 70, 85).random()
        val ans = 90 - a
        val esInverso = format == ExerciseFormat.INVERSO

        val question = if (esInverso)
            "El complementario de un ángulo mide $ans°. ¿Cuánto mide el ángulo original?"
        else
            "¿Cuánto mide el ángulo complementario del ángulo de $a°?"

        // Los dos generadores son simétricos: el alumno ve un ángulo y debe dar
        // el otro. Los distractores se construyen sobre el ángulo QUE VE, no
        // sobre el que debe calcular; si no, "repetir el dato" coincidiría con
        // la respuesta correcta en el formato inverso.
        val dado = if (esInverso) ans else a
        val correcta = (if (esInverso) a else ans).toDouble()

        return MathQuestion(
            question = question,
            options = Gen.numOptsSuffix(
                "°",
                correcta,
                180.0 - dado,            // error clave: usar 180 (suplementario)
                dado.toDouble(),         // error: repetir el ángulo que ya te dieron
                90.0 + dado              // error: sumar en vez de restar
            ),
            answer = "${Gen.fmt(correcta)}°",
            steps = listOf(
                "Dos ángulos complementarios suman 90°",
                if (format == ExerciseFormat.INVERSO)
                    "90 − $ans = ${Gen.fmt(correcta)}"
                else
                    "90 − $a = $ans",
                "Comprueba: $a + $ans = 90 ✓",
                MNEMOTECNIA
            ),
            example = EX_COMPLEMENTARIO,
            skillId = Curriculum.ANG_COMPLEMENTARIO.id,
            format = format
        )
    }

    // ------------------------------------------------------------------
    // Suplementarios (180°)
    // ------------------------------------------------------------------
    private val EX_SUPLEMENTARIO = WorkedExample(
        "Ejemplo: ángulo suplementario",
        listOf(
            "¿Cuánto mide el suplementario de 60°?",
            "1) Dos suplementarios SUMAN 180°",
            "2) Se resta: 180 − 60 = 120",
            "Respuesta: 120°  (comprueba: 60 + 120 = 180 ✓)",
            MNEMOTECNIA
        )
    )

    fun suplementario(format: ExerciseFormat): MathQuestion {
        if (format == ExerciseFormat.CONCEPTUAL) {
            return MathQuestion(
                question = "¿Cuánto suman dos ángulos suplementarios?",
                options = Gen.numOptsSuffix("°", 180.0, 90.0, 360.0, 270.0),
                answer = "180°",
                steps = listOf(MNEMOTECNIA),
                example = EX_SUPLEMENTARIO,
                skillId = Curriculum.ANG_SUPLEMENTARIO.id,
                format = format
            )
        }

        // Se excluyen 45° y 90° a propósito: con esos valores un distractor
        // coincidiría con la respuesta correcta y el ejercicio se resolvería
        // por descarte.
        val a = listOf(30, 60, 75, 105, 120, 135, 150).random()
        val ans = 180 - a
        val esInverso = format == ExerciseFormat.INVERSO

        val question = if (esInverso)
            "El suplementario de un ángulo mide $ans°. ¿Cuánto mide el ángulo original?"
        else
            "¿Cuánto mide el ángulo suplementario del ángulo de $a°?"

        val dado = if (esInverso) ans else a
        val correcta = (if (esInverso) a else ans).toDouble()
        // Si el ángulo pasa de 90° su complementario sería negativo, así que el
        // distractor "usar 90" no aplica y se sustituye por otro error común.
        val otro = if (dado < 90) 90.0 - dado else 360.0 - dado

        return MathQuestion(
            question = question,
            options = Gen.numOptsSuffix(
                "°",
                correcta,
                otro,                    // error clave: usar 90 (complementario)
                dado.toDouble(),         // error: repetir el ángulo que ya te dieron
                180.0 + dado             // error: sumar en vez de restar
            ),
            answer = "${Gen.fmt(correcta)}°",
            steps = listOf(
                "Dos ángulos suplementarios suman 180°",
                if (format == ExerciseFormat.INVERSO)
                    "180 − $ans = ${Gen.fmt(correcta)}"
                else
                    "180 − $a = $ans",
                "Comprueba: $a + $ans = 180 ✓",
                MNEMOTECNIA
            ),
            example = EX_SUPLEMENTARIO,
            skillId = Curriculum.ANG_SUPLEMENTARIO.id,
            format = format
        )
    }
}
