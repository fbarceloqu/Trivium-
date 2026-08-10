package com.controlparental.kioscosuave.curriculum

import com.controlparental.kioscosuave.MathQuestion
import com.controlparental.kioscosuave.WorkedExample

/**
 * TEMA · PROBABILIDAD  (examen de junio, el peor calificado del ciclo)
 *
 * Errores observados:
 *   · "¿cuántos resultados posibles hay al lanzar un dado?" → se respondió 4
 *   · "¿cuántos al lanzar dos monedas?" → se respondió 3 (son 4)
 *   · evento imposible: se eligió "sacar un 3 en un dado" (lo imposible es el 7)
 *   · el signo "!" se identificó como potencia, y 5! se calculó como 25 (= 5²)
 *   · P(roja) con 3 rojas y 2 azules → se respondió 2/5
 *
 * Las probabilidades se expresan SIN simplificar (favorables/totales), que es
 * como las presenta el colegio; así nunca hay dos opciones correctas
 * (3/6 y 1/2 no pueden aparecer juntas).
 */
internal object Sec1Probabilidad {

    // ==================================================================
    // Espacio muestral  ⚠️
    // ==================================================================
    private val EX_ESPACIO = WorkedExample(
        "Ejemplo: contar resultados posibles",
        listOf(
            "MONEDA → 2 resultados: águila o sol",
            "DADO   → 6 resultados: 1, 2, 3, 4, 5 y 6",
            "DOS MONEDAS → se multiplican: 2 × 2 = 4",
            "  (AA, AS, SA, SS — son cuatro, no tres:",
            "   'águila y sol' es distinto de 'sol y águila')",
            "DOS DADOS → 6 × 6 = 36"
        )
    )

    fun espacioMuestral(format: ExerciseFormat): MathQuestion {
        val casos = listOf(
            Triple("lanzar una moneda", 2, "Una moneda tiene 2 caras: águila y sol."),
            Triple("lanzar un dado", 6, "Un dado tiene 6 caras: 1, 2, 3, 4, 5 y 6."),
            Triple("lanzar dos monedas", 4, "Se multiplican: 2 × 2 = 4 (AA, AS, SA, SS)."),
            Triple("lanzar dos dados", 36, "Se multiplican: 6 × 6 = 36."),
            Triple("lanzar una moneda y un dado", 12, "Se multiplican: 2 × 6 = 12.")
        )
        val (exp, n, explica) = if (format == ExerciseFormat.RAZONAMIENTO)
            casos.filter { it.second > 6 }.random()
        else
            casos.take(3).random()

        return MathQuestion(
            "¿Cuántos resultados posibles existen al $exp?",
            Gen.numOpts(
                n.toDouble(),
                (n - 1).toDouble(),      // el error observado: contar uno de menos
                (n + 1).toDouble(),
                (n / 2).toDouble()
            ),
            n.toString(),
            listOf(explica, "Total de resultados posibles: $n"),
            EX_ESPACIO, Curriculum.PROB_ESPACIO.id, format
        )
    }

    // ==================================================================
    // Probabilidad simple  ⚠️
    // ==================================================================
    private val EX_SIMPLE = WorkedExample(
        "Ejemplo: probabilidad de un evento",
        listOf(
            "Una bolsa tiene 3 pelotas rojas y 2 azules. ¿P(roja)?",
            "1) Cuenta los casos FAVORABLES (las que te piden): 3 rojas",
            "2) Cuenta el TOTAL de casos: 3 + 2 = 5 pelotas",
            "3) Se escribe favorables / total:  3/5",
            "Error común: poner 2/5, que son las azules (lo que NO te piden).",
            "La probabilidad siempre va entre 0 y 1: nunca puede pasar de 1."
        )
    )

