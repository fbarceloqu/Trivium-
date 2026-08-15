import com.controlparental.kioscosuave.ui.Adaptive
import com.controlparental.kioscosuave.ui.LayoutMode
import com.controlparental.kioscosuave.ui.Level

/**
 * Las DOS tablets reales de la familia, en sus dos orientaciones.
 *
 * SM-T500 (Galaxy Tab A7 10.4", 2000x1200) -> secundaria -> modo normal
 * SM-X210 (Galaxy Tab A9  8.7", 1340x800)  -> 1o primaria -> modo grande
 *
 * De cada una se prueban las dos densidades que Android puede asignarle, para
 * no depender de acertar el bucket exacto. Se descuentan ~40dp de barras.
 */
private data class Device(val name: String, val w: Float, val h: Float, val level: Level)

private val DEVICES = listOf(
    // --- SM-T500, secundaria -> lenguaje SECONDARY ---
    Device("T500 horiz @1.5x", 1333f, 760f, Level.SECONDARY),
    Device("T500 vert  @1.5x",  800f, 1293f, Level.SECONDARY),
    Device("T500 horiz @2.0x", 1000f, 560f, Level.SECONDARY),
    Device("T500 vert  @2.0x",  600f,  960f, Level.SECONDARY),
    // --- SM-X210, 1o primaria -> lenguaje PRIMARY ---
    Device("X210 horiz @1.5x",  893f, 493f, Level.PRIMARY),
    Device("X210 vert  @1.5x",  533f, 853f, Level.PRIMARY),
    Device("X210 horiz @1.33x",1007f, 561f, Level.PRIMARY),
    Device("X210 vert  @1.33x", 755f, 967f, Level.PRIMARY)
)

private var fallas = 0
private fun check(cond: Boolean, msg: String) {
    if (!cond) { println("  FALLA: $msg"); fallas++ }
}

