package com.controlparental.kioscosuave.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlparental.kioscosuave.TtsManager

internal val spellingColors = listOf(
    Color(0xFFBCA7FF), Color(0xFF67DED0), Color(0xFFFFCB78), Color(0xFFFFA7C8)
)

/** A visual word playground; grading and progress stay in MultipleChoiceStage. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpellingCard(
    question: String,
    word: String,
    imageRes: Int?,
    result: Boolean?,
    modifier: Modifier = Modifier
) {
    val m = LocalMetrics.current
    val ctx = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val glow by animateColorAsState(
        targetValue = if (result == true) spellingColors[1] else spellingColors[0],
        label = "spelling celebration"
    )
    val blank = question.startsWith("_")
    // Only show the full word after answering. Letter-search questions should
    // not accidentally disclose their answer through decorative letter tiles.
    val tiles = when {
        result != null -> word
        blank -> question
        else -> null
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(m.corner.dp),
        border = BorderStroke(1.dp, glow.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = scheme.surface)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Brush.linearGradient(
            listOf(glow.copy(alpha = 0.16f),
                Color.Transparent, spellingColors[1].copy(alpha = 0.12f))
        ))) {
            val pictureSize = minOf(m.heroImageMax, maxWidth.value * 0.65f,
                maxHeight.value * 0.43f).coerceAtLeast(m.minTouch)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(m.cardPad.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(m.itemGap.dp, Alignment.CenterVertically)
            ) {
                Text(
                    when (result) {
                        true -> "★  ¡Palabra conquistada!  ★"
                        false -> "¡Vamos a descubrirla!"
                        null -> "✦  SPELLING LAB  ✦"
                    },
                    color = if (result == true) spellingColors[1] else spellingColors[2],
                    fontWeight = FontWeight.Bold,
                    fontSize = m.statusLine.sp,
                    textAlign = TextAlign.Center
                )
                if (imageRes != null) {
                    Box(
                        Modifier.size(pictureSize.dp)
                            .clip(RoundedCornerShape(m.corner.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(painterResource(imageRes), contentDescription = word,
                            modifier = Modifier.fillMaxSize().padding(m.itemGap.dp))
                    }
                } else {
                    Icon(Icons.Filled.VolumeUp, contentDescription = null,
                        tint = spellingColors[1], modifier = Modifier.size((pictureSize * 0.55f).dp))
                }
                Text(
                    if (blank) "¿Qué letra falta?" else question,
                    fontSize = m.instruction.sp,
                    lineHeight = m.lineHeight(m.instruction).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (tiles != null) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                        horizontalArrangement = Arrangement.spacedBy(m.itemGap.dp / 2, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(m.itemGap.dp / 2)
                    ) {
                        tiles.forEachIndexed { index, letter ->
                            val color = spellingColors[index % spellingColors.size]
                            Surface(
                                shape = RoundedCornerShape((m.corner / 2).dp),
                                color = if (letter == '_') color.copy(alpha = 0.12f) else color,
                                border = BorderStroke(2.dp, color)
                            ) {
                                Box(Modifier.size(m.minTouch.dp), contentAlignment = Alignment.Center) {
                                    Text(if (letter == '_') "?" else letter.toString(),
                                        color = if (letter == '_') color else Color(0xFF192139),
                                        fontSize = m.option.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
                FilledTonalButton(
                    onClick = { TtsManager.speak(ctx, word, english = true) },
                    modifier = Modifier.heightIn(min = m.minTouch.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = spellingColors[0].copy(alpha = 0.2f),
                        contentColor = scheme.onSurface)
                ) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = null)
                    Spacer(Modifier.size(m.itemGap.dp))
                    Text("Escuchar palabra", fontSize = m.buttonLabel.sp)
                }
            }
        }
    }
}
