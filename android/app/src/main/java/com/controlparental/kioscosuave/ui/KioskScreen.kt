package com.controlparental.kioscosuave.ui

import androidx.compose.foundation.Image
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlparental.kioscosuave.ChallengeEngine
import com.controlparental.kioscosuave.ChildProfile
import com.controlparental.kioscosuave.EnglishExercise
import com.controlparental.kioscosuave.GeminiClient
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
    val speechEnglish: Boolean = false,
    val wordToSpeak: String? = null,     // palabra en inglés a pronunciar al responder
    val exampleSentence: String? = null  // oración de ejemplo con esa palabra (botón "escuchar en una oración")
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

    // Palabra a pronunciar al seleccionar una respuesta: en preescolar, una de
    // (pregunta, respuesta) siempre es la palabra en texto (la otra es el emoji);
    // en primaria/secundaria la respuesta correcta ya es la palabra/frase en inglés.
    val word = if (starter) {
        listOf(question, correctAnswer).firstOrNull { it.any(Char::isLetter) }
    } else correctAnswer

    // Oración de ejemplo para el botón "Escuchar en una oración".
    val sentence = if (starter && word != null) {
        val article = if (word.firstOrNull()?.lowercaseChar() in listOf('a', 'e', 'i', 'o', 'u')) "an" else "a"
        "I see $article $word."
    } else if (question.contains("______")) {
        question.replace("______", correctAnswer)
    } else null

    return Quiz(
        instruction = instruction,
        question = question,
        options = options,
        answer = correctAnswer,
        afterLines = listOf(explanation, "🔊 Pronunciación: $phonetic"),
        help = if (starter) Help(ChallengeEngine.starterEnglishHelp.title, ChallengeEngine.starterEnglishHelp.lines)
        else Help(ChallengeEngine.englishHelp.title, ChallengeEngine.englishHelp.lines),
        speech = if (questionIsText) stripEmoji(question) else instruction,
        speechEnglish = questionIsText,
        wordToSpeak = word,
        exampleSentence = sentence
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
    val headerScale = screenScale()

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
                    fontSize = if (big) (26 * headerScale).sp else MaterialTheme.typography.titleMedium.fontSize,
                    lineHeight = if (big) (32 * headerScale).sp else MaterialTheme.typography.titleMedium.lineHeight,
                    fontWeight = if (big) FontWeight.Bold else null,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Completa tus tareas para desbloquear la tablet",
                    fontSize = if (big) (16 * headerScale).sp else MaterialTheme.typography.bodySmall.fontSize,
                    lineHeight = if (big) (21 * headerScale).sp else MaterialTheme.typography.bodySmall.lineHeight,
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
                // Secundaria rota también entre el banco de gramática avanzada
                // (más variedad y dificultad); Primaria se queda solo con lo básico.
                val advanced = profile.grade == GradeLevel.SECUNDARIA
                MultipleChoiceStage(
                    title = if (starter) "Inglés · palabras con dibujos"
                    else "Inglés · Lección de hoy: ${ChallengeEngine.todaysEnglishUnitTitle(advanced)}",
                    accent = MaterialTheme.colorScheme.secondary,
                    window = config.englishWindow,
                    nextLabel = "Continuar a Lectura",
                    stageKey = "english",
                    big = big,
                    initial = { ChallengeEngine.randomEnglish(starter = starter, advanced = advanced).toQuiz(starter) },
                    loadNext = { prev ->
                        ChallengeEngine.randomEnglish(starter = starter, exclude = prev, advanced = advanced).toQuiz(starter)
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
    val scale = screenScale()
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
            fontSize = if (big) (18 * scale).sp else MaterialTheme.typography.labelMedium.fontSize,
            lineHeight = if (big) (23 * scale).sp else MaterialTheme.typography.labelMedium.lineHeight,
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
    val scale = screenScale()
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
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Entendido", fontSize = (if (big) 18 * scale else 14f).sp) } },
            title = { Text(help.title, fontSize = if (big) (20 * scale).sp else MaterialTheme.typography.titleLarge.fontSize) },
            text = {
                Text(
                    help.lines.joinToString("\n"),
                    fontSize = if (big) (18 * scale).sp else MaterialTheme.typography.bodyMedium.fontSize,
                    lineHeight = if (big) (24 * scale).sp else MaterialTheme.typography.bodyMedium.lineHeight,
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
            fontSize = if (big) (22 * scale).sp else MaterialTheme.typography.labelLarge.fontSize,
            lineHeight = if (big) (28 * scale).sp else MaterialTheme.typography.labelLarge.lineHeight,
            fontWeight = if (big) FontWeight.Bold else null,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            modifier = Modifier.weight(1f)
        )
        quiz.speech?.let { speech ->
            IconButton(onClick = { TtsManager.speak(ctx, speech, quiz.speechEnglish) }) {
                Icon(
                    Icons.Filled.VolumeUp, contentDescription = "Escuchar la pregunta", tint = accent,
                    modifier = if (big) Modifier.size((36 * scale).dp) else Modifier
                )
            }
        }
        if (quiz.help != null) {
            IconButton(onClick = { showHelp = true }) {
                Icon(
                    Icons.Filled.Lightbulb, contentDescription = "Ver ejemplo de ayuda", tint = accent,
                    modifier = if (big) Modifier.size((36 * scale).dp) else Modifier
                )
            }
        }
    }
    Text(
        "Últimas $windowCount/$window · Aciertos en ventana: $windowHits/$window · " +
            "Precisión: $accuracy% (meta $PASS_ACCURACY%)",
        fontSize = if (big) (16 * scale).sp else MaterialTheme.typography.bodySmall.fontSize,
        lineHeight = if (big) (21 * scale).sp else MaterialTheme.typography.bodySmall.lineHeight,
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(8.dp))
    quiz.instruction?.let {
        Text(
            it,
            fontSize = if (big) (22 * scale).sp else MaterialTheme.typography.bodyMedium.fontSize,
            lineHeight = if (big) (29 * scale).sp else MaterialTheme.typography.bodyMedium.lineHeight,
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
        // Pronuncia la palabra correcta al responder (acierte o no), para reforzar
        // la pronunciación correcta cada vez.
        quiz.wordToSpeak?.let { TtsManager.speak(ctx, it, english = true) }
    }

    result?.let { ok ->
        Spacer(Modifier.height(12.dp))
        val header = if (ok) "¡Correcto!" else "La respuesta correcta era ${quiz.answer}."
        FeedbackBox(ok, (listOf(header) + quiz.afterLines).joinToString("\n"), big)
        quiz.exampleSentence?.let { sentence ->
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { TtsManager.speak(ctx, sentence, english = true) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Escuchar en una oración", fontSize = (if (big) 18 * scale else 14f).sp)
            }
        }
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
                fontSize = (if (big) 20 * scale else 14f).sp
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
    var evaluating by remember { mutableStateOf(false) }
    var evaluatedByAi by remember { mutableStateOf(false) }

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

    if (evaluating) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.size(8.dp))
            Text("IA evaluando tu resumen…", style = MaterialTheme.typography.bodySmall)
        }
    }

    result?.let { r ->
        Spacer(Modifier.height(12.dp))
        val badge = if (evaluatedByAi) "🤖 Evaluado por IA" else "📋 Evaluado localmente (sin conexión)"
        FeedbackBox(passed, "$badge\n\n${r.feedback}\n\n💡 ${r.suggestions}  ·  Puntaje: ${r.score}/100")
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
                evaluating = true
                // Intenta evaluar con IA (Gemini); si falla, no hay internet, o no
                // hay API key configurada, degrada a la heurística local (misma
                // política fail-safe que el backend web: nunca aprueba a ciegas).
                GeminiClient.evaluateSummary(passage.text, summary) { aiResult ->
                    evaluating = false
                    if (aiResult != null) {
                        evaluatedByAi = true
                        result = SummaryResult(
                            approved = aiResult.approved,
                            score = aiResult.score,
                            feedback = aiResult.feedback,
                            suggestions = aiResult.suggestions
                        )
                    } else {
                        evaluatedByAi = false
                        result = ChallengeEngine.evaluateSummary(passage.text, summary)
                    }
                    // Sube el resultado de lectura (score y nº de envíos).
                    ProgressSync.reportReading(ctx, result!!.score, submitCount)
                }
            },
            enabled = summary.trim().length >= 15 && !evaluating,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Enviar resumen") }
    }
}

