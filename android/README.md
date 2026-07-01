# Kiosco Suave — APK Android (v1 standalone)

App nativa del kiosco educativo. **v1 = 100% offline**: los retos de matemáticas,
inglés (past tense) y comprensión lectora, y la evaluación del resumen, se
generan y califican **localmente en Kotlin**. No requiere servidor ni Firebase
(eso llega en v2 para control remoto y IA con Gemini).

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
├─ KioskAccessibilityService.kt # Watchdog (rebote reactivo)
├─ KioskForegroundService.kt    # Servicio persistente
├─ BootReceiver.kt              # Auto-arranque tras reinicio
├─ KioskDeviceAdminReceiver.kt  # Device Admin opcional (Lock Now)
└─ ui/                          # Pantallas Jetpack Compose
```
