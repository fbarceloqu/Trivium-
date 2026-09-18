import com.controlparental.kioscosuave.ChallengeEngine

/**
 * Arnés del modo spelling. Mismas invariantes que GenSmokeTest, más una propia:
 * en "¿Cuál comienza con la letra X?" debe haber EXACTAMENTE una opción que
 * empiece con X. Si hay dos, el niño que elige la otra se marca como error
 * aunque haya contestado bien.
 */
private val LISTAS = mapOf(
    // Palabras de las imágenes que se agregaron para spelling.
    "imagenes nuevas" to listOf(
        "sand", "sea", "summer", "sun", "top", "tap", "tiny", "bat",
        "bubble", "apple", "ball", "book", "car", "house", "pencil"
    ),
    // La lista por defecto de la letra B.
    "letra B" to listOf(
        "ball", "banana", "bear", "bee", "bird", "book", "box", "bread",
        "bus", "butterfly", "boat", "blue"
    ),
    // Una semana tipica centrada en una sola letra.
    "semana de la S" to listOf("sand", "sea", "summer", "sun", "sock", "snake")
)

fun main() {
    var fallas = 0
    val ambiguos = LinkedHashMap<String, Int>()
    val rotos = LinkedHashMap<String, Int>()
    var total = 0

    for ((nombre, lista) in LISTAS) {
        repeat(6000) {
            val ex = ChallengeEngine.randomEnglish(spellingWords = lista)
            total++

            if (ex.correctAnswer !in ex.options) {
                rotos.merge("$nombre: respuesta fuera de las opciones", 1, Int::plus)
            }
            if (ex.options.size != 4 || ex.options.distinct().size != 4) {
                rotos.merge("$nombre: no hay 4 opciones distintas -> ${ex.options}", 1, Int::plus)
            }

            // Formato "¿Cuál comienza con la letra X?"
            val m = Regex("letra ([A-Z])\\?").find(ex.question)
            if (m != null && ex.question.startsWith("¿Cuál comienza")) {
                val letra = m.groupValues[1].lowercase()
                val validas = ex.options.filter { it.lowercase().startsWith(letra) }
                if (validas.size > 1) {
                    ambiguos.merge("$nombre / ${ex.question}  opciones validas: $validas", 1, Int::plus)
                }
            }
        }
    }

    println("Ejercicios de spelling generados: $total\n")

    if (rotos.isNotEmpty()) {
        println("INVARIANTES ROTAS:")
        rotos.forEach { (k, v) -> println("  [$v x] $k") }
        fallas += rotos.size
    }

    if (ambiguos.isNotEmpty()) {
        println("PREGUNTAS CON DOS RESPUESTAS CORRECTAS (el nino puede acertar y verse marcado como error):")
        ambiguos.entries.sortedByDescending { it.value }.take(12).forEach { (k, v) -> println("  [$v x] $k") }
        val veces = ambiguos.values.sum()
        println("\n  -> $veces de $total ejercicios (${"%.1f".format(veces * 100.0 / total)}%) son ambiguos")
        fallas += ambiguos.size
    }

    println("\n" + "=".repeat(70))
    println(if (fallas == 0) "OK: el modo spelling no tiene invariantes rotas." else "FALLAS distintas: $fallas")
}
