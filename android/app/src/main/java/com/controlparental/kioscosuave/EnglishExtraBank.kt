package com.controlparental.kioscosuave

/**
 * Ejercicios adicionales para las unidades BÁSICAS de inglés.
 *
 * Cada unidad básica tenía solo 5 ejercicios y la etapa pide 10 respuestas, de
 * modo que el alumno veía cada pregunta dos veces el mismo día y podía aprobar
 * de memoria. Con estos, cada unidad tiene al menos 10.
 *
 * Se añaden por título de unidad ([ChallengeEngine] los une a su banco), sin
 * tocar los ejercicios originales. Mismas reglas que el resto: una sola
 * respuesta correcta y distractores de la misma categoría que la respuesta.
 */
internal object EnglishExtraBank {

    private fun ex(
        instruction: String, question: String, answer: String,
        w1: String, w2: String, w3: String, explanation: String, phonetic: String
    ) = EnglishExercise(instruction, question, listOf(answer, w1, w2, w3), answer, explanation, phonetic)

    private val byUnit: Map<String, List<EnglishExercise>> = mapOf(
        "Viaje al pasado (Past Simple)" to listOf(
            ex("Completa con el pasado de 'walk'.", "We ______ to the park after lunch.",
                "walked", "walk", "walking", "walks",
                "'walk' es regular: se le agrega -ed → 'walked'.", "walked = /wɔːkt/ («uókt»)"),
            ex("Completa con el pasado de 'visit'.", "Last summer, they ______ their grandparents.",
                "visited", "visit", "visits", "visiting",
                "'visit' es regular: 'visited'. 'Last summer' indica pasado.", "visited = /ˈvɪzɪtɪd/ («vísited»)"),
            ex("Completa la negación en pasado.", "She ______ go to the party last night.",
                "didn't", "doesn't", "isn't", "won't",
                "En pasado se niega con 'didn't' + verbo en su forma base.", "didn't = /ˈdɪdənt/ («dídent»)"),
            ex("Completa la pregunta en pasado.", "______ you finish your homework yesterday?",
                "Did", "Do", "Does", "Will",
                "Las preguntas en pasado empiezan con 'Did'.", "did = /dɪd/ («did»)"),
            ex("Elige la forma correcta después de 'didn't'.", "He didn't ______ the answer.",
                "know", "knew", "knows", "knowing",
                "Después de 'didn't' el verbo va en su forma base, sin cambiar.", "know = /noʊ/ («nóu»)"),
            ex("Completa con el pasado de 'be'.", "The children ______ very happy yesterday.",
                "were", "was", "are", "is",
                "'were' es el pasado de 'be' para plural (the children = they).", "were = /wɜːr/ («uer»)")
        ),
        "El futuro (will / going to)" to listOf(
            ex("Completa con 'going to'.", "Look at those clouds! It is ______ to rain.",
                "going", "go", "went", "goes",
                "Con una señal clara de lo que viene se usa 'be going to'.", "going = /ˈɡoʊɪŋ/ («góuing»)"),
            ex("Completa la negación en futuro.", "Don't worry, I ______ tell anyone.",
                "won't", "don't", "didn't", "wasn't",
                "'won't' es la forma corta de 'will not'.", "won't = /woʊnt/ («uóunt»)"),
            ex("Completa la pregunta en futuro.", "______ you help me with this box?",
                "Will", "Did", "Are", "Does",
                "Las preguntas con 'will' empiezan con 'Will' + sujeto + verbo.", "will = /wɪl/ («uil»)"),
            ex("Elige el verbo 'be' correcto.", "We ______ going to watch a movie tonight.",
                "are", "is", "am", "be",
                "Con 'we' el verbo 'be' es 'are': 'We are going to…'.", "are = /ɑːr/ («ar»)"),
            ex("Elige la forma correcta después de 'will'.", "My sister will ______ ten next month.",
                "be", "is", "being", "was",
                "Después de 'will' el verbo va en su forma base: 'will be'.", "be = /biː/ («bi»)"),
            ex("Elige la forma corta de 'I will'.", "______ call you when I arrive.",
                "I'll", "I'm", "I've", "I'd",
                "'I'll' es la contracción de 'I will'.", "I'll = /aɪl/ («áil»)")
        ),
        "Verbos irregulares" to listOf(
            ex("¿Cuál es el pasado de 'write' (escribir)?", "She ______ a letter to her friend.",
                "wrote", "writed", "writes", "writing",
                "'write' es irregular: su pasado es 'wrote'.", "wrote = /roʊt/ («róut»)"),
            ex("¿Cuál es el pasado de 'give' (dar)?", "My uncle ______ me a book for my birthday.",
                "gave", "gived", "gives", "giving",
                "'give' es irregular: su pasado es 'gave'.", "gave = /ɡeɪv/ («guéiv»)"),
            ex("¿Cuál es el pasado de 'find' (encontrar)?", "I ______ my keys under the sofa.",
                "found", "finded", "finds", "finding",
                "'find' es irregular: su pasado es 'found'.", "found = /faʊnd/ («fáund»)"),
            ex("¿Cuál es el pasado de 'sleep' (dormir)?", "The baby ______ all night.",
                "slept", "sleeped", "sleeps", "sleeping",
                "'sleep' es irregular: su pasado es 'slept'.", "slept = /slɛpt/ («slépt»)"),
            ex("¿Cuál es el pasado de 'tell' (decir, contar)?", "Grandpa ______ us a funny story.",
                "told", "telled", "tells", "telling",
                "'tell' es irregular: su pasado es 'told'.", "told = /toʊld/ («tóuld»)"),
            ex("¿Cuál es el pasado de 'lose' (perder)?", "Our team ______ the game last Friday.",
                "lost", "losed", "loses", "losing",
                "'lose' es irregular: su pasado es 'lost'.", "lost = /lɔːst/ («lost»)")
        ),
        "El presente (Present Simple)" to listOf(
            ex("Completa la negación en presente.", "He ______ like spicy food.",
                "doesn't", "don't", "isn't", "didn't",
                "Con he/she/it se niega con 'doesn't' + verbo base.", "doesn't = /ˈdʌzənt/ («dósent»)"),
            ex("Completa en presente (tercera persona).", "My dad ______ TV in the evening.",
                "watches", "watch", "watching", "watched",
                "Con he/she/it, los verbos que terminan en -ch llevan -es.", "watches = /ˈwɒtʃɪz/ («uótchis»)"),
            ex("Completa la pregunta.", "______ your sister speak English?",
                "Does", "Do", "Is", "Are",
                "Con he/she/it las preguntas empiezan con 'Does'.", "does = /dʌz/ («dos»)"),
            ex("Completa la negación en presente.", "We ______ have classes on Saturday.",
                "don't", "doesn't", "aren't", "isn't",
                "Con I/you/we/they se niega con 'don't' + verbo base.", "don't = /doʊnt/ («dóunt»)"),
            ex("Completa en presente (tercera persona).", "Ana ______ math every afternoon.",
                "studies", "study", "studys", "studying",
                "Si el verbo termina en consonante + y: cambia a -ies.", "studies = /ˈstʌdiz/ («stódis»)"),
            ex("Elige la forma correcta después de 'does'.", "Does he ______ near the school?",
                "live", "lives", "living", "lived",
                "Después de 'does' el verbo va sin -s: 'Does he live…?'", "live = /lɪv/ («liv»)")
        ),
        "Un día de escuela (vocabulario)" to listOf(
            ex("¿Cómo se dice 'diccionario' en inglés?", "I look up new words in the ______.",
                "dictionary", "calendar", "magazine", "newspaper",
                "'dictionary' = diccionario.", "dictionary = /ˈdɪkʃənɛri/ («díkshoneri»)"),
            ex("¿Cómo se dice 'uniforme' en inglés?", "We wear a ______ to school.",
                "uniform", "costume", "pajamas", "swimsuit",
                "'uniform' = uniforme.", "uniform = /ˈjuːnɪfɔːrm/ («iúniform»)"),
            ex("¿Cómo se dice 'calculadora' en inglés?", "I use a ______ to check my math.",
                "calculator", "computer", "telephone", "television",
                "'calculator' = calculadora.", "calculator = /ˈkælkjəleɪtər/ («kálkiuleiter»)"),
            ex("¿Cómo se dice 'tijeras' en inglés?", "Cut the paper with the ______.",
                "scissors", "brushes", "crayons", "markers",
                "'scissors' = tijeras.", "scissors = /ˈsɪzərz/ («sísers»)"),
            ex("¿Cómo se dice 'materia' (de la escuela) en inglés?", "Science is my favorite ______.",
                "subject", "teacher", "building", "holiday",
                "'subject' = materia o asignatura.", "subject = /ˈsʌbdʒɪkt/ («sóbyekt»)"),
            ex("¿Cómo se dice 'examen' en inglés?", "I studied a lot for the ______.",
                "exam", "break", "trip", "game",
                "'exam' = examen.", "exam = /ɪɡˈzæm/ («igsám»)")
        ),
        "De viaje (vocabulario)" to listOf(
            ex("¿Cómo se dice 'turista' en inglés?", "A ______ visits new places on vacation.",
                "tourist", "pilot", "waiter", "farmer",
                "'tourist' = turista.", "tourist = /ˈtʊrɪst/ («túrist»)"),
            ex("¿Cómo se dice 'tienda de campaña' en inglés?", "We slept in a ______ in the forest.",
                "tent", "boat", "cave", "tree",
                "'tent' = tienda de campaña.", "tent = /tɛnt/ («tent»)"),
            ex("¿Cómo se dice 'bloqueador solar' en inglés?", "Put on ______ before you go out in the sun.",
                "sunscreen", "sunglasses", "raincoat", "umbrella",
                "'sunscreen' = bloqueador solar.", "sunscreen = /ˈsʌnskriːn/ («sónskrin»)"),
            ex("¿Cómo se dice 'isla' en inglés?", "An ______ has water all around it.",
                "island", "airport", "avenue", "office",
                "'island' = isla. La 's' no se pronuncia.", "island = /ˈaɪlənd/ («áiland»)"),
            ex("¿Cómo se dice 'postal' en inglés?", "I sent a ______ to my grandparents.",
                "postcard", "suitcase", "notebook", "backpack",
                "'postcard' = tarjeta postal.", "postcard = /ˈpoʊstkɑːrd/ («póustkard»)"),
            ex("¿Cómo se dice 'metro' (transporte) en inglés?", "We took the ______ under the city.",
                "subway", "bicycle", "airplane", "balloon",
                "'subway' = metro (tren subterráneo).", "subway = /ˈsʌbweɪ/ («sóbuei»)")
        ),
        "La familia (vocabulario)" to listOf(
            ex("¿Cómo se dice 'hermana' en inglés?", "My ______ is two years older than me.",
                "sister", "mother", "teacher", "neighbor",
                "'sister' = hermana.", "sister = /ˈsɪstər/ («síster»)"),
            ex("¿Cómo se dice 'tío' en inglés?", "My ______ is my dad's brother.",
                "uncle", "cousin", "nephew", "grandpa",
                "'uncle' = tío: el hermano de tu papá o de tu mamá.", "uncle = /ˈʌŋkəl/ («ónkol»)"),
            ex("¿Cómo se dice 'abuelos' en inglés?", "My ______ are my mom's parents.",
                "grandparents", "classmates", "neighbors", "brothers",
                "'grandparents' = abuelos (abuelo y abuela).", "grandparents = /ˈɡrænpɛrənts/ («gránperents»)"),
            ex("¿Cómo se dice 'gemelos' en inglés?", "The ______ were born on the same day.",
                "twins", "uncles", "babies", "adults",
                "'twins' = gemelos.", "twins = /twɪnz/ («tuíns»)"),
            ex("¿Cómo se dice 'bebé' en inglés?", "The ______ is sleeping in her crib.",
                "baby", "aunt", "lady", "bride",
                "'baby' = bebé.", "baby = /ˈbeɪbi/ («béibi»)")
        ),
        "La comida (vocabulario)" to listOf(
            ex("¿Cómo se dice 'mantequilla' en inglés?", "I put ______ on my toast.",
                "butter", "pepper", "flour", "honey",
                "'butter' = mantequilla.", "butter = /ˈbʌtər/ («bóter»)"),
            ex("¿Cómo se dice 'azúcar' en inglés?", "Do you want ______ in your coffee?",
                "sugar", "salt", "oil", "rice",
                "'sugar' = azúcar.", "sugar = /ˈʃʊɡər/ («shúguer»)"),
            ex("¿Cómo se dice 'tener hambre' en inglés?", "I didn't eat today, so I am very ______.",
                "hungry", "thirsty", "sleepy", "angry",
                "'hungry' = con hambre. 'thirsty' es con sed.", "hungry = /ˈhʌŋɡri/ («jóngri»)"),
            ex("¿Cómo se dice 'tener sed' en inglés?", "It is hot. I am ______ and I need a drink.",
                "thirsty", "hungry", "tired", "happy",
                "'thirsty' = con sed. 'hungry' es con hambre.", "thirsty = /ˈθɜːrsti/ («zérsti»)"),
            ex("¿Cómo se dice 'postre' en inglés?", "We had ice cream for ______.",
                "dessert", "desert", "dinner", "diner",
                "'dessert' (doble s) = postre. 'desert' es desierto.", "dessert = /dɪˈzɜːrt/ («disért»)"),
            ex("¿Cómo se dice 'cuchara' en inglés?", "I eat my cereal with a ______.",
                "spoon", "fork", "knife", "plate",
                "'spoon' = cuchara.", "spoon = /spuːn/ («spún»)")
        )
    )

    fun forUnit(title: String): List<EnglishExercise> = byUnit[title].orEmpty()

    val unitTitles: Set<String> get() = byUnit.keys
}
