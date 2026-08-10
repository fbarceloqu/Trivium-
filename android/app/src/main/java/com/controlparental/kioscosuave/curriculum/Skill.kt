package com.controlparental.kioscosuave.curriculum

import com.controlparental.kioscosuave.GradeLevel

/**
 * TAXONOMÍA DE HABILIDADES
 * ========================
 *
 * Hasta ahora Trivium medía el progreso por MATERIA ("mate: 7/10"). Con ese dato
 * es imposible saber QUÉ necesita practicar el niño, así que tampoco se puede
 * hacer repetición espaciada ni adaptar la dificultad.
 *
 * Una [Skill] es la unidad mínima de conocimiento que se puede rastrear: no
 * "matemáticas" ni siquiera "porcentajes", sino "calcular el porcentaje de una
 * cantidad". La memoria del alumno (Fase 2) guarda un estado por cada una.
 *
 * ORIGEN DEL TEMARIO DE 1° DE SECUNDARIA
 * --------------------------------------
 * No está inventado ni sacado de un plan genérico: se derivó de los exámenes
 * mensuales reales del Colegio de Obregón (ciclo 2025-2026, campo formativo
 * "Saberes y Pensamiento Científico"). Cada tema indica en qué examen aparece,
 * para poder anticipar el examen del mes en el repaso.
 *
 * Los reactivos que se fallaron en esos exámenes están marcados con ⚠️: son los
 * huecos reales detectados, y el generador les da formatos extra para atacar la
 * confusión concreta (p. ej. complementario↔suplementario están invertidos).
 */

enum class Subject { MATH, ENGLISH, READING }

/**
 * Cómo se le presenta un mismo conocimiento al alumno.
 *
 * Esto es lo que permite REPETIR SIN ABURRIR: el concepto "multiplicar por 7"
 * se puede pedir como 7×8=?, como 7×__=56, como secuencia 7,14,21,__ o como
 * problema de granja. Mismo conocimiento, actividad distinta.
 *
 * [cognitiveLevel] ordena la progresión recordar → comprender → aplicar →
 * razonar. La memoria sube de nivel conforme el alumno demuestra dominio, en
 * vez de seguir mostrando el formato más básico indefinidamente.
 */
enum class ExerciseFormat(val label: String, val cognitiveLevel: Int) {
    /** Recordar: "¿cómo se llama la medida que aparece con más frecuencia?" */
    CONCEPTUAL("Definición", 1),

    /** Comprender: el cálculo directo, con todos los datos dados. */
    DIRECTO("Operación directa", 2),

    /** Aplicar al revés: se da el resultado y falta un dato de entrada. */
    INVERSO("Dato faltante", 3),

    /** Aplicar: reconocer el patrón dentro de una serie. */
    SECUENCIA("Patrón", 3),

    /** Aplicar: el mismo cálculo escondido en una situación cotidiana. */
    CONTEXTO("Problema con contexto", 4),

    /** Razonar: exige combinar dos o más pasos para llegar al resultado. */
    RAZONAMIENTO("Varios pasos", 5)
}

data class Skill(
    /** Identificador estable. NUNCA cambiarlo: es la llave del historial. */
    val id: String,
    val subject: Subject,
    val grade: GradeLevel,
    /** Agrupación visible para el padre en el panel ("Porcentajes"). */
    val topic: String,
    /** Descripción corta de la habilidad, en lenguaje de padre. */
    val label: String,
    /** Formatos en los que este conocimiento se puede pedir. */
    val formats: List<ExerciseFormat>,
    /** Habilidades que conviene dominar antes; la Fase 2 las usa para ordenar. */
    val prerequisites: List<String> = emptyList()
)

object Curriculum {

    private val G = GradeLevel.SECUNDARIA
    private val M = Subject.MATH

    private fun skill(
        id: String,
        topic: String,
        label: String,
        formats: List<ExerciseFormat>,
        prerequisites: List<String> = emptyList()
    ) = Skill(id, M, G, topic, label, formats, prerequisites)

