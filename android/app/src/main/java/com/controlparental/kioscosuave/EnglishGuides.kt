package com.controlparental.kioscosuave

/**
 * MINI-GUÍAS DE INGLÉS, UNA POR UNIDAD
 * ====================================
 *
 * Antes había una sola guía (pasado / futuro / presente) para TODOS los
 * ejercicios: un alumno que pedía ayuda en un ejercicio de comparativos recibía
 * una explicación de tiempos verbales. Ahora cada unidad tiene la suya y el
 * botón de ayuda muestra la del tema que se está practicando.
 *
 * Reglas para escribir una guía (las comprueba EnglishGuideTest):
 *  - Explica la REGLA del tema, con ejemplos propios.
 *  - No copia ninguna oración del banco ya resuelta: ayuda a pensar, no
 *    entrega la respuesta.
 *
 * La llave es el título exacto de la unidad en [ChallengeEngine].
 */
object EnglishGuides {

    private fun guide(title: String, vararg lines: String) = WorkedExample(title, lines.toList())

    /** Deletreo: el NOMBRE de la letra no es lo mismo que su SONIDO. */
    val spelling = guide(
        "Guía: deletrear en inglés",
        "Cada letra tiene un NOMBRE y un SONIDO, y no son iguales.",
        "     La letra S se llama «es», pero suena /s/ como en «sun».",
        "     La letra C se llama «si», pero suena /k/ en «cup».",
        "Para DELETREAR se dicen los nombres: D-O-G = «di, ou, yi».",
        "Para LEER se juntan los sonidos: /d/ /o/ /g/ = dog.",
        "Vocales que confunden: A = «ei», E = «i», I = «ai».",
        "Truco: di la palabra despacio y escucha con qué sonido EMPIEZA."
    )

