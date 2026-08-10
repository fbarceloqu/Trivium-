package com.controlparental.kioscosuave.curriculum

import com.controlparental.kioscosuave.MathQuestion
import com.controlparental.kioscosuave.WorkedExample

/**
 * TEMAS · DECIMALES, NÚMEROS CON SIGNO Y PROPORCIONALIDAD  (examen diagnóstico)
 *
 * Errores observados:
 *   · 1.8 ÷ 2 = 8   y   3.85 × 5 = 25.19  (el punto decimal se pierde)
 *   · −5 + 3 + 8 − 10 = 4  y  4 + (−3) = −1  (signos)
 *   · regla de tres: 9 L por cada 100 km → en 350 km se respondió 9 L
 */
internal object Sec1Aritmetica {

    // ==================================================================
    // Operaciones con decimales
    // ==================================================================
    private val EX_DEC_OP = WorkedExample(
        "Ejemplo: operar con decimales",
        listOf(
            "MULTIPLICAR 3.85 × 5:",
            "  Multiplica sin el punto: 385 × 5 = 1925",
            "  3.85 tiene 2 decimales → el resultado también: 19.25",
            "DIVIDIR 1.8 ÷ 2:",
            "  18 ÷ 2 = 9, y como 1.8 tiene 1 decimal → 0.9",
            "Comprueba con lógica: la mitad de 1.8 no puede ser 8."
        )
    )

    fun decimalesOperaciones(format: ExerciseFormat): MathQuestion {
        val op = listOf("mult", "div", "suma", "resta").random()
        val a = listOf(1.8, 2.5, 3.85, 4.2, 0.64, 7.5, 12.6, 0.75).random()
        val b = listOf(2, 3, 4, 5).random()

        val (question, ans, pasos) = when (op) {
            "mult" -> Triple(
                "¿Cuánto es ${Gen.fmt(a)} × $b?",
                a * b,
                listOf(
                    "Multiplica ignorando el punto y luego colócalo.",
                    "${Gen.fmt(a)} × $b = ${Gen.fmt(a * b)}"
                )
            )
            "div" -> Triple(
                "¿Cuánto es ${Gen.fmt(a)} ÷ $b?",
                a / b,
                listOf(
                    "Divide como si no hubiera punto y luego colócalo.",
                    "${Gen.fmt(a)} ÷ $b = ${Gen.fmt(a / b, 4)}",
                    "Verifica con lógica: el resultado debe ser MENOR que ${Gen.fmt(a)}."
                )
            )
            "suma" -> Triple(
                "¿Cuánto es ${Gen.fmt(a)} + ${Gen.fmt(a / 2)}?",
                a + a / 2,
                listOf(
                    "Alinea los puntos decimales uno debajo del otro y suma.",
                    "${Gen.fmt(a)} + ${Gen.fmt(a / 2)} = ${Gen.fmt(a + a / 2, 3)}"
                )
            )
            else -> Triple(
                "Una jarra vacía pesa ${Gen.fmt(a / 2)} kg y llena de agua pesa ${Gen.fmt(a)} kg. ¿Cuánto pesa el agua?",
                a - a / 2,
                listOf(
                    "Al peso lleno se le quita el peso de la jarra vacía.",
                    "${Gen.fmt(a)} − ${Gen.fmt(a / 2)} = ${Gen.fmt(a - a / 2, 3)} kg"
                )
            )
        }

        return MathQuestion(
            question,
            Gen.numOptsD(
                3, ans,
                ans * 10,        // error: punto decimal corrido
                ans / 10,
                ans + b          // error: operar mal
            ),
            Gen.fmt(ans, 3),
            pasos,
            EX_DEC_OP, Curriculum.DEC_OPERACIONES.id, format
        )
    }

    // ==================================================================
    // Problemas con decimales
    // ==================================================================
    private val EX_DEC_PROB = WorkedExample(
        "Ejemplo: problema con decimales",
        listOf(
            "Un coche mide 3.85 m. ¿Cuánto ocupan 5 coches en fila?",
            "1) Identifica la operación: se repite 5 veces → multiplicar",
            "2) 3.85 × 5 = 19.25 metros",
            "Truco: estima primero. 4 × 5 = 20, así que 19.25 tiene sentido; 25.19 no."
        )
    )

