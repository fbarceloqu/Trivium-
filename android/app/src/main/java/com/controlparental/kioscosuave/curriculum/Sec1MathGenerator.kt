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

    /**
     * Habilidades que YA tienen generador. Con la Fase 1 completa esto cubre
     * los 13 temas del temario, pero se conserva el mecanismo por si en el
     * futuro se declaran habilidades antes de escribir su generador.
     */
    val implementedIds: Set<String> = setOf(
        // Números y fracciones (examen de septiembre)
        Curriculum.NUM_TIPOS.id,
        Curriculum.NUM_FINITO_INFINITO.id,
        Curriculum.NUM_PRIMOS.id,
        Curriculum.NUM_POTENCIAS.id,
        Curriculum.FRAC_PROPIA.id,
        Curriculum.FRAC_EQUIVALENTES.id,
        Curriculum.FRAC_COMPARAR.id,
        Curriculum.FRAC_A_DECIMAL.id,
        Curriculum.FRAC_DE_CANTIDAD.id,
        // Decimales (examen diagnóstico)
        Curriculum.DEC_OPERACIONES.id,
        Curriculum.DEC_PROBLEMAS.id,
        Curriculum.DEC_EQUIVALENCIAS.id,
        // Números con signo (examen diagnóstico)
        Curriculum.ENT_SUMA_RESTA.id,
        Curriculum.ENT_REPRESENTAR.id,
        // Proporcionalidad (examen diagnóstico)
        Curriculum.PROP_REGLA_TRES.id,
        Curriculum.PROP_VALOR_UNITARIO.id,
        // Porcentajes (examen de febrero)
        Curriculum.PCT_DE_CANTIDAD.id,
        Curriculum.PCT_INVERSO.id,
        Curriculum.PCT_COMPLEMENTO.id,
        Curriculum.PCT_DESCUENTO.id,
        Curriculum.PCT_AUMENTO.id,
        Curriculum.PCT_VARIACION.id,
        // Sucesiones y patrones (examen de marzo)
        Curriculum.SUC_SIGUIENTE.id,
        Curriculum.SUC_REGLA_GENERAL.id,
        Curriculum.SUC_EVALUAR_REGLA.id,
        Curriculum.SUC_FIGURAL.id,
        // Ecuaciones
        Curriculum.EC_LINEAL.id,
        Curriculum.EC_PROBLEMA.id,
        // Ángulos (examen de noviembre)
        Curriculum.ANG_CLASIFICAR.id,
        Curriculum.ANG_COMPLEMENTARIO.id,
        Curriculum.ANG_SUPLEMENTARIO.id,
        // Triángulos y figuras (noviembre + diagnóstico)
        Curriculum.TRI_CLASIFICAR.id,
        Curriculum.TRI_PERIMETRO.id,
        Curriculum.FIG_AREA_PERIMETRO.id,
        // Círculo (examen diagnóstico)
        Curriculum.CIR_ELEMENTOS.id,
        // Plano cartesiano (examen de noviembre)
        Curriculum.PLANO_DISTANCIA.id,
        // Estadística (exámenes de noviembre y marzo)
        Curriculum.EST_IDENTIFICAR.id,
        Curriculum.EST_MEDIA.id,
        Curriculum.EST_MODA.id,
        Curriculum.EST_MEDIANA.id,
        Curriculum.EST_RANGO.id,
        Curriculum.EST_GRAFICAS.id,
        // Probabilidad (examen de junio)
        Curriculum.PROB_ESPACIO.id,
        Curriculum.PROB_SIMPLE.id,
        Curriculum.PROB_TIPOS.id,
        Curriculum.PROB_FACTORIAL.id,
        Curriculum.PROB_ARBOL.id
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
            Curriculum.NUM_TIPOS.id -> Sec1Numeros.tipos(f)
            Curriculum.NUM_FINITO_INFINITO.id -> Sec1Numeros.finitoInfinito(f)
            Curriculum.NUM_PRIMOS.id -> Sec1Numeros.primos(f)
            Curriculum.NUM_POTENCIAS.id -> Sec1Numeros.potencias(f)
            Curriculum.FRAC_PROPIA.id -> Sec1Numeros.fraccionTipo(f)
            Curriculum.FRAC_EQUIVALENTES.id -> Sec1Numeros.equivalentes(f)
            Curriculum.FRAC_COMPARAR.id -> Sec1Numeros.comparar(f)
            Curriculum.FRAC_A_DECIMAL.id -> Sec1Numeros.aDecimal(f)
            Curriculum.FRAC_DE_CANTIDAD.id -> Sec1Numeros.deCantidad(f)

            Curriculum.DEC_OPERACIONES.id -> Sec1Aritmetica.decimalesOperaciones(f)
            Curriculum.DEC_PROBLEMAS.id -> Sec1Aritmetica.decimalesProblemas(f)
            Curriculum.DEC_EQUIVALENCIAS.id -> Sec1Aritmetica.equivalencias(f)

            Curriculum.ENT_SUMA_RESTA.id -> Sec1Aritmetica.enterosSumaResta(f)
            Curriculum.ENT_REPRESENTAR.id -> Sec1Aritmetica.enterosRepresentar(f)

            Curriculum.PROP_REGLA_TRES.id -> Sec1Aritmetica.reglaDeTres(f)
            Curriculum.PROP_VALOR_UNITARIO.id -> Sec1Aritmetica.valorUnitario(f)

            Curriculum.SUC_SIGUIENTE.id -> Sec1Algebra.siguiente(f)
            Curriculum.SUC_REGLA_GENERAL.id -> Sec1Algebra.reglaGeneral(f)
            Curriculum.SUC_EVALUAR_REGLA.id -> Sec1Algebra.evaluarRegla(f)
            Curriculum.SUC_FIGURAL.id -> Sec1Algebra.figural(f)

            Curriculum.EC_LINEAL.id -> Sec1Algebra.ecuacionLineal(f)
            Curriculum.EC_PROBLEMA.id -> Sec1Algebra.ecuacionProblema(f)

            Curriculum.TRI_CLASIFICAR.id -> Sec1Geometria.triangulosClasificar(f)
            Curriculum.TRI_PERIMETRO.id -> Sec1Geometria.trianguloPerimetro(f)
            Curriculum.FIG_AREA_PERIMETRO.id -> Sec1Geometria.areaPerimetro(f)
            Curriculum.CIR_ELEMENTOS.id -> Sec1Geometria.circuloElementos(f)
            Curriculum.PLANO_DISTANCIA.id -> Sec1Geometria.distancia(f)

            Curriculum.PROB_ESPACIO.id -> Sec1Probabilidad.espacioMuestral(f)
            Curriculum.PROB_SIMPLE.id -> Sec1Probabilidad.probabilidadSimple(f)
            Curriculum.PROB_TIPOS.id -> Sec1Probabilidad.tiposEvento(f)
            Curriculum.PROB_FACTORIAL.id -> Sec1Probabilidad.factorial(f)
            Curriculum.PROB_ARBOL.id -> Sec1Probabilidad.diagramaArbol(f)

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
