package com.controlparental.kioscosuave.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlparental.kioscosuave.LiteracyStore
import com.controlparental.kioscosuave.MemoryStore
import com.controlparental.kioscosuave.ProgressSync
import com.controlparental.kioscosuave.StageAttempt
import com.controlparental.kioscosuave.StageProgressStore
import com.controlparental.kioscosuave.TtsManager
import com.controlparental.kioscosuave.curriculum.LiteracyActivity
import com.controlparental.kioscosuave.curriculum.LiteracyKind
import com.controlparental.kioscosuave.curriculum.LiteracySession

/**
 * ALFABETIZACIÓN (1º de primaria)
 * ===============================
 *
 * Tercera etapa del reto para quien está aprendiendo a leer: formar palabras
 * con sílabas, distinguir palabras parecidas, leer oraciones y comprender
 * cuentos breves. QUÉ toca lo decide [LiteracySession] con la memoria de
 * aprendizaje; esta pantalla solo presenta y recoge la respuesta.
 *
 * Reglas que NO cambian respecto a las otras etapas:
 *  - Se aprueba con 80% sobre las últimas [WINDOW] respuestas y [WINDOW]
 *    aciertos acumulados (la misma regla de MultipleChoiceStage).
 *  - Una sola calificación por actividad.
 *
 * Lo que sí es propio de esta etapa:
 *  - Tocar, deshacer o cambiar de opción NO califica: solo cuenta "Revisar".
 *    Un niño que explora las sílabas no está fallando.
 *  - La actividad en pantalla se guarda, así que cerrar la app no la cambia ni
 *    la cuenta como error.
 */
private const val WINDOW = 10
private val literacyAccent = Color(0xFFF59E0B)

