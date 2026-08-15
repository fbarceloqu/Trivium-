package com.controlparental.kioscosuave.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.controlparental.kioscosuave.MemoryStore
import com.controlparental.kioscosuave.TtsManager
import com.controlparental.kioscosuave.curriculum.Curriculum
import com.controlparental.kioscosuave.curriculum.ExerciseFormat
import com.controlparental.kioscosuave.curriculum.StudySession

/**
 * PANTALLA DEL KIOSCO
 * ===================
 *
 * El reparto del espacio es por RESTRICCIONES, no por números fijos: el
 * ejercicio recibe `weight(1f)` y los dibujos se dimensionan a partir del
 * espacio que realmente sobra ([Adaptive.fitGridItem]). Así el contenido no
 * puede desbordarse: si hay menos sitio, el dibujo encoge; no se corta.
 *
 * Antes todo colgaba de un `verticalScroll` con tamaños fijos multiplicados por
 * una escala que solo miraba el ancho, y en horizontal (1013 × 456 dp) la
 * interfaz crecía al máximo justo donde menos altura había.
 *
 * Todos los tamaños salen de [LocalMetrics]; no debe haber literales sueltos.
 */

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
    val exampleSentence: String? = null, // oración de ejemplo con esa palabra
    // Etiquetas de la memoria de aprendizaje: dicen QUÉ habilidad se está
    // practicando y con qué presentación, para poder registrar el resultado.
    val skillId: String? = null,
    val format: ExerciseFormat? = null
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
    speech = stripEmoji(question.replace("\n", ". ")),
    skillId = skillId,
    format = format
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

// =====================================================================
//  RAÍZ
// =====================================================================

