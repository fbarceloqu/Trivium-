package com.controlparental.kioscosuave.curriculum

import com.controlparental.kioscosuave.curriculum.ComprehensionSkill.CAUSE_EFFECT
import com.controlparental.kioscosuave.curriculum.ComprehensionSkill.CRITICAL
import com.controlparental.kioscosuave.curriculum.ComprehensionSkill.INFERENCE
import com.controlparental.kioscosuave.curriculum.ComprehensionSkill.LITERAL
import com.controlparental.kioscosuave.curriculum.ComprehensionSkill.MAIN_IDEA
import com.controlparental.kioscosuave.curriculum.ComprehensionSkill.VOCABULARY

/**
 * Banco local de lecturas largas para secundaria. Funciona sin conexión.
 *
 * Cada lectura pasa [LongReadingValidator] (lo comprueba LongReadingTest): 500–800
 * palabras, 6–8 preguntas que cubren los seis tipos de comprensión, una sola
 * clave, evidencia literal en el texto y opciones de tamaño parejo.
 *
 * Los ids se guardan en el historial del alumno: NO renombrarlos.
 * La primera opción de cada pregunta es la correcta; en pantalla se barajan.
 */
object LongReadingBank {

    private fun q(
        skill: ComprehensionSkill, question: String,
        answer: String, w1: String, w2: String, w3: String,
        explanation: String, evidence: String
    ) = LongQuestion(skill, question, listOf(answer, w1, w2, w3), answer, explanation, evidence)

    // -----------------------------------------------------------------
    private val sleep = LongReading(
        "sci.sleep_memory", ReadingTopic.SCIENCE, "Lo que el cerebro hace mientras duermes",
        listOf(
            "Durante mucho tiempo se pensó que dormir era simplemente apagar el cuerpo, una pausa inevitable entre un día y el siguiente. Hoy sabemos que ocurre casi lo contrario: mientras dormimos, el cerebro realiza algunas de sus tareas más importantes. Una de ellas es decidir qué recuerdos del día vale la pena conservar y cuáles pueden desecharse. Sin ese trabajo nocturno, buena parte de lo que aprendemos se perdería en pocas horas.",
            "Para entenderlo conviene imaginar dos almacenes. El primero es el hipocampo, una pequeña estructura situada en el interior del cerebro que funciona como una libreta de notas rápidas: guarda lo que acaba de ocurrir, pero tiene poco espacio y sus apuntes son frágiles. El segundo es la corteza cerebral, mucho más amplia, donde los recuerdos se guardan de forma duradera. Durante el día la libreta se va llenando. Por la noche, sobre todo en la fase de sueño profundo, el cerebro repasa esas notas una y otra vez y las va copiando al almacén permanente. Los científicos llaman a este proceso consolidación de la memoria.",
            "Las pruebas de que esto sucede son numerosas. En un experimento clásico, dos grupos de estudiantes memorizaron la misma lista de palabras. Un grupo durmió toda la noche y el otro permaneció despierto. Al día siguiente, quienes habían dormido recordaban muchas más palabras, aunque ambos grupos habían estudiado el mismo tiempo. Otros estudios muestran algo todavía más llamativo: durante el sueño se reactivan las mismas neuronas que se usaron al aprender, como si el cerebro volviera a practicar la lección por su cuenta. Dormir, entonces, no interrumpe el estudio; lo completa.",
            "No todas las fases del sueño hacen lo mismo. El sueño profundo, que predomina en las primeras horas de la noche, parece fijar sobre todo datos y conceptos, como fechas históricas o definiciones. El sueño REM, la fase en la que soñamos con más intensidad y que se concentra hacia el final de la noche, ayuda a relacionar ideas entre sí y a afianzar habilidades, como tocar un instrumento o resolver un tipo de problema. Por eso quien recorta sus horas de descanso levantándose muy temprano pierde justamente la parte más rica en sueño REM.",
            "Los adolescentes necesitan entre ocho y diez horas de sueño, más que los adultos, pero suelen dormir bastante menos. Parte de la explicación es biológica: en la adolescencia el reloj interno se retrasa y el sueño llega más tarde. Otra parte tiene que ver con las pantallas. La luz azul de teléfonos y tabletas frena la producción de melatonina, la hormona que avisa al cuerpo de que es hora de dormir. El resultado es un círculo difícil de romper: se duerme tarde, se madruga para ir a la escuela y se acumula cansancio durante la semana.",
            "De todo lo anterior se desprende un consejo que contradice una costumbre muy extendida. Pasar la noche en vela antes de un examen puede dar la sensación de aprovechar el tiempo, pero deja al hipocampo saturado y sin oportunidad de trasladar lo estudiado. Resulta más eficaz repartir el estudio en varios días y dormir bien entre una sesión y otra. Quien estudia dos horas y luego duerme suele recordar más que quien estudia cuatro y no descansa. La almohada, bien usada, es una herramienta de estudio tan valiosa como el cuaderno."
        ),
        listOf(
            q(LITERAL, "Según el texto, ¿qué función cumple el hipocampo?",
                "Guarda por poco tiempo lo que acaba de ocurrir",
                "Almacena los recuerdos de manera duradera y estable",
                "Produce la hormona que provoca el sueño",
                "Reactiva las neuronas mientras soñamos",
                "El texto lo compara con una libreta de notas rápidas, de poco espacio y apuntes frágiles; el almacén duradero es la corteza.",
                "guarda lo que acaba de ocurrir, pero tiene poco espacio"),
            q(MAIN_IDEA, "¿Cuál es la idea principal del texto?",
                "Dormir es una parte activa y necesaria del aprendizaje",
                "Los adolescentes duermen menos por culpa de las pantallas",
                "El cerebro tiene dos almacenes con funciones distintas",
                "Estudiar muchas horas seguidas perjudica al hipocampo",
                "Las pantallas y los dos almacenes son detalles que apoyan una idea mayor: el sueño completa lo que se estudia.",
                "Dormir, entonces, no interrumpe el estudio; lo completa."),
            q(INFERENCE, "Una alumna practica una pieza de piano y esa noche se levanta dos horas antes de lo habitual. Según el texto, ¿qué es lo más probable?",
                "Que afiance peor la pieza, porque perdió sueño REM",
                "Que olvide las fechas de historia que repasó por la tarde",
                "Que recuerde mejor la pieza al estar más despierta",
                "Que no note diferencia si durmió profundamente",
                "Tocar un instrumento es una habilidad, y las habilidades se afianzan en el sueño REM, que se concentra al final de la noche: justo lo que ella recortó.",
                "pierde justamente la parte más rica en sueño REM"),
            q(CAUSE_EFFECT, "¿Por qué la luz de las pantallas dificulta conciliar el sueño?",
                "Porque reduce la hormona que anuncia la hora de dormir",
                "Porque retrasa el reloj interno propio de los adolescentes",
                "Porque mantiene saturado al hipocampo toda la noche",
                "Porque acorta la fase de sueño profundo inicial",
                "El retraso del reloj interno es la causa biológica, distinta de las pantallas. La luz azul actúa frenando la melatonina.",
                "frena la producción de melatonina"),
            q(VOCABULARY, "En el texto, la palabra «consolidación» se refiere a…",
                "volver firme y duradero un recuerdo",
                "seleccionar los datos menos útiles",
                "reunir varios recuerdos en uno solo",
                "repetir en voz alta lo aprendido",
                "Se llama así al paso de las notas frágiles del hipocampo al almacén permanente: el recuerdo se asienta.",
                "Los científicos llaman a este proceso consolidación de la memoria."),
            q(CRITICAL, "¿Qué limitación tiene el experimento de la lista de palabras para afirmar que dormir mejora cualquier aprendizaje?",
                "Solo midió memorizar palabras, no otras habilidades",
                "Los dos grupos dedicaron tiempos de estudio diferentes",
                "No comparó a quienes durmieron con quienes no",
                "Se realizó únicamente con personas adultas",
                "El experimento sí comparó dos grupos con el mismo tiempo de estudio; su límite es que solo probó un tipo de aprendizaje.",
                "dos grupos de estudiantes memorizaron la misma lista de palabras"),
            q(CAUSE_EFFECT, "¿Por qué el autor desaconseja desvelarse antes de un examen?",
                "Porque lo estudiado no llega a pasar a la memoria duradera",
                "Porque el cansancio impide leer las preguntas con la calma necesaria",
                "Porque el sueño REM solo aparece tras varios días",
                "Porque el hipocampo necesita luz para funcionar",
                "Sin dormir, el hipocampo queda lleno y no traslada lo estudiado a la corteza, que es donde perdura.",
                "deja al hipocampo saturado y sin oportunidad de trasladar lo estudiado")
        )
    )

