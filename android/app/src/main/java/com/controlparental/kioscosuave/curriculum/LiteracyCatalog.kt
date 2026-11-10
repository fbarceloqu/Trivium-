package com.controlparental.kioscosuave.curriculum

import com.controlparental.kioscosuave.GradeLevel

/** Reviewed offline starter bank. IDs are persisted and must not be renamed. */
object LiteracyCatalog {
    const val TWO = "lit.syllables.two"
    const val THREE = "lit.syllables.three"
    const val MATCH = "lit.words.match"
    const val WHERE = "lit.sentences.where"
    const val ACTION = "lit.sentences.action"
    const val DETAIL = "lit.stories.detail"
    const val SEQUENCE = "lit.stories.sequence"

    private fun skill(id: String, topic: String, label: String, format: ExerciseFormat, vararg prerequisites: String) =
        Skill(id, Subject.READING, GradeLevel.PRIMARIA, topic, label, listOf(format), prerequisites.toList())

    val skills = listOf(
        skill(TWO, "Sílabas", "Formar palabras de dos sílabas", ExerciseFormat.DIRECTO),
        skill(THREE, "Sílabas", "Formar palabras de tres sílabas", ExerciseFormat.DIRECTO, TWO),
        skill(MATCH, "Palabras", "Distinguir palabras parecidas con una imagen", ExerciseFormat.DIRECTO, THREE),
        skill(WHERE, "Oraciones", "Encontrar el lugar en una oración", ExerciseFormat.CONTEXTO, MATCH),
        skill(ACTION, "Oraciones", "Identificar la acción en una oración", ExerciseFormat.CONTEXTO, MATCH),
        skill(DETAIL, "Cuentos", "Identificar personajes, acciones y lugares", ExerciseFormat.RAZONAMIENTO, WHERE, ACTION),
        skill(SEQUENCE, "Cuentos", "Reconocer el orden de los acontecimientos", ExerciseFormat.RAZONAMIENTO, DETAIL)
    )

    fun forGrade(grade: GradeLevel): List<Skill> =
        if (grade == GradeLevel.SECUNDARIA) emptyList() else skills.map { it.copy(grade = grade) }

    /**
     * [extra] son las sílabas distractoras: misma consonante con otra vocal o
     * consonante parecida, que es el error típico al empezar a leer. Se evitan
     * a propósito las que formarían un homófono de la palabra (BA-CA por VACA):
     * a esta edad eso es ortografía, no lectura, y contaría un fallo injusto.
     */
    private data class Word(val id: String, val parts: List<String>, val extra: List<String>, val image: String, val emoji: String, val similar: List<String> = emptyList()) {
        val text get() = parts.joinToString("")
    }
    private val words = listOf(
        Word("casa", listOf("CA", "SA"), listOf("CO", "SE", "MA"), "house", "🏠", listOf("CAMA", "CAPA", "MASA")),
        Word("pato", listOf("PA", "TO"), listOf("PO", "TA", "DO"), "duck", "🦆", listOf("GATO", "PALO", "DATO")),
        Word("gato", listOf("GA", "TO"), listOf("GO", "TA", "CA"), "cat", "🐱", listOf("PATO", "GAJO", "GOTA")),
        Word("vaca", listOf("VA", "CA"), listOf("VE", "CO", "PA"), "cow", "🐄", listOf("VARA", "BATA", "VAGA")),
        Word("lobo", listOf("LO", "BO"), listOf("LA", "BA", "DO"), "wolf", "🐺", listOf("LODO", "LORO", "LOCO")),
        Word("rana", listOf("RA", "NA"), listOf("RO", "NO", "MA"), "frog", "🐸", listOf("RAMA", "RATA", "LANA")),
        Word("oso", listOf("O", "SO"), listOf("A", "SA", "SE"), "bear", "🐻", listOf("ORO", "OJO", "ASA")),
        Word("mono", listOf("MO", "NO"), listOf("MA", "NA", "NE"), "monkey", "🐒", listOf("MANO", "MOÑO", "CONO")),
        Word("perro", listOf("PE", "RRO"), listOf("PA", "RRA", "TO"), "dog", "🐶", listOf("PERO", "PELO", "CERRO")),
        Word("luna", listOf("LU", "NA"), listOf("LA", "NO", "MA"), "moon", "🌙", listOf("LANA", "LUPA", "CUNA")),
        Word("raton", listOf("RA", "TÓN"), listOf("RO", "TAN", "MA"), "mouse", "🐭", listOf("RATA", "BOTÓN", "RAZÓN")),
        Word("pelota", listOf("PE", "LO", "TA"), listOf("PA", "LA", "TO"), "ball", "⚽", listOf("PALETA", "PILOTO", "PELUSA")),
        Word("conejo", listOf("CO", "NE", "JO"), listOf("CA", "NA", "JA"), "rabbit", "🐰"),
        Word("caballo", listOf("CA", "BA", "LLO"), listOf("CO", "LLA", "PA"), "horse", "🐴"),
        Word("gallina", listOf("GA", "LLI", "NA"), listOf("GO", "LLA", "NO"), "chicken", "🐔", listOf("GALLETA", "GALLITO", "COLINA")),
        Word("tortuga", listOf("TOR", "TU", "GA"), listOf("TA", "GO", "TAR"), "turtle", "🐢"),
        Word("camello", listOf("CA", "ME", "LLO"), listOf("MA", "LLA", "CO"), "camel", "🐪", listOf("CABELLO", "CAMINO", "CASTILLO")),
        Word("oveja", listOf("O", "VE", "JA"), listOf("A", "VA", "JO"), "sheep", "🐑"),
        Word("jirafa", listOf("JI", "RA", "FA"), listOf("JA", "RO", "FO"), "giraffe", "🦒"),
        Word("abeja", listOf("A", "BE", "JA"), listOf("O", "BA", "JO"), "bee", "🐝")
    )

