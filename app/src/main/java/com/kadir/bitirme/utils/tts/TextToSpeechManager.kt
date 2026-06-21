package com.kadir.bitirme.utils.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TextToSpeechManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("tr", "TR"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Turkish language is not supported or missing data.")
            } else {
                isInitialized = true
                pendingText?.let {
                    speak(it)
                    pendingText = null
                }
            }
        } else {
            Log.e(TAG, "TTS Initialization failed!")
        }
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (isInitialized) {
            tts?.speak(text, queueMode, null, null)
        } else {
            pendingText = text
            Log.e(TAG, "TTS not initialized yet. Queued text for later: $text")
        }
    }

    /**
     * TTS'i duraklat (lifecycle için)
     */
    fun pause() {
        if (isInitialized && tts?.isSpeaking == true) {
            tts?.stop()
        }
    }

    /**
     * TTS'in konuşup konuşmadığını kontrol et
     */
    fun isSpeaking(): Boolean {
        return tts?.isSpeaking ?: false
    }

    fun shutdown() {
        pause()
        tts?.shutdown()
        isInitialized = false
    }

    companion object {
        private const val TAG = "TTSManager"
    }
}
