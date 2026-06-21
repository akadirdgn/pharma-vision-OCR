package com.kadir.bitirme.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.kadir.bitirme.R
import com.kadir.bitirme.databinding.ActivityMainBinding
import com.kadir.bitirme.ui.camera.CameraActivity
import com.kadir.bitirme.ui.history.HistoryActivity
import com.kadir.bitirme.data.repository.DoseTrackerRepository
import com.kadir.bitirme.utils.tts.TextToSpeechManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var doseTrackerRepository: DoseTrackerRepository
    private lateinit var doseTrackerAdapter: DoseTrackerAdapter
    private lateinit var gestureDetector: android.view.GestureDetector

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize TTS & Repo
        ttsManager = TextToSpeechManager(this)
        doseTrackerRepository = DoseTrackerRepository(this)

        // Welcome message
        ttsManager.speak("İlaç Asistanı. Kamerayı açmak için ekrana iki kez dokunun.")

        // Setup accessibility click listeners
        setupAccessibility()
        setupDoseTracker()
        
        // Ekrana çift dokunarak kamerayı açma tetikleyicisi
        gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: android.view.MotionEvent): Boolean {
                if (checkCameraPermission()) {
                    startCameraActivity()
                } else {
                    requestCameraPermission()
                }
                return true
            }
        })

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

        // History button
        binding.btnHistory.setOnClickListener {
            ttsManager.speak("Tarama geçmişi açılıyor.")
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onResume() {
        super.onResume()
        updateDoses()
    }

    private fun setupDoseTracker() {
        doseTrackerAdapter = DoseTrackerAdapter(emptyList()) { dose, isTaken ->
            doseTrackerRepository.addOrUpdateDose(dose.medicineName, isTaken)
            if (isTaken) {
                ttsManager.speak("${dose.medicineName} alındı olarak işaretlendi.")
            } else {
                ttsManager.speak("${dose.medicineName} alınmadı olarak işaretlendi.")
            }
            updateDoses()
        }
        binding.rvDoseTracker.layoutManager = LinearLayoutManager(this)
        binding.rvDoseTracker.adapter = doseTrackerAdapter
    }

    private fun updateDoses() {
        val doses = doseTrackerRepository.getTodayDoses()
        if (doses.isEmpty()) {
            binding.rvDoseTracker.visibility = View.GONE
            binding.tvNoDoses.visibility = View.VISIBLE
        } else {
            binding.rvDoseTracker.visibility = View.VISIBLE
            binding.tvNoDoses.visibility = View.GONE
            doseTrackerAdapter.updateData(doses)
        }
    }

    private fun setupAccessibility() {
        // Title - tıklanınca başlığı oku
        binding.tvTitle.setOnClickListener {
            ttsManager.speak(getString(R.string.welcome_title))
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

    override fun onPause() {
        super.onPause()
        ttsManager.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
        doseTrackerRepository.close()
    }
}