    // ---------------------------------------------------------------------
    // TEMA 1 · Números y fracciones            (examen de SEPTIEMBRE)
    // ---------------------------------------------------------------------
    private const val T_NUM = "Números y fracciones"

    val NUM_TIPOS = skill(
        "sec1.num.tipos", T_NUM,
        "Clasificar números: naturales, enteros, racionales e irracionales",
        listOf(ExerciseFormat.CONCEPTUAL, ExerciseFormat.DIRECTO)
    )
    val NUM_FINITO_INFINITO = skill(
        "sec1.num.finito_infinito", T_NUM,
        "Distinguir un decimal finito de uno infinito periódico", // ⚠️ fallado
        listOf(ExerciseFormat.CONCEPTUAL, ExerciseFormat.DIRECTO)
    )
    val NUM_PRIMOS = skill(
        "sec1.num.primos", T_NUM,
        "Números primos, divisores y múltiplos",
        listOf(ExerciseFormat.CONCEPTUAL, ExerciseFormat.DIRECTO, ExerciseFormat.INVERSO)
    )
    val NUM_POTENCIAS = skill(
        "sec1.num.potencias", T_NUM,
        "Potencias de 10, raíz cuadrada y raíz cúbica",
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.INVERSO)
    )
    val FRAC_PROPIA = skill(
        "sec1.frac.propia_impropia", T_NUM,
        "Clasificar fracciones: propia, impropia, mixta y decimal", // ⚠️ invertidas
        listOf(ExerciseFormat.CONCEPTUAL, ExerciseFormat.DIRECTO)
    )
    val FRAC_EQUIVALENTES = skill(
        "sec1.frac.equivalentes", T_NUM,
        "Reconocer fracciones equivalentes", // ⚠️ fallado
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.INVERSO)
    )
    val FRAC_COMPARAR = skill(
        "sec1.frac.comparar", T_NUM,
        "Comparar fracciones con >, < o =", // ⚠️ fallado
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.RAZONAMIENTO),
        prerequisites = listOf("sec1.frac.equivalentes")
    )
    val FRAC_A_DECIMAL = skill(
        "sec1.frac.a_decimal", T_NUM,
        "Convertir una fracción común a decimal", // ⚠️ fallado
        listOf(ExerciseFormat.DIRECTO)
    )
    val FRAC_DE_CANTIDAD = skill(
        "sec1.frac.de_cantidad", T_NUM,
        "Calcular la fracción de una cantidad", // ⚠️ fallado (1/5 de 6000)
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.CONTEXTO, ExerciseFormat.RAZONAMIENTO)
    )

    // ---------------------------------------------------------------------
    // TEMA 2 · Decimales                       (examen DIAGNÓSTICO)
    // ---------------------------------------------------------------------
    private const val T_DEC = "Decimales"

    val DEC_OPERACIONES = skill(
        "sec1.dec.operaciones", T_DEC,
        "Sumar, restar, multiplicar y dividir decimales", // ⚠️ 1.8÷2 y 3.85×5
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.INVERSO)
    )
    val DEC_PROBLEMAS = skill(
        "sec1.dec.problemas", T_DEC,
        "Resolver problemas cotidianos con decimales",
        listOf(ExerciseFormat.CONTEXTO, ExerciseFormat.RAZONAMIENTO),
        prerequisites = listOf("sec1.dec.operaciones")
    )
    val DEC_EQUIVALENCIAS = skill(
        "sec1.dec.equivalencias", T_DEC,
        "Equivalencias entre fracción, decimal y porcentaje (3/5 = 0.6 = 60%)",
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.CONCEPTUAL),
        prerequisites = listOf("sec1.frac.a_decimal")
    )

    // ---------------------------------------------------------------------
    // TEMA 3 · Números con signo               (examen DIAGNÓSTICO)
    // ---------------------------------------------------------------------
    private const val T_ENT = "Números con signo"

    val ENT_SUMA_RESTA = skill(
        "sec1.ent.suma_resta", T_ENT,
        "Sumar y restar números positivos y negativos", // ⚠️ −5+3+8−10
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.INVERSO, ExerciseFormat.RAZONAMIENTO)
    )
    val ENT_REPRESENTAR = skill(
        "sec1.ent.representar", T_ENT,
        "Representar situaciones con signo (ganó $200 → +200)",
        listOf(ExerciseFormat.CONCEPTUAL, ExerciseFormat.CONTEXTO)
    )

    // ---------------------------------------------------------------------
    // TEMA 4 · Proporcionalidad                (examen DIAGNÓSTICO)
    // ---------------------------------------------------------------------
    private const val T_PROP = "Proporcionalidad"

    val PROP_REGLA_TRES = skill(
        "sec1.prop.regla_de_tres", T_PROP,
        "Regla de tres simple", // ⚠️ gasolina y canciones
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.CONTEXTO, ExerciseFormat.RAZONAMIENTO)
    )
    val PROP_VALOR_UNITARIO = skill(
        "sec1.prop.valor_unitario", T_PROP,
        "Encontrar el valor unitario (precio por pieza, consumo por km)",
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.CONTEXTO)
    )

    // ---------------------------------------------------------------------
    // TEMA 5 · Porcentajes                     (examen de FEBRERO)
    // El tema con peor desempeño de todos: casi todos los reactivos fallados.
    // ---------------------------------------------------------------------
    private const val T_PCT = "Porcentajes"

    val PCT_DE_CANTIDAD = skill(
        "sec1.pct.de_cantidad", T_PCT,
        "Calcular el porcentaje de una cantidad", // ⚠️ 35% de 13.8
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.CONTEXTO),
        prerequisites = listOf("sec1.dec.operaciones")
    )
    val PCT_INVERSO = skill(
        "sec1.pct.inverso", T_PCT,
        "Encontrar el total cuando se conoce el porcentaje", // ⚠️ "el 20% es 8"
        listOf(ExerciseFormat.INVERSO, ExerciseFormat.CONTEXTO, ExerciseFormat.RAZONAMIENTO),
        prerequisites = listOf("sec1.pct.de_cantidad")
    )
    val PCT_COMPLEMENTO = skill(
        "sec1.pct.complemento", T_PCT,
        "Calcular el resto: si el 25% reprueba, ¿cuántos aprueban?", // ⚠️ fallado
        listOf(ExerciseFormat.CONTEXTO, ExerciseFormat.RAZONAMIENTO),
        prerequisites = listOf("sec1.pct.de_cantidad")
    )
    val PCT_DESCUENTO = skill(
        "sec1.pct.descuento", T_PCT,
        "Precio final con descuento", // ⚠️ camisa de 160 con 20%
        listOf(ExerciseFormat.CONTEXTO, ExerciseFormat.RAZONAMIENTO),
        prerequisites = listOf("sec1.pct.de_cantidad")
    )
    val PCT_AUMENTO = skill(
        "sec1.pct.aumento", T_PCT,
        "Precio o cantidad final con un aumento",
        listOf(ExerciseFormat.CONTEXTO, ExerciseFormat.RAZONAMIENTO),
        prerequisites = listOf("sec1.pct.de_cantidad")
    )
    val PCT_VARIACION = skill(
        "sec1.pct.variacion", T_PCT,
        "Qué porcentaje aumentó o disminuyó algo", // ⚠️ de 510 a 440
        listOf(ExerciseFormat.INVERSO, ExerciseFormat.CONTEXTO),
        prerequisites = listOf("sec1.pct.de_cantidad")
    )

    // ---------------------------------------------------------------------
    // TEMA 6 · Sucesiones y patrones           (examen de MARZO)
    // ---------------------------------------------------------------------
    private const val T_SUC = "Sucesiones y patrones"

    val SUC_SIGUIENTE = skill(
        "sec1.suc.siguiente", T_SUC,
        "Encontrar el siguiente término de una sucesión",
        listOf(ExerciseFormat.SECUENCIA, ExerciseFormat.DIRECTO)
    )
    val SUC_REGLA_GENERAL = skill(
        "sec1.suc.regla_general", T_SUC,
        "Deducir la regla general (a(n)) de una sucesión", // ⚠️ 5,11,17 → 6n−1
        listOf(ExerciseFormat.SECUENCIA, ExerciseFormat.RAZONAMIENTO),
        prerequisites = listOf("sec1.suc.siguiente")
    )
    val SUC_EVALUAR_REGLA = skill(
        "sec1.suc.evaluar_regla", T_SUC,
        "Dada la regla, escribir los términos o el término n", // ⚠️ posición 10
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.INVERSO),
        prerequisites = listOf("sec1.suc.regla_general")
    )
    val SUC_FIGURAL = skill(
        "sec1.suc.figural", T_SUC,
        "Sucesiones de figuras: cuántos elementos tendrá la figura n", // ⚠️ cubos
        listOf(ExerciseFormat.SECUENCIA, ExerciseFormat.RAZONAMIENTO),
        prerequisites = listOf("sec1.suc.regla_general")
    )

    // ---------------------------------------------------------------------
    // TEMA 7 · Ecuaciones
    // Único tema que el motor actual ya cubría.
    // ---------------------------------------------------------------------
    private const val T_EC = "Ecuaciones"

    val EC_LINEAL = skill(
        "sec1.ec.lineal", T_EC,
        "Resolver ecuaciones de primer grado (4x + 8 = 44)",
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.INVERSO)
    )
    val EC_PROBLEMA = skill(
        "sec1.ec.problema", T_EC,
        "Plantear y resolver un problema con una ecuación",
        listOf(ExerciseFormat.CONTEXTO, ExerciseFormat.RAZONAMIENTO),
        prerequisites = listOf("sec1.ec.lineal")
    )

    // ---------------------------------------------------------------------
    // TEMA 8 · Ángulos                         (examen de NOVIEMBRE)
    // ---------------------------------------------------------------------
    private const val T_ANG = "Ángulos"

    val ANG_CLASIFICAR = skill(
        "sec1.ang.clasificar", T_ANG,
        "Clasificar ángulos: agudo, recto, obtuso, llano y completo", // ⚠️ fallado
        listOf(ExerciseFormat.CONCEPTUAL, ExerciseFormat.DIRECTO)
    )
    val ANG_COMPLEMENTARIO = skill(
        "sec1.ang.complementario", T_ANG,
        "Ángulos complementarios (suman 90°)", // ⚠️ confundido con suplementario
        listOf(ExerciseFormat.CONCEPTUAL, ExerciseFormat.DIRECTO, ExerciseFormat.INVERSO)
    )
    val ANG_SUPLEMENTARIO = skill(
        "sec1.ang.suplementario", T_ANG,
        "Ángulos suplementarios (suman 180°)", // ⚠️ confundido con complementario
        listOf(ExerciseFormat.CONCEPTUAL, ExerciseFormat.DIRECTO, ExerciseFormat.INVERSO)
    )

    // ---------------------------------------------------------------------
    // TEMA 9 · Triángulos y figuras            (NOVIEMBRE + DIAGNÓSTICO)
    // ---------------------------------------------------------------------
    private const val T_FIG = "Triángulos y figuras"

    val TRI_CLASIFICAR = skill(
        "sec1.tri.clasificar", T_FIG,
        "Clasificar triángulos por sus lados y por sus ángulos", // ⚠️ 15,6,x
        listOf(ExerciseFormat.CONCEPTUAL, ExerciseFormat.DIRECTO)
    )
    val TRI_PERIMETRO = skill(
        "sec1.tri.perimetro", T_FIG,
        "Perímetro de un triángulo y lados de un isósceles", // ⚠️ fallado
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.INVERSO, ExerciseFormat.RAZONAMIENTO)
    )
    val FIG_AREA_PERIMETRO = skill(
        "sec1.fig.area_perimetro", T_FIG,
        "Área y perímetro de cuadrado y rectángulo, con sus unidades", // ⚠️ m vs m²
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.INVERSO, ExerciseFormat.CONTEXTO)
    )

    // ---------------------------------------------------------------------
    // TEMA 10 · Círculo                        (examen DIAGNÓSTICO)
    // ---------------------------------------------------------------------
    private const val T_CIR = "Círculo"

    val CIR_ELEMENTOS = skill(
        "sec1.cir.elementos", T_CIR,
        "Elementos del círculo: radio, diámetro, cuerda y secante", // ⚠️ radio↔diámetro
        listOf(ExerciseFormat.CONCEPTUAL, ExerciseFormat.DIRECTO)
    )

    // ---------------------------------------------------------------------
    // TEMA 11 · Plano cartesiano               (examen de NOVIEMBRE)
    // ---------------------------------------------------------------------
    private const val T_PLANO = "Plano cartesiano"

    val PLANO_DISTANCIA = skill(
        "sec1.plano.distancia", T_PLANO,
        "Distancia entre dos puntos del plano", // ⚠️ con coordenadas negativas
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.CONTEXTO, ExerciseFormat.RAZONAMIENTO),
        prerequisites = listOf("sec1.num.potencias")
    )

    // ---------------------------------------------------------------------
    // TEMA 12 · Estadística                    (NOVIEMBRE y otra vez MARZO)
    // El mismo error apareció en dos exámenes con 4 meses de diferencia y
    // nadie lo reforzó. Es el caso que mejor justifica la memoria de Fase 2.
    // ---------------------------------------------------------------------
    private const val T_EST = "Estadística"

    val EST_IDENTIFICAR = skill(
        "sec1.est.identificar", T_EST,
        "Distinguir qué es media, moda, mediana y rango", // ⚠️ intercambiadas
        listOf(ExerciseFormat.CONCEPTUAL)
    )
    val EST_MEDIA = skill(
        "sec1.est.media", T_EST,
        "Calcular la media aritmética", // ⚠️ fallado
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.INVERSO, ExerciseFormat.CONTEXTO)
    )
    val EST_MODA = skill(
        "sec1.est.moda", T_EST,
        "Encontrar la moda, incluyendo casos sin moda o con dos", // ⚠️ fallado
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.CONTEXTO)
    )
    val EST_MEDIANA = skill(
        "sec1.est.mediana", T_EST,
        "Encontrar la mediana ordenando los datos", // ⚠️ fallado
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.RAZONAMIENTO)
    )
    val EST_RANGO = skill(
        "sec1.est.rango", T_EST,
        "Calcular el rango de un conjunto de datos",
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.CONTEXTO)
    )
    val EST_GRAFICAS = skill(
        "sec1.est.graficas", T_EST,
        "Leer gráficas de barras y circulares",
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.RAZONAMIENTO)
    )

    // ---------------------------------------------------------------------
    // TEMA 13 · Probabilidad                   (examen de JUNIO)
    // ---------------------------------------------------------------------
    private const val T_PROB = "Probabilidad"

    val PROB_ESPACIO = skill(
        "sec1.prob.espacio_muestral", T_PROB,
        "Contar cuántos resultados posibles tiene un experimento", // ⚠️ dado = 4
        listOf(ExerciseFormat.CONCEPTUAL, ExerciseFormat.DIRECTO, ExerciseFormat.RAZONAMIENTO)
    )
    val PROB_SIMPLE = skill(
        "sec1.prob.simple", T_PROB,
        "Calcular la probabilidad de un evento sencillo", // ⚠️ 3 rojas de 5
        listOf(ExerciseFormat.DIRECTO, ExerciseFormat.CONTEXTO),
        prerequisites = listOf("sec1.prob.espacio_muestral")
    )
    val PROB_TIPOS = skill(
        "sec1.prob.tipos_evento", T_PROB,
        "Distinguir evento seguro, imposible y aleatorio", // ⚠️ fallado
        listOf(ExerciseFormat.CONCEPTUAL, ExerciseFormat.DIRECTO)
    )
    val PROB_FACTORIAL = skill(
        "sec1.prob.factorial", T_PROB,
        "Calcular el factorial de un número", // ⚠️ 5! = 25, y "!" = potencia
        listOf(ExerciseFormat.CONCEPTUAL, ExerciseFormat.DIRECTO)
    )
    val PROB_ARBOL = skill(
        "sec1.prob.diagrama_arbol", T_PROB,
        "Usar el diagrama de árbol para contar combinaciones",
        listOf(ExerciseFormat.CONCEPTUAL, ExerciseFormat.RAZONAMIENTO),
        prerequisites = listOf("sec1.prob.espacio_muestral")
    )

    // ---------------------------------------------------------------------

    /** Todas las habilidades de 1° de secundaria, en orden de temario. */
    val sec1Math: List<Skill> = listOf(
        NUM_TIPOS, NUM_FINITO_INFINITO, NUM_PRIMOS, NUM_POTENCIAS,
        FRAC_PROPIA, FRAC_EQUIVALENTES, FRAC_COMPARAR, FRAC_A_DECIMAL, FRAC_DE_CANTIDAD,
        DEC_OPERACIONES, DEC_PROBLEMAS, DEC_EQUIVALENCIAS,
        ENT_SUMA_RESTA, ENT_REPRESENTAR,
        PROP_REGLA_TRES, PROP_VALOR_UNITARIO,
        PCT_DE_CANTIDAD, PCT_INVERSO, PCT_COMPLEMENTO, PCT_DESCUENTO, PCT_AUMENTO, PCT_VARIACION,
        SUC_SIGUIENTE, SUC_REGLA_GENERAL, SUC_EVALUAR_REGLA, SUC_FIGURAL,
        EC_LINEAL, EC_PROBLEMA,
        ANG_CLASIFICAR, ANG_COMPLEMENTARIO, ANG_SUPLEMENTARIO,
        TRI_CLASIFICAR, TRI_PERIMETRO, FIG_AREA_PERIMETRO,
        CIR_ELEMENTOS,
        PLANO_DISTANCIA,
        EST_IDENTIFICAR, EST_MEDIA, EST_MODA, EST_MEDIANA, EST_RANGO, EST_GRAFICAS,
        PROB_ESPACIO, PROB_SIMPLE, PROB_TIPOS, PROB_FACTORIAL, PROB_ARBOL
    )

    private val skillsById: Map<String, Skill> = sec1Math.associateBy { it.id }

    fun byId(id: String): Skill? = skillsById[id]

    fun forGrade(grade: GradeLevel, subject: Subject): List<Skill> =
        sec1Math.filter { it.grade == grade && it.subject == subject }

    /** Habilidades agrupadas por tema, para el mapa de dominio del panel de padres. */
    fun topics(grade: GradeLevel, subject: Subject): Map<String, List<Skill>> =
        forGrade(grade, subject).groupBy { it.topic }

    /**
     * Tema que el colegio evalúa en el mes dado (1 = enero), para poder reforzar
     * antes del examen. null si ese mes no tiene examen conocido.
     *
     * Derivado del calendario real observado en los exámenes del ciclo 2025-2026.
     * Si cambia el colegio o el ciclo, esto es lo único que hay que actualizar.
     */
    fun examTopicForMonth(month: Int): String? = when (month) {
        9 -> T_NUM      // septiembre
        11 -> T_ANG     // noviembre (ángulos + estadística)
        2 -> T_PCT      // febrero
        3 -> T_SUC      // marzo (sucesiones + estadística)
        6 -> T_PROB     // junio
        else -> null
    }
}