@Composable
internal fun LiteracyStage(nextLabel: String, onDone: () -> Unit) {
    val m = LocalMetrics.current
    val ctx = LocalContext.current
    val progressKey = LiteracyStore.PROGRESS_KEY

    val session = remember { LiteracySession(MemoryStore.load(ctx), LiteracyStore.recent(ctx)) }
    val history = remember { mutableStateListOf<Boolean>().apply { addAll(StageProgressStore.load(ctx, progressKey)) } }
    val attemptLog = remember { mutableStateListOf<StageAttempt>().apply { addAll(StageProgressStore.loadAttempts(ctx, progressKey)) } }

    fun fresh(): LiteracyActivity = session.next().also {
        LiteracyStore.saveCurrent(ctx, it.id, it.choices)
        LiteracyStore.saveRecent(ctx, session.recentIds())
    }

    var activity by remember {
        mutableStateOf(LiteracyStore.current(ctx)?.let { session.restore(it.id, it.choices) } ?: fresh())
    }
    // Identidad de la ronda: dos actividades iguales seguidas siguen siendo
    // rondas distintas, con su selección y su resultado propios.
    var round by remember { mutableIntStateOf(0) }
    val selection = remember(round) { mutableStateListOf<Int>() }
    var result by remember(round) { mutableStateOf<Boolean?>(null) }

    val recent = history.takeLast(WINDOW)
    val windowHits = recent.count { it }
    val totalHits = history.count { it }
    val passed = totalHits >= WINDOW && recent.size >= WINDOW && windowHits >= (WINDOW * 8 + 9) / 10

    // Al empezar cada actividad se lee la consigna. La palabra a formar se
    // dice en voz alta (el niño la construye de oído y con el dibujo); en
    // cambio la palabra del dibujo NO se dice cuando hay que reconocerla
    // escrita, porque eso sería responder por él.
    LaunchedEffect(round) {
        if (result == null && !passed) TtsManager.speak(ctx, introSpeech(activity))
    }

    fun toggle(index: Int) {
        if (result != null) return
        if (activity.buildsWord) {
            if (index in selection || selection.size >= activity.syllables.size) return
            selection.add(index)
            // En minúsculas para que la voz diga la sílaba y no la deletree.
            TtsManager.speak(ctx, activity.choices[index].lowercase())
        } else {
            selection.clear()
            selection.add(index)
        }
    }

    fun grade() {
        if (result != null || !activity.canSubmit(selection)) return
        val ok = activity.correct(selection)
        val response = activity.response(selection)
        result = ok
        history.add(ok)
        attemptLog += StageAttempt(
            question = "${activity.kind.label}: " +
                (if (activity.passage.isNotBlank()) "«${activity.passage}» " else "") + activity.question,
            selected = response,
            answer = activity.answer,
            correct = ok,
            explanation = activity.explanation
        )
        StageProgressStore.save(ctx, progressKey, history)
        StageProgressStore.saveAttempts(ctx, progressKey, attemptLog)
        // Ya está calificada: si la app se cierra ahora no debe volver a salir.
        LiteracyStore.saveCurrent(ctx, null)
        val state = session.record(activity, ok, if (ok) null else response)
        MemoryStore.save(ctx, session.snapshot())
        ProgressSync.reportSkill(ctx, activity.skillId, state)
        TtsManager.speak(ctx, reviewSpeech(activity, ok))
    }

    fun finish() {
        // Se reporta como "reading": es la etapa de lectura del día y el panel
        // de padres ya sabe mostrarla, sin cambios en Firestore.
        ProgressSync.reportStage(ctx, "reading", history.count { it }, history.size)
        ProgressSync.reportSession(ctx, "reading", attemptLog)
        StageProgressStore.clear(ctx, progressKey)
        LiteracyStore.saveCurrent(ctx, null)
        onDone()
    }

    val action: @Composable () -> Unit = {
        Spacer(Modifier.size(m.itemGap.dp))
        Button(
            onClick = {
                when {
                    passed -> finish()
                    result == null -> grade()
                    else -> { activity = fresh(); round++ }
                }
            },
            enabled = passed || result != null || activity.canSubmit(selection),
            shape = RoundedCornerShape(m.corner.dp),
            contentPadding = PaddingValues(vertical = m.buttonVPad.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = m.minTouch.dp)
        ) {
            Text(
                when {
                    passed -> nextLabel
                    result == null -> "Revisar"
                    else -> "Siguiente"
                },
                fontSize = m.buttonLabel.sp, maxLines = 1
            )
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(m.sectionGap.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                activity.kind.label,
                fontSize = m.stageTitle.sp,
                lineHeight = m.lineHeight(m.stageTitle).sp,
                fontWeight = FontWeight.Bold,
                color = literacyAccent,
                maxLines = 1
            )
            Box(Modifier.weight(1f)) {
                ProgressIndicator(windowHits, WINDOW, totalHits, WINDOW, literacyAccent)
            }
        }
        Spacer(Modifier.size(m.sectionGap.dp))

        key(round) {
            val listen = result != null || session.listeningSupport(activity.skillId)
            if (m.mode == LayoutMode.TWO_PANE) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(m.sectionGap.dp)
                ) {
                    CluePane(activity, listen, Modifier.weight(0.42f).fillMaxHeight())
                    Column(Modifier.weight(0.58f).fillMaxHeight()) {
                        Column(
                            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        ) {
                            AnswerPane(activity, selection, result, ::toggle) {
                                if (result == null && selection.isNotEmpty()) selection.removeAt(selection.lastIndex)
                            }
                        }
                        action()
                    }
                }
            } else {
                CluePane(activity, listen, Modifier.fillMaxWidth().weight(0.8f))
                Spacer(Modifier.size(m.sectionGap.dp))
                Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                    AnswerPane(activity, selection, result, ::toggle) {
                        if (result == null && selection.isNotEmpty()) selection.removeAt(selection.lastIndex)
                    }
                }
                action()
            }
        }
    }
}

private fun introSpeech(a: LiteracyActivity): String = when (a.kind) {
    LiteracyKind.SYLLABLES -> "Forma la palabra ${a.answer.lowercase()}."
    LiteracyKind.WORD_IMAGE -> "Mira el dibujo y busca su palabra."
    LiteracyKind.SENTENCE -> "Lee la oración. ${a.question}"
    LiteracyKind.STORY -> "Lee el cuento. ${a.question}"
}

private fun reviewSpeech(a: LiteracyActivity, ok: Boolean): String = when (a.kind) {
    LiteracyKind.SYLLABLES ->
        "${if (ok) "Muy bien." else "Casi."} ${a.syllables.joinToString(", ") { it.lowercase() }}. ${a.answer.lowercase()}."
    LiteracyKind.WORD_IMAGE -> "${if (ok) "Muy bien." else "Casi."} ${a.answer.lowercase()}."
    else -> if (ok) "Muy bien." else "Casi. ${a.evidence}"
}

