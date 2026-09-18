package com.controlparental.kioscosuave

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

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
    private const val KEY_PIN_VERIFIER = "parent_pin_verifier"
    private const val KEY_PIN_SALT = "parent_pin_salt"
    private const val KEY_LEGACY_PIN_HASH = "parent_pin_hash"
    private const val KEY_BLOCK_SETTINGS = "block_settings"
    private const val KEY_EMERGENCY_CALLS = "emergency_calls"
    private const val KEY_CLOUD_CHILD_ID = "cloud_child_id"

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
            .applyPin(pin)
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

    /** Verificador PBKDF2 del PIN, apto para respaldo; no contiene el PIN. */
    fun pinVerifier(ctx: Context): String? = prefs(ctx).getString(KEY_PIN_VERIFIER, null)

    fun pinSalt(ctx: Context): String? = prefs(ctx).getString(KEY_PIN_SALT, null)

    /**
     * Restaura el perfil desde Firestore. También acepta el hash heredado una
     * sola vez y lo migra a PBKDF2 cuando el padre escriba su PIN correctamente.
     */
    fun restoreFromCloud(
        ctx: Context,
        name: String,
        gradeName: String?,
        pinVerifier: String?,
        pinSalt: String?,
        legacyPinHash: String?,
        blockSettings: Boolean,
        emergencyCalls: Boolean
    ) {
        prefs(ctx).edit()
            .putBoolean(KEY_CONFIGURED, true)
            .putString(KEY_CHILD_NAME, name.trim().ifBlank { "Estudiante" })
            .putString(KEY_GRADE, GradeLevel.fromName(gradeName).name)
            .apply {
                if (!pinVerifier.isNullOrBlank() && !pinSalt.isNullOrBlank()) {
                    putString(KEY_PIN_VERIFIER, pinVerifier)
                    putString(KEY_PIN_SALT, pinSalt)
                    remove(KEY_LEGACY_PIN_HASH)
                } else if (!legacyPinHash.isNullOrBlank()) {
                    putString(KEY_LEGACY_PIN_HASH, legacyPinHash)
                }
            }
            .putBoolean(KEY_BLOCK_SETTINGS, blockSettings)
            .putBoolean(KEY_EMERGENCY_CALLS, emergencyCalls)
            .apply()
    }

    fun verifyPin(ctx: Context, pin: String): Boolean {
        val p = prefs(ctx)
        val verifier = p.getString(KEY_PIN_VERIFIER, null)
        val salt = p.getString(KEY_PIN_SALT, null)
        if (!verifier.isNullOrBlank() && !salt.isNullOrBlank()) {
            return MessageDigest.isEqual(verifier.toByteArray(), derivePin(pin, salt).toByteArray())
        }
        // Migración automática de instalaciones antiguas tras verificar el PIN.
        val legacy = p.getString(KEY_LEGACY_PIN_HASH, null) ?: return false
        val ok = MessageDigest.isEqual(legacy.toByteArray(), sha256(pin).toByteArray())
        if (ok) p.edit().applyPin(pin).apply()
        return ok
    }

    fun hasPin(ctx: Context): Boolean =
        !pinVerifier(ctx).isNullOrBlank() || prefs(ctx).getString(KEY_LEGACY_PIN_HASH, null) != null

    /** ID estable creado por el padre en el panel; no depende de la tablet. */
    fun cloudChildId(ctx: Context): String? =
        prefs(ctx).getString(KEY_CLOUD_CHILD_ID, null)?.takeIf { it.isNotBlank() }

    fun setCloudChildId(ctx: Context, childId: String) {
        prefs(ctx).edit().putString(KEY_CLOUD_CHILD_ID, childId).apply()
    }

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun android.content.SharedPreferences.Editor.applyPin(pin: String): android.content.SharedPreferences.Editor {
        val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val salt = Base64.getEncoder().encodeToString(saltBytes)
        return putString(KEY_PIN_SALT, salt)
            .putString(KEY_PIN_VERIFIER, derivePin(pin, salt))
            .remove(KEY_LEGACY_PIN_HASH)
    }

    private fun derivePin(pin: String, saltBase64: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), Base64.getDecoder().decode(saltBase64), 210_000, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded.let { Base64.getEncoder().encodeToString(it) }
        } finally {
            spec.clearPassword()
        }
    }
}