@Composable
fun KioskScreen(
    profile: ChildProfile,
    onAllComplete: () -> Unit,
    onParentAccess: () -> Unit
) {
    val ctx = LocalContext.current
    val config = remember(profile) { profile.config }
    var stage by remember { mutableStateOf(Stage.MATH) }
    // Lenguaje visual según la edad: preescolar y primaria comparten uno
    // amplio y visual; secundaria usa otro más denso y textual. La escala del
    // espacio sigue mandando encima, así que ningún nivel deja de caber.
    val level = Level.forGrade(profile.grade)

    // Se miden las dimensiones REALES disponibles, no la configuración global:
    // así también es correcto en pantalla dividida o ventana flotante.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth.value
        val h = maxHeight.value
        val m = remember(w, h, level) { Adaptive.metrics(w, h, level) }

        CompositionLocalProvider(LocalMetrics provides m) {
            Column(
                modifier = Modifier
                    // widthIn ANTES de fillMaxSize: primero se acota el ancho
                    // máximo y luego se rellena hasta ese tope. Al revés,
                    // fillMaxSize ya habría fijado el mínimo al ancho completo
                    // y el tope no tendría efecto.
                    .widthIn(max = m.contentMaxWidth.dp)
                    .fillMaxSize()
                    .align(Alignment.TopCenter)
                    .padding(m.pagePad.dp)
            ) {
                // En horizontal el saludo no merece una fila entera: comparte
                // con las pestañas y el candado. Se recupera casi el 10% de la
                // altura útil, que es mucho cuando solo hay 760dp.
                if (m.mode == LayoutMode.TWO_PANE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(m.sectionGap.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Hola, ${profile.name} 👋",
                            fontSize = m.greeting.sp,
                            lineHeight = m.lineHeight(m.greeting).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        StepIndicator(stage, Modifier.weight(1f))
                        LockButton(onParentAccess)
                    }
                } else {
                    Header(profile.name, onParentAccess)
                    Spacer(Modifier.size(m.sectionGap.dp))
                    StepIndicator(stage)
                }
                Spacer(Modifier.size(m.sectionGap.dp))

                // ZONA DEL EJERCICIO: se queda con todo el espacio restante.
                // Que sea `weight(1f)` es lo que impide que el contenido empuje
                // los botones fuera de la pantalla.
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    when (stage) {
                        Stage.MATH -> {
                            // SECUNDARIA usa la memoria de aprendizaje: qué se
                            // practica hoy lo decide el planificador según lo
                            // vencido y lo que más se falla, no el azar. Los
                            // demás niveles siguen con generación aleatoria
                            // porque su temario aún no está modelado.
                            val usaMemoria = profile.grade == GradeLevel.SECUNDARIA
                            val session = if (usaMemoria) {
                                remember { StudySession(Curriculum.sec1Math, MemoryStore.load(ctx)) }
                            } else null

                            MultipleChoiceStage(
                                title = "Matemáticas · operaciones y situaciones",
                                accent = MaterialTheme.colorScheme.primary,
                                window = config.mathWindow,
                                nextLabel = "Continuar a Inglés",
                                stageKey = "math",
                                initial = {
                                    (session?.nextQuestion()
                                        ?: ChallengeEngine.generateMath(config.difficulty)).toQuiz()
                                },
                                loadNext = { prev ->
                                    (session?.nextQuestion()
                                        ?: ChallengeEngine.generateMath(config.difficulty, prev)).toQuiz()
                                },
                                onResult = { quiz, ok, wrong ->
                                    val id = quiz.skillId
                                    val fmt = quiz.format
                                    if (session != null && id != null && fmt != null) {
                                        session.record(id, fmt, ok, wrong)
                                        MemoryStore.save(ctx, session.snapshot())
                                        ProgressSync.reportSkill(ctx, id, session.stateOf(id))
                                    }
                                },
                                onDone = { stage = Stage.ENGLISH }
                            )
                        }
                        Stage.ENGLISH -> {
                            val starter = profile.grade == GradeLevel.PREESCOLAR
                            // Secundaria rota también entre el banco de gramática
                            // avanzada; Primaria se queda solo con lo básico.
                            val advanced = profile.grade == GradeLevel.SECUNDARIA
                            MultipleChoiceStage(
                                title = if (starter) "Inglés · palabras con dibujos"
                                else "Inglés · Lección de hoy: ${ChallengeEngine.todaysEnglishUnitTitle(advanced)}",
                                accent = MaterialTheme.colorScheme.secondary,
                                window = config.englishWindow,
                                nextLabel = "Continuar a Lectura",
                                stageKey = "english",
                                initial = { ChallengeEngine.randomEnglish(starter = starter, advanced = advanced).toQuiz(starter) },
                                loadNext = { prev ->
                                    ChallengeEngine.randomEnglish(starter = starter, exclude = prev, advanced = advanced).toQuiz(starter)
                                },
                                onDone = { stage = Stage.READING }
                            )
                        }
                        Stage.READING -> when (profile.grade) {
                            // Preescolar/1º: leer una oración corta y responder.
                            GradeLevel.PREESCOLAR -> MultipleChoiceStage(
                                title = "Lectura · lee y responde",
                                accent = Color(0xFFF59E0B),
                                window = 5,
                                nextLabel = "¡Desbloquear tablet!",
                                stageKey = "reading",
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
        }
    }
}

// =====================================================================
//  CABECERA Y PESTAÑAS
// =====================================================================

@Composable
private fun Header(name: String, onParentAccess: () -> Unit) {
    val m = LocalMetrics.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Hola, $name 👋",
                fontSize = m.greeting.sp,
                lineHeight = m.lineHeight(m.greeting).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        LockButton(onParentAccess)
    }
}

/** Botón de acceso parental. Aparte para poder reubicarlo en horizontal. */
@Composable
private fun LockButton(onParentAccess: () -> Unit) {
    val m = LocalMetrics.current
    IconButton(onClick = onParentAccess, modifier = Modifier.size(m.minTouch.dp)) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = "Acceso de padres",
            modifier = Modifier.size((m.actionIcon * 0.8f).dp)
        )
    }
}

@Composable
private fun StepIndicator(stage: Stage, modifier: Modifier = Modifier) {
    val m = LocalMetrics.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(m.itemGap.dp)
    ) {
        Chip("Mate", stage == Stage.MATH, Modifier.weight(1f))
        Chip("Inglés", stage == Stage.ENGLISH, Modifier.weight(1f))
        Chip("Lectura", stage == Stage.READING, Modifier.weight(1f))
    }
}

