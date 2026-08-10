package com.controlparental.kioscosuave.curriculum

import com.controlparental.kioscosuave.MathQuestion
import com.controlparental.kioscosuave.WorkedExample

/**
 * TEMA · NÚMEROS Y FRACCIONES  (examen de septiembre)
 *
 * Errores observados, todos de CLASIFICACIÓN más que de cálculo:
 *   · "finito" e "infinito periódico" invertidos (se marcó 23.6565... como finito)
 *   · "propia" e "impropia" invertidas
 *   · fracciones equivalentes: se eligió 6/2 como equivalente de 6/3
 *   · al pedir cuánto FALTA para el viaje se respondió lo que YA se tiene
 */
internal object Sec1Numeros {

    // ==================================================================
    // Tipos de número
    // ==================================================================
    private val EX_TIPOS = WorkedExample(
        "Ejemplo: conjuntos de números",
        listOf(
            "NATURALES → los de contar: 1, 2, 3, 4...",
            "ENTEROS   → los naturales más el cero y los negativos: ...−2, −1, 0, 1, 2...",
            "RACIONALES→ los que SÍ se pueden escribir como fracción: 3/4, 0.5, 7",
            "IRRACIONALES → los que NO se pueden escribir como fracción: π, √2",
            "Truco: 'irracional' = no hay razón (fracción) que lo represente."
        )
    )

    fun tipos(format: ExerciseFormat): MathQuestion {
        if (format == ExerciseFormat.CONCEPTUAL) {
            val (q, a) = listOf(
                "¿Cómo se le llama al conjunto de los números que utilizamos para contar?" to "Naturales",
                "¿Cómo se le llama a los números que NO se pueden escribir como una fracción?" to "Irracionales",
                "¿Cómo se le llama a los números que SÍ se pueden escribir como una fracción?" to "Racionales",
                "¿Cómo se le llama al conjunto que incluye los negativos, el cero y los positivos, sin decimales?" to "Enteros"
            ).random()
            return MathQuestion(
                q,
                listOf("Naturales", "Enteros", "Racionales", "Irracionales").shuffled(),
                a,
                listOf("Naturales: para contar | Enteros: con negativos | Racionales: se escriben como fracción | Irracionales: no se pueden"),
                EX_TIPOS, Curriculum.NUM_TIPOS.id, format
            )
        }

        val irracional = listOf("π", "√2", "√3", "√5", "√7").random()
        return MathQuestion(
            "¿Cuál de los siguientes es un número irracional?",
            Gen.opts(irracional, "0.75", "3/4", "0.5", "12").take(4).shuffled(),
            irracional,
            listOf(
                "Un número es irracional si NO se puede escribir como fracción exacta.",
                "0.75, 3/4, 0.5 y 12 sí se pueden: son racionales.",
                "$irracional tiene infinitas cifras que nunca se repiten en un patrón."
            ),
            EX_TIPOS, Curriculum.NUM_TIPOS.id, format
        )
    }

    // ==================================================================
    // Decimal finito vs. infinito periódico  ⚠️
    // ==================================================================
    private val EX_FINITO = WorkedExample(
        "Ejemplo: finito o infinito",
        listOf(
            "FINITO → se acaba. Tiene un número exacto de cifras: 0.67, 2.5, 0.125",
            "INFINITO PERIÓDICO → nunca se acaba, se repite: 0.888..., 1.4555...",
            "Los puntos suspensivos (...) son la pista: significan 'y sigue'.",
            "Ojo: 23.6565... NO es finito aunque tenga muchas cifras; se repite el 65."
        )
    )