/** Una línea es "de dibujos" si no tiene letras ni dígitos (solo emojis/espacios). */
private fun isEmojiLine(s: String): Boolean =
    s.isNotBlank() && s.none { it.isLetterOrDigit() }

/**
 * Factor de escala según el ancho REAL de la pantalla (dp), para que el modo
 * "big" (Preescolar) se vea bien tanto en un teléfono chico como en una
 * tablet grande, en vez de usar los mismos tamaños fijos siempre.
 * 400dp = ancho de referencia (tablet mediana en retrato); se limita el
 * rango para no exagerar en pantallas extremas.
 */
@Composable
private fun screenScale(): Float {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return (widthDp / 400f).coerceIn(0.75f, 1.5f)
}

/**
 * Recurso drawable real para un emoji del vocabulario, si ya existe un
 * archivo en res/drawable-nodpi/ (ver ChallengeEngine.wordForEmoji). Se
 * revisa por nombre en tiempo real, así que basta con soltar el archivo en
 * la carpeta -sin tocar código- para que se use automáticamente.
 */
@Composable
private fun imageResFor(emoji: String): Int? {
    val ctx = LocalContext.current
    return remember(emoji) {
        val word = ChallengeEngine.wordForEmoji(emoji) ?: return@remember null
        val name = word.replace(" ", "_").lowercase()
        val id = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        id.takeIf { it != 0 }
    }
}

