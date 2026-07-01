package com.controlparental.kioscosuave

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ciclo de vida del bloqueo diario. Bloqueado (LOCKED) hasta que el menor
 * completa los retos del día; se desbloquea por el resto de la jornada.
 *
 * LIMITACIÓN CONOCIDA (v1): usa la fecha LOCAL del dispositivo, por lo que el
 * menor podría cambiar la hora para "saltar" el día. En v2 (con Firestore) se
 * validará contra serverTimestamp. Ver memoria trivium-pendientes.
 */
object SessionStateMachine {

    private const val PREFS = "KioscoStatePrefs"
    private const val KEY_LAST_UNLOCKED_DATE = "last_unlocked_date"

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    fun isLocked(ctx: Context): Boolean {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_UNLOCKED_DATE, "") != today()
    }

    fun setUnlocked(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_UNLOCKED_DATE, today())
            .apply()
    }

    /** Re-bloquea (fin de sesión / downtime iniciado por el padre). */
    fun setLocked(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LAST_UNLOCKED_DATE)
            .apply()
    }
}