// =====================================================================
//  PISTA: dibujo y, si lo hay, el texto que se lee
// =====================================================================

@Composable
private fun CluePane(a: LiteracyActivity, listen: Boolean, modifier: Modifier) {
    val m = LocalMetrics.current
    val ctx = LocalContext.current
    val image = imageResForWord(a.imageKey)
    val hasText = a.passage.isNotBlank()

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(m.corner.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().padding(m.cardPad.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Con texto, el dibujo acompaña; sin texto, el dibujo ES la pista.
                Picture(
                    image, a.imageFallback,
                    if (hasText) Modifier.fillMaxWidth().weight(0.4f) else Modifier.fillMaxWidth().weight(1f)
                )
                if (hasText) {
                    Spacer(Modifier.size(m.itemGap.dp))
                    Column(
                        Modifier.fillMaxWidth().weight(0.6f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy((m.itemGap / 2).dp)
                    ) {
                        // Una oración por renglón: seguir el cuento con el
                        // dedo es más fácil que en un párrafo corrido.
                        a.passage.split(Regex("(?<=[.!?])\\s+")).forEach { sentence ->
                            Text(
                                sentence,
                                fontSize = m.question.sp,
                                lineHeight = m.lineHeight(m.question).sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            // Formar palabras: siempre se puede oír la palabra. Lectura: el
            // texto solo se escucha mientras la habilidad es nueva o después de
            // responder, para que el audio acompañe sin sustituir a la lectura.
            val speech = when {
                a.buildsWord -> a.answer.lowercase()
                hasText && listen -> a.passage
                else -> null
            }
            speech?.let { s ->
                IconButton(
                    onClick = { TtsManager.speak(ctx, s) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding((m.cardPad / 2).dp)
                        .size(m.minTouch.dp)
                        .background(literacyAccent.copy(alpha = 0.22f), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (a.buildsWord) "Escuchar la palabra" else "Escuchar el texto",
                        tint = literacyAccent,
                        modifier = Modifier.size((m.actionIcon * 0.8f).dp)
                    )
                }
            }
        }
    }
}

/** Ilustración educativa: imagen real si existe; si no, el dibujo de respaldo. */
@Composable
private fun Picture(image: Int?, fallback: String, modifier: Modifier) {
    val m = LocalMetrics.current
    val density = LocalDensity.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth.value, maxHeight.value).coerceAtMost(m.heroImageMax * 1.6f)
        if (image != null) {
            Image(
                painterResource(image), contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.size(side.dp).clip(RoundedCornerShape(m.corner.dp))
            )
        } else {
            val sp = with(density) { (side * 0.72f).dp.toSp() }
            Text(fallback, fontSize = sp, lineHeight = sp, textAlign = TextAlign.Center)
        }
    }
}

// =====================================================================
//  RESPUESTA
// =====================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnswerPane(
    a: LiteracyActivity,
    selection: List<Int>,
    result: Boolean?,
    onTap: (Int) -> Unit,
    onUndo: () -> Unit
) {
    val m = LocalMetrics.current
    val ctx = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(m.itemGap.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                a.question,
                fontSize = m.instruction.sp,
                lineHeight = m.lineHeight(m.instruction).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { TtsManager.speak(ctx, a.question) }, modifier = Modifier.size(m.minTouch.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Escuchar la pregunta",
                    tint = literacyAccent, modifier = Modifier.size((m.actionIcon * 0.7f).dp)
                )
            }
        }

        if (a.buildsWord) {
            // Casillas: una por sílaba. Se llenan en el orden en que se toca.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(m.itemGap.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                a.syllables.indices.forEach { slot ->
                    val text = selection.getOrNull(slot)?.let { a.choices[it] }
                    val border = when (result) {
                        true -> scheme.secondary
                        false -> scheme.error
                        null -> if (text != null) scheme.primary else scheme.onSurface.copy(alpha = 0.35f)
                    }
                    Box(
                        Modifier
                            .widthIn(min = (m.minTouch * 1.5f).dp)
                            .heightIn(min = (m.minTouch * 1.25f).dp)
                            .border(3.dp, border, RoundedCornerShape(m.corner.dp))
                            .padding(horizontal = m.itemGap.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text ?: " ", fontSize = (m.question * 1.25f).sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                OutlinedButton(
                    onClick = onUndo,
                    enabled = result == null && selection.isNotEmpty(),
                    shape = RoundedCornerShape(m.corner.dp),
                    modifier = Modifier.heightIn(min = m.minTouch.dp)
                ) { Text("Borrar", fontSize = m.buttonLabel.sp, maxLines = 1) }
            }
            // Fichas: grandes, de colores, y cada una dice su sílaba al tocarla.
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(m.itemGap.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(m.itemGap.dp)
            ) {
                a.choices.forEachIndexed { index, syllable ->
                    val used = index in selection
                    val tint = spellingColors[index % spellingColors.size]
                    val canTap = result == null && !used && selection.size < a.syllables.size
                    Box(
                        Modifier
                            .widthIn(min = (m.minTouch * 1.7f).dp)
                            .heightIn(min = (m.minTouch * 1.4f).dp)
                            .clip(RoundedCornerShape(m.corner.dp))
                            .background(tint.copy(alpha = if (used) 0.08f else 0.28f))
                            .border(2.dp, tint.copy(alpha = if (used) 0.2f else 0.8f), RoundedCornerShape(m.corner.dp))
                            .clickable(enabled = canTap) { onTap(index) }
                            .padding(horizontal = m.cardPad.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            syllable,
                            fontSize = (m.question * 1.25f).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = scheme.onSurface.copy(alpha = if (used) 0.25f else 1f)
                        )
                    }
                }
            }
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                // Palabras sueltas en dos columnas; frases en una, para no partirlas.
                val columns = if (maxWidth.value < 380f || a.choices.maxOf { it.length } > 12) 1 else 2
                Column(verticalArrangement = Arrangement.spacedBy(m.itemGap.dp)) {
                    a.choices.indices.chunked(columns).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(m.itemGap.dp)) {
                            row.forEach { index ->
                                val isAnswer = LiteracyActivity.normalize(a.choices[index]) == LiteracyActivity.normalize(a.answer)
                                val chosen = index in selection
                                ChoiceButton(
                                    text = a.choices[index],
                                    big = a.kind == LiteracyKind.WORD_IMAGE,
                                    tint = spellingColors[index % spellingColors.size],
                                    state = when {
                                        result == null -> if (chosen) ChoiceState.SELECTED else ChoiceState.IDLE
                                        chosen && result == true -> ChoiceState.RIGHT
                                        chosen -> ChoiceState.WRONG
                                        isAnswer -> ChoiceState.REVEALED
                                        else -> ChoiceState.IDLE
                                    },
                                    enabled = result == null,
                                    onClick = { onTap(index) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        result?.let { ok ->
            val header = if (ok) "🎉 ¡Muy bien!" else "Casi. La respuesta es: ${a.answer}"
            FeedbackBox(ok, "$header\n${a.explanation}")
        }
    }
}

private enum class ChoiceState { IDLE, SELECTED, RIGHT, WRONG, REVEALED }

@Composable
private fun ChoiceButton(
    text: String,
    big: Boolean,
    tint: Color,
    state: ChoiceState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val m = LocalMetrics.current
    val scheme = MaterialTheme.colorScheme
    val container = when (state) {
        ChoiceState.IDLE -> tint.copy(alpha = 0.22f)
        ChoiceState.SELECTED -> scheme.primary.copy(alpha = 0.45f)
        ChoiceState.RIGHT -> scheme.secondary
        ChoiceState.WRONG -> scheme.error
        ChoiceState.REVEALED -> scheme.secondary.copy(alpha = 0.35f)
    }
    val edge = when (state) {
        ChoiceState.IDLE -> tint.copy(alpha = 0.65f)
        ChoiceState.SELECTED -> scheme.primary
        ChoiceState.RIGHT, ChoiceState.REVEALED -> scheme.secondary
        ChoiceState.WRONG -> scheme.error
    }
    Box(
        modifier
            .heightIn(min = (if (big) m.minTouch * 1.4f else m.answerMinHeight).dp)
            .clip(RoundedCornerShape(m.corner.dp))
            .background(container)
            .border(BorderStroke(if (state == ChoiceState.IDLE) 2.dp else 3.dp, edge), RoundedCornerShape(m.corner.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = m.optionVPad.dp, horizontal = m.cardPad.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = (if (big) m.question * 1.15f else m.option).sp,
            lineHeight = m.lineHeight(if (big) m.question * 1.15f else m.option).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = scheme.onSurface
        )
    }
}
