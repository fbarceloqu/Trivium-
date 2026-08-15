package com.controlparental.kioscosuave.ui

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * SISTEMA DE DIMENSIONES ADAPTATIVAS
 * ==================================
 *
 * Kotlin PURO a propósito: este archivo no importa nada de Compose ni de
 * Android. Toda la aritmética de tamaños vive aquí para poder ejecutarla y
 * verificarla fuera del emulador; la capa visual ([Adaptive.kt]) solo convierte
 * estos números a `dp`/`sp` y los reparte.
 *
 * QUÉ ESTABA MAL ANTES
 * --------------------
 * La escala se calculaba con `screenWidthDp / 400f`. En el emulador real
 * (1013 × 456 dp en horizontal) eso daba 2.53, recortado al máximo de 1.5:
 * la interfaz crecía al tamaño MÁXIMO justo en la pantalla con MENOS altura
 * disponible. Se escalaba por la dimensión equivocada.
 *
 * QUÉ HACE AHORA
 * --------------
 * 1. La escala toma el MENOR entre el ancho útil y la altura, así la dimensión
 *    escasa es la que manda.
 * 2. Las pantallas bajas y anchas cambian a dos paneles (dibujo | respuestas)
 *    en vez de apilar todo y obligar a desplazarse.
 * 3. Los dibujos NO tienen tamaño fijo: [fitGridItem] calcula el mayor tamaño
 *    que permite que TODOS quepan en el espacio realmente disponible.
 *
 * Todas las medidas son Float en unidades dp (o sp para texto).
 */

/** Clase de tamaño de una dimensión de la pantalla. */
enum class SizeClass { COMPACT, MEDIUM, EXPANDED }

/** Cómo se reparte el ejercicio en la pantalla. */
enum class LayoutMode {
    /** Vertical: pregunta arriba, respuestas abajo. Retrato y pantallas altas. */
    STACKED,

    /** Horizontal: pregunta a la izquierda, respuestas a la derecha. */
    TWO_PANE
}

/**
 * Todos los tamaños que usa la interfaz, ya resueltos para una pantalla
 * concreta. Ninguna pantalla debe escribir un número suelto: todo sale de aquí,
 * para poder reajustar la app entera desde un solo lugar.
 */
data class AdaptiveMetrics(
    val widthDp: Float,
    val heightDp: Float,
    val widthClass: SizeClass,
    val heightClass: SizeClass,
    val mode: LayoutMode,
    /** Lenguaje visual según la edad. Eje perpendicular al del espacio. */
    val level: Level,
    /**
     * Si los dibujos pueden ser protagonistas aunque no sean imprescindibles.
     * En PRIMARY sí: ayudan a entender el concepto. En SECONDARY no: una imagen
     * solo aparece si aporta valor educativo (un diagrama, una figura), nunca
     * como decoración.
     */
    val showsDecorativeVisuals: Boolean,
    val scale: Float,
    /** Ancho máximo de la columna de contenido (centra en pantallas muy anchas). */
    val contentMaxWidth: Float,

    // --- espacios ---
    val pagePad: Float,
    val sectionGap: Float,
    val itemGap: Float,
    val cardPad: Float,
    val corner: Float,

    // --- tipografía (sp) ---
    val greeting: Float,
    val greetingSub: Float,
    val tabLabel: Float,
    val stageTitle: Float,
    val statusLine: Float,
    val instruction: Float,
    val question: Float,
    val option: Float,
    val feedback: Float,
    val buttonLabel: Float,

    // --- componentes ---
    val actionIcon: Float,
    val minTouch: Float,
    /**
     * Alto mínimo de un botón de respuesta.
     *
     * En dos paneles crece con la altura disponible para que el ejercicio
     * OCUPE la pantalla en vez de quedar apelotonado arriba con dos tercios
     * vacíos. En una tablet de 10" en horizontal salen botones de ~120dp:
     * áreas táctiles muy cómodas que además llenan el hueco sin inventar
     * contenido. En apilado se queda en [minTouch], que ahí ya funciona.
     */
    val answerMinHeight: Float,
    /** Alto de las pestanas de materia. Mas compacto que [minTouch]: son
     *  botones muy anchos, asi que no necesitan 48dp de alto para ser comodos. */
    val tabHeight: Float,
    val optionVPad: Float,
    val buttonVPad: Float,

    // --- dibujos ---
    val heroImageMax: Float,
    val heroImageMin: Float,
    val countImageMax: Float,
    val countImageMin: Float,

    /** true si conviene esconder el subtítulo del saludo por falta de altura. */
    val compactHeader: Boolean
) {
    /** Interlineado proporcional: evita el bug de texto encimado al escalar. */
    fun lineHeight(fontSize: Float): Float = fontSize * 1.28f
}

object Adaptive {

    /** Pantalla de referencia: tablet mediana en retrato. Escala = 1.0 */
    const val REF_W = 600f
    const val REF_H = 900f