    fun decimalesProblemas(format: ExerciseFormat): MathQuestion {
        val name = Gen.NAMES.random()
        return when (listOf("coches", "tubo", "cambio", "gasto").random()) {
            "coches" -> {
                val largo = listOf(3.85, 4.2, 2.75, 5.1).random()
                val n = (3..6).random()
                val ans = largo * n
                MathQuestion(
                    "Un coche mide aproximadamente ${Gen.fmt(largo)} metros. ¿Cuál es la distancia mínima que ocuparían $n coches iguales puestos uno junto al otro?",
                    Gen.numOptsD(3, ans, largo + n, ans / n, ans * 10),
                    Gen.fmt(ans, 3),
                    listOf(
                        "Son $n coches del mismo largo, así que se multiplica:",
                        "${Gen.fmt(largo)} × $n = ${Gen.fmt(ans, 3)} metros"
                    ),
                    EX_DEC_PROB, Curriculum.DEC_PROBLEMAS.id, format
                )
            }
            "tubo" -> {
                val largo = listOf(1.8, 2.4, 3.6, 4.5).random()
                val partes = listOf(2, 3).random()
                val ans = largo / partes
                MathQuestion(
                    "Para terminar una instalación, un plomero necesita cortar un tubo de ${Gen.fmt(largo)} m en $partes partes iguales. ¿Cuánto mide cada tramo?",
                    Gen.numOptsD(3, ans, largo * partes, ans * 10, largo - partes),
                    Gen.fmt(ans, 3),
                    listOf(
                        "Repartir en partes iguales significa DIVIDIR:",
                        "${Gen.fmt(largo)} ÷ $partes = ${Gen.fmt(ans, 3)} metros",
                        "Verifica: cada tramo debe ser menor que el tubo completo."
                    ),
                    EX_DEC_PROB, Curriculum.DEC_PROBLEMAS.id, format
                )
            }
            "cambio" -> {
                val paga = listOf(25.0, 50.0, 100.0, 20.0).random()
                val cuesta = listOf(15.75, 8.4, 33.6, 12.25).filter { it < paga }.random()
                val ans = paga - cuesta
                MathQuestion(
                    "$name tiene \$${Gen.fmt(paga)} y quiere comprar algo de \$${Gen.fmt(cuesta)}. ¿Cuánto le darán de cambio?",
                    Gen.numOptsD(2, ans, paga + cuesta, cuesta, paga),
                    Gen.fmt(ans, 2),
                    listOf(
                        "Al dinero que lleva se le resta lo que cuesta:",
                        "${Gen.fmt(paga)} − ${Gen.fmt(cuesta)} = ${Gen.fmt(ans, 2)}"
                    ),
                    EX_DEC_PROB, Curriculum.DEC_PROBLEMAS.id, format
                )
            }
            else -> {
                val precio = listOf(12.5, 8.75, 15.4, 6.25).random()
                val n = (3..8).random()
                val ans = precio * n
                MathQuestion(
                    "$name compró $n cuadernos de \$${Gen.fmt(precio)} cada uno. ¿Cuánto pagó en total?",
                    Gen.numOptsD(2, ans, precio + n, ans / 2, precio),
                    Gen.fmt(ans, 2),
                    listOf(
                        "$n cuadernos al mismo precio: se multiplica.",
                        "${Gen.fmt(precio)} × $n = ${Gen.fmt(ans, 2)}"
                    ),
                    EX_DEC_PROB, Curriculum.DEC_PROBLEMAS.id, format
                )
            }
        }
    }

    // ==================================================================
    // Equivalencias fracción / decimal / porcentaje
    // ==================================================================
    private val EX_EQUIV = WorkedExample(
        "Ejemplo: tres formas del mismo número",
        listOf(
            "3/5  =  0.6  =  60%   son EL MISMO valor escrito distinto.",
            "De fracción a decimal: divide → 3 ÷ 5 = 0.6",
            "De decimal a porcentaje: multiplica por 100 → 0.6 × 100 = 60%",
            "De porcentaje a decimal: divide entre 100 → 60 ÷ 100 = 0.6"
        )
    )

