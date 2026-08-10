package com.controlparental.kioscosuave.curriculum

import com.controlparental.kioscosuave.MathQuestion
import com.controlparental.kioscosuave.WorkedExample

/**
 * TEMA · PORCENTAJES  (examen de febrero)
 *
 * El tema con peor desempeño del ciclo. Los errores observados fueron
 * sistemáticos, no de cálculo:
 *   · dividir entre el porcentaje en vez de multiplicar (13.8 → 39.42)
 *   · quedarse con el descuento en vez del precio final (160 −20% → 32)
 *   · confundir "el 25% reprueba" con "25 reprueban"
 *   · no distinguir el problema inverso ("el 20% es 8, ¿el total?")
 *
 * Cada uno de esos errores está representado como distractor en su generador.
 */
internal object Sec1Porcentajes {

    // ------------------------------------------------------------------
    // % de una cantidad
    // ------------------------------------------------------------------
    private val EX_DE_CANTIDAD = WorkedExample(
        "Ejemplo: el % de una cantidad",
        listOf(
            "¿Cuánto es el 20% de 80?",
            "1) Convierte el % a decimal: 20 ÷ 100 = 0.20",
            "2) Multiplica: 80 × 0.20 = 16",
            "Respuesta: 16",
            "Ojo: se MULTIPLICA por el decimal, no se divide."
        )
    )

    fun deCantidad(format: ExerciseFormat): MathQuestion {
        val base = listOf(60, 80, 120, 160, 240, 300, 45, 90, 200).random()
        val p = listOf(10, 15, 20, 25, 30, 35, 40, 50, 75).random()
        val dec = p / 100.0
        val ans = base * dec

        val question = if (format == ExerciseFormat.CONTEXTO) {
            val name = Gen.NAMES.random()
            val ctx = listOf(
                "En un salón de $base alumnos, el $p% practica algún deporte. ¿Cuántos alumnos son?",
                "$name leyó el $p% de un libro de $base páginas. ¿Cuántas páginas leyó?",
                "De $base árboles plantados, el $p% ya dio fruto. ¿Cuántos árboles son?"
            )
            ctx.random()
        } else {
            "¿Cuánto es el $p% de $base?"
        }

        return MathQuestion(
            question = question,
            options = Gen.numOpts(
                ans,
                base / dec,            // error: dividir en vez de multiplicar
                base * (1 - dec),      // error: calcular el complemento
                base - p               // error: restar el número del porcentaje
            ),
            answer = Gen.fmt(ans),
            steps = listOf(
                "$p% = $p ÷ 100 = ${Gen.fmt(dec)}",
                "$base × ${Gen.fmt(dec)} = ${Gen.fmt(ans)}"
            ),
            example = EX_DE_CANTIDAD,
            skillId = Curriculum.PCT_DE_CANTIDAD.id,
            format = format
        )
    }

    // ------------------------------------------------------------------
    // Problema inverso: se conoce la parte y el %, falta el total
    // ------------------------------------------------------------------
    private val EX_INVERSO = WorkedExample(
        "Ejemplo: encontrar el total",
        listOf(
            "Si el 25% de una cantidad es 15, ¿cuál es la cantidad?",
            "1) El 25% vale 15",
            "2) Saca cuánto vale 1%: 15 ÷ 25 = 0.6",
            "3) El total es el 100%: 0.6 × 100 = 60",
            "Comprueba: el 25% de 60 = 15 ✓"
        )
    )

    fun inverso(format: ExerciseFormat): MathQuestion {
        val p = listOf(10, 15, 20, 25, 30, 40, 50).random()
        val total = listOf(40, 60, 80, 120, 160, 200, 240).random()
        val part = total * p / 100.0

        val question = if (format == ExerciseFormat.CONTEXTO) {
            val name = Gen.NAMES.random()
            listOf(
                "$name vendió el $p% de las galletas de una caja y fueron ${Gen.fmt(part)} galletas. ¿Cuántas galletas tenía la caja?",
                "El $p% de los alumnos de un grupo son ${Gen.fmt(part)} personas. ¿Cuántos alumnos hay en total?",
                "$name pagó ${Gen.money(part)} de enganche, que es el $p% del precio. ¿Cuánto cuesta el artículo?"
            ).random()
        } else {
            "Si el $p% de una cantidad es ${Gen.fmt(part)}, ¿cuál es la cantidad?"
        }

        return MathQuestion(
            question = question,
            options = Gen.numOpts(
                total,
                part * p / 100.0,      // error: volver a aplicar el %
                part * (100 - p) / 100.0,
                part + p               // error: sumar el % al dato
            ),
            answer = Gen.fmt(total.toDouble()),
            steps = listOf(
                "El $p% vale ${Gen.fmt(part)}",
                "1% = ${Gen.fmt(part)} ÷ $p = ${Gen.fmt(part / p)}",
                "100% = ${Gen.fmt(part / p)} × 100 = ${Gen.fmt(total.toDouble())}",
                "Comprueba: el $p% de $total = ${Gen.fmt(part)} ✓"
            ),
            example = EX_INVERSO,
            skillId = Curriculum.PCT_INVERSO.id,
            format = format
        )
    }

