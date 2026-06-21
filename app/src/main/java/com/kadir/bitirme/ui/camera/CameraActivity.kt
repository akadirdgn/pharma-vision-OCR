package com.kadir.bitirme.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kadir.bitirme.data.model.ProcessedResult
import com.kadir.bitirme.data.repository.MedicineRepository
import com.kadir.bitirme.databinding.ActivityCameraBinding
import com.kadir.bitirme.domain.processor.MedicineTextProcessor
import com.kadir.bitirme.domain.usecase.ProcessOcrTextUseCase
import com.kadir.bitirme.utils.tts.TextToSpeechManager
import com.kadir.bitirme.data.model.ScanHistoryEntity
import com.kadir.bitirme.data.repository.ScanHistoryRepository
import com.kadir.bitirme.utils.gesture.GestureManager
import com.kadir.bitirme.utils.gesture.ShakeDetector
import com.kadir.bitirme.utils.reminder.ReminderManager
import android.content.DialogInterface
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityCameraBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private lateinit var ttsManager: TextToSpeechManager
    
    // Domain layer dependencies
    private lateinit var medicineRepository: MedicineRepository
    private lateinit var scanHistoryRepository: ScanHistoryRepository
    private lateinit var textProcessor: MedicineTextProcessor
    private lateinit var processOcrUseCase: ProcessOcrTextUseCase
    
    // Feature managers
    private lateinit var reminderManager: ReminderManager
    private lateinit var gestureManager: GestureManager
    private lateinit var shakeDetector: ShakeDetector
    
    // Processing state
    private var isProcessing = false
    private var lastSpeechOutput = ""
    private var lastExtractedName = ""
    private var lastScannedMedicineName = "" // Hatırlatıcı için son taranan ilaç
    
    // Camera state
    private var camera: Camera? = null
    private var isFlashEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Initialize dependencies
        ttsManager = TextToSpeechManager(this)
        medicineRepository = MedicineRepository(this)
        scanHistoryRepository = ScanHistoryRepository(this)
        textProcessor = MedicineTextProcessor()
        processOcrUseCase = ProcessOcrTextUseCase(textProcessor, medicineRepository, scanHistoryRepository)
        reminderManager = ReminderManager(this)
        
        // Initialize gestures
        setupGestures()
        
        // Give TTS a moment to init, then announce
        viewBinding.root.postDelayed({
             ttsManager.speak("Kamera açıldı. İlacı kameraya yöneltip ekrana dokunun ve ilacı tanıyın.")
        }, 1000)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        viewBinding.viewFinder.setOnClickListener {
            captureAndReadText()
        }
        
        // FAB button listener
        viewBinding.btnCapture.setOnClickListener {
            captureAndReadText()
        }
        
        // Flashlight button listener
        viewBinding.btnFlashlight.setOnClickListener {
            toggleFlashlight()
        }
        
        // Reminder button listener
        viewBinding.btnReminder.setOnClickListener {
            if (lastScannedMedicineName.isNotEmpty()) {
                showReminderDialog(lastScannedMedicineName)
            }
        }
    }
    
    private fun toggleFlashlight() {
        camera?.cameraControl?.enableTorch(!isFlashEnabled)
        isFlashEnabled = !isFlashEnabled
        
        if (isFlashEnabled) {
            ttsManager.speak("Flaş açıldı.")
        } else {
            ttsManager.speak("Flaş kapatıldı.")
        }
    }

    private fun setupGestures() {
        gestureManager = GestureManager(this, 
            onDoubleTap = {
                if (lastSpeechOutput.isNotEmpty()) {
                    ttsManager.speak("Tekrar okunuyor. $lastSpeechOutput")
                } else {
                    ttsManager.speak("Henüz okunacak bir metin yok.")
                }
            },
            onLongPress = {
                 captureAndReadText()
            }
        )
        
        shakeDetector = ShakeDetector(this) {
            ttsManager.speak("Hareket algılandı, kamera yeniden taranıyor.")
            captureAndReadText()
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        gestureManager.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureAndReadText() {
        // İşlem devam ediyorsa yeni istek alma
        if (isProcessing) {
            Log.d(TAG, "Already processing, ignoring request")
            return
        }
        
        val imageCapture = imageCapture ?: return
        
        isProcessing = true
        viewBinding.btnCapture.isEnabled = false
        
        // Yeni tarama başladığında sonucu gizle, rehber çerçeveyi göster
        viewBinding.svScanResult.visibility = android.view.View.GONE
        viewBinding.scanFrameOverlay.visibility = android.view.View.VISIBLE
        
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    ttsManager.speak("Hata oluştu.")
                    isProcessing = false
                    viewBinding.btnCapture.isEnabled = true
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    processImage(image)
                }
            }
        )
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val rawOcrText = visionText.text
                    
                    try {
                        if (rawOcrText.isNotEmpty()) {
                            Log.d(TAG, "Raw OCR Text: $rawOcrText")
                            
                            // Use the new processing pipeline
                            val result = processOcrUseCase.execute(rawOcrText)
                            
                            when (result) {
                                is ProcessedResult.Success -> {
                                    Log.d(TAG, "Medicine found: ${result.medicine.name}")
                                    lastSpeechOutput = result.speech
                                    lastExtractedName = result.medicine.name
                                    lastScannedMedicineName = result.medicine.name
                                    ttsManager.speak(result.speech + ". Hatırlatıcı kurmak için alttaki butona basabilirsiniz. Tekrar dinlemek için ekrana iki kez dokunun.")
                                    
                                    // İlaç adını ve detaylarını doğrudan ekrana yazdır
                                    val formattedText = "💊 ${result.medicine.name}\n\n${result.speech}"
                                    viewBinding.tvScanResult.text = formattedText
                                    viewBinding.svScanResult.visibility = android.view.View.VISIBLE
                                    viewBinding.scanFrameOverlay.visibility = android.view.View.GONE
                                    
                                    // Hatırlatıcı butonunu görünür yap
                                    viewBinding.btnReminder.visibility = android.view.View.VISIBLE
                                    try {
                                        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                                        } else {
                                            @Suppress("DEPRECATION")
                                            vibrator?.vibrate(200)
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Vibration failed", e)
                                    }
                                    
                                    // Geçmişe kaydet
                                    try {
                                        scanHistoryRepository.insertScan(ScanHistoryEntity(
                                            medicineName = result.medicine.name,
                                            isSuccess = true,
                                            rawText = rawOcrText,
                                            speechOutput = result.speech
                                        ))
                                    } catch (e: Exception) {
                                        Log.e(TAG, "History insert failed", e)
                                    }
                                    
                                    // Eski auto-popup kaldırıldı, buton kullanılıyor
                                }
                                is ProcessedResult.NotFound -> {
                                    Log.d(TAG, "Medicine not found in database")
                                    lastSpeechOutput = result.speech
                                    lastExtractedName = result.extractedName
                                    ttsManager.speak(result.speech + ". Tekrar dinlemek için ekrana iki kez dokunun.")
                                    
                                    // Ekrana sonucu yazdır
                                    viewBinding.tvScanResult.text = "❌ Bulunamadı\n\n${result.speech}"
                                    viewBinding.svScanResult.visibility = android.view.View.VISIBLE
                                    viewBinding.scanFrameOverlay.visibility = android.view.View.GONE
                                    
                                    try {
                                        scanHistoryRepository.insertScan(ScanHistoryEntity(
                                            medicineName = result.extractedName.ifEmpty { "Bilinmeyen İlaç" },
                                            isSuccess = false,
                                            rawText = rawOcrText,
                                            speechOutput = result.speech
                                        ))
                                    } catch (e: Exception) {
                                        Log.e(TAG, "History insert failed", e)
                                    }
                                }
                                is ProcessedResult.Error -> {
                                    Log.e(TAG, "Error processing: ${result.message}")
                                    lastSpeechOutput = result.speech
                                    ttsManager.speak(result.speech + ". Tekrar dinlemek için ekrana iki kez dokunun.")
                                    
                                    // Ekrana hatayı yazdır
                                    viewBinding.tvScanResult.text = "⚠️ Hata\n\n${result.speech}"
                                    viewBinding.svScanResult.visibility = android.view.View.VISIBLE
                                    viewBinding.scanFrameOverlay.visibility = android.view.View.GONE
                                }
                            }
                        } else {
                            ttsManager.speak("Metin bulunamadı.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in success listener", e)
                        ttsManager.speak("İşlem sırasında bir hata oluştu.")
                    }
                    // İşlem tamamlandı, butonu aktif et
                    isProcessing = false
                    viewBinding.btnCapture.isEnabled = true
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "OCR failed", exception)
                    ttsManager.speak("Okuma hatası.")
                    // İşlem tamamlandı, butonu aktif et
                    isProcessing = false
                    viewBinding.btnCapture.isEnabled = true
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun showReminderDialog(medicineName: String) {
        val labels = arrayOf(
            "Sabah 07:00",
            "Sabah 08:00",
            "Öğle  12:00",
            "Öğle  13:00",
            "Akşam 18:00",
            "Akşam 19:00",
            "Gece  21:00",
            "Gece  22:00"
        )
        val hours = intArrayOf(7, 8, 12, 13, 18, 19, 21, 22)

        MaterialAlertDialogBuilder(this)
            .setTitle("Doz Hatırlatıcı - $medicineName")
            .setItems(labels) { _, which ->
                val selectedHour = hours[which]
                val selectedLabel = labels[which]

                val target = java.util.Calendar.getInstance()
                target.set(java.util.Calendar.HOUR_OF_DAY, selectedHour)
                target.set(java.util.Calendar.MINUTE, 0)
                target.set(java.util.Calendar.SECOND, 0)
                target.set(java.util.Calendar.MILLISECOND, 0)
                if (target.timeInMillis <= System.currentTimeMillis()) {
                    target.add(java.util.Calendar.DAY_OF_YEAR, 1)
                }

                val delayMinutes = ((target.timeInMillis - System.currentTimeMillis()) / 60000L)
                    .toInt().coerceAtLeast(1)

                reminderManager.scheduleReminder(medicineName, delayMinutes)
                
                // Hatırlatıcı kurulduğunda aynı zamanda doz takibine de ekle
                val doseTrackerRepo = com.kadir.bitirme.data.repository.DoseTrackerRepository(this)
                doseTrackerRepo.addOrUpdateDose(medicineName, false)
                doseTrackerRepo.close()
                
                ttsManager.speak("$selectedLabel saatinde $medicineName icin hatirlatici kuruldu. İlaç doz takibine eklendi.")
                Toast.makeText(this, "$selectedLabel - $medicineName", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Iptal") { _, _ ->
                ttsManager.speak("Hatirlatici kurulmadi.")
            }
            .show()

    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onResume() {
        super.onResume()
        shakeDetector.start()
    }

    override fun onPause() {
        super.onPause()
        ttsManager.pause()
        shakeDetector.stop()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        ttsManager.shutdown()
        medicineRepository.close()
        scanHistoryRepository.close()
    }
    
     override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Kamera izni gerekli.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "CameraActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }
}
