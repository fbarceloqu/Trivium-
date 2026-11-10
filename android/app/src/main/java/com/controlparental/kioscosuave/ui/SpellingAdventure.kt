package com.controlparental.kioscosuave.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlparental.kioscosuave.SpellingLetters
import com.controlparental.kioscosuave.TtsManager

private val adventureInk = Color(0xFF283E4D)

/** A persistent scene; the parent owns grading, memory, confirmation and progression. */
@Composable
internal fun SpellingAdventure(
    roundKey: Any,
    question: String,
    word: String,
    options: List<String>,
    building: Boolean,
    selectedAnswer: String?,
    answer: String,
    result: Boolean?,
    imageRes: Int?,
    onSelection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val m = LocalMetrics.current
    val ctx = LocalContext.current
    val round = remember(roundKey) { SpellingAdventureRound(word, question, options, building) }
    // The cast changes independently of the answer, without changing letter identities.
    val companions = remember(roundKey) { AdventureCompanion.entries.shuffled() }
    val selected = remember(roundKey) { mutableStateListOf<Int>() }
    val scroll = rememberScrollState()
    LaunchedEffect(roundKey) {
        scroll.scrollTo(0)
        TtsManager.speak(ctx, round.speech, english = true)
    }
    fun choose(index: Int) {
        if (result != null) return
        if (building) {
            if (index in selected || selected.size >= word.length) return
            selected.add(index)
            TtsManager.speak(ctx, SpellingLetters.letterName(round.choices[index].single()), english = true)
        } else {
            selected.clear()
            selected.add(index)
        }
        onSelection(round.selection(selected))
    }

    Box(modifier.clip(RoundedCornerShape(m.corner.dp))) {
        AdventureBackdrop(Modifier.matchParentSize())
        // Short landscape screens split the clue and the team; either column can scroll.
        if (m.mode == LayoutMode.TWO_PANE) {
            Row(Modifier.fillMaxSize().padding(m.cardPad.dp), horizontalArrangement = Arrangement.spacedBy(m.sectionGap.dp)) {
                Column(Modifier.weight(0.34f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    AdventureClue(round, imageRes, result)
                }
                Column(Modifier.weight(0.66f).fillMaxHeight().verticalScroll(scroll), horizontalAlignment = Alignment.CenterHorizontally) {
                    AdventurePlayArea(round, companions, selected, selectedAnswer, answer, result, ::choose) {
                        if (selected.isNotEmpty()) {
                            selected.removeAt(selected.lastIndex)
                            onSelection(round.selection(selected))
                        }
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(m.cardPad.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AdventureClue(round, imageRes, result)
                Spacer(Modifier.height(m.sectionGap.dp))
                AdventurePlayArea(round, companions, selected, selectedAnswer, answer, result, ::choose) {
                    if (selected.isNotEmpty()) {
                        selected.removeAt(selected.lastIndex)
                        onSelection(round.selection(selected))
                    }
                }
            }
        }
    }
}

@Composable
private fun AdventureClue(round: SpellingAdventureRound, imageRes: Int?, result: Boolean?) {
    val m = LocalMetrics.current
    val ctx = LocalContext.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(m.itemGap.dp)) {
        Text("WORD QUEST", color = adventureInk, fontSize = m.stageTitle.sp, fontWeight = FontWeight.ExtraBold)
        Text("A little adventure. A new word.", color = adventureInk, fontSize = m.statusLine.sp, textAlign = TextAlign.Center)
        if (imageRes != null) {
            Image(painterResource(imageRes), contentDescription = "Picture clue. Listen to hear the word.",
                modifier = Modifier.size((m.minTouch * 1.65f).dp).clip(RoundedCornerShape(m.corner.dp)).background(Color.White))
        }
        Text(round.prompt, color = adventureInk, fontSize = m.instruction.sp, lineHeight = m.lineHeight(m.instruction).sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        FilledTonalButton(
            onClick = { TtsManager.speak(ctx, if (result == null) round.speech else SpellingLetters.reviewSpeech(round.word), english = true) },
            modifier = Modifier.heightIn(min = m.minTouch.dp),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFFFD76A), contentColor = adventureInk)
        ) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
            Spacer(Modifier.width(m.itemGap.dp))
            Text("Listen", fontSize = m.buttonLabel.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdventurePlayArea(
    round: SpellingAdventureRound,
    companions: List<AdventureCompanion>,
    selected: List<Int>,
    selectedAnswer: String?,
    answer: String,
    result: Boolean?,
    onChoose: (Int) -> Unit,
    onUndo: () -> Unit
) {
    val m = LocalMetrics.current
    val slots = round.slots(selected)
    if (slots.isNotEmpty()) {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(m.itemGap.dp / 2, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(m.itemGap.dp / 2)) {
            slots.forEach { letter ->
                Box(Modifier.size(m.minTouch.dp).clip(RoundedCornerShape((m.corner / 2).dp)).background(Color.White), contentAlignment = Alignment.Center) {
                    Text(letter, color = adventureInk, fontWeight = FontWeight.ExtraBold, fontSize = m.option.sp)
                }
            }
        }
        Spacer(Modifier.height(m.itemGap.dp))
    }
    if (round.building) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("${selected.size} / ${round.word.length}", color = adventureInk, fontSize = m.statusLine.sp)
            Spacer(Modifier.width(m.itemGap.dp))
            OutlinedButton(onClick = onUndo, enabled = result == null && selected.isNotEmpty(),
                modifier = Modifier.heightIn(min = m.minTouch.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = adventureInk)) { Text("Undo", fontSize = m.buttonLabel.sp) }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth.value >= m.minTouch * 7) 4 else 2
        val cellWidth = ((maxWidth.value - m.itemGap * (columns - 1)) / columns).dp
        FlowRow(horizontalArrangement = Arrangement.spacedBy(m.itemGap.dp), verticalArrangement = Arrangement.spacedBy(m.itemGap.dp)) {
            round.choices.forEachIndexed { index, choice ->
                val chosen = index in selected
                val correctChoice = result != null && !round.building && choice == answer
                CompanionChoice(choice, companions[index % companions.size], chosen, correctChoice || (result == true && chosen),
                    enabled = result == null && (!round.building || (!chosen && selected.size < round.word.length)),
                    modifier = Modifier.width(cellWidth), onClick = { onChoose(index) })
            }
        }
    }
    Spacer(Modifier.height(m.itemGap.dp))
    Text(
        when (result) {
            true -> "Great job!  ${round.word}"
            false -> "Let's learn it: ${if (round.building) round.word else answer}"
            null -> if (!selectedAnswer.isNullOrEmpty()) "Ready? Tap Continue to check." else "Choose a friend${if (round.building) " for each letter" else ""}."
        },
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        color = adventureInk, fontSize = m.feedback.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold
    )
    if (result == false) {
        Text(SpellingLetters.answer(round.word.toList()), color = adventureInk, fontSize = m.option.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun CompanionChoice(label: String, companion: AdventureCompanion, chosen: Boolean, celebrate: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val m = LocalMetrics.current
    val jump = remember { Animatable(0f) }
    LaunchedEffect(chosen, celebrate) {
        if (chosen || celebrate) {
            jump.animateTo(-m.itemGap, tween(180))
            jump.animateTo(0f, tween(240))
        } else jump.snapTo(0f)
    }
    Column(
        modifier.clip(RoundedCornerShape(m.corner.dp))
            .background(if (chosen || celebrate) Color(0xFFFFF1BB) else Color.White.copy(alpha = 0.88f))
            .border(if (chosen || celebrate) 3.dp else 1.dp, if (celebrate) Color(0xFF297B48) else if (chosen) adventureInk else Color(0xFFD4E1CD), RoundedCornerShape(m.corner.dp))
            .selectable(selected = chosen, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "${companion.label}: $label"; if (celebrate) stateDescription = "Correct" }
            .padding((m.itemGap / 2).dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().height((m.minTouch * 1.65f).dp).graphicsLayer { translationY = jump.value.dp.toPx() }, contentAlignment = Alignment.Center) {
            CompanionDrawing(companion, celebrate, Modifier.fillMaxHeight().aspectRatio(1.2f))
        }
        Surface(color = if (chosen || celebrate) Color(0xFFFFD76A) else Color(0xFFEAF1F6), shape = RoundedCornerShape((m.corner / 2).dp)) {
            Text(if (celebrate) "✓ $label" else label, color = adventureInk, fontSize = m.option.sp, fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = (m.itemGap / 2).dp))
        }
    }
}
