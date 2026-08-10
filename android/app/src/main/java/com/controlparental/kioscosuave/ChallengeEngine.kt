package com.controlparental.kioscosuave

import com.controlparental.kioscosuave.curriculum.Curriculum
import com.controlparental.kioscosuave.curriculum.ExerciseFormat
import com.controlparental.kioscosuave.curriculum.Sec1MathGenerator
import java.text.Normalizer
import kotlin.random.Random

/** Ejemplo resuelto (para el botón de ayuda 💡). */
data class WorkedExample(val title: String, val lines: List<String>)

data class MathQuestion(
    val question: String,
    val options: List<String>,
    val answer: String,
    val steps: List<String>,      // procedimiento paso a paso de ESTA pregunta
    val example: WorkedExample,   // ejemplo resuelto (otro caso) para la ayuda
    // Etiquetas de la Fase 1. Son nullables porque los generadores viejos
    // (preescolar/primaria) todavía no están mapeados a habilidades; en cuanto
    // lo estén, la memoria de la Fase 2 podrá rastrearlos igual.
    val skillId: String? = null,
    val format: com.controlparental.kioscosuave.curriculum.ExerciseFormat? = null
)

data class EnglishExercise(
    val instruction: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String,
    val phonetic: String          // pronunciación del verbo clave
)

data class ReadingPassage(
    val title: String,
    val text: String
)

data class SummaryResult(
    val approved: Boolean,
    val score: Int,
    val feedback: String,
    val suggestions: String
)

/**
 * Motor de retos 100% offline (v1 standalone). Genera operaciones y problemas
 * de contexto con procedimiento paso a paso, ejercicios de inglés con fonética
 * y lecturas (más amplias en secundaria).
 */
object ChallengeEngine {

    private val names = listOf("María", "Luis", "Ana", "Pedro", "Sofía", "Diego", "Lucía", "Mateo")
    private val things = listOf("manzanas", "canicas", "lápices", "galletas", "stickers", "monedas")

    // Vocabulario con dibujos para Preescolar/1º: (emoji, palabra, fonética, español).
    // Se declara aquí (antes de matemáticas) porque countEmojis reutiliza estos
    // mismos emoji para los ejercicios de contar -> un solo pozo de imágenes
    // sirve tanto para inglés como para matemáticas, con mucha más variedad
    // que los 8 objetos fijos de antes.
    // "emoji" es de mejor esfuerzo: si no hay un emoji Unicode claro para la
    // palabra, se deja "" — la palabra queda lista en el vocabulario para
    // emparejarse con una imagen real más adelante, pero mientras tanto no
    // participa en los ejercicios de dibujos (ver filtro en starterEnglish()).
    private data class VocabItem(val emoji: String, val word: String, val phon: String, val es: String)
    private val starterVocab = listOf(
        // --- Original (hoja "Listen, repeat and trace" + básicos) ---
        VocabItem("🐶", "dog", "/dɒg/ («dog»)", "perro"),
        VocabItem("🐱", "cat", "/kæt/ («cat»)", "gato"),
        VocabItem("🍎", "apple", "/ˈæpəl/ («ápol»)", "manzana"),
        VocabItem("☀️", "sun", "/sʌn/ («san»)", "sol"),
        VocabItem("🏠", "house", "/haʊs/ («jaus»)", "casa"),
        VocabItem("🐟", "fish", "/fɪʃ/ («fish»)", "pez"),
        VocabItem("🐦", "bird", "/bɜːrd/ («berd»)", "pájaro"),
        VocabItem("🥛", "milk", "/mɪlk/ («milk»)", "leche"),
        VocabItem("⚽", "ball", "/bɔːl/ («bol»)", "pelota"),
        VocabItem("🌙", "moon", "/muːn/ («mun»)", "luna"),
        VocabItem("💧", "water", "/ˈwɔːtər/ («uóter»)", "agua"),
        VocabItem("📖", "book", "/bʊk/ («buk»)", "libro"),
        VocabItem("😊", "face", "/feɪs/ («féis»)", "cara"),
        VocabItem("👁️", "eye", "/aɪ/ («ái»)", "ojo"),
        VocabItem("👃", "nose", "/noʊz/ («nóus»)", "nariz"),
        VocabItem("👄", "mouth", "/maʊθ/ («máuz»)", "boca"),
        VocabItem("👂", "ear", "/ɪr/ («íer»)", "oreja"),
        VocabItem("✋", "hand", "/hænd/ («jand»)", "mano"),

        // --- Animales (granja, selva, mar) ---
        VocabItem("🐘", "elephant", "/ˈɛlɪfənt/ («élefant»)", "elefante"),
        VocabItem("🦒", "giraffe", "/dʒəˈræf/ («chiráf»)", "jirafa"),
        VocabItem("🦁", "lion", "/ˈlaɪən/ («láion»)", "león"),
        VocabItem("🐯", "tiger", "/ˈtaɪgər/ («táiguer»)", "tigre"),
        VocabItem("🐻", "bear", "/bɛr/ («ber»)", "oso"),
        VocabItem("🐒", "monkey", "/ˈmʌŋki/ («mónki»)", "mono"),
        VocabItem("🐮", "cow", "/kaʊ/ («cáu»)", "vaca"),
        VocabItem("🐷", "pig", "/pɪg/ («pig»)", "cerdo"),
        VocabItem("🐑", "sheep", "/ʃiːp/ («ship»)", "oveja"),
        VocabItem("🐴", "horse", "/hɔːrs/ («jors»)", "caballo"),
        VocabItem("🦆", "duck", "/dʌk/ («dak»)", "pato"),
        VocabItem("🐸", "frog", "/frɒg/ («frog»)", "rana"),
        VocabItem("🐝", "bee", "/biː/ («bi»)", "abeja"),
        VocabItem("🦋", "butterfly", "/ˈbʌtərflaɪ/ («báterflai»)", "mariposa"),
        VocabItem("🐍", "snake", "/sneɪk/ («snéik»)", "serpiente"),
        VocabItem("🐢", "turtle", "/ˈtɜːrtl/ («tértel»)", "tortuga"),
        VocabItem("🐰", "rabbit", "/ˈræbɪt/ («rábit»)", "conejo"),
        VocabItem("🐭", "mouse", "/maʊs/ («máus»)", "ratón"),
        VocabItem("🦉", "owl", "/aʊl/ («ául»)", "búho"),
        VocabItem("🐧", "penguin", "/ˈpɛŋgwɪn/ («pénguin»)", "pingüino"),
        VocabItem("🐔", "chicken", "/ˈtʃɪkɪn/ («chíken»)", "gallina"),
        VocabItem("🐺", "wolf", "/wʊlf/ («wulf»)", "lobo"),
        VocabItem("🦊", "fox", "/fɒks/ («foks»)", "zorro"),
        VocabItem("🐨", "koala", "/koʊˈɑːlə/ («koála»)", "koala"),
        VocabItem("🐼", "panda", "/ˈpændə/ («panda»)", "panda"),
        VocabItem("🦘", "kangaroo", "/ˌkæŋgəˈruː/ («kanguerú»)", "canguro"),
        VocabItem("🐊", "crocodile", "/ˈkrɒkədaɪl/ («krókodail»)", "cocodrilo"),
        VocabItem("🦈", "shark", "/ʃɑːrk/ («shark»)", "tiburón"),
        VocabItem("🐳", "whale", "/weɪl/ («uéil»)", "ballena"),
        VocabItem("🐬", "dolphin", "/ˈdɒlfɪn/ («dólfin»)", "delfín"),
        VocabItem("🐙", "octopus", "/ˈɒktəpəs/ («óktopus»)", "pulpo"),
        VocabItem("🦀", "crab", "/kræb/ («krab»)", "cangrejo"),
        VocabItem("🐌", "snail", "/sneɪl/ («snéil»)", "caracol"),
        VocabItem("🐜", "ant", "/ænt/ («ant»)", "hormiga"),
        VocabItem("🦖", "dinosaur", "/ˈdaɪnəsɔːr/ («dáinosor»)", "dinosaurio"),
        VocabItem("🐿️", "squirrel", "/ˈskwɜːrəl/ («eskuérel»)", "ardilla"),
        VocabItem("🐫", "camel", "/ˈkæməl/ («kámel»)", "camello"),

        // --- Comida, frutas, verduras, bebidas ---
        VocabItem("🍌", "banana", "/bəˈnænə/ («banána»)", "plátano"),
        VocabItem("🍊", "orange", "/ˈɒrɪndʒ/ («óranch»)", "naranja"),
        VocabItem("🍓", "strawberry", "/ˈstrɔːbɛri/ («estróberi»)", "fresa"),
        VocabItem("🍇", "grape", "/greɪp/ («gréip»)", "uva"),
        VocabItem("🍉", "watermelon", "/ˈwɔːtərmɛlən/ («uótermelon»)", "sandía"),
        VocabItem("🍍", "pineapple", "/ˈpaɪnæpəl/ («páinapol»)", "piña"),
        VocabItem("🥭", "mango", "/ˈmæŋgoʊ/ («mángou»)", "mango"),
        VocabItem("🍋", "lemon", "/ˈlɛmən/ («lémon»)", "limón"),
        VocabItem("🥕", "carrot", "/ˈkærət/ («kárrot»)", "zanahoria"),
        VocabItem("🥔", "potato", "/pəˈteɪtoʊ/ («potéito»)", "papa"),
        VocabItem("🍅", "tomato", "/təˈmeɪtoʊ/ («toméito»)", "jitomate"),
        VocabItem("🌽", "corn", "/kɔːrn/ («korn»)", "elote"),
        VocabItem("🥦", "broccoli", "/ˈbrɒkəli/ («brókoli»)", "brócoli"),
        VocabItem("🧅", "onion", "/ˈʌnjən/ («onion»)", "cebolla"),
        VocabItem("🍞", "bread", "/brɛd/ («bred»)", "pan"),
        VocabItem("🧀", "cheese", "/tʃiːz/ («chis»)", "queso"),
        VocabItem("🥚", "egg", "/ɛg/ («eg»)", "huevo"),
        VocabItem("🍕", "pizza", "/ˈpiːtsə/ («pítsa»)", "pizza"),
        VocabItem("🎂", "cake", "/keɪk/ («kéik»)", "pastel"),
        VocabItem("🍪", "cookie", "/ˈkʊki/ («cúki»)", "galleta"),
        VocabItem("🍦", "ice cream", "/aɪs kriːm/ («áis krim»)", "helado"),
        VocabItem("🍚", "rice", "/raɪs/ («ráis»)", "arroz"),
        VocabItem("🌮", "taco", "/ˈtɑːkoʊ/ («táko»)", "taco"),
        VocabItem("🍔", "hamburger", "/ˈhæmbɜːrgər/ («jámburguer»)", "hamburguesa"),
        VocabItem("🌭", "hot dog", "/hɒt dɒg/ («jat dog»)", "hot dog"),
        VocabItem("🧃", "juice", "/dʒuːs/ («llus»)", "jugo"),
        VocabItem("☕", "coffee", "/ˈkɒfi/ («cófi»)", "café"),
        VocabItem("🍫", "chocolate", "/ˈtʃɒklət/ («chóklet»)", "chocolate"),
        VocabItem("🍬", "candy", "/ˈkændi/ («kándi»)", "dulce"),

        // --- Colores (cuadros de color) ---
        VocabItem("🟥", "red", "/rɛd/ («red»)", "rojo"),
        VocabItem("🟦", "blue", "/bluː/ («blu»)", "azul"),
        VocabItem("🟩", "green", "/griːn/ («grin»)", "verde"),
        VocabItem("🟨", "yellow", "/ˈjɛloʊ/ («iélou»)", "amarillo"),
        VocabItem("⬛", "black", "/blæk/ («blak»)", "negro"),
        VocabItem("⬜", "white", "/waɪt/ («uáit»)", "blanco"),
        VocabItem("🟧", "orange color", "/ˈɒrɪndʒ/ («óranch»)", "color naranja"),
        VocabItem("🟪", "purple", "/ˈpɜːrpəl/ («pérpol»)", "morado"),
        VocabItem("🟫", "brown", "/braʊn/ («bráun»)", "café (color)"),
        VocabItem("💗", "pink", "/pɪŋk/ («pink»)", "rosa"),

        // --- Formas ---
        VocabItem("⭕", "circle", "/ˈsɜːrkəl/ («sérkol»)", "círculo"),
        VocabItem("🔺", "triangle", "/ˈtraɪæŋgəl/ («tráiangol»)", "triángulo"),
        VocabItem("⭐", "star", "/stɑːr/ («estár»)", "estrella"),
        VocabItem("❤️", "heart", "/hɑːrt/ («jart»)", "corazón"),
        VocabItem("🔶", "diamond", "/ˈdaɪəmənd/ («dáiamond»)", "diamante"),
        VocabItem("", "square", "/skwɛr/ («eskuér»)", "cuadrado"),

        // --- Familia ---
        VocabItem("👩", "mom", "/mɑm/ («mam»)", "mamá"),
        VocabItem("👨", "dad", "/dæd/ («dad»)", "papá"),
        VocabItem("👦", "brother", "/ˈbrʌðər/ («bróder»)", "hermano"),
        VocabItem("👧", "sister", "/ˈsɪstər/ («síster»)", "hermana"),
        VocabItem("👶", "baby", "/ˈbeɪbi/ («béibi»)", "bebé"),
        VocabItem("👵", "grandma", "/ˈgrænmɑː/ («gránma»)", "abuela"),
        VocabItem("👴", "grandpa", "/ˈgrænpɑː/ («gránpa»)", "abuelo"),
        VocabItem("👨‍👩‍👧‍👦", "family", "/ˈfæməli/ («fámili»)", "familia"),

        // --- Cuerpo (además de la cara) ---
        VocabItem("🦶", "foot", "/fʊt/ («fut»)", "pie"),
        VocabItem("🦵", "leg", "/lɛg/ («leg»)", "pierna"),
        VocabItem("🦷", "tooth", "/tuːθ/ («tuz»)", "diente"),
        VocabItem("💪", "arm", "/ɑːrm/ («arm»)", "brazo"),
        VocabItem("", "hair", "/hɛr/ («jer»)", "cabello"),
        VocabItem("", "head", "/hɛd/ («jed»)", "cabeza"),

        // --- Ropa ---
        VocabItem("👕", "shirt", "/ʃɜːrt/ («shert»)", "camisa"),
        VocabItem("👖", "pants", "/pænts/ («pants»)", "pantalón"),
        VocabItem("👗", "dress", "/drɛs/ («dres»)", "vestido"),
        VocabItem("👟", "shoes", "/ʃuːz/ («shus»)", "zapatos"),
        VocabItem("🧢", "cap", "/kæp/ («kap»)", "gorra"),
        VocabItem("🧦", "socks", "/sɒks/ («soks»)", "calcetines"),
        VocabItem("🧥", "jacket", "/ˈdʒækɪt/ («cháket»)", "chaqueta"),
        VocabItem("👓", "glasses", "/ˈglæsɪz/ («gláses»)", "lentes"),
        VocabItem("🧣", "scarf", "/skɑːrf/ («eskarf»)", "bufanda"),
        VocabItem("🧤", "gloves", "/glʌvz/ («glavs»)", "guantes"),

        // --- Casa, muebles, cocina, baño ---
        VocabItem("🛏️", "bed", "/bɛd/ («bed»)", "cama"),
        VocabItem("🪑", "chair", "/tʃɛr/ («cher»)", "silla"),
        VocabItem("🚪", "door", "/dɔːr/ («dor»)", "puerta"),
        VocabItem("🪟", "window", "/ˈwɪndoʊ/ («uíndou»)", "ventana"),
        VocabItem("🔑", "key", "/kiː/ («ki»)", "llave"),
        VocabItem("💡", "lamp", "/læmp/ («lamp»)", "lámpara"),
        VocabItem("🛋️", "sofa", "/ˈsoʊfə/ («sófa»)", "sofá"),
        VocabItem("🛁", "bathtub", "/ˈbæθtʌb/ («báztab»)", "tina"),
        VocabItem("🪥", "toothbrush", "/ˈtuːθbrʌʃ/ («tuzbrash»)", "cepillo de dientes"),
        VocabItem("🧼", "soap", "/soʊp/ («sóup»)", "jabón"),
        VocabItem("🪞", "mirror", "/ˈmɪrər/ («mírror»)", "espejo"),
        VocabItem("", "table", "/ˈteɪbəl/ («téibol»)", "mesa"),

        // --- Escuela ---
        VocabItem("✏️", "pencil", "/ˈpɛnsəl/ («pénsol»)", "lápiz"),
        VocabItem("✂️", "scissors", "/ˈsɪzərz/ («sísors»)", "tijeras"),
        VocabItem("🖍️", "crayon", "/ˈkreɪɒn/ («kréion»)", "crayola"),
        VocabItem("📏", "ruler", "/ˈruːlər/ («rúler»)", "regla"),
        VocabItem("🎒", "backpack", "/ˈbækpæk/ («bákpak»)", "mochila"),
        VocabItem("📓", "notebook", "/ˈnoʊtbʊk/ («nóutbuk»)", "cuaderno"),

        // --- Transporte ---
        VocabItem("🚗", "car", "/kɑːr/ («kar»)", "carro"),
        VocabItem("🚌", "bus", "/bʌs/ («bas»)", "autobús"),
        VocabItem("🚂", "train", "/treɪn/ («tréin»)", "tren"),
        VocabItem("✈️", "airplane", "/ˈɛərpleɪn/ («érplein»)", "avión"),
        VocabItem("⛵", "boat", "/boʊt/ («bóut»)", "barco"),
        VocabItem("🚲", "bike", "/baɪk/ («báik»)", "bicicleta"),
        VocabItem("🏍️", "motorcycle", "/ˈmoʊtərsaɪkəl/ («móutorsaikol»)", "motocicleta"),
        VocabItem("🚚", "truck", "/trʌk/ («trak»)", "camión"),
        VocabItem("🚁", "helicopter", "/ˈhɛlɪkɒptər/ («jélikopter»)", "helicóptero"),
        VocabItem("🚀", "rocket", "/ˈrɒkɪt/ («róket»)", "cohete"),

        // --- Naturaleza y clima ---
        VocabItem("🌳", "tree", "/triː/ («tri»)", "árbol"),
        VocabItem("🌸", "flower", "/ˈflaʊər/ («fláuer»)", "flor"),
        VocabItem("🍃", "leaf", "/liːf/ («lif»)", "hoja"),
        VocabItem("⛰️", "mountain", "/ˈmaʊntən/ («máunten»)", "montaña"),
        VocabItem("🌈", "rainbow", "/ˈreɪnboʊ/ («réinbou»)", "arcoíris"),
        VocabItem("🌧️", "rain", "/reɪn/ («réin»)", "lluvia"),
        VocabItem("❄️", "snow", "/snoʊ/ («snóu»)", "nieve"),
        VocabItem("☁️", "cloud", "/klaʊd/ («cláud»)", "nube"),
        VocabItem("🔥", "fire", "/ˈfaɪər/ («fáier»)", "fuego"),
        VocabItem("🌊", "sea", "/siː/ («si»)", "mar"),

        // --- Emociones ---
        VocabItem("😊", "happy", "/ˈhæpi/ («jápi»)", "feliz"),
        VocabItem("😢", "sad", "/sæd/ («sad»)", "triste"),
        VocabItem("😠", "angry", "/ˈæŋgri/ («ángri»)", "enojado"),
        VocabItem("😲", "surprised", "/sərˈpraɪzd/ («serpráisd»)", "sorprendido"),
        VocabItem("😱", "scared", "/skɛrd/ («eskérd»)", "asustado"),
        VocabItem("😴", "sleepy", "/ˈsliːpi/ («slípi»)", "con sueño"),

        // --- Números en palabra (1-10) ---
        VocabItem("1️⃣", "one", "/wʌn/ («uán»)", "uno"),
        VocabItem("2️⃣", "two", "/tuː/ («tu»)", "dos"),
        VocabItem("3️⃣", "three", "/θriː/ («zri»)", "tres"),
        VocabItem("4️⃣", "four", "/fɔːr/ («for»)", "cuatro"),
        VocabItem("5️⃣", "five", "/faɪv/ («fáiv»)", "cinco"),
        VocabItem("6️⃣", "six", "/sɪks/ («siks»)", "seis"),
        VocabItem("7️⃣", "seven", "/ˈsɛvən/ («séven»)", "siete"),
        VocabItem("8️⃣", "eight", "/eɪt/ («éit»)", "ocho"),
        VocabItem("9️⃣", "nine", "/naɪn/ («náin»)", "nueve"),
        VocabItem("🔟", "ten", "/tɛn/ («ten»)", "diez"),

        // --- Profesiones ---
        VocabItem("🧑‍⚕️", "doctor", "/ˈdɒktər/ («dóktor»)", "doctor(a)"),
        VocabItem("🧑‍🏫", "teacher", "/ˈtiːtʃər/ («tícher»)", "maestro(a)"),
        VocabItem("🧑‍🚒", "firefighter", "/ˈfaɪərfaɪtər/ («fáierfaiter»)", "bombero"),
        VocabItem("👮", "police officer", "/pəˈliːs/ («polís»)", "policía"),
        VocabItem("🧑‍🍳", "chef", "/ʃɛf/ («shef»)", "cocinero(a)"),
        VocabItem("🧑‍🌾", "farmer", "/ˈfɑːrmər/ («fármer»)", "granjero(a)"),
        VocabItem("🧑‍🚀", "astronaut", "/ˈæstrənɔːt/ («ástronot»)", "astronauta"),

        // --- Música ---
        VocabItem("🎸", "guitar", "/gɪˈtɑːr/ («guitár»)", "guitarra"),
        VocabItem("🥁", "drum", "/drʌm/ («dram»)", "tambor"),
        VocabItem("🎹", "piano", "/piˈænoʊ/ («piáno»)", "piano"),
        VocabItem("🎤", "microphone", "/ˈmaɪkrəfoʊn/ («máikrofon»)", "micrófono"),
        VocabItem("🎺", "trumpet", "/ˈtrʌmpɪt/ («trámpet»)", "trompeta"),

        // --- Deportes y juguetes ---
        VocabItem("🏀", "basketball", "/ˈbæskɪtbɔːl/ («básketbol»)", "básquetbol"),
        VocabItem("🎾", "tennis", "/ˈtɛnɪs/ («ténis»)", "tenis"),
        VocabItem("🪁", "kite", "/kaɪt/ («káit»)", "papalote"),
        VocabItem("🧱", "blocks", "/blɒks/ («bloks»)", "bloques"),
        VocabItem("🎈", "balloon", "/bəˈluːn/ («balún»)", "globo"),

        // --- Electrónica ---
        VocabItem("📺", "television", "/ˈtɛlɪvɪʒən/ («télevisión»)", "televisión"),
        VocabItem("📱", "phone", "/foʊn/ («fóun»)", "teléfono"),
        VocabItem("💻", "computer", "/kəmˈpjuːtər/ («kompiúter»)", "computadora"),
        VocabItem("📷", "camera", "/ˈkæmərə/ («kámera»)", "cámara")
    )