    fun equivalencias(format: ExerciseFormat): MathQuestion {
        val (num, den) = listOf(1 to 2, 1 to 4, 3 to 4, 1 to 5, 2 to 5, 3 to 5, 4 to 5, 1 to 10, 7 to 10)
            .random()
        val dec = num.toDouble() / den
        val porc = dec * 100

        if (format == ExerciseFormat.CONCEPTUAL) {
            // El reactivo del examen: encontrar el intruso.
            val intruso = Gen.fmt(dec, 3) + "0"   // p. ej. 0.660 frente a 0.6
            val distinto = Gen.fmt(dec + 0.06, 3)
            return MathQuestion(
                "¿Cuál de las siguientes cantidades NO es equivalente a las demás?",
                listOf("$num/$den", Gen.fmt(dec), Gen.pct(porc), distinto).shuffled(),
                distinto,
                listOf(
                    "$num/$den = ${Gen.fmt(dec)} = ${Gen.pct(porc)} son el mismo valor.",
                    "$distinto es un número diferente.",
                    "Cuidado: $intruso sí sería igual a ${Gen.fmt(dec)}; los ceros a la derecha no cambian el valor."
                ),
                EX_EQUIV, Curriculum.DEC_EQUIVALENCIAS.id, format
            )
        }

        return MathQuestion(
            "¿A qué porcentaje equivale la fracción $num/$den?",
            Gen.numOptsSuffix("%", porc, dec, num.toDouble() * den, 100 - porc),
            Gen.pct(porc),
            listOf(
                "1) Pasa la fracción a decimal: $num ÷ $den = ${Gen.fmt(dec)}",
                "2) Pasa el decimal a porcentaje: ${Gen.fmt(dec)} × 100 = ${Gen.pct(porc)}"
            ),
            EX_EQUIV, Curriculum.DEC_EQUIVALENCIAS.id, format
        )
    }

    // ==================================================================
    // Enteros con signo  ⚠️
    // ==================================================================
    private val EX_ENTEROS = WorkedExample(
        "Ejemplo: sumar y restar con signo",
        listOf(
            "−5 + 3 + 8 − 10 = ?",
            "Truco: junta los positivos por un lado y los negativos por otro.",
            "  Positivos: 3 + 8 = 11",
            "  Negativos: −5 − 10 = −15",
            "  Ahora réstalos: 11 − 15 = −4",
            "Piénsalo como dinero: ganaste 11 y debes 15 → quedas debiendo 4."
        )
    )

    fun enterosSumaResta(format: ExerciseFormat): MathQuestion {
        if (format == ExerciseFormat.INVERSO) {
            val a = listOf(-9, -7, -5, -3, 4, 6).random()
            val ans = listOf(-6, -2, 2, 5, 8).random()
            val b = ans - a
            return MathQuestion(
                "Completa la operación:   $a + ___ = $ans",
                Gen.numOpts(b.toDouble(), (ans + a).toDouble(), (a - ans).toDouble(), (-b).toDouble()),
                b.toString(),
                listOf(
                    "Para despejar, se pasa el $a al otro lado con signo contrario:",
                    "___ = $ans − ($a) = ${ans - a}",
                    "Comprueba: $a + ${b} = $ans ✓"
                ),
                EX_ENTEROS, Curriculum.ENT_SUMA_RESTA.id, format
            )
        }

        return if (format == ExerciseFormat.RAZONAMIENTO) {
            val nums = listOf(
                listOf(-5, 3, 8, -10),
                listOf(-7, 2, -4, 6),
                listOf(4, -9, 5, -3),
                listOf(-2, -6, 12, -1)
            ).random()
            val ans = nums.sum()
            // Guion ASCII a propósito: es el mismo que aparece en las opciones
            // (que salen de Gen.fmt), y mezclarlo con el signo menos tipográfico
            // haría que el enunciado y las respuestas se vieran distintos.
            val expr = nums.mapIndexed { i, n ->
                if (i == 0) n.toString() else if (n < 0) "- ${-n}" else "+ $n"
            }.joinToString(" ")
            val positivos = nums.filter { it > 0 }
            val negativos = nums.filter { it < 0 }
            MathQuestion(
                "¿Cuál es el resultado de:   $expr ?",
                Gen.numOpts(
                    ans.toDouble(),
                    (-ans).toDouble(),                       // error: signo invertido
                    nums.sumOf { kotlin.math.abs(it) }.toDouble(), // error: ignorar signos
                    (ans + 2).toDouble()
                ),
                ans.toString(),
                listOf(
                    "Junta los positivos: ${positivos.joinToString(" + ")} = ${positivos.sum()}",
                    "Junta los negativos: ${negativos.joinToString(" ")} = ${negativos.sum()}",
                    "Ahora súmalos: ${positivos.sum()} + (${negativos.sum()}) = $ans"
                ),
                EX_ENTEROS, Curriculum.ENT_SUMA_RESTA.id, format
            )
        } else {
            val a = listOf(4, 7, -3, -8, 5, -6).random()
            val b = listOf(-3, -5, 2, -9, 6, -2).random()
            val ans = a + b
            MathQuestion(
                "¿Cuál es el resultado de   $a + (${b})?",
                Gen.numOpts(
                    ans.toDouble(),
                    (a - b).toDouble(),                    // error: restar
                    (-ans).toDouble(),                     // error: signo invertido
                    (kotlin.math.abs(a) + kotlin.math.abs(b)).toDouble()
                ),
                ans.toString(),
                listOf(
                    if (b < 0) "Sumar un negativo es lo mismo que restar: $a + ($b) = $a − ${-b}"
                    else "Los dos son positivos, se suman normal.",
                    "Resultado: $ans"
                ),
                EX_ENTEROS, Curriculum.ENT_SUMA_RESTA.id, format
            )
        }
    }

