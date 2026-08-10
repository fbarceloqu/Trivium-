package com.controlparental.kioscosuave.curriculum

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Utilidades compartidas por los generadores de ejercicios de 1° de secundaria.
 *
 * SOBRE LOS DISTRACTORES
 * ----------------------
 * Las opciones incorrectas NO son números al azar: cada una es el resultado de
 * un ERROR TÍPICO concreto. Si el alumno divide cuando debía multiplicar, o se
 * queda con el descuento en vez del precio final, esa respuesta equivocada
 * existe entre las opciones.
 *
 * Esto tiene dos ventajas: el ejercicio deja de resolverse por descarte, y
 * cuando la Fase 2 registre CUÁL opción incorrecta eligió, sabremos qué error
 * está cometiendo, no solo que falló.
 */
internal object Gen {

    /** Nombres para los problemas con contexto. */
    val NAMES = listOf(
        "Gretel", "Mateo", "Renata", "Diego", "Ximena",
        "Emiliano", "Camila", "Santiago", "Regina", "Andrés"
    )

    /** Formatea quitando ceros sobrantes: 4.830 -> "4.83", 45.0 -> "45". */
    fun fmt(v: Double, decimals: Int = 2): String =
        BigDecimal.valueOf(v)
            .setScale(decimals, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()

    fun pct(v: Double, decimals: Int = 2): String = "${fmt(v, decimals)}%"

    fun money(v: Double): String = "$${fmt(v)}"

    /**
     * Arma 4 opciones a partir de la correcta y los errores típicos.
     * Si dos errores coinciden en el mismo número, rellena con valores cercanos
     * para que siempre haya 4 opciones distintas.
     */
    fun opts(correct: String, vararg wrong: String): List<String> {
        val set = LinkedHashSet<String>()
        set += correct
        wrong.forEach { set += it }
        return set.toList().take(4).shuffled()
    }

    /** Igual que [opts] pero con números: garantiza 4 opciones distintas. */
    fun numOpts(correct: Double, vararg wrong: Double): List<String> =
        numOptsD(2, correct, *wrong)

    fun numOptsD(decimals: Int, correct: Double, vararg wrong: Double): List<String> {
        val set = LinkedHashSet<String>()
        set += fmt(correct, decimals)
        wrong.forEach { set += fmt(it, decimals) }
        // Relleno cuando los errores típicos dieron el mismo número.
        var delta = 1.0
        while (set.size < 4) {
            set += fmt(correct + delta, decimals)
            delta = if (delta > 0) -delta else -delta + 1.0
        }
        return set.toList().take(4).shuffled()
    }

    /** Versión con sufijo (%, °, cm...) aplicada a todas las opciones. */
    fun numOptsSuffix(suffix: String, correct: Double, vararg wrong: Double): List<String> {
        val set = LinkedHashSet<String>()
        set += fmt(correct) + suffix
        wrong.forEach { set += fmt(it) + suffix }
        var delta = 1.0
        while (set.size < 4) {
            set += fmt(correct + delta) + suffix
            delta = if (delta > 0) -delta else -delta + 1.0
        }
        return set.toList().take(4).shuffled()
    }

    /** Mediana de una lista (promedia los dos centrales si son pares). */
    fun median(data: List<Int>): Double {
        val s = data.sorted()
        val n = s.size
        return if (n % 2 == 1) s[n / 2].toDouble()
        else (s[n / 2 - 1] + s[n / 2]) / 2.0
    }

    /** Todas las modas: vacío si no hay (todos aparecen igual de veces). */
    fun modes(data: List<Int>): List<Int> {
        val counts = data.groupingBy { it }.eachCount()
        val max = counts.values.maxOrNull() ?: return emptyList()
        if (max == 1) return emptyList()
        return counts.filterValues { it == max }.keys.sorted()
    }
}