    // ---------- Ejemplos resueltos para el botón de ayuda ----------
    // Ejemplos para OPERACIONES DIRECTAS (mismo formato "¿Cuánto es A+B?").
    private val EX_ADD_DIRECT = WorkedExample(
        "Ejemplo resuelto: suma",
        listOf("¿Cuánto es 7 + 5?", "Sumamos:  7 + 5 = 12", "Respuesta: 12")
    )
    private val EX_SUB_DIRECT = WorkedExample(
        "Ejemplo resuelto: resta",
        listOf("¿Cuánto es 15 − 6?", "Restamos:  15 − 6 = 9", "Respuesta: 9")
    )
    private val EX_MUL_DIRECT = WorkedExample(
        "Ejemplo resuelto: multiplicación",
        listOf("¿Cuánto es 6 × 4?", "Multiplicamos:  6 × 4 = 24", "Respuesta: 24")
    )
    private val EX_DIV_DIRECT = WorkedExample(
        "Ejemplo resuelto: división",
        listOf("¿Cuánto es 24 ÷ 6?", "Dividimos:  24 ÷ 6 = 4", "Respuesta: 4")
    )

    // Ejemplos para PROBLEMAS DE CONTEXTO (mismo formato de historia/situación).
    private val EX_ADD = WorkedExample(
        "Ejemplo resuelto: suma",
        listOf("María tenía 7 y consiguió 5 más.", "Sumamos:  7 + 5 = 12", "Respuesta: 12")
    )
    private val EX_SUB = WorkedExample(
        "Ejemplo resuelto: resta",
        listOf("Luis tenía 15 y regaló 6.", "Restamos:  15 − 6 = 9", "Respuesta: 9")
    )
    private val EX_MUL = WorkedExample(
        "Ejemplo resuelto: multiplicación",
        listOf("6 cajas con 4 lápices cada una.", "Multiplicamos:  6 × 4 = 24", "Respuesta: 24")
    )
    private val EX_DIV = WorkedExample(
        "Ejemplo resuelto: división",
        listOf("Repartir 24 dulces entre 6 niños.", "Dividimos:  24 ÷ 6 = 4", "A cada uno le tocan 4")
    )
    // Ejemplo para ECUACIÓN DIRECTA ("Resuelve para X: ...").
    private val EX_LINEAR = WorkedExample(
        "Ejemplo resuelto: despejar X",
        listOf(
            "Resuelve:  4X + 8 = 44",
            "1) El 8 está sumando → pasa restando:",
            "     4X = 44 − 8  →  4X = 36",
            "2) El 4 está multiplicando → pasa dividiendo:",
            "     X = 36 ÷ 4",
            "Respuesta:  X = 9"
        )
    )
    // Ejemplo para "PIENSO UN NÚMERO..." (mismo formato que wordLinear).
    private val EX_WORD_NUMBER = WorkedExample(
        "Ejemplo resuelto: pienso un número",
        listOf(
            "Pienso un número, lo multiplico por 3 y le sumo 4;",
            "obtengo 19. ¿Qué número pensé?",
            "Sea n el número:  3·n + 4 = 19",
            "1) El 4 está sumando → pasa restando:",
            "     3·n = 19 − 4 = 15",
            "2) El 3 está multiplicando → pasa dividiendo:",
            "     n = 15 ÷ 3",
            "Respuesta:  n = 5"
        )
    )
    private val EX_PURCHASE = WorkedExample(
        "Ejemplo resuelto: precio por unidad",
        listOf(
            "Compró 3 libretas y pagó \$62, con \$14 de envío.",
            "1) Quita el envío:  62 − 14 = 48",
            "2) Divide entre la cantidad:  48 ÷ 3 = 16",
            "Cada libreta cuesta \$16"
        )
    )

