package com.controlparental.kioscosuave.curriculum

import kotlin.math.abs

/**
 * CALIDAD DE LAS OPCIONES DE MATEMÁTICAS
 * ======================================
 *
 * Que haya cuatro opciones distintas no basta: un ejercicio está mal hecho si
 * la respuesta se puede elegir SIN resolverlo. Los casos reales que motivaron
 * esto:
 *
 *   «El 15% de una cantidad es 36, ¿cuál es la cantidad?»
 *   opciones: 240 · 5.4 · 30.6 · 51
 *
 * La correcta es la única grande; y si la pregunta habla de galletas, «5.4
 * galletas» se descarta sola. Aquí se detectan esas pistas involuntarias.
 *
 * Kotlin puro. Lo usan [Gen] para reparar opciones al generarlas y
 * tools/harness para auditar todos los generadores.
 */
object MathOptionValidator {

    /** Un número reconocido dentro de una opción, con lo que lo rodea. */
    data class Parsed(val value: Double, val shape: String, val hasDecimals: Boolean)

    private val fraction = Regex("^(-?\\d+)\\s*/\\s*(\\d+)$")
    private val number = Regex("-?\\d+(?:\\.\\d+)?")

    /**
     * Interpreta una opción como número: «$12.50», «45%», «3/4», «-7°», «12 cm».
     * [Parsed.shape] es la opción con el número sustituido por «#»: dos
     * opciones solo son comparables si tienen la misma forma (misma unidad).
     * Devuelve null si no es un único número (texto, pares «3 y 5», etc.).
     */
    fun parse(option: String): Parsed? {
        val t = option.trim().replace(",", "").replace("−", "-")
        fraction.find(t)?.let { m ->
            val d = m.groupValues[2].toDouble()
            if (d == 0.0) return null
            return Parsed(m.groupValues[1].toDouble() / d, "#/#", false)
        }
        val found = number.findAll(t).toList()
        if (found.size != 1) return null
        val raw = found[0].value
        val shape = t.replaceRange(found[0].range, "#").replace(Regex("\\s+"), " ")
        // Letras pegadas al número (2x, x²) son álgebra, no una cantidad.
        if (shape.any { it.isLetter() } && !shape.matches(Regex("^#\\s?[a-zA-Zµ°²³/ ]{1,12}$|^\\$#$"))) {
            if (!shape.startsWith("# ")) return null
        }
        return Parsed(raw.toDouble(), shape, raw.contains('.'))
    }

    private val countable = Regex(
        "\\b(alumnos?|alumnas?|personas?|niñ[oa]s?|estudiantes?|galletas?|árbol(es)?|páginas?|canicas?|" +
            "boletos?|libros?|dulces?|pelotas?|animales?|vacas?|gallinas?|autos?|coches?|sillas?|" +
            "cajas?|jugador(es)?|equipos?|votos?|piezas?|manzanas?|naranjas?|lápices|cuadernos?|" +
            "asistentes?|pasajeros?|trabajador(es)?|empleados?|monedas?|billetes?|fichas?|tarjetas?|" +
            "habitantes?|camisetas?|plumas?|juguetes?|botellas?|produce)\\b"
    )

    /** ¿La pregunta pide contar cosas que no se pueden partir? */
    fun asksForCount(question: String): Boolean {
        val q = question.lowercase()
        val asks = Regex("¿\\s*cuánt[oa]s\\s+([a-záéíóúñ]+)").findAll(q).map { it.groupValues[1] }.toList()
        return asks.any { countable.matches(it) }
    }