    private val EX_REPRESENTAR = WorkedExample(
        "Ejemplo: representar con signo",
        listOf(
            "Los números con signo sirven para indicar DIRECCIÓN u OPUESTOS:",
            "  Ganar / subir / sobre el nivel del mar  →  POSITIVO (+)",
            "  Perder / bajar / bajo el nivel del mar  →  NEGATIVO (−)",
            "Óscar ganó $200  →  +200",
            "Un buzo bajó 30 metros  →  −30"
        )
    )

    fun enterosRepresentar(format: ExerciseFormat): MathQuestion {
        val n = listOf(15, 30, 45, 120, 200, 350).random()
        val (situacion, ans) = listOf(
            "Óscar ganó \$$n" to "+$n",
            "Un buzo descendió $n metros bajo el nivel del mar" to "-$n",
            "La temperatura subió $n grados" to "+$n",
            "Una empresa perdió \$$n" to "-$n",
            "Un avión vuela a $n metros sobre el nivel del mar" to "+$n",
            "Un elevador bajó $n pisos" to "-$n"
        ).random()

        return MathQuestion(
            "¿Cómo se representa con número y signo la siguiente situación?\n\n$situacion",
            listOf("+$n", "-$n", "+${n * 2}", "-${n / 2}").shuffled(),
            ans,
            listOf(
                "Ganar, subir o estar por encima → signo POSITIVO (+)",
                "Perder, bajar o estar por debajo → signo NEGATIVO (−)",
                "En este caso: $ans"
            ),
            EX_REPRESENTAR, Curriculum.ENT_REPRESENTAR.id, format
        )
    }

    // ==================================================================
    // Regla de tres  ⚠️
    // ==================================================================
    private val EX_REGLA3 = WorkedExample(
        "Ejemplo: regla de tres",
        listOf(
            "Un coche gasta 9 L cada 100 km. ¿Cuánto gasta en 350 km?",
            "1) Acomoda lo que sabes:   100 km → 9 L",
            "2) Lo que buscas:          350 km → ?",
            "3) Multiplica en cruz y divide:  (350 × 9) ÷ 100 = 31.5 L",
            "Verifica con lógica: 350 km es más del triple de 100, así que",
            "el resultado tiene que ser bastante MÁS de 9 L."
        )
    )