    /** Guía de ayuda de inglés (mini-gramática general por tiempos). */
    val englishHelp = WorkedExample(
        "Mini-guía de inglés",
        listOf(
            "PASADO: verbo + -ed (watched) o irregular (go→went, eat→ate).",
            "     'Yesterday I went home.'",
            "FUTURO: will + verbo, o 'be going to'.",
            "     'Tomorrow I will play.' / 'I am going to study.'",
            "PRESENTE: con he/she/it el verbo lleva -s.",
            "     'She goes to school.' / '¿Do you like...?'",
            "PISTAS: yesterday=pasado, tomorrow/next=futuro,",
            "     every day=presente."
        )
    )

    // ---------------- MATEMÁTICAS ----------------
    fun generateMath(difficulty: Difficulty, exclude: String? = null): MathQuestion {
        val generators: List<() -> MathQuestion> = when (difficulty) {
            Difficulty.STARTER ->
                listOf(
                    ::countObjects, ::addObjects, ::subObjects, ::whichSumShown,
                    ::moreOrLess, ::numberSequence, ::patternNext, ::biggerNumber
                )
            Difficulty.EASY ->
                listOf(::opAddition, ::opSubtraction, ::wordAddition, ::wordSubtraction)
            Difficulty.MEDIUM ->
                listOf(::opMultiplication, ::opDivision, ::wordMultiplication, ::wordDivision)
            // SECUNDARIA: antes eran 4 plantillas fijas (ecuación, compra,
            // descuento) que cubrían ~1 de los 13 temas que evalúa la escuela.
            // Ahora el grueso del pozo es el temario real de 1° de secundaria
            // (ver curriculum/Skill.kt), y las plantillas de ecuaciones se
            // conservan porque ese tema todavía no está en el temario nuevo.
            //
            // wordPercentage se retiró: Sec1Porcentajes.descuento hace lo mismo
            // con distractores que sí corresponden a los errores típicos.
            Difficulty.HARD -> buildList<() -> MathQuestion> {
                Sec1MathGenerator.implemented.forEach { skill ->
                    add { Sec1MathGenerator.generate(skill) ?: opLinear() }
                }
                add(::opLinear)
                add(::wordLinear)
                add(::wordPurchase)
            }
        }
        var q = generators.random().invoke()
        var tries = 0
        while (exclude != null && q.question == exclude && tries < 6) {
            q = generators.random().invoke()
            tries++
        }
        return q
    }

    // --- Preescolar / 1º: contar objetos con dibujos ---
    // Reutiliza el vocabulario de inglés como pozo de objetos para contar: mucha
    // más variedad (antes 8 objetos fijos) y refuerza el mismo vocabulario que
    // el niño practica en inglés (ver starterVocab más arriba).
    private val countEmojis = starterVocab.mapNotNull { it.emoji.takeIf(String::isNotBlank) }.distinct()

    private val EX_COUNT = WorkedExample(
        "Ejemplo: contar",
        listOf(
            "¿Cuántas hay?  🍎 🍎 🍎",
            "Cuenta una por una con el dedo:",
            "1... 2... 3",
            "¡Son 3!"
        )
    )

    private fun countObjects(): MathQuestion {
        val n = Random.nextInt(2, 10)
        val e = countEmojis.random()
        val row = List(n) { e }.joinToString(" ")
        return MathQuestion(
            "¿Cuántos hay?\n\n$row",
            distinctOptions(n, listOf(n + 1, n - 1, n + 2)), n.toString(),
            listOf("Cuenta uno por uno con el dedo:", row, "¡Son $n!"), EX_COUNT)
    }

    private fun addObjects(): MathQuestion {
        val a = Random.nextInt(1, 5); val b = Random.nextInt(1, 5); val ans = a + b
        val e = countEmojis.random()
        val rowA = List(a) { e }.joinToString(" ")
        val rowB = List(b) { e }.joinToString(" ")
        return MathQuestion(
            "$rowA  y  $rowB\n\n¿Cuántos hay en total?",
            distinctOptions(ans, listOf(ans + 1, ans - 1, ans + 2)), ans.toString(),
            listOf("Junta los dos grupos y cuenta todos:", "$rowA  $rowB", "¡Son $ans!"), EX_COUNT)
    }

    private val EX_RESTA = WorkedExample(
        "Ejemplo: restar contando hacia atrás",
        listOf(
            "Resta:  4 − 1 = ?",
            "Puedes contar hacia atrás para restar.",
            "Empieza desde el 4 y cuenta 1 hacia atrás:",
            "4... 3",
            "¡La respuesta es 3!"
        )
    )

    /** Resta visual: había N, se van B, ¿cuántos quedan? (contar hacia atrás). */
    private fun subObjects(): MathQuestion {
        val a = Random.nextInt(3, 10); val b = Random.nextInt(1, minOf(a, 4))
        val ans = a - b
        val e = countEmojis.random()
        val row = List(a) { e }.joinToString(" ")
        val backwards = (a downTo ans).joinToString("... ")
        return MathQuestion(
            "Hay $a. Se van $b.\n\n$row\n\n¿Cuántos quedan?",
            distinctOptions(ans, listOf(ans + 1, ans - 1, ans + 2)), ans.toString(),
            listOf(
                "Cuenta hacia atrás desde $a, $b ${if (b == 1) "vez" else "veces"}:",
                backwards,
                "¡Quedan $ans!"
            ), EX_RESTA)
    }

    private val EX_QUE_SUMA = WorkedExample(
        "Ejemplo: ¿qué suma muestra el dibujo?",
        listOf(
            "🔵 🔵    🟠",
            "Cuenta los azules: 2. Cuenta los naranjas: 1.",
            "2 + 1 = 3",
            "La suma correcta es «2 + 1 = 3»."
        )
    )

    /** Como IXL: muestra dos grupos y las opciones son ecuaciones completas. */
    private fun whichSumShown(): MathQuestion {
        val a = Random.nextInt(2, 6); val b = Random.nextInt(1, 5)
        val rowA = List(a) { "🔵" }.joinToString(" ")
        val rowB = List(b) { "🟠" }.joinToString(" ")
        val correct = "$a + $b = ${a + b}"
        val opts = LinkedHashSet<String>()
        opts.add(correct)
        opts.add("$a + ${b + 1} = ${a + b + 1}")
        opts.add("${a + 1} + $b = ${a + b + 1}")
        opts.add("$a + $b = ${a + b + 1}")
        return MathQuestion(
            "¿Qué suma muestra este dibujo?\n\n$rowA   $rowB",
            opts.toList().shuffled(), correct,
            listOf(
                "Cuenta los azules: $a. Cuenta los naranjas: $b.",
                "$a + $b = ${a + b}"
            ), EX_QUE_SUMA)
    }

    private val EX_COMPARA = WorkedExample(
        "Ejemplo: ¿hay más?",
        listOf(
            "¿Hay más 🍎 que 🍌?",
            "🍎 🍎 🍎",
            "🍌 🍌",
            "Cuenta cada fila: 3 manzanas y 2 plátanos.",
            "3 es más que 2 → la respuesta es «sí»."
        )
    )

    /** Comparación sí/no: ¿hay más/menos X que Y? (dos filas de dibujos). */
    private fun moreOrLess(): MathQuestion {
        val e1 = countEmojis.random()
        var e2 = countEmojis.random()
        while (e2 == e1) e2 = countEmojis.random()
        val n1 = Random.nextInt(1, 6); var n2 = Random.nextInt(1, 6)
        while (n2 == n1) n2 = Random.nextInt(1, 6)
        val askMore = Random.nextBoolean()
        val word = if (askMore) "más" else "menos"
        val answerYes = if (askMore) n1 > n2 else n1 < n2
        val row1 = List(n1) { e1 }.joinToString(" ")
        val row2 = List(n2) { e2 }.joinToString(" ")
        return MathQuestion(
            "¿Hay $word $e1 que $e2?\n\n$row1\n$row2",
            listOf("sí", "no"), if (answerYes) "sí" else "no",
            listOf(
                "Cuenta cada fila: $n1 $e1 y $n2 $e2.",
                "$n1 ${if (n1 > n2) "es más que" else "es menos que"} $n2 → «${if (answerYes) "sí" else "no"}»."
            ), EX_COMPARA)
    }

    /** Secuencia numérica con hueco: 3, 4, _, 6. */
    private fun numberSequence(): MathQuestion {
        val start = Random.nextInt(1, 7)
        val blankAt = Random.nextInt(1, 3) // posición 1 o 2 de 4
        val nums = (start until start + 4).toList()
        val shown = nums.mapIndexed { i, n -> if (i == blankAt) "_" else n.toString() }
        val ans = nums[blankAt]
        return MathQuestion(
            "¿Qué número falta?\n\n${shown.joinToString(",  ")}",
            distinctOptions(ans, listOf(ans + 1, ans - 1, ans + 2)), ans.toString(),
            listOf(
                "Cuenta en orden: ${nums.joinToString(", ")}.",
                "El número que falta es $ans."
            ),
            WorkedExample("Ejemplo: el número que falta",
                listOf("1, 2, _, 4", "Cuenta: 1, 2, 3, 4...", "¡Falta el 3!"))
        )
    }

    /** Patrón AB: 🔴 🔵 🔴 🔵 🔴 _ ¿qué sigue? */
    private fun patternNext(): MathQuestion {
        val pool = listOf("🔴", "🔵", "🟡", "🟢", "⭐", "🌸", "⚽", "🍎").shuffled()
        val a = pool[0]; val b = pool[1]
        val seq = listOf(a, b, a, b, a)
        val ans = b
        val options = (listOf(a, b) + pool.drop(2).take(2)).shuffled()
        return MathQuestion(
            "¿Qué sigue en el patrón?\n\n${seq.joinToString("  ")}  _",
            options, ans,
            listOf(
                "El patrón se repite: $a $b $a $b...",
                "Después de $a sigue $b."
            ),
            WorkedExample("Ejemplo: patrones",
                listOf("🔴 🔵 🔴 🔵 🔴 _", "Se repite rojo, azul, rojo, azul...", "Después del 🔴 sigue el 🔵."))
        )
    }

    private fun biggerNumber(): MathQuestion {
        val nums = (1..9).shuffled().take(4)
        val ans = nums.max()
        return MathQuestion(
            "¿Cuál número es el MÁS GRANDE?",
            nums.map { it.toString() }.shuffled(), ans.toString(),
            listOf("De estos números, el más grande es $ans."),
            WorkedExample("Ejemplo: el más grande",
                listOf("Entre 2, 5 y 3...", "el más grande es 5,", "porque 5 tiene más que 2 y que 3."))
        )
    }

    // --- Operaciones directas ---
    private fun opAddition(): MathQuestion {
        val a = Random.nextInt(3, 15); val b = Random.nextInt(2, 11); val ans = a + b
        return MathQuestion("¿Cuánto es $a + $b?",
            distinctOptions(ans, listOf(ans + 2, ans - 3, ans + 5)), ans.toString(),
            listOf("Sumamos las cantidades:", "$a + $b = $ans"), EX_ADD_DIRECT)
    }

    private fun opSubtraction(): MathQuestion {
        val a = Random.nextInt(8, 20); val b = Random.nextInt(2, a); val ans = a - b
        return MathQuestion("¿Cuánto es $a − $b?",
            distinctOptions(ans, listOf(ans + 2, ans + 1, ans + 4)), ans.toString(),
            listOf("Restamos:", "$a − $b = $ans"), EX_SUB_DIRECT)
    }

    private fun opMultiplication(): MathQuestion {
        val a = Random.nextInt(4, 12); val b = Random.nextInt(3, 10); val ans = a * b
        return MathQuestion("¿Cuánto es $a × $b?",
            distinctOptions(ans, listOf(ans + 4, ans - 6, ans + 10)), ans.toString(),
            listOf("Multiplicamos:", "$a × $b = $ans"), EX_MUL_DIRECT)
    }

    private fun opDivision(): MathQuestion {
        val b = Random.nextInt(2, 10); val ans = Random.nextInt(2, 10); val a = b * ans
        return MathQuestion("¿Cuánto es $a ÷ $b?",
            distinctOptions(ans, listOf(ans + 1, ans + 2, ans - 1)), ans.toString(),
            listOf("Dividimos:", "$a ÷ $b = $ans"), EX_DIV_DIRECT)
    }

    private fun opLinear(): MathQuestion {
        val x = Random.nextInt(3, 10); val coeff = Random.nextInt(2, 5); val c = Random.nextInt(1, 11)
        val right = coeff * x + c
        return MathQuestion("Resuelve para X:  ${coeff}X + $c = $right",
            distinctOptions(x, listOf(x + 2, x - 1, x + 3)), x.toString(),
            listOf(
                "${coeff}X + $c = $right",
                "1) El $c está sumando → pasa restando:",
                "     ${coeff}X = $right − $c = ${right - c}",
                "2) El $coeff está multiplicando → pasa dividiendo:",
                "     X = ${right - c} ÷ $coeff",
                "X = $x"
            ), EX_LINEAR, Curriculum.EC_LINEAL.id, ExerciseFormat.DIRECTO)
    }

    // --- Situaciones (problemas de contexto) ---
    private fun wordAddition(): MathQuestion {
        val n = names.random(); val o = things.random()
        val a = Random.nextInt(3, 15); val b = Random.nextInt(2, 10); val ans = a + b
        return MathQuestion(
            "$n tenía $a $o y consiguió $b más. ¿Cuántas $o tiene ahora?",
            distinctOptions(ans, listOf(ans + 2, ans - 3, ans + 4)), ans.toString(),
            listOf("Hay que sumar lo que tenía y lo que consiguió:", "$a + $b = $ans"), EX_ADD)
    }

