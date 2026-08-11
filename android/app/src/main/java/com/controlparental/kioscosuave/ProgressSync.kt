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

    /**
     * Registra/actualiza el perfil del niño y marca actividad (llamar al abrir
     * la app). Incluye el RESPALDO del pinHash y ajustes: si se borran los
     * datos locales o se reinstala, tryRestoreProfile los recupera.
     */
    fun registerChild(ctx: Context) {
        if (!ProfileStore.isConfigured(ctx)) return
        val profile = ProfileStore.getProfile(ctx)
        withAuth {
            childDoc(ctx).set(
                mapOf(
                    "name" to profile.name,
                    "grade" to profile.grade.name,
                    "pinHash" to (ProfileStore.pinHash(ctx) ?: ""),
                    "blockSettings" to ProfileStore.blockSettings(ctx),
                    "emergencyCalls" to ProfileStore.emergencyCalls(ctx),
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).addOnFailureListener { Log.w(TAG, "registerChild: ${it.message}") }
        }
    }

    /**
     * Intenta restaurar el perfil (nombre, nivel, PIN, ajustes) y la sesión de
     * hoy desde Firestore. Llama a [onResult] con true si restauró algo.
     * Best-effort: si no hay internet o no hay respaldo, onResult(false).
     */
    fun tryRestoreProfile(ctx: Context, onResult: (Boolean) -> Unit) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val run = {
            childDoc(ctx).get()
                .addOnSuccessListener { snap ->
                    val pinHash = snap.getString("pinHash")
                    if (snap.exists() && !pinHash.isNullOrBlank()) {
                        ProfileStore.restoreFromCloud(
                            ctx,
                            name = snap.getString("name") ?: "Estudiante",
                            gradeName = snap.getString("grade"),
                            pinHash = pinHash,
                            blockSettings = snap.getBoolean("blockSettings") ?: true,
                            emergencyCalls = snap.getBoolean("emergencyCalls") ?: true
                        )
                        // Restaurar también la sesión del día (si hoy ya desbloqueó).
                        dayDoc(ctx).get()
                            .addOnSuccessListener { day ->
                                if (day.exists() && day.get("unlockedAt") != null) {
                                    SessionStateMachine.setUnlocked(ctx)
                                }
                                onResult(true)
                            }
                            .addOnFailureListener { onResult(true) }
                    } else {
                        onResult(false)
                    }
                }
                .addOnFailureListener {
                    Log.w(TAG, "tryRestoreProfile: ${it.message}")
                    onResult(false)
                }
        }
        if (auth.currentUser != null) run()
        else auth.signInAnonymously()
            .addOnSuccessListener { run() }
            .addOnFailureListener { onResult(false) }
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

    /**
     * Sube el estado de UNA habilidad a `children/{id}/skills/{skillId}`.
     *
     * Es lo que permitirá al panel de padres mostrar el mapa de dominio ("qué
     * domina, qué le cuesta") en vez del `7/10` de hoy, que no dice nada sobre
     * QUÉ necesita practicar.
     *
     * Best-effort como el resto: si no hay red, el SDK de Firestore lo guarda
     * en disco y lo sincroniza después. La memoria local nunca depende de esto.
     */
    fun reportSkill(ctx: Context, skillId: String, state: com.controlparental.kioscosuave.curriculum.SkillState?) {
        if (state == null) return
        withAuth {
            childDoc(ctx).collection("skills").document(skillId).set(
                mapOf(
                    "practices" to state.practices,
                    "correct" to state.correct,
                    "streak" to state.streak,
                    "lapses" to state.lapses,
                    "accuracy" to state.accuracy,
                    "mastery" to state.mastery.name,
                    "intervalDays" to state.intervalDays,
                    "lastPracticedAt" to state.lastPracticedAt,
                    "dueAt" to state.dueAt,
                    "recentErrors" to state.recentErrors,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).addOnFailureListener { Log.w(TAG, "reportSkill($skillId): ${it.message}") }
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