    /**
     * Altura de referencia en dos paneles. Menor que [REF_H] porque el
     * contenido se reparte en dos columnas: la pregunta ya no va encima de las
     * respuestas, así que cada una necesita bastante menos altura.
     */
    const val REF_H_TWO_PANE = 560f

    /** Mínimo táctil recomendado por las guías de accesibilidad de Android. */
    const val MIN_TOUCH = 48f

    /** Nunca escalar por debajo/encima de esto, pase lo que pase. */
    private const val SCALE_MIN = 0.70f
    private const val SCALE_MAX = 1.45f

    /** Pisos absolutos de legibilidad, en sp. */
    private const val TEXT_FLOOR = 12f
    private const val TEXT_FLOOR_BIG = 15f

    fun widthClassOf(w: Float): SizeClass = when {
        w < 600f -> SizeClass.COMPACT
        w < 840f -> SizeClass.MEDIUM
        else -> SizeClass.EXPANDED
    }

    fun heightClassOf(h: Float): SizeClass = when {
        h < 500f -> SizeClass.COMPACT
        h < 750f -> SizeClass.MEDIUM
        else -> SizeClass.EXPANDED
    }

    /**
     * Dos paneles en cuanto la pantalla es claramente apaisada y hay ancho
     * suficiente para partirla. En horizontal la altura SIEMPRE es el recurso
     * escaso: apilar obliga a desplazarse mientras media pantalla queda vacía.
     *
     * La regla es por PROPORCIÓN, no por un umbral de altura fijo: con un
     * umbral de "alto < 500" la tablet del usuario (893 × 533 dp en horizontal)
     * se quedaba apilada justo en el caso que más necesita los dos paneles.
     *
     * El mínimo de 560 dp de ancho evita partir pantallas donde cada panel
     * quedaría demasiado angosto para leerse.
     */
    fun modeOf(widthDp: Float, heightDp: Float): LayoutMode =
        if (widthDp >= 560f && widthDp > heightDp * 1.25f)
            LayoutMode.TWO_PANE
        else
            LayoutMode.STACKED

    /**
     * Ancho máximo de la columna de contenido en modo apilado. En una tablet
     * grande en retrato (1024 dp) estirar el texto a todo lo ancho da renglones
     * larguísimos e incómodos de leer; se centra la columna en su lugar.
     */
    fun contentMaxWidthOf(widthDp: Float, mode: LayoutMode): Float =
        if (mode == LayoutMode.STACKED) min(widthDp, 820f) else widthDp

    /**
     * Escala global. Toma el MENOR entre ancho útil y altura: manda la
     * dimensión escasa, que es justo lo que fallaba antes.
     */
    fun scaleOf(widthDp: Float, heightDp: Float, mode: LayoutMode): Float {
        // En dos paneles cada columna dispone de la mitad del ancho...
        val usableW = if (mode == LayoutMode.TWO_PANE) widthDp / 2f else widthDp
        // ...pero también necesita MENOS altura, porque el contenido se reparte
        // en horizontal en vez de apilarse. Exigirle los mismos 900dp que al
        // modo apilado penalizaba de más: en una tablet de 10" en horizontal la
        // escala salía 0.84 y todo se veía pequeño con media pantalla vacía.
        val refH = if (mode == LayoutMode.TWO_PANE) REF_H_TWO_PANE else REF_H
        return min(usableW / REF_W, heightDp / refH).coerceIn(SCALE_MIN, SCALE_MAX)
    }

