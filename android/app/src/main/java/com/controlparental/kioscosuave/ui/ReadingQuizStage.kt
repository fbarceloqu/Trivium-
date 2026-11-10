package com.controlparental.kioscosuave.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlparental.kioscosuave.MemoryStore
import com.controlparental.kioscosuave.ProgressSync
import com.controlparental.kioscosuave.ReadingQuizStore
import com.controlparental.kioscosuave.StageAttempt
import com.controlparental.kioscosuave.curriculum.ComprehensionSkill
import com.controlparental.kioscosuave.curriculum.LongReading
import com.controlparental.kioscosuave.curriculum.LongReadingBank
import com.controlparental.kioscosuave.curriculum.LongReadingQuiz
import com.controlparental.kioscosuave.curriculum.LongReadingValidator

/**
 * COMPRENSIÓN LECTORA DE SECUNDARIA
 * =================================
 *
 * Una lectura de 500–800 palabras y un cuestionario de opción múltiple. La
 * lectura está SIEMPRE a la vista (panel izquierdo en horizontal, pestaña en
 * vertical): consultar el texto para responder es justo lo que se quiere.
 *
 * Reglas:
 *  - Se envía el cuestionario completo y se aprueba con 80 % o más
 *    (5/6, 6/7 o 7/8), el mismo umbral que tenía el resumen escrito.
 *  - Si no se alcanza, se marcan las preguntas falladas SIN revelar la
 *    respuesta; el alumno vuelve al texto y las corrige. Cada envío cuenta
 *    como un intento en el reporte para el padre.
 *  - Si falla MÁS de la mitad, no se corrige: se cambia de lectura (decisión
 *    del padre, nov 2026). Evita aprobar por descarte un texto no entendido.
 *  - Las explicaciones aparecen al aprobar, no antes.
 *  - A la memoria de aprendizaje solo llega el PRIMER envío: es el que mide
 *    lo que se entendió al leer.
 */
private val readingAccent = Color(0xFFF59E0B)

@Composable
internal fun ReadingQuizStage(onApproved: () -> Unit) {
    // Cada lectura es una ronda con su propio estado; al cambiar de texto se
    // descarta la ronda entera y se empieza otra.
    var generation by remember { mutableIntStateOf(0) }
    key(generation) {
        ReadingQuizRound(onApproved = onApproved, onNewReading = { generation++ })
    }
}

