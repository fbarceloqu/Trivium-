package com.controlparental.kioscosuave

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Rearranca el servicio en primer plano tras reiniciar la tablet. Al ser el
 * HOME, el kiosco vuelve a mostrarse solo tras el boot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("KioscoBoot", "Boot completado. Iniciando servicio del kiosco.")
            if (ProfileStore.isConfigured(context)) {
                KioskForegroundService.start(context)
            }
        }
    }
}
