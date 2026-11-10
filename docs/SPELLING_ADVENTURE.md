# Word Quest — aventura de spelling

Primera integración de los ejercicios gráficos: una aventura de criaturas para
los ejercicios que tienen `visualWord` y usan `LocalStarterEnglish`.
La selección del nivel sigue siendo la existente en `KioskScreen`.

Las referencias de ranas eran ejemplos de interacción, no un tema que copiar.
La dirección visual toma como referencia las aventuras de compañeros de Pokémon
y los animales: ratón eléctrico, zorro, tortuga y búho, con siluetas distintas,
contornos definidos, colores vivos y un paisaje de exploración. Los personajes
se reparten aleatoriamente en cada ronda, sin relación con la respuesta correcta.
Las letras y palabras tienen su propia ficha legible debajo de cada personaje.

## Interacción

- Identificar la letra inicial: escuchar la palabra y seleccionar un compañero.
- Completar la letra inicial: ver `_ee` y seleccionar B.
- Reconocer palabras: escuchar el nombre de una letra y elegir una palabra
  que empiece con ella.
- Armar palabras: seleccionar un compañero por letra. Las letras repetidas
  tienen identidades independientes; BEE usa dos fichas con E. `Undo` permite
  retirar la última letra antes de confirmar.

Se conservan los nombres de las letras en inglés y la confirmación añadidos
en `7f96303`. Seleccionar un compañero lo hace saltar; `Continue` envía una sola
respuesta al evaluador. Después se muestra el resultado y el botón para
avanzar. Los aciertos muestran sonrisas y destellos. El escenario permanece
entre preguntas. No hay avance por temporizador.

Las instrucciones se pronuncian al entrar a cada ronda y pueden repetirse
con `Listen`. La palabra completa no aparece en las instrucciones de armado
ni de letra inicial. El dibujo y el audio dan la pista. Al responder se usa
el repaso hablado existente; `Listen` permite repetir el deletreo completo.

## Integración

`SpellingAdventureRound` adapta preguntas existentes a la presentación y mantiene
las identidades de las fichas. `SpellingAdventure` presenta el escenario y devuelve
la selección. `MultipleChoiceStage` sigue siendo dueño de la calificación,
historial, memoria, sincronización y desbloqueo. Los ejercicios de otros
niveles y las matemáticas conservan su presentación anterior.

Los dibujos son vectores Compose originales; no se descargan imágenes ni se
agregan dependencias. `AdventureArtwork` contiene las cuatro criaturas y el
paisaje, separados de la lógica educativa. Se reutilizan las imágenes de vocabulario existentes.
En horizontal se separan pista y respuestas; en vertical se apilan. El
contenido puede desplazarse y el botón de confirmar queda fuera del scroll.

## Validación

- `tools/harness/SpellingAdventureTest.kt`: armado con letras repetidas, selección
  incorrecta sin autocorrección, preguntas reales del motor y pistas que no
  muestran la respuesta completa.
- Regresión: `StarterEnglishTest.kt` y `SpellingTest.kt`.
- Compilación Android: `:app:assembleDebug`.

Validación del cambio a criaturas: pasó `:app:compileDebugKotlin` y se repitió
`SpellingAdventureTest` (1 200 armados y 300 rondas del motor), sin fallos.

La integración anterior pasó los tres arneses (22 600 ejercicios/casos).
En esa revisión el build completo se detuvo en
`:app:compileDebugJavaWithJavac` por `Unable to establish loopback connection`
en Java/Windows, tanto con JDK 17 como con JDK 21. El empaquetado no se repitió
para este cambio visual y no se generó un APK nuevo.

Revisión manual pendiente en tablet: audio al entrar y al tocar letras,
selección/Undo/confirmación, salto y retroalimentación, cambio de pregunta,
final de etapa y legibilidad en ambas orientaciones.
