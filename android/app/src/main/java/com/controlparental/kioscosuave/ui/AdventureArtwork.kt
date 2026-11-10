package com.controlparental.kioscosuave.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/** The cast is decorative: no species, color or position predicts a correct answer. */
internal enum class AdventureCompanion(val label: String) {
    SPARK_MOUSE("Spark mouse"), EMBER_FOX("Ember fox"), MOSS_TURTLE("Moss turtle"), CLOUD_OWL("Cloud owl")
}

private val outline = Color(0xFF304453)
private val cream = Color(0xFFFFF3D9)

/** Small, original adventure companions drawn at a 120 × 100 logical size. */
@Composable
internal fun CompanionDrawing(companion: AdventureCompanion, celebrating: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        withTransform({ scale(size.width / 120f, size.height / 100f, pivot = Offset.Zero) }) {
            drawOval(Color(0xFFD9E7CF), Offset(20f, 87f), Size(80f, 9f))
            when (companion) {
                AdventureCompanion.SPARK_MOUSE -> {
                    val gold = Color(0xFFFFCF56)
                    polygon(gold, 86f, 66f, 107f, 48f, 103f, 67f, 113f, 65f, 91f, 85f)
                    ellipse(gold, 33f, 50f, 54f, 40f)
                    ellipse(gold, 24f, 6f, 29f, 34f)
                    ellipse(gold, 67f, 6f, 29f, 34f)
                    ellipse(Color(0xFFF3AA87), 31f, 13f, 15f, 21f, border = false)
                    ellipse(Color(0xFFF3AA87), 74f, 13f, 15f, 21f, border = false)
                    ellipse(gold, 28f, 26f, 64f, 47f)
                    ellipse(cream, 46f, 66f, 28f, 19f, border = false)
                    ellipse(Color(0xFFF29164), 33f, 53f, 11f, 7f, border = false)
                    ellipse(Color(0xFFF29164), 76f, 53f, 11f, 7f, border = false)
                    face(46f, 74f, 48f, celebrating)
                    ellipse(gold, 32f, 82f, 23f, 10f)
                    ellipse(gold, 65f, 82f, 23f, 10f)
                }
                AdventureCompanion.EMBER_FOX -> {
                    val orange = Color(0xFFF29A61)
                    val tail = Path().apply {
                        moveTo(79f, 86f); cubicTo(115f, 89f, 116f, 53f, 104f, 38f)
                        cubicTo(104f, 67f, 78f, 56f, 76f, 75f); close()
                    }
                    drawPath(tail, orange); drawPath(tail, outline, style = Stroke(2.3f))
                    polygon(cream, 104f, 38f, 111f, 58f, 101f, 66f, 97f, 58f, border = false)
                    ellipse(orange, 31f, 48f, 52f, 42f)
                    polygon(orange, 29f, 38f, 28f, 5f, 51f, 24f)
                    polygon(orange, 65f, 24f, 88f, 5f, 86f, 40f)
                    polygon(cream, 34f, 29f, 34f, 15f, 45f, 28f, border = false)
                    polygon(cream, 71f, 28f, 82f, 15f, 81f, 31f, border = false)
                    ellipse(orange, 25f, 24f, 66f, 47f)
                    polygon(cream, 28f, 48f, 58f, 57f, 88f, 48f, 76f, 66f, 58f, 73f, 40f, 66f, border = false)
                    ellipse(cream, 44f, 70f, 26f, 13f, border = false)
                    face(43f, 73f, 45f, celebrating)
                    ellipse(outline, 52f, 56f, 10f, 6f, border = false)
                    ellipse(orange, 29f, 82f, 23f, 10f)
                    ellipse(orange, 63f, 82f, 23f, 10f)
                }
                AdventureCompanion.MOSS_TURTLE -> {
                    val mint = Color(0xFF91D5AF)
                    ellipse(mint, 22f, 77f, 24f, 14f)
                    ellipse(mint, 75f, 77f, 24f, 14f)
                    ellipse(Color(0xFF4D9A83), 23f, 40f, 74f, 46f)
                    polygon(Color(0xFF91BD67), 60f, 45f, 78f, 56f, 78f, 72f, 60f, 82f, 42f, 72f, 42f, 56f)
                    drawLine(outline, Offset(24f, 60f), Offset(42f, 56f), 2f)
                    drawLine(outline, Offset(78f, 56f), Offset(96f, 60f), 2f)
                    ellipse(mint, 32f, 17f, 56f, 44f)
                    polygon(Color(0xFF79B35C), 59f, 20f, 49f, 7f, 60f, 12f, 72f, 3f, 68f, 18f)
                    face(47f, 73f, 36f, celebrating)
                    ellipse(mint, 17f, 54f, 22f, 15f)
                    ellipse(mint, 81f, 54f, 22f, 15f)
                }
                AdventureCompanion.CLOUD_OWL -> {
                    val blue = Color(0xFF8DABDC)
                    polygon(blue, 30f, 36f, 30f, 10f, 49f, 22f, 70f, 22f, 90f, 10f, 90f, 36f)
                    ellipse(blue, 24f, 23f, 72f, 64f)
                    ellipse(Color(0xFF6D8BC1), 17f, 48f, 20f, 32f)
                    ellipse(Color(0xFF6D8BC1), 83f, 48f, 20f, 32f)
                    ellipse(cream, 35f, 57f, 50f, 27f, border = false)
                    ellipse(cream, 31f, 30f, 30f, 31f, border = false)
                    ellipse(cream, 59f, 30f, 30f, 31f, border = false)
                    eyes(46f, 74f, 44f, celebrating)
                    polygon(Color(0xFFFFC657), 54f, 53f, 66f, 53f, 60f, 62f)
                    for (x in listOf(49f, 60f, 71f)) {
                        drawLine(blue, Offset(x - 3f, 69f), Offset(x, 73f), 2f, StrokeCap.Round)
                        drawLine(blue, Offset(x, 73f), Offset(x + 3f, 69f), 2f, StrokeCap.Round)
                    }
                    ellipse(Color(0xFFFFC657), 34f, 82f, 20f, 10f)
                    ellipse(Color(0xFFFFC657), 66f, 82f, 20f, 10f)
                }
            }
            if (celebrating) {
                for ((x, y) in listOf(Offset(12f, 20f), Offset(108f, 25f), Offset(14f, 65f))) {
                    polygon(Color(0xFFFFC94E), x, y - 6f, x + 2f, y - 2f, x + 6f, y,
                        x + 2f, y + 2f, x, y + 6f, x - 2f, y + 2f, x - 6f, y, x - 2f, y - 2f, border = false)
                }
            }
        }
    }
}

