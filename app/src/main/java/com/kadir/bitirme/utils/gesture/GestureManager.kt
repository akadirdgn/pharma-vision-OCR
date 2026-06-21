package com.kadir.bitirme.utils.gesture

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent

/**
 * Erişilebilirlik jestlerini yöneten sınıf
 * - Çift dokunuş → son TTS çıktısını tekrar oku
 * - Uzun basış → son OCR sonucunu tekrar oku
 */
class GestureManager(
    context: Context,
    private val onDoubleTap: () -> Unit,
    private val onLongPress: () -> Unit
) {

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {

            override fun onDoubleTap(e: MotionEvent): Boolean {
                onDoubleTap()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                onLongPress()
            }

            override fun onDown(e: MotionEvent): Boolean = true
        }
    )

    /**
     * Touch event'i GestureDetector'a ilet
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }
}
