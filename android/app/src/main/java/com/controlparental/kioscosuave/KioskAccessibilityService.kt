package com.controlparental.kioscosuave

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

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
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceConnected = true
        Log.d(TAG, "Watchdog de accesibilidad conectado")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        val ctx = applicationContext

        if (!SessionStateMachine.isLocked(ctx)) return
        if (packageName == ctx.packageName) return

        // Permitir llamadas de emergencia si el padre lo habilitó.
        if (ProfileStore.emergencyCalls(ctx) && packageName in EMERGENCY_PACKAGES) {
            Log.i(TAG, "Excepción de emergencia: $packageName permitido.")
            return
        }

        // Bloquear Ajustes explícitamente si está configurado (evita apagar el kiosco).
        if (ProfileStore.blockSettings(ctx) && packageName == SETTINGS_PACKAGE) {
            Log.w(TAG, "Intento de abrir Ajustes bloqueado.")
            bounceToLauncher(ctx)
            return
        }

        Log.w(TAG, "Desafíos incompletos. Rebotando desde $packageName al kiosco.")
        bounceToLauncher(ctx)
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
            Log.e(TAG, "Error al reabrir el kiosco: ${e.message}")
            // Respaldo: como somos el HOME, forzar ir a inicio nos trae de vuelta.
            performGlobalAction(GLOBAL_ACTION_HOME)
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
