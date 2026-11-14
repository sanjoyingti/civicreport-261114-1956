package com.arin.civicreport.ui.media

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(private val context: Context) {

    private var cameraProviderFuture: androidx.lifecycle.LifecycleProvider? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var currentPhotoCapture: ImageCapture? = null
    private var currentVideoCapture: VideoCapture<MediaStoreOutputOptions>? = null
    private var preview: Preview? = null

    fun initializeCamera(lifecycleOwner: LifecycleOwner, onReady: () -> Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            preview = Preview.Builder().build().also {
                it.setSurfaceProvider(lifecycleOwner.findViewTreeRoot()?.findViewWithTag<PreviewView>("preview")?.surfaceProvider)
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            cameraProviderFuture = lifecycleOwner as? androidx.lifecycle.LifecycleProvider
            onReady()
        }, ContextCompat.getMainExecutor(context))
    }

    fun takePhoto(
        previewView: PreviewView,
        onImage: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        val photoCapture = ImageCapture.Builder().apply {
            setTargetRotation(previewView.rotation)
        }.build()
        currentPhotoCapture = photoCapture
        val photo = object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bytes = image.use {
                    val bb = it.planes[0].buffer
                    val arr = ByteArray(bb.remaining())
                    bb.get(arr)
                    arr
                }
                val file = saveImageFile()
                file.writeBytes(bytes)
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStoreHelper.saveToMediaStore(context, file, MediaStoreHelper.MediaKind.Photo)
                } else file.toUri()
                onImage(uri)
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception.message ?: "Capture failed")
                super.onError(exception)
            }
        }
        photoCapture.takePicture(cameraExecutor, photo)
    }

    fun startVideo(previewView: PreviewView, onVideoReady: (Uri) -> Unit, onError: (String) -> Unit) {
        val name = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date()) + "_record.mp4"
        val contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStoreOutputOptions.Builder(context.contentResolver.uriForMediaStoreVideo(name), MediaStore.RELATIVE_PATH, "CivicReport/recorded").build()
        } else {
            val file = File(context.externalFilesDir("media") ?: context.filesDir, "video_${System.currentTimeMillis()}.mp4")
            file.toUri()
        }
        val videoCapture = VideoCapture.withOutput(contentUri)
        currentVideoCapture = videoCapture
        videoCapture.setOnVideoStartCallback(
            ContextCompat.getMainExecutor(context),
            object : VideoCapture.OnVideoStartCallback() {
                override fun onVideoStart() {
                    super.onVideoStart()
                    onVideoReady(contentUri)
                }
            }
        )
        val video = object : VideoCapture.OnVideoStoppedCallback() {
            override fun onVideoStopped() {
                currentVideoCapture = null
                super.onVideoStopped()
            }
        }
        videoCapture.stop(video)
    }

    fun stopVideo(onVideoReady: (Uri) -> Unit, onError: (String) -> Unit) {
        currentVideoCapture?.stop(object : VideoCapture.OnVideoStoppedCallback() {
            override fun onVideoStopped() {
                currentVideoCapture = null
                super.onVideoStopped()
            }
        })
    }

    fun releaseCamera() {
        preview?.clearSurfaceProvider()
        currentVideoCapture?.stop(object : VideoCapture.OnVideoStoppedCallback() {
            override fun onVideoStopped() {
                super.onVideoStopped()
            }
        })
    }

    private fun saveImageFile(): File {
        val dir = File(context.externalFilesDir("media") ?: context.filesDir, "media").also { it.mkdirs() }
        val name = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date()) + "_capture.jpg"
        return File(dir, name)
    }
}
