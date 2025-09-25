package com.example.aishield

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(androidx.camera.core.ExperimentalGetImage::class)
class weapondetector : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var tvResult: TextView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var detector: ObjectDetector

    // List of weapon labels
    private val weaponList = listOf("knife", "gun", "pistol", "rifle", "weapon")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weapondetector)

        previewView = findViewById(R.id.previewView)
        tvResult = findViewById(R.id.tvResult)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Load EfficientDet Lite0 model from assets
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(5)
            .setScoreThreshold(0.4f)
            .build()

        detector = ObjectDetector.createFromFileAndOptions(
            this,
            "efficientdet-lite0.tflite", // Make sure this is in assets/
            options
        )

        // Request Camera permission
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else tvResult.text = "Camera permission denied"
        }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("WeaponDetector", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap() ?: run {
            imageProxy.close()
            return
        }

        val tensorImage = TensorImage.fromBitmap(bitmap)
        val results = detector.detect(tensorImage)

        runOnUiThread {
            if (results.isNotEmpty()) {
                val displayText = StringBuilder()
                for (obj in results) {
                    val category = obj.categories[0]
                    val label = category.label.lowercase()
                    val score = category.score
                    if (weaponList.any { w -> label.contains(w) }) {
                        displayText.append("⚠️ Weapon detected: ${category.label} (${String.format("%.2f", score)})\n")
                    } else {
                        displayText.append("Detected: ${category.label} (${String.format("%.2f", score)})\n")
                    }
                }
                tvResult.text = displayText.toString().trim()
            } else {
                tvResult.text = "No object detected"
            }
        }
        imageProxy.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

/**
 * Extension function to convert ImageProxy (YUV_420_888) to Bitmap
 */
private fun ImageProxy.toBitmap(): Bitmap? {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = android.graphics.YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, out)
    return BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
}
