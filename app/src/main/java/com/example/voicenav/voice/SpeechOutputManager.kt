package com.example.voicenav.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Manages text-to-speech announcements for maneuvers and voice feedback.
 */
class SpeechOutputManager(context: Context) {
    private val tag = "SpeechOutputManager"
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to English (India or US)
                    val fallback = tts?.setLanguage(Locale("en", "IN"))
                    if (fallback == TextToSpeech.LANG_MISSING_DATA || fallback == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.US)
                    }
                }
                isInitialized = true
                Log.d(tag, "TTS successfully initialized")
            } else {
                Log.w(tag, "TTS initialization failed with code: $status")
            }
        }
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isInitialized || tts == null) {
            Log.w(tag, "TTS not ready yet: $text")
            onComplete?.invoke()
            return
        }

        val utteranceId = "VoiceNav_${System.currentTimeMillis()}"
        if (onComplete != null) {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) {
                    if (id == utteranceId) {
                        onComplete()
                    }
                }
                override fun onError(id: String?) {
                    if (id == utteranceId) {
                        onComplete()
                    }
                }
            })
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(tag, "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e(tag, "Error shutting down TTS", e)
        }
    }
}