    fun errors(question: String, options: List<String>, answer: String): List<String> = buildList {
        if (options.size != 4) add("${options.size} opciones en vez de 4")
        if (options.distinct().size != options.size) add("opciones repetidas")
        if (options.count { it == answer } != 1) add("la respuesta no aparece exactamente una vez")
        if (options.size != 4 || options.count { it == answer } != 1) return@buildList

        val parsed = options.map(::parse)
        val ans = parsed[options.indexOf(answer)]

        // En «¿cuál de los siguientes es primo?» las opciones son ejemplares a
        // clasificar, no resultados de un cálculo: su tamaño o su forma no
        // delatan nada, y dos equivalentes entre los incorrectos no estorban.
        val selection = isSelection(question)

        // Dos opciones que valen lo mismo (0.5 y 0.50, 1/2 y 2/4): elegir la
        // "otra" se marcaría como error aunque el alumno haya resuelto bien.
        for (i in options.indices) for (j in i + 1 until options.size) {
            val a = parsed[i]; val b = parsed[j]
            val touchesAnswer = options[i] == answer || options[j] == answer
            if (a != null && b != null && sameUnit(a, b) && abs(a.value - b.value) < 1e-9 && (touchesAnswer || !selection))
                add("opciones equivalentes: «${options[i]}» y «${options[j]}»")
        }
        if (selection) return@buildList

        if (ans == null || parsed.any { it == null }) {
            // Opciones de texto: solo se vigila que la clave no destaque por tamaño.
            if (parsed.all { it == null }) {
                val others = options.filter { it != answer }.map { it.length }
                if (answer.length > others.max() * 1.6f && answer.length - others.max() > 12)
                    add("la respuesta destaca por ser mucho más larga")
            }
            return@buildList
        }
        val nums = parsed.filterNotNull()
        val wrong = nums.filter { it !== ans }

        // --- Forma: la clave no debe ser la única entera / la única con decimales.
        if (wrong.all { it.hasDecimals != ans.hasDecimals })
            add(if (ans.hasDecimals) "la respuesta es la única con decimales" else "la respuesta es la única entera")
        if (wrong.all { it.shape != ans.shape }) add("la respuesta es la única con su unidad o formato")

        // --- Contexto: no se pueden contar 5.4 galletas ni −3 alumnos.
        if (asksForCount(question)) {
            // Si la propia respuesta es imposible, el defecto está en los datos
            // del problema: no se arregla cambiando opciones, hay que regenerar.
            if (ans.value != Math.floor(ans.value)) add("$ANSWER_IMPOSSIBLE: ${fmtValue(ans.value)}")
            wrong.filter { it.hasDecimals && it.value != Math.floor(it.value) }
                .forEach { add("cantidad imposible para algo que se cuenta: ${fmtValue(it.value)}") }
            if (ans.value > 0) wrong.filter { it.value <= 0 }
                .forEach { add("cantidad imposible para algo que se cuenta: ${fmtValue(it.value)}") }
        }

        // --- Magnitud: la clave no debe ser un extremo aislado. Solo delata
        // cuando TODOS los distractores quedan del mismo lado y lejos (240
        // frente a 5.4, 30.6 y 51). Si la respuesta queda entre ellos, o alguno
        // se le parece en tamaño, hay que calcular para decidir.
        if (ans.value != 0.0 && nums.all { it.value >= 0 } || nums.all { it.value <= 0 }) {
            val a = abs(ans.value)
            val allSmaller = wrong.all { abs(it.value) < a / BAND }
            val allLarger = a > 0 && wrong.all { abs(it.value) > a * BAND }
            if (allSmaller) add("la respuesta es mucho mayor que todos los distractores")
            if (allLarger) add("la respuesta es mucho menor que todos los distractores")
        }
    }

    /** Preguntas de clasificar ejemplares: «¿Cuál de los siguientes…?», «¿Cuál NO es…?». */
    fun isSelection(question: String): Boolean {
        val q = question.lowercase()
        return Regex("cuál(es)? de (los|las|estos|estas)").containsMatchIn(q) ||
            Regex("cuál .*no").containsMatchIn(q) || "cuál opción" in q
    }

    const val ANSWER_IMPOSSIBLE = "la respuesta correcta es una cantidad imposible"

    /** Factor dentro del cual un distractor "se parece" en tamaño a la respuesta. */
    const val BAND = 3.0

    fun sameUnit(a: Parsed, b: Parsed): Boolean =
        a.shape == b.shape || setOf(a.shape, b.shape) == setOf("#", "#/#")

    private fun fmtValue(v: Double): String = if (v == Math.floor(v)) v.toLong().toString() else v.toString()
}

