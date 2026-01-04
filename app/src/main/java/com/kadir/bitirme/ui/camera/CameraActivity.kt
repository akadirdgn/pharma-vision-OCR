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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityCameraBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private lateinit var ttsManager: TextToSpeechManager
    
    // Domain layer dependencies
    private lateinit var medicineRepository: MedicineRepository
    private lateinit var textProcessor: MedicineTextProcessor
    private lateinit var processOcrUseCase: ProcessOcrTextUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Initialize dependencies
        ttsManager = TextToSpeechManager(this)
        medicineRepository = MedicineRepository(this)
        textProcessor = MedicineTextProcessor()
        processOcrUseCase = ProcessOcrTextUseCase(textProcessor, medicineRepository)
        
        // Give TTS a moment to init, then announce
        viewBinding.root.postDelayed({
             ttsManager.speak("Kamera açıldı. Okumak için dokunun.")
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
        viewBinding.fabCapture.setOnClickListener {
            captureAndReadText()
        }
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
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureAndReadText() {
        val imageCapture = imageCapture ?: return
        ttsManager.speak("Fotoğraf çekiliyor...")
        
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    ttsManager.speak("Hata oluştu.")
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
                    
                    if (rawOcrText.isNotEmpty()) {
                        Log.d(TAG, "Raw OCR Text: $rawOcrText")
                        
                        // Use the new processing pipeline
                        val result = processOcrUseCase.execute(rawOcrText)
                        
                        when (result) {
                            is ProcessedResult.Success -> {
                                Log.d(TAG, "Medicine found: ${result.medicine.name}")
                                Log.d(TAG, "Processing time: ${result.processingTimeMs}ms")
                                ttsManager.speak(result.speech)
                            }
                            is ProcessedResult.NotFound -> {
                                Log.d(TAG, "Medicine not found in database")
                                Log.d(TAG, "Processing time: ${result.processingTimeMs}ms")
                                ttsManager.speak(result.speech)
                            }
                            is ProcessedResult.Error -> {
                                Log.e(TAG, "Error processing: ${result.message}")
                                ttsManager.speak(result.speech)
                            }
                        }
                    } else {
                        ttsManager.speak("Metin bulunamadı.")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "OCR failed", exception)
                    ttsManager.speak("Okuma hatası.")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        ttsManager.shutdown()
        medicineRepository.close()
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
        private val REQUIRED_PERMISSIONS = mutableListOf(Manifest.permission.CAMERA).toTypedArray()
    }
}