    // -----------------------------------------------------------------
    private val press = LongReading(
        "his.printing_press", ReadingTopic.HISTORY, "La imprenta: más libros, más ideas, más rumores",
        listOf(
            "A mediados del siglo XV, un libro era un objeto de lujo. Cada ejemplar se copiaba a mano, letra por letra, en talleres donde un copista podía tardar meses en terminar una sola obra. El resultado era hermoso y carísimo: una biblioteca de cien volúmenes se consideraba un tesoro, y la mayoría de las personas pasaba la vida entera sin tener un libro entre las manos. Leer era, en buena medida, un privilegio de monasterios, universidades y cortes. Incluso quienes sabían leer dependían de lo que otros decidían copiar, de modo que muchas obras existían en un único ejemplar y bastaba un incendio para perderlas para siempre.",
            "Hacia 1450, en la ciudad alemana de Maguncia, un orfebre llamado Johannes Gutenberg perfeccionó un sistema que cambiaría esa situación. Su idea no fue inventar la impresión, que ya se practicaba en Asia desde hacía siglos, sino combinar varias técnicas de un modo nuevo. Fabricó pequeñas piezas de metal, cada una con una letra en relieve, que podían ordenarse para formar una página, entintarse, prensarse sobre el papel y después desarmarse para componer la página siguiente. Estos tipos móviles eran resistentes y reutilizables, de manera que el mismo juego de letras servía para imprimir obras distintas.",
            "Las consecuencias fueron rápidas. Un taller podía producir en pocas semanas lo que antes exigía años de trabajo, y el precio de los libros cayó de forma notable. Se calcula que hacia el año 1500, apenas medio siglo después, circulaban por Europa unos veinte millones de ejemplares impresos. Ya no se imprimían solo textos religiosos: aparecieron gramáticas, mapas, tratados de medicina, calendarios y relatos de viajes. Como había más que leer y costaba menos, más gente tuvo motivos para aprender a hacerlo. Los impresores, que eran también comerciantes, descubrieron pronto que les convenía publicar en las lenguas que hablaba la gente y no solo en latín, la lengua de los estudiosos. Así, el español, el francés, el alemán o el italiano empezaron a fijar su ortografía y a ganar prestigio como lenguas de cultura.",
            "La imprenta transformó también la manera de discutir. Antes, una idea nueva viajaba despacio y podía ser silenciada con relativa facilidad, pues bastaba con destruir unos pocos manuscritos. Con miles de copias repartidas por distintas ciudades, eso se volvió casi imposible. Cuando en 1517 Martín Lutero criticó a la Iglesia, sus escritos se reimprimieron una y otra vez y llegaron a lectores de toda Europa en cuestión de semanas. Algo parecido ocurrió con la ciencia: los astrónomos y médicos pudieron comparar sus observaciones con las de colegas lejanos trabajando sobre ejemplares idénticos, lo que permitió detectar errores y corregirlos.",
            "Sin embargo, sería ingenuo pensar que la imprenta solo difundió conocimiento. Las mismas prensas que publicaban tratados científicos imprimían panfletos con rumores, profecías inventadas y acusaciones sin fundamento contra vecinos o pueblos enteros. Un texto impreso parecía más serio que una habladuría, y muchos lectores le concedían una autoridad que no merecía. Las autoridades reaccionaron con censura y listas de libros prohibidos, medidas que frenaron algunas mentiras pero también persiguieron ideas valiosas.",
            "Mirada desde hoy, la historia de la imprenta resulta familiar. Cada vez que una tecnología abarata la difusión de las palabras, se multiplican a la vez el saber y el engaño, y la sociedad tarda un tiempo en aprender a distinguirlos. La imprenta no hizo a nadie más sabio por sí sola: puso más textos al alcance de más personas y dejó en manos de los lectores la tarea de juzgarlos. Más de quinientos años después, esa tarea sigue siendo nuestra."
        ),
        listOf(
            q(LITERAL, "¿Qué aportó Gutenberg, según el texto?",
                "Una nueva combinación de técnicas con letras metálicas",
                "La primera forma de impresión conocida en el mundo",
                "Un método para copiar manuscritos a mano con más rapidez",
                "Una prensa capaz de imprimir sin intervención humana",
                "El texto aclara que la impresión ya existía en Asia: lo nuevo fue combinar técnicas usando piezas de metal reutilizables.",
                "combinar varias técnicas de un modo nuevo"),
            q(CAUSE_EFFECT, "¿Por qué aumentó el número de personas que sabían leer?",
                "Porque había más libros, más variados y más baratos",
                "Porque las universidades abrieron sus puertas a todos",
                "Porque la Iglesia ordenó enseñar a leer en cada ciudad",
                "Porque los copistas empezaron a dar clases públicas",
                "La causa que da el texto es económica y práctica: más que leer y a menor precio.",
                "Como había más que leer y costaba menos, más gente tuvo motivos para aprender a hacerlo."),
            q(INFERENCE, "¿Por qué resultó tan difícil silenciar las ideas de Lutero?",
                "Porque existían demasiadas copias en lugares distintos",
                "Porque las autoridades estaban de acuerdo con sus críticas",
                "Porque sus escritos se guardaban en monasterios seguros",
                "Porque nadie sabía quién era el autor de los textos",
                "Antes bastaba destruir unos pocos manuscritos; con miles de copias dispersas ya no había forma de eliminarlas todas.",
                "Con miles de copias repartidas por distintas ciudades, eso se volvió casi imposible."),
            q(VOCABULARY, "En el texto, la expresión «tipos móviles» designa…",
                "piezas con letras que se ordenan y se reutilizan",
                "talleres que se trasladaban de una ciudad a otra",
                "páginas ya impresas que podían cambiarse de sitio",
                "copistas que trabajaban en varios monasterios",
                "Son las pequeñas piezas de metal con una letra en relieve: se arman en una página y luego se desarman para la siguiente.",
                "Estos tipos móviles eran resistentes y reutilizables"),
            q(MAIN_IDEA, "¿Qué idea resume mejor el texto?",
                "La imprenta multiplicó tanto el conocimiento como el engaño",
                "Gutenberg fue el inventor más importante de la historia europea",
                "La censura fue la única respuesta eficaz contra los panfletos",
                "Los libros impresos eran más hermosos que los copiados a mano",
                "El texto recorre los beneficios y también los rumores impresos, y cierra uniendo ambos efectos.",
                "se multiplican a la vez el saber y el engaño"),
            q(CRITICAL, "¿Qué postura adopta el autor frente a la censura de la época?",
                "Reconoce que frenó mentiras, pero también ideas valiosas",
                "La defiende como la mejor forma de proteger a los lectores",
                "La rechaza porque nunca logró detener ningún rumor",
                "Evita opinar y se limita a enumerar los libros prohibidos",
                "El autor matiza: admite un efecto positivo y otro negativo, sin defenderla ni rechazarla del todo.",
                "medidas que frenaron algunas mentiras pero también persiguieron ideas valiosas"),
            q(INFERENCE, "¿Con qué situación actual se relaciona mejor el último párrafo?",
                "La circulación de noticias falsas en las redes sociales",
                "El aumento del precio de los libros de texto escolares",
                "La desaparición de las bibliotecas en las ciudades pequeñas",
                "El uso de tabletas para sustituir los cuadernos",
                "El párrafo habla de tecnologías que abaratan difundir palabras y multiplican saber y engaño a la vez: es lo que ocurre hoy en las redes.",
                "Cada vez que una tecnología abarata la difusión de las palabras")
        )
    )