    private fun wordActivities(): List<LiteracyActivity> = words.flatMap { w ->
        val build = LiteracyActivity("build.${w.id}", if (w.parts.size == 2) TWO else THREE,
            LiteracyKind.SYLLABLES, "Toca las sílabas en orden para formar la palabra.", w.parts + w.extra, w.text,
            "${w.parts.joinToString(" + ")} forma ${w.text.lowercase()}.", w.image, w.emoji, syllables = w.parts)
        if (w.similar.isEmpty()) listOf(build) else listOf(build,
            LiteracyActivity("match.${w.id}", MATCH, LiteracyKind.WORD_IMAGE, "¿Qué palabra corresponde al dibujo?",
                listOf(w.text) + w.similar, w.text, "La palabra del dibujo es ${w.text.lowercase()}.", w.image, w.emoji))
    }

    private fun sentence(id: String, skill: String, text: String, question: String, answer: String, wrong: List<String>, image: String, emoji: String) =
        LiteracyActivity("sentence.$id", skill, LiteracyKind.SENTENCE, question, listOf(answer) + wrong, answer,
            "El texto dice: «$text»", image, emoji, text, text)

    private val sentences = listOf(
        sentence("cat.where", WHERE, "El gato duerme en el sofá.", "¿Dónde duerme el gato?", "en el sofá", listOf("en la cama", "en la silla", "en la alfombra"), "cat", "🐱"),
        sentence("duck.where", WHERE, "El pato nada en el lago.", "¿Dónde nada el pato?", "en el lago", listOf("en el río", "en el estanque", "en la piscina"), "duck", "🦆"),
        sentence("rabbit.where", WHERE, "El conejo descansa bajo el árbol.", "¿Dónde descansa el conejo?", "bajo el árbol", listOf("junto al muro", "dentro de la caja", "entre las flores"), "rabbit", "🐰"),
        sentence("dog.where", WHERE, "El perro juega en el jardín.", "¿Dónde juega el perro?", "en el jardín", listOf("en el patio", "en el parque", "en la sala"), "dog", "🐶"),
        sentence("bird.where", WHERE, "El pájaro canta en la ventana.", "¿Dónde canta el pájaro?", "en la ventana", listOf("en la rama", "en el techo", "en el nido"), "bird", "🐦"),
        sentence("horse.where", WHERE, "El caballo corre por el campo.", "¿Por dónde corre el caballo?", "por el campo", listOf("por el camino", "por la playa", "por el bosque"), "horse", "🐴"),
        sentence("cat.action", ACTION, "El gato busca su pelota.", "¿Qué hace el gato?", "busca su pelota", listOf("mira su plato", "bebe su leche", "sube al sofá"), "cat", "🐱"),
        sentence("rabbit.action", ACTION, "El conejo come una zanahoria.", "¿Qué hace el conejo?", "come una zanahoria", listOf("muerde una hoja", "busca una flor", "bebe un poco de agua"), "rabbit", "🐰"),
        sentence("bear.action", ACTION, "El oso recoge unas moras.", "¿Qué hace el oso?", "recoge unas moras", listOf("busca unas nueces", "mira unas flores", "come unas manzanas"), "bear", "🐻"),
        sentence("girl.action", ACTION, "Ana guarda la pelota.", "¿Qué hace Ana?", "guarda la pelota", listOf("lanza la pelota", "busca la pelota", "limpia la pelota"), "ball", "⚽"),
        sentence("turtle.action", ACTION, "La tortuga camina hacia el agua.", "¿Qué hace la tortuga?", "camina hacia el agua", listOf("descansa bajo una hoja", "come una hoja verde", "sale de su refugio"), "turtle", "🐢"),
        sentence("monkey.action", ACTION, "El mono pela un plátano.", "¿Qué hace el mono?", "pela un plátano", listOf("busca un plátano", "guarda un plátano", "comparte un plátano"), "monkey", "🐒")
    )

