package com.controlparental.kioscosuave.curriculum

/**
 * PISTAS PROGRESIVAS
 * ==================
 *
 * Cada ejercicio trae su procedimiento paso a paso, que hoy solo se muestra
 * DESPUÉS de responder. Los primeros pasos sirven como pista antes de
 * responder, siempre que no contengan el resultado.
 *
 * Regla: se ofrecen los pasos en orden y se corta en el primero que mencione
 * la respuesta. Es deliberadamente conservador: si la respuesta es «5» y un
 * paso dice «5 cuadernos», ese paso tampoco se muestra. Preferible quedarse
 * corto de pistas que regalar el resultado.
 *
 * Kotlin puro: lo verifica MathOptionsTest sobre todos los generadores.
 */
object MathHints {
    /** Como mucho dos pistas: orientan el planteamiento, no resuelven. */
    const val MAX = 2

    fun safe(steps: List<String>, answer: String): List<String> {
        val out = ArrayList<String>()
        for (step in steps) {
            if (out.size >= MAX || reveals(step, answer)) break
            if (step.isNotBlank()) out += step
        }
        // El último paso del procedimiento es, por definición, la solución.
        return if (out.size == steps.size) out.dropLast(1) else out
    }

    /** ¿Este texto contiene la respuesta (como número suelto o como texto)? */
    fun reveals(text: String, answer: String): Boolean {
        val core = answer.trim().trim('$', '%', '°').trim()
        if (core.isEmpty()) return true
        val t = text.replace(",", "")
        return if (core.any { it.isDigit() }) {
            // «12» no debe coincidir dentro de «120» ni de «3.12».
            Regex("(?<![\\d.])" + Regex.escape(core.replace(",", "")) + "(?![\\d]|\\.\\d)").containsMatchIn(t)
        } else t.contains(core, ignoreCase = true)
    }
}