@Composable
private fun Chip(label: String, active: Boolean, modifier: Modifier = Modifier) {
    val m = LocalMetrics.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(m.corner.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = m.tabHeight.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                textAlign = TextAlign.Center,
                fontSize = m.tabLabel.sp,
                lineHeight = m.lineHeight(m.tabLabel).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

// =====================================================================
//  ETAPA DE OPCIÓN MÚLTIPLE
// =====================================================================

/**
 * Etapa con meta de precisión del 80% sobre una VENTANA MÓVIL de las últimas
 * [window] respuestas.
 * - Una sola oportunidad por pregunta (evita adivinar presionando al azar).
 * - Cada respuesta entra en la ventana; las viejas salen. El niño puede remontar.
 * - Se aprueba cuando la ventana está llena y >= 80% de ella son aciertos.
 *
 * El layout se parte en dos paneles cuando la pantalla es apaisada: el dibujo a
 * la izquierda y las respuestas a la derecha, en vez de apilar y desplazar.
 */
@Composable
private fun MultipleChoiceStage(
    title: String,
    accent: Color,
    window: Int,
    nextLabel: String,
    stageKey: String,
    initial: () -> Quiz,
    loadNext: (String) -> Quiz,
    onDone: () -> Unit,
    /** Se avisa de cada respuesta para alimentar la memoria de aprendizaje. */
    onResult: (quiz: Quiz, correct: Boolean, wrongChoice: String?) -> Unit = { _, _, _ -> }
) {
    val m = LocalMetrics.current
    val ctx = LocalContext.current
    val history = remember { mutableStateListOf<Boolean>() } // aciertos/fallos
    var quiz by remember { mutableStateOf(initial()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<Boolean?>(null) }
    var showHelp by remember { mutableStateOf(false) }

    val recent = history.takeLast(window)
    val windowHits = recent.count { it }
    val windowCount = recent.size
    val requiredCorrect = (window * 8 + 9) / 10 // ceil(window * 0.8)
    val passed = windowCount >= window && windowHits >= requiredCorrect

    if (showHelp && quiz.help != null) {
        HelpDialog(quiz.help!!) { showHelp = false }
    }

    val onAnswer: (String) -> Unit = onAnswer@{ opt ->
        if (result != null) return@onAnswer // ya respondió: no se reintenta
        selected = opt
        val ok = opt == quiz.answer
        result = ok
        history.add(ok)
        // Alimenta la memoria: al fallar se guarda CUÁL opción incorrecta
        // eligió, porque los distractores son errores típicos concretos.
        onResult(quiz, ok, if (ok) null else opt)
        // Pronuncia la palabra correcta al responder (acierte o no).
        quiz.wordToSpeak?.let { TtsManager.speak(ctx, it, english = true) }
    }

    val onNext: () -> Unit = {
        if (passed) {
            ProgressSync.reportStage(ctx, stageKey, history.count { it }, history.size)
            onDone()
        } else {
            quiz = loadNext(quiz.question) // siempre una pregunta diferente
            selected = null
            result = null
        }
    }

    Column(Modifier.fillMaxSize()) {
        // En dos paneles (horizontal) la altura es el recurso escaso y la
        // cabecera llegaba a comerse la mitad de la pantalla. Ahí el título de
        // etapa sobra —la pestaña activa ya dice de qué materia se trata— y el
        // progreso comparte fila con los controles: dos filas menos de adorno.
        if (m.mode == LayoutMode.TWO_PANE) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(m.sectionGap.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) {
                    ProgressIndicator(windowHits, window, accent)
                }
                StageControls(accent, quiz, ctx) { showHelp = true }
            }
        } else {
            StageBar(title, accent, quiz, ctx) { showHelp = true }
            ProgressIndicator(windowHits, window, accent)
        }
        Spacer(Modifier.size(m.sectionGap.dp))

        if (m.mode == LayoutMode.TWO_PANE) {
            // El reparto depende del CONTENIDO, no es 50/50 fijo: una pregunta
            // de solo texto no necesita media pantalla, y estirarla dejaba una
            // tarjeta casi vacía. Con dibujo la izquierda manda; sin dibujo
            // cede ancho a las respuestas y la tarjeta se ajusta al texto.
            val tieneDibujo = quiz.question.split("\n").any { isEmojiLine(it) }
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(m.sectionGap.dp)
            ) {
                // Ambas columnas arrancan ARRIBA. Centrarlas dejaba ~200dp
                // muertos encima en una tablet de 760dp de alto: el ejercicio
                // aparecía flotando a media pantalla, lejos de las pestañas.
                // Alineado arriba, el espacio sobrante queda abajo, que es
                // donde no estorba.
                Column(
                    modifier = Modifier.weight(if (tieneDibujo) 1f else 0.75f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Top
                ) {
                    InstructionText(quiz.instruction)
                    QuestionCard(
                        quiz.question,
                        if (tieneDibujo) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth(),
                        fill = tieneDibujo
                    )
                }
                // Respuestas + retroalimentación se desplazan si hace falta,
                // pero el botón de continuar va FUERA del scroll: es la acción
                // que siempre debe estar a un toque de distancia.
                // Respuestas, retroalimentación y botón se centran COMO GRUPO.
                // Antes el botón iba anclado al fondo y en una pantalla alta
                // quedaba descolgado, lejos de la explicación que lo motiva.
                //
                // weight(1f, fill = false) es la clave: el contenido toma solo
                // lo que necesita —y se desplaza si algún día no cabe—, en vez
                // de estirarse hasta empujar el botón al borde.
                Column(
                    modifier = Modifier
                        .weight(if (tieneDibujo) 1f else 1.25f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Top
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        AnswerArea(quiz, selected, result, onAnswer)
                    }
                    NextButton(result, passed, nextLabel, onNext)
                }
            }
        } else {
            InstructionText(quiz.instruction)
            QuestionCard(quiz.question, Modifier.fillMaxWidth().weight(1f))
            Spacer(Modifier.size(m.sectionGap.dp))
            AnswerArea(quiz, selected, result, onAnswer)
            NextButton(result, passed, nextLabel, onNext)
        }
    }
}

@Composable
private fun StageBar(
    title: String,
    accent: Color,
    quiz: Quiz,
    ctx: android.content.Context,
    onHelp: () -> Unit
) {
    val m = LocalMetrics.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = m.stageTitle.sp,
            lineHeight = m.lineHeight(m.stageTitle).sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 2,
            modifier = Modifier.weight(1f)
        )
        StageControls(accent, quiz, ctx, onHelp)
    }
}