    fun finitoInfinito(format: ExerciseFormat): MathQuestion {
        val finitos = listOf("0.67", "2.5", "0.125", "4.8", "0.25", "13.4")
        val infinitos = listOf("1.4555...", "0.888...", "23.6565...", "0.333...", "2.7171...", "5.1666...")

        return if (format == ExerciseFormat.CONCEPTUAL || Gen.NAMES.indices.random() % 2 == 0) {
            val correcto = finitos.random()
            MathQuestion(
                "¿Cuál de los siguientes números es finito?",
                (listOf(correcto) + infinitos.shuffled().take(3)).shuffled(),
                correcto,
                listOf(
                    "Un decimal FINITO se acaba: tiene un número exacto de cifras.",
                    "Los que llevan '...' siguen para siempre: son infinitos periódicos.",
                    "$correcto se acaba, así que es finito."
                ),
                EX_FINITO, Curriculum.NUM_FINITO_INFINITO.id, format
            )
        } else {
            val correcto = infinitos.random()
            MathQuestion(
                "¿Cuál de los siguientes números es infinito periódico?",
                (listOf(correcto) + finitos.shuffled().take(3)).shuffled(),
                correcto,
                listOf(
                    "Un decimal INFINITO PERIÓDICO nunca se acaba y repite un patrón.",
                    "Los puntos suspensivos (...) lo indican.",
                    "$correcto es el único que sigue para siempre."
                ),
                EX_FINITO, Curriculum.NUM_FINITO_INFINITO.id, format
            )
        }
    }

    // ==================================================================
    // Primos, divisores y múltiplos
    // ==================================================================
    private val EX_PRIMOS = WorkedExample(
        "Ejemplo: divisores y múltiplos",
        listOf(
            "DIVISOR de 12 → un número que cabe exacto en 12: 1, 2, 3, 4, 6, 12",
            "MÚLTIPLO de 12 → 12 multiplicado por algo: 12, 24, 36, 48...",
            "PRIMO → solo se puede dividir entre 1 y entre sí mismo: 2, 3, 5, 7, 11",
            "Ojo: el 2 es el ÚNICO primo que además es par."
        )
    )

    fun primos(format: ExerciseFormat): MathQuestion {
        return when (listOf("primo_par", "divisor", "multiplo", "es_primo").random()) {
            "primo_par" -> MathQuestion(
                "¿Cuál es el único número que es par y primo a la vez?",
                Gen.numOpts(2.0, 1.0, 4.0, 5.0),
                "2",
                listOf(
                    "Un número primo solo se divide entre 1 y entre sí mismo.",
                    "Todo par (excepto el 2) se divide también entre 2, así que no es primo.",
                    "El 2 es el único par que se salva: es primo."
                ),
                EX_PRIMOS, Curriculum.NUM_PRIMOS.id, format
            )

            "divisor" -> {
                val n = listOf(12, 18, 20, 24, 30).random()
                val divisores = (1..n).filter { n % it == 0 }
                val noDivisor = (2..12).filter { n % it != 0 }.random()
                MathQuestion(
                    "¿Cuál de los siguientes números NO es divisor de $n?",
                    Gen.numOpts(
                        noDivisor.toDouble(),
                        *divisores.filter { it in 2..n }.shuffled().take(3)
                            .map { it.toDouble() }.toDoubleArray()
                    ),
                    noDivisor.toString(),
                    listOf(
                        "Los divisores de $n son: ${divisores.joinToString(", ")}",
                        "$n ÷ $noDivisor no da un resultado exacto, así que $noDivisor no es divisor."
                    ),
                    EX_PRIMOS, Curriculum.NUM_PRIMOS.id, format
                )
            }

            "multiplo" -> {
                val n = listOf(4, 6, 8, 12, 16).random()
                val k = (2..6).random()
                val multiplo = n * k
                MathQuestion(
                    "¿Cuál de los siguientes números es múltiplo de $n?",
                    Gen.numOpts(
                        multiplo.toDouble(),
                        (n - 1).toDouble(),
                        (n / 2).toDouble(),
                        (multiplo + 1).toDouble()
                    ),
                    multiplo.toString(),
                    listOf(
                        "Un múltiplo de $n es $n multiplicado por algún número entero.",
                        "$n × $k = $multiplo",
                        "Comprueba: $multiplo ÷ $n = $k, exacto ✓"
                    ),
                    EX_PRIMOS, Curriculum.NUM_PRIMOS.id, format
                )
            }

            else -> {
                val primo = listOf(2, 3, 5, 7, 11, 13, 17, 19, 23).random()
                val compuestos = listOf(4, 6, 8, 9, 10, 12, 14, 15, 16, 21, 25)
                    .shuffled().take(3)
                MathQuestion(
                    "¿Cuál de los siguientes números es primo?",
                    Gen.numOpts(
                        primo.toDouble(),
                        *compuestos.map { it.toDouble() }.toDoubleArray()
                    ),
                    primo.toString(),
                    listOf(
                        "Un primo solo se divide exacto entre 1 y entre sí mismo.",
                        "${compuestos.joinToString(", ")} tienen más divisores.",
                        "$primo es primo."
                    ),
                    EX_PRIMOS, Curriculum.NUM_PRIMOS.id, format
                )
            }
        }
    }