    private fun wordSubtraction(): MathQuestion {
        val n = names.random(); val o = things.random()
        val a = Random.nextInt(8, 20); val b = Random.nextInt(2, a); val ans = a - b
        return MathQuestion(
            "$n tenía $a $o y regaló $b. ¿Cuántas $o le quedan?",
            distinctOptions(ans, listOf(ans + 2, ans + 1, ans + 3)), ans.toString(),
            listOf("Hay que restar lo que regaló:", "$a − $b = $ans"), EX_SUB)
    }

    private fun wordMultiplication(): MathQuestion {
        val o = things.random(); val boxes = Random.nextInt(3, 9); val per = Random.nextInt(3, 9)
        val ans = boxes * per
        return MathQuestion(
            "Cada caja trae $per $o. Si hay $boxes cajas, ¿cuántas $o hay en total?",
            distinctOptions(ans, listOf(ans + boxes, ans - per, ans + 5)), ans.toString(),
            listOf("Multiplicamos cajas por lo de cada caja:", "$boxes × $per = $ans"), EX_MUL)
    }

    private fun wordDivision(): MathQuestion {
        val o = things.random(); val per = Random.nextInt(2, 9); val kids = Random.nextInt(2, 8)
        val total = per * kids; val ans = per
        return MathQuestion(
            "Se reparten $total $o en partes iguales entre $kids niños. ¿Cuántas le tocan a cada uno?",
            distinctOptions(ans, listOf(ans + 1, ans + 2, ans - 1)), ans.toString(),
            listOf("Dividimos el total entre los niños:", "$total ÷ $kids = $ans"), EX_DIV)
    }

    private fun wordLinear(): MathQuestion {
        val x = Random.nextInt(2, 11); val m = Random.nextInt(2, 5); val b = Random.nextInt(1, 10)
        val result = m * x + b
        return MathQuestion(
            "Pienso un número, lo multiplico por $m y le sumo $b; obtengo $result. ¿Qué número pensé?",
            distinctOptions(x, listOf(x + 1, x + 2, x - 1)), x.toString(),
            listOf(
                "Sea n el número:  ${m}·n + $b = $result",
                "1) El $b está sumando → pasa restando:",
                "     ${m}·n = $result − $b = ${result - b}",
                "2) El $m está multiplicando → pasa dividiendo:",
                "     n = ${result - b} ÷ $m",
                "n = $x"
            ), EX_WORD_NUMBER, Curriculum.EC_PROBLEMA.id, ExerciseFormat.CONTEXTO)
    }

    private fun wordPurchase(): MathQuestion {
        val n = names.random(); val count = Random.nextInt(2, 6)
        val unit = Random.nextInt(8, 20); val ship = Random.nextInt(5, 15)
        val total = count * unit + ship; val ans = unit
        return MathQuestion(
            "$n compró $count cuadernos y pagó \$$total en total, incluidos \$$ship de envío. ¿Cuánto costó cada cuaderno?",
            distinctOptions(ans, listOf(ans + 2, ans - 1, ans + 4)), ans.toString(),
            listOf(
                "Total = \$$total, envío = \$$ship, cantidad = $count",
                "1) Quita el envío:  $total − $ship = ${total - ship}",
                "2) Divide entre la cantidad:  ${total - ship} ÷ $count = $unit",
                "Cada cuaderno cuesta \$$unit"
            ), EX_PURCHASE, Curriculum.PROP_VALOR_UNITARIO.id, ExerciseFormat.CONTEXTO)
    }

    private fun distinctOptions(correct: Int, distractors: List<Int>): List<String> {
        val set = LinkedHashSet<Int>()
        set.add(correct)
        distractors.forEach { if (it != correct && it >= 0) set.add(it) }
        var filler = correct + 1
        while (set.size < 4) { if (filler != correct) set.add(filler); filler++ }
        return set.toList().shuffled().map { it.toString() }
    }

    // ---------------- INGLÉS (currículo rotativo estilo Duolingo) ----------------
    // Cada día toca UNA unidad distinta (rotación determinística por fecha):
    // pasado, futuro, irregulares, presente, y días temáticos de vocabulario.
    data class EnglishUnit(val title: String, val bank: List<EnglishExercise>)

    private val englishUnits = listOf(
        EnglishUnit("Viaje al pasado (Past Simple)", listOf(
            EnglishExercise(
                "Elige la forma correcta del verbo 'run' en pasado.",
                "Yesterday, Liam ______ to school because he was late.",
                listOf("runned", "ran", "runs", "running"), "ran",
                "El pasado de 'run' (correr) es irregular: 'ran'.",
                "ran = /ræn/ («ran»)"
            ),
            EnglishExercise(
                "Elige la forma correcta en pasado del verbo 'go'.",
                "Last weekend, we ______ to the beach with our dog.",
                listOf("goed", "goes", "went", "going"), "went",
                "El pasado de 'go' (ir) es irregular: 'went'.",
                "went = /wɛnt/ («uént»)"
            ),
            EnglishExercise(
                "Identifica el verbo en pasado simple de la oración.",
                "Which word is in the past tense? 'She watched a movie.'",
                listOf("She", "watched", "movie", "a"), "watched",
                "'watched' es el pasado regular de 'watch' (mirar).",
                "watched = /wɒtʃt/ («uótcht»)"
            ),
            EnglishExercise(
                "Traduce usando el tiempo pasado.",
                "Nosotros comimos manzanas ayer.",
                listOf(
                    "We ate apples yesterday", "We eat apples yesterday",
                    "We eaten apples yesterday", "We eated apples yesterday"
                ), "We ate apples yesterday",
                "'eat' es irregular: su pasado es 'ate'. 'ayer' = 'yesterday'.",
                "ate = /eɪt/ («éit»)"
            ),
            EnglishExercise(
                "Completa con el pasado de 'see'.",
                "I ______ a beautiful bird this morning.",
                listOf("seed", "saw", "seen", "sees"), "saw",
                "El pasado de 'see' (ver) es irregular: 'saw'.",
                "saw = /sɔː/ («so»)"
            )
        )),

        EnglishUnit("El futuro (will / going to)", listOf(
            EnglishExercise(
                "Completa la frase sobre el futuro.",
                "Tomorrow, I ______ visit my grandma.",
                listOf("will", "did", "was", "am"), "will",
                "'will' + verbo expresa futuro: 'I will visit' = visitaré.",
                "will = /wɪl/ («uíl»)"
            ),
            EnglishExercise(
                "Completa con 'going to'.",
                "She is ______ to study tonight.",
                listOf("going", "goes", "went", "go"), "going",
                "'be going to' = plan futuro: 'is going to study' = va a estudiar.",
                "going = /ˈgoʊɪŋ/ («góuing»)"
            ),
            EnglishExercise(
                "Traduce al inglés (futuro).",
                "Yo comeré pizza mañana.",
                listOf(
                    "I will eat pizza tomorrow", "I ate pizza tomorrow",
                    "I eat pizza yesterday", "I eating pizza tomorrow"
                ), "I will eat pizza tomorrow",
                "Futuro con 'will' + verbo base: will eat. 'mañana' = 'tomorrow'.",
                "tomorrow = /təˈmɒroʊ/ («tumórrou»)"
            ),
            EnglishExercise(
                "Completa la frase sobre el futuro.",
                "They ______ play soccer next Saturday.",
                listOf("will", "played", "was", "were"), "will",
                "'next Saturday' (el próximo sábado) pide futuro: 'will play'.",
                "will play = /wɪl pleɪ/ («uíl pléi»)"
            ),
            EnglishExercise(
                "¿Cuál frase habla del FUTURO?",
                "Choose the sentence about the future.",
                listOf(
                    "We will travel next year", "We traveled last year",
                    "We travel every year", "We were traveling"
                ), "We will travel next year",
                "'will travel' + 'next year' = viajaremos el próximo año.",
                "travel = /ˈtrævəl/ («trável»)"
            )
        )),

        EnglishUnit("Verbos irregulares", listOf(
            EnglishExercise(
                "¿Cuál es el pasado de 'buy' (comprar)?",
                "Yesterday, mom ______ tortillas at the market.",
                listOf("buyed", "bought", "buys", "buying"), "bought",
                "'buy' es irregular: su pasado es 'bought'.",
                "bought = /bɔːt/ («bot»)"
            ),
            EnglishExercise(
                "¿Cuál es el pasado de 'make' (hacer)?",
                "He ______ a beautiful drawing in class.",
                listOf("maked", "made", "makes", "making"), "made",
                "'make' es irregular: su pasado es 'made'.",
                "made = /meɪd/ («méid»)"
            ),
            EnglishExercise(
                "¿Cuál es el pasado de 'have' (tener)?",
                "We ______ a great time at the party.",
                listOf("haved", "had", "has", "having"), "had",
                "'have' es irregular: su pasado es 'had'.",
                "had = /hæd/ («jad»)"
            ),
            EnglishExercise(
                "¿Cuál es el pasado de 'take' (tomar/llevar)?",
                "She ______ the bus to school this morning.",
                listOf("taked", "took", "takes", "taking"), "took",
                "'take' es irregular: su pasado es 'took'.",
                "took = /tʊk/ («tuk»)"
            ),
            EnglishExercise(
                "¿Cuál es el pasado de 'come' (venir)?",
                "My cousins ______ to visit us last month.",
                listOf("comed", "came", "comes", "coming"), "came",
                "'come' es irregular: su pasado es 'came'.",
                "came = /keɪm/ («kéim»)"
            )
        )),

        EnglishUnit("El presente (Present Simple)", listOf(
            EnglishExercise(
                "Completa en presente (tercera persona).",
                "She ______ to school every day.",
                listOf("goes", "go", "went", "going"), "goes",
                "Con he/she/it el verbo lleva -s/-es: 'she goes'.",
                "goes = /goʊz/ («góus»)"
            ),
            EnglishExercise(
                "Completa en presente.",
                "I ______ breakfast at seven o'clock.",
                listOf("have", "has", "had", "having"), "have",
                "Con 'I' se usa 'have' (has es solo para he/she/it).",
                "have = /hæv/ («jav»)"
            ),
            EnglishExercise(
                "Completa la pregunta.",
                "______ you like apples?",
                listOf("Do", "Does", "Did", "Is"), "Do",
                "Preguntas en presente con you/we/they usan 'Do'.",
                "do = /duː/ («du»)"
            ),
            EnglishExercise(
                "Traduce al inglés (presente).",
                "Nosotros vivimos en México.",
                listOf(
                    "We live in Mexico", "We lived in Mexico",
                    "We living in Mexico", "We lives in Mexico"
                ), "We live in Mexico",
                "Presente simple: 'we live'. Sin -s porque no es he/she/it.",
                "live = /lɪv/ («liv»)"
            ),
            EnglishExercise(
                "Completa en presente (tercera persona).",
                "My brother ______ soccer on Sundays.",
                listOf("plays", "play", "played", "playing"), "plays",
                "Con 'my brother' (él) el verbo lleva -s: 'plays'.",
                "plays = /pleɪz/ («pléis»)"
            )
        )),

        EnglishUnit("Un día de escuela (vocabulario)", listOf(
            EnglishExercise(
                "¿Cómo se dice 'mochila' en inglés?",
                "I carry my books in my ______.",
                listOf("backpack", "pencil", "desk", "lunch"), "backpack",
                "'backpack' = mochila.",
                "backpack = /ˈbækpæk/ («bákpak»)"
            ),
            EnglishExercise(
                "¿Quién enseña la clase?",
                "The ______ teaches the class.",
                listOf("teacher", "student", "doctor", "driver"), "teacher",
                "'teacher' = maestro/maestra.",
                "teacher = /ˈtiːtʃər/ («tícher»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'cuaderno' en inglés?",
                "I write my homework in my ______.",
                listOf("notebook", "window", "chair", "door"), "notebook",
                "'notebook' = cuaderno.",
                "notebook = /ˈnoʊtbʊk/ («nóutbuk»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'recreo' en inglés?",
                "We play with our friends at ______.",
                listOf("recess", "homework", "test", "class"), "recess",
                "'recess' = recreo.",
                "recess = /ˈriːsɛs/ («ríses»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'tarea' en inglés?",
                "I do my ______ after school.",
                listOf("homework", "breakfast", "shower", "game"), "homework",
                "'homework' = tarea.",
                "homework = /ˈhoʊmwɜːrk/ («jóumuerk»)"
            )
        )),

        EnglishUnit("De viaje (vocabulario)", listOf(
            EnglishExercise(
                "¿En qué volamos a otro país?",
                "We fly in an ______.",
                listOf("airplane", "car", "bicycle", "boat"), "airplane",
                "'airplane' = avión.",
                "airplane = /ˈɛərpleɪn/ («érplein»)"
            ),
            EnglishExercise(
                "¿Dónde dormimos en un viaje?",
                "We sleep in a ______.",
                listOf("hotel", "school", "kitchen", "garden"), "hotel",
                "'hotel' = hotel (¡se escribe igual!).",
                "hotel = /hoʊˈtɛl/ («joutél»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'playa' en inglés?",
                "We swim at the ______.",
                listOf("beach", "mountain", "city", "store"), "beach",
                "'beach' = playa.",
                "beach = /biːtʃ/ («bich»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'maleta' en inglés?",
                "I pack my clothes in my ______.",
                listOf("suitcase", "wallet", "hat", "shoe"), "suitcase",
                "'suitcase' = maleta.",
                "suitcase = /ˈsuːtkeɪs/ («sútkeis»)"
            ),
            EnglishExercise(
                "¿Qué necesitas para subir al avión?",
                "Show your ______ to get on the plane.",
                listOf("ticket", "toy", "sandwich", "pillow"), "ticket",
                "'ticket' = boleto.",
                "ticket = /ˈtɪkɪt/ («tíket»)"
            )
        )),

        EnglishUnit("La familia (vocabulario)", listOf(
            EnglishExercise(
                "¿Cómo se dice 'hermano' en inglés?",
                "My ______ plays video games with me.",
                listOf("brother", "sister", "father", "uncle"), "brother",
                "'brother' = hermano.",
                "brother = /ˈbrʌðər/ («bróder»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'abuela' en inglés?",
                "My ______ makes delicious cookies.",
                listOf("grandmother", "teacher", "cousin", "aunt"), "grandmother",
                "'grandmother' = abuela.",
                "grandmother = /ˈgrænmʌðər/ («gránmader»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'primo' en inglés?",
                "My ______ lives in another city.",
                listOf("cousin", "brother", "nephew", "son"), "cousin",
                "'cousin' = primo o prima.",
                "cousin = /ˈkʌzən/ («kásen»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'padres' en inglés?",
                "My ______ take care of me.",
                listOf("parents", "friends", "teachers", "neighbors"), "parents",
                "'parents' = padres (papá y mamá).",
                "parents = /ˈpɛərənts/ («pérents»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'tía' en inglés?",
                "My ______ is my mom's sister.",
                listOf("aunt", "uncle", "grandma", "sister"), "aunt",
                "'aunt' = tía.",
                "aunt = /ænt/ («ant»)"
            )
        )),

        EnglishUnit("La comida (vocabulario)", listOf(
            EnglishExercise(
                "¿Cómo se dice 'desayuno' en inglés?",
                "I eat ______ in the morning.",
                listOf("breakfast", "dinner", "lunch", "snack"), "breakfast",
                "'breakfast' = desayuno.",
                "breakfast = /ˈbrɛkfəst/ («brékfast»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'pollo' en inglés?",
                "We had rice and ______ for lunch.",
                listOf("chicken", "beef", "fish", "cheese"), "chicken",
                "'chicken' = pollo.",
                "chicken = /ˈtʃɪkɪn/ («chíken»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'arroz' en inglés?",
                "My favorite food is ______ with beans.",
                listOf("rice", "bread", "soup", "salad"), "rice",
                "'rice' = arroz.",
                "rice = /raɪs/ («ráis»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'jugo' en inglés?",
                "I drink orange ______ every morning.",
                listOf("juice", "milk", "water", "soda"), "juice",
                "'juice' = jugo.",
                "juice = /dʒuːs/ («llus»)"
            ),
            EnglishExercise(
                "¿Cómo se dice 'verduras' en inglés?",
                "Eat your ______ to grow strong!",
                listOf("vegetables", "candies", "cookies", "chips"), "vegetables",
                "'vegetables' = verduras.",
                "vegetables = /ˈvɛdʒtəbəlz/ («véchtabols»)"
            )
        ))
    )