@Composable
private fun ReadingQuizRound(onApproved: () -> Unit, onNewReading: () -> Unit) {
    val m = LocalMetrics.current
    val ctx = LocalContext.current

    // Lectura en curso si la hay y sigue siendo válida; si no, una nueva.
    val start = remember {
        val saved = ReadingQuizStore.current(ctx)
        val savedReading = saved?.let { LongReadingBank.byId(it.id) }
            ?.takeIf { it.questions.size == saved.answers.size }
        if (saved != null && savedReading != null) savedReading to saved
        else {
            // Solo se ofrecen lecturas que pasan la validación completa.
            val valid = LongReadingBank.readings.filter { LongReadingValidator.errors(it).isEmpty() }
                .ifEmpty { LongReadingBank.readings }
            val pick = LongReadingQuiz.pick(valid, ReadingQuizStore.history(ctx), ReadingQuizStore.lastScore(ctx))
            val fresh = ReadingQuizStore.Current(
                pick.id, System.nanoTime(), List(pick.questions.size) { null },
                submits = ReadingQuizStore.carriedSubmits(ctx)
            )
            ReadingQuizStore.saveCurrent(ctx, fresh)
            pick to fresh
        }
    }
    val reading = start.first
    val seed = start.second.seed
    val order = remember { LongReadingQuiz.order(reading, seed) }

    val answers = remember { mutableStateListOf<Int?>().apply { addAll(start.second.answers) } }
    val locked = remember { mutableStateListOf<Int>().apply { addAll(start.second.locked) } }
    var submits by remember { mutableIntStateOf(start.second.submits) }
    var firstAnswers by remember { mutableStateOf(start.second.firstAnswers) }
    var showingResult by remember { mutableStateOf(start.second.showingResult) }
    var index by remember { mutableIntStateOf(answers.indexOfFirst { it == null }.coerceAtLeast(0)) }
    // En vertical no caben los dos paneles: se alterna con pestañas.
    var showText by remember { mutableStateOf(answers.all { it == null }) }

    val result = if (showingResult) LongReadingQuiz.grade(reading, answers) else null

    fun persist() = ReadingQuizStore.saveCurrent(
        ctx, ReadingQuizStore.Current(reading.id, seed, answers.toList(), locked.toSet(), submits, firstAnswers, showingResult)
    )

    fun choose(q: Int, option: Int) {
        if (showingResult || q in locked) return
        answers[q] = option
        persist()
    }

    fun submit() {
        if (answers.any { it == null }) return
        val graded = LongReadingQuiz.grade(reading, answers)
        submits++
        if (firstAnswers == null) {
            val first = answers.toList()
            firstAnswers = first
            // Habilidades que requieren refuerzo: una práctica por pregunta.
            val states = LongReadingQuiz.remember(MemoryStore.load(ctx), reading, first, System.currentTimeMillis())
            MemoryStore.save(ctx, states)
            ComprehensionSkill.entries.forEach { s -> ProgressSync.reportSkill(ctx, s.id, states[s.id]) }
        }
        showingResult = true
        persist()
        ProgressSync.reportReading(ctx, graded.score, submits)
    }

    fun retry() {
        val graded = LongReadingQuiz.grade(reading, answers)
        graded.perQuestion.forEachIndexed { i, ok ->
            if (ok) { if (i !in locked) locked.add(i) } else answers[i] = null
        }
        showingResult = false
        index = answers.indexOfFirst { it == null }.coerceAtLeast(0)
        persist()
    }

    /** Reporta el primer envío de esta lectura y la cierra. */
    fun close(carrySubmits: Int) {
        val first = firstAnswers ?: answers.toList()
        val firstResult = LongReadingQuiz.grade(reading, first)
        ProgressSync.reportSession(ctx, "reading", reading.questions.mapIndexed { i, q ->
            StageAttempt(
                question = "${reading.title} · ${q.skill.label}: ${q.question}",
                selected = first.getOrNull(i)?.let { q.options.getOrNull(it) } ?: "(sin respuesta)",
                answer = q.answer,
                correct = firstResult.perQuestion[i],
                explanation = q.explanation
            )
        })
        ReadingQuizStore.finish(ctx, reading.id, firstResult.score, carrySubmits)
    }

    fun finish() { close(carrySubmits = 0); onApproved() }

    // El puntaje bajo queda como "último resultado", así que la siguiente
    // lectura sale de la mitad más corta del banco.
    fun changeReading() { close(carrySubmits = submits); onNewReading() }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(m.sectionGap.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Comprensión lectora · ${reading.topic.label}",
                fontSize = m.stageTitle.sp,
                lineHeight = m.lineHeight(m.stageTitle).sp,
                fontWeight = FontWeight.Bold,
                color = readingAccent,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Text(
                when {
                    result?.passed == true -> "${result.correct} de ${result.total} correctas"
                    else -> "${answers.count { it != null }} de ${answers.size} respondidas · meta ${LongReadingQuiz.PASS_SCORE} %"
                },
                fontSize = m.statusLine.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
        Spacer(Modifier.size(m.itemGap.dp))

        val questions: @Composable (Modifier) -> Unit = { mod ->
            Column(mod) {
                when {
                    result?.passed == true ->
                        ReviewPane(reading, answers, firstAnswers, Modifier.weight(1f))
                    result != null ->
                        RetryPane(result, Modifier.weight(1f))
                    else ->
                        QuestionPane(reading, order, answers, locked, index, { index = it }, ::choose, Modifier.weight(1f))
                }
                Spacer(Modifier.size(m.itemGap.dp))
                Button(
                    onClick = {
                        when {
                            result?.passed == true -> finish()
                            result?.mustChangeReading == true -> changeReading()
                            result != null -> retry()
                            else -> submit()
                        }
                    },
                    enabled = result != null || answers.none { it == null },
                    shape = RoundedCornerShape(m.corner.dp),
                    contentPadding = PaddingValues(vertical = m.buttonVPad.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = m.minTouch.dp)
                ) {
                    Text(
                        when {
                            result?.passed == true -> "Desbloquear tablet"
                            result?.mustChangeReading == true -> "Leer otra lectura"
                            result != null -> "Volver al texto y corregir"
                            answers.any { it == null } -> "Responde todas para enviar"
                            else -> "Enviar respuestas"
                        },
                        fontSize = m.buttonLabel.sp, maxLines = 1
                    )
                }
            }
        }

        if (m.mode == LayoutMode.TWO_PANE) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(m.sectionGap.dp)
            ) {
                TextPane(reading, Modifier.weight(0.56f).fillMaxHeight())
                questions(Modifier.weight(0.44f).fillMaxHeight())
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(m.itemGap.dp)) {
                Tab("Lectura", showText, Modifier.weight(1f)) { showText = true }
                Tab("Preguntas", !showText, Modifier.weight(1f)) { showText = false }
            }
            Spacer(Modifier.size(m.itemGap.dp))
            if (showText) TextPane(reading, Modifier.fillMaxWidth().weight(1f))
            else questions(Modifier.fillMaxWidth().weight(1f))
        }
    }
}

