package com.controlparental.kioscosuave.curriculum

import com.controlparental.kioscosuave.MathQuestion
import com.controlparental.kioscosuave.WorkedExample

/**
 * TEMAS · SUCESIONES Y ECUACIONES  (examen de marzo)
 *
 * Errores observados en sucesiones:
 *   · serie 5, 11, 17... se respondió n+4 en vez de 6n−1
 *   · la sucesión 9, 12, 15... en la décima posición se respondió 26 (es 36)
 *   · la máquina que multiplica por 4 se leyó como n+1
 *
 * El patrón del error es claro: se mira solo la DIFERENCIA entre términos y se
 * escribe como si esa diferencia fuera la regla completa, olvidando de dónde
 * arranca la serie. Por eso todos los generadores muestran en los pasos cómo
 * comprobar la regla con el PRIMER término.
 */
internal object Sec1Algebra {

    // ==================================================================
    // Siguiente término
    // ==================================================================
    private val EX_SIGUIENTE = WorkedExample(
        "Ejemplo: siguiente término",
        listOf(
            "Sucesión: 9, 12, 15, ...",
            "1) Busca la diferencia entre términos consecutivos:",
            "   12 − 9 = 3   y   15 − 12 = 3   → siempre suma 3",
            "2) El siguiente es 15 + 3 = 18",
            "Si la diferencia no es constante, prueba si se multiplica."
        )
    )

    fun siguiente(format: ExerciseFormat): MathQuestion {
        val a1 = (2..12).random()
        val d = listOf(2, 3, 4, 5, 6, 7).random()
        val serie = (0..3).map { a1 + it * d }
        val ans = a1 + 4 * d

        return MathQuestion(
            "Observa la sucesión y di qué número sigue:\n\n${serie.joinToString(", ")}, ___",
            Gen.numOpts(
                ans.toDouble(),
                (ans + d).toDouble(),        // error: saltarse un término
                (serie.last() + 1).toDouble(), // error: sumar 1
                (ans - 1).toDouble()
            ),
            ans.toString(),
            listOf(
                "Diferencia entre términos: ${serie[1]} − ${serie[0]} = $d",
                "Se confirma: ${serie[2]} − ${serie[1]} = $d",
                "El siguiente es ${serie.last()} + $d = $ans"
            ),
            EX_SIGUIENTE, Curriculum.SUC_SIGUIENTE.id, format
        )
    }

    // ==================================================================
    // Regla general  ⚠️
    // ==================================================================
    private val EX_REGLA = WorkedExample(
        "Ejemplo: encontrar la regla general",
        listOf(
            "Sucesión: 5, 11, 17, 23, ...",
            "1) La diferencia es 6 → la regla empieza con 6n",
            "2) Prueba con n = 1:  6(1) = 6, pero el primero es 5",
            "3) Sobra 1, así que hay que restarlo:  a(n) = 6n − 1",
            "4) Comprueba con n = 2:  6(2) − 1 = 11 ✓",
            "ERROR COMÚN: escribir solo 'n + 6' porque la diferencia es 6.",
            "La diferencia es el número que MULTIPLICA a n, no el que se suma."
        )
    )

    fun reglaGeneral(format: ExerciseFormat): MathQuestion {
        val d = listOf(2, 3, 4, 5, 6).random()
        val b = listOf(-2, -1, 0, 1, 2, 3).random()
        val serie = (1..4).map { d * it + b }

        fun regla(coef: Int, cte: Int) = when {
            cte == 0 -> "${coef}n"
            cte > 0 -> "${coef}n + $cte"
            else -> "${coef}n − ${-cte}"
        }

        val correcta = regla(d, b)
        val distractores = listOf(
            regla(d, b + 1),
            "n + $d",              // el error observado: confundir diferencia con suma
            regla(d + 1, b)
        ).filter { it != correcta }.distinct()

        return MathQuestion(
            "De la siguiente sucesión, ¿cuál es la regla general que la describe?\n\n${serie.joinToString(", ")}, ...",
            (listOf(correcta) + distractores).take(4).shuffled(),
            correcta,
            listOf(
                "1) Diferencia entre términos: ${serie[1]} − ${serie[0]} = $d → la regla lleva ${d}n",
                "2) Prueba con n = 1: $d × 1 = $d, y el primer término es ${serie[0]}",
                "3) Ajusta la diferencia: $correcta",
                "4) Comprueba con n = 2: ${d * 2 + b} ✓ (coincide con el segundo término)"
            ),
            EX_REGLA, Curriculum.SUC_REGLA_GENERAL.id, format
        )
    }