    // ==================================================================
    // Potencias y raíces
    // ==================================================================
    private val EX_POTENCIAS = WorkedExample(
        "Ejemplo: potencias y raíces",
        listOf(
            "POTENCIA: 10³ = 10 × 10 × 10 = 1000",
            "RAÍZ CUADRADA: √64 = 8, porque 8 × 8 = 64",
            "RAÍZ CÚBICA: ∛27 = 3, porque 3 × 3 × 3 = 27",
            "La raíz es la operación contraria de la potencia."
        )
    )

    fun potencias(format: ExerciseFormat): MathQuestion {
        return when (listOf("pot10", "raiz2", "raiz3").random()) {
            "pot10" -> {
                val e = (2..5).random()
                val v = Math.pow(10.0, e.toDouble())
                MathQuestion(
                    if (format == ExerciseFormat.INVERSO)
                        "¿Cuál de las siguientes cantidades es una potencia de 10?"
                    else
                        "¿Cuánto es 10 elevado a la $e?",
                    Gen.numOpts(v, v * 2, v / 2, v + 10),
                    Gen.fmt(v),
                    listOf(
                        "10^$e significa multiplicar 10 por sí mismo $e veces.",
                        "10^$e = ${Gen.fmt(v)}",
                        "Atajo: el exponente dice cuántos ceros lleva."
                    ),
                    EX_POTENCIAS, Curriculum.NUM_POTENCIAS.id, format
                )
            }

            "raiz2" -> {
                val r = (2..12).random()
                val n = r * r
                MathQuestion(
                    "¿Cuál es la raíz cuadrada de $n?",
                    Gen.numOpts(r.toDouble(), (n / 2).toDouble(), (r + 1).toDouble(), (r * 2).toDouble()),
                    r.toString(),
                    listOf(
                        "Buscas el número que multiplicado por sí mismo dé $n.",
                        "$r × $r = $n",
                        "√$n = $r"
                    ),
                    EX_POTENCIAS, Curriculum.NUM_POTENCIAS.id, format
                )
            }

            else -> {
                val r = (2..6).random()
                val n = r * r * r
                MathQuestion(
                    "¿Cuál es la raíz cúbica de $n?",
                    Gen.numOpts(r.toDouble(), (r * r).toDouble(), (n / 3).toDouble(), (r + 1).toDouble()),
                    r.toString(),
                    listOf(
                        "Buscas el número que multiplicado TRES veces por sí mismo dé $n.",
                        "$r × $r × $r = $n",
                        "∛$n = $r"
                    ),
                    EX_POTENCIAS, Curriculum.NUM_POTENCIAS.id, format
                )
            }
        }
    }

    // ==================================================================
    // Clasificar fracciones  ⚠️
    // ==================================================================
    private val EX_FRAC_TIPO = WorkedExample(
        "Ejemplo: tipos de fracción",
        listOf(
            "PROPIA   → el de arriba es MENOR: 3/8, 2/5, 1/2  (vale menos que 1)",
            "IMPROPIA → el de arriba es MAYOR: 5/2, 10/6, 7/3 (vale más que 1)",
            "MIXTA    → un entero y una fracción juntos: 2 1/3",
            "DECIMAL  → el de abajo es 10, 100, 1000...: 2/10, 45/100",
            "Truco: PROPIA es la 'normal', la que sí cabe en el pastel."
        )
    )