private fun DrawScope.ellipse(color: Color, x: Float, y: Float, width: Float, height: Float, border: Boolean = true) {
    drawOval(color, Offset(x, y), Size(width, height))
    if (border) drawOval(outline, Offset(x, y), Size(width, height), style = Stroke(2.3f))
}

private fun DrawScope.polygon(color: Color, vararg points: Float, border: Boolean = true) {
    val path = Path().apply {
        moveTo(points[0], points[1])
        for (i in 2 until points.size step 2) lineTo(points[i], points[i + 1])
        close()
    }
    drawPath(path, color)
    if (border) drawPath(path, outline, style = Stroke(2.3f))
}

private fun DrawScope.eyes(left: Float, right: Float, y: Float, happy: Boolean) {
    for (x in listOf(left, right)) {
        if (happy) {
            val arc = Path().apply { moveTo(x - 4f, y + 2f); quadraticBezierTo(x, y - 6f, x + 4f, y + 2f) }
            drawPath(arc, outline, style = Stroke(2.5f, cap = StrokeCap.Round))
        } else {
            ellipse(outline, x - 4f, y - 5f, 8f, 11f, border = false)
            drawCircle(Color.White, 1.8f, Offset(x - 1f, y - 2f))
        }
    }
}

private fun DrawScope.face(left: Float, right: Float, y: Float, happy: Boolean) {
    eyes(left, right, y, happy)
    val center = (left + right) / 2
    val smile = Path().apply { moveTo(center - 5f, y + 13f); quadraticBezierTo(center, y + 18f, center + 5f, y + 13f) }
    drawPath(smile, outline, style = Stroke(2f, cap = StrokeCap.Round))
}

/** A sunny expedition clearing, with a winding trail instead of the reference pond. */
@Composable
internal fun AdventureBackdrop(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFFF1F9E9), Color(0xFFE5F0CE), Color(0xFFC7DEAA))))
        drawCircle(Color(0xFFFFE7A0), w * 0.11f, Offset(w * 0.92f, h * 0.06f))
        val hill = Path().apply {
            moveTo(0f, h * 0.82f); cubicTo(w * 0.3f, h * 0.64f, w * 0.7f, h * 0.91f, w, h * 0.72f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(hill, Color(0xFFA9CA85))
        val trail = Path().apply {
            moveTo(w * 0.62f, h); cubicTo(w * 0.85f, h * 0.88f, w * 0.77f, h * 0.84f, w * 0.95f, h * 0.78f)
            lineTo(w, h * 0.8f); cubicTo(w * 0.84f, h * 0.89f, w * 0.98f, h * 0.91f, w * 0.9f, h); close()
        }
        drawPath(trail, Color(0xFFEBDDAD))
    }
}