@Composable
private fun Tab(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val m = LocalMetrics.current
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier
            .heightIn(min = m.minTouch.dp)
            .clip(RoundedCornerShape(m.corner.dp))
            .background(if (active) scheme.primary else scheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = m.buttonLabel.sp, fontWeight = FontWeight.Bold,
            color = if (active) scheme.onPrimary else scheme.onSurface)
    }
}

// =====================================================================
//  LECTURA
// =====================================================================

@Composable
private fun TextPane(reading: LongReading, modifier: Modifier) {
    val m = LocalMetrics.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(m.corner.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = (m.cardPad * 1.4f).dp, vertical = m.cardPad.dp),
            // Párrafos bien separados: en 550 palabras el aire entre bloques
            // es lo que permite volver a encontrar un dato.
            verticalArrangement = Arrangement.spacedBy((m.feedback * 0.9f).dp)
        ) {
            Text(
                reading.title,
                fontSize = m.question.sp,
                lineHeight = m.lineHeight(m.question).sp,
                fontWeight = FontWeight.Bold
            )
            reading.paragraphs.forEach { p ->
                Text(
                    p,
                    fontSize = m.feedback.sp,
                    // Interlineado más amplio que el resto de la app: es texto
                    // corrido para leer con calma, no una consigna.
                    lineHeight = (m.feedback * 1.55f).sp
                )
            }
        }
    }
}

// =====================================================================
//  PREGUNTAS
// =====================================================================

