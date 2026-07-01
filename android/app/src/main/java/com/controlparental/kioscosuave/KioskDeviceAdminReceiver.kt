package com.controlparental.kioscosuave

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.util.Log

/**
 * Device Admin (legacy) opcional. Solo habilita 'force-lock' (apagar la pantalla
 * de inmediato). NO impide desinstalar la app: eso requiere Device Owner, que en
 * este proyecto se descartó para coexistir con Family Link (modo suave).
 */
class KioskDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "KioscoDeviceAdmin"

        fun componentName(context: Context) =
            ComponentName(context, KioskDeviceAdminReceiver::class.java)

        fun isActive(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            return dpm.isAdminActive(componentName(context))
        }

        /** Apaga/bloquea la pantalla si el admin está activo. */
        fun lockNow(context: Context) {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (dpm.isAdminActive(componentName(context))) {
                dpm.lockNow()
            } else {
                Log.w(TAG, "lockNow ignorado: Device Admin no está activo.")
            }
        }
    }

    override fun onEnabled(context: Context, intent: android.content.Intent) {
        Log.i(TAG, "Device Admin habilitado.")
    }

    override fun onDisabled(context: Context, intent: android.content.Intent) {
        Log.i(TAG, "Device Admin deshabilitado.")
    }
}
