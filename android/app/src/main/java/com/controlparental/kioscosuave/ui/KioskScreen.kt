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
import androidx.compose.runtime.mutableStateListOf
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

/** Precisión mínima (aciertos ÷ intentos) para aprobar cada etapa. */
private const val PASS_ACCURACY = 80

private enum class Stage { MATH, ENGLISH, READING }

/** Modelo unificado de pregunta de opción múltiple (mate o inglés). */
private data class Quiz(
    val instruction: String?,
    val question: String,
    val options: List<String>,
    val answer: String,
    val explanation: String
)

private fun MathQuestion.toQuiz() = Quiz(null, question, options, answer, explanation)
private fun EnglishExercise.toQuiz() = Quiz(instruction, question, options, correctAnswer, explanation)

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
            Stage.MATH -> MultipleChoiceStage(
                title = "Matemáticas · operaciones y situaciones",
                accent = MaterialTheme.colorScheme.primary,
                window = config.mathWindow,
                nextLabel = "Continuar a Inglés",
                initial = { ChallengeEngine.generateMath(config.difficulty).toQuiz() },
                loadNext = { prev -> ChallengeEngine.generateMath(config.difficulty, prev).toQuiz() },
                onDone = { stage = Stage.ENGLISH }
            )
            Stage.ENGLISH -> MultipleChoiceStage(
                title = "Inglés · Past Tense",
                accent = MaterialTheme.colorScheme.secondary,
                window = config.englishWindow,
                nextLabel = "Continuar a Lectura",
                initial = { ChallengeEngine.randomEnglish().toQuiz() },
                loadNext = { prev -> ChallengeEngine.randomEnglish(prev).toQuiz() },
                onDone = { stage = Stage.READING }
            )
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

/**
 * Etapa de opción múltiple con meta de precisión del 80% sobre una VENTANA
 * MÓVIL de las últimas [window] respuestas.
 * - Una sola oportunidad por pregunta (evita adivinar presionando al azar).
 * - Cada respuesta entra en la ventana; las viejas salen. El niño puede remontar.
 * - Se aprueba cuando la ventana está llena y >= 80% de ella son aciertos.
 */
@Composable
private fun MultipleChoiceStage(
    title: String,
    accent: Color,
    window: Int,
    nextLabel: String,
    initial: () -> Quiz,
    loadNext: (String) -> Quiz,
    onDone: () -> Unit
) {
    val history = remember { mutableStateListOf<Boolean>() } // historial de aciertos/fallos
    var quiz by remember { mutableStateOf(initial()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<Boolean?>(null) }

    val recent = history.takeLast(window)
    val windowHits = recent.count { it }
    val windowCount = recent.size
    val requiredCorrect = (window * 8 + 9) / 10 // ceil(window * 0.8)
    val accuracy = if (windowCount > 0) windowHits * 100 / windowCount else 0
    val passed = windowCount >= window && windowHits >= requiredCorrect

    Text(title, style = MaterialTheme.typography.labelLarge, color = accent)
    Text(
        "Últimas $windowCount/$window · Aciertos en ventana: $windowHits/$window · " +
            "Precisión: $accuracy% (meta $PASS_ACCURACY%)",
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(8.dp))
    quiz.instruction?.let {
        Text(it, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
    }
    QuestionCard(quiz.question)
    Spacer(Modifier.height(12.dp))

    OptionsGrid(quiz.options, selected, result) { opt ->
        if (result != null) return@OptionsGrid // ya respondió: no se reintenta
        selected = opt
        val ok = opt == quiz.answer
        result = ok
        history.add(ok)
    }

    result?.let { ok ->
        Spacer(Modifier.height(12.dp))
        FeedbackBox(
            ok,
            if (ok) "¡Correcto! ${quiz.explanation}"
            else "La respuesta correcta era ${quiz.answer}. ${quiz.explanation}"
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (passed) {
                    onDone()
                } else {
                    quiz = loadNext(quiz.question) // siempre una pregunta diferente
                    selected = null
                    result = null
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (passed) nextLabel else "Siguiente pregunta") }
    }
}

@Composable
private fun ReadingStage(onApproved: () -> Unit) {
    val passage by remember { mutableStateOf<ReadingPassage>(ChallengeEngine.randomReading()) }
    var summary by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<SummaryResult?>(null) }

    val passed = (result?.score ?: 0) >= PASS_ACCURACY

    Text(
        "Comprensión lectora · meta $PASS_ACCURACY/100",
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
        onValueChange = { if (!passed) summary = it },
        label = { Text("Escribe tu resumen ($words palabras)") },
        modifier = Modifier.fillMaxWidth().height(140.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
    )

    result?.let { r ->
        Spacer(Modifier.height(12.dp))
        FeedbackBox(passed, "${r.feedback}\n\n💡 ${r.suggestions}  ·  Puntaje: ${r.score}/100")
    }

    Spacer(Modifier.height(12.dp))
    if (passed) {
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
