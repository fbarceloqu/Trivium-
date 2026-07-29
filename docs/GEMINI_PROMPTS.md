# Prompts para generar contenido de Trivium (Gemini, ChatGPT, o cualquier LLM)

Prompts listos para generar más ejercicios. Cada prompt produce **JSON con la
forma exacta de los bancos en `android/.../ChallengeEngine.kt`**, para poder
revisarlo y pegarlo directo, o servirlo desde el backend. Funcionan igual con
Gemini (AI Studio/API), ChatGPT, o cualquier otro modelo — no dependen de
ningún proveedor en particular.

Flujo recomendado: generar → **revisar a mano** (un adulto valida que sea
correcto y apropiado) → pegar en el banco correspondiente (pásamelo a mí y yo
lo integro al código Kotlin).

⚠️ **Tamaño deliberadamente acotado.** No se pide un banco de "10,000
palabras estilo MCER" — Trivium es un kiosco para 3 niños, no una plataforma
de idiomas. Los tamaños de abajo (~150 palabras, ~180 ejercicios) ya dan
semanas de variedad sin repetirse; pedir mucho más es esfuerzo sin beneficio
real para este proyecto.

---

## 1. Inglés · vocabulario con dibujos (Preescolar/1º) — AMPLIADO

Banco destino: `starterVocab` (`VocabItem(emoji, word, phon, es)`). El campo
`emoji` ahora es de "mejor esfuerzo": si una palabra no tiene un buen emoji
único, se deja vacío para poder emparejarla con una imagen real más adelante
(no bloquea la generación del vocabulario en sí).

```
Eres maestro de inglés para niños hispanohablantes de 6 años (recién salidos
de preescolar). Genera 150 palabras de vocabulario básico en inglés,
repartidas en estas categorías (con esta cantidad aproximada cada una):

- animales (30): mascotas, granja, selva, mar
- comida (25): frutas, verduras, comidas cotidianas, bebidas
- colores (10)
- formas (8): círculo, cuadrado, triángulo, estrella, corazón...
- familia (10): mamá, papá, hermano, abuela...
- cuerpo (12): más allá de cara (mano, pie, brazo, pierna, cabello...)
- ropa (10)
- casa y muebles (10)
- escuela (10): mochila, lápiz, libro, tijeras...
- transporte (10)
- naturaleza y clima (10)
- emociones (8): feliz, triste, enojado, cansado... (hay emoji de caritas)
- números en palabra, uno a diez (10)

Reglas:
- Palabras de 1 o 2 sílabas en inglés, muy comunes, concretas (nada
  abstracto), apropiadas para un niño de 6 años.
- NO repitas estas 18 ya existentes: dog, cat, apple, sun, house, fish, bird,
  milk, ball, moon, water, book, face, eye, nose, mouth, ear, hand.
- "phon" = pronunciación IPA + entre «» una transcripción figurada al español
  (como la leería un niño mexicano), p. ej. "/dɒg/ («dog»)".
- "emoji": SOLO si existe un emoji Unicode estándar claro y reconocible para
  la palabra; si no hay uno bueno, deja "emoji": "" (cadena vacía). No
  inventes combinaciones de varios emojis.
- "category": una de las categorías de la lista de arriba, en español y en
  minúsculas (p. ej. "animales").

Devuelve SOLO un arreglo JSON, sin comentarios ni texto adicional:
[{"emoji": "🐘", "word": "elephant", "phon": "/ˈɛlɪfənt/ («élefant»)",
  "es": "elefante", "category": "animales"}, ...]
```

## 2. Inglés · Past Tense básico (Primaria/Secundaria)

Banco destino: `englishBank` — nivel básico, ya cubierto (no hace falta
regenerar salvo que quieras más variedad). Se deja aquí como referencia del
formato exacto que reutiliza el prompt 3.

```
Eres tutor de inglés estilo Duolingo para niños hispanohablantes de
secundaria. Genera 20 ejercicios de opción múltiple sobre PAST SIMPLE
(regulares e irregulares), variados: completar hueco, identificar el verbo
en pasado, traducir una frase corta, y elegir la forma negativa/pregunta
con "did".

Reglas:
- "instruction" y "explanation" en español, tono motivador y breve.
- "question" en inglés (o la frase a traducir en español).
- Exactamente 4 "options"; una sola correcta ("correctAnswer" debe ser
  idéntica a una opción).
- "phonetic" = pronunciación del verbo clave: IPA + «transcripción figurada
  al español», p. ej. "went = /wɛnt/ («uént»)".
- Distractores plausibles (goed, eated...) que reflejen errores típicos.

Devuelve SOLO un arreglo JSON:
[{"instruction": "...", "question": "...", "options": ["...","...","...","..."],
  "correctAnswer": "...", "explanation": "...", "phonetic": "..."}, ...]
```