    // ---------------- INGLÉS AVANZADO (Secundaria) ----------------
    // Gramática de nivel intermedio, más allá de las 8 unidades básicas de
    // arriba (que Primaria y Secundaria comparten). Secundaria rota entre
    // AMBOS bancos (básico + avanzado); Primaria se queda solo en el básico.
    private val englishAdvancedUnits = listOf(
        EnglishUnit("Comparativos y superlativos", listOf(
            EnglishExercise("Elige la forma correcta.", "This book is ______ than that one.",
                listOf("more interesting", "interesting", "most interesting", "interestinger"), "more interesting",
                "Adjetivos largos (interesting) usan 'more' + adjetivo para comparar.", "more = /mɔːr/ («mor»)"),
            EnglishExercise("Elige la forma correcta.", "She is the ______ student in the class.",
                listOf("tallest", "taller", "more tall", "tall"), "tallest",
                "Adjetivo corto de 1 sílaba: se agrega '-est' para el superlativo.", "tallest = /ˈtɔːlɪst/ («tólest»)"),
            EnglishExercise("Elige la forma correcta.", "This is the ______ mountain in Mexico.",
                listOf("highest", "higher", "more high", "high"), "highest",
                "Superlativo de 'high': se agrega '-est'.", "highest = /ˈhaɪɪst/ («jáiest»)"),
            EnglishExercise("Elige la forma correcta.", "My brother is ______ than me.",
                listOf("younger", "young", "more young", "youngest"), "younger",
                "Comparativo de 'young': se agrega '-er'.", "younger = /ˈjʌŋgər/ («iánguer»)"),
            EnglishExercise("Elige la forma correcta.", "That was the ______ movie I've ever seen!",
                listOf("best", "goodest", "more good", "gooder"), "best",
                "'good' es irregular: best/better, no lleva '-est'.", "best = /bɛst/ («best»)"),
            EnglishExercise("Elige la forma correcta.", "Winter is ______ than summer.",
                listOf("colder", "cold", "more cold", "coldest"), "colder",
                "Comparativo de 'cold': se agrega '-er'.", "colder = /ˈkoʊldər/ («kóulder»)"),
            EnglishExercise("Elige la forma correcta.", "This is the ______ day of my life.",
                listOf("worst", "badest", "more bad", "worse"), "worst",
                "'bad' es irregular: worst es el superlativo (worse es el comparativo).", "worst = /wɜːrst/ («uerst»)"),
            EnglishExercise("Elige la forma correcta.", "He runs ______ than his friend.",
                listOf("faster", "fast", "more fast", "fastest"), "faster",
                "Comparativo de 'fast': se agrega '-er'.", "faster = /ˈfæstər/ («fáster»)"),
            EnglishExercise("Elige la forma correcta.", "This exercise is ______ than the last one.",
                listOf("more difficult", "difficulter", "difficult", "most difficult"), "more difficult",
                "Adjetivo largo: se usa 'more' + adjetivo.", "difficult = /ˈdɪfɪkəlt/ («dífikolt»)"),
            EnglishExercise("Elige la forma correcta.", "She is ______ than her sister.",
                listOf("prettier", "pretty", "more pretty", "prettiest"), "prettier",
                "Adjetivos terminados en '-y' cambian a '-ier'.", "prettier = /ˈprɪtiər/ («príter»)"),
            EnglishExercise("Elige la forma correcta.", "This is the ______ city I have visited.",
                listOf("biggest", "bigest", "more big", "bigger"), "biggest",
                "Se dobla la consonante final: big → biggest.", "biggest = /ˈbɪgɪst/ («bíguest»)"),
            EnglishExercise("¿Cuál oración es correcta?", "Choose the correct sentence.",
                listOf("She is taller than me.", "She is more taller than me.", "She is tallest than me.", "She is more tall than me."),
                "She is taller than me.", "No se combina 'more' con '-er' (no digas 'more taller').", "taller = /ˈtɔːlər/ («tóler»)"),
            EnglishExercise("Elige la forma correcta.", "Of the three brothers, John is the ______.",
                listOf("funniest", "funnier", "more funny", "funny"), "funniest",
                "'-y' final cambia a '-iest' en el superlativo.", "funniest = /ˈfʌniɪst/ («fániest»)"),
            EnglishExercise("Elige la forma correcta.", "This year's exam was ______ than last year's.",
                listOf("easier", "easyer", "more easy", "easiest"), "easier",
                "'-y' final cambia a '-ier': easy → easier.", "easier = /ˈiːziər/ («ísier»)"),
            EnglishExercise("Elige la forma correcta.", "He is the ______ person I know.",
                listOf("kindest", "kinder", "more kind", "kindly"), "kindest",
                "Adjetivo corto: se agrega '-est' para el superlativo.", "kindest = /ˈkaɪndɪst/ («káindest»)")
        )),

        EnglishUnit("Verbos modales", listOf(
            EnglishExercise("Elige el modal correcto (obligación).", "You ______ wear a seatbelt in the car.",
                listOf("must", "can", "might", "would"), "must",
                "'must' expresa obligación/regla importante.", "must = /mʌst/ («mast»)"),
            EnglishExercise("Elige el modal correcto (permiso formal).", "______ I open the window?",
                listOf("May", "Must", "Should", "Would"), "May",
                "'May' se usa para pedir permiso de forma educada.", "may = /meɪ/ («méi»)"),
            EnglishExercise("Elige el modal correcto (habilidad).", "She ______ speak three languages.",
                listOf("can", "must", "should", "might"), "can",
                "'can' expresa habilidad/capacidad.", "can = /kæn/ («kan»)"),
            EnglishExercise("Elige el modal correcto (prohibición).", "You ______ smoke here, it's not allowed.",
                listOf("must not", "don't have to", "might not", "should"), "must not",
                "'must not' expresa prohibición.", "must not = /mʌst nɒt/ («mast nat»)"),
            EnglishExercise("Elige el modal correcto (necesidad).", "We ______ finish the homework by Friday.",
                listOf("have to", "might", "would", "may"), "have to",
                "'have to' expresa una necesidad externa.", "have to = /hæv tuː/ («jav tu»)"),
            EnglishExercise("Elige el modal correcto (consejo).", "You ______ see a doctor if you feel sick.",
                listOf("should", "must", "can", "will"), "should",
                "'should' se usa para dar un consejo.", "should = /ʃʊd/ («shud»)"),
            EnglishExercise("Elige el modal correcto (posibilidad).", "It ______ rain later, the sky is cloudy.",
                listOf("might", "must", "can", "should"), "might",
                "'might' expresa una posibilidad, no certeza.", "might = /maɪt/ («máit»)"),
            EnglishExercise("Elige el modal correcto (petición educada).", "______ you help me with this bag?",
                listOf("Could", "Must", "Should", "May"), "Could",
                "'Could' se usa para pedir ayuda de forma educada.", "could = /kʊd/ («kud»)"),
            EnglishExercise("Elige el modal correcto (regla).", "Students ______ arrive on time.",
                listOf("must", "might", "could", "would"), "must",
                "'must' expresa una regla obligatoria.", "must = /mʌst/ («mast»)"),
            EnglishExercise("Elige el modal correcto (deducción negativa).", "He ______ be at home; his car isn't there.",
                listOf("can't", "must", "should", "may"), "can't",
                "'can't' expresa que algo es casi imposible/improbable.", "can't = /kænt/ («kant»)"),
            EnglishExercise("Elige el modal correcto (consejo).", "You ______ eat vegetables, they're good for you.",
                listOf("should", "must not", "can't", "might"), "should",
                "'should' es un consejo, no una obligación estricta.", "should = /ʃʊd/ («shud»)"),
            EnglishExercise("Elige el modal correcto (no es necesario).", "We ______ wait; the bus is coming.",
                listOf("don't have to", "must not", "shouldn't", "can't"), "don't have to",
                "'don't have to' significa que no es necesario.", "don't have to = /doʊnt hæv tuː/ («dont jav tu»)"),
            EnglishExercise("Elige el modal correcto (permiso informal).", "______ I borrow your pencil?",
                listOf("Can", "Must", "Should", "Would"), "Can",
                "'Can' es informal para pedir permiso.", "can = /kæn/ («kan»)"),
            EnglishExercise("¿Cuál oración expresa OBLIGACIÓN?", "Choose the sentence with obligation.",
                listOf("You must wear a helmet.", "You might wear a helmet.", "You could wear a helmet.", "You may wear a helmet."),
                "You must wear a helmet.", "'must' es el modal de obligación.", "must = /mʌst/ («mast»)"),
            EnglishExercise("Elige el modal correcto (duda fuerte).", "It ______ be true, I don't believe it.",
                listOf("can't", "must", "should", "have to"), "can't",
                "'can't be true' = es casi imposible que sea verdad.", "can't = /kænt/ («kant»)")
        )),

        EnglishUnit("Primer condicional", listOf(
            EnglishExercise("Completa el primer condicional.", "If it rains, we ______ stay home.",
                listOf("will stay", "stay", "stayed", "would stay"), "will stay",
                "Primer condicional: If + presente, will + verbo.", "will = /wɪl/ («uíl»)"),
            EnglishExercise("Completa el primer condicional.", "If you study hard, you ______ pass the exam.",
                listOf("will pass", "pass", "passed", "would pass"), "will pass",
                "Estructura: If + presente, will + verbo base.", "pass = /pæs/ («pas»)"),
            EnglishExercise("Completa el primer condicional.", "She will be happy if you ______ her.",
                listOf("call", "will call", "called", "calling"), "call",
                "Después de 'if' se usa presente simple, no 'will'.", "call = /kɔːl/ («kol»)"),
            EnglishExercise("Completa el primer condicional.", "If I ______ time, I will visit you.",
                listOf("have", "will have", "had", "having"), "have",
                "Después de 'if' va presente simple: have.", "have = /hæv/ («jav»)"),
            EnglishExercise("Completa el primer condicional.", "They will miss the bus if they ______ up now.",
                listOf("don't get", "won't get", "didn't get", "not get"), "don't get",
                "Negativo en presente simple: don't + verbo.", "get = /gɛt/ («guet»)"),
            EnglishExercise("Completa el primer condicional.", "If we leave now, we ______ arrive on time.",
                listOf("will arrive", "arrive", "arrived", "would arrive"), "will arrive",
                "Resultado con 'will' + verbo base.", "arrive = /əˈraɪv/ («arráiv»)"),
            EnglishExercise("Completa el primer condicional.", "You will get wet if you ______ an umbrella.",
                listOf("don't take", "won't take", "didn't take", "not take"), "don't take",
                "Presente negativo después de 'if': don't + verbo.", "take = /teɪk/ («téik»)"),
            EnglishExercise("Completa el primer condicional.", "If he practices every day, he ______ improve.",
                listOf("will", "practices", "practiced", "would"), "will",
                "Resultado con 'will' + verbo base (improve).", "will = /wɪl/ («uíl»)"),
            EnglishExercise("Completa el primer condicional.", "I will call you if I ______ any news.",
                listOf("get", "will get", "got", "getting"), "get",
                "Presente simple después de 'if'.", "get = /gɛt/ («guet»)"),
            EnglishExercise("Completa el primer condicional.", "If the weather is nice, we ______ go to the beach.",
                listOf("will", "go", "went", "would"), "will",
                "Resultado con 'will' + verbo base (go).", "will = /wɪl/ («uíl»)"),
            EnglishExercise("Completa el primer condicional.", "She won't come if it ______ too cold.",
                listOf("is", "will be", "was", "being"), "is",
                "Presente simple después de 'if', aunque el resultado sea negativo.", "is = /ɪz/ («is»)"),
            EnglishExercise("Completa el primer condicional.", "If you don't eat, you ______ hungry later.",
                listOf("will be", "are", "were", "being"), "will be",
                "Resultado con 'will be'.", "will be = /wɪl biː/ («uíl bi»)"),
            EnglishExercise("Elige la oración correcta.", "Choose the correct first conditional sentence.",
                listOf("If it snows, we will build a snowman.", "If it will snow, we build a snowman.",
                    "If it snows, we built a snowman.", "If it snowed, we will build a snowman."),
                "If it snows, we will build a snowman.", "If + presente, will + verbo: esa es la fórmula correcta.", "snow = /snoʊ/ («snóu»)"),
            EnglishExercise("Completa el primer condicional.", "We will be late if the train ______.",
                listOf("is delayed", "delays", "delayed", "will delay"), "is delayed",
                "Presente simple (pasivo) después de 'if'.", "delayed = /dɪˈleɪd/ («diléid»)"),
            EnglishExercise("Completa el primer condicional.", "If you ______ me, I will explain everything.",
                listOf("ask", "will ask", "asked", "asking"), "ask",
                "Presente simple después de 'if'.", "ask = /æsk/ («ask»)")
        )),

        EnglishUnit("Verbos frasales", listOf(
            EnglishExercise("Completa la partícula correcta.", "Please turn ______ the TV, it's too loud.",
                listOf("down", "up", "off", "on"), "down",
                "'turn down' = bajar el volumen.", "turn down = /tɜːrn daʊn/ («tern dáun»)"),
            EnglishExercise("Completa la partícula correcta.", "I need to get ______ early tomorrow.",
                listOf("up", "down", "off", "on"), "up",
                "'get up' = levantarse.", "get up = /gɛt ʌp/ («guet ap»)"),
            EnglishExercise("Completa la partícula correcta.", "Don't give ______! We can solve this problem.",
                listOf("up", "down", "off", "away"), "up",
                "'give up' = darse por vencido.", "give up = /gɪv ʌp/ («guiv ap»)"),
            EnglishExercise("Completa la partícula correcta.", "She is looking ______ her keys.",
                listOf("for", "at", "up", "over"), "for",
                "'look for' = buscar algo.", "look for = /lʊk fɔːr/ («luk for»)"),
            EnglishExercise("Completa la partícula correcta.", "Please take ______ your shoes before entering.",
                listOf("off", "out", "up", "down"), "off",
                "'take off' = quitarse (ropa/zapatos).", "take off = /teɪk ɒf/ («téik of»)"),
            EnglishExercise("Completa la partícula correcta.", "Can you turn ______ the lights? It's dark.",
                listOf("on", "off", "up", "down"), "on",
                "'turn on' = encender.", "turn on = /tɜːrn ɒn/ («tern on»)"),
            EnglishExercise("Completa la partícula correcta.", "He decided to give ______ smoking.",
                listOf("up", "out", "in", "away"), "up",
                "'give up (doing something)' = dejar un hábito.", "give up = /gɪv ʌp/ («guiv ap»)"),
            EnglishExercise("Completa la partícula correcta.", "I'm looking ______ to the weekend.",
                listOf("forward", "for", "at", "up"), "forward",
                "'look forward to' = esperar algo con ganas.", "forward = /ˈfɔːrwərd/ («fórward»)"),
            EnglishExercise("Completa la partícula correcta.", "The plane will take ______ in ten minutes.",
                listOf("off", "out", "up", "down"), "off",
                "'take off' (de un avión) = despegar.", "take off = /teɪk ɒf/ («téik of»)"),
            EnglishExercise("Completa la partícula correcta.", "I need to look ______ this word in the dictionary.",
                listOf("up", "for", "at", "over"), "up",
                "'look up' = buscar información (en diccionario/internet).", "look up = /lʊk ʌp/ («luk ap»)"),
            EnglishExercise("Completa la partícula correcta.", "Let's turn ______ the lights, it's getting dark.",
                listOf("on", "off", "up", "down"), "on",
                "'turn on' = encender.", "turn on = /tɜːrn ɒn/ («tern on»)"),
            EnglishExercise("Completa la partícula correcta.", "Please put ______ your books, class is starting.",
                listOf("away", "off", "out", "down"), "away",
                "'put away' = guardar algo en su lugar.", "put away = /pʊt əˈweɪ/ («put auéi»)"),
            EnglishExercise("Completa la partícula correcta.", "She grew ______ in Mexico City.",
                listOf("up", "off", "out", "over"), "up",
                "'grow up' = crecer (de niño a adulto).", "grow up = /groʊ ʌp/ («gróu ap»)"),
            EnglishExercise("Completa la partícula correcta.", "They ran ______ of time during the test.",
                listOf("out", "off", "over", "up"), "out",
                "'run out of' = quedarse sin algo.", "run out = /rʌn aʊt/ («ran áut»)"),
            EnglishExercise("Completa la partícula correcta.", "Please hand ______ your homework by Friday.",
                listOf("in", "out", "up", "over"), "in",
                "'hand in' = entregar (tarea/trabajo).", "hand in = /hænd ɪn/ («jand in»)")
        )),

        EnglishUnit("Voz pasiva básica", listOf(
            EnglishExercise("Completa con la voz pasiva correcta.", "English ______ in many countries.",
                listOf("is spoken", "speaks", "spoke", "is speak"), "is spoken",
                "Voz pasiva presente: is/are + participio.", "spoken = /ˈspoʊkən/ («spóuken»)"),
            EnglishExercise("Completa con la voz pasiva correcta.", "The cake ______ by my mom yesterday.",
                listOf("was baked", "baked", "is baked", "bakes"), "was baked",
                "Voz pasiva pasado: was/were + participio.", "baked = /beɪkt/ («béikt»)"),
            EnglishExercise("Completa con la voz pasiva correcta.", "This bridge ______ in 1990.",
                listOf("was built", "built", "is built", "builds"), "was built",
                "Voz pasiva pasado: was + participio (build → built).", "built = /bɪlt/ («bilt»)"),
            EnglishExercise("Completa con la voz pasiva correcta.", "The homework ______ every day.",
                listOf("is checked", "checks", "checked", "check"), "is checked",
                "Voz pasiva presente: is + participio.", "checked = /tʃɛkt/ («chekt»)"),
            EnglishExercise("Completa con la voz pasiva correcta.", "The window ______ by the wind.",
                listOf("was broken", "broke", "breaks", "is break"), "was broken",
                "Voz pasiva pasado: was + participio (break → broken).", "broken = /ˈbroʊkən/ («bróuken»)"),
            EnglishExercise("Completa con la voz pasiva correcta.", "Rice ______ in many Asian countries.",
                listOf("is grown", "grows", "grew", "is grow"), "is grown",
                "Voz pasiva presente: is + participio (grow → grown).", "grown = /groʊn/ («gróun»)"),
            EnglishExercise("Completa con la voz pasiva correcta.", "The movie ______ by millions of people.",
                listOf("was watched", "watched", "watch", "is watch"), "was watched",
                "Voz pasiva pasado: was + participio.", "watched = /wɒtʃt/ («uótcht»)"),
            EnglishExercise("Completa con la voz pasiva correcta.", "New phones ______ every year.",
                listOf("are released", "release", "released", "releasing"), "are released",
                "Voz pasiva presente plural: are + participio.", "released = /rɪˈliːst/ («rilíst»)"),
            EnglishExercise("Completa con la voz pasiva correcta.", "The song ______ by a famous singer.",
                listOf("was sung", "sang", "sings", "is sing"), "was sung",
                "Voz pasiva pasado: was + participio (sing → sung).", "sung = /sʌŋ/ («san»)"),
            EnglishExercise("Completa con la voz pasiva correcta.", "The park ______ every morning.",
                listOf("is cleaned", "cleans", "cleaned", "clean"), "is cleaned",
                "Voz pasiva presente: is + participio.", "cleaned = /kliːnd/ («klind»)"),
            EnglishExercise("Completa con la voz pasiva correcta.", "The book ______ into five languages.",
                listOf("was translated", "translated", "translates", "is translate"), "was translated",
                "Voz pasiva pasado: was + participio.", "translated = /trænzˈleɪtɪd/ («transléitid»)"),
            EnglishExercise("Completa con la voz pasiva correcta.", "Coffee ______ in Colombia.",
                listOf("is grown", "grows", "grew", "growing"), "is grown",
                "Voz pasiva presente: is + participio.", "grown = /groʊn/ («gróun»)"),
            EnglishExercise("Completa con la voz pasiva correcta.", "The email ______ yesterday.",
                listOf("was sent", "sent", "sends", "is send"), "was sent",
                "Voz pasiva pasado: was + participio (send → sent).", "sent = /sɛnt/ («sent»)"),
            EnglishExercise("Completa con la voz pasiva correcta.", "This song ______ by everyone at the party.",
                listOf("was loved", "loved", "loves", "is love"), "was loved",
                "Voz pasiva pasado: was + participio.", "loved = /lʌvd/ («lavd»)"),
            EnglishExercise("Completa con la voz pasiva correcta.", "The store ______ at 9 pm.",
                listOf("is closed", "closes", "closed", "close"), "is closed",
                "Voz pasiva presente: is + participio.", "closed = /kloʊzd/ («klóuzd»)")
        )),

        EnglishUnit("Preguntas con Wh- y do/does/did", listOf(
            EnglishExercise("Elige la palabra Wh- correcta.", "______ do you live?",
                listOf("Where", "What", "Who", "Why"), "Where",
                "'Where' pregunta por un lugar.", "where = /wɛr/ («uér»)"),
            EnglishExercise("Elige la palabra Wh- correcta.", "______ does she work?",
                listOf("Where", "When", "Who", "How"), "Where",
                "'Where' pregunta por el lugar de trabajo.", "where = /wɛr/ («uér»)"),
            EnglishExercise("Elige la palabra Wh- correcta.", "______ did you go yesterday?",
                listOf("Where", "What", "Why", "Whose"), "Where",
                "'Where' pregunta por un lugar (en pasado con 'did').", "where = /wɛr/ («uér»)"),
            EnglishExercise("Elige la palabra Wh- correcta.", "______ is your favorite color?",
                listOf("What", "Who", "Where", "When"), "What",
                "'What' pregunta por una cosa/información.", "what = /wʌt/ («uát»)"),
            EnglishExercise("Elige la palabra Wh- correcta.", "______ do you usually wake up?",
                listOf("When", "Where", "Who", "Whose"), "When",
                "'When' pregunta por el momento/hora.", "when = /wɛn/ («uén»)"),
            EnglishExercise("Elige la palabra Wh- correcta.", "______ made this cake? (pregunta sobre el sujeto)",
                listOf("Who", "What", "Where", "Whose"), "Who",
                "'Who' pregunta por la persona que hizo la acción (sujeto).", "who = /huː/ («ju»)"),
            EnglishExercise("Elige la palabra Wh- correcta.", "______ does he like pizza?",
                listOf("Why", "Where", "When", "Who"), "Why",
                "'Why' pregunta por la razón.", "why = /waɪ/ («uái»)"),
            EnglishExercise("Elige la palabra Wh- correcta.", "How ______ brothers do you have?",
                listOf("many", "much", "old", "long"), "many",
                "'How many' se usa con sustantivos contables (brothers).", "many = /ˈmɛni/ («méni»)"),
            EnglishExercise("Elige la palabra Wh- correcta.", "How ______ are you?",
                listOf("old", "many", "much", "long"), "old",
                "'How old' pregunta por la edad.", "old = /oʊld/ («óuld»)"),
            EnglishExercise("Elige la palabra Wh- correcta.", "______ did you learn English?",
                listOf("How", "What", "Whose", "Why"), "How",
                "'How' pregunta por el método/manera.", "how = /haʊ/ («jáu»)"),
            EnglishExercise("Elige la palabra Wh- correcta.", "______ is coming to the party?",
                listOf("Who", "What", "Where", "Whose"), "Who",
                "'Who' pregunta por la persona (sujeto).", "who = /huː/ («ju»)"),
            EnglishExercise("Elige la palabra Wh- correcta.", "______ does the movie start?",
                listOf("When", "Where", "Who", "Why"), "When",
                "'When' pregunta por el momento.", "when = /wɛn/ («uén»)"),
            EnglishExercise("Elige la palabra Wh- correcta.", "______ book is this?",
                listOf("Whose", "Who", "What", "Where"), "Whose",
                "'Whose' pregunta por el dueño de algo.", "whose = /huːz/ («jus»)"),
            EnglishExercise("Elige la palabra Wh- correcta.", "______ did you do that?",
                listOf("Why", "How", "When", "Where"), "Why",
                "'Why' pregunta por la razón.", "why = /waɪ/ («uái»)"),
            EnglishExercise("Elige la palabra Wh- correcta.", "______ are you going?",
                listOf("Where", "What", "Who", "Whose"), "Where",
                "'Where' pregunta por el destino.", "where = /wɛr/ («uér»)")
        )),

        EnglishUnit("Estilo indirecto (reported speech)", listOf(
            EnglishExercise("Cambia al estilo indirecto.", "He said, 'I like pizza.' → He said that he ______ pizza.",
                listOf("liked", "likes", "like", "liking"), "liked",
                "El presente ('like') cambia a pasado ('liked') en estilo indirecto.", "liked = /laɪkt/ («láikt»)"),
            EnglishExercise("Cambia al estilo indirecto.", "She said, 'I am happy.' → She said that she ______ happy.",
                listOf("was", "is", "were", "be"), "was",
                "'am/is' cambia a 'was' en estilo indirecto.", "was = /wʌz/ («uas»)"),
            EnglishExercise("Cambia al estilo indirecto.", "They said, 'We live in Mexico.' → They said that they ______ in Mexico.",
                listOf("lived", "live", "living", "lives"), "lived",
                "Presente ('live') cambia a pasado ('lived').", "lived = /lɪvd/ («livd»)"),
            EnglishExercise("Cambia al estilo indirecto.", "He said, 'I will call you.' → He said that he ______ call me.",
                listOf("would", "will", "can", "could"), "would",
                "'will' cambia a 'would' en estilo indirecto.", "would = /wʊd/ («wud»)"),
            EnglishExercise("Cambia al estilo indirecto.", "She said, 'I can swim.' → She said that she ______ swim.",
                listOf("could", "can", "would", "will"), "could",
                "'can' cambia a 'could' en estilo indirecto.", "could = /kʊd/ («kud»)"),
            EnglishExercise("Cambia al estilo indirecto.", "They said, 'We are studying.' → They said that they ______ studying.",
                listOf("were", "are", "was", "be"), "were",
                "'are' cambia a 'were' en estilo indirecto.", "were = /wɜːr/ («uer»)"),
            EnglishExercise("Cambia al estilo indirecto.", "He said, 'I have a car.' → He said that he ______ a car.",
                listOf("had", "has", "have", "having"), "had",
                "'have' cambia a 'had' en estilo indirecto.", "had = /hæd/ («jad»)"),
            EnglishExercise("Cambia al estilo indirecto.", "She said, 'I want to travel.' → She said that she ______ to travel.",
                listOf("wanted", "want", "wants", "wanting"), "wanted",
                "Presente ('want') cambia a pasado ('wanted').", "wanted = /ˈwɒntɪd/ («uántid»)"),
            EnglishExercise("Cambia al estilo indirecto.", "He said, 'I don't know.' → He said that he ______ know.",
                listOf("didn't", "doesn't", "don't", "isn't"), "didn't",
                "'don't' cambia a 'didn't' en estilo indirecto.", "didn't = /ˈdɪdənt/ («dídent»)"),
            EnglishExercise("Cambia al estilo indirecto.", "They said, 'We need help.' → They said that they ______ help.",
                listOf("needed", "need", "needs", "needing"), "needed",
                "Presente ('need') cambia a pasado ('needed').", "needed = /ˈniːdɪd/ («nídid»)"),
            EnglishExercise("Cambia al estilo indirecto.", "She said, 'I love music.' → She said that she ______ music.",
                listOf("loved", "loves", "love", "loving"), "loved",
                "Presente ('love') cambia a pasado ('loved').", "loved = /lʌvd/ («lavd»)"),
            EnglishExercise("Cambia al estilo indirecto.", "He said, 'I am learning English.' → He said that he ______ learning English.",
                listOf("was", "is", "were", "be"), "was",
                "'am' cambia a 'was' en estilo indirecto.", "was = /wʌz/ («uas»)"),
            EnglishExercise("Cambia al estilo indirecto.", "They said, 'We will win.' → They said that they ______ win.",
                listOf("would", "will", "can", "could"), "would",
                "'will' cambia a 'would' en estilo indirecto.", "would = /wʊd/ («wud»)"),
            EnglishExercise("Cambia al estilo indirecto.", "She said, 'I can help you.' → She said that she ______ help me.",
                listOf("could", "can", "would", "will"), "could",
                "'can' cambia a 'could' en estilo indirecto.", "could = /kʊd/ («kud»)"),
            EnglishExercise("Cambia al estilo indirecto.", "He said, 'I have finished.' → He said that he ______ finished.",
                listOf("had", "has", "have", "having"), "had",
                "'have' cambia a 'had' en estilo indirecto.", "had = /hæd/ («jad»)")
        )),

        EnglishUnit("Conectores (because, although, however, so that)", listOf(
            EnglishExercise("Elige el conector correcto.", "I stayed home ______ I was sick.",
                listOf("because", "although", "however", "so that"), "because",
                "'because' da la razón de algo.", "because = /bɪˈkɒz/ («bikós»)"),
            EnglishExercise("Elige el conector correcto.", "She was tired ______ she worked all day.",
                listOf("because", "although", "however", "so that"), "because",
                "'because' explica la causa (trabajó todo el día).", "because = /bɪˈkɒz/ («bikós»)"),
            EnglishExercise("Elige el conector correcto.", "He passed the test ______ he studied a lot.",
                listOf("because", "although", "however", "so that"), "because",
                "'because' explica la causa del resultado.", "because = /bɪˈkɒz/ («bikós»)"),
            EnglishExercise("Elige el conector correcto.", "They stayed inside ______ it was cold.",
                listOf("because", "although", "however", "so that"), "because",
                "'because' explica la razón.", "because = /bɪˈkɒz/ («bikós»)"),
            EnglishExercise("Elige el conector correcto.", "______ it was raining, they played outside.",
                listOf("Although", "Because", "However", "So that"), "Although",
                "'Although' introduce un contraste (a pesar de).", "although = /ɔːlˈðoʊ/ («oldóu»)"),
            EnglishExercise("Elige el conector correcto.", "______ she was nervous, she gave a great speech.",
                listOf("Although", "Because", "However", "So that"), "Although",
                "'Although' muestra un contraste inesperado.", "although = /ɔːlˈðoʊ/ («oldóu»)"),
            EnglishExercise("Elige el conector correcto.", "He went to work ______ he was sick.",
                listOf("although", "because", "however", "so that"), "although",
                "'although' muestra que fue a trabajar a pesar de estar enfermo.", "although = /ɔːlˈðoʊ/ («oldóu»)"),
            EnglishExercise("Elige el conector correcto.", "______ the movie was long, we enjoyed it.",
                listOf("Although", "Because", "However", "So that"), "Although",
                "'Although' introduce el contraste (larga, pero la disfrutaron).", "although = /ɔːlˈðoʊ/ («oldóu»)"),
            EnglishExercise("Elige el conector correcto.", "She studied hard. ______, she didn't pass.",
                listOf("However", "Because", "Although", "So that"), "However",
                "'However' conecta dos oraciones mostrando contraste.", "however = /haʊˈɛvər/ («jauéver»)"),
            EnglishExercise("Elige el conector correcto.", "The food was expensive. ______, it was delicious.",
                listOf("However", "Because", "Although", "So that"), "However",
                "'However' introduce un contraste entre oraciones.", "however = /haʊˈɛvər/ («jauéver»)"),
            EnglishExercise("Elige el conector correcto.", "He is very shy. ______, he loves to sing.",
                listOf("However", "Because", "Although", "So that"), "However",
                "'However' muestra contraste entre dos ideas.", "however = /haʊˈɛvər/ («jauéver»)"),
            EnglishExercise("Elige el conector correcto.", "It was cold outside. ______, we decided to go for a walk.",
                listOf("However", "Because", "Although", "So that"), "However",
                "'However' conecta ideas contrastantes entre oraciones.", "however = /haʊˈɛvər/ («jauéver»)"),
            EnglishExercise("Elige el conector correcto.", "He saved money ______ he could buy a car.",
                listOf("so that", "although", "however", "because"), "so that",
                "'so that' expresa el propósito de una acción.", "so that = /soʊ ðæt/ («sóu dat»)"),
            EnglishExercise("Elige el conector correcto.", "She studies every day ______ she can get good grades.",
                listOf("so that", "although", "however", "because"), "so that",
                "'so that' expresa el propósito (para poder...).", "so that = /soʊ ðæt/ («sóu dat»)"),
            EnglishExercise("Elige el conector correcto.", "I wrote it down ______ I wouldn't forget.",
                listOf("so that", "although", "however", "because"), "so that",
                "'so that' expresa el propósito de la acción.", "so that = /soʊ ðæt/ («sóu dat»)")
        )),

        EnglishUnit("Vocabulario intermedio (tecnología, medio ambiente, profesiones)", listOf(
            EnglishExercise("¿Cómo se dice 'contaminación' en inglés?", "Air ______ is a big problem in cities.",
                listOf("pollution", "pollute", "polluted", "polluting"), "pollution",
                "'pollution' = contaminación.", "pollution = /pəˈluːʃən/ («polúshon»)"),
            EnglishExercise("¿Cómo se dice 'reciclar' en inglés?", "We should ______ plastic bottles.",
                listOf("recycle", "recycling", "recycled", "recycles"), "recycle",
                "'recycle' = reciclar.", "recycle = /riːˈsaɪkəl/ («risáikol»)"),
            EnglishExercise("¿Cómo se dice 'medio ambiente' en inglés?", "We must protect the ______.",
                listOf("environment", "environments", "environmental", "environmentally"), "environment",
                "'environment' = medio ambiente.", "environment = /ɪnˈvaɪrənmənt/ («invairenment»)"),
            EnglishExercise("¿Cómo se dice 'calentamiento global'?", "______ is affecting the polar ice caps.",
                listOf("Global warming", "Global warm", "Warming global", "Globally warm"), "Global warming",
                "'global warming' = calentamiento global.", "warming = /ˈwɔːrmɪŋ/ («uórming»)"),
            EnglishExercise("¿Cómo se dice 'aplicación' (de celular)?", "I downloaded a new ______ on my phone.",
                listOf("app", "apply", "applied", "application form"), "app",
                "'app' = aplicación de celular.", "app = /æp/ («ap»)"),
            EnglishExercise("¿Cómo se dice 'contraseña'?", "Don't forget your ______.",
                listOf("password", "pass word", "code word", "key word"), "password",
                "'password' = contraseña.", "password = /ˈpæswɜːrd/ («pásuerd»)"),
            EnglishExercise("¿Cómo se dice 'internet inalámbrico'?", "The hotel has free ______.",
                listOf("wifi", "internet cable", "wire", "network cable"), "wifi",
                "'wifi' = internet inalámbrico.", "wifi = /ˈwaɪfaɪ/ («uáifai»)"),
            EnglishExercise("¿Cómo se dice 'ingeniero(a)'?", "My uncle is an ______.",
                listOf("engineer", "engine", "engineering", "engineered"), "engineer",
                "'engineer' = ingeniero(a).", "engineer = /ˌɛndʒɪˈnɪr/ («enllinír»)"),
            EnglishExercise("¿Cómo se dice 'abogado(a)'?", "She wants to become a ______.",
                listOf("lawyer", "law", "lawful", "lawyer's"), "lawyer",
                "'lawyer' = abogado(a).", "lawyer = /ˈlɔɪər/ («lóier»)"),
            EnglishExercise("¿Cómo se dice 'orgulloso(a)'?", "I am very ______ of you.",
                listOf("proud", "pride", "proudly", "prouder"), "proud",
                "'proud' = orgulloso(a).", "proud = /praʊd/ («práud»)"),
            EnglishExercise("¿Cómo se dice 'preocupado(a)'?", "My mom is ______ about the exam.",
                listOf("worried", "worry", "worrying", "worries"), "worried",
                "'worried' = preocupado(a).", "worried = /ˈwɜːrid/ («uérid»)"),
            EnglishExercise("¿Cómo se dice 'aburrido(a)' (sentirse)?", "I feel ______ today, there's nothing to do.",
                listOf("bored", "boring", "bore", "bores"), "bored",
                "'bored' = aburrido(a) (cómo se siente uno).", "bored = /bɔːrd/ («bord»)"),
            EnglishExercise("¿Cómo se dice 'emocionado(a)'?", "We are ______ about the trip!",
                listOf("excited", "exciting", "excite", "excites"), "excited",
                "'excited' = emocionado(a) (cómo se siente uno).", "excited = /ɪkˈsaɪtɪd/ («eksáitid»)"),
            EnglishExercise("¿Cómo se dice 'basura'?", "Please put the ______ in the bin.",
                listOf("trash", "trashy", "trashed", "trashes"), "trash",
                "'trash' = basura.", "trash = /træʃ/ («trash»)"),
            EnglishExercise("¿Cómo se dice 'energía renovable'?", "Solar power is a type of ______ energy.",
                listOf("renewable", "renew", "renewal", "renewing"), "renewable",
                "'renewable' = renovable.", "renewable = /rɪˈnuːəbəl/ («rinúabol»)")
        ))
    )