/**
 * Audio y ayuda. Son controles SECUNDARIOS: área táctil cómoda, pero sin
 * competir con la pregunta. Van aparte de [StageBar] porque en horizontal se
 * reubican junto al progreso para ahorrar una fila entera.
 */
@Composable
private fun StageControls(
    accent: Color,
    quiz: Quiz,
    ctx: android.content.Context,
    onHelp: () -> Unit
) {
    val m = LocalMetrics.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        quiz.speech?.let { speech ->
            IconButton(
                onClick = { TtsManager.speak(ctx, speech, quiz.speechEnglish) },
                modifier = Modifier.size(m.minTouch.dp)
            ) {
                Icon(
                    Icons.Filled.VolumeUp,
                    contentDescription = "Escuchar la pregunta",
                    tint = accent,
                    modifier = Modifier.size((m.actionIcon * 0.8f).dp)
                )
            }
        }
        if (quiz.help != null) {
            IconButton(
                onClick = onHelp,
                modifier = Modifier.size(m.minTouch.dp)
            ) {
                Icon(
                    Icons.Filled.Lightbulb,
                    contentDescription = "Ver ejemplo de ayuda",
                    tint = accent,
                    modifier = Modifier.size((m.actionIcon * 0.8f).dp)
                )
            }
        }
    }
}

/**
 * Progreso de la etapa.
 *
 * Sustituye a la línea "Últimas 1/10 · Aciertos: 0/10 · Precisión: 0% (meta
 * 80%)", que era un reporte analítico compitiendo con la pregunta. El detalle
 * sigue existiendo —se sube a Firestore y se ve en el panel de padres—, que es
 * donde le sirve a un adulto. El niño solo necesita saber cuánto lleva.
 *
 * PRIMARY usa puntos: se entiende sin leer. SECONDARY usa fracción y barra,
 * que es más informativo y menos infantil.
 */
