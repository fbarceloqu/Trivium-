package com.controlparental.kioscosuave.curriculum

import com.controlparental.kioscosuave.MathQuestion
import com.controlparental.kioscosuave.WorkedExample
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * TEMAS · TRIÁNGULOS, ÁREAS, CÍRCULO Y PLANO CARTESIANO
 * (exámenes diagnóstico y de noviembre)
 *
 * Errores observados:
 *   · triángulo de lados 15, 6 y otro distinto → se respondió "equilátero"
 *   · perímetro de un cuadrado de lado 12 m → se respondió "48 m²" (unidades)
 *   · "distancia del centro a la circunferencia" → se respondió "diámetro"
 *   · distancia entre (−4,−6) y (−1,5) → se respondió 13 (es 11.4)
 *
 * El error de unidades (m frente a m²) es tan frecuente que [areaPerimetro]
 * incluye SIEMPRE el mismo número con las dos unidades entre las opciones.
 */
internal object Sec1Geometria {

    // ==================================================================
    // Clasificar triángulos
    // ==================================================================
    private val EX_TRI_TIPO = WorkedExample(
        "Ejemplo: tipos de triángulo",
        listOf(
            "POR SUS LADOS:",
            "  EQUILÁTERO → los TRES lados iguales",
            "  ISÓSCELES  → DOS lados iguales",
            "  ESCALENO   → los tres lados DISTINTOS",
            "Si te dicen que dos lados miden 15 y 6, y el tercero mide diferente",
            "de ambos, entonces los tres son distintos → ESCALENO.",
            "Truco: equi = igual (los tres), iso = dos iguales."
        )
    )

    fun triangulosClasificar(format: ExerciseFormat): MathQuestion {
        val tipos = listOf("Equilátero", "Isósceles", "Escaleno", "Rectángulo")

        if (format == ExerciseFormat.CONCEPTUAL) {
            val (q, a) = listOf(
                "¿Cómo se llama el triángulo que tiene sus tres ángulos iguales?" to "Equilátero",
                "¿Cómo se llama el triángulo que tiene sus tres lados diferentes?" to "Escaleno",
                "¿Cómo se llama el triángulo que tiene exactamente dos lados iguales?" to "Isósceles",
                "¿Cómo se llama el triángulo que tiene un ángulo de 90°?" to "Rectángulo"
            ).random()
            return MathQuestion(
                q, tipos.shuffled(), a,
                listOf(
                    "Equilátero: tres iguales | Isósceles: dos iguales | Escaleno: todos distintos",
                    "Rectángulo se refiere a los ÁNGULOS, no a los lados: tiene uno de 90°."
                ),
                EX_TRI_TIPO, Curriculum.TRI_CLASIFICAR.id, format
            )
        }

        val (lados, answer) = listOf(
            Triple(15, 6, 12) to "Escaleno",
            Triple(8, 8, 5) to "Isósceles",
            Triple(7, 7, 7) to "Equilátero",
            Triple(10, 4, 9) to "Escaleno",
            Triple(6, 9, 9) to "Isósceles",
            Triple(11, 11, 11) to "Equilátero"
        ).random()

        return MathQuestion(
            "Un triángulo tiene lados que miden ${lados.first} cm, ${lados.second} cm y ${lados.third} cm. ¿Qué tipo de triángulo es?",
            listOf("Equilátero", "Isósceles", "Escaleno", "Rectángulo").shuffled(),
            answer,
            listOf(
                "Compara los tres lados: ${lados.first}, ${lados.second} y ${lados.third}",
                when (answer) {
                    "Equilátero" -> "Los tres son iguales → EQUILÁTERO"
                    "Isósceles" -> "Hay exactamente dos iguales → ISÓSCELES"
                    else -> "Los tres son distintos → ESCALENO"
                }
            ),
            EX_TRI_TIPO, Curriculum.TRI_CLASIFICAR.id, format
        )
    }

    // ==================================================================
    // Perímetro de triángulos
    // ==================================================================
    private val EX_TRI_PER = WorkedExample(
        "Ejemplo: perímetro de un isósceles",
        listOf(
            "Un isósceles tiene 20.28 cm de perímetro y su base mide 8.2 cm.",
            "¿Cuánto mide cada lado igual?",
            "1) Quita la base del perímetro:  20.28 − 8.2 = 12.08",
            "2) Eso son los DOS lados iguales, así que divide entre 2:",
            "   12.08 ÷ 2 = 6.04 cm",
            "Error común: responder 12.08 (los dos juntos) o repetir la base."
        )
    )