    /** Índice de la unidad del día (rotación determinística por fecha). [advanced]=true
     *  incluye también el banco de gramática avanzada (Secundaria); Primaria usa
     *  solo el básico. */
    private fun englishPool(advanced: Boolean): List<EnglishUnit> =
        if (advanced) englishUnits + englishAdvancedUnits else englishUnits

    private fun todaysUnitIndex(advanced: Boolean = false): Int {
        val pool = englishPool(advanced)
        val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        return dayOfYear % pool.size
    }

    /** Título de la lección de inglés de HOY (para mostrar en la UI). */
    fun todaysEnglishUnitTitle(advanced: Boolean = false): String =
        englishPool(advanced)[todaysUnitIndex(advanced)].title

    /**
     * Palabra (inglés) asociada a un emoji del vocabulario, si existe.
     * Permite a la UI mostrar una imagen real en vez del emoji cuando ya
     * exista un archivo en res/drawable-nodpi/ para esa palabra.
     */
    fun wordForEmoji(emoji: String): String? =
        starterVocab.firstOrNull { it.emoji == emoji }?.word

    /** Guía de ayuda de inglés para los más pequeños. */
    val starterEnglishHelp = WorkedExample(
        "Palabras en inglés",
        listOf(
            "Mira el dibujo y di su nombre en inglés:",
            "🐶 dog («dog»)   🐱 cat («cat»)",
            "🍎 apple («ápol»)   ☀️ sun («san»)",
            "Repite la palabra en voz alta 3 veces. ¡Así se aprende!"
        )
    )