    fun fraccionTipo(format: ExerciseFormat): MathQuestion {
        if (format == ExerciseFormat.CONCEPTUAL) {
            val (q, a) = listOf(
                "¿Cómo se le llama a la fracción donde el numerador es menor que el denominador?" to "Propia",
                "¿Cómo se le llama a la fracción donde el numerador es mayor que el denominador?" to "Impropia",
                "¿Cómo se le llama a la fracción cuyo denominador es 10, 100 o 1000?" to "Decimal",
                "¿Cómo se le llama a la combinación de un número entero con una fracción?" to "Mixta"
            ).random()
            return MathQuestion(
                q,
                listOf("Propia", "Impropia", "Mixta", "Decimal").shuffled(),
                a,
                listOf(
                    "Numerador = el de ARRIBA. Denominador = el de ABAJO.",
                    "Arriba menor → propia. Arriba mayor → impropia."
                ),
                EX_FRAC_TIPO, Curriculum.FRAC_PROPIA.id, format
            )
        }

        val propias = listOf("3/8", "2/5", "1/2", "4/9", "5/7")
        val impropias = listOf("5/2", "10/6", "7/3", "8/4", "9/5")
        val buscaPropia = listOf(true, false).random()
        val correcta = if (buscaPropia) propias.random() else impropias.random()
        val otras = (if (buscaPropia) impropias else propias).shuffled().take(3)

        return MathQuestion(
            "¿Cuál de las siguientes representa una fracción ${if (buscaPropia) "propia" else "impropia"}?",
            (listOf(correcta) + otras).shuffled(),
            correcta,
            listOf(
                if (buscaPropia)
                    "En una fracción PROPIA el numerador (arriba) es MENOR que el denominador (abajo)."
                else
                    "En una fracción IMPROPIA el numerador (arriba) es MAYOR que el denominador (abajo).",
                "En $correcta se cumple."
            ),
            EX_FRAC_TIPO, Curriculum.FRAC_PROPIA.id, format
        )
    }

    // ==================================================================
    // Fracciones equivalentes  ⚠️
    // ==================================================================
    private val EX_EQUIV = WorkedExample(
        "Ejemplo: fracciones equivalentes",
        listOf(
            "¿Cuál es equivalente a 2/3?",
            "Se multiplican ARRIBA y ABAJO por el mismo número:",
            "2/3 × 2/2 = 4/6   |   2/3 × 3/3 = 6/9   |   2/3 × 4/4 = 8/12",
            "Comprobación rápida: 2÷3 = 0.666 y 4÷6 = 0.666 ✓",
            "Ojo: 6/3 NO es equivalente a 6/2; hay que dividir para comprobar."
        )
    )

    fun equivalentes(format: ExerciseFormat): MathQuestion {
        val num = (1..6).random()
        val den = (2..9).random() + num  // asegura den > num
        val k = (2..5).random()
        val eqN = num * k
        val eqD = den * k

        if (format == ExerciseFormat.INVERSO) {
            return MathQuestion(
                "Completa para que las fracciones sean equivalentes:  $num/$den = ___/$eqD",
                Gen.numOpts(
                    eqN.toDouble(),
                    (num + k).toDouble(),      // error: sumar en vez de multiplicar
                    (eqN + 1).toDouble(),
                    (num * (k + 1)).toDouble()
                ),
                eqN.toString(),
                listOf(
                    "El denominador pasó de $den a $eqD: se multiplicó por $k ($den × $k = $eqD)",
                    "Hay que hacer LO MISMO arriba: $num × $k = $eqN",
                    "$num/$den = $eqN/$eqD ✓"
                ),
                EX_EQUIV, Curriculum.FRAC_EQUIVALENTES.id, format
            )
        }

        val correcta = "$eqN/$eqD"
        val distractores = listOf(
            "$eqD/$eqN",              // error: invertir
            "${num + k}/${den + k}",  // error: sumar en vez de multiplicar
            "$num/$eqD"               // error: multiplicar solo abajo
        ).filter { it != correcta }

        return MathQuestion(
            "¿Cuál de las siguientes fracciones es equivalente a $num/$den?",
            (listOf(correcta) + distractores).distinct().take(4).shuffled(),
            correcta,
            listOf(
                "Para obtener una equivalente se multiplica ARRIBA y ABAJO por el mismo número.",
                "$num × $k = $eqN   y   $den × $k = $eqD",
                "Comprueba dividiendo: $num ÷ $den = ${Gen.fmt(num.toDouble() / den, 3)} y $eqN ÷ $eqD = ${Gen.fmt(eqN.toDouble() / eqD, 3)} ✓"
            ),
            EX_EQUIV, Curriculum.FRAC_EQUIVALENTES.id, format
        )
    }