## 3. Inglés · gramática avanzada (Secundaria) — NUEVO

Banco destino: nuevo (p. ej. `englishAdvancedUnits`), **misma forma exacta**
que el punto 2 (`EnglishExercise`). Pensado para que Secundaria tenga temas
más complejos que Primaria, no solo Past Tense repetido. Genera esto en 9
tandas (una por tema) para no saturar una sola respuesta del modelo.

```
Eres tutor de inglés para adolescentes mexicanos de secundaria (12-14 años),
estilo Duolingo pero con gramática de nivel intermedio. Genera 20 ejercicios
de opción múltiple sobre: {TEMA}.

Temas a cubrir (uno por tanda, pide esta plantilla 9 veces cambiando {TEMA}):
1. Comparativos y superlativos (bigger, the biggest, more interesting...)
2. Verbos modales: can/could, must/have to, should, might
3. Primer condicional (If + presente, will + verbo)
4. Verbos frasales comunes (give up, look for, turn on/off, get up, take off)
5. Voz pasiva básica (is/was + participio)
6. Preguntas con Wh- + do/does/did en distintos tiempos
7. Estilo indirecto básico (reported speech: "he said that...")
8. Conectores (because, although, however, so that)
9. Vocabulario de nivel intermedio: tecnología, medio ambiente, profesiones,
   opiniones y emociones complejas

Reglas:
- "instruction" y "explanation" en español, claras, sin ser infantiles
  (el público es adolescente, no niño pequeño).
- "question" en inglés; variar el tipo: completar hueco, identificar el
  error, elegir la reformulación correcta, traducir una frase.
- Exactamente 4 "options"; "correctAnswer" idéntica a una opción;
  distractores que reflejen errores típicos de un hispanohablante.
- "phonetic" = pronunciación de la palabra/expresión clave de esa pregunta,
  IPA + «transcripción figurada al español».

Devuelve SOLO un arreglo JSON (misma forma que el ejemplo de abajo), sin
comentarios ni texto adicional:
[{"instruction": "...", "question": "...", "options": ["...","...","...","..."],
  "correctAnswer": "...", "explanation": "...", "phonetic": "..."}, ...]
```

Con las 9 tandas de 20 se obtienen ~180 ejercicios nuevos — suficiente para
que Secundaria no vea el mismo ejercicio dos veces en semanas.

## 4. Lectura · mini-lecturas con pregunta (Preescolar/1º)

Banco destino: `readingQuizBank` (`ReadingQuiz(sentence, question, options, answer)`).

```
Eres autor de lecturas para niños de 6 años que están aprendiendo a leer
(recién salidos de preescolar, español de México). Genera 20 ítems de
comprensión lectora ultra simples.

Reglas:
- "sentence": UNA oración de 4 a 8 palabras, vocabulario cotidiano, sílabas
  simples (evita trabadas complejas), termina en punto.
- "question": pregunta directa cuya respuesta está literal en la oración.
- 4 "options" cortas; "answer" idéntica a una opción.
- Temas: animales, familia, comida, juegos, naturaleza. Nada de miedo.

Devuelve SOLO un arreglo JSON:
[{"sentence": "El gato bebe leche.", "question": "¿Qué bebe el gato?",
  "options": ["leche","agua","jugo","pan"], "answer": "leche"}, ...]
```

## 5. Lectura · completar vocal o sílaba (Preescolar/1º)

Mismo banco `readingQuizBank` (la UI detecta el `_` y cambia la consigna).

```
Eres maestro de lectoescritura de 1º de primaria (español de México).
Genera 20 ítems para completar palabras, mitad de VOCAL faltante y mitad de
SÍLABA faltante.

Reglas:
- "sentence": un emoji del objeto + espacio + la palabra con UN hueco "_"
  (vocal en MAYÚSCULAS: "🐢  T_RTUGA"; sílaba en minúsculas: "🍎  man_na").
- "question": "¿Qué vocal falta?" o "¿Qué sílaba falta?" según el caso.
- 4 "options" (vocales o sílabas de una consonante+vocal, cha/lla/rra
  permitidas); "answer" idéntica a una opción y correcta para la palabra.
- Palabras de 2 a 4 sílabas, cotidianas, representables con emoji estándar.
- Verifica que al sustituir el "_" por "answer" la palabra quede bien escrita.

Devuelve SOLO un arreglo JSON con la misma forma del punto 4.
```