fun main() {
    println("=".repeat(92))
    println("LAS DOS TABLETS DE LA FAMILIA")
    println("=".repeat(92))
    println(String.format("%-20s %6s %6s %6s %-9s %6s %6s %6s %7s %7s",
        "dispositivo", "ancho", "alto", "escala", "modo", "preg", "opcion", "tactil", "hero", "dibujo"))
    println("-".repeat(92))

    for (d in DEVICES) {
        val m = Adaptive.metrics(d.w, d.h, d.level)
        println(String.format("%-20s %6.0f %6.0f %6.2f %-9s %6.1f %6.1f %6.0f %7.0f %7.0f",
            d.name, d.w, d.h, m.scale, m.mode.name, m.question, m.option,
            m.minTouch, m.heroImageMax, m.countImageMax))

        val piso = if (d.level.isPrimary) 15f else 12f
        check(m.question >= piso, "${d.name}: pregunta ilegible")
        check(m.option >= piso, "${d.name}: opcion ilegible")
        check(m.minTouch >= 48f, "${d.name}: area tactil < 48dp")
        check(m.question > m.statusLine, "${d.name}: la info secundaria compite con la pregunta")
        check(m.question >= m.option, "${d.name}: la pregunta no domina")
        check(m.contentMaxWidth <= d.w + 0.01f, "${d.name}: columna mas ancha que la pantalla")

        // En horizontal SIEMPRE debe partirse en dos paneles: es el caso que
        // provocaba el desbordamiento.
        val esHorizontal = d.w > d.h
        check(!esHorizontal || m.mode == LayoutMode.TWO_PANE,
            "${d.name}: horizontal pero NO usa dos paneles")
        check(esHorizontal || m.mode == LayoutMode.STACKED,
            "${d.name}: vertical pero usa dos paneles")

        // --- presupuesto vertical: cabe todo sin desplazarse? ---
        // Se suma lo que ocupan las zonas fijas y se comprueba que quede sitio
        // razonable para el dibujo y las respuestas.
        val header = m.lineHeight(m.greeting) + (if (m.compactHeader) 0f else m.lineHeight(m.greetingSub))
        val tabs = m.minTouch
        val barra = m.lineHeight(m.stageTitle) + m.lineHeight(m.statusLine)
        val fijo = m.pagePad * 2 + header + tabs + barra + m.sectionGap * 3

        val filasResp = if (m.mode == LayoutMode.TWO_PANE) 2 else 2
        val respuestas = filasResp * (m.answerMinHeight + m.itemGap)
        val botonSiguiente = m.minTouch + m.itemGap
        // En dos paneles las respuestas van al lado, pero su alto SI compite con
        // la retroalimentacion y el boton dentro de esa misma columna.
        val columnaResp = respuestas + botonSiguiente + m.lineHeight(m.feedback) * 3

        if (m.mode == LayoutMode.TWO_PANE) {
            check(columnaResp <= d.h - fijo + 0.01f,
                "${d.name}: respuestas+feedback+boton (${"%.0f".format(columnaResp)}dp) no caben en la columna (${"%.0f".format(d.h - fijo)}dp)")
            check(m.answerMinHeight >= m.minTouch, "${d.name}: boton de respuesta por debajo del minimo tactil")
        }

        val libre = if (m.mode == LayoutMode.TWO_PANE) {
            // En dos paneles las respuestas van al lado, no debajo.
            d.h - fijo
        } else {
            d.h - fijo - respuestas - botonSiguiente
        }

        check(libre > 0f, "${d.name}: no queda espacio para el dibujo (libre=$libre)")
        check(libre >= m.countImageMin * 2,
            "${d.name}: el dibujo quedaria demasiado chico (libre=${"%.0f".format(libre)}dp)")

        // La rejilla de dibujos debe caber en ese hueco, para 1..12 objetos.
        val paneW = if (m.mode == LayoutMode.TWO_PANE) d.w / 2f else m.contentMaxWidth
        val areaW = paneW - m.pagePad * 2 - m.cardPad * 2
        for (n in 1..12) {
            val size = Adaptive.fitGridItem(n, areaW, libre, m.itemGap, m.countImageMin, m.countImageMax)
            val cols = Adaptive.columnsFor(size, areaW, m.itemGap)
            val rows = Adaptive.rowsFor(n, size, areaW, m.itemGap)
            check(cols * (size + m.itemGap) - m.itemGap <= areaW + 0.01f,
                "${d.name}/$n dibujos: se desborda a lo ancho")
            check(rows * (size + m.itemGap) - m.itemGap <= libre + 0.01f,
                "${d.name}/$n dibujos: se desborda a lo alto")
            check(rows * cols >= n, "${d.name}/$n dibujos: no caben todos")
        }
    }

    // =============================================================
    // Los dos lenguajes deben DIFERENCIARSE, no ser el mismo mas chico
    // =============================================================
    println("\n### PRIMARY vs SECONDARY, a igual pantalla ###\n")
    val W = 800f; val H = 1280f
    val p = Adaptive.metrics(W, H, Level.PRIMARY)
    val s = Adaptive.metrics(W, H, Level.SECONDARY)
    println(String.format("%-26s %10s %10s", "", "PRIMARY", "SECONDARY"))
    fun fila(n: String, a: Float, b: Float) =
        println(String.format("%-26s %10.1f %10.1f", n, a, b))
    fila("pregunta", p.question, s.question)
    fila("opcion", p.option, s.option)
    fila("hero (dibujo grande)", p.heroImageMax, s.heroImageMax)
    fila("dibujo de contar", p.countImageMax, s.countImageMax)
    fila("separacion de secciones", p.sectionGap, s.sectionGap)
    fila("alto de pestana", p.tabHeight, s.tabHeight)
    fila("alto de respuesta", p.answerMinHeight, s.answerMinHeight)
    println("  dibujos decorativos:      ${p.showsDecorativeVisuals}       ${s.showsDecorativeVisuals}")

    check(p.showsDecorativeVisuals && !s.showsDecorativeVisuals,
        "los dibujos decorativos deberian existir solo en PRIMARY")
    check(p.heroImageMax > s.heroImageMax * 1.4f,
        "en PRIMARY el dibujo deberia ser CLARAMENTE protagonista, no solo un poco mayor")
    check(p.sectionGap > s.sectionGap,
        "SECONDARY deberia ser mas denso (menos separacion)")
    // Densidad: en SECONDARY la distancia entre pregunta y opcion es menor,
    // porque conviven mas elementos en pantalla.
    check((p.question - p.option) > (s.question - s.option),
        "la jerarquia de SECONDARY deberia ser mas apretada que la de PRIMARY")
    check(s.question >= 12f && s.option >= 12f,
        "SECONDARY no debe volverse ilegible por ser mas denso")
    check(p.minTouch >= 48f && s.minTouch >= 48f, "area tactil por debajo de 48dp")

    // El mapeo desde el perfil no debe dejar ningun grado fuera.
    check(Level.forGrade(com.controlparental.kioscosuave.GradeLevel.PREESCOLAR) == Level.PRIMARY,
        "preescolar deberia usar PRIMARY")
    check(Level.forGrade(com.controlparental.kioscosuave.GradeLevel.PRIMARIA) == Level.PRIMARY,
        "primaria deberia usar PRIMARY")
    check(Level.forGrade(com.controlparental.kioscosuave.GradeLevel.SECUNDARIA) == Level.SECONDARY,
        "secundaria deberia usar SECONDARY")

    println("\n" + "=".repeat(92))
    if (fallas == 0) println("OK: las dos tablets cumplen todas las invariantes, y los dos lenguajes se distinguen.")
    else println("FALLAS: $fallas")
}
