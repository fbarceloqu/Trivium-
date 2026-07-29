# Prompts de Gemini para generar contenido de Trivium

Prompts listos para generar más ejercicios con Gemini (AI Studio, la API, o la
futura Fase D). Cada prompt produce **JSON con la forma exacta de los bancos en
`android/.../ChallengeEngine.kt`**, para poder revisarlo y pegarlo directo, o
servirlo desde el backend.

Flujo recomendado: generar → **revisar a mano** (un adulto valida que sea
correcto y apropiado) → pegar en el banco correspondiente o cargar a Firestore.

---

## 1. Inglés · vocabulario con dibujos (Preescolar/1º)

Banco destino: `starterVocab` (`VocabItem(emoji, word, phon, es)`).

```
Eres maestro de inglés para niños hispanohablantes de 6 años que apenas
aprenden a leer. Genera 20 palabras de vocabulario básico en inglés
(animales, comida, cuerpo, casa, escuela, colores) que puedan representarse
con UN emoji estándar de Unicode.

Reglas:
- Palabras de 1 sílaba o 2 sílabas máximo, muy comunes.
- "phon" = pronunciación IPA + entre «» una transcripción figurada al español
  (como la leería un niño mexicano), p. ej. "/dɒg/ («dog»)", "/feɪs/ («féis»)".
- Sin repetir: dog, cat, apple, sun, house, fish, bird, milk, ball, moon,
  water, book, face, eye, nose, mouth, ear, hand.

Devuelve SOLO un arreglo JSON:
[{"emoji": "🐮", "word": "cow", "phon": "/kaʊ/ («cáu»)", "es": "vaca"}, ...]
```

## 2. Inglés · Past Tense (Primaria/Secundaria)

Banco destino: `englishBank` (`EnglishExercise(instruction, question, options,
correctAnswer, explanation, phonetic)`).

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

## 3. Lectura · mini-lecturas con pregunta (Preescolar/1º)

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

## 4. Lectura · completar vocal o sílaba (Preescolar/1º)

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

Devuelve SOLO un arreglo JSON con la misma forma del punto 3.
```

## 5. Lecturas largas (Primaria y Secundaria)

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

## 6. Mate · situaciones/problemas (para ampliar plantillas)

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

## 7. Alineado al plan SEP · 2º de Secundaria (México)

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
usar el prompt 5 pidiendo: "temas alineados a 2º de secundaria SEP:
diversidad cultural de México, ecosistemas mexicanos, historia de México,
derechos humanos".

## 8. Evaluador de resúmenes (ya en producción en `server.ts`)

El prompt endurecido (anti-inyección, con rúbrica) ya está implementado en
`server.ts` → endpoint `POST /api/ai/evaluate-summary`. Úsalo como referencia
si se quiere portar a la Fase D (evaluación desde la tablet vía backend).

---

### Notas de uso
- Modelo recomendado: `gemini-2.5-flash` (o el flash vigente; verificar ID).
- En la API usar `responseMimeType: "application/json"` para forzar JSON.
- Pedir tandas de 20 y descartar las malas es más eficaz que pedir 5 perfectas.
- SIEMPRE revisar el contenido generado antes de dárselo a los niños
  (correctitud, ortografía, edad apropiada).
```