    private val byUnit: Map<String, WorkedExample> = listOf(
        "Viaje al pasado (Past Simple)" to guide(
            "Guía: pasado simple",
            "Se usa para algo que YA terminó.",
            "Verbos regulares: verbo + -ed.",
            "     play → played      clean → cleaned",
            "Verbos irregulares: cambian de forma y hay que memorizarlos.",
            "     give → gave      drink → drank",
            "Negación y preguntas: did + verbo SIN cambiar.",
            "     'I did not play.'   'Did you play?'",
            "PISTAS de pasado: yesterday, last week, ago, in 2020."
        ),
        "El futuro (will / going to)" to guide(
            "Guía: futuro",
            "WILL + verbo: decisión del momento, promesa o predicción.",
            "     'I will help you.'   'It will be sunny.'",
            "BE GOING TO + verbo: plan que ya estaba decidido.",
            "     'We are going to travel in July.'",
            "Después de will el verbo va SIN -s y sin 'to'.",
            "     will go ✓      will goes ✗      will to go ✗",
            "PISTAS de futuro: tomorrow, next week, soon, tonight."
        ),
        "Verbos irregulares" to guide(
            "Guía: verbos irregulares",
            "No llevan -ed en pasado: cada uno tiene su propia forma.",
            "Grupos que ayudan a recordarlos:",
            "     cambian una vocal:   sing → sang,   drink → drank",
            "     terminan en -ought:  think → thought,   bring → brought",
            "     no cambian:          cut → cut,   put → put",
            "Ojo: '-ed' pegado a un irregular casi nunca es correcto.",
            "     'singed' ✗      'sang' ✓",
            "Con did se usa la forma base: 'Did you sing?'"
        ),
        "El presente (Present Simple)" to guide(
            "Guía: presente simple",
            "Se usa para rutinas y cosas que siempre son verdad.",
            "Con I / you / we / they: el verbo va igual.",
            "     'They play chess.'",
            "Con he / she / it: el verbo lleva -s o -es.",
            "     'He plays chess.'   'She watches TV.'",
            "Preguntas: Do (I/you/we/they) · Does (he/she/it).",
            "     'Do they play?'   'Does he play?'  (sin -s tras does)",
            "PISTAS: every day, always, usually, on Mondays."
        ),
        "Un día de escuela (vocabulario)" to guide(
            "Guía: vocabulario de la escuela",
            "Lee la oración completa: las otras palabras son la pista.",
            "     Si dice 'write', busca algo con lo que se escribe.",
            "     Si dice 'teaches' o 'studies', busca una persona.",
            "Lugares de la escuela: classroom, library, playground.",
            "Objetos: pencil, eraser, ruler, desk, board.",
            "Personas: student, classmate, principal.",
            "Truco: imagina la escena y nombra lo que ves en ella."
        ),
        "De viaje (vocabulario)" to guide(
            "Guía: vocabulario de viajes",
            "Fíjate en el VERBO: te dice qué tipo de palabra falta.",
            "     fly → transporte por aire     sleep → un lugar",
            "     swim → un lugar con agua      pack → algo donde se guarda",
            "Transportes: train, bus, ship, taxi.",
            "Documentos y objetos: passport, map, camera, souvenir.",
            "Lugares: airport, station, museum, mountain.",
            "Truco: 'a' va antes de consonante y 'an' antes de vocal."
        ),
        "La familia (vocabulario)" to guide(
            "Guía: vocabulario de la familia",
            "Muchas palabras van en pareja (masculino / femenino):",
            "     father / mother      son / daughter",
            "     husband / wife       nephew / niece",
            "'grand-' sube una generación: grandfather, grandson.",
            "Algunas sirven para los dos: child, sibling, relative.",
            "Fíjate si el verbo es singular o plural:",
            "     'My ___ lives' → una persona   'My ___ live' → varias"
        ),
        "La comida (vocabulario)" to guide(
            "Guía: vocabulario de comida",
            "Comidas del día: lunch (mediodía) y dinner (noche).",
            "     Un snack es un bocadillo entre comidas.",
            "El VERBO es la pista: eat → alimento, drink → bebida.",
            "Bebidas: water, milk, tea, lemonade.",
            "Alimentos: bread, cheese, egg, soup, salad, fruit.",
            "Sabores: sweet (dulce), salty (salado), spicy (picante).",
            "Lee qué acompaña: 'with', 'and' unen dos alimentos."
        ),
        "Comparativos y superlativos" to guide(
            "Guía: comparativos y superlativos",
            "COMPARATIVO: compara DOS cosas. Lleva 'than'.",
            "     adjetivo corto + -er:   fast → faster than",
            "     adjetivo largo: more…   more expensive than",
            "SUPERLATIVO: el número uno de un grupo. Lleva 'the'.",
            "     adjetivo corto + -est:  the fastest",
            "     adjetivo largo: most…   the most expensive",
            "Irregulares: good → better → the best",
            "             bad → worse → the worst",
            "PISTA: ¿ves 'than'? comparativo. ¿ves 'the … in/of'? superlativo.",
            "Nunca se mezclan: 'more faster' ✗   'most fastest' ✗"
        ),
        "Verbos modales" to guide(
            "Guía: verbos modales",
            "Van antes de otro verbo, que queda SIN 'to' y sin -s.",
            "     'He can swim.' ✓      'He cans swim.' ✗",
            "CAN: habilidad o permiso informal.   COULD: habilidad en pasado.",
            "MUST: obligación fuerte.   MUST NOT: prohibición.",
            "SHOULD: consejo.   MAY / MIGHT: posibilidad o permiso formal.",
            "DON'T HAVE TO: no es necesario (no es lo mismo que prohibido).",
            "Pregúntate: ¿la frase habla de poder, deber, consejo o permiso?"
        ),
        "Primer condicional" to guide(
            "Guía: primer condicional",
            "Habla de algo POSIBLE en el futuro y su resultado.",
            "     IF + presente simple ,  WILL + verbo",
            "     'If you hurry, you will catch the bus.'",
            "El orden puede invertirse (sin coma):",
            "     'You will catch the bus if you hurry.'",
            "Regla de oro: en la parte del IF NO se usa will.",
            "     'If it will snow' ✗      'If it snows' ✓",
            "Con he/she/it el presente lleva -s: 'If she hurries…'"
        ),
        "Verbos frasales" to guide(
            "Guía: verbos frasales",
            "Verbo + partícula = un significado nuevo.",
            "     turn on (encender)      turn off (apagar)",
            "     put on (ponerse ropa)   take off (quitarse)",
            "     wake up (despertar)     sit down (sentarse)",
            "La partícula suele indicar dirección:",
            "     up = arriba / completo      down = abajo / menos",
            "     on = en marcha              off = detenido",
            "Lee el resto de la oración para saber qué sentido encaja."
        ),
        "Voz pasiva básica" to guide(
            "Guía: voz pasiva",
            "Importa lo que se HACE, no quién lo hace.",
            "     BE + participio pasado",
            "Presente: am / is / are + participio.",
            "     'The windows are cleaned every week.'",
            "Pasado: was / were + participio.",
            "     'The letter was written in 1980.'",
            "'by' presenta a quien lo hizo: 'painted by Frida'.",
            "PISTAS: every day → presente · yesterday, in 1990 → pasado.",
            "Singular usa is/was; plural usa are/were."
        ),
        "Preguntas con Wh- y do/does/did" to guide(
            "Guía: preguntas con Wh-",
            "Cada palabra pregunta por algo distinto:",
            "     What → qué         Where → dónde      When → cuándo",
            "     Who → quién        Why → por qué      How → cómo",
            "     How many → cuántos (se cuentan)   How much → cuánto",
            "Orden: Wh- + auxiliar + sujeto + verbo.",
            "     'When does the class start?'",
            "Auxiliar: do (I/you/we/they) · does (he/she/it) · did (pasado).",
            "Truco: piensa en la RESPUESTA. Si es un lugar, es Where."
        ),
        "Estilo indirecto (reported speech)" to guide(
            "Guía: estilo indirecto",
            "Cuentas lo que alguien dijo, y el verbo da un paso atrás.",
            "     presente → pasado:    'I need help' → he needed help",
            "     am / is → was         are → were",
            "     will → would          can → could",
            "También cambian los pronombres:",
            "     'I' → he / she      'my' → his / her      'we' → they",
            "Ejemplo: Ana said, 'I am tired.' → Ana said that she was tired."
        ),
        "Conectores (because, although, however, so that)" to guide(
            "Guía: conectores",
            "BECAUSE da la causa: responde a «¿por qué?».",
            "     'We left because it was late.'",
            "ALTHOUGH une dos ideas que chocan (aunque).",
            "     'Although it was late, we stayed.'",
            "HOWEVER también contrasta, pero empieza otra oración y lleva coma.",
            "     'It was late. However, we stayed.'",
            "SO THAT da la finalidad: responde a «¿para qué?».",
            "     'Speak slowly so that I can understand.'",
            "Pregúntate: ¿la segunda idea es causa, contraste o propósito?"
        ),
        "Vocabulario intermedio (tecnología, medio ambiente, profesiones)" to guide(
            "Guía: vocabulario intermedio",
            "Mira la terminación: te dice qué tipo de palabra es.",
            "     -tion / -ment → una cosa o idea (education, movement)",
            "     -er / -or / -ist → una persona (driver, actor, artist)",
            "     re- → volver a hacer (reuse, rebuild)",
            "Muchas se parecen al español: energy, device, climate.",
            "Fíjate qué falta en la oración:",
            "     tras 'should' o 'must' va un VERBO",
            "     tras 'the' o 'a' va un SUSTANTIVO"
        )
    ).toMap()

    /** Guía de una unidad, o null si esa unidad todavía no tiene la suya. */
    fun forUnit(title: String): WorkedExample? = byUnit[title]

    val unitTitles: Set<String> get() = byUnit.keys
}