    // ==================================================================
    // Comparar fracciones  ⚠️
    // ==================================================================
    private val EX_COMPARAR = WorkedExample(
        "Ejemplo: comparar fracciones",
        listOf(
            "MISMO denominador → gana el numerador mayor:  5/7 > 3/7",
            "MISMO numerador   → gana el denominador MENOR: 3/6 > 3/8",
            "  (si repartes entre menos personas, a cada quien le toca más)",
            "DISTINTOS → divide y compara: 3/4 = 0.75 y 2/3 = 0.666 → 3/4 > 2/3"
        )
    )

    fun comparar(format: ExerciseFormat): MathQuestion {
        // Se incluye a propósito el caso de fracciones EQUIVALENTES: si "=" nunca
        // fuera la respuesta correcta, el alumno aprendería a descartarla sin
        // razonar (y el examen del colegio sí la usa).
        val caso = listOf("mismo_den", "mismo_num", "distintos", "iguales").random()
        val (a, b, c, d) = when (caso) {
            "mismo_den" -> {
                val den = (2..9).random()
                val n1 = (1..9).random()
                var n2 = (1..9).random()
                if (n2 == n1) n2 = n1 + 1
                listOf(n1, den, n2, den)
            }
            "mismo_num" -> {
                val num = (1..8).random()
                val d1 = (2..9).random()
                var d2 = (2..9).random()
                if (d2 == d1) d2 = d1 + 1
                listOf(num, d1, num, d2)
            }
            "iguales" -> {
                val n1 = (1..5).random()
                val d1 = (2..6).random()
                val k = (2..4).random()
                listOf(n1, d1, n1 * k, d1 * k)
            }
            else -> {
                val n1 = (1..7).random()
                val d1 = (2..9).random()
                var n2 = (1..7).random()
                var d2 = (2..9).random()
                // Si salieran equivalentes por casualidad se fuerza que no lo sean:
                // este caso está reservado para "iguales".
                if (n1 * d2 == n2 * d1) { n2 = n1 + 1; d2 = d1 }
                listOf(n1, d1, n2, d2)
            }
        }.let { Quad(it[0], it[1], it[2], it[3]) }

        val v1 = a.toDouble() / b
        val v2 = c.toDouble() / d
        val signo = when {
            v1 > v2 -> ">"
            v1 < v2 -> "<"
            else -> "="
        }

        return MathQuestion(
            "¿Qué signo va entre estas fracciones?\n\n$a/$b  ___  $c/$d",
            listOf(">", "<", "=", "No se puede comparar").shuffled(),
            signo,
            listOf(
                "Convierte cada una a decimal dividiendo:",
                "$a ÷ $b = ${Gen.fmt(v1, 3)}   y   $c ÷ $d = ${Gen.fmt(v2, 3)}",
                "Como ${Gen.fmt(v1, 3)} $signo ${Gen.fmt(v2, 3)}, entonces $a/$b $signo $c/$d",
                if (caso == "mismo_num")
                    "Atajo: con el mismo numerador, gana la de DENOMINADOR MENOR."
                else
                    "Atajo: con el mismo denominador, gana la de NUMERADOR MAYOR."
            ),
            EX_COMPARAR, Curriculum.FRAC_COMPARAR.id, format
        )
    }

    private data class Quad(val a: Int, val b: Int, val c: Int, val d: Int)

    // ==================================================================
    // Fracción a decimal  ⚠️
    // ==================================================================
    private val EX_A_DECIMAL = WorkedExample(
        "Ejemplo: fracción a decimal",
        listOf(
            "¿Cuánto es 3/4 en decimal?",
            "La raya de la fracción SIGNIFICA dividir:",
            "3 ÷ 4 = 0.75",
            "Truco: siempre se divide el de arriba entre el de abajo."
        )
    )