    fun trianguloPerimetro(format: ExerciseFormat): MathQuestion {
        if (format == ExerciseFormat.DIRECTO) {
            val a = (5..20).random()
            val b = (5..20).random()
            val c = (5..20).random()
            val per = a + b + c
            return MathQuestion(
                "¿Cuál es el perímetro de un triángulo cuyos lados miden $a cm, $b cm y $c cm?",
                Gen.numOptsSuffix(
                    " cm",
                    per.toDouble(),
                    (a + b).toDouble(),           // error: olvidar un lado
                    (per / 2).toDouble(),
                    (a * b * c).toDouble() / 10
                ),
                "$per cm",
                listOf(
                    "El perímetro es la suma de TODOS los lados:",
                    "$a + $b + $c = $per cm"
                ),
                EX_TRI_PER, Curriculum.TRI_PERIMETRO.id, format
            )
        }

        // INVERSO / RAZONAMIENTO: isósceles con la base conocida.
        val lado = listOf(6.04, 7.5, 9.25, 12.4, 5.6).random()
        val base = listOf(8.2, 5.0, 11.3, 7.8).random()
        val per = lado * 2 + base

        return MathQuestion(
            "El perímetro de un triángulo isósceles mide ${Gen.fmt(per, 2)} cm. Si la base mide ${Gen.fmt(base)} cm, ¿cuánto mide cada uno de sus lados congruentes?",
            Gen.numOptsSuffix(
                " cm",
                lado,
                per - base,          // error: no dividir entre 2
                base,                // error: repetir la base
                per / 3              // error: dividir todo entre 3
            ),
            "${Gen.fmt(lado)} cm",
            listOf(
                "1) Quita la base del perímetro: ${Gen.fmt(per, 2)} − ${Gen.fmt(base)} = ${Gen.fmt(per - base, 2)}",
                "2) Eso son los DOS lados iguales, así que se divide entre 2:",
                "   ${Gen.fmt(per - base, 2)} ÷ 2 = ${Gen.fmt(lado)} cm",
                "Comprueba: ${Gen.fmt(lado)} + ${Gen.fmt(lado)} + ${Gen.fmt(base)} = ${Gen.fmt(per, 2)} ✓"
            ),
            EX_TRI_PER, Curriculum.TRI_PERIMETRO.id, format
        )
    }

    // ==================================================================
    // Área y perímetro  ⚠️ (el error de unidades)
    // ==================================================================
    private val EX_AREA = WorkedExample(
        "Ejemplo: área y perímetro (¡ojo con las unidades!)",
        listOf(
            "Un cuadrado de lado 12 m:",
            "  PERÍMETRO = suma del contorno = 12 × 4 = 48 metros (m)",
            "  ÁREA      = superficie        = 12 × 12 = 144 metros CUADRADOS (m²)",
            "El perímetro se mide en m porque es una LONGITUD (una línea).",
            "El área se mide en m² porque cubre una SUPERFICIE.",
            "Nunca escribas el perímetro en m²: es el error más común del tema."
        )
    )

    fun areaPerimetro(format: ExerciseFormat): MathQuestion {
        val lado = listOf(8, 12, 15, 20, 9).random()
        val per = lado * 4
        val area = lado * lado

        if (format == ExerciseFormat.INVERSO) {
            // Se da el perímetro y se pide el área (reactivo real del examen).
            return MathQuestion(
                "¿Cuál será el área de un cuadrado que tiene $per m de perímetro?",
                listOf(
                    "$area m²",
                    "$per m²",              // error: confundir área con perímetro
                    "${lado * 2} m²",
                    "$area m"               // error: unidad equivocada
                ).distinct().take(4).shuffled(),
                "$area m²",
                listOf(
                    "1) Del perímetro saca el lado: $per ÷ 4 = $lado m",
                    "2) El área del cuadrado es lado × lado: $lado × $lado = $area",
                    "3) Como es una superficie, la unidad es m² → $area m²"
                ),
                EX_AREA, Curriculum.FIG_AREA_PERIMETRO.id, format
            )
        }

        val pidePerimetro = listOf(true, false).random()
        val answer = if (pidePerimetro) "$per m" else "$area m²"

        // Las cuatro opciones incluyen a propósito el MISMO número con las dos
        // unidades: así elegir mal identifica exactamente el error de unidades.
        val options = listOf("$per m", "$per m²", "$area m", "$area m²")

        return MathQuestion(
            if (pidePerimetro)
                "Determina el perímetro de un cuadrado que tiene lados de longitud $lado m."
            else
                "Determina el área de un cuadrado que tiene lados de longitud $lado m.",
            options.shuffled(),
            answer,
            if (pidePerimetro) listOf(
                "El perímetro es el contorno: se suman los 4 lados.",
                "$lado × 4 = $per",
                "Es una LONGITUD, así que va en metros: $per m (no m²)"
            ) else listOf(
                "El área es la superficie: lado × lado.",
                "$lado × $lado = $area",
                "Es una SUPERFICIE, así que va en metros cuadrados: $area m²"
            ),
            EX_AREA, Curriculum.FIG_AREA_PERIMETRO.id, format
        )
    }

    // ==================================================================
    // Elementos del círculo  ⚠️
    // ==================================================================
    private val EX_CIRCULO = WorkedExample(
        "Ejemplo: partes del círculo",
        listOf(
            "RADIO    → del CENTRO a la orilla. Es la mitad del diámetro.",
            "DIÁMETRO → de orilla a orilla PASANDO por el centro. Es el doble del radio.",
            "CUERDA   → une dos puntos de la orilla SIN pasar por el centro.",
            "SECANTE  → una recta que corta la circunferencia en dos puntos.",
            "Truco: RADIO es más corto que DIÁMETRO, igual que su nombre en la frase:",
            "el radio va del centro (medio camino), el diámetro cruza entero."
        )
    )