    // -----------------------------------------------------------------
    private val gps = LongReading(
        "tec.gps", ReadingTopic.TECHNOLOGY, "¿Cómo sabe tu teléfono dónde estás?",
        listOf(
            "Abres una aplicación de mapas y, en un par de segundos, un punto azul señala la calle exacta en la que te encuentras. Es un gesto tan cotidiano que rara vez nos preguntamos cómo es posible. La respuesta no está dentro del teléfono, sino a unos veinte mil kilómetros sobre nuestras cabezas, donde una treintena de satélites da vueltas a la Tierra sin descanso. Hace apenas unas décadas, orientarse en una ciudad desconocida exigía desplegar un mapa de papel y preguntar a los transeúntes; hoy confiamos esa tarea a un aparato que cabe en el bolsillo.",
            "Esos satélites forman el Sistema de Posicionamiento Global, conocido como GPS por sus siglas en inglés. Cada uno lleva a bordo un reloj atómico de enorme precisión y emite continuamente una señal de radio que dice, en esencia, dos cosas: quién soy y qué hora es exactamente en este instante. Conviene subrayar un detalle que suele sorprender: los satélites no saben dónde estás ni reciben nada de tu teléfono. Solo hablan; es tu aparato el que escucha.",
            "El cálculo se basa en una idea sencilla. La señal viaja a la velocidad de la luz, de modo que tarda una pequeñísima fracción de segundo en llegar. El teléfono compara la hora en que el mensaje salió con la hora en que lo recibió y, a partir de esa diferencia, deduce a qué distancia está el satélite. Con un solo satélite sabría únicamente que se encuentra en algún punto de una enorme esfera imaginaria. Con dos, las esferas se cortan en un círculo. Con tres, las posibilidades se reducen a dos puntos. Un cuarto satélite elimina la duda y, además, corrige el reloj del teléfono, que es mucho menos exacto que los atómicos.",
            "La precisión exigida es extraordinaria. Como la luz recorre unos trescientos metros en una millonésima de segundo, un error diminuto en la medida del tiempo se convierte en un error enorme en el mapa. Aquí interviene, de forma inesperada, la física de Einstein. Según la teoría de la relatividad, el tiempo no transcurre igual para un reloj que se mueve muy rápido y está lejos de la Tierra que para otro situado en el suelo. Los relojes de los satélites se adelantan unas treinta y ocho millonésimas de segundo cada día. Parece insignificante, pero si los ingenieros no lo corrigieran, la posición que marca tu teléfono se desviaría alrededor de diez kilómetros diarios.",
            "En las ciudades, los edificios altos bloquean o hacen rebotar las señales, y dentro de un túnel directamente no llegan. Por eso los teléfonos combinan el GPS con otras pistas: las antenas de telefonía cercanas, las redes wifi que detectan a su alrededor y sensores que registran el movimiento. Gracias a esa mezcla, el punto azul sigue avanzando aunque el cielo no esté a la vista. La posición resultante es menos exacta que la obtenida a cielo abierto, pero suficiente para no perderse.",
            "Que el satélite no sepa dónde estás no significa que nadie lo sepa. Una vez que el teléfono ha calculado su posición, las aplicaciones pueden guardarla y enviarla por internet a las empresas que las fabrican. Un historial de ubicaciones revela dónde vives, a qué escuela vas y con quién pasas el tiempo. La tecnología que te orienta es la misma que puede seguirte, y decidir qué aplicaciones tienen permiso para usarla es una elección que conviene tomar con cuidado."
        ),
        listOf(
            q(LITERAL, "¿Qué información transmite cada satélite?",
                "Su identidad y la hora exacta de emisión",
                "La posición del teléfono que lo escucha",
                "La distancia que lo separa de la Tierra",
                "El mapa de la zona que está sobrevolando",
                "La señal dice dos cosas: quién es el satélite y qué hora es. La distancia la deduce el teléfono, no viaja en la señal.",
                "quién soy y qué hora es exactamente en este instante"),
            q(CAUSE_EFFECT, "¿Para qué se necesita un cuarto satélite?",
                "Para descartar un punto y ajustar el reloj del teléfono",
                "Para medir la velocidad a la que se desplaza el usuario",
                "Para enviar la posición calculada a las aplicaciones",
                "Para aplicar la corrección que exige la relatividad",
                "Con tres satélites quedan dos puntos posibles; el cuarto resuelve cuál es y corrige el reloj poco exacto del teléfono.",
                "Un cuarto satélite elimina la duda y, además, corrige el reloj del teléfono"),
            q(INFERENCE, "Un teléfono está en modo avión, sin datos ni wifi. Según el texto, ¿podría calcular su posición con GPS?",
                "Sí, porque solo necesita recibir las señales",
                "No, porque debe avisar al satélite de dónde está",
                "No, porque el reloj atómico se consulta por internet",
                "Sí, pero solo si hay una antena de telefonía cerca",
                "El teléfono únicamente escucha a los satélites; no les envía nada, así que no necesita conexión para calcular.",
                "Solo hablan; es tu aparato el que escucha."),
            q(VOCABULARY, "En el texto, la palabra «insignificante» quiere decir…",
                "tan pequeño que parece no importar",
                "imposible de medir con precisión",
                "difícil de explicar con palabras",
                "que carece de explicación científica",
                "Se refiere a un adelanto de millonésimas de segundo: parece no tener importancia, aunque el texto muestra que sí la tiene.",
                "Parece insignificante, pero si los ingenieros no lo corrigieran"),
            q(MAIN_IDEA, "¿Cuál es el propósito principal del texto?",
                "Explicar cómo se calcula la posición y qué implica",
                "Demostrar que la teoría de Einstein era correcta",
                "Advertir de que los satélites espían a los usuarios",
                "Comparar el GPS con las antenas de telefonía móvil",
                "La mayor parte explica el cálculo, y el cierre añade su consecuencia para la privacidad. Einstein y las antenas son detalles.",
                "La tecnología que te orienta es la misma que puede seguirte"),
            q(CRITICAL, "¿Qué afirmación del texto sirve para corregir la idea de que «los satélites nos vigilan»?",
                "Los satélites emiten señales, pero no reciben nada",
                "Los edificios altos bloquean o desvían las señales",
                "Los relojes atómicos se adelantan un poco cada día",
                "Las aplicaciones pueden guardar las ubicaciones",
                "Si no reciben nada del teléfono, no pueden saber dónde está. Quien puede conocer la ubicación son las aplicaciones, no los satélites.",
                "los satélites no saben dónde estás ni reciben nada de tu teléfono"),
            q(CAUSE_EFFECT, "¿Qué ocurriría si no se corrigiera el adelanto de los relojes?",
                "El mapa acumularía kilómetros de error cada día",
                "Harían falta más de cuatro satélites para orientarse",
                "La señal tardaría más en llegar al teléfono",
                "El teléfono dejaría de recibir las señales",
                "Un error mínimo de tiempo se vuelve un error enorme de distancia: unos diez kilómetros por día.",
                "se desviaría alrededor de diez kilómetros diarios")
        )
    )