    fun probabilidadSimple(format: ExerciseFormat): MathQuestion {
        return when (listOf("bolsa", "dado_par", "moneda", "dado_num").random()) {
            "bolsa" -> {
                // Deben ser cantidades DISTINTAS: si hay las mismas rojas que
                // azules, "las que te piden" y "las otras" dan la misma
                // fracción y el ejercicio se queda con 3 opciones.
                val rojas = (2..5).random()
                val azules = ((2..6) - rojas).random()
                val total = rojas + azules
                val color = listOf("rojas" to rojas, "azules" to azules).random()
                MathQuestion(
                    "Si una bolsa tiene $rojas pelotas rojas y $azules azules, ¿cuál es la probabilidad de sacar una ${color.first.dropLast(1)}?",
                    listOf(
                        "${color.second}/$total",
                        "${total - color.second}/$total",   // error: contar las otras
                        "$total/${color.second}",           // error: invertir
                        "${color.second}/${total - color.second}"
                    ).distinct().take(4).shuffled(),
                    "${color.second}/$total",
                    listOf(
                        "Casos favorables (${color.first}): ${color.second}",
                        "Casos totales: $rojas + $azules = $total",
                        "Probabilidad = favorables / total = ${color.second}/$total"
                    ),
                    EX_SIMPLE, Curriculum.PROB_SIMPLE.id, format
                )
            }
            "dado_par" -> MathQuestion(
                "¿Cuál es la probabilidad de obtener un número par al lanzar un dado?",
                listOf("3/6", "1/6", "2/6", "4/6").shuffled(),
                "3/6",
                listOf(
                    "Los pares de un dado son 2, 4 y 6 → 3 casos favorables",
                    "El dado tiene 6 caras en total",
                    "Probabilidad = 3/6"
                ),
                EX_SIMPLE, Curriculum.PROB_SIMPLE.id, format
            )
            "moneda" -> MathQuestion(
                "¿Cuál es la probabilidad de obtener \"águila\" al lanzar una moneda?",
                listOf("1/2", "1/4", "1/3", "2/3").shuffled(),
                "1/2",
                listOf(
                    "Hay 1 caso favorable (águila) de 2 posibles (águila o sol)",
                    "Probabilidad = 1/2"
                ),
                EX_SIMPLE, Curriculum.PROB_SIMPLE.id, format
            )
            else -> {
                val cara = (1..6).random()
                MathQuestion(
                    "¿Cuál es la probabilidad de obtener el número $cara al lanzar un dado?",
                    listOf("1/6", "1/3", "6/1", "2/6").shuffled(),
                    "1/6",
                    listOf(
                        "Solo hay 1 cara con el número $cara → 1 caso favorable",
                        "El dado tiene 6 caras → 6 casos totales",
                        "Probabilidad = 1/6"
                    ),
                    EX_SIMPLE, Curriculum.PROB_SIMPLE.id, format
                )
            }
        }
    }

    // ==================================================================
    // Tipos de evento  ⚠️
    // ==================================================================
    private val EX_TIPOS = WorkedExample(
        "Ejemplo: tipos de evento",
        listOf(
            "SEGURO    → siempre ocurre. Su probabilidad es 1.",
            "            'Sacar un número menor que 7 en un dado'",
            "IMPOSIBLE → nunca puede ocurrir. Su probabilidad es 0.",
            "            'Sacar un 7 en un dado' (el dado solo llega al 6)",
            "ALEATORIO → puede ocurrir o no. Su probabilidad está entre 0 y 1.",
            "            'Que el dado caiga en 5'",
            "Ojo: sacar un 3 en un dado SÍ es posible, es aleatorio."
        )
    )

    fun tiposEvento(format: ExerciseFormat): MathQuestion {
        return when (listOf("imposible", "aleatorio", "seguro_valor", "definicion").random()) {
            "imposible" -> MathQuestion(
                "¿Cuál de los siguientes eventos es imposible?",
                listOf(
                    "Sacar un 7 en un dado",
                    "Sacar un 3 en un dado",
                    "Lanzar una moneda y que caiga sol",
                    "Sacar un número par en un dado"
                ).shuffled(),
                "Sacar un 7 en un dado",
                listOf(
                    "Un dado solo tiene los números del 1 al 6.",
                    "El 7 no existe en el dado, así que ese evento NUNCA puede ocurrir.",
                    "Los otros tres sí pueden pasar: son eventos aleatorios."
                ),
                EX_TIPOS, Curriculum.PROB_TIPOS.id, format
            )
            "aleatorio" -> MathQuestion(
                "¿Cuál de los siguientes es un ejemplo de evento aleatorio?",
                listOf(
                    "Que un dado caiga en el número 5",
                    "Que el Sol salga de día",
                    "Que el agua moje",
                    "Que un año tenga 12 meses"
                ).shuffled(),
                "Que un dado caiga en el número 5",
                listOf(
                    "Un evento ALEATORIO puede ocurrir o no: no se sabe de antemano.",
                    "Que el Sol salga, que el agua moje y que el año tenga 12 meses son SEGUROS.",
                    "Que el dado caiga en 5 depende del azar."
                ),
                EX_TIPOS, Curriculum.PROB_TIPOS.id, format
            )
            "seguro_valor" -> {
                val esSeguro = listOf(true, false).random()
                MathQuestion(
                    "La probabilidad de un evento ${if (esSeguro) "seguro" else "imposible"} es:",
                    Gen.numOpts(if (esSeguro) 1.0 else 0.0, if (esSeguro) 0.0 else 1.0, 2.0, 10.0),
                    if (esSeguro) "1" else "0",
                    listOf(
                        "La probabilidad siempre va de 0 a 1.",
                        "0 = imposible (nunca pasa)  |  1 = seguro (siempre pasa)",
                        "Nunca puede ser mayor que 1."
                    ),
                    EX_TIPOS, Curriculum.PROB_TIPOS.id, format
                )
            }
            else -> MathQuestion(
                "¿Qué representa el azar?",
                listOf(
                    "Algo que puede suceder o no",
                    "Algo seguro",
                    "Algo imposible",
                    "Una operación matemática"
                ).shuffled(),
                "Algo que puede suceder o no",
                listOf("El azar es la incertidumbre: no se sabe si el evento ocurrirá."),
                EX_TIPOS, Curriculum.PROB_TIPOS.id, format
            )
        }
    }