@Composable
private fun ProgressIndicator(hits: Int, total: Int, accent: Color) {
    val m = LocalMetrics.current
    val apagado = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)

    if (m.level.isPrimary) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(m.itemGap.dp / 2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(total) { i ->
                Box(
                    Modifier
                        .size((m.statusLine * 0.9f).dp)
                        .background(if (i < hits) accent else apagado, CircleShape)
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(m.itemGap.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$hits / $total",
                fontSize = m.statusLine.sp,
                lineHeight = m.lineHeight(m.statusLine).sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1
            )
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else hits.toFloat() / total },
                color = accent,
                trackColor = apagado,
                strokeCap = StrokeCap.Round,
                modifier = Modifier.weight(1f).height((m.statusLine * 0.35f).dp)
            )
            // La meta importa, pero como dato de apoyo, no como titular.
            Text(
                "meta $PASS_ACCURACY%",
                fontSize = (m.statusLine * 0.9f).sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun InstructionText(instruction: String?) {
    val m = LocalMetrics.current
    instruction?.let {
        Text(
            it,
            fontSize = m.instruction.sp,
            lineHeight = m.lineHeight(m.instruction).sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = m.itemGap.dp)
        )
    }
}

/**
 * Respuestas y retroalimentación. Es el contenido que PUEDE desplazarse.
 *
 * El botón de continuar vive aparte, en [NextButton], para que quede fuera del
 * área desplazable: tener que hacer scroll para pulsar "Siguiente" después de
 * cada respuesta es fricción en el camino que el niño recorre decenas de veces
 * al día.
 */
@Composable
private fun AnswerArea(
    quiz: Quiz,
    selected: String?,
    result: Boolean?,
    onAnswer: (String) -> Unit
) {
    val m = LocalMetrics.current
    val ctx = LocalContext.current

    Column(Modifier.fillMaxWidth()) {
        OptionsGrid(quiz.options, selected, result, quiz.answer, onAnswer)

        result?.let { ok ->
            Spacer(Modifier.size(m.itemGap.dp))
            // El tono también cambia con la edad: a un niño el acierto se le
            // celebra y el fallo se le suaviza ("casi"); a un adolescente se le
            // informa sin adornos, que es lo que espera de una herramienta.
            // El contenido educativo (quiz.afterLines) es el mismo en ambos.
            val header = when {
                ok && m.level.isPrimary -> "🎉 ¡Muy bien!"
                ok -> "✓ Correcto"
                m.level.isPrimary -> "Casi. La respuesta era ${quiz.answer}."
                else -> "La respuesta correcta era ${quiz.answer}."
            }
            FeedbackBox(ok, (listOf(header) + quiz.afterLines).joinToString("\n"))

            quiz.exampleSentence?.let { sentence ->
                Spacer(Modifier.size(m.itemGap.dp))
                OutlinedButton(
                    onClick = { TtsManager.speak(ctx, sentence, english = true) },
                    shape = RoundedCornerShape(m.corner.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = m.minTouch.dp)
                ) {
                    Icon(
                        Icons.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size((m.actionIcon * 0.6f).dp)
                    )
                    Spacer(Modifier.size(m.itemGap.dp))
                    Text("Escuchar en una oración", fontSize = m.buttonLabel.sp, maxLines = 1)
                }
            }
        }
    }
}

/** Acción de continuar. Siempre visible, nunca dentro de un área desplazable. */
@Composable
private fun NextButton(
    result: Boolean?,
    passed: Boolean,
    nextLabel: String,
    onNext: () -> Unit
) {
    val m = LocalMetrics.current
    if (result == null) return

    Spacer(Modifier.size(m.itemGap.dp))
    Button(
        onClick = onNext,
        shape = RoundedCornerShape(m.corner.dp),
        contentPadding = PaddingValues(vertical = m.buttonVPad.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = m.minTouch.dp)
    ) {
        Text(
            if (passed) nextLabel else "Siguiente pregunta",
            fontSize = m.buttonLabel.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun HelpDialog(help: Help, onDismiss: () -> Unit) {
    val m = LocalMetrics.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido", fontSize = m.buttonLabel.sp)
            }
        },
        title = {
            Text(
                help.title,
                fontSize = m.stageTitle.sp,
                lineHeight = m.lineHeight(m.stageTitle).sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            // El ejemplo puede ser largo: aquí el desplazamiento SÍ es correcto.
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    help.lines.joinToString("\n"),
                    fontSize = m.feedback.sp,
                    lineHeight = m.lineHeight(m.feedback).sp
                )
            }
        }
    )
}

// =====================================================================
//  TARJETA DE LA PREGUNTA (texto + dibujos adaptativos)
// =====================================================================

/** Una línea es "de dibujos" si no tiene letras ni dígitos (solo emojis/espacios). */
private fun isEmojiLine(s: String): Boolean =
    s.isNotBlank() && s.none { it.isLetterOrDigit() }