    /**
     * Resuelve todos los tamaños para una pantalla y un nivel.
     *
     * Cada [Level] es un CONJUNTO DE BASES distinto, no un multiplicador: no se
     * trata de que secundaria sea "primaria más chica". PRIMARY es amplio y
     * visual; SECONDARY es denso y textual, con la letra cómoda pero cabiendo
     * más información a la vez. La escala del espacio sigue mandando encima de
     * ambos, así que ningún nivel puede llegar a "no cabe".
     */
    fun metrics(widthDp: Float, heightDp: Float, level: Level): AdaptiveMetrics {
        val mode = modeOf(widthDp, heightDp)
        val scale = scaleOf(widthDp, heightDp, mode)
        val big = level.isPrimary
        val floor = if (big) TEXT_FLOOR_BIG else TEXT_FLOOR
        val touch = max((if (big) 56f else 48f) * scale, MIN_TOUCH)

        // Reparto vertical en dos paneles: descontada la cabecera (~200dp entre
        // saludo, pestañas y progreso), lo que queda se reparte entre las dos
        // filas de respuestas, la retroalimentación y el botón de continuar.
        // El divisor 4.5 deja margen para esos dos últimos.
        val answerH = if (mode == LayoutMode.TWO_PANE)
            ((heightDp - 200f) / 4.5f).coerceIn(touch, 150f)
        else touch

        // Texto: escala con piso de legibilidad.
        fun t(base: Float) = max(base * scale, floor)
        // Medidas de componente: escala con piso propio.
        fun d(base: Float, minimum: Float = 4f) = max(base * scale, minimum)

        return if (big) AdaptiveMetrics(
            widthDp = widthDp, heightDp = heightDp,
            widthClass = widthClassOf(widthDp), heightClass = heightClassOf(heightDp),
            mode = mode, scale = scale,
            level = level, showsDecorativeVisuals = big,
            contentMaxWidth = contentMaxWidthOf(widthDp, mode),

            pagePad = d(18f, 10f),
            sectionGap = d(12f, 6f),
            itemGap = d(8f, 4f),
            cardPad = d(18f, 8f),
            corner = d(20f, 12f),

            greeting = t(26f),
            greetingSub = t(15f),
            tabLabel = t(18f),
            stageTitle = t(20f),
            statusLine = t(14f),
            instruction = t(21f),
            question = t(30f),
            option = t(24f),
            feedback = t(18f),
            buttonLabel = t(20f),

            actionIcon = d(34f, 24f),
            minTouch = touch,
            answerMinHeight = answerH,
            tabHeight = max(42f * scale, 38f),
            optionVPad = d(18f, 10f),
            buttonVPad = d(16f, 10f),

            heroImageMax = d(200f, 80f),
            heroImageMin = d(72f, 48f),
            countImageMax = d(96f, 40f),
            countImageMin = d(34f, 22f),

            compactHeader = heightDp < 420f
        ) else AdaptiveMetrics(
            widthDp = widthDp, heightDp = heightDp,
            widthClass = widthClassOf(widthDp), heightClass = heightClassOf(heightDp),
            mode = mode, scale = scale,
            level = level, showsDecorativeVisuals = big,
            contentMaxWidth = contentMaxWidthOf(widthDp, mode),

            pagePad = d(16f, 10f),
            sectionGap = d(10f, 6f),
            itemGap = d(8f, 4f),
            cardPad = d(14f, 8f),
            corner = d(16f, 10f),

            // Texto cómodo de leer, pero con jerarquía más apretada que en
            // PRIMARY: la diferencia entre pregunta y opción es menor, así cabe
            // más contenido a la vez sin que nada quede pequeño.
            greeting = t(20f),
            greetingSub = t(13f),
            tabLabel = t(14f),
            stageTitle = t(15f),
            statusLine = t(12f),
            instruction = t(17f),
            question = t(23f),
            // La opción va MÁS cerca de la pregunta que en PRIMARY (5 puntos de
            // diferencia frente a 6): en secundaria las respuestas son frases,
            // no números sueltos, y necesitan peso propio para leerse cómodas.
            option = t(18f),
            feedback = t(14f),
            buttonLabel = t(15f),

            actionIcon = d(26f, 20f),
            minTouch = touch,
            answerMinHeight = answerH,
            tabHeight = max(38f * scale, 34f),
            optionVPad = d(12f, 8f),
            buttonVPad = d(12f, 8f),

            // Dibujos contenidos: en secundaria una imagen aparece solo cuando
            // aporta (una figura geométrica, un diagrama), nunca como adorno,
            // así que no debe competir con el enunciado.
            heroImageMax = d(110f, 56f),
            heroImageMin = d(48f, 36f),
            countImageMax = d(54f, 28f),
            countImageMin = d(24f, 18f),

            compactHeader = heightDp < 420f
        )
    }

    /**
     * El mayor tamaño de elemento con el que [count] dibujos caben COMPLETOS en
     * un área de [maxW] × [maxH], acomodados en rejilla con [gap] de separación.
     *
     * Esto sustituye al `chunked(5)` con tamaño fijo, que se recortaba en
     * pantallas angostas y desbordaba en las bajas. Aquí el tamaño se deduce
     * del espacio que realmente hay, así que por construcción siempre cabe
     * (salvo que ni siquiera quepa uno de tamaño mínimo, caso en el que se
     * devuelve el mínimo y el contenedor decidirá).
     */
    fun fitGridItem(
        count: Int,
        maxW: Float,
        maxH: Float,
        gap: Float,
        minSize: Float,
        maxSize: Float
    ): Float {
        if (count <= 0) return minSize
        if (maxW <= 0f || maxH <= 0f) return minSize

        var size = maxSize
        while (size > minSize) {
            val cols = columnsFor(size, maxW, gap)
            val rows = ceil(count.toFloat() / cols).toInt()
            val neededH = rows * (size + gap) - gap
            if (neededH <= maxH) return size
            size -= 2f
        }
        return minSize
    }

    /** Cuántos elementos de [size] caben a lo ancho de [maxW] con [gap]. */
    fun columnsFor(size: Float, maxW: Float, gap: Float): Int =
        max(1, ((maxW + gap) / (size + gap)).toInt())

    /** Filas que ocupan [count] elementos de [size] en [maxW]. */
    fun rowsFor(count: Int, size: Float, maxW: Float, gap: Float): Int =
        if (count <= 0) 0 else ceil(count.toFloat() / columnsFor(size, maxW, gap)).toInt()
}