    private data class StoryQuestion(val question: String, val answer: String, val wrong: List<String>, val evidence: String, val sequence: Boolean = false)
    private fun story(id: String, text: String, image: String, emoji: String, vararg questions: StoryQuestion) = questions.mapIndexed { i, q ->
        LiteracyActivity("story.$id.$i", if (q.sequence) SEQUENCE else DETAIL, LiteracyKind.STORY, q.question,
            listOf(q.answer) + q.wrong, q.answer, "La pista está aquí: «${q.evidence}»", image, emoji, text, q.evidence)
    }
    private val stories = listOf(
        story("ball", "Ana llevó una pelota al parque. La pelota rodó detrás de un banco. Leo la encontró y se la devolvió. Al final, Ana y Leo jugaron juntos.", "ball", "⚽",
            StoryQuestion("¿Quién llevó la pelota al parque?", "Ana", listOf("Leo", "Lola", "Pablo"), "Ana llevó una pelota al parque."),
            StoryQuestion("¿Dónde quedó la pelota?", "detrás de un banco", listOf("junto a un árbol", "debajo de una mesa", "dentro de una caja"), "La pelota rodó detrás de un banco."),
            StoryQuestion("¿Qué hicieron al final?", "jugaron juntos", listOf("buscaron la pelota", "volvieron a casa", "merendaron juntos"), "Al final, Ana y Leo jugaron juntos.", true)),
        story("rabbit", "Un conejo encontró una semilla. La puso en una maceta. Cada mañana regó la tierra. Después de varios días, apareció una planta.", "rabbit", "🐰",
            StoryQuestion("¿Qué encontró el conejo?", "una semilla", listOf("una hoja", "una piedra", "una flor"), "Un conejo encontró una semilla."),
            StoryQuestion("¿Dónde puso la semilla?", "en una maceta", listOf("en una caja", "en una bolsa", "en una cesta"), "La puso en una maceta."),
            StoryQuestion("¿Qué apareció después de varios días?", "una planta", listOf("una flor", "un fruto", "una raíz"), "Después de varios días, apareció una planta.", true)),
        story("house", "Lola visitó a su abuela. Juntas prepararon pan en la cocina. Mientras el pan se horneaba, pusieron la mesa. Luego compartieron el pan con la familia.", "house", "🏠",
            StoryQuestion("¿A quién visitó Lola?", "a su abuela", listOf("a su tía", "a su amiga", "a su hermana"), "Lola visitó a su abuela."),
            StoryQuestion("¿Dónde prepararon el pan?", "en la cocina", listOf("en el patio", "en el comedor", "en la terraza"), "Juntas prepararon pan en la cocina."),
            StoryQuestion("¿Qué hicieron mientras se horneaba el pan?", "pusieron la mesa", listOf("prepararon la masa", "compartieron el pan", "guardaron la harina"), "Mientras el pan se horneaba, pusieron la mesa.", true)),
        story("bird", "Pablo vio un pájaro junto a la ventana. Su plato de agua estaba vacío. Pablo llenó el plato y lo dejó en el patio. El pájaro bajó a beber.", "bird", "🐦",
            StoryQuestion("¿Quién llenó el plato?", "Pablo", listOf("Pedro", "Leo", "Ana"), "Pablo llenó el plato y lo dejó en el patio."),
            StoryQuestion("¿Dónde dejó Pablo el plato?", "en el patio", listOf("en la ventana", "en la mesa", "en el jardín"), "Pablo llenó el plato y lo dejó en el patio."),
            StoryQuestion("¿Qué hizo el pájaro después?", "bajó a beber", listOf("voló al árbol", "volvió al nido", "empezó a cantar"), "El pájaro bajó a beber.", true)),
        story("turtle", "La tortuga salió a caminar. En el camino encontró una rama que le impedía pasar. Un conejo movió la rama. La tortuga pudo continuar hasta el lago.", "turtle", "🐢",
            StoryQuestion("¿Quién movió la rama?", "Un conejo", listOf("La tortuga", "Un pato", "Un ratón"), "Un conejo movió la rama."),
            StoryQuestion("¿Hasta dónde continuó la tortuga?", "hasta el lago", listOf("hasta el río", "hasta el árbol", "hasta la casa"), "La tortuga pudo continuar hasta el lago."),
            StoryQuestion("¿Qué encontró antes de recibir ayuda?", "una rama", listOf("una piedra", "una hoja", "una caja"), "En el camino encontró una rama que le impedía pasar.", true)),
        story("cat", "El gato de Sara se escondió. Sara lo buscó en la sala y en el patio. Al mover una manta, encontró al gato dormido en una silla. Sara lo dejó descansar.", "cat", "🐱",
            StoryQuestion("¿Quién buscó al gato?", "Sara", listOf("Lola", "Ana", "Leo"), "Sara lo buscó en la sala y en el patio."),
            StoryQuestion("¿Dónde estaba dormido el gato?", "en una silla", listOf("en un sofá", "en una cama", "en una caja"), "Al mover una manta, encontró al gato dormido en una silla."),
            StoryQuestion("¿Qué hizo Sara al final?", "lo dejó descansar", listOf("lo llevó al patio", "le sirvió comida", "lo llamó a jugar"), "Sara lo dejó descansar.", true))
    ).flatten()

    val activities: List<LiteracyActivity> = wordActivities() + sentences + stories
}
