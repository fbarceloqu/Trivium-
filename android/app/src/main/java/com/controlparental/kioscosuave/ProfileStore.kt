package com.controlparental.kioscosuave

import android.content.Context
import java.security.MessageDigest

/**
 * Persistencia local del perfil del niño y ajustes del kiosco (SharedPreferences).
 * Diseñado para ser migrable a Firestore: los mismos campos vivirán bajo
 * families/{familyId}/children/{childId} cuando exista la nube.
 */
object ProfileStore {

    private const val PREFS = "KioscoProfilePrefs"
    private const val KEY_CONFIGURED = "configured"
    private const val KEY_CHILD_NAME = "child_name"
    private const val KEY_GRADE = "grade_level"
    private const val KEY_PIN_HASH = "parent_pin_hash"
    private const val KEY_BLOCK_SETTINGS = "block_settings"
    private const val KEY_EMERGENCY_CALLS = "emergency_calls"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isConfigured(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_CONFIGURED, false)

    fun getProfile(ctx: Context): ChildProfile {
        val p = prefs(ctx)
        return ChildProfile(
            name = p.getString(KEY_CHILD_NAME, "Estudiante") ?: "Estudiante",
            grade = GradeLevel.fromName(p.getString(KEY_GRADE, GradeLevel.PRIMARIA.name))
        )
    }

    fun saveProfile(
        ctx: Context,
        name: String,
        grade: GradeLevel,
        pin: String,
        blockSettings: Boolean,
        emergencyCalls: Boolean
    ) {
        prefs(ctx).edit()
            .putBoolean(KEY_CONFIGURED, true)
            .putString(KEY_CHILD_NAME, name.trim().ifBlank { "Estudiante" })
            .putString(KEY_GRADE, grade.name)
            .putString(KEY_PIN_HASH, sha256(pin))
            .putBoolean(KEY_BLOCK_SETTINGS, blockSettings)
            .putBoolean(KEY_EMERGENCY_CALLS, emergencyCalls)
            .apply()
    }

    /** Actualiza el perfil conservando el PIN actual (edición sin cambiarlo). */
    fun updateProfileKeepPin(
        ctx: Context,
        name: String,
        grade: GradeLevel,
        blockSettings: Boolean,
        emergencyCalls: Boolean
    ) {
        prefs(ctx).edit()
            .putBoolean(KEY_CONFIGURED, true)
            .putString(KEY_CHILD_NAME, name.trim().ifBlank { "Estudiante" })
            .putString(KEY_GRADE, grade.name)
            .putBoolean(KEY_BLOCK_SETTINGS, blockSettings)
            .putBoolean(KEY_EMERGENCY_CALLS, emergencyCalls)
            .apply()
    }

    fun blockSettings(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_BLOCK_SETTINGS, true)

    fun emergencyCalls(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_EMERGENCY_CALLS, true)

    fun verifyPin(ctx: Context, pin: String): Boolean {
        val stored = prefs(ctx).getString(KEY_PIN_HASH, null) ?: return false
        return stored == sha256(pin)
    }

    fun hasPin(ctx: Context): Boolean =
        prefs(ctx).getString(KEY_PIN_HASH, null) != null

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
