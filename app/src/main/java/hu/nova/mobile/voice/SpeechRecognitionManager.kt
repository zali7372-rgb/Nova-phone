package hu.nova.mobile.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import hu.nova.mobile.domain.model.AppLanguage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed class SpeechRecognitionEvent {
    data class PartialResult(val text: String) : SpeechRecognitionEvent()
    data class FinalResult(val text: String) : SpeechRecognitionEvent()
    data class Error(val code: Int, val message: String) : SpeechRecognitionEvent()
    object ReadyForSpeech : SpeechRecognitionEvent()
    object EndOfSpeech : SpeechRecognitionEvent()
}

/**
 * Wraps Android's built-in [SpeechRecognizer] (android.speech). This is the standard,
 * legitimate on-device/cloud-assisted speech-to-text API Android provides; it supports
 * both Hungarian ("hu-HU") and English ("en-US") depending on the user's language
 * setting and the language packs installed on the device.
 */
class SpeechRecognitionManager(private val context: Context) {

    fun isRecognitionAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun listen(language: AppLanguage, preferOffline: Boolean = false): Flow<SpeechRecognitionEvent> = callbackFlow {
        if (!isRecognitionAvailable()) {
            trySend(SpeechRecognitionEvent.Error(-1, "Speech recognition is not available on this device."))
            close()
            return@callbackFlow
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val localeTag = if (language == AppLanguage.HUNGARIAN) "hu-HU" else "en-US"

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(SpeechRecognitionEvent.ReadyForSpeech)
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                trySend(SpeechRecognitionEvent.EndOfSpeech)
            }

            override fun onError(error: Int) {
                trySend(SpeechRecognitionEvent.Error(error, describeError(error)))
                close()
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                trySend(SpeechRecognitionEvent.FinalResult(text))
                close()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) trySend(SpeechRecognitionEvent.PartialResult(text))
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        recognizer.setRecognitionListener(listener)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
        }

        recognizer.startListening(intent)

        awaitClose {
            recognizer.stopListening()
            recognizer.destroy()
        }
    }

    private fun describeError(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
        SpeechRecognizer.ERROR_CLIENT -> "Client-side error."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing RECORD_AUDIO permission."
        SpeechRecognizer.ERROR_NETWORK -> "Network error during recognition."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout during recognition."
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy."
        SpeechRecognizer.ERROR_SERVER -> "Server error."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input."
        else -> "Unknown recognition error ($code)."
    }
}
