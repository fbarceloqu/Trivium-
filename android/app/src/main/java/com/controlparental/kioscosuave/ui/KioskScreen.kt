package com.controlparental.kioscosuave.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlparental.kioscosuave.ChallengeEngine
import com.controlparental.kioscosuave.ChildProfile
import com.controlparental.kioscosuave.EnglishExercise
import com.controlparental.kioscosuave.GradeLevel
import com.controlparental.kioscosuave.MathQuestion
import com.controlparental.kioscosuave.ProgressSync
import com.controlparental.kioscosuave.ReadingPassage
import com.controlparental.kioscosuave.SummaryResult
import com.controlparental.kioscosuave.TtsManager

/** Precisión mínima (aciertos ÷ intentos) para aprobar cada etapa. */
private const val PASS_ACCURACY = 80

private enum class Stage { MATH, ENGLISH, READING }

/** Contenido del botón de ayuda 💡 (ejemplo resuelto o guía). */
private data class Help(val title: String, val lines: List<String>)

/** Modelo unificado de pregunta de opción múltiple (mate o inglés). */
private data class Quiz(
    val instruction: String?,
    val question: String,
    val options: List<String>,
    val answer: String,
    val afterLines: List<String>, // se muestran tras responder (procedimiento / regla)
    val help: Help?,              // contenido del botón de ayuda
    val speech: String? = null,   // texto que lee el botón 🔊 (TTS)
    val speechEnglish: Boolean = false
)

