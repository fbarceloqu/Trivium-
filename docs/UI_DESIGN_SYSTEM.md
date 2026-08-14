# Trivium · Sistema de diseño por nivel educativo

> Documento de diseño previo a la implementación. El **motor no se toca**:
> generación, memoria, repetición espaciada, calificación, desbloqueo,
> persistencia y Firebase quedan exactamente como están. Todo lo de aquí vive
> en `ui/`.

## 1. Diagnóstico de la interfaz actual

La UI actual tiene **un solo lenguaje visual con un interruptor de tamaño**
(`big: Boolean`, derivado de `grade == PREESCOLAR`). Eso produce dos problemas
que no se arreglan ajustando números:

| Problema | Causa real |
|---|---|
| Secundaria se siente infantil | Comparte el mismo lenguaje que preescolar, solo con letra más chica. Mismos emojis gigantes, mismas tarjetas redondeadas enormes, mismo tono. |
| Primaria no explota lo visual | El "modo grande" agranda el texto, pero no cambia la naturaleza del ejercicio: sigue siendo texto con un emoji al lado. |
| Sensación de dashboard | La línea `Últimas 1/5 · Aciertos: 1/5 · Precisión: 100% (meta 80%)` es un reporte analítico compitiendo con la pregunta. |
| Jerarquía plana | Saludo, pestañas, título de etapa, estadísticas y pregunta tienen pesos visuales parecidos. |

**Conclusión:** hace falta una segunda dimensión. Hoy las métricas responden
solo a *cuánto espacio hay*; deben responder también a *quién está mirando*.

## 2. Las dos dimensiones

```
                 ESPACIO  (ya implementado en AdaptiveMetrics)
                 ancho × alto → escala, modo apilado o dos paneles

    NIVEL   ┌──────────────────────┬──────────────────────┐
            │  PRIMARY             │  SECONDARY           │
            │  Gael (1º primaria)  │  Camila y gretel     │
            │  SM-X210             │  SM-T500 ×2          │
            └──────────────────────┴──────────────────────┘
```

`big: Boolean` se sustituye por `level: Level` (`PRIMARY` / `SECONDARY`). No es
un renombrado: cambia **qué** se dibuja, no solo de qué tamaño.

Mapeo desde el perfil, sin tocar `ChildProfile`:
`PREESCOLAR` y `PRIMARIA` → `PRIMARY`; `SECUNDARIA` → `SECONDARY`.

## 3. Qué distingue a cada lenguaje

| Aspecto | PRIMARY | SECONDARY |
|---|---|---|
| Intención | "Estoy jugando y aprendiendo" | "Es una herramienta moderna para estudiar" |
| Protagonista | La representación visual | El enunciado |
| Color | Acentos saturados, fondos de tarjeta con tinte | Neutros, acento solo en lo interactivo |
| Emojis | Sí, con función (contar, representar) | Solo funcionales; nunca decorativos |
| Tarjetas | Radio amplio, sombra suave | Radio contenido, borde en vez de sombra |
| Densidad | Baja: pocos elementos, grandes | Media: más información simultánea |
| Progreso | `● ● ○ ○ ○` (puntos) | `3 / 10` + barra fina |
| Feedback correcto | `🎉 ¡Correcto!` + operación visual | `✓ Correcto` + razón conceptual |
| Feedback incorrecto | `Casi. Mira:` + representación | `La respuesta era X.` + porqué |
| Audio y ayuda | Visibles, con etiqueta | Iconos discretos |
| Respuestas | Botones altos, 2 columnas máx. | Botones compactos, hasta 2 columnas |

**Lo que comparten** (identidad Trivium): tema oscuro, la misma paleta base,
esquinas redondeadas, la misma tipografía, y el mismo comportamiento
responsive del punto 5.

## 4. Jerarquía visual objetivo

Reordenar el peso visual así, en ambos niveles:

```
1. Pregunta / contenido del ejercicio   ← protagonista
2. Respuestas
3. Feedback
4. Progreso                             ← simplificado
5. Controles (audio, ayuda)
6. Cabecera y navegación                ← lo más ligero
```

Cambios concretos frente a hoy:

- **Estadísticas fuera del ejercicio.** `Últimas X/Y · Aciertos · Precisión`
  se reduce a un indicador de progreso. El detalle ya está en el panel de
  padres, que es donde tiene sentido.
- **Título de etapa contextual.** "Matemáticas · operaciones y situaciones"
  puede vivir en la pestaña activa, no en una línea propia.
- **Cabecera mínima durante el ejercicio.** El saludo pertenece a la pantalla
  de inicio; durante la práctica basta con la navegación y el candado.

## 5. Responsive (ya resuelto, se conserva)

`AdaptiveMetrics` ya hace lo correcto y **no hay que rehacerlo**: escala por la
dimensión escasa (el menor entre ancho útil y altura), dos paneles en
horizontal, dibujos dimensionados con `fitGridItem` desde el espacio real, y
reparto por `weight` en vez de alturas fijas. Está verificado contra las dos
tablets en ambas orientaciones y densidades.

Lo único que se le añade es el eje `Level`.

## 6. Componentes

Un solo árbol de componentes con variante por nivel. **No duplicar pantallas.**

```
ui/design/
  Level.kt              PRIMARY | SECONDARY + mapeo desde GradeLevel
  TriviumTheme.kt       colores, formas y tipografía por nivel
  components/
    TriviumHeader       compacta; el saludo solo en inicio
    SubjectSelector     pestañas con icono; alto contenido
    ProgressIndicator   puntos (PRIMARY) | fracción + barra (SECONDARY)
    QuestionArea        enunciado; variante por nivel
    VisualExercise      rejilla de dibujos (usa fitGridItem)
    AnswerGrid          columnas según ancho real y longitud del texto
    AnswerButton        estados: normal, elegida, correcta, incorrecta
    FeedbackCard        educativo, no solo veredicto
    NextButton          SIEMPRE fuera del área desplazable
    SecondaryControls   audio y ayuda
```

Regla: los componentes leen `LocalMetrics` y `LocalLevel`. Ninguna pantalla
escribe un número suelto.

## 7. Tipos de ejercicio

`QuestionArea` no debe ser una tarjeta rígida. Se define un contrato:

```
ExerciseKind = MULTIPLE_CHOICE | VISUAL_COUNT | FILL_BLANK
             | MATCH | ORDER | READING | OPEN
```

Hoy el motor produce opción múltiple, conteo visual y lectura. El resto queda
declarado para que añadir uno no obligue a rediseñar. **El motor no cambia**:
el tipo se deduce del contenido que ya genera.

## 8. Orden de implementación

1. `Level` + extender `AdaptiveMetrics` con el eje de nivel (Kotlin puro →
   verificable con arnés).
2. `TriviumTheme` con las dos variantes.
3. Componentes, de abajo hacia arriba: `AnswerButton` → `AnswerGrid` →
   `QuestionArea` → `FeedbackCard`.
4. Recomponer `KioskScreen` con esos componentes, sin tocar su lógica de
   estado (ventana móvil, `ProgressSync`, memoria, TTS).
5. Verificar las tres materias × los dos niveles.

## 9. Riesgos

- **Regresión funcional.** `MultipleChoiceStage` mezcla estado y presentación.
  Al extraer componentes hay que mover *solo* el dibujado y dejar intactos
  `history`, `passed`, `onResult` y las llamadas a `ProgressSync`.
- **Verificación.** La capa Compose no se puede compilar en el entorno del
  asistente (Gradle no abre su socket local). Lo verificable es la aritmética
  de métricas; el resto exige build en Android Studio.
