package com.example.voicenav.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

interface VoiceInputListener {
    fun onReadyForSpeech()
    fun onBeginningOfSpeech()
    fun onRmsChanged(rmsdB: Float) {}
    fun onEndOfSpeech()
    fun onSpeechResult(text: String, isSimulated: Boolean)
    fun onSpeechError(errorMessage: String, isSimulated: Boolean)
}

/**
 * Manages native Android SpeechRecognizer and simulated voice input.
 * Explicitly indicates whether live hardware recognition or simulated input is active.
 */
class VoiceInputManager(
    private val context: Context,
    private val listener: VoiceInputListener
) {
    private val tag = "VoiceInputManager"
    private var speechRecognizer: SpeechRecognizer? = null

    val isNativeSpeechAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    init {
        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        if (isNativeSpeechAvailable) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to create SpeechRecognizer", e)
                speechRecognizer = null
            }
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listener.onReadyForSpeech()
            }

            override fun onBeginningOfSpeech() {
                listener.onBeginningOfSpeech()
            }

            override fun onRmsChanged(rmsdB: Float) {
                listener.onRmsChanged(rmsdB)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                listener.onEndOfSpeech()
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try again."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer is busy"
                    SpeechRecognizer.ERROR_SERVER -> "Speech server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                    else -> "Speech recognition error ($error)"
                }
                Log.w(tag, "Native speech error: $errorMsg")
                listener.onSpeechError(errorMsg, isSimulated = false)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognized = matches?.firstOrNull() ?: ""
                if (recognized.isNotBlank()) {
                    listener.onSpeechResult(recognized, isSimulated = false)
                } else {
                    listener.onSpeechError("No speech detected", isSimulated = false)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            initializeRecognizer()
        }

        if (speechRecognizer != null && isNativeSpeechAvailable) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening for navigation command…")
            }
            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(tag, "Error starting SpeechRecognizer", e)
                listener.onSpeechError("Speech recognizer unavailable", isSimulated = false)
            }
        } else {
            // Speech recognizer not available (e.g. standard cloud emulator without Google Speech Services)
            listener.onSpeechError("Native SpeechRecognizer not installed on this device/emulator. Using simulated mode.", isSimulated = true)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(tag, "Error stopping recognizer", e)
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(tag, "Error destroying recognizer", e)
        }
    }
}