    // ------------------------------------------------------------------
    // Complemento: si el X% hace algo, ¿cuántos hacen lo contrario?
    // ------------------------------------------------------------------
    private val EX_COMPLEMENTO = WorkedExample(
        "Ejemplo: calcular el resto",
        listOf(
            "De 40 alumnos, el 30% reprobó. ¿Cuántos aprobaron?",
            "1) Los que reprobaron: 40 × 0.30 = 12",
            "2) Los que aprobaron son el resto: 40 − 12 = 28",
            "Atajo: aprobó el 70%, y 40 × 0.70 = 28",
            "Cuidado: la pregunta NO pide los que reprobaron."
        )
    )

    fun complemento(format: ExerciseFormat): MathQuestion {
        val total = listOf(40, 60, 80, 120, 200).random()
        val p = listOf(15, 20, 25, 30, 40, 45).random()
        val fallan = total * p / 100.0
        val ans = total - fallan

        val name = Gen.NAMES.random()
        val question = listOf(
            "De un salón de $total alumnos, el $p% reprueba matemáticas. ¿Cuántos alumnos aprueban?",
            "En una canasta hay $total manzanas y el $p% está maltratada. ¿Cuántas están en buen estado?",
            "$name tenía $total canicas y perdió el $p%. ¿Cuántas le quedan?"
        ).random()

        return MathQuestion(
            question = question,
            options = Gen.numOpts(
                ans,
                fallan,                // error: responder la parte que sí falla
                total - p.toDouble(),  // error: restar el número del porcentaje
                total + fallan
            ),
            answer = Gen.fmt(ans),
            steps = listOf(
                "La parte mencionada: $total × ${Gen.fmt(p / 100.0)} = ${Gen.fmt(fallan)}",
                "El resto: $total − ${Gen.fmt(fallan)} = ${Gen.fmt(ans)}",
                "Atajo: 100% − $p% = ${100 - p}%, y $total × ${Gen.fmt((100 - p) / 100.0)} = ${Gen.fmt(ans)}"
            ),
            example = EX_COMPLEMENTO,
            skillId = Curriculum.PCT_COMPLEMENTO.id,
            format = format
        )
    }

    // ------------------------------------------------------------------
    // Descuento
    // ------------------------------------------------------------------
    private val EX_DESCUENTO = WorkedExample(
        "Ejemplo: precio con descuento",
        listOf(
            "Una playera cuesta $200 y tiene 15% de descuento. ¿Cuánto pagas?",
            "1) El descuento: 200 × 0.15 = 30",
            "2) El precio final: 200 − 30 = 170",
            "Atajo: pagas el 85%, y 200 × 0.85 = 170",
            "Cuidado: 30 es lo que TE AHORRAS, no lo que pagas."
        )
    )

    fun descuento(format: ExerciseFormat): MathQuestion {
        val price = listOf(160, 240, 320, 450, 580, 1200, 12580).random()
        val p = listOf(10, 15, 20, 25, 30, 40).random()
        val desc = price * p / 100.0
        val ans = price - desc
        val item = listOf(
            "una camisa", "un par de tenis", "una mochila", "una pantalla",
            "una bicicleta", "un juego de mesa"
        ).random()

        return MathQuestion(
            question = "Si $item cuesta ${Gen.money(price.toDouble())} y la vendedora te descuenta el $p%, ¿cuánto tienes que pagar?",
            options = Gen.opts(
                Gen.money(ans),
                Gen.money(desc),                      // error: dar el descuento
                Gen.money(price + desc),              // error: sumar en vez de restar
                Gen.money(price - p.toDouble())       // error: restar el número del %
            ),
            answer = Gen.money(ans),
            steps = listOf(
                "El descuento: $price × ${Gen.fmt(p / 100.0)} = ${Gen.fmt(desc)}",
                "El precio final: $price − ${Gen.fmt(desc)} = ${Gen.fmt(ans)}",
                "Atajo: pagas el ${100 - p}%, y $price × ${Gen.fmt((100 - p) / 100.0)} = ${Gen.fmt(ans)}"
            ),
            example = EX_DESCUENTO,
            skillId = Curriculum.PCT_DESCUENTO.id,
            format = format
        )
    }