/** Quita emojis/símbolos para que el TTS no lea basura. */
private fun stripEmoji(s: String): String =
    s.replace(Regex("[\\p{So}\\p{Cs}\\uFE0F\\u20E3_]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun MathQuestion.toQuiz() = Quiz(
    instruction = null,
    question = question,
    options = options,
    answer = answer,
    afterLines = steps,
    help = Help(example.title, example.lines),
    speech = stripEmoji(question.replace("\n", ". "))
)

private fun EnglishExercise.toQuiz(starter: Boolean = false): Quiz {
    // Si la "pregunta" tiene letras es texto en inglés (se lee con voz en inglés);
    // si es solo un dibujo (emoji), se lee la instrucción en español.
    val questionIsText = question.any { it.isLetter() }
    return Quiz(
        instruction = instruction,
        question = question,
        options = options,
        answer = correctAnswer,
        afterLines = listOf(explanation, "🔊 Pronunciación: $phonetic"),
        help = if (starter) Help(ChallengeEngine.starterEnglishHelp.title, ChallengeEngine.starterEnglishHelp.lines)
        else Help(ChallengeEngine.englishHelp.title, ChallengeEngine.englishHelp.lines),
        speech = if (questionIsText) stripEmoji(question) else instruction,
        speechEnglish = questionIsText
    )
}

private fun ChallengeEngine.ReadingQuiz.toQuiz(): Quiz {
    val isCompletion = sentence.contains('_')
    return Quiz(
        instruction = if (isCompletion) "Mira el dibujo y completa la palabra:  $sentence"
        else "Lee despacio:  «$sentence»",
        question = question,
        options = options,
        answer = answer,
        afterLines = if (isCompletion)
            listOf("La palabra completa es: ${sentence.replaceFirst("_", answer)}")
        else listOf("La oración dice: «$sentence»"),
        help = Help(ChallengeEngine.readingQuizHelp.title, ChallengeEngine.readingQuizHelp.lines),
        speech = if (isCompletion) "Mira el dibujo y completa la palabra. $question"
        else "${stripEmoji(sentence)} $question"
    )
}

@Composable
fun KioskScreen(
    profile: ChildProfile,
    onAllComplete: () -> Unit,
    onParentAccess: () -> Unit
) {
    val config = remember(profile) { profile.config }
    var stage by remember { mutableStateOf(Stage.MATH) }
    // Preescolar/1º: todo el texto y los dibujos se muestran mucho más grandes.
    val big = profile.grade == GradeLevel.PREESCOLAR

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
                Text(
                    "Hola, ${profile.name} 👋",
                    fontSize = if (big) 26.sp else MaterialTheme.typography.titleMedium.fontSize,
                    fontWeight = if (big) FontWeight.Bold else null,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Completa tus tareas para desbloquear la tablet",
                    fontSize = if (big) 16.sp else MaterialTheme.typography.bodySmall.fontSize,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            IconButton(onClick = onParentAccess) {
                Icon(Icons.Filled.Lock, contentDescription = "Acceso de padres")
            }
        }

        Spacer(Modifier.height(8.dp))
        StepIndicator(stage, big)
        Spacer(Modifier.height(16.dp))

        when (stage) {
            Stage.MATH -> MultipleChoiceStage(
                title = "Matemáticas · operaciones y situaciones",
                accent = MaterialTheme.colorScheme.primary,
                window = config.mathWindow,
                nextLabel = "Continuar a Inglés",
                stageKey = "math",
                big = big,
                initial = { ChallengeEngine.generateMath(config.difficulty).toQuiz() },
                loadNext = { prev -> ChallengeEngine.generateMath(config.difficulty, prev).toQuiz() },
                onDone = { stage = Stage.ENGLISH }
            )
            Stage.ENGLISH -> {
                val starter = profile.grade == GradeLevel.PREESCOLAR
                MultipleChoiceStage(
                    title = if (starter) "Inglés · palabras con dibujos"
                    else "Inglés · Lección de hoy: ${ChallengeEngine.todaysEnglishUnitTitle()}",
                    accent = MaterialTheme.colorScheme.secondary,
                    window = config.englishWindow,
                    nextLabel = "Continuar a Lectura",
                    stageKey = "english",
                    big = big,
                    initial = { ChallengeEngine.randomEnglish(starter = starter).toQuiz(starter) },
                    loadNext = { prev ->
                        ChallengeEngine.randomEnglish(starter = starter, exclude = prev).toQuiz(starter)
                    },
                    onDone = { stage = Stage.READING }
                )
            }
            Stage.READING -> when (profile.grade) {
                // Preescolar/1º: leer una oración corta y responder (sin escribir).
                GradeLevel.PREESCOLAR -> MultipleChoiceStage(
                    title = "Lectura · lee y responde",
                    accent = Color(0xFFF59E0B),
                    window = 5,
                    nextLabel = "¡Desbloquear tablet!",
                    stageKey = "reading",
                    big = big,
                    initial = { ChallengeEngine.randomReadingQuiz().toQuiz() },
                    loadNext = { prev -> ChallengeEngine.randomReadingQuiz(exclude = prev).toQuiz() },
                    onDone = onAllComplete
                )
                else -> ReadingStage(
                    advanced = profile.grade == GradeLevel.SECUNDARIA,
                    onApproved = onAllComplete
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(stage: Stage, big: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("Mate", stage == Stage.MATH, big, Modifier.weight(1f))
        Chip("Inglés", stage == Stage.ENGLISH, big, Modifier.weight(1f))
        Chip("Lectura", stage == Stage.READING, big, Modifier.weight(1f))
    }
}

@Composable
private fun Chip(label: String, active: Boolean, big: Boolean = false, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Text(
            label,
            modifier = Modifier.fillMaxWidth().padding(vertical = if (big) 14.dp else 8.dp),
            textAlign = TextAlign.Center,
            fontSize = if (big) 18.sp else MaterialTheme.typography.labelMedium.fontSize,
            fontWeight = if (big) FontWeight.Bold else null,
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
    stageKey: String,
    big: Boolean = false,
    initial: () -> Quiz,
    loadNext: (String) -> Quiz,
    onDone: () -> Unit
) {
    val ctx = LocalContext.current
    val history = remember { mutableStateListOf<Boolean>() } // historial de aciertos/fallos
    var quiz by remember { mutableStateOf(initial()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<Boolean?>(null) }
    var showHelp by remember { mutableStateOf(false) }

    val recent = history.takeLast(window)
    val windowHits = recent.count { it }
    val windowCount = recent.size
    val requiredCorrect = (window * 8 + 9) / 10 // ceil(window * 0.8)
    val accuracy = if (windowCount > 0) windowHits * 100 / windowCount else 0
    val passed = windowCount >= window && windowHits >= requiredCorrect

    // Diálogo de ayuda con ejemplo resuelto / guía.
    if (showHelp && quiz.help != null) {
        val help = quiz.help!!
        AlertDialog(
            onDismissRequest = { showHelp = false },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Entendido", fontSize = if (big) 18.sp else 14.sp) } },
            title = { Text(help.title, fontSize = if (big) 20.sp else MaterialTheme.typography.titleLarge.fontSize) },
            text = {
                Text(
                    help.lines.joinToString("\n"),
                    fontSize = if (big) 18.sp else MaterialTheme.typography.bodyMedium.fontSize,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = if (big) 22.sp else MaterialTheme.typography.labelLarge.fontSize,
            fontWeight = if (big) FontWeight.Bold else null,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            modifier = Modifier.weight(1f)
        )
        quiz.speech?.let { speech ->
            IconButton(onClick = { TtsManager.speak(ctx, speech, quiz.speechEnglish) }) {
                Icon(
                    Icons.Filled.VolumeUp, contentDescription = "Escuchar la pregunta", tint = accent,
                    modifier = if (big) Modifier.size(36.dp) else Modifier
                )
            }
        }
        if (quiz.help != null) {
            IconButton(onClick = { showHelp = true }) {
                Icon(
                    Icons.Filled.Lightbulb, contentDescription = "Ver ejemplo de ayuda", tint = accent,
                    modifier = if (big) Modifier.size(36.dp) else Modifier
                )
            }
        }
    }
    Text(
        "Últimas $windowCount/$window · Aciertos en ventana: $windowHits/$window · " +
            "Precisión: $accuracy% (meta $PASS_ACCURACY%)",
        fontSize = if (big) 16.sp else MaterialTheme.typography.bodySmall.fontSize,
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(8.dp))
    quiz.instruction?.let {
        Text(
            it,
            fontSize = if (big) 22.sp else MaterialTheme.typography.bodyMedium.fontSize,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
    }
    QuestionCard(quiz.question, big)
    Spacer(Modifier.height(12.dp))

    OptionsGrid(quiz.options, selected, result, big) { opt ->
        if (result != null) return@OptionsGrid // ya respondió: no se reintenta
        selected = opt
        val ok = opt == quiz.answer
        result = ok
        history.add(ok)
    }

    result?.let { ok ->
        Spacer(Modifier.height(12.dp))
        val header = if (ok) "¡Correcto!" else "La respuesta correcta era ${quiz.answer}."
        FeedbackBox(ok, (listOf(header) + quiz.afterLines).joinToString("\n"), big)
        Spacer(Modifier.height(8.dp))
        Button(
            contentPadding = if (big) androidx.compose.foundation.layout.PaddingValues(vertical = 18.dp)
            else androidx.compose.material3.ButtonDefaults.ContentPadding,
            onClick = {
                if (passed) {
                    // Sube el desempeño de la etapa completa (aciertos/intentos totales).
                    ProgressSync.reportStage(ctx, stageKey, history.count { it }, history.size)
                    onDone()
                } else {
                    quiz = loadNext(quiz.question) // siempre una pregunta diferente
                    selected = null
                    result = null
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (passed) nextLabel else "Siguiente pregunta",
                fontSize = if (big) 20.sp else 14.sp
            )
        }
    }
}

@Composable
private fun ReadingStage(advanced: Boolean, onApproved: () -> Unit) {
    val ctx = LocalContext.current
    val passage by remember { mutableStateOf<ReadingPassage>(ChallengeEngine.randomReading(advanced)) }
    var summary by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<SummaryResult?>(null) }
    var submitCount by remember { mutableStateOf(0) }

    val passed = (result?.score ?: 0) >= PASS_ACCURACY

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Comprensión lectora · meta $PASS_ACCURACY/100",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFF59E0B)
        )
        IconButton(onClick = {
            TtsManager.speak(ctx, "${passage.title}. ${passage.text}")
        }) {
            Icon(Icons.Filled.VolumeUp, contentDescription = "Escuchar la lectura", tint = Color(0xFFF59E0B))
        }
    }
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
            onClick = {
                submitCount++
                val r = ChallengeEngine.evaluateSummary(passage.text, summary)
                result = r
                // Sube el resultado de lectura (score y nº de envíos).
                ProgressSync.reportReading(ctx, r.score, submitCount)
            },
            enabled = summary.trim().length >= 15,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Enviar resumen") }
    }
}

/** Una línea es "de dibujos" si no tiene letras ni dígitos (solo emojis/espacios). */
private fun isEmojiLine(s: String): Boolean =
    s.isNotBlank() && s.none { it.isLetterOrDigit() }

@Composable
private fun QuestionCard(text: String, big: Boolean = false) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            text.split("\n").forEach { line ->
                if (line.isBlank()) return@forEach
                if (isEmojiLine(line)) {
                    // Fila de dibujos: grande para poder contarlos; si es un solo
                    // dibujo (vocabulario), gigante. En preescolar (big), aún más.
                    val single = line.count { !it.isWhitespace() } <= 4
                    val size = if (big) { if (single) 130.sp else 60.sp } else { if (single) 72.sp else 40.sp }
                    val lh = if (big) { if (single) 150.sp else 76.sp } else { if (single) 84.sp else 52.sp }
                    Text(
                        line,
                        textAlign = TextAlign.Center,
                        fontSize = size,
                        lineHeight = lh,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                } else {
                    Text(
                        line,
                        textAlign = TextAlign.Center,
                        fontSize = if (big) 32.sp else MaterialTheme.typography.titleLarge.fontSize,
                        fontWeight = if (big) FontWeight.Bold else null,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionsGrid(
    options: List<String>,
    selected: String?,
    correctFlag: Boolean?,
    big: Boolean = false,
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
                        contentPadding = if (big)
                            androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp, horizontal = 12.dp)
                        else androidx.compose.material3.ButtonDefaults.ContentPadding,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Opciones de dibujo (elegir el emoji correcto) en grande;
                        // más grande aún en preescolar. Texto plano también se agranda.
                        if (isEmojiLine(opt)) Text(opt, fontSize = if (big) 56.sp else 34.sp)
                        else Text(opt, fontSize = if (big) 26.sp else 14.sp, fontWeight = if (big) FontWeight.Bold else null)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FeedbackBox(ok: Boolean, message: String, big: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (ok) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
        )
    ) {
        Text(
            message,
            modifier = Modifier.fillMaxWidth().padding(if (big) 16.dp else 12.dp),
            fontSize = if (big) 20.sp else MaterialTheme.typography.bodySmall.fontSize,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