@Composable
private fun QuestionPane(
    reading: LongReading,
    order: List<List<Int>>,
    answers: List<Int?>,
    locked: List<Int>,
    index: Int,
    onIndex: (Int) -> Unit,
    onChoose: (Int, Int) -> Unit,
    modifier: Modifier
) {
    val m = LocalMetrics.current
    val scheme = MaterialTheme.colorScheme
    val q = reading.questions[index]
    val isLocked = index in locked

    Column(modifier) {
        // Navegación libre: se puede saltar, volver y cambiar de respuesta
        // antes de enviar. El número se rellena al contestar.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy((m.itemGap / 2).dp)) {
            reading.questions.indices.forEach { i ->
                val answered = answers[i] != null
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = (m.minTouch * 0.8f).dp)
                        .clip(RoundedCornerShape((m.corner / 2).dp))
                        .background(
                            when {
                                i in locked -> scheme.secondary.copy(alpha = 0.45f)
                                answered -> scheme.primary.copy(alpha = 0.45f)
                                else -> scheme.surface
                            }
                        )
                        .border(
                            if (i == index) 2.dp else 0.dp,
                            if (i == index) scheme.onSurface else Color.Transparent,
                            RoundedCornerShape((m.corner / 2).dp)
                        )
                        .clickable { onIndex(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("${i + 1}", fontSize = m.statusLine.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.size(m.itemGap.dp))

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(m.itemGap.dp)
        ) {
            Text(
                "Pregunta ${index + 1} de ${reading.questions.size} · ${q.skill.label}" +
                    if (isLocked) " · ya correcta" else "",
                fontSize = m.statusLine.sp,
                color = scheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                q.question,
                fontSize = m.instruction.sp,
                lineHeight = m.lineHeight(m.instruction).sp,
                fontWeight = FontWeight.Bold
            )
            order[index].forEach { original ->
                val chosen = answers[index] == original
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = m.minTouch.dp)
                        .clip(RoundedCornerShape(m.corner.dp))
                        .background(
                            when {
                                chosen && isLocked -> scheme.secondary.copy(alpha = 0.35f)
                                chosen -> scheme.primary.copy(alpha = 0.4f)
                                else -> scheme.surface
                            }
                        )
                        .border(
                            2.dp,
                            when {
                                chosen && isLocked -> scheme.secondary
                                chosen -> scheme.primary
                                else -> scheme.onSurface.copy(alpha = 0.15f)
                            },
                            RoundedCornerShape(m.corner.dp)
                        )
                        .clickable(enabled = !isLocked) { onChoose(index, original) }
                        .padding(horizontal = m.cardPad.dp, vertical = m.optionVPad.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        q.options[original],
                        fontSize = m.feedback.sp,
                        lineHeight = m.lineHeight(m.feedback).sp,
                        color = scheme.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.size(m.itemGap.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(m.itemGap.dp)) {
            OutlinedButton(
                onClick = { onIndex(index - 1) },
                enabled = index > 0,
                shape = RoundedCornerShape(m.corner.dp),
                modifier = Modifier.weight(1f).heightIn(min = m.minTouch.dp)
            ) { Text("Anterior", fontSize = m.buttonLabel.sp, maxLines = 1) }
            OutlinedButton(
                onClick = { onIndex(index + 1) },
                enabled = index < reading.questions.lastIndex,
                shape = RoundedCornerShape(m.corner.dp),
                modifier = Modifier.weight(1f).heightIn(min = m.minTouch.dp)
            ) { Text("Siguiente", fontSize = m.buttonLabel.sp, maxLines = 1) }
        }
    }
}

/** No se aprobó: cuántas faltan y cuáles revisar, sin revelar las respuestas. */
@Composable
private fun RetryPane(result: LongReadingQuiz.Result, modifier: Modifier) {
    val m = LocalMetrics.current
    val scheme = MaterialTheme.colorScheme
    val needed = (1..result.total).first { it * 100 / result.total >= LongReadingQuiz.PASS_SCORE }
    Column(
        modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(m.itemGap.dp)
    ) {
        Text(
            "${result.correct} de ${result.total} correctas",
            fontSize = m.question.sp, fontWeight = FontWeight.Bold
        )
        if (result.mustChangeReading) {
            // Aquí no se marcan las falladas: este texto ya no se corrige.
            Text(
                "Para aprobar necesitas $needed. Fallaste más de la mitad, así que vas a leer otro texto. " +
                    "Léelo completo y con calma antes de responder; puedes consultarlo cuantas veces quieras.",
                fontSize = m.feedback.sp, lineHeight = m.lineHeight(m.feedback).sp
            )
            return@Column
        }
        Text(
            "Para aprobar necesitas $needed. Las preguntas marcadas tienen una respuesta incorrecta: " +
                "vuelve al texto, busca el fragmento que las responde y corrígelas. " +
                "Las que ya acertaste se conservan.",
            fontSize = m.feedback.sp, lineHeight = m.lineHeight(m.feedback).sp
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy((m.itemGap / 2).dp)) {
            result.perQuestion.forEachIndexed { i, ok ->
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = (m.minTouch * 0.8f).dp)
                        .background(
                            if (ok) scheme.secondary.copy(alpha = 0.45f) else scheme.error.copy(alpha = 0.55f),
                            RoundedCornerShape((m.corner / 2).dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${i + 1}", fontSize = m.statusLine.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Aprobado: explicación de cada pregunta, con el fragmento que la respalda. */
@Composable
private fun ReviewPane(
    reading: LongReading,
    answers: List<Int?>,
    firstAnswers: List<Int?>?,
    modifier: Modifier
) {
    val m = LocalMetrics.current
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(m.itemGap.dp)
    ) {
        reading.questions.forEachIndexed { i, q ->
            val final = answers.getOrNull(i)?.let { q.options.getOrNull(it) }
            val first = (firstAnswers ?: answers).getOrNull(i)?.let { q.options.getOrNull(it) }
            val okNow = final == q.answer
            val okFirst = first == q.answer
            Card(
                shape = RoundedCornerShape(m.corner.dp),
                colors = CardDefaults.cardColors(
                    containerColor = (if (okNow) scheme.secondary else scheme.error).copy(alpha = 0.13f)
                )
            ) {
                Column(Modifier.fillMaxWidth().padding(m.cardPad.dp), verticalArrangement = Arrangement.spacedBy((m.itemGap / 2).dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size((m.statusLine * 1.6f).dp)
                                .background(if (okNow) scheme.secondary else scheme.error, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (okNow) "✓" else "✗", fontSize = m.statusLine.sp, color = Color.White, textAlign = TextAlign.Center)
                        }
                        Spacer(Modifier.size(m.itemGap.dp))
                        Text(
                            "${i + 1}. ${q.skill.label}" + when {
                                okFirst -> ""
                                okNow -> " · corregida"
                                else -> ""
                            },
                            fontSize = m.statusLine.sp,
                            color = scheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Text(q.question, fontSize = m.feedback.sp, lineHeight = m.lineHeight(m.feedback).sp, fontWeight = FontWeight.Bold)
                    if (!okNow && final != null) {
                        Text("Tu respuesta: $final", fontSize = m.feedback.sp, lineHeight = m.lineHeight(m.feedback).sp)
                    }
                    Text("Respuesta correcta: ${q.answer}", fontSize = m.feedback.sp, lineHeight = m.lineHeight(m.feedback).sp)
                    Text(q.explanation, fontSize = m.feedback.sp, lineHeight = m.lineHeight(m.feedback).sp)
                    Text(
                        "En el texto: «${q.evidence}»",
                        fontSize = (m.feedback * 0.92f).sp,
                        lineHeight = m.lineHeight(m.feedback).sp,
                        fontStyle = FontStyle.Italic,
                        color = scheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}