    // ==================================================================
    // Factorial  ⚠️
    // ==================================================================
    private val EX_FACTORIAL = WorkedExample(
        "Ejemplo: factorial",
        listOf(
            "El signo ! significa FACTORIAL, no potencia.",
            "5! = 5 × 4 × 3 × 2 × 1 = 120",
            "  (se multiplica el número por todos los menores hasta llegar a 1)",
            "4! = 4 × 3 × 2 × 1 = 24",
            "3! = 3 × 2 × 1 = 6",
            "ERROR COMÚN: calcular 5! como 5² = 25. No es una potencia:",
            "el factorial crece muchísimo más rápido."
        )
    )

    private fun fact(n: Int): Int = if (n <= 1) 1 else n * fact(n - 1)

    fun factorial(format: ExerciseFormat): MathQuestion {
        if (format == ExerciseFormat.CONCEPTUAL) {
            return MathQuestion(
                "¿Qué operación se representa con el signo \"!\"?",
                listOf("Regla factorial", "Potencia", "División", "Multiplicación").shuffled(),
                "Regla factorial",
                listOf(
                    "El signo ! indica FACTORIAL.",
                    "n! = n × (n−1) × (n−2) × ... × 1",
                    "No confundir con la potencia: 5! = 120, pero 5² = 25."
                ),
                EX_FACTORIAL, Curriculum.PROB_FACTORIAL.id, format
            )
        }

        val n = (3..6).random()
        val ans = fact(n)
        return MathQuestion(
            "¿Cuánto vale $n! ?",
            Gen.numOpts(
                ans.toDouble(),
                (n * n).toDouble(),          // el error observado: calcularlo como potencia
                (n * (n - 1)).toDouble(),    // error: quedarse en el primer producto
                (ans * 2).toDouble()
            ),
            ans.toString(),
            listOf(
                "$n! = ${(n downTo 1).joinToString(" × ")}",
                "= $ans",
                "Recuerda: NO es $n × $n = ${n * n}; eso sería una potencia."
            ),
            EX_FACTORIAL, Curriculum.PROB_FACTORIAL.id, format
        )
    }

    // ==================================================================
    // Diagrama de árbol
    // ==================================================================
    private val EX_ARBOL = WorkedExample(
        "Ejemplo: diagrama de árbol",
        listOf(
            "Sirve para ORGANIZAR todas las posibilidades sin que se escape ninguna.",
            "Con 3 playeras y 2 pantalones:",
            "  Playera 1 → pantalón A, pantalón B   (2 combinaciones)",
            "  Playera 2 → pantalón A, pantalón B   (2 combinaciones)",
            "  Playera 3 → pantalón A, pantalón B   (2 combinaciones)",
            "Total: 3 × 2 = 6 combinaciones.",
            "Atajo: se multiplican las opciones de cada paso."
        )
    )

    fun diagramaArbol(format: ExerciseFormat): MathQuestion {
        if (format == ExerciseFormat.RAZONAMIENTO) {
            val a = (2..5).random()
            val b = (2..4).random()
            val ans = a * b
            val name = Gen.NAMES.random()
            return MathQuestion(
                "$name tiene $a playeras y $b pantalones. ¿De cuántas formas distintas puede vestirse?",
                Gen.numOpts(
                    ans.toDouble(),
                    (a + b).toDouble(),          // error: sumar en vez de multiplicar
                    (ans + 1).toDouble(),
                    kotlin.math.max(a, b).toDouble()
                ),
                ans.toString(),
                listOf(
                    "Por cada playera puede elegir cualquiera de los $b pantalones.",
                    "Se multiplican las opciones: $a × $b = $ans",
                    "Un diagrama de árbol lo muestra: $a ramas que se abren en $b cada una."
                ),
                EX_ARBOL, Curriculum.PROB_ARBOL.id, format
            )
        }

        val (q, a) = listOf(
            "¿Qué es un diagrama de árbol?" to "Una forma de organizar posibilidades y resultados",
            "¿Para qué sirve un diagrama de árbol?" to "Para representar posibles resultados",
            "¿Qué herramienta ayuda a organizar posibilidades en probabilidad?" to "Diagrama de árbol"
        ).random()

        val opciones = if (a == "Diagrama de árbol")
            listOf("Diagrama de árbol", "Regla", "Transportador", "Tabla periódica")
        else
            listOf(
                "Una forma de organizar posibilidades y resultados",
                "Para representar posibles resultados",
                "Una gráfica de barras",
                "Una operación matemática"
            ).let { base -> (listOf(a) + base.filter { it != a }).distinct().take(4) }

        return MathQuestion(
            q, opciones.shuffled(), a,
            listOf("El diagrama de árbol organiza todas las posibilidades para no dejar ninguna fuera."),
            EX_ARBOL, Curriculum.PROB_ARBOL.id, format
        )
    }
}
