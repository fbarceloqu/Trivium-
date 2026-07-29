package com.controlparental.kioscosuave

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Voz de la app (Android TextToSpeech, offline y gratuito).
 * Lee instrucciones y textos en español, y palabras de inglés con voz en inglés.
 * Pensado para Preescolar/1º: el niño que aún no lee bien escucha la consigna.
 */
object TtsManager {

    private const val TAG = "TriviumTts"
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingText: String? = null
    private var pendingEnglish = false

    fun speak(context: Context, text: String, english: Boolean = false) {
        val clean = text.trim()
        if (clean.isEmpty()) return

        val engine = tts
        if (engine == null) {
            // Primera vez: inicializa el motor y pronuncia al estar listo.
            pendingText = clean
            pendingEnglish = english
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ready = true
                    pendingText?.let { doSpeak(it, pendingEnglish) }
                    pendingText = null
                } else {
                    Log.w(TAG, "TTS no disponible (status=$status)")
                }
            }
            return
        }
        if (ready) doSpeak(clean, english)
    }

    private fun doSpeak(text: String, english: Boolean) {
        val engine = tts ?: return
        val locale = if (english) Locale.US else Locale("es", "MX")
        val result = engine.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Si no hay voz para ese idioma, intenta con la predeterminada.
            engine.language = Locale.getDefault()
        }
        engine.setSpeechRate(0.9f) // un poco más lento, para niños
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "trivium-tts")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