    // ==================================================================
    // Evaluar la regla  ⚠️
    // ==================================================================
    private val EX_EVALUAR = WorkedExample(
        "Ejemplo: usar la regla",
        listOf(
            "Sucesión 9, 12, 15, ... ¿qué número va en la décima posición?",
            "1) La diferencia es 3, y con n = 1 debe dar 9 → a(n) = 3n + 6",
            "2) Sustituye n = 10:  3(10) + 6 = 36",
            "ERROR COMÚN: contar de 3 en 3 y perderse. La regla evita contar.",
            "Verificación: del término 3 (que vale 15) al 10 hay 7 saltos de 3,",
            "o sea 15 + 21 = 36 ✓"
        )
    )

    fun evaluarRegla(format: ExerciseFormat): MathQuestion {
        val d = listOf(2, 3, 4, 5).random()
        val b = listOf(0, 1, 2, 3, 6).random()

        if (format == ExerciseFormat.INVERSO) {
            // Se da la regla, se piden los primeros términos.
            val regla = if (b == 0) "${d}n" else "${d}n + $b"
            val primeros = (1..4).map { d * it + b }
            val correcta = primeros.joinToString(", ")
            return MathQuestion(
                "Si la regla de una sucesión es a(n) = $regla, ¿cuáles son sus primeros 4 términos?",
                // Ojo: un distractor "d + b·n" colisiona con la respuesta cuando
                // d == b (con d=b=2 ambos dan 4, 6, 8, 10). Se usa en su lugar
                // "empezar en n=2", que nunca coincide.
                listOf(
                    correcta,
                    (1..4).map { d * it + b + 1 }.joinToString(", "),
                    (0..3).map { d * it + b }.joinToString(", "),       // error: empezar en n=0
                    (1..4).map { d * (it + 1) + b }.joinToString(", ")  // error: empezar en n=2
                ).distinct().take(4).shuffled(),
                correcta,
                listOf(
                    "Se sustituye n por 1, 2, 3 y 4:",
                    "n=1 → $d×1${if (b != 0) " + $b" else ""} = ${d + b}",
                    "n=2 → $d×2${if (b != 0) " + $b" else ""} = ${d * 2 + b}",
                    "n=3 → ${d * 3 + b}   |   n=4 → ${d * 4 + b}",
                    "Ojo: las sucesiones empiezan en n = 1, no en n = 0."
                ),
                EX_EVALUAR, Curriculum.SUC_EVALUAR_REGLA.id, format
            )
        }

        val pos = listOf(8, 10, 12, 15).random()
        val serie = (1..3).map { d * it + b }
        val ans = d * pos + b

        return MathQuestion(
            "En la siguiente sucesión numérica, ¿qué número va en la posición $pos?\n\n${serie.joinToString(", ")}, ...",
            Gen.numOpts(
                ans.toDouble(),
                (serie.last() + d * (pos - 3) - d).toDouble(), // error: contar mal los saltos
                (d * pos).toDouble(),                          // error: olvidar la constante
                (serie.last() + pos).toDouble()                // error: sumar la posición
            ),
            ans.toString(),
            listOf(
                "1) La diferencia es $d y el primer término es ${serie[0]} → a(n) = ${d}n${if (b > 0) " + $b" else if (b < 0) " − ${-b}" else ""}",
                "2) Sustituye n = $pos:  $d × $pos${if (b != 0) " + $b" else ""} = $ans",
                "Contar de $d en $d hasta la posición $pos es lento y fácil de errar; la regla lo resuelve directo."
            ),
            EX_EVALUAR, Curriculum.SUC_EVALUAR_REGLA.id, format
        )
    }