    /** Ejercicio de vocabulario con dibujo: emoji→palabra o palabra→emoji. */
    private fun starterEnglish(): EnglishExercise {
        // Solo participan las palabras que SÍ tienen emoji (las de emoji="" son
        // reales en el vocabulario pero esperan una imagen real más adelante).
        val withEmoji = starterVocab.filter { it.emoji.isNotBlank() }
        val item = withEmoji.random()
        val others = withEmoji.filter { it != item }.shuffled().take(3)
        return if (Random.nextBoolean()) {
            EnglishExercise(
                "Mira el dibujo. ¿Cómo se dice en inglés?",
                item.emoji,
                (others.map { it.word } + item.word).shuffled(),
                item.word,
                "${item.emoji} es '${item.word}' en inglés (${item.es}). Dilo en voz alta: ${item.word}!",
                "${item.word} = ${item.phon}"
            )
        } else {
            EnglishExercise(
                "¿Cuál dibujo es '${item.word}'?",
                item.word,
                (others.map { it.emoji } + item.emoji).shuffled(),
                item.emoji,
                "'${item.word}' significa ${item.es}: ${item.emoji}. Repite: ${item.word}!",
                "${item.word} = ${item.phon}"
            )
        }
    }

    fun randomEnglish(starter: Boolean = false, exclude: String? = null, advanced: Boolean = false): EnglishExercise {
        if (starter) {
            var ex = starterEnglish()
            var tries = 0
            while (exclude != null && ex.question == exclude && tries < 6) {
                ex = starterEnglish(); tries++
            }
            return ex
        }
        // Primaria: solo unidades básicas. Secundaria (advanced=true): básicas +
        // gramática avanzada, rotación por fecha sobre el pool combinado.
        val bank = englishPool(advanced)[todaysUnitIndex(advanced)].bank
        val pool = bank.filter { it.question != exclude }.ifEmpty { bank }
        return pool.random().let { it.copy(options = it.options.shuffled()) }
    }

