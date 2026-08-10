package com.controlparental.kioscosuave.curriculum

import com.controlparental.kioscosuave.MathQuestion
import com.controlparental.kioscosuave.WorkedExample

/**
 * TEMA · ESTADÍSTICA  (exámenes de noviembre y de marzo)
 *
 * Este es el caso que mejor justifica que Trivium tenga memoria: los MISMOS
 * reactivos ("¿cómo se llama la medida que aparece con más frecuencia?")
 * aparecieron en noviembre y otra vez en marzo, y se fallaron las dos veces.
 * Cuatro meses sin que nadie detectara ni reforzara el hueco.
 *
 * El error no es de cálculo sino de nombres: media, moda y mediana están
 * intercambiadas. Por eso [identificar] usa SIEMPRE las cuatro medidas como
 * opciones: obliga a distinguirlas en vez de resolver por descarte.
 */
internal object Sec1Estadistica {

    private const val MEDIA = "Media aritmética"
    private const val MODA = "Moda"
    private const val MEDIANA = "Mediana"
    private const val RANGO = "Rango"
    private val LAS_CUATRO = listOf(MEDIA, MODA, MEDIANA, RANGO)

    private val EX_IDENTIFICAR = WorkedExample(
        "Ejemplo: cómo distinguirlas",
        listOf(
            "Con los datos 3, 5, 5, 7, 10:",
            "MEDIA → se suman y se divide: (3+5+5+7+10) ÷ 5 = 6",
            "MODA → la que MÁS se repite: 5",
            "MEDIANA → la de en MEDIO al ordenar: 3, 5, [5], 7, 10 → 5",
            "RANGO → el mayor menos el menor: 10 − 3 = 7",
            "Truco: MODA y MÁS empiezan igual; MEDIANA y MEDIO también."
        )
    )

    /** Pregunta conceptual pura: las 4 medidas siempre como opciones. */
    fun identificar(format: ExerciseFormat): MathQuestion {
        val (question, answer, tip) = listOf(
            Triple(
                "¿Cómo se le llama a la medida de tendencia central que aparece con más frecuencia en un conjunto de datos?",
                MODA,
                "MODA = la que está de moda = la que MÁS se repite."
            ),
            Triple(
                "¿Cómo se le llama al valor que se obtiene al sumar todos los datos y dividir el resultado entre la cantidad de datos?",
                MEDIA,
                "MEDIA = promedio: sumar todo y repartir en partes iguales."
            ),
            Triple(
                "¿Cómo se le llama al valor que ocupa la posición central de un conjunto de datos, una vez que estos han sido ordenados?",
                MEDIANA,
                "MEDIANA = la de en MEDIO. Primero hay que ORDENAR."
            ),
            Triple(
                "¿Cómo se le llama a la diferencia entre el dato mayor y el dato menor?",
                RANGO,
                "RANGO = qué tan separados están: mayor − menor."
            )
        ).random()

        return MathQuestion(
            question = question,
            options = LAS_CUATRO.shuffled(),
            answer = answer,
            steps = listOf(tip),
            example = EX_IDENTIFICAR,
            skillId = Curriculum.EST_IDENTIFICAR.id,
            format = format
        )
    }

    // ------------------------------------------------------------------
    // Media
    // ------------------------------------------------------------------
    private val EX_MEDIA = WorkedExample(
        "Ejemplo: media aritmética",
        listOf(
            "Datos: 4, 6, 6, 8",
            "1) Súmalos todos: 4 + 6 + 6 + 8 = 24",
            "2) Divide entre CUÁNTOS son: 24 ÷ 4 = 6",
            "Media = 6",
            "Ojo: se divide entre la cantidad de datos, no entre el dato mayor."
        )
    )

    fun media(format: ExerciseFormat): MathQuestion {
        val n = listOf(4, 5, 6).random()
        val data = List(n) { (20..90).random() }
        val suma = data.sum()
        val ans = suma.toDouble() / n

        if (format == ExerciseFormat.INVERSO) {
            // Se conoce la media y falta un dato.
            val faltante = data.last()
            val visibles = data.dropLast(1)
            return MathQuestion(
                question = "La media de $n calificaciones es ${Gen.fmt(ans)}. " +
                    "Si ${visibles.size} de ellas son ${visibles.joinToString(", ")}, ¿cuál es la que falta?",
                options = Gen.numOpts(
                    faltante.toDouble(),
                    ans,                                  // error: repetir la media
                    (suma - faltante).toDouble(),         // error: dar la suma parcial
                    visibles.average()
                ),
                answer = Gen.fmt(faltante.toDouble()),
                steps = listOf(
                    "La suma total debe ser: ${Gen.fmt(ans)} × $n = $suma",
                    "Lo que ya tienes suma: ${visibles.joinToString(" + ")} = ${suma - faltante}",
                    "El dato que falta: $suma − ${suma - faltante} = $faltante"
                ),
                example = EX_MEDIA,
                skillId = Curriculum.EST_MEDIA.id,
                format = format
            )
        }

        val question = if (format == ExerciseFormat.CONTEXTO) {
            val name = Gen.NAMES.random()
            "$name registró el peso (en kg) de $n canastas: ${data.joinToString(", ")}. " +
                "¿Cuál fue el peso medio de las canastas?"
        } else {
            "¿Cuál es la media aritmética de estos datos?\n${data.joinToString(", ")}"
        }

        return MathQuestion(
            question = question,
            options = Gen.numOpts(
                ans,
                Gen.median(data),                    // error: dar la mediana
                (data.max() - data.min()).toDouble(), // error: dar el rango
                suma.toDouble()                      // error: quedarse en la suma
            ),
            answer = Gen.fmt(ans),
            steps = listOf(
                "Suma: ${data.joinToString(" + ")} = $suma",
                "Divide entre la cantidad de datos: $suma ÷ $n = ${Gen.fmt(ans)}"
            ),
            example = EX_MEDIA,
            skillId = Curriculum.EST_MEDIA.id,
            format = format
        )
    }