    fun reglaDeTres(format: ExerciseFormat): MathQuestion {
        return when (listOf("gasolina", "canciones", "receta").random()) {
            "gasolina" -> {
                val litros = listOf(6, 8, 9, 12).random()
                val km = 100
                val kmNuevo = listOf(250, 350, 450, 500).random()
                val ans = litros.toDouble() * kmNuevo / km
                MathQuestion(
                    "Un automóvil consume $litros L de gasolina cada $km km. ¿Cuántos litros consumirá en un viaje de $kmNuevo km?",
                    Gen.numOptsD(2, ans, litros.toDouble(), ans / 10, ans * 10),
                    Gen.fmt(ans, 2),
                    listOf(
                        "$km km → $litros L",
                        "$kmNuevo km → ?",
                        "($kmNuevo × $litros) ÷ $km = ${Gen.fmt(ans, 2)} L",
                        "Lógica: $kmNuevo km es más que $km km, así que debe gastar MÁS de $litros L."
                    ),
                    EX_REGLA3, Curriculum.PROP_REGLA_TRES.id, format
                )
            }
            "canciones" -> {
                val unidad = listOf(15, 17, 20, 22).random()
                val n1 = listOf(12, 14, 15).random()
                val n2 = listOf(8, 9, 11).random()
                val total1 = unidad * n1
                val ans = unidad.toDouble() * n2
                MathQuestion(
                    "A ${Gen.NAMES.random()} le cobraron \$$total1 por descargar $n1 canciones. ¿Cuánto le cobrarían por $n2 canciones?",
                    Gen.numOptsD(2, ans, total1.toDouble(), ans + unidad, total1.toDouble() * n2 / 10),
                    Gen.fmt(ans, 2),
                    listOf(
                        "1) Precio de UNA canción: $total1 ÷ $n1 = $unidad",
                        "2) Precio de $n2 canciones: $unidad × $n2 = ${Gen.fmt(ans, 2)}",
                        "Lógica: $n2 es menos que $n1, así que debe costar MENOS de \$$total1."
                    ),
                    EX_REGLA3, Curriculum.PROP_REGLA_TRES.id, format
                )
            }
            else -> {
                val personas1 = listOf(4, 6, 8).random()
                val gramos = listOf(150, 200, 250).random()
                val personas2 = listOf(10, 12, 15).random()
                val ans = gramos.toDouble() * personas2 / personas1
                MathQuestion(
                    "Una receta para $personas1 personas lleva $gramos g de harina. ¿Cuánta harina se necesita para $personas2 personas?",
                    Gen.numOptsD(2, ans, gramos.toDouble(), ans / 2, gramos.toDouble() * personas1 / personas2),
                    Gen.fmt(ans, 2),
                    listOf(
                        "$personas1 personas → $gramos g",
                        "$personas2 personas → ?",
                        "($personas2 × $gramos) ÷ $personas1 = ${Gen.fmt(ans, 2)} g"
                    ),
                    EX_REGLA3, Curriculum.PROP_REGLA_TRES.id, format
                )
            }
        }
    }

    private val EX_UNITARIO = WorkedExample(
        "Ejemplo: valor unitario",
        listOf(
            "12 cuadernos cuestan $180. ¿Cuánto cuesta uno?",
            "Valor unitario = total ÷ cantidad",
            "180 ÷ 12 = $15 cada cuaderno",
            "Sirve para comparar precios: siempre lleva a 'cuánto cuesta UNO'."
        )
    )

    fun valorUnitario(format: ExerciseFormat): MathQuestion {
        val unidad = listOf(12, 15, 17, 18, 24).random()
        val cantidad = listOf(6, 8, 12, 14).random()
        val total = unidad * cantidad
        val cosa = listOf("cuadernos", "plumas", "libros", "camisetas", "boletos").random()

        return MathQuestion(
            "Si $cantidad $cosa cuestan \$$total en total, ¿cuánto cuesta cada uno?",
            Gen.numOpts(
                unidad.toDouble(),
                total.toDouble(),                    // error: dar el total
                (total - cantidad).toDouble(),       // error: restar
                total.toDouble() * cantidad          // error: multiplicar
            ),
            unidad.toString(),
            listOf(
                "El valor unitario se obtiene dividiendo el total entre la cantidad:",
                "$total ÷ $cantidad = $unidad",
                "Comprueba: $unidad × $cantidad = $total ✓"
            ),
            EX_UNITARIO, Curriculum.PROP_VALOR_UNITARIO.id, format
        )
    }
}