    fun circuloElementos(format: ExerciseFormat): MathQuestion {
        val opciones = listOf("Radio", "Diámetro", "Cuerda", "Secante")

        if (format == ExerciseFormat.DIRECTO) {
            val r = listOf(3, 5, 7, 8, 12).random()
            val pideDiametro = listOf(true, false).random()
            return if (pideDiametro) MathQuestion(
                "Si el radio de un círculo mide $r cm, ¿cuánto mide su diámetro?",
                Gen.numOptsSuffix(" cm", (r * 2).toDouble(), r.toDouble(), (r / 2.0), (r * 4).toDouble()),
                "${r * 2} cm",
                listOf(
                    "El diámetro es el DOBLE del radio.",
                    "$r × 2 = ${r * 2} cm"
                ),
                EX_CIRCULO, Curriculum.CIR_ELEMENTOS.id, format
            ) else MathQuestion(
                "Si el diámetro de un círculo mide ${r * 2} cm, ¿cuánto mide su radio?",
                Gen.numOptsSuffix(" cm", r.toDouble(), (r * 2).toDouble(), (r * 4).toDouble(), (r / 2.0)),
                "$r cm",
                listOf(
                    "El radio es la MITAD del diámetro.",
                    "${r * 2} ÷ 2 = $r cm"
                ),
                EX_CIRCULO, Curriculum.CIR_ELEMENTOS.id, format
            )
        }

        val (q, a) = listOf(
            "¿Cómo se le llama a la distancia que hay del centro del círculo a cualquier punto de la circunferencia?" to "Radio",
            "¿Cómo se le llama al segmento que va de un punto de la circunferencia a otro pasando por el centro?" to "Diámetro",
            "¿Cómo se le llama al segmento que une dos puntos de la circunferencia sin pasar por el centro?" to "Cuerda",
            "¿Cómo se le llama a la recta que corta a la circunferencia en dos puntos?" to "Secante"
        ).random()

        return MathQuestion(
            q, opciones.shuffled(), a,
            listOf(
                "Radio: del centro a la orilla (la mitad del diámetro)",
                "Diámetro: cruza el círculo entero pasando por el centro",
                "Cuerda: une dos puntos de la orilla SIN pasar por el centro"
            ),
            EX_CIRCULO, Curriculum.CIR_ELEMENTOS.id, format
        )
    }

    // ==================================================================
    // Distancia entre dos puntos  ⚠️
    // ==================================================================
    private val EX_DISTANCIA = WorkedExample(
        "Ejemplo: distancia entre dos puntos",
        listOf(
            "Distancia entre (3, 2) y (6, 6):",
            "1) Resta las x:  6 − 3 = 3",
            "2) Resta las y:  6 − 2 = 4",
            "3) Eleva al cuadrado y suma:  3² + 4² = 9 + 16 = 25",
            "4) Saca la raíz:  √25 = 5",
            "Fórmula: d = √[(x₂−x₁)² + (y₂−y₁)²]",
            "Con negativos, cuidado: −1 − (−4) = −1 + 4 = 3"
        )
    )

    fun distancia(format: ExerciseFormat): MathQuestion {
        // Se alternan ternas pitagóricas (resultado exacto) con casos que dan
        // decimales, como el reactivo real que se falló en el examen.
        val exacta = listOf(true, true, false).random()
        val (dx, dy) = if (exacta) {
            listOf(3 to 4, 6 to 8, 5 to 12, 8 to 15, 9 to 12, 7 to 24).random()
        } else {
            listOf(3 to 11, 2 to 7, 5 to 9, 4 to 6, 6 to 10).random()
        }

        val x1 = (-6..4).random()
        val y1 = (-6..4).random()
        val x2 = x1 + dx
        val y2 = y1 + dy
        val d = sqrt((dx * dx + dy * dy).toDouble())

        return MathQuestion(
            "¿Cuál es la distancia entre los puntos ($x1, $y1) y ($x2, $y2)?",
            Gen.numOptsD(
                2, d,
                (dx + dy).toDouble(),           // error: sumar sin elevar al cuadrado
                (dx * dx + dy * dy).toDouble(), // error: olvidar la raíz
                abs(dx - dy).toDouble()         // error: restar
            ),
            Gen.fmt(d, 2),
            listOf(
                "1) Diferencia en x:  $x2 − ($x1) = $dx",
                "2) Diferencia en y:  $y2 − ($y1) = $dy",
                "3) Eleva al cuadrado y suma: $dx² + $dy² = ${dx * dx} + ${dy * dy} = ${dx * dx + dy * dy}",
                "4) Saca la raíz cuadrada: √${dx * dx + dy * dy} = ${Gen.fmt(d, 2)}",
                "No olvides el paso 4: sin la raíz el resultado queda enorme."
            ),
            EX_DISTANCIA, Curriculum.PLANO_DISTANCIA.id, format
        )
    }
}
