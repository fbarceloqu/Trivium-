# Kiosco Suave — APK Android

App nativa del kiosco educativo. El **núcleo es 100% offline**: los retos de
matemáticas, inglés y comprensión lectora, y su evaluación, se generan y
califican **localmente en Kotlin** sin necesitar internet.

**Fase D (opcional):** si hay internet Y una API key de Gemini configurada,
la evaluación del resumen de lectura se hace con IA real (`GeminiClient.kt`,
llamada directa desde la tablet). Si falla, no hay internet, o no hay key, se
degrada automáticamente a la heurística local — nunca se rompe la experiencia.

## Configurar la IA (opcional)

1. Crea/copia `android/local.properties` (si no existe: Android Studio lo crea
   solo al abrir el proyecto, ya trae `sdk.dir`).
2. Agrega una línea con tu API key de [aistudio.google.com/apikey](https://aistudio.google.com/apikey):
   ```
   GEMINI_API_KEY=tu_key_aqui
   ```
3. Sincroniza Gradle y recompila. `local.properties` nunca se sube a git.

⚠️ La key viaja embebida en el APK (llamada directa, sin backend). Es una
decisión aceptada para este proyecto familiar: usa una key de nivel gratuito
sin facturación, así que el peor caso de una fuga es agotar la cuota, no un
cargo de dinero. Si se comparte el APK fuera de la familia, hay que rotar la key.

## Imágenes de vocabulario (opcional)

`app/src/main/res/drawable-nodpi/` contiene imágenes reales de vocabulario
(generadas externamente con el prompt de `docs/GEMINI_PROMPTS.md`, sección
"Imágenes para el vocabulario"). `ChallengeEngine.wordForEmoji()` +
`imageResFor()` en la UI detectan automáticamente si existe un archivo para
una palabra y lo usan en vez del emoji — no requiere tocar código.

⚠️ **Esa carpeta es una carpeta de RECURSOS de Android**: solo puede contener
imágenes (`.png`, `.jpg`, `.webp`) o `.xml`. **Nunca pongas ahí un `.md` u
otro archivo** — rompe la compilación (`mergeDebugResources`/
`packageDebugResources` fallan con "file name must end with .xml or .png").

Convención de nombre: la palabra en inglés, minúsculas, `_` en vez de
espacio (p. ej. `police_officer.jpg`).

## Escenario soportado
- **Una tablet por niño.** Cada dispositivo se configura una vez con el perfil
  del niño (nombre + nivel escolar) protegido por un PIN de padres.
- **Dificultad automática por nivel:** Primaria → fácil (sumas, 5 ejercicios);
  Secundaria → difícil (ecuaciones, 8 ejercicios). Editable en `ChildProfile.kt`.

## Cómo compilar

Requisitos: **Android Studio** (Ladybug o superior) con JDK 17.

1. En Android Studio: *File → Open* y selecciona esta carpeta `android/`.
2. Android Studio generará el `gradle-wrapper.jar` y sincronizará Gradle.
   - (Alternativa por consola si ya tienes Gradle 8.9: `gradle wrapper` y luego
     `./gradlew assembleDebug`.)
3. Conecta una tablet (o emulador) con **depuración USB** y pulsa *Run*, o genera
   el APK con *Build → Build Bundle(s)/APK(s) → Build APK(s)*.

El APK de depuración queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Activación en la tablet (una vez instalado)

1. **Configura el perfil:** al primer arranque, pulsa *Configuración parental*,
   crea el nombre del niño, elige el nivel y define un **PIN de padres**.
2. **Ponlo como Launcher/HOME:** pulsa el botón Home → elige *Kiosco Suave* →
   *Siempre*.
3. **Activa el servicio de accesibilidad (Watchdog):** Ajustes → Accesibilidad →
   *Kiosco Suave* → activar.
4. *(Opcional)* **Administrador de dispositivo** (para *Lock Now*): Ajustes →
   Seguridad → Administradores de dispositivo → activar *Kiosco Suave Admin*.

## Alcance y límites (honestidad de diseño)

Es un **kiosco "suave"**: reencauza al menor y funciona bien con niños pequeños,
pero **es evadible** (Modo Seguro, desactivar accesibilidad). Su función es
convivir con **Google Family Link**, que aporta la coerción dura (impedir
desinstalar/cambiar ajustes). No es una barrera de seguridad inquebrantable.

## Estructura

```
app/src/main/java/com/controlparental/kioscosuave/
├─ LauncherActivity.kt          # HOME: decide setup / kiosco / launcher libre
├─ ParentSetupActivity.kt       # Configuración parental protegida por PIN
├─ ChildProfile.kt              # Perfil + mapeo nivel→dificultad (multi-niño)
├─ ProfileStore.kt              # Persistencia local (SharedPreferences)
├─ SessionStateMachine.kt       # Bloqueo/desbloqueo diario
├─ ChallengeEngine.kt           # Retos y evaluación de resumen OFFLINE
├─ GeminiClient.kt              # Evaluación de resumen con IA (Fase D, opcional)
├─ KioskAccessibilityService.kt # Watchdog (rebote reactivo)
├─ KioskForegroundService.kt    # Servicio persistente
├─ BootReceiver.kt              # Auto-arranque tras reinicio
├─ KioskDeviceAdminReceiver.kt  # Device Admin opcional (Lock Now)
└─ ui/                          # Pantallas Jetpack Compose
```
