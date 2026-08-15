# Arneses de verificación

Cinco programas que ejercitan la lógica de Trivium **sin emulador ni Gradle**.

Encontraron **7 bugs reales** que habrían llegado a las tablets: respuestas
fuera de las opciones, distractores que colapsaban dejando el ejercicio
resoluble por descarte, jerarquías visuales que el diseño afirmaba pero el
código no cumplía.

| Arnés | Qué verifica |
|---|---|
| `GenSmokeTest` | 31,200 ejercicios: la respuesta está entre las opciones, hay 4 y no se repiten |
| `MemoryTest` | Simulación de 40 días: intervalos crecientes, lo flojo se practica más, el formato rota |
| `ValidatorTest` | 15 casos de narrativa de IA: acepta lo válido, rechaza lo que cambia números o regala la respuesta |
| `GuideTest` | Guías: emparejan sin duplicar, la urgencia escala, no monopolizan la sesión |
| `MetricsTest` | Las dos tablets reales × 2 orientaciones × 2 densidades: nada se desborda |

## Por qué están aquí y no en `src/test/`

Porque funcionan **hoy**, sin tocar la configuración del build.

Todo lo que ejercitan —`curriculum/`, `ChallengeEngine.kt`, `ChildProfile.kt`,
`ui/AdaptiveMetrics.kt`, `ui/Level.kt`— es Kotlin puro sin dependencias de
Android, así que se compilan con el compilador de Kotlin suelto.

Convertirlos a JUnit es el siguiente paso natural (ver abajo), pero exige
añadir dependencias de test a `build.gradle.kts`, y un error ahí no rompe un
test: rompe la compilación del proyecto entero.

## Cómo ejecutarlos ahora

Desde PowerShell, en la raíz del repo. Ajusta las rutas si tu SDK está en otro
sitio:

```powershell
$java = "C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
$m2 = "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1"
$jars = @(
  (Get-ChildItem "$m2\org.jetbrains.kotlin\kotlin-compiler-embeddable\2.1.0\*\*.jar" | Select -First 1).FullName,
  (Get-ChildItem "$m2\org.jetbrains.kotlin\kotlin-stdlib\2.1.0\*\*.jar" | Select -First 1).FullName,
  (Get-ChildItem "$m2\org.jetbrains.kotlinx\kotlinx-coroutines-core-jvm\1.9.0\*\*.jar" | Select -First 1).FullName,
  (Get-ChildItem "$m2\org.jetbrains.kotlin\kotlin-reflect\*\*\*.jar" | Select -First 1).FullName,
  (Get-ChildItem "$m2\org.jetbrains.kotlin\kotlin-script-runtime\2.1.0\*\*.jar" | Select -First 1).FullName,
  (Get-ChildItem "$m2\org.jetbrains.intellij.deps\trove4j\*\*\*.jar" | Select -First 1).FullName,
  (Get-ChildItem "$m2\org.jetbrains\annotations\23.0.0\*\*.jar" | Select -First 1).FullName
)
$src = "android\app\src\main\java\com\controlparental\kioscosuave"
$out = "$env:TEMP\trivium-harness"

& $java -cp ($jars -join ";") org.jetbrains.kotlin.cli.jvm.K2JVMCompiler `
  "$src\ChallengeEngine.kt" "$src\ChildProfile.kt" "$src\curriculum" `
  "$src\ui\AdaptiveMetrics.kt" "$src\ui\Level.kt" `
  "tools\harness\MetricsTest.kt" `
  -classpath $jars[1] -d $out -no-stdlib

& $java -cp "$out;$($jars[1])" MetricsTestKt
```

Cambia el `.kt` y la clase final (`MetricsTestKt`, `MemoryTestKt`…) para correr
otro arnés. Cada uno tiene su propio `main()`, así que van de uno en uno.

⚠️ Desde Bash hay que exportar `MSYS2_ARG_CONV_EXCL='*'` o el classpath con `;`
se corrompe.

## Convertirlos a JUnit (pendiente)

1. En `android/app/build.gradle.kts`:
   ```kotlin
   testImplementation("junit:junit:4.13.2")
   ```
2. Mover los `.kt` a `android/app/src/test/java/com/controlparental/kioscosuave/`.
3. En cada uno: `fun main()` pasa a `@Test fun nombre()`, y los `check(cond, msg)`
   a `assertTrue(msg, cond)`.
4. `./gradlew test`

El cambio es mecánico, pero conviene hacerlo con el proyecto compilando y
verificando el build justo después.