    // ==================================================================
    // Sucesiones de figuras  ⚠️
    // ==================================================================
    private val EX_FIGURAL = WorkedExample(
        "Ejemplo: sucesión de figuras",
        listOf(
            "Figura 1: 1 cubo | Figura 2: 4 cubos | Figura 3: 9 cubos",
            "1) Los números son 1, 4, 9: no suben de forma pareja.",
            "2) Fíjate: 1 = 1×1, 4 = 2×2, 9 = 3×3 → cada figura es n × n",
            "3) La regla es a(n) = n²",
            "4) La figura 10 tendría 10² = 100 cubos.",
            "Cuando la diferencia NO es constante, prueba con cuadrados."
        )
    )

    fun figural(format: ExerciseFormat): MathQuestion {
        val tipo = listOf("cuadrado", "lineal", "triangular").random()
        val pieza = listOf("cubos", "cuadritos", "puntos", "palillos").random()
        val pos = listOf(6, 8, 10).random()

        val (serie, ans, reglaTxt) = when (tipo) {
            "cuadrado" -> Triple(
                (1..3).map { it * it },
                pos * pos,
                "a(n) = n²  (cada figura es n × n)"
            )
            "triangular" -> Triple(
                (1..3).map { it * (it + 1) / 2 },
                pos * (pos + 1) / 2,
                "a(n) = n(n+1) ÷ 2  (números triangulares)"
            )
            else -> {
                val m = listOf(3, 4, 5).random()
                Triple(
                    (1..3).map { m * it - (m - 1) },
                    m * pos - (m - 1),
                    "a(n) = ${m}n − ${m - 1}"
                )
            }
        }

        // Singular cuando la figura tiene un solo elemento ("1 cubo", no "1 cubos").
        val descripcion = serie.mapIndexed { i, v ->
            "Figura ${i + 1}: $v ${if (v == 1) pieza.dropLast(1) else pieza}"
        }.joinToString("   |   ")

        return MathQuestion(
            "Observa esta sucesión de figuras:\n\n$descripcion\n\n¿Cuántos $pieza tendrá la figura $pos?",
            Gen.numOpts(
                ans.toDouble(),
                (serie.last() + (pos - 3)).toDouble(),   // error: sumar de uno en uno
                (pos * 2).toDouble(),
                (ans / 2).toDouble()
            ),
            ans.toString(),
            listOf(
                "Los valores son ${serie.joinToString(", ")}",
                "La diferencia no es constante, así que la regla no es una simple suma.",
                "Regla: $reglaTxt",
                "Para la figura $pos: $ans $pieza"
            ),
            EX_FIGURAL, Curriculum.SUC_FIGURAL.id, format
        )
    }

    // ==================================================================
    // Ecuaciones de primer grado
    // ==================================================================
    private val EX_ECUACION = WorkedExample(
        "Ejemplo: ecuación de primer grado",
        listOf(
            "Resuelve:  4x + 8 = 44",
            "1) El 8 está sumando → pasa restando:",
            "     4x = 44 − 8 = 36",
            "2) El 4 está multiplicando → pasa dividiendo:",
            "     x = 36 ÷ 4 = 9",
            "3) Comprueba siempre: 4(9) + 8 = 36 + 8 = 44 ✓"
        )
    )