    // ---------------- LECTURA ----------------
    private val readingBank = listOf(
        ReadingPassage(
            "El pingüino emperador",
            "El pingüino emperador es la especie de pingüino más grande del mundo. Vive en la fría Antártida. A pesar de ser un ave, no puede volar, pero es un nadador excepcional que caza peces en el océano helado."
        ),
        ReadingPassage(
            "Las abejas y las flores",
            "Las abejas son insectos trabajadores que vuelan de flor en flor recolectando néctar para hacer miel. Al hacer esto, transportan el polen de las flores, lo cual ayuda a que crezcan nuevas plantas y frutos."
        ),
        ReadingPassage(
            "El misterio de la Luna",
            "La Luna es el único satélite natural de la Tierra. No tiene luz propia, sino que refleja la luz del Sol. Tarda aproximadamente 28 días en dar una vuelta completa alrededor de nuestro planeta."
        )
    )

    // Lecturas más amplias para secundaria (comprensión de textos largos).
    private val readingAdvancedBank = listOf(
        ReadingPassage(
            "La fotosíntesis",
            "La fotosíntesis es el proceso por el cual las plantas, las algas y algunas bacterias transforman la energía de la luz solar en energía química. Usando el dióxido de carbono del aire y el agua del suelo, producen glucosa, que les sirve de alimento, y liberan oxígeno como subproducto. Este oxígeno es esencial para la respiración de casi todos los seres vivos. Además, la glucosa producida es la base de las cadenas alimenticias: los animales que comen plantas obtienen de ellas la energía que originalmente vino del Sol. Por eso se dice que la fotosíntesis sostiene la vida en la Tierra."
        ),
        ReadingPassage(
            "La Revolución Industrial",
            "La Revolución Industrial comenzó en Inglaterra a finales del siglo XVIII y transformó profundamente la sociedad. La invención de la máquina de vapor permitió mecanizar la producción, que antes se hacía a mano en talleres pequeños. Surgieron grandes fábricas y las ciudades crecieron rápidamente cuando muchas personas dejaron el campo para trabajar en ellas. Aunque aumentó la producción de bienes y aparecieron nuevos inventos, también trajo problemas como largas jornadas laborales, trabajo infantil y contaminación. Estos cambios sentaron las bases del mundo industrial y tecnológico en el que vivimos hoy."
        ),
        ReadingPassage(
            "El ciclo del agua",
            "El ciclo del agua describe el movimiento continuo del agua en la Tierra. El calor del Sol evapora el agua de los océanos, ríos y lagos, convirtiéndola en vapor que sube a la atmósfera. Allí el vapor se enfría y se condensa formando las nubes. Cuando las gotas se vuelven demasiado pesadas, caen como lluvia, nieve o granizo en un proceso llamado precipitación. Parte de esa agua regresa a los ríos y mares, y otra parte se filtra en el suelo formando aguas subterráneas. Así, el mismo agua se recicla una y otra vez desde hace millones de años."
        )
    )

    fun randomReading(advanced: Boolean = false): ReadingPassage =
        if (advanced) readingAdvancedBank.random() else readingBank.random()

    // --- Mini-lecturas para Preescolar/1º: una oración + pregunta de opción ---
    data class ReadingQuiz(
        val sentence: String,
        val question: String,
        val options: List<String>,
        val answer: String
    )

    private val readingQuizBank = listOf(
        ReadingQuiz("El gato bebe leche.", "¿Qué bebe el gato?",
            listOf("leche", "agua", "jugo", "pan"), "leche"),
        ReadingQuiz("El sol es amarillo.", "¿De qué color es el sol?",
            listOf("amarillo", "azul", "verde", "rojo"), "amarillo"),
        ReadingQuiz("Ana tiene un globo rojo.", "¿Qué tiene Ana?",
            listOf("un globo", "un perro", "una pelota", "un pan"), "un globo"),
        ReadingQuiz("El perro corre en el parque.", "¿Dónde corre el perro?",
            listOf("en el parque", "en la casa", "en la escuela", "en el mar"), "en el parque"),
        ReadingQuiz("Mamá compra pan.", "¿Qué compra mamá?",
            listOf("pan", "leche", "fruta", "queso"), "pan"),
        ReadingQuiz("El pez nada en el agua.", "¿Dónde nada el pez?",
            listOf("en el agua", "en la arena", "en el cielo", "en la mesa"), "en el agua"),
        ReadingQuiz("Luis juega con la pelota.", "¿Con qué juega Luis?",
            listOf("la pelota", "el carro", "la muñeca", "el libro"), "la pelota"),
        ReadingQuiz("La luna sale de noche.", "¿Cuándo sale la luna?",
            listOf("de noche", "de día", "en la tarde", "en verano"), "de noche"),

        // --- Completar la vocal que falta (conciencia fonológica) ---
        ReadingQuiz("🐢  T_RTUGA", "¿Qué vocal falta?",
            listOf("O", "A", "E", "U"), "O"),
        ReadingQuiz("🐰  CON_JO", "¿Qué vocal falta?",
            listOf("E", "A", "O", "I"), "E"),
        ReadingQuiz("🐔  G_LLINA", "¿Qué vocal falta?",
            listOf("A", "E", "O", "U"), "A"),
        ReadingQuiz("🐱  GAT_", "¿Qué vocal falta?",
            listOf("O", "A", "E", "I"), "O"),
        ReadingQuiz("🍎  MANZAN_", "¿Qué vocal falta?",
            listOf("A", "O", "E", "U"), "A"),

        // --- Completar la sílaba que falta ---
        ReadingQuiz("🍎  man_na", "¿Qué sílaba falta?",
            listOf("za", "ta", "pa", "sa"), "za"),
        ReadingQuiz("🥄  cu_ra", "¿Qué sílaba falta?",
            listOf("cha", "ta", "ra", "ma"), "cha"),
        ReadingQuiz("☂️  pa_guas", "¿Qué sílaba falta?",
            listOf("ra", "va", "za", "ta"), "ra"),
        ReadingQuiz("🍌  plá_no", "¿Qué sílaba falta?",
            listOf("ta", "sa", "ma", "pa"), "ta"),
        ReadingQuiz("👻  fantas_", "¿Qué sílaba falta?",
            listOf("ma", "za", "ra", "cha"), "ma"),
        ReadingQuiz("👟  za_tillas", "¿Qué sílaba falta?",
            listOf("pa", "va", "ta", "sa"), "pa"),
        ReadingQuiz("🦋  maripo_", "¿Qué sílaba falta?",
            listOf("sa", "za", "ma", "ra"), "sa"),

        // --- Mini-lecturas con varias preguntas (el texto se repite por pregunta) ---
        ReadingQuiz("La jirafa tiene un cuello muy largo. Es amarilla con manchas de color café.",
            "¿Cómo tiene el cuello la jirafa?",
            listOf("largo", "corto", "gordo", "azul"), "largo"),
        ReadingQuiz("La jirafa tiene un cuello muy largo. Es amarilla con manchas de color café.",
            "¿De qué color son sus manchas?",
            listOf("café", "amarillas", "rojas", "verdes"), "café"),
        ReadingQuiz("El elefante es muy grande. Tiene una trompa larga y orejas grandes. Le gusta bañarse en el río.",
            "¿Qué tiene largo el elefante?",
            listOf("la trompa", "la cola", "las patas", "el pelo"), "la trompa"),
        ReadingQuiz("El elefante es muy grande. Tiene una trompa larga y orejas grandes. Le gusta bañarse en el río.",
            "¿Dónde le gusta bañarse?",
            listOf("en el río", "en el mar", "en la casa", "en la escuela"), "en el río")
    )

    val readingQuizHelp = WorkedExample(
        "Cómo leer la oración",
        listOf(
            "1) Lee despacio, palabra por palabra.",
            "2) Puedes leerla en voz alta.",
            "Ejemplo: «El gato bebe leche.»",
            "Pregunta: ¿Qué bebe el gato? → leche 🥛"
        )
    )

    fun randomReadingQuiz(exclude: String? = null): ReadingQuiz {
        // exclude llega como la PREGUNTA del quiz anterior (Quiz.question en la UI).
        val pool = readingQuizBank.filter { it.question != exclude }.ifEmpty { readingQuizBank }
        return pool.random().let { it.copy(options = it.options.shuffled()) }
    }

    // ---------------- EVALUACIÓN DE RESUMEN (heurística local) ----------------
    fun evaluateSummary(readingText: String, userSummary: String): SummaryResult {
        val cleanSummary = normalize(userSummary)
        val words = cleanSummary.split(Regex("\\s+")).filter { it.isNotBlank() }

        if (words.size < 12) {
            return SummaryResult(
                approved = false,
                score = 40,
                feedback = "Tu resumen es un poco corto para evaluar tu comprensión.",
                suggestions = "Escribe al menos un par de oraciones describiendo de qué trata la lectura."
            )
        }

        val summarySet = words.toHashSet()
        val keywords = normalize(readingText)
            .split(Regex("\\s+"))
            .filter { it.length > 5 }
        val matches = keywords.count { summarySet.contains(it) }

        return if (matches >= 2) {
            SummaryResult(
                approved = true,
                score = minOf(65 + matches * 8, 100),
                feedback = "¡Buen trabajo! Tu resumen demuestra que entendiste las ideas clave de la lectura.",
                suggestions = "Excelente esfuerzo de redacción autónoma."
            )
        } else {
            SummaryResult(
                approved = false,
                score = 55,
                feedback = "Escribiste un buen texto, pero intenta incluir más ideas de la lectura.",
                suggestions = "Relee el texto y menciona de qué tema principal se está hablando."
            )
        }
    }

    private fun normalize(s: String): String {
        val noAccents = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return noAccents.replace(Regex("[^a-z0-9ñ\\s]"), " ")
    }
}