    // ------------------------------------------------------------------
    // Moda (incluye los casos difíciles: sin moda y con dos modas)
    // ------------------------------------------------------------------
    private val EX_MODA = WorkedExample(
        "Ejemplo: moda",
        listOf(
            "Datos: 2, 2, 3, 5, 7, 7, 9",
            "Cuenta cuántas veces aparece cada uno:",
            "2 → dos veces | 3 → una | 5 → una | 7 → dos veces | 9 → una",
            "Empatan el 2 y el 7, así que la moda es 2 y 7.",
            "Si TODOS aparecen el mismo número de veces, NO hay moda."
        )
    )

    fun moda(format: ExerciseFormat): MathQuestion {
        // Se rota entre los tres casos a propósito: una moda, dos modas y
        // ninguna. El caso "no hay moda" fue fallado en el examen real.
        val caso = listOf("una", "dos", "ninguna").random()
        val data: List<Int> = when (caso) {
            "una" -> {
                val m = (2..9).random()
                (listOf(m, m, m) + List(3) { (2..9).random() }.filter { it != m }).shuffled()
            }
            "dos" -> {
                val a = (2..5).random()
                val b = (6..9).random()
                listOf(a, a, b, b, (2..9).random().let { if (it == a || it == b) 1 else it }).shuffled()
            }
            else -> (1..5).map { it * 2 }.shuffled() // todos distintos -> sin moda
        }

        val ms = Gen.modes(data)
        val answer = when {
            ms.isEmpty() -> "No hay moda"
            ms.size == 1 -> ms.first().toString()
            else -> ms.joinToString(" y ")
        }

        // Las opciones siempre incluyen "No hay moda" y candidatos plausibles.
        val candidatos = LinkedHashSet<String>()
        candidatos += answer
        candidatos += "No hay moda"
        candidatos += Gen.fmt(data.average())          // error: dar la media
        candidatos += data.max().toString()            // error: dar el mayor
        candidatos += Gen.fmt(Gen.median(data))        // error: dar la mediana

        return MathQuestion(
            question = "¿Cuál es la moda de estos datos?\n${data.joinToString(", ")}",
            options = candidatos.toList().take(4).shuffled(),
            answer = answer,
            steps = buildList {
                add("Ordenados: ${data.sorted().joinToString(", ")}")
                add("Veces que aparece cada dato: " +
                    data.groupingBy { it }.eachCount().toSortedMap()
                        .entries.joinToString(" | ") { "${it.key}→${it.value}" })
                add(
                    when {
                        ms.isEmpty() -> "Todos aparecen las mismas veces, así que NO hay moda."
                        ms.size == 1 -> "El que más se repite es ${ms.first()}."
                        else -> "Empatan ${ms.joinToString(" y ")}: hay dos modas."
                    }
                )
            },
            example = EX_MODA,
            skillId = Curriculum.EST_MODA.id,
            format = format
        )
    }

    // ------------------------------------------------------------------
    // Mediana
    // ------------------------------------------------------------------
    private val EX_MEDIANA = WorkedExample(
        "Ejemplo: mediana",
        listOf(
            "Datos: 7, 3, 9, 4, 5",
            "1) ORDENA primero: 3, 4, 5, 7, 9",
            "2) Busca el de en medio: 3, 4, [5], 7, 9 → mediana = 5",
            "Si son PARES, se promedian los dos de en medio:",
            "2, 4, [6, 8], 10, 12 → (6+8) ÷ 2 = 7"
        )
    )

