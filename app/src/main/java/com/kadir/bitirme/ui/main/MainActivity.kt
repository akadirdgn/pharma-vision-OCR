package com.kadir.bitirme.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kadir.bitirme.R
import com.kadir.bitirme.databinding.ActivityMainBinding
import com.kadir.bitirme.ui.camera.CameraActivity
import com.kadir.bitirme.utils.tts.TextToSpeechManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var ttsManager: TextToSpeechManager

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize TTS
        ttsManager = TextToSpeechManager(this)

        // Welcome message
        ttsManager.speak(getString(R.string.welcome_title) + ". " + getString(R.string.welcome_subtitle))

        // Setup click-to-speak for accessibility
        setupAccessibility()

        // Start button - İzin kontrolü ile kamera aç
        binding.btnStart.setOnClickListener {
            if (checkCameraPermission()) {
                startCameraActivity()
            } else {
                requestCameraPermission()
            }
        }

        // Help button - Kullanım bilgisi göster
        binding.btnHelp.setOnClickListener {
            showHowItWorksDialog()
        }
    }

    private fun setupAccessibility() {
        // Logo - tıklanınca uygulama adını oku
        binding.ivLogo.setOnClickListener {
            ttsManager.speak(getString(R.string.app_name))
        }

        // Title - tıklanınca başlığı oku
        binding.tvTitle.setOnClickListener {
            ttsManager.speak(getString(R.string.welcome_title))
        }

        // Subtitle - tıklanınca alt başlığı oku
        binding.tvSubtitle.setOnClickListener {
            ttsManager.speak(getString(R.string.welcome_subtitle))
        }

        // Description card - tıklanınca açıklamayı oku
        binding.cardDescription.setOnClickListener {
            ttsManager.speak(getString(R.string.welcome_description))
        }

        // Features layout - tıklanınca özellikleri oku
        binding.featuresLayout.setOnClickListener {
            val features = """
                ${getString(R.string.feature_ocr)}. ${getString(R.string.feature_ocr_desc)}.
                ${getString(R.string.feature_database)}. ${getString(R.string.feature_database_desc)}.
                ${getString(R.string.feature_voice)}. ${getString(R.string.feature_voice_desc)}.  
                ${getString(R.string.feature_privacy)}. ${getString(R.string.feature_privacy_desc)}.
            """.trimIndent()
            ttsManager.speak(features)
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            // Kullanıcıya neden izin gerektiğini açıkla
            AlertDialog.Builder(this)
                .setTitle("Kamera İzni")
                .setMessage(getString(R.string.camera_permission_rationale))
                .setPositiveButton(getString(R.string.grant_permission)) { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.CAMERA),
                        CAMERA_PERMISSION_CODE
                    )
                }
                .setNegativeButton(getString(R.string.close), null)
                .show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ttsManager.speak("Kamera izni verildi. Kamera açılıyor.")
                startCameraActivity()
            } else {
                ttsManager.speak("Kamera izni reddedildi. Uygulama kamera olmadan çalışamaz.")
            }
        }
    }

    private fun startCameraActivity() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }

    private fun showHowItWorksDialog() {
        val message = """
            ${getString(R.string.info_step1)}
            
            ${getString(R.string.info_step2)}
            
            ${getString(R.string.info_step3)}
            
            ${getString(R.string.info_step4)}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.info_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.close)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()

        ttsManager.speak(message)
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }
}