    // -----------------------------------------------------------------
    private val axolotl = LongReading(
        "nat.axolotl", ReadingTopic.NATURE, "El ajolote, el animal que no quiere crecer",
        listOf(
            "En los canales de Xochimilco, al sur de la Ciudad de México, vive un animal que parece salido de un cuento. Tiene el cuerpo alargado, una cabeza ancha con una expresión que recuerda a una sonrisa y, a ambos lados, tres pares de branquias que se abren como pequeñas plumas. Es el ajolote, una salamandra que los antiguos mexicas relacionaban con el dios Xólotl y que hoy figura entre los anfibios más estudiados y más amenazados del planeta. Su nombre procede del náhuatl y suele traducirse como «monstruo de agua», aunque quien lo observa nadar con calma entre las plantas difícilmente lo encuentra temible.",
            "Lo que hace único al ajolote es que nunca termina de crecer del modo habitual. La mayoría de los anfibios pasa por una metamorfosis: nace en el agua, respira por branquias y, al madurar, desarrolla pulmones y se traslada a tierra, como hacen las ranas. El ajolote, en cambio, alcanza la edad adulta y se reproduce conservando su aspecto de larva y sin abandonar el agua. Los biólogos llaman neotenia a esta capacidad de mantener rasgos juveniles durante toda la vida.",
            "Su segunda rareza interesa todavía más a la medicina. Si un ajolote pierde una pata, le vuelve a crecer completa, con huesos, músculos y nervios, y sin dejar cicatriz. Puede regenerar también la cola, partes del corazón e incluso fragmentos del cerebro. Lo más sorprendente es que puede repetir la hazaña varias veces a lo largo de su vida sin que el resultado empeore. En los seres humanos, una herida profunda se cierra con tejido cicatricial, que tapa el daño pero no recupera la función original. Por eso laboratorios de todo el mundo crían ajolotes: comprender cómo ordenan sus células reconstruir un miembro podría inspirar algún día tratamientos para personas con lesiones graves.",
            "Resulta paradójico que un animal tan abundante en los acuarios de los laboratorios esté a punto de desaparecer en libertad. Un censo realizado en 1998 encontró unos seis mil ajolotes por kilómetro cuadrado en Xochimilco; en 2014 se contaron menos de cuarenta. Las causas son varias y se refuerzan entre sí. El crecimiento de la ciudad ha contaminado el agua con desechos y ha secado parte de los canales. Además, hace décadas se introdujeron carpas y tilapias para fomentar la pesca, y estos peces devoran los huevos y las crías del ajolote, además de competir con él por el alimento.",
            "Frente a este panorama, científicos y agricultores locales han puesto en marcha un proyecto que recupera una técnica muy antigua. Las chinampas son parcelas de cultivo construidas sobre el agua, separadas por canales estrechos. Al colocar filtros de plantas y barreras en la entrada de algunos de esos canales, se obtienen refugios de agua limpia a los que no pueden entrar las carpas ni las tilapias. Los agricultores que participan se comprometen a no usar pesticidas, y a cambio pueden vender sus hortalizas con una etiqueta que las distingue. De este modo, proteger al anfibio deja de ser un sacrificio para quienes cultivan y se convierte en una ventaja.",
            "La apuesta tiene límites: los refugios son todavía pocos y dependen de que la agricultura tradicional siga siendo rentable. Aun así, plantea una idea valiosa. Salvar al ajolote no consiste solo en proteger a un animal simpático, sino en cuidar el lugar entero del que depende, incluidas las personas que viven de él."
        ),
        listOf(
            q(LITERAL, "¿Qué diferencia al ajolote de la mayoría de los anfibios?",
                "Llega a adulto sin perder su forma de larva",
                "Nace en tierra y después se traslada al agua",
                "Respira por pulmones durante toda su vida",
                "Completa su metamorfosis antes que las ranas",
                "Los demás anfibios cambian de forma al madurar; el ajolote se reproduce conservando el aspecto de larva.",
                "alcanza la edad adulta y se reproduce conservando su aspecto de larva"),
            q(VOCABULARY, "En el texto, la palabra «paradójico» significa…",
                "que parece contradictorio a primera vista",
                "que ocurre con muchísima frecuencia",
                "que resulta imposible de comprobar",
                "que provoca preocupación en los expertos",
                "Lo contradictorio es que el animal abunde en laboratorios y, a la vez, casi haya desaparecido en libertad.",
                "Resulta paradójico que un animal tan abundante"),
            q(CAUSE_EFFECT, "¿Cómo perjudican las carpas y las tilapias al ajolote?",
                "Se comen sus huevos y crías y le quitan alimento",
                "Contaminan el agua de los canales con sus desechos",
                "Transmiten enfermedades que impiden la regeneración",
                "Ocupan los refugios que construyen los agricultores",
                "La contaminación viene del crecimiento de la ciudad; los peces introducidos dañan devorando huevos y crías y compitiendo por la comida.",
                "estos peces devoran los huevos y las crías del ajolote"),
            q(INFERENCE, "¿Por qué interesa a la medicina la regeneración del ajolote?",
                "Porque podría enseñar a reparar tejidos sin cicatriz",
                "Porque sus células curan heridas humanas al trasplantarse",
                "Porque demuestra que las cicatrices recuperan la función",
                "Porque permite criar ajolotes más resistentes",
                "El texto habla de una posibilidad futura («podría inspirar»), no de una cura que ya exista ni de trasplantes.",
                "podría inspirar algún día tratamientos para personas con lesiones graves"),
            q(MAIN_IDEA, "¿Cuál es la idea central del texto?",
                "Un animal extraordinario cuya salvación exige cuidar su entorno",
                "Una especie que ya solo puede sobrevivir dentro de laboratorios",
                "Una técnica agrícola antigua que produce hortalizas sin pesticidas",
                "Un anfibio cuyas células curarán pronto las lesiones humanas",
                "El texto describe sus rarezas, su declive y un proyecto de rescate, y concluye que protegerlo implica cuidar todo el lugar.",
                "sino en cuidar el lugar entero del que depende"),
            q(CRITICAL, "¿Qué debilidad del proyecto de refugios reconoce el propio texto?",
                "Depende de que la agricultura tradicional dé ganancias",
                "Requiere eliminar todas las chinampas de Xochimilco",
                "Obliga a los agricultores a dejar de vender hortalizas",
                "Impide que los científicos sigan estudiando al animal",
                "El último párrafo admite dos límites: hay pocos refugios y solo se mantienen si a los agricultores les resulta rentable.",
                "dependen de que la agricultura tradicional siga siendo rentable"),
            q(INFERENCE, "¿Qué permiten concluir las cifras de 1998 y 2014?",
                "Que la población silvestre se desplomó en pocos años",
                "Que el censo de 2014 se hizo en una zona más pequeña",
                "Que los ajolotes se trasladaron a otros canales",
                "Que la especie se recupera lentamente desde 1998",
                "Pasar de unos seis mil a menos de cuarenta por kilómetro cuadrado en dieciséis años es una caída casi total.",
                "en 2014 se contaron menos de cuarenta")
        )
    )

