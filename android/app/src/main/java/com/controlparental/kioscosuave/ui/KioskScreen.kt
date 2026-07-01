package com.controlparental.kioscosuave.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.controlparental.kioscosuave.ChallengeEngine
import com.controlparental.kioscosuave.ChildProfile
import com.controlparental.kioscosuave.EnglishExercise
import com.controlparental.kioscosuave.MathQuestion
import com.controlparental.kioscosuave.ReadingPassage
import com.controlparental.kioscosuave.SummaryResult

private enum class Stage { MATH, ENGLISH, READING }

@Composable
fun KioskScreen(
    profile: ChildProfile,
    onAllComplete: () -> Unit,
    onParentAccess: () -> Unit
) {
    val config = remember(profile) { profile.config }
    var stage by remember { mutableStateOf(Stage.MATH) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Hola, ${profile.name} 👋", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Completa tus tareas para desbloquear la tablet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            IconButton(onClick = onParentAccess) {
                Icon(Icons.Filled.Lock, contentDescription = "Acceso de padres")
            }
        }

        Spacer(Modifier.height(8.dp))
        StepIndicator(stage)
        Spacer(Modifier.height(16.dp))

        when (stage) {
            Stage.MATH -> MathStage(
                difficulty = config.difficulty,
                total = config.mathTotal,
                requiredCorrect = config.mathRequiredCorrect
            ) { stage = Stage.ENGLISH }
            Stage.ENGLISH -> EnglishStage(config.englishTarget) { stage = Stage.READING }
            Stage.READING -> ReadingStage(onApproved = onAllComplete)
        }
    }
}

@Composable
private fun StepIndicator(stage: Stage) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("Mate", stage == Stage.MATH, Modifier.weight(1f))
        Chip("Inglés", stage == Stage.ENGLISH, Modifier.weight(1f))
        Chip("Lectura", stage == Stage.READING, Modifier.weight(1f))
    }
}

@Composable
private fun Chip(label: String, active: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Text(
            label,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun MathStage(
    difficulty: com.controlparental.kioscosuave.Difficulty,
    total: Int,
    requiredCorrect: Int,
    onDone: () -> Unit
) {
    var correct by remember { mutableIntStateOf(0) }
    var attempts by remember { mutableIntStateOf(0) }
    var question by remember { mutableStateOf(ChallengeEngine.generateMath(difficulty)) }
    var selected by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<Boolean?>(null) } // null = sin responder aún

    val done = correct >= requiredCorrect
    val accuracy = if (attempts > 0) (correct * 100 / attempts) else 0

    Text(
        "Matemáticas · necesitas $requiredCorrect de $total (80%)",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        "Aciertos: $correct/$requiredCorrect · Intentos: $attempts · Precisión: $accuracy%",
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(8.dp))
    QuestionCard(question.question)
    Spacer(Modifier.height(12.dp))

    // Cada respuesta (correcta o no) bloquea el ejercicio; NO se reintenta el mismo.
    OptionsGrid(options = question.options, selected = selected, correctFlag = result) { opt ->
        if (result != null) return@OptionsGrid
        selected = opt
        val ok = opt == question.answer
        result = ok
        attempts++
        if (ok) correct++
    }

    result?.let { ok ->
        Spacer(Modifier.height(12.dp))
        FeedbackBox(
            ok,
            if (ok) "¡Correcto! ${question.explanation}"
            else "La respuesta correcta era ${question.answer}. ${question.explanation}"
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (correct >= requiredCorrect) {
                    onDone()
                } else {
                    // Siempre un ejercicio DIFERENTE al recién resuelto.
                    question = ChallengeEngine.generateMath(difficulty, exclude = question.question)
                    selected = null
                    result = null
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (done) "Continuar a Inglés" else "Siguiente ejercicio") }
    }
}

@Composable
private fun EnglishStage(target: Int, onDone: () -> Unit) {
    var count by remember { mutableIntStateOf(0) }
    var exercise by remember { mutableStateOf<EnglishExercise>(ChallengeEngine.randomEnglish()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var correct by remember { mutableStateOf<Boolean?>(null) }

    Text(
        "Inglés · Past Tense (${count + 1} de $target)",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary
    )
    Spacer(Modifier.height(8.dp))
    Text(exercise.instruction, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(8.dp))
    QuestionCard(exercise.question)
    Spacer(Modifier.height(12.dp))

    OptionsGrid(exercise.options, selected, correct) { opt ->
        if (correct == true) return@OptionsGrid
        selected = opt
        correct = opt == exercise.correctAnswer
        if (correct == true) count++
    }

    correct?.let { ok ->
        Spacer(Modifier.height(12.dp))
        FeedbackBox(ok, exercise.explanation)
        if (ok) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (count >= target) {
                        onDone()
                    } else {
                        exercise = ChallengeEngine.randomEnglish()
                        selected = null
                        correct = null
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (count >= target) "Continuar a Lectura" else "Siguiente") }
        }
    }
}

@Composable
private fun ReadingStage(onApproved: () -> Unit) {
    val passage by remember { mutableStateOf<ReadingPassage>(ChallengeEngine.randomReading()) }
    var summary by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<SummaryResult?>(null) }

    Text(
        "Comprensión lectora",
        style = MaterialTheme.typography.labelLarge,
        color = Color(0xFFF59E0B)
    )
    Spacer(Modifier.height(8.dp))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Text(passage.title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Text(passage.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
    Spacer(Modifier.height(12.dp))

    val words = summary.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    OutlinedTextField(
        value = summary,
        onValueChange = { if (result?.approved != true) summary = it },
        label = { Text("Escribe tu resumen ($words palabras)") },
        modifier = Modifier.fillMaxWidth().height(140.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
    )

    result?.let { r ->
        Spacer(Modifier.height(12.dp))
        FeedbackBox(r.approved, "${r.feedback}\n\n💡 ${r.suggestions}  ·  Nota: ${r.score}/100")
    }

    Spacer(Modifier.height(12.dp))
    if (result?.approved == true) {
        Button(onClick = onApproved, modifier = Modifier.fillMaxWidth()) {
            Text("¡Desbloquear tablet!")
        }
    } else {
        Button(
            onClick = { result = ChallengeEngine.evaluateSummary(passage.text, summary) },
            enabled = summary.trim().length >= 15,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Enviar resumen") }
    }
}

@Composable
private fun QuestionCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Text(
            text,
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun OptionsGrid(
    options: List<String>,
    selected: String?,
    correctFlag: Boolean?,
    onClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { opt ->
                    val container = when {
                        selected == opt && correctFlag == true -> MaterialTheme.colorScheme.secondary
                        selected == opt && correctFlag == false -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.surface
                    }
                    Button(
                        onClick = { onClick(opt) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = container),
                        modifier = Modifier.weight(1f)
                    ) { Text(opt) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FeedbackBox(ok: Boolean, message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (ok) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
        )
    ) {
        Text(
            message,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
