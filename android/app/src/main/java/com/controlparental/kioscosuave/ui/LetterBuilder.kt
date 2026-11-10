package com.controlparental.kioscosuave.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlparental.kioscosuave.SpellingLetters
import com.controlparental.kioscosuave.TtsManager

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LetterBuilder(word: String, reviewed: Boolean, onSelection: (String) -> Unit) {
    val m = LocalMetrics.current
    val ctx = LocalContext.current
    val bank = remember(word) { SpellingLetters.bank(word) }
    val selected = remember(word) { mutableStateListOf<Int>() }
    fun update() = onSelection(SpellingLetters.answer(selected.map { bank[it] }))
    fun letterColor(letter: Char): Color =
        if (word.first().isUpperCase() && letter == word.first()) Color(0xFFFF5252)
        else Color(0xFFFFFFFF)

    Column(verticalArrangement = Arrangement.spacedBy(m.itemGap.dp)) {
        Text("Tap the letters in order.", fontSize = m.instruction.sp)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(m.itemGap.dp),
            verticalArrangement = Arrangement.spacedBy(m.itemGap.dp)
        ) {
            repeat(word.length) { position ->
                val letter = selected.getOrNull(position)?.let { bank[it] }
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Box(Modifier.size(m.minTouch.dp), contentAlignment = Alignment.Center) {
                        Text(letter?.toString() ?: "_", fontSize = m.option.sp,
                            color = letter?.let(::letterColor) ?: MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(m.itemGap.dp),
            verticalArrangement = Arrangement.spacedBy(m.itemGap.dp)
        ) {
            bank.forEachIndexed { index, letter ->
                Button(
                    enabled = !reviewed && index !in selected && selected.size < word.length,
                    onClick = {
                        selected.add(index)
                        TtsManager.speak(ctx, SpellingLetters.letterName(letter), english = true)
                        update()
                    },
                    modifier = Modifier.sizeIn(minWidth = m.minTouch.dp, minHeight = m.minTouch.dp)
                ) {
                    Text(letter.toString(), fontSize = m.option.sp, color = letterColor(letter))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(m.itemGap.dp)) {
            OutlinedButton(enabled = !reviewed && selected.isNotEmpty(), onClick = {
                selected.removeAt(selected.lastIndex)
                update()
            }) { Text("Undo") }
            OutlinedButton(enabled = !reviewed && selected.isNotEmpty(), onClick = {
                selected.clear()
                update()
            }) { Text("Clear") }
        }
    }
}