    // -----------------------------------------------------------------
    private val cinema = LongReading(
        "lit.last_screening", ReadingTopic.LITERATURE, "La última función",
        listOf(
            "El cine Edén cerraba ese sábado. Lo anunciaba un cartel escrito a mano en la taquilla, con una letra tan temblorosa que Julia tuvo que leerlo dos veces. El dueño había vendido el edificio a una cadena de tiendas, y nadie en el barrio se había sorprendido: hacía tiempo que a las funciones acudían más butacas vacías que espectadores. Julia llevaba todo el verano ayudando allí por las tardes. Barría la sala, ordenaba las butacas que aún no estaban rotas y, sobre todo, subía a la cabina para ver trabajar a don Anselmo, el proyeccionista, que llevaba cuarenta y un años haciendo girar las mismas máquinas.",
            "—Esta noche nos despedimos como se debe —dijo él sin levantar la vista de la bobina—. Con una película de verdad, no con esas que caben en un teléfono. Julia sonrió, pero no contestó. En el bolsillo llevaba, precisamente, el teléfono en el que había visto casi todas las películas de su vida. Nunca se lo había dicho a don Anselmo. Le parecía que confesarlo sería como admitir, delante de un panadero, que una prefería el pan del supermercado.",
            "A las ocho, la sala estaba casi llena por primera vez en años. Habían venido vecinos que no pisaban el Edén desde niños, y todos hablaban en voz baja, como en un velorio. Julia reconoció a la maestra de primaria, al farmacéutico y a una pareja de ancianos que, según se decía, se había conocido en la fila de esa misma taquilla. Don Anselmo apagó las luces, accionó la palanca y el haz de luz atravesó el polvo hasta la pantalla. Julia, de pie junto a él, notó que al viejo le temblaban las manos, aunque no supo si era por la edad o por otra cosa.",
            "A mitad de la función ocurrió el desastre. Se oyó un chasquido, la imagen se quemó desde el centro hacia los bordes y la pantalla quedó en blanco. La cinta se había roto. En la sala creció un murmullo. Don Anselmo se quedó inmóvil, mirando el proyector como quien mira a un amigo que acaba de fallarle. —Hace diez años la habría arreglado en dos minutos —murmuró—. Ahora ni siquiera veo dónde se cortó.",
            "Julia no lo pensó demasiado. Sacó el teléfono, encendió la linterna y la acercó a la cinta. —Yo veo por los dos —dijo—. Usted dígame qué hacer. Durante un rato trabajaron así: él dictaba, con una paciencia que no parecía suya, y ella cortaba, alineaba y pegaba con unos dedos que aprendían sobre la marcha. Cuando la imagen volvió a la pantalla, abajo estalló un aplauso que ninguno de los dos esperaba.",
            "Al terminar, mientras la gente salía despacio, don Anselmo le puso en la mano las tijeras de empalmar, pequeñas y gastadas. —Ya no me hacen falta —dijo—. Y esa linterna tuya… no está tan mal, para ser un teléfono. Julia guardó las tijeras en el mismo bolsillo que el aparato. Pesaban poco, pero camino a casa tuvo la impresión de llevar algo que no debía perder. Detrás de ella, las letras luminosas del Edén se apagaron una por una, y la calle quedó tan oscura como una sala antes de que empiece la película."
        ),
        listOf(
            q(LITERAL, "¿Por qué se interrumpe la función?",
                "Porque la cinta de la película se rompe",
                "Porque se corta la luz en todo el barrio",
                "Porque el público empieza a protestar",
                "Porque don Anselmo se siente enfermo",
                "Se oye un chasquido, la imagen se quema y la pantalla queda en blanco: la cinta se había roto.",
                "La cinta se había roto."),
            q(INFERENCE, "¿Por qué Julia no le había contado a don Anselmo que veía películas en el teléfono?",
                "Porque temía que él lo tomara como un desprecio",
                "Porque sus padres le habían prohibido usarlo",
                "Porque pensaba que él no sabría qué es un teléfono",
                "Porque quería sorprenderlo el día del cierre",
                "La comparación con el panadero muestra que confesarlo le parecía ofender el oficio al que él había dedicado su vida.",
                "sería como admitir, delante de un panadero, que una prefería el pan del supermercado"),
            q(VOCABULARY, "En el relato, «empalmar» una cinta significa…",
                "unir de nuevo los dos extremos rotos",
                "enrollarla con cuidado en la bobina",
                "limpiarla del polvo acumulado",
                "cambiarla por una copia más nueva",
                "Las tijeras de empalmar sirven para lo que hacen juntos: cortar, alinear y pegar la cinta rota.",
                "las tijeras de empalmar, pequeñas y gastadas"),
            q(CAUSE_EFFECT, "¿Qué hace posible que la película vuelva a proyectarse?",
                "La unión de la experiencia de él y la vista de ella",
                "La llegada de un técnico que estaba entre el público",
                "El uso de una copia digital guardada en el teléfono",
                "La paciencia de los vecinos, que deciden esperar",
                "Él sabe cómo reparar pero ya no ve; ella ve pero no sabe. La reparación ocurre porque cada uno pone lo que al otro le falta.",
                "él dictaba, con una paciencia que no parecía suya, y ella cortaba, alineaba y pegaba"),
            q(MAIN_IDEA, "¿Cuál es el tema central del relato?",
                "Lo viejo y lo nuevo pueden colaborar en vez de enfrentarse",
                "Los cines antiguos desaparecen por culpa de los teléfonos",
                "Los jóvenes aprenden cualquier oficio con más rapidez",
                "La vejez impide seguir haciendo lo que uno ama",
                "El teléfono que don Anselmo despreciaba es lo que permite salvar la función, guiado por el saber de él.",
                "—Yo veo por los dos —dijo—."),
            q(CRITICAL, "¿Qué sugiere que don Anselmo regale las tijeras a Julia?",
                "Que le confía la continuidad de lo que él sabía hacer",
                "Que quiere deshacerse de todo lo que le recuerda al cine",
                "Que le paga con ellas el trabajo de todo el verano",
                "Que reconoce que los teléfonos han ganado la partida",
                "Julia siente que lleva algo que no debe perder: el regalo funciona como una herencia del oficio, no como un pago.",
                "tuvo la impresión de llevar algo que no debía perder"),
            q(INFERENCE, "¿Qué indica el comentario final de don Anselmo sobre la linterna?",
                "Que su opinión sobre los teléfonos se ha suavizado",
                "Que piensa comprarse un teléfono como el de Julia",
                "Que sigue creyendo que el cine no necesita ninguno",
                "Que se burla de Julia por depender del aparato",
                "Al principio despreciaba las películas «que caben en un teléfono»; al final admite, a su manera, que el aparato sirvió.",
                "no está tan mal, para ser un teléfono")
        )
    )

