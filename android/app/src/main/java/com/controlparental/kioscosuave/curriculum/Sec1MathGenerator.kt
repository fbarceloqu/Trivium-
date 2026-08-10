package com.controlparental.kioscosuave.curriculum

import com.controlparental.kioscosuave.MathQuestion

/**
 * Punto de entrada del temario de 1° de secundaria: dado una habilidad (y
 * opcionalmente un formato), produce un ejercicio.
 *
 * Todo lo que sale de aquí es DETERMINISTA y verificable en el dispositivo: la
 * respuesta correcta la calcula el programa, no un modelo de lenguaje. Cuando
 * llegue la Fase 3, la IA se encargará de la NARRATIVA del problema (el
 * contexto, los nombres, la situación) pero los números y la clave de respuesta
 * seguirán saliendo de aquí. Así ningún niño practica con una respuesta
 * inventada.
 *
 * [generate] devuelve null si la habilidad todavía no tiene generador: el
 * temario completo está declarado en [Curriculum] desde el principio, y los
 * generadores se van llenando por tema. Consulta [implementedIds] para saber
 * qué está listo.
 */
object Sec1MathGenerator {

    /** Habilidades que YA tienen generador. Se amplía conforme avanza la Fase 1. */
    val implementedIds: Set<String> = setOf(
        // Porcentajes (examen de febrero)
        Curriculum.PCT_DE_CANTIDAD.id,
        Curriculum.PCT_INVERSO.id,
        Curriculum.PCT_COMPLEMENTO.id,
        Curriculum.PCT_DESCUENTO.id,
        Curriculum.PCT_AUMENTO.id,
        Curriculum.PCT_VARIACION.id,
        // Estadística (exámenes de noviembre y marzo)
        Curriculum.EST_IDENTIFICAR.id,
        Curriculum.EST_MEDIA.id,
        Curriculum.EST_MODA.id,
        Curriculum.EST_MEDIANA.id,
        Curriculum.EST_RANGO.id,
        Curriculum.EST_GRAFICAS.id,
        // Ángulos (examen de noviembre)
        Curriculum.ANG_CLASIFICAR.id,
        Curriculum.ANG_COMPLEMENTARIO.id,
        Curriculum.ANG_SUPLEMENTARIO.id
    )

    val implemented: List<Skill> = Curriculum.sec1Math.filter { it.id in implementedIds }

    /** Habilidades declaradas en el temario a las que aún les falta generador. */
    val pending: List<Skill> = Curriculum.sec1Math.filter { it.id !in implementedIds }

    /**
     * Genera un ejercicio de [skill]. Si [format] es null o no aplica a esa
     * habilidad, se elige uno de los formatos declarados en el temario.
     */
    fun generate(skill: Skill, format: ExerciseFormat? = null): MathQuestion? {
        val f = format?.takeIf { it in skill.formats } ?: skill.formats.random()
        return when (skill.id) {
            Curriculum.PCT_DE_CANTIDAD.id -> Sec1Porcentajes.deCantidad(f)
            Curriculum.PCT_INVERSO.id -> Sec1Porcentajes.inverso(f)
            Curriculum.PCT_COMPLEMENTO.id -> Sec1Porcentajes.complemento(f)
            Curriculum.PCT_DESCUENTO.id -> Sec1Porcentajes.descuento(f)
            Curriculum.PCT_AUMENTO.id -> Sec1Porcentajes.aumento(f)
            Curriculum.PCT_VARIACION.id -> Sec1Porcentajes.variacion(f)

            Curriculum.EST_IDENTIFICAR.id -> Sec1Estadistica.identificar(f)
            Curriculum.EST_MEDIA.id -> Sec1Estadistica.media(f)
            Curriculum.EST_MODA.id -> Sec1Estadistica.moda(f)
            Curriculum.EST_MEDIANA.id -> Sec1Estadistica.mediana(f)
            Curriculum.EST_RANGO.id -> Sec1Estadistica.rango(f)
            Curriculum.EST_GRAFICAS.id -> Sec1Estadistica.graficas(f)

            Curriculum.ANG_CLASIFICAR.id -> Sec1Angulos.clasificar(f)
            Curriculum.ANG_COMPLEMENTARIO.id -> Sec1Angulos.complementario(f)
            Curriculum.ANG_SUPLEMENTARIO.id -> Sec1Angulos.suplementario(f)

            else -> null
        }
    }

    fun generate(skillId: String, format: ExerciseFormat? = null): MathQuestion? =
        Curriculum.byId(skillId)?.let { generate(it, format) }

    /**
     * Un ejercicio al azar del temario implementado.
     *
     * PROVISIONAL: mientras no exista la memoria (Fase 2) la elección es
     * uniforme. En cuanto la memoria esté, esta llamada la sustituye el
     * planificador, que escoge según lo que el alumno necesita repasar.
     */
    fun randomExercise(): MathQuestion? {
        if (implemented.isEmpty()) return null
        return generate(implemented.random())
    }
}