    fun aDecimal(format: ExerciseFormat): MathQuestion {
        val (num, den) = listOf(
            1 to 2, 1 to 4, 3 to 4, 1 to 5, 2 to 5, 3 to 5, 4 to 5,
            1 to 8, 3 to 8, 5 to 8, 7 to 10, 9 to 20, 3 to 25
        ).random()
        val v = num.toDouble() / den

        return MathQuestion(
            "¿Cuál es el resultado de $num/$den escrito como decimal?",
            Gen.numOptsD(
                4, v,
                den.toDouble() / num,          // error: dividir al revés
                v * 10,                        // error: correr el punto
                (num + den) / 100.0
            ),
            Gen.fmt(v, 4),
            listOf(
                "La raya de la fracción significa DIVIDIR: el de arriba entre el de abajo.",
                "$num ÷ $den = ${Gen.fmt(v, 4)}"
            ),
            EX_A_DECIMAL, Curriculum.FRAC_A_DECIMAL.id, format
        )
    }

    // ==================================================================
    // Fracción de una cantidad  ⚠️
    // ==================================================================
    private val EX_DE_CANTIDAD = WorkedExample(
        "Ejemplo: fracción de una cantidad",
        listOf(
            "¿Cuánto es 2/5 de 300?",
            "1) Divide entre el de ABAJO:  300 ÷ 5 = 60   (eso vale 1/5)",
            "2) Multiplica por el de ARRIBA:  60 × 2 = 120",
            "Respuesta: 120",
            "Si preguntan cuánto FALTA, hay que restar: 300 − 120 = 180"
        )
    )

    fun deCantidad(format: ExerciseFormat): MathQuestion {
        // Se excluye num=1 y num/den=1/2 porque con esos valores dos distractores
        // colapsan con la respuesta correcta (con 1/2, "lo que falta" y "lo que
        // tiene" son el mismo número) y el ejercicio se resolvería por descarte.
        val den = listOf(3, 4, 5, 6, 8, 10).random()
        val num = (2 until den).filter { it * 2 != den }.random()
        val total = den * listOf(20, 30, 50, 100, 200, 400).random()
        val parte = total.toDouble() * num / den
        val falta = total - parte

        // El formato RAZONAMIENTO pregunta lo que FALTA: en el examen real se
        // respondió la parte que ya se tiene en vez de la que falta.
        if (format == ExerciseFormat.RAZONAMIENTO) {
            val name = Gen.NAMES.random()
            return MathQuestion(
                "$name quiere hacer un viaje que cuesta \$$total. Si solo tiene $num/$den del total, ¿cuánto dinero le FALTA para completar su viaje?",
                Gen.numOpts(
                    falta,
                    parte,                      // error: dar lo que YA tiene
                    total.toDouble(),           // error: dar el precio completo
                    total.toDouble() / num      // error: dividir entre el numerador
                ),
                Gen.fmt(falta),
                listOf(
                    "1) Lo que ya tiene: $total ÷ $den = ${Gen.fmt(total.toDouble() / den)}, por $num = ${Gen.fmt(parte)}",
                    "2) La pregunta es cuánto FALTA, así que se resta:",
                    "   $total − ${Gen.fmt(parte)} = ${Gen.fmt(falta)}",
                    "Cuidado: ${Gen.fmt(parte)} es lo que YA TIENE, no lo que le falta."
                ),
                EX_DE_CANTIDAD, Curriculum.FRAC_DE_CANTIDAD.id, format
            )
        }

        val question = if (format == ExerciseFormat.CONTEXTO) {
            val name = Gen.NAMES.random()
            "$name leyó $num/$den de un libro de $total páginas. ¿Cuántas páginas leyó?"
        } else {
            "¿Cuánto es $num/$den de $total?"
        }

        return MathQuestion(
            question,
            Gen.numOpts(
                parte,
                falta,                          // error: dar el resto
                total.toDouble() / num,         // error: dividir entre el numerador
                total.toDouble() * den / num    // error: invertir la fracción
            ),
            Gen.fmt(parte),
            listOf(
                "1) Divide entre el de abajo: $total ÷ $den = ${Gen.fmt(total.toDouble() / den)}",
                "2) Multiplica por el de arriba: ${Gen.fmt(total.toDouble() / den)} × $num = ${Gen.fmt(parte)}"
            ),
            EX_DE_CANTIDAD, Curriculum.FRAC_DE_CANTIDAD.id, format
        )
    }
}