    // ------------------------------------------------------------------
    // Aumento
    // ------------------------------------------------------------------
    private val EX_AUMENTO = WorkedExample(
        "Ejemplo: cantidad con aumento",
        listOf(
            "Una escuela tenía 500 alumnos y creció 20%. ¿Cuántos tiene ahora?",
            "1) El aumento: 500 × 0.20 = 100",
            "2) El total nuevo: 500 + 100 = 600",
            "Atajo: ahora es el 120%, y 500 × 1.20 = 600"
        )
    )

    fun aumento(format: ExerciseFormat): MathQuestion {
        val base = listOf(80, 150, 400, 500, 835, 1200).random()
        val p = listOf(10, 15, 20, 25, 30, 50).random()
        val extra = base * p / 100.0
        val ans = base + extra

        val question = listOf(
            "Una escuela tenía $base alumnos y la matrícula aumentó $p%. ¿Cuántos alumnos hay ahora?",
            "Un taller producía $base piezas al mes y subió su producción $p%. ¿Cuántas produce ahora?",
            "Un pueblo tenía $base habitantes y creció $p%. ¿Cuántos habitantes tiene ahora?"
        ).random()

        return MathQuestion(
            question = question,
            options = Gen.numOpts(
                ans,
                extra,                     // error: dar solo el aumento
                base - extra,              // error: restar en vez de sumar
                base + p.toDouble()        // error: sumar el número del %
            ),
            answer = Gen.fmt(ans),
            steps = listOf(
                "El aumento: $base × ${Gen.fmt(p / 100.0)} = ${Gen.fmt(extra)}",
                "El total nuevo: $base + ${Gen.fmt(extra)} = ${Gen.fmt(ans)}",
                "Atajo: ahora es el ${100 + p}%, y $base × ${Gen.fmt((100 + p) / 100.0)} = ${Gen.fmt(ans)}"
            ),
            example = EX_AUMENTO,
            skillId = Curriculum.PCT_AUMENTO.id,
            format = format
        )
    }

    // ------------------------------------------------------------------
    // Variación porcentual: de A a B, ¿qué % cambió?
    // ------------------------------------------------------------------
    private val EX_VARIACION = WorkedExample(
        "Ejemplo: qué porcentaje cambió",
        listOf(
            "Una tienda bajó su precio de $200 a $150. ¿Qué % bajó?",
            "1) La diferencia: 200 − 150 = 50",
            "2) Se divide entre el valor INICIAL: 50 ÷ 200 = 0.25",
            "3) Se pasa a %: 0.25 × 100 = 25%",
            "Clave: siempre se divide entre el valor de PARTIDA."
        )
    )

    fun variacion(format: ExerciseFormat): MathQuestion {
        val subio = listOf(true, false).random()
        val a = listOf(200, 300, 400, 510, 800, 835).random()
        val p = listOf(10, 15, 20, 25, 30).random()
        val b = if (subio) a + a * p / 100 else a - a * p / 100
        val diff = kotlin.math.abs(a - b).toDouble()
        val ans = diff / a * 100
        val verbo = if (subio) "aumentó" else "disminuyó"

        return MathQuestion(
            question = "El número de alumnos de una escuela pasó de $a a $b. ¿Qué porcentaje $verbo?",
            options = Gen.numOptsSuffix(
                "%",
                ans,
                diff / b * 100,     // error: dividir entre el valor final
                100 - ans,          // error: dar el complemento
                diff                // error: dar la diferencia sin convertir
            ),
            answer = Gen.pct(ans),
            steps = listOf(
                "La diferencia: |$a − $b| = ${Gen.fmt(diff)}",
                "Se divide entre el valor inicial: ${Gen.fmt(diff)} ÷ $a = ${Gen.fmt(diff / a, 4)}",
                "Se pasa a porcentaje: ${Gen.fmt(diff / a, 4)} × 100 = ${Gen.pct(ans)}"
            ),
            example = EX_VARIACION,
            skillId = Curriculum.PCT_VARIACION.id,
            format = format
        )
    }
}
