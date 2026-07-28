package com.controlparental.kioscosuave

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

/**
 * Watchdog (modo suave). Mientras el día esté bloqueado, si el niño abre otra
 * app se le "rebota" de vuelta al kiosco. Es REACTIVO (no una barrera de
 * seguridad): puede haber un parpadeo y es evadible con Safe Mode / apagando
 * este servicio. Su valor real es reencauzar al menor y (a futuro) auditar.
 */
class KioskAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "KioscoWatchdog"
        var isServiceConnected = false
            private set

        private val EMERGENCY_PACKAGES = setOf(
            "com.android.phone",
            "com.android.server.telecom",
            "com.google.android.dialer",
            "com.android.dialer"
        )
        private const val SETTINGS_PACKAGE = "com.android.settings"

        // Ventanas del sistema que NO deben provocar rebote: la barra/panel del
        // sistema y los teclados (si rebotáramos al abrir el teclado, el niño no
        // podría escribir su resumen dentro del propio kiosco).
        private val SYSTEM_UI_PACKAGES = setOf(
            "com.android.systemui"
        )
    }

    /** Paquetes de los teclados (IME) instalados; se calculan al conectar. */
    private var imePackages: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceConnected = true
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imePackages = imm.inputMethodList.map { it.packageName }.toSet()
        Log.d(TAG, "Watchdog conectado. Teclados excluidos: $imePackages")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        val ctx = applicationContext

        if (!SessionStateMachine.isLocked(ctx)) return
        if (packageName == ctx.packageName) return

        // Ignorar ventanas del sistema y teclados: no son "salidas" del kiosco.
        if (packageName in SYSTEM_UI_PACKAGES || packageName in imePackages) return

        // Permitir llamadas de emergencia si el padre lo habilitó.
        if (ProfileStore.emergencyCalls(ctx) && packageName in EMERGENCY_PACKAGES) {
            Log.i(TAG, "Excepción de emergencia: $packageName permitido.")
            return
        }

        // Bloquear Ajustes explícitamente si está configurado (evita apagar el kiosco).
        if (ProfileStore.blockSettings(ctx) && packageName == SETTINGS_PACKAGE) {
            Log.w(TAG, "Intento de abrir Ajustes bloqueado.")
            ProgressSync.reportEvasion(ctx, packageName)
            bounceToLauncher(ctx)
            return
        }

        Log.w(TAG, "Desafíos incompletos. Rebotando desde $packageName al kiosco.")
        ProgressSync.reportEvasion(ctx, packageName)
        bounceToLauncher(ctx)
    }

    private fun bounceToLauncher(context: Context) {
        // MECANISMO PRINCIPAL: ir a HOME. Como Kiosco Suave ES el launcher
        // predeterminado, esto nos trae de vuelta y está SIEMPRE permitido para
        // un servicio de accesibilidad. (startActivity desde background es
        // bloqueado silenciosamente por Android 10+ — no lanza excepción, solo
        // no hace nada, por eso no puede ser el mecanismo principal.)
        performGlobalAction(GLOBAL_ACTION_HOME)

        // Refuerzo best-effort: si el sistema lo permite, traer la Activity al
        // frente explícitamente (en algunos OEM el HOME tarda un ciclo).
        try {
            val intent = Intent(context, LauncherActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "startActivity de refuerzo falló (esperado en background): ${e.message}")
        }
    }

    override fun onInterrupt() {
        Log.e(TAG, "Watchdog interrumpido por el sistema.")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceConnected = false
        Log.d(TAG, "Watchdog apagado.")
    }
}
