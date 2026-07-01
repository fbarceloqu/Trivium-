/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { KioskSettings } from "../types";

export function getKotlinTemplates(settings: KioskSettings): {
  name: string;
  filename: string;
  language: string;
  description: string;
  content: string;
}[] {
  const blockSettingsSnippet = settings.blockSettings
    ? `        // Rebote preventivo de Ajustes del Sistema para evitar trucos
        if (packageName == "com.android.settings") {
            bounceToLauncher(this)
            return
        }`
    : `        // El bloqueo de ajustes está desactivado por configuración`;

  const emergencyCallSnippet = settings.emergencyCalls
    ? `        // Permitir llamadas de emergencia (Dialer / Telecomunicaciones nativas)
        if (packageName == "com.android.phone" || 
            packageName == "com.android.server.telecom" || 
            packageName == "com.google.android.dialer") {
            return // Excluir del bloqueo para resguardar la seguridad del menor
        }`
    : `        // Llamadas de emergencia sin excepción directa`;

  return [
    {
      name: "AndroidManifest.xml",
      filename: "AndroidManifest.xml",
      language: "xml",
      description: "Manifiesto de Android nativo con permisos críticos de accesibilidad, boot, primer plano y políticas de dispositivo.",
      content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="${settings.launcherPackage}">

    <!-- Permisos de Sistema para Control Parental y Kiosco Ineludible -->
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.REORDER_TASKS" />
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" 
        tools:ignore="QueryAllPackagesPermission" />
    ${settings.emergencyCalls ? '<uses-permission android:name="android.permission.CALL_PHONE" />' : ""}

    <application
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="Kiosco de Desafíos Inteligentes"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.KioscoSuave">

        <!-- Launcher Kiosco Principal (Establecido como Home predeterminado) -->
        <activity
            android:name=".LauncherActivity"
            android:exported="true"
            android:launchMode="singleInstance"
            android:stateNotNeeded="true"
            android:excludeFromRecents="true"
            android:clearTaskOnLaunch="true">
            <intent-filter android:priority="1000">
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
                
                <!-- Categorías de Home para bloquear el botón físico de Inicio -->
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>

        <!-- Watchdog: Servicio de Accesibilidad para capturar e impedir salidas no deseadas -->
        <service
            android:name=".KioskAccessibilityService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

        <!-- Receptor del Administrador de Dispositivos (Anti-desinstalación por el menor) -->
        <receiver
            android:name=".KioskDeviceAdminReceiver"
            android:label="Kiosco Suave Admin"
            android:description="@string/device_admin_description"
            android:permission="android.permission.BIND_DEVICE_ADMIN"
            android:exported="true">
            <meta-data
                android:name="android.device_admin"
                android:resource="@xml/device_admin" />
            <intent-filter>
                <action android:name="android.app.action.DEVICE_ADMIN_ENABLED" />
            </intent-filter>
        </receiver>

        <!-- Servicio Persistente en Primer Plano (Para que Android nunca mate el monitor) -->
        <service
            android:name=".KioskForegroundService"
            android:foregroundServiceType="specialUse"
            android:enabled="true"
            android:exported="false">
            <property android:name="android.app.PROPERTY_SPECIAL_USE_USAGE_DESCRIPTION"
                android:value="Garantiza la ineludibilidad de los desafíos de matemáticas, inglés por IA y lecturas antes de liberar la tablet." />
        </service>

        <!-- Receptor de Boot para auto-arranque inmediato tras apagar y encender la tablet -->
        <receiver
            android:name=".BootReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter android:priority="999">
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.QUICKBOOT_POWERON" />
            </intent-filter>
        </receiver>

    </application>
</manifest>`
    },
    {
      name: "KioskAccessibilityService.kt",
      filename: "KioskAccessibilityService.kt",
      language: "kotlin",
      description: "Servicio de Accesibilidad nativo (Watchdog). Mientras los desafíos de hoy no estén completados (isLocked == true), cualquier intento de abrir otra app o cerrar el kiosco provoca un rebote inmediato hacia la pantalla del Kiosco de Desafíos. No restringe ninguna app una vez completados.",
      content: `package ${settings.launcherPackage}

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class KioskAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "KioscoWatchdog"
        var isServiceConnected = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceConnected = true
        Log.d(TAG, "Watchdog de Accesibilidad Inicializado")

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = ${settings.watchdogSensitivity === "High" ? 50 : settings.watchdogSensitivity === "Medium" ? 100 : 200}
            flags = AccessibilityServiceInfo.DEFAULT or 
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // 1. Obtener estado de finalización de desafíos (persistido localmente/sincronizado con Firestore)
        val isLocked = SessionStateMachine.isLocked(applicationContext)

        // Mientras no se completen los desafíos, la app Kiosco es la única opción de uso
        if (isLocked) {
            // Si la app en primer plano es el propio Kiosco, se permite continuar
            if (packageName == applicationContext.packageName) return

${emergencyCallSnippet}

${blockSettingsSnippet}

            // Si intenta abrir cualquier otra aplicación (WhatsApp, Juegos, Redes), se intercepta
            // y se vuelve a abrir el kiosco de manera ineludible.
            Log.w(TAG, "Desafíos incompletos. Interceptado intento de abrir: $packageName. Rebotando al Kiosco.")
            bounceToLauncher(applicationContext)
        } else {
            // Una vez que el niño termina los desafíos, puede utilizar todas las aplicaciones permitidas
            // por Family Link de manera libre. El servicio de accesibilidad opera de forma pasiva.
            Log.v(TAG, "Tablet liberada. Niño navega libremente en $packageName")
        }
    }

    private fun bounceToLauncher(context: Context) {
        try {
            val intent = Intent(context, LauncherActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error al forzar reapertura del Kiosco: \${e.message}")
        }
    }

    override fun onInterrupt() {
        Log.e(TAG, "Servicio Watchdog interrumpido por el sistema.")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceConnected = false
        Log.d(TAG, "Servicio Watchdog Apagado.")
    }
}`
    },
    {
      name: "LauncherActivity.kt",
      filename: "LauncherActivity.kt",
      language: "kotlin",
      description: "Actividad Kiosco Principal. Aloja la interfaz con los 10 ejercicios de matemáticas, lecciones de inglés con IA, y comprensión lectora. Bloquea botones de navegación de Android para evitar trampas mientras la tablet esté locked.",
      content: `package ${settings.launcherPackage}

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class LauncherActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "KioscoLauncherActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Arrancar el servicio persistente para resistir recolector de memoria de Android
        startKioskForegroundService()

        // Si los desafíos siguen pendientes, configurar ventana ineludible
        if (SessionStateMachine.isLocked(this)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun startKioskForegroundService() {
        val serviceIntent = Intent(this, KioskForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Contraer barra de notificaciones para evitar trampas (Cerrar diálogos del sistema)
        sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
    }

    // Interceptar y bloquear botón Atrás (Back Button)
    override fun onBackPressed() {
        if (SessionStateMachine.isLocked(this)) {
            Log.i(TAG, "Atrás bloqueado. Se deben completar las tareas escolares.")
            return // Consumir evento, no hacer nada
        }
        super.onBackPressed()
    }

    // Interceptar botones Home y Recientes
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (SessionStateMachine.isLocked(this)) {
            if (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
                Log.i(TAG, "Botón Home/Recientes presionado y bloqueado.")
                return true // Consumir evento nativo
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    // Una vez que el niño termina todos los ejercicios, esta función libera la app y vuelve al launcher estándar de la tablet
    fun onChallengesCompleted() {
        SessionStateMachine.setUnlocked(this)
        Log.i(TAG, "Desafíos aprobados. Liberando tablet.")
        
        // Retornar al lanzador estándar de Android (donde las restricciones de Family Link aplicarán directamente)
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(homeIntent)
        finish()
    }
}`
    },
    {
      name: "SessionStateMachine.kt",
      filename: "SessionStateMachine.kt",
      language: "kotlin",
      description: "Control de ciclo de vida del bloqueo. Persiste el estado de desbloqueo diario para que el niño no tenga que repetir tareas si se apaga el dispositivo.",
      content: `package ${settings.launcherPackage}

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SessionStateMachine {

    private const val PREFS_NAME = "KioscoStatePrefs"
    private const val KEY_LAST_UNLOCKED_DATE = "last_unlocked_date"

    /**
     * Retorna verdadero si la tablet de hoy está bloqueada (LOCKED)
     * esperando que se completen las 10 tareas de mate, inglés por IA y el resumen.
     */
    fun isLocked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastUnlockedDate = prefs.getString(KEY_LAST_UNLOCKED_DATE, "")
        
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // Bloqueado si la última fecha desbloqueada NO coincide con el día de hoy
        return lastUnlockedDate != currentDate
    }

    /**
     * Registra que los desafíos del día de hoy ya fueron aprobados y calificados.
     * Esto deshabilita el watchdog y permite el acceso libre regulado por Family Link.
     */
    fun setUnlocked(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        prefs.edit()
            .putString(KEY_LAST_UNLOCKED_DATE, currentDate)
            .apply()
    }

    /**
     * Resetea el bloqueo manual (Downtime/Bedtime iniciado por papá desde la consola).
     */
    fun setLocked(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_LAST_UNLOCKED_DATE)
            .apply()
    }
}`
    },
    {
      name: "FirebaseSync.kt",
      filename: "FirebaseSync.kt",
      language: "kotlin",
      description: "Escucha remota en tiempo real de Firestore. Conecta la tablet del menor con la base de datos Spark Cloud para recibir bloqueos inmediatos o comandos del tutor.",
      content: `package ${settings.launcherPackage}

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object FirebaseSync {

    private const val TAG = "KioscoFirebaseSync"
    private var firestoreRegistration: ListenerRegistration? = null

    fun startConfigListening(context: Context, deviceId: String) {
        if (firestoreRegistration != null) return

        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("devices").document(deviceId)

        firestoreRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando Firestore: \${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val blockSettings = snapshot.getBoolean("blockSettings") ?: true
                val emergencyCalls = snapshot.getBoolean("emergencyCalls") ?: true
                val forceLock = snapshot.getBoolean("forceLock") ?: false

                val prefs = context.getSharedPreferences("KioscoPrefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putBoolean("blockSettings", blockSettings)
                    putBoolean("emergencyCalls", emergencyCalls)
                    apply()
                }

                if (forceLock) {
                    KioskDeviceAdminReceiver.lockNow(context)
                    // Resetear bandera en la nube
                    docRef.update("forceLock", false)
                }
            }
        }
    }

    fun stopConfigListening() {
        firestoreRegistration?.remove()
        firestoreRegistration = null
    }
}`
    }
  ];
}