## 6. Lecturas largas (Primaria y Secundaria)

Banco destino: `readingBank` (cortas) y `readingAdvancedBank` (amplias),
`ReadingPassage(title, text)`. También sirve para `/api/ai/reading-passage`
del backend web.

```
Eres escritor de textos escolares en español. Genera 10 lecturas de
comprensión: 5 para PRIMARIA (3 oraciones, ~50 palabras, ciencia/naturaleza
fascinante) y 5 para SECUNDARIA (5-7 oraciones, 90-120 palabras, ciencia,
historia o geografía con datos concretos).

Reglas:
- Información verídica y verificable; sin opiniones.
- "title" corto y atractivo.
- Marca cada una con "level": "primaria" o "secundaria".

Devuelve SOLO un arreglo JSON:
[{"level": "primaria", "title": "...", "text": "..."}, ...]
```

## 7. Mate · situaciones/problemas (para ampliar plantillas)

La mate se genera por código con números aleatorios (no banco). Este prompt
sirve para idear NUEVAS PLANTILLAS de problemas que luego se programan en
`ChallengeEngine.kt` con variables.

```
Eres diseñador de ejercicios de matemáticas. Propón 10 plantillas de
problemas de contexto para {NIVEL: preescolar 6 años | primaria 8-10 años |
secundaria 12-14 años}, donde los números sean variables.

Formato por plantilla:
- "enunciado": con placeholders {a}, {b}, {nombre}, {objeto}.
- "operacion": fórmula de la respuesta (p. ej. "(a*b)+c").
- "restricciones": rangos de las variables para que el resultado sea entero
  y razonable a la edad.
- "pasos": explicación paso a paso con los placeholders (estilo "el {b} está
  sumando → pasa restando").

Devuelve SOLO un arreglo JSON.
```

## 8. Alineado al plan SEP · 2º de Secundaria (México)

Referencia oficial: https://conocetuslibros.sep.gob.mx/sec2 (colección Sk'asolil,
Nueva Escuela Mexicana). Libros completos en https://libros.conaliteg.gob.mx.
Campos formativos: Lenguajes · Saberes y pensamiento científico · Ética,
naturaleza y sociedades · De lo humano y lo comunitario.

```
Eres profesor de 2º de secundaria en México y conoces el plan de la Nueva
Escuela Mexicana (colección Sk'asolil). Genera 20 ejercicios de opción
múltiple del campo "Saberes y pensamiento científico" para repaso diario,
cubriendo el temario típico de 2º:

- Multiplicación y división de fracciones y decimales
- Ecuaciones lineales de una incógnita (incluye contextos)
- Proporcionalidad directa e inversa, porcentajes
- Sucesiones y expresiones algebraicas equivalentes
- Perímetros, áreas y volúmenes de prismas
- Probabilidad clásica y estadística básica (media, mediana)

Reglas:
- Todo en español; enunciados con contextos mexicanos cotidianos.
- 4 opciones, una correcta; distractores basados en errores típicos.
- "steps": procedimiento paso a paso estilo "el 8 está sumando → pasa
  restando" (el que usa la app).
- Resultados numéricos limpios (enteros o decimales de 1 cifra).

Devuelve SOLO un arreglo JSON:
[{"question": "...", "options": ["..."], "answer": "...",
  "steps": ["...", "..."]}, ...]
```

Para lecturas del campo "Lenguajes" o "Ética, naturaleza y sociedades",
usar el prompt 6 pidiendo: "temas alineados a 2º de secundaria SEP:
diversidad cultural de México, ecosistemas mexicanos, historia de México,
derechos humanos".

## 9. Evaluador de resúmenes (ya en producción)

El prompt endurecido (anti-inyección, con rúbrica) ya está implementado en
`server.ts` (backend web) y en `GeminiClient.kt` (APK, Fase D). Úsalo como
referencia si se quiere ajustar en algún otro lugar.

---

### Notas de uso
- Modelo recomendado: `gemini-3.5-flash` (o el flash vigente; verificar ID);
  con ChatGPT, cualquier versión reciente funciona igual de bien para esto.
- En la API de Gemini usar `responseMimeType: "application/json"` para forzar JSON.
- Pedir tandas de 20 y descartar las malas es más eficaz que pedir pocas perfectas.
- SIEMPRE revisar el contenido generado antes de dárselo a los niños
  (correctitud, ortografía, edad apropiada).
- Cuando tengas el JSON de vuelta, pégamelo a mí y yo lo integro al banco
  correspondiente en `ChallengeEngine.kt` (verifico forma, evito duplicados,
  hago el wiring de código).
