package com.controlparental.kioscosuave

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fase A de la nube: sube el progreso de la tablet a Firestore.
 *
 * Modelo de datos (una tablet = un niño):
 *   children/{androidId}
 *       name, grade, lastSeen, updatedAt
 *   children/{androidId}/days/{yyyy-MM-dd}
 *       math:    { correct, attempts }
 *       english: { correct, attempts }
 *       reading: { score, attempts }
 *       unlockedAt, evasions: { count, lastPackage, lastAt }
 *
 * Todo es best-effort y fire-and-forget: si no hay internet, el SDK de
 * Firestore guarda en disco y sincroniza solo cuando vuelva la conexión.
 * El kiosco NUNCA depende de la nube para funcionar (sigue 100% offline).
 */
object ProgressSync {

    private const val TAG = "TriviumSync"
    private val db get() = FirebaseFirestore.getInstance()
    private var lastEvasionReportAt = 0L

    @SuppressLint("HardwareIds")
    fun childId(ctx: Context): String =
        Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun childDoc(ctx: Context) = db.collection("children").document(childId(ctx))
    private fun dayDoc(ctx: Context) = childDoc(ctx).collection("days").document(today())

    /** Ejecuta [block] con sesión anónima de Firebase garantizada. */
    private fun withAuth(block: () -> Unit) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            block()
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener { block() }
            .addOnFailureListener { Log.w(TAG, "Auth anónima falló (¿proveedor deshabilitado?): ${it.message}") }
    }

    /** Registra/actualiza el perfil del niño y marca actividad (llamar al abrir la app). */
    fun registerChild(ctx: Context) {
        if (!ProfileStore.isConfigured(ctx)) return
        val profile = ProfileStore.getProfile(ctx)
        withAuth {
            childDoc(ctx).set(
                mapOf(
                    "name" to profile.name,
                    "grade" to profile.grade.name,
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).addOnFailureListener { Log.w(TAG, "registerChild: ${it.message}") }
        }
    }

    /** Resultado de una etapa de opción múltiple ("math" o "english"). */
    fun reportStage(ctx: Context, stage: String, correct: Int, attempts: Int) {
        withAuth {
            dayDoc(ctx).set(
                mapOf(
                    stage to mapOf("correct" to correct, "attempts" to attempts),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).addOnFailureListener { Log.w(TAG, "reportStage($stage): ${it.message}") }
        }
    }

    /** Resultado de la comprensión lectora (score 0-100, nº de envíos). */
    fun reportReading(ctx: Context, score: Int, attempts: Int) {
        withAuth {
            dayDoc(ctx).set(
                mapOf(
                    "reading" to mapOf("score" to score, "attempts" to attempts),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).addOnFailureListener { Log.w(TAG, "reportReading: ${it.message}") }
        }
    }

    /** El niño completó todo y la tablet quedó libre. */
    fun reportUnlocked(ctx: Context) {
        withAuth {
            dayDoc(ctx).set(
                mapOf("unlockedAt" to FieldValue.serverTimestamp()),
                SetOptions.merge()
            ).addOnFailureListener { Log.w(TAG, "reportUnlocked: ${it.message}") }
        }
    }

    /**
     * Intento de salir del kiosco interceptado por el Watchdog.
     * Con acelerador de 10s para no inundar Firestore con la ráfaga de rebotes.
     */
    fun reportEvasion(ctx: Context, packageName: String) {
        val now = System.currentTimeMillis()
        if (now - lastEvasionReportAt < 10_000) return
        lastEvasionReportAt = now
        withAuth {
            dayDoc(ctx).set(
                mapOf(
                    "evasions" to mapOf(
                        "count" to FieldValue.increment(1),
                        "lastPackage" to packageName,
                        "lastAt" to FieldValue.serverTimestamp()
                    )
                ),
                SetOptions.merge()
            ).addOnFailureListener { Log.w(TAG, "reportEvasion: ${it.message}") }
        }
    }
}