/**
 * Recurso drawable real para un emoji del vocabulario, si existe un archivo en
 * res/drawable-nodpi/ (ver ChallengeEngine.wordForEmoji). Se resuelve por
 * nombre en tiempo real: basta soltar el archivo en la carpeta para que se use.
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

/**
 * Pregunta y dibujos. El tamaño de los dibujos NO es fijo: se calcula con
 * [Adaptive.fitGridItem] a partir del espacio que queda después del texto, así
 * que la fila de objetos a contar siempre cabe completa, en cualquier pantalla.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionCard(
    text: String,
    modifier: Modifier = Modifier,
    /**
     * Si la tarjeta debe ocupar todo el alto que le den. Con dibujos sí: hacen
     * falta las dos dimensiones para calcular su tamaño. Con solo texto no,
     * porque estirarla deja una tarjeta con mucho aire muerto.
     */
    fill: Boolean = true
) {
    val m = LocalMetrics.current
    val density = LocalDensity.current

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(m.corner.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        BoxWithConstraints(
            (if (fill) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                .padding(m.cardPad.dp)
        ) {
            val availW = maxWidth.value.takeIf { it.isFinite() } ?: m.contentMaxWidth
            val availH = maxHeight.value.takeIf { it.isFinite() } ?: (m.heightDp * 0.4f)

            val lines = text.split("\n").filter { it.isNotBlank() }
            val drawLines = lines.filter { isEmojiLine(it) }
            val prose = lines.size - drawLines.size

            // Altura que se lleva el texto; el resto es para los dibujos.
            val proseH = prose * m.lineHeight(m.question) * 1.2f
            val perDrawH = if (drawLines.isEmpty()) 0f
            else ((availH - proseH) / drawLines.size).coerceAtLeast(m.countImageMin)

            Column(
                modifier = (if (fill) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                lines.forEach { line ->
                    if (!isEmojiLine(line)) {
                        Text(
                            line,
                            textAlign = TextAlign.Center,
                            fontSize = m.question.sp,
                            lineHeight = m.lineHeight(m.question).sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        return@forEach
                    }

                    val tokens = line.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                    val single = tokens.size <= 1
                    // Solo se usa imagen real si TODA la fila es el mismo emoji
                    // repetido (contar/vocabulario); con emojis mixtos (🔵🟠 de
                    // "qué suma muestra") se queda en emoji.
                    val imgRes = tokens.distinct().singleOrNull()?.let { imageResFor(it) }

                    val minSize = if (single) m.heroImageMin else m.countImageMin
                    val maxSize = if (single) m.heroImageMax else m.countImageMax
                    val itemDp = Adaptive.fitGridItem(
                        count = tokens.size,
                        maxW = availW,
                        maxH = perDrawH,
                        gap = m.itemGap,
                        minSize = minSize,
                        maxSize = maxSize
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.Center,
                        maxItemsInEachRow = Adaptive.columnsFor(itemDp, availW, m.itemGap)
                    ) {
                        tokens.forEach { token ->
                            if (imgRes != null) {
                                Image(
                                    painter = painterResource(imgRes),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(m.itemGap.dp / 2)
                                        .size(itemDp.dp)
                                )
                            } else {
                                // El emoji se mide en dp y se convierte a sp para
                                // que ocupe EXACTAMENTE la casilla calculada, sin
                                // que la escala de fuente del sistema lo desborde.
                                val emojiSp = with(density) { (itemDp * 0.78f).dp.toSp() }
                                Box(
                                    modifier = Modifier
                                        .padding(m.itemGap.dp / 2)
                                        .size(itemDp.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        token,
                                        fontSize = emojiSp,
                                        lineHeight = emojiSp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
//  RESPUESTAS
// =====================================================================

/**
 * Rejilla de respuestas. El número de columnas se decide por el ancho REAL
 * disponible y por la longitud del texto: dos columnas en cuanto quepan, una
 * sola cuando las opciones son frases largas o el panel es angosto.
 */
@Composable
private fun OptionsGrid(
    options: List<String>,
    selected: String?,
    correctFlag: Boolean?,
    /** Cuál es la correcta, para poder resaltarla cuando el alumno falla. */
    answer: String,
    onClick: (String) -> Unit
) {
    val m = LocalMetrics.current

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val availW = maxWidth.value.takeIf { it.isFinite() } ?: m.contentMaxWidth
        val longest = options.maxOfOrNull { it.length } ?: 0
        // Una columna si el panel es angosto o si las opciones son frases:
        // partirlas en dos columnas obligaría a cortar palabras.
        val columns = if (availW < 380f || longest > 22) 1 else 2

        Column(verticalArrangement = Arrangement.spacedBy(m.itemGap.dp)) {
            options.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(m.itemGap.dp)
                ) {
                    row.forEach { opt ->
                        AnswerButton(
                            text = opt,
                            state = when {
                                selected == opt && correctFlag == true -> AnswerState.CHOSEN_RIGHT
                                selected == opt && correctFlag == false -> AnswerState.CHOSEN_WRONG
                                // Al fallar se resalta CUÁL era la correcta: ver
                                // la respuesta buena junto a la propia enseña
                                // más que solo saber que uno se equivocó.
                                correctFlag == false && opt == answer -> AnswerState.REVEALED
                                else -> AnswerState.IDLE
                            },
                            onClick = { onClick(opt) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Rellena el hueco si la última fila va incompleta.
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** En qué situación está un botón de respuesta. */
private enum class AnswerState {
    /** Sin responder todavía, o no elegida. */
    IDLE,
    /** La eligió y era correcta. */
    CHOSEN_RIGHT,
    /** La eligió y era incorrecta. */
    CHOSEN_WRONG,
    /** No la eligió, pero era la correcta: se resalta para que la vea. */
    REVEALED
}

/**
 * Botón de respuesta. Encapsula estado visual, forma, área táctil y contenido
 * (imagen, dibujo o texto), para que añadir un tipo de ejercicio nuevo no
 * obligue a repetir toda esta lógica.
 *
 * El contenido se adapta solo: si hay imagen real para esa palabra la usa, si
 * no cae al emoji, y si es texto lo escribe. Ver [OptionContent].
 */
@Composable
private fun AnswerButton(
    text: String,
    state: AnswerState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val m = LocalMetrics.current
    val esquema = MaterialTheme.colorScheme

    val container = when (state) {
        AnswerState.CHOSEN_RIGHT -> esquema.secondary
        AnswerState.CHOSEN_WRONG -> esquema.error
        // La correcta no elegida se marca en verde translúcido: se distingue de
        // la que sí se pulsó, sin gritar tanto como un acierto propio.
        AnswerState.REVEALED -> esquema.secondary.copy(alpha = 0.35f)
        AnswerState.IDLE -> esquema.surface
    }

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(m.corner.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container),
        contentPadding = PaddingValues(vertical = m.optionVPad.dp, horizontal = m.itemGap.dp),
        // answerMinHeight crece con la altura disponible: en tablet horizontal
        // los botones se estiran hasta ocupar el hueco que antes quedaba vacío.
        modifier = modifier.heightIn(min = m.answerMinHeight.dp)
    ) {
        OptionContent(text)
    }
}

@Composable
private fun OptionContent(opt: String) {
    val m = LocalMetrics.current
    val density = LocalDensity.current
    val imgRes = if (isEmojiLine(opt)) imageResFor(opt) else null
    // Los dibujos de respuesta se limitan a la mitad del hero para no competir
    // con la pregunta ni empujar los botones fuera de la pantalla.
    val pic = (m.heroImageMax * 0.45f).coerceAtLeast(m.countImageMin)

    when {
        imgRes != null -> Image(
            painter = painterResource(imgRes),
            contentDescription = null,
            modifier = Modifier.size(pic.dp)
        )
        isEmojiLine(opt) -> Text(
            opt,
            fontSize = with(density) { (pic * 0.8f).dp.toSp() },
            lineHeight = with(density) { (pic * 0.9f).dp.toSp() }
        )
        else -> Text(
            opt,
            fontSize = m.option.sp,
            lineHeight = m.lineHeight(m.option).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FeedbackBox(ok: Boolean, message: String) {
    val m = LocalMetrics.current
    Card(
        shape = RoundedCornerShape(m.corner.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ok) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
        )
    ) {
        // Acotada: el procedimiento paso a paso puede ser largo y, sin tope,
        // empujaría al dibujo y al botón fuera de la pantalla. Si no cabe, se
        // desplaza SOLO la explicación, no la pantalla entera.
        Text(
            message,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = (m.heightDp * 0.34f).dp)
                .verticalScroll(rememberScrollState())
                .padding(m.cardPad.dp),
            fontSize = m.feedback.sp,
            lineHeight = m.lineHeight(m.feedback).sp
        )
    }
}

// =====================================================================
//  COMPRENSIÓN LECTORA
// =====================================================================

@Composable
private fun ReadingStage(advanced: Boolean, onApproved: () -> Unit) {
    val m = LocalMetrics.current
    val ctx = LocalContext.current
    val passage by remember { mutableStateOf<ReadingPassage>(ChallengeEngine.randomReading(advanced)) }
    var summary by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<SummaryResult?>(null) }
    var submitCount by remember { mutableStateOf(0) }
    var evaluating by remember { mutableStateOf(false) }
    var evaluatedByAi by remember { mutableStateOf(false) }

    val passed = (result?.score ?: 0) >= PASS_ACCURACY
    val words = summary.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Comprensión lectora · meta $PASS_ACCURACY/100",
                fontSize = m.stageTitle.sp,
                lineHeight = m.lineHeight(m.stageTitle).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { TtsManager.speak(ctx, "${passage.title}. ${passage.text}") },
                modifier = Modifier.size(m.minTouch.dp)
            ) {
                Icon(
                    Icons.Filled.VolumeUp,
                    contentDescription = "Escuchar la lectura",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size((m.actionIcon * 0.8f).dp)
                )
            }
        }
        Spacer(Modifier.size(m.itemGap.dp))

        // La lectura sí puede ser larga: aquí el desplazamiento es legítimo, y
        // se limita a la lectura para que el campo de texto y el botón de
        // enviar queden SIEMPRE visibles.
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(m.corner.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                Modifier
                    .padding(m.cardPad.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    passage.title,
                    fontSize = m.instruction.sp,
                    lineHeight = m.lineHeight(m.instruction).sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.size(m.itemGap.dp))
                Text(
                    passage.text,
                    fontSize = m.feedback.sp,
                    lineHeight = m.lineHeight(m.feedback).sp
                )
            }
        }
        Spacer(Modifier.size(m.sectionGap.dp))

        OutlinedTextField(
            value = summary,
            onValueChange = { if (!passed) summary = it },
            label = { Text("Escribe tu resumen ($words palabras)", fontSize = m.statusLine.sp) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = m.feedback.sp),
            shape = RoundedCornerShape(m.corner.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = (m.minTouch * 2f).dp, max = (m.heightDp * 0.28f).dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        if (evaluating) {
            Spacer(Modifier.size(m.itemGap.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size((m.actionIcon * 0.6f).dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.size(m.itemGap.dp))
                Text("IA evaluando tu resumen…", fontSize = m.statusLine.sp)
            }
        }

        result?.let { r ->
            Spacer(Modifier.size(m.itemGap.dp))
            val badge = if (evaluatedByAi) "🤖 Evaluado por IA" else "📋 Evaluado localmente (sin conexión)"
            FeedbackBox(passed, "$badge\n\n${r.feedback}\n\n💡 ${r.suggestions}  ·  Puntaje: ${r.score}/100")
        }

        Spacer(Modifier.size(m.sectionGap.dp))
        if (passed) {
            Button(
                onClick = onApproved,
                shape = RoundedCornerShape(m.corner.dp),
                contentPadding = PaddingValues(vertical = m.buttonVPad.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = m.minTouch.dp)
            ) { Text("¡Desbloquear tablet!", fontSize = m.buttonLabel.sp) }
        } else {
            Button(
                onClick = {
                    submitCount++
                    evaluating = true
                    // Intenta evaluar con IA (Gemini); si falla, no hay internet,
                    // o no hay API key, degrada a la heurística local (misma
                    // política fail-safe: nunca aprueba a ciegas).
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
                        ProgressSync.reportReading(ctx, result!!.score, submitCount)
                    }
                },
                enabled = summary.trim().length >= 15 && !evaluating,
                shape = RoundedCornerShape(m.corner.dp),
                contentPadding = PaddingValues(vertical = m.buttonVPad.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = m.minTouch.dp)
            ) { Text("Enviar resumen", fontSize = m.buttonLabel.sp) }
        }
    }
}