    fun ecuacionLineal(format: ExerciseFormat): MathQuestion {
        val x = (2..12).random()
        val coef = (2..6).random()
        val cte = (1..15).random()
        val suma = listOf(true, false).random()
        val right = if (suma) coef * x + cte else coef * x - cte
        val signo = if (suma) "+" else "−"

        if (format == ExerciseFormat.INVERSO) {
            val correcta = "${coef}x $signo $cte = $right"
            return MathQuestion(
                "¿Cuál de las siguientes ecuaciones tiene como solución x = $x?",
                listOf(
                    correcta,
                    "${coef}x $signo $cte = ${right + coef}",
                    "${coef + 1}x $signo $cte = $right",
                    "${coef}x $signo ${cte + 2} = $right"
                ).distinct().take(4).shuffled(),
                correcta,
                listOf(
                    "Se sustituye x = $x en cada opción y se ve cuál da la igualdad.",
                    "$coef × $x = ${coef * x}, y ${coef * x} $signo $cte = $right ✓"
                ),
                EX_ECUACION, Curriculum.EC_LINEAL.id, format
            )
        }

        return MathQuestion(
            "Resuelve para x:   ${coef}x $signo $cte = $right",
            Gen.numOpts(
                x.toDouble(),
                (right.toDouble() / coef),                     // error: olvidar la constante
                ((if (suma) right + cte else right - cte).toDouble() / coef), // error: signo al pasar
                (x + 1).toDouble()
            ),
            x.toString(),
            listOf(
                "1) El $cte está ${if (suma) "sumando → pasa restando" else "restando → pasa sumando"}:",
                "     ${coef}x = $right ${if (suma) "−" else "+"} $cte = ${coef * x}",
                "2) El $coef está multiplicando → pasa dividiendo:",
                "     x = ${coef * x} ÷ $coef = $x",
                "3) Comprueba: $coef($x) $signo $cte = $right ✓"
            ),
            EX_ECUACION, Curriculum.EC_LINEAL.id, format
        )
    }

    private val EX_EC_PROBLEMA = WorkedExample(
        "Ejemplo: problema con ecuación",
        listOf(
            "Pienso un número, lo multiplico por 3 y le sumo 7; obtengo 22.",
            "1) Ponle nombre a lo desconocido:  sea n el número",
            "2) Traduce la frase a una ecuación:  3n + 7 = 22",
            "3) Resuelve:  3n = 22 − 7 = 15  →  n = 15 ÷ 3 = 5",
            "4) Comprueba con la frase original: 5 × 3 = 15, más 7 = 22 ✓"
        )
    )

    fun ecuacionProblema(format: ExerciseFormat): MathQuestion {
        val x = (2..12).random()
        val m = (2..5).random()
        val b = (1..10).random()
        val result = m * x + b
        val name = Gen.NAMES.random()

        val (question, pasos) = listOf(
            Pair(
                "Pienso un número, lo multiplico por $m y le sumo $b; obtengo $result. ¿Qué número pensé?",
                listOf(
                    "Sea n el número:  ${m}n + $b = $result",
                    "${m}n = $result − $b = ${m * x}",
                    "n = ${m * x} ÷ $m = $x"
                )
            ),
            Pair(
                "$name compró $m libros iguales y pagó \$$b de envío. En total gastó \$$result. ¿Cuánto costó cada libro?",
                listOf(
                    "Sea p el precio de cada libro:  ${m}p + $b = $result",
                    "Quita el envío: ${m}p = $result − $b = ${m * x}",
                    "Divide entre la cantidad: p = ${m * x} ÷ $m = $x"
                )
            ),
            Pair(
                "La edad de $name multiplicada por $m, más $b, da $result. ¿Cuántos años tiene?",
                listOf(
                    "Sea e la edad:  ${m}e + $b = $result",
                    "${m}e = $result − $b = ${m * x}",
                    "e = ${m * x} ÷ $m = $x años"
                )
            )
        ).random()

        return MathQuestion(
            question,
            Gen.numOpts(
                x.toDouble(),
                (result.toDouble() / m),          // error: no quitar la constante
                (result - b).toDouble(),          // error: quedarse a medio camino
                (x + 2).toDouble()
            ),
            x.toString(),
            pasos + "Comprueba: $m × $x + $b = $result ✓",
            EX_EC_PROBLEMA, Curriculum.EC_PROBLEMA.id, format
        )
    }
}
