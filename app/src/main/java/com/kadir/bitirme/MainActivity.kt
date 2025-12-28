package com.kadir.bitirme

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kadir.bitirme.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var ttsManager: TextToSpeechManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        ttsManager = TextToSpeechManager(this)

        // Announce app name on launch
        viewBinding.root.postDelayed({
            ttsManager.speak("Bitirme projesi açıldı. Taramaya başlamak için butona basın.")
        }, 1000)

        viewBinding.btnStart.setOnClickListener {
             val intent = Intent(this, CameraActivity::class.java)
             startActivity(intent)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }
}