    // -----------------------------------------------------------------
    private val antSpending = LongReading(
        "day.ant_spending", ReadingTopic.DAILY_LIFE, "¿A dónde se fue mi dinero?",
        listOf(
            "Regina recibe cada lunes doscientos pesos para sus gastos de la semana. El viernes, casi siempre, su monedero está vacío, y cuando su madre le pregunta en qué se fue el dinero, ella contesta con sinceridad que no lo sabe. No ha comprado nada grande: ni ropa, ni un videojuego, ni entradas para un concierto. Sin embargo, el dinero desapareció. A este fenómeno, tan común que tiene nombre propio, se le llama gasto hormiga.",
            "La expresión describe las compras pequeñas y frecuentes que, por separado, parecen no tener importancia: una bebida a la salida de la escuela, unas papas en el recreo, un paquete de estampas, el cobro de una aplicación que se renueva sola. Ninguna duele en el momento de pagarla, y justo ahí reside el problema. Como cada gasto es mínimo, no lo registramos en la memoria, y al no registrarlo tampoco lo sumamos. Las hormigas, una por una, no pesan nada; juntas pueden vaciar una despensa. Algo parecido sucede con las monedas sueltas: como parecen valer poco, se gastan sin pensar.",
            "Hagamos la cuenta que Regina nunca hizo. Supongamos que gasta treinta pesos cada día de clases en bebidas y botanas. En una semana son ciento cincuenta pesos, es decir, tres cuartas partes de todo lo que recibe. En un ciclo escolar de doscientos días, la cifra llega a seis mil pesos. Con esa cantidad podría haber comprado la bicicleta que lleva meses mirando en un escaparate y que, según ella, jamás podrá pagar. Lo más revelador es que, al preguntarle, Regina habría jurado que gastaba mucho menos: su cálculo de memoria se quedaba en unos cincuenta pesos semanales.",
            "Los comercios conocen bien este comportamiento y lo aprovechan. No es casualidad que los dulces estén colocados junto a la caja, a la altura de los ojos, ni que muchos precios terminen en noventa y nueve centavos para parecer más bajos de lo que son. Tampoco lo es que las suscripciones digitales ofrezcan un primer mes gratuito y después cobren de manera automática: confían en que el cliente olvidará cancelarlas. Nada de esto es ilegal, pero todo está pensado para que la decisión de comprar se tome deprisa y sin calcular. Saberlo no vuelve a nadie inmune, pero ayuda a reconocer el momento en que conviene detenerse un segundo antes de pagar.",
            "Combatir el gasto hormiga no exige renunciar a todos los gustos. El primer paso consiste, sencillamente, en anotar durante dos semanas cada peso que sale del bolsillo, por pequeño que sea. La mayoría de las personas se sorprende al ver el total. El segundo paso es distinguir entre lo que se necesita y lo que solo se antoja, y fijar un límite semanal para lo segundo. El tercero es ponerle nombre al ahorro: guardar dinero «por si acaso» resulta mucho más difícil que guardarlo para algo concreto, con un precio y una fecha.",
            "Regina hizo la prueba. Descubrió que podía llevar agua de su casa tres días a la semana sin sentirse castigada y reservar las botanas para los viernes. También canceló una suscripción que llevaba cuatro meses pagando sin usar, un cobro del que ni siquiera se acordaba. Seis meses después todavía no tiene la bicicleta, pero conoce la cantidad exacta que le falta, y esa cifra se reduce cada lunes. Aprender a ver a dónde va el dinero pequeño es, quizá, la manera más sencilla de empezar a decidir sobre el grande."
        ),
        listOf(
            q(LITERAL, "Según el cálculo del texto, ¿cuánto gastaría Regina en un ciclo escolar?",
                "Seis mil pesos",
                "Ciento cincuenta pesos",
                "Doscientos pesos",
                "Tres mil pesos",
                "Treinta pesos por doscientos días de clases dan seis mil. Ciento cincuenta es el gasto de una semana y doscientos, lo que recibe.",
                "la cifra llega a seis mil pesos"),
            q(CAUSE_EFFECT, "¿Por qué el gasto hormiga suele pasar inadvertido?",
                "Porque cada compra es tan pequeña que no se recuerda",
                "Porque se paga con tarjeta y no con dinero en efectivo",
                "Porque los comercios ocultan el precio de los productos",
                "Porque ocurre solo unas pocas veces al año",
                "Al ser mínimo, cada gasto no se registra en la memoria, y lo que no se registra no se suma.",
                "Como cada gasto es mínimo, no lo registramos en la memoria"),
            q(VOCABULARY, "En el texto, ahorrar «por si acaso» quiere decir ahorrar…",
                "sin un propósito definido",
                "con una fecha establecida",
                "a escondidas de los demás",
                "solo cuando sobra dinero",
                "El texto lo opone a guardar para «algo concreto, con un precio y una fecha»: es ahorrar sin una meta clara.",
                "guardar dinero «por si acaso» resulta mucho más difícil"),
            q(INFERENCE, "¿Qué se busca al colocar los dulces junto a la caja?",
                "Provocar una compra rápida y poco pensada",
                "Facilitar que los clientes encuentren ofertas",
                "Evitar que los productos pequeños sean robados",
                "Cumplir una norma sobre alimentos envasados",
                "El texto dice que no es casualidad: todo está dispuesto para que se decida comprar deprisa y sin calcular.",
                "todo está pensado para que la decisión de comprar se tome deprisa y sin calcular"),
            q(MAIN_IDEA, "¿Cuál es la idea principal del texto?",
                "Los gastos pequeños y repetidos suman mucho si no se vigilan",
                "Los comercios engañan a los jóvenes con prácticas ilegales",
                "Ahorrar exige eliminar por completo los pequeños gustos",
                "Una bicicleta cuesta menos de lo que la gente imagina",
                "El texto aclara que esas prácticas no son ilegales y que no hay que renunciar a todo: lo central es el efecto acumulado.",
                "juntas pueden vaciar una despensa"),
            q(CRITICAL, "El texto afirma que las estrategias de los comercios no son ilegales. ¿Qué se desprende de ello?",
                "Que protegerse depende de la atención del comprador",
                "Que no influyen de verdad en lo que la gente compra",
                "Que deberían prohibirse en las tiendas escolares",
                "Que solo afectan a quienes reciben poco dinero",
                "Si son legales, nadie va a impedirlas: quien compra es quien tiene que detenerse a calcular.",
                "Nada de esto es ilegal, pero todo está pensado"),
            q(INFERENCE, "¿Qué muestra el final de la historia de Regina?",
                "Que controlar el gasto le dio un plan, aunque no la meta",
                "Que el método no funcionó porque sigue sin bicicleta",
                "Que dejó de comprar botanas de manera definitiva",
                "Que su madre decidió aumentarle la cantidad semanal",
                "Aún no tiene la bicicleta, pero sabe cuánto le falta y esa cantidad baja cada semana: ha ganado control, no el objeto.",
                "todavía no tiene la bicicleta, pero conoce la cantidad exacta que le falta")
        )
    )

    val readings: List<LongReading> = listOf(sleep, press, gps, axolotl, cinema, antSpending)

    fun byId(id: String): LongReading? = readings.firstOrNull { it.id == id }
}
