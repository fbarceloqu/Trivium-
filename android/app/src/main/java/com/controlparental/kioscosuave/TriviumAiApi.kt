package com.controlparental.kioscosuave

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.controlparental.kioscosuave.curriculum.NarrativeRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions

/**
 * Cliente de las callable Functions de Trivium. No contiene claves Gemini.
 * Un error de red, autorización o servidor siempre devuelve null para que el
 * llamador conserve el ejercicio/evaluación local y el niño pueda continuar.
 */
object TriviumAiApi {
    private const val TAG = "TriviumAiApi"
    private val main = Handler(Looper.getMainLooper())
    private val functions by lazy { FirebaseFunctions.getInstance("us-central1") }

    data class SummaryEval(
        val approved: Boolean,
        val score: Int,
        val feedback: String,
        val suggestions: String
    )

    private fun withAuth(onReady: () -> Unit, onFailure: () -> Unit) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            onReady()
        } else {
            auth.signInAnonymously()
                .addOnSuccessListener { onReady() }
                .addOnFailureListener { error ->
                    Log.w(TAG, "No se pudo autenticar para IA: ${error.message}")
                    onFailure()
                }
        }
    }

    fun evaluateSummary(
        ctx: Context,
        readingText: String,
        userSummary: String,
        onResult: (SummaryEval?) -> Unit
    ) = withAuth(
        onReady = {
            functions.getHttpsCallable("evaluateSummary")
                .call(mapOf(
                    "childId" to ProgressSync.childId(ctx),
                    "readingText" to readingText,
                    "userSummary" to userSummary
                ))
                .addOnSuccessListener { result ->
                    val data = result.data as? Map<*, *> ?: run {
                        main.post { onResult(null) }; return@addOnSuccessListener
                    }
                    val feedback = data["feedback"] as? String
                    if (feedback.isNullOrBlank()) {
                        main.post { onResult(null) }
                    } else {
                        main.post {
                            onResult(SummaryEval(
                                approved = data["approved"] as? Boolean ?: false,
                                score = (data["score"] as? Number)?.toInt()?.coerceIn(0, 100) ?: 0,
                                feedback = feedback,
                                suggestions = data["suggestions"] as? String ?: ""
                            ))
                        }
                    }
                }
                .addOnFailureListener { error ->
                    Log.w(TAG, "evaluateSummary remoto falló: ${error.message}")
                    main.post { onResult(null) }
                }
        },
        onFailure = { main.post { onResult(null) } }
    )

    fun generateNarratives(
        ctx: Context,
        requests: List<NarrativeRequest>,
        onResult: (String?) -> Unit
    ) = withAuth(
        onReady = {
            val payload = requests.take(AiNarrator.BATCH).map {
                mapOf(
                    "skillId" to it.skillId,
                    "original" to it.original,
                    "numbers" to it.requiredNumbers
                )
            }
            functions.getHttpsCallable("generateNarratives")
                .call(mapOf("childId" to ProgressSync.childId(ctx), "requests" to payload))
                .addOnSuccessListener { result ->
                    val data = result.data as? Map<*, *>
                    val narratives = data?.get("narratives")
                    // NarrativeValidator ya opera con JSONArray; lo convertimos
                    // de forma segura sin introducir el texto en logs.
                    val raw = if (narratives is Collection<*>) {
                        org.json.JSONArray(narratives).toString()
                    } else null
                    main.post { onResult(raw) }
                }
                .addOnFailureListener { error ->
                    Log.w(TAG, "generateNarratives remoto falló: ${error.message}")
                    main.post { onResult(null) }
                }
        },
        onFailure = { main.post { onResult(null) } }
    )
}
