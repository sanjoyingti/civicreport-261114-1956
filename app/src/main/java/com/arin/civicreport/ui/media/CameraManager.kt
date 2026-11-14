package com.arin.civicreport.ui.media

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var preview: Preview? = null
    private var videoCapture: androidx.camera.video.VideoCapture? = null

    fun initializeCamera(lifecycleOwner: LifecycleOwner, onReady: () -> Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                cameraProvider = future.get()
                val selectorBuilder = ResolutionSelector.Builder()
                val resolutionStrategy = ResolutionStrategy(
                    ResolutionStrategy.FROM_BACKWARD_COMPATIBLE,
                    ResolutionStrategy.FROM_CAMERA_CAPABILITIES,
                    null
                )
                selectorBuilder.setResolutionStrategy(resolutionStrategy)
                val selector = selectorBuilder.build()
                preview = Preview.Builder().setResolutionSelector(selector).build().also {
                    it.setSurfaceProvider(cameraProvider?.getSurfaceProvider(PreviewView(context))!!)
                }
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, preview!!)
                onReady()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun takePhoto(
        previewView: PreviewView,
        onImage: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        val imageCapture = ImageCapture.Builder().build()
        val photo = object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                val file = saveImageFile()
                try {
                    image.image?.let { bitmap ->
                        android.graphics.Matrix().apply {
                            val rotation = when (image.imageInfo.rotationDegrees) {
                                90 -> rotate(90f)
                                180 -> rotate(180f)
                                270 -> rotate(270f)
                                else -> this
                            }
                        }
                        val rotated = android.graphics.Bitmap.createBitmap(
                            bitmap, 0, 0, bitmap.width, bitmap.height, this, true
                        )
                        file.outputStream().use { out ->
                            rotated.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        rotated.recycle()
                        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            MediaStoreHelper.saveToMediaStore(context, file, MediaStoreHelper.MediaKind.Photo)
                        } else {
                            Uri.fromFile(file)
                        }
                        onImage(uri)
                    } ?: onError("Image bitmap was null")
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception.message ?: "Capture failed")
            }
        }
        imageCapture.takePicture(cameraExecutor, photo)
    }

    fun startVideo(
        previewView: PreviewView,
        onVideoReady: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        val name = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date()) + "_record.mp4"
        val contentUri = MediaStoreHelper.getContentUriForVideo(name)
        videoCapture = androidx.camera.video.VideoRecordEvent.Factory()
        val recordArgs = androidx.camera.video.Record
        val output = androidx.camera.video.MediaStoreOutputOptions.Builder(context.contentResolver, contentUri).build()
        videoCapture = androidx.camera.video.VideoCapture.withOutput(output)
        videoCapture?.start(ContextCompat.getMainExecutor(context), object : androidx.camera.video.VideoCapture.OnVideoStartedCallback {
            override fun onVideoStarted() {
                onVideoReady(contentUri)
            }
        })
    }

    fun stopVideo(onVideoReady: (Uri) -> Unit, onError: (String) -> Unit) {
        videoCapture?.let {
            it.stop()
            videoCapture = null
        }
    }

    fun releaseCamera() {
        preview?.clearSurfaceProvider()
        videoCapture?.stop()
        cameraProvider?.unbindAll()
        cameraProvider = null
    }

    private fun saveImageFile(): File {
        val dir = File(context.externalFilesDir("media") ?: context.filesDir, "media").also { it.mkdirs() }
        val name = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date()) + "_capture.jpg"
        return File(dir, name)
    }
}

private const val TAG = "CameraManager"