@Composable
private fun QuestionCard(text: String, big: Boolean = false) {
    val scale = screenScale()
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            text.split("\n").forEach { line ->
                if (line.isBlank()) return@forEach
                if (isEmojiLine(line)) {
                    val tokens = line.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                    val single = tokens.size <= 1
                    // Solo se usa imagen real si TODA la fila es el mismo emoji
                    // repetido (el caso de contar/vocabulario); si son emoji
                    // mixtos (p. ej. 🔵🟠 de "qué suma muestra"), se queda en emoji.
                    val imgRes = tokens.distinct().singleOrNull()?.let { imageResFor(it) }

                    if (imgRes != null) {
                        val imgSize = if (big) { if (single) 140 * scale else 64 * scale } else { if (single) 84f else 44f }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            tokens.chunked(5).forEach { rowTokens ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    rowTokens.forEach {
                                        Image(
                                            painter = painterResource(imgRes),
                                            contentDescription = null,
                                            modifier = Modifier.size(imgSize.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Fila de dibujos: grande para poder contarlos; si es un solo
                        // dibujo (vocabulario), gigante. En preescolar (big), aún más,
                        // y escalado según el ancho real de la pantalla.
                        val size = if (big) { if (single) 130 * scale else 60 * scale } else { if (single) 72f else 40f }
                        val lh = if (big) { if (single) 150 * scale else 76 * scale } else { if (single) 84f else 52f }
                        Text(
                            line,
                            textAlign = TextAlign.Center,
                            fontSize = size.sp,
                            lineHeight = lh.sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        line,
                        textAlign = TextAlign.Center,
                        fontSize = if (big) (32 * scale).sp else MaterialTheme.typography.titleLarge.fontSize,
                        lineHeight = if (big) (40 * scale).sp else MaterialTheme.typography.titleLarge.lineHeight,
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
    val scale = screenScale()
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
                        // Opciones de dibujo (elegir el emoji/imagen correcta) en
                        // grande; más grande aún en preescolar. Si ya hay una
                        // imagen real para esa palabra, se usa en vez del emoji.
                        val imgRes = if (isEmojiLine(opt)) imageResFor(opt) else null
                        when {
                            imgRes != null -> Image(
                                painter = painterResource(imgRes),
                                contentDescription = null,
                                modifier = Modifier.size((if (big) 64 * scale else 40f).dp)
                            )
                            isEmojiLine(opt) -> Text(opt, fontSize = (if (big) 56 * scale else 34f).sp)
                            else -> Text(
                                opt,
                                fontSize = (if (big) 26 * scale else 14f).sp,
                                fontWeight = if (big) FontWeight.Bold else null
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FeedbackBox(ok: Boolean, message: String, big: Boolean = false) {
    val scale = screenScale()
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (ok) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
        )
    ) {
        Text(
            message,
            modifier = Modifier.fillMaxWidth().padding(if (big) 16.dp else 12.dp),
            fontSize = if (big) (20 * scale).sp else MaterialTheme.typography.bodySmall.fontSize,
            lineHeight = if (big) (27 * scale).sp else MaterialTheme.typography.bodySmall.lineHeight,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
