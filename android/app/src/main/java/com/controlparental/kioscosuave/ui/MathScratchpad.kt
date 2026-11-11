package com.controlparental.kioscosuave.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * PIZARRA PARA RESOLVER
 * =====================
 *
 * Hoja de borrador a pantalla completa para hacer las operaciones con el dedo
 * o con un lápiz óptico, sin salir del ejercicio.
 *
 * Es SOLO una ayuda para pensar:
 *  - No envía respuestas, no califica y no toca la memoria de aprendizaje.
 *  - Los trazos pertenecen a la pregunta actual: se conservan al cerrar y
 *    volver a abrir, y se borran al pasar a la siguiente.
 *
 * El enunciado queda arriba para no tener que memorizarlo, y las pistas se
 * piden de una en una; nunca contienen el resultado (ver MathHints).
 */

/** Un trazo: los puntos por los que pasó el dedo, en orden. */
internal typealias InkStroke = SnapshotStateList<Offset>

@Composable
internal fun MathScratchpad(
    question: String,
    strokes: SnapshotStateList<InkStroke>,
    hints: List<String>,
    shownHints: Int,
    onMoreHints: () -> Unit,
    onClose: () -> Unit
) {
    val m = LocalMetrics.current
    val scheme = MaterialTheme.colorScheme
    val ink = scheme.onSurface
    val grid = scheme.onSurface.copy(alpha = 0.07f)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = scheme.background) {
            Column(Modifier.fillMaxSize().padding(m.pagePad.dp)) {
                // Enunciado: acotado en altura para que la hoja mande.
                Text(
                    question,
                    fontSize = m.feedback.sp,
                    lineHeight = m.lineHeight(m.feedback).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = (m.heightDp * 0.22f).dp)
                        .verticalScroll(rememberScrollState())
                )
                hints.take(shownHints).forEachIndexed { i, hint ->
                    Text(
                        "Pista ${i + 1}: $hint",
                        fontSize = m.statusLine.sp,
                        lineHeight = m.lineHeight(m.statusLine).sp,
                        color = scheme.tertiary,
                        modifier = Modifier.fillMaxWidth().padding(top = (m.itemGap / 2).dp)
                    )
                }
                Spacer(Modifier.size(m.itemGap.dp))

                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(m.corner.dp))
                        .background(scheme.surface)
                ) {
                    Canvas(
                        Modifier
                            .fillMaxSize()
                            // Dedo o lápiz: cada contacto es un trazo nuevo. Un
                            // toque sin arrastrar deja un punto (decimales).
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    val stroke: InkStroke = mutableStateListOf(down.position)
                                    strokes.add(stroke)
                                    down.consume()
                                    while (true) {
                                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) break
                                        stroke.add(change.position)
                                        change.consume()
                                    }
                                }
                            }
                    ) {
                        // Cuadrícula tenue: ayuda a alinear columnas en sumas,
                        // restas y divisiones.
                        val cell = 32.dp.toPx()
                        var x = cell
                        while (x < size.width) { drawLine(grid, Offset(x, 0f), Offset(x, size.height)); x += cell }
                        var y = cell
                        while (y < size.height) { drawLine(grid, Offset(0f, y), Offset(size.width, y)); y += cell }

                        val width = 3.5.dp.toPx()
                        strokes.forEach { stroke ->
                            if (stroke.size == 1) drawCircle(ink, width * 0.7f, stroke.first())
                            else if (stroke.size > 1) {
                                val path = Path().apply {
                                    moveTo(stroke.first().x, stroke.first().y)
                                    for (i in 1 until stroke.size) lineTo(stroke[i].x, stroke[i].y)
                                }
                                drawPath(path, ink, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            }
                        }
                    }
                    if (strokes.isEmpty()) {
                        Text(
                            "Escribe aquí tus operaciones con el dedo",
                            fontSize = m.statusLine.sp,
                            color = scheme.onSurface.copy(alpha = 0.35f),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(Modifier.size(m.itemGap.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(m.itemGap.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) },
                        enabled = strokes.isNotEmpty(),
                        shape = RoundedCornerShape(m.corner.dp),
                        modifier = Modifier.weight(1f).heightIn(min = m.minTouch.dp)
                    ) { Text("Deshacer", fontSize = m.buttonLabel.sp, maxLines = 1) }
                    OutlinedButton(
                        onClick = { strokes.clear() },
                        enabled = strokes.isNotEmpty(),
                        shape = RoundedCornerShape(m.corner.dp),
                        modifier = Modifier.weight(1f).heightIn(min = m.minTouch.dp)
                    ) { Text("Borrar todo", fontSize = m.buttonLabel.sp, maxLines = 1) }
                    if (hints.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onMoreHints,
                            enabled = shownHints < hints.size,
                            shape = RoundedCornerShape(m.corner.dp),
                            modifier = Modifier.weight(1f).heightIn(min = m.minTouch.dp)
                        ) {
                            Text(
                                if (shownHints < hints.size) "Pista" else "Sin más pistas",
                                fontSize = m.buttonLabel.sp, maxLines = 1
                            )
                        }
                    }
                    Button(
                        onClick = onClose,
                        shape = RoundedCornerShape(m.corner.dp),
                        modifier = Modifier.weight(1.2f).heightIn(min = m.minTouch.dp)
                    ) { Text("Volver a responder", fontSize = m.buttonLabel.sp, maxLines = 1) }
                }
            }
        }
    }
}