    fun mediana(format: ExerciseFormat): MathQuestion {
        // Alterna cantidad impar y par: el caso par (promediar los dos
        // centrales) es el que se suele fallar.
        val n = listOf(5, 6, 7, 8).random()
        val data = List(n) { (1..20).random() }
        val ordenados = data.sorted()
        val ans = Gen.median(data)

        return MathQuestion(
            question = "¿Cuál es la mediana de estos datos?\n${data.joinToString(", ")}",
            options = Gen.numOpts(
                ans,
                data.average(),                       // error: dar la media
                data[n / 2].toDouble(),               // error: no ordenar antes
                (data.max() - data.min()).toDouble()  // error: dar el rango
            ),
            answer = Gen.fmt(ans),
            steps = buildList {
                add("1) Ordena los datos: ${ordenados.joinToString(", ")}")
                if (n % 2 == 1) {
                    add("2) Son $n datos (impar): el de en medio es la posición ${n / 2 + 1}")
                    add("Mediana = ${Gen.fmt(ans)}")
                } else {
                    add("2) Son $n datos (par): hay DOS de en medio, ${ordenados[n / 2 - 1]} y ${ordenados[n / 2]}")
                    add("3) Se promedian: (${ordenados[n / 2 - 1]} + ${ordenados[n / 2]}) ÷ 2 = ${Gen.fmt(ans)}")
                }
            },
            example = EX_MEDIANA,
            skillId = Curriculum.EST_MEDIANA.id,
            format = format
        )
    }

    // ------------------------------------------------------------------
    // Rango
    // ------------------------------------------------------------------
    private val EX_RANGO = WorkedExample(
        "Ejemplo: rango",
        listOf(
            "Datos: 12, 5, 20, 8",
            "1) El mayor: 20",
            "2) El menor: 5",
            "3) Réstalos: 20 − 5 = 15",
            "Rango = 15"
        )
    )

    fun rango(format: ExerciseFormat): MathQuestion {
        val data = List(listOf(5, 6, 7).random()) { (3..40).random() }
        val ans = (data.max() - data.min()).toDouble()

        return MathQuestion(
            question = "¿Cuál es el rango de estos datos?\n${data.joinToString(", ")}",
            options = Gen.numOpts(
                ans,
                data.max().toDouble(),      // error: dar solo el mayor
                data.average(),             // error: dar la media
                (data.max() + data.min()).toDouble()  // error: sumar en vez de restar
            ),
            answer = Gen.fmt(ans),
            steps = listOf(
                "El dato mayor es ${data.max()} y el menor es ${data.min()}",
                "Rango = ${data.max()} − ${data.min()} = ${Gen.fmt(ans)}"
            ),
            example = EX_RANGO,
            skillId = Curriculum.EST_RANGO.id,
            format = format
        )
    }

    // ------------------------------------------------------------------
    // Lectura de gráficas
    // ------------------------------------------------------------------
    private val EX_GRAFICAS = WorkedExample(
        "Ejemplo: leer una gráfica de barras",
        listOf(
            "Perros  ████████ 8",
            "Gatos   █████ 5",
            "Aves    ███ 3",
            "¿Cuál es el favorito? El de la barra MÁS LARGA: Perros.",
            "¿Cuántos votaron en total? Se SUMAN todas: 8 + 5 + 3 = 16"
        )
    )

    fun graficas(format: ExerciseFormat): MathQuestion {
        val cats = listOf(
            "Fútbol", "Básquet", "Natación", "Ciclismo", "Voleibol", "Atletismo"
        ).shuffled().take(4)
        val vals = cats.map { (2..15).random() }
        val chart = cats.zip(vals).joinToString("\n") { (c, v) ->
            "${c.padEnd(10)} ${"█".repeat(v)} $v"
        }

        val total = vals.sum()
        val maxIdx = vals.indexOf(vals.max())
        val minIdx = vals.indexOf(vals.min())

        val (pregunta, answer, pasos) = listOf(
            Triple(
                "¿Cuántas personas votaron en total?",
                total.toString(),
                listOf("Se suman TODAS las barras: ${vals.joinToString(" + ")} = $total")
            ),
            Triple(
                "¿Cuál es el deporte que más les gusta?",
                cats[maxIdx],
                listOf("Es el de la barra más larga: ${cats[maxIdx]} con ${vals[maxIdx]} votos")
            ),
            Triple(
                "¿Cuál es el deporte que menos les gusta?",
                cats[minIdx],
                listOf("Es el de la barra más corta: ${cats[minIdx]} con ${vals[minIdx]} votos")
            ),
            Triple(
                "¿Cuántas personas más prefieren ${cats[maxIdx]} que ${cats[minIdx]}?",
                (vals[maxIdx] - vals[minIdx]).toString(),
                listOf("Se restan las dos barras: ${vals[maxIdx]} − ${vals[minIdx]} = ${vals[maxIdx] - vals[minIdx]}")
            )
        ).random()

        // Las opciones dependen de si la respuesta es un nombre o un número.
        val options = if (answer.toIntOrNull() == null) {
            cats.shuffled()
        } else {
            val a = answer.toInt()
            Gen.opts(
                answer,
                vals.max().toString(),
                vals.min().toString(),
                (a + vals.min()).toString(),
                cats.size.toString()
            )
        }

        return MathQuestion(
            question = "Observa la gráfica y responde:\n\n$chart\n\n$pregunta",
            options = options,
            answer = answer,
            steps = pasos,
            example = EX_GRAFICAS,
            skillId = Curriculum.EST_GRAFICAS.id,
            format = format
        )
    }
}