/**
 * Repara las opciones de un ejercicio cuando [MathOptionValidator] encuentra
 * una pista involuntaria.
 *
 * Conserva todos los distractores originales que puede (son errores típicos y
 * la memoria registra cuál se eligió) y sustituye solo los que delatan la
 * respuesta por valores cercanos, con el mismo formato y unidad.
 *
 * Nunca toca la respuesta ni el enunciado, así que la calificación no cambia.
 * Si no encuentra una combinación válida, devuelve las opciones como estaban.
 */
object MathOptionRepair {

    fun repair(
        question: String,
        options: List<String>,
        answer: String,
        rnd: kotlin.random.Random = kotlin.random.Random
    ): List<String> {
        val v = MathOptionValidator
        if (v.errors(question, options, answer).isEmpty()) return options
        // En preguntas de clasificar no se pueden inventar ejemplares.
        if (v.isSelection(question)) return options
        val ans = v.parse(answer) ?: return options
        if (options.count { it == answer } != 1) return options

        val existing = options.filter { it != answer }.distinct()
        val near = near(answer, ans, v.asksForCount(question), rnd)
            .filter { it != answer && it !in existing }.take(10)
        fun valid(wrong: List<String>) =
            wrong.size == 3 && v.errors(question, wrong + answer, answer).isEmpty()

        // Primero se intenta conservar 3 originales, luego 2, 1 y ninguno.
        for (keep in minOf(3, existing.size) downTo 0) {
            for (kept in subsets(existing, keep)) {
                for (fill in subsets(near, 3 - keep)) {
                    val wrong = kept + fill
                    if (valid(wrong)) return (wrong + answer).shuffled(rnd)
                }
            }
        }
        return options
    }

    private fun <T> subsets(items: List<T>, k: Int): List<List<T>> {
        if (k == 0) return listOf(emptyList())
        if (items.size < k) return emptyList()
        val head = items.first()
        val tail = items.drop(1)
        return subsets(tail, k - 1).map { listOf(head) + it } + subsets(tail, k)
    }

    private val fractionText = Regex("^(-?\\d+)\\s*/\\s*(\\d+)$")
    private val numberText = Regex("-?\\d+(?:\\.\\d+)?")

    /**
     * Valores vecinos de la respuesta, escritos igual que ella: mismo prefijo o
     * sufijo, mismos decimales y la misma "redondez" (si la respuesta es 240,
     * los vecinos van de 10 en 10; si es 4.25, de décima en décima).
     */
    private fun near(
        answer: String,
        ans: MathOptionValidator.Parsed,
        count: Boolean,
        rnd: kotlin.random.Random
    ): List<String> {
        val frac = fractionText.find(answer.trim())
        if (frac != null) {
            val n = frac.groupValues[1].toInt()
            val d = frac.groupValues[2].toInt()
            val same = (1..d + 2).filter { it != n }.map { "$it/$d" }
            val other = listOf(d - 1, d + 1, d + 2).filter { it > 0 }.map { "$n/$it" }
            return (same + other).shuffled(rnd)
        }
        val raw = numberText.find(answer.replace("−", "-").replace(",", ""))?.value ?: return emptyList()
        val decimals = raw.substringAfter('.', "").length
        val a = ans.value
        val size = abs(a)
        val step: Double = if (decimals == 0) when {
            size >= 1000 && a % 100.0 == 0.0 -> 100.0
            size >= 50 && a % 10.0 == 0.0 -> 10.0
            size >= 20 && a % 5.0 == 0.0 -> 5.0
            else -> 1.0
        } else maxOf(
            Math.pow(10.0, -decimals.toDouble()),
            Math.pow(10.0, Math.floor(Math.log10(size.coerceAtLeast(1e-9))) - 1)
        )
        val values = (1..5).flatMap { k -> listOf(a + k * step, a - k * step) }
            .filter { if (count || a > 0) it > 0 else true }
        // Los más cercanos primero, con algo de azar para no repetir siempre.
        val ordered = values.take(6).shuffled(rnd) + values.drop(6).shuffled(rnd)
        return ordered.map { ans.shape.replace("#", Gen.fmt(it, decimals)) }.distinct()
    }
}
