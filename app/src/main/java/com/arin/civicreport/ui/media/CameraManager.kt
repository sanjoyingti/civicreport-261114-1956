package com.arin.civicreport.ui.media

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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
    private var currentPhotoCapture: PhotoCapture? = null
    private var currentVideoCapture: VideoCapture<MediaStoreOutput>? = null

    fun startCamera(previewView: PreviewView, onCameraInitialized: () -> Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(context, cameraSelector, preview)
                onCameraInitialized()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
        cameraProviderFuture = context
    }

    fun bindTakePhoto(previewView: PreviewView, onImage: (Uri) -> Unit): PhotoCapture {
        val photoCapture = ImageCapture.Builder().apply {
            setTargetRotation(previewView.rotation)
        }.build()
        currentPhotoCapture = photoCapture
        val photo = object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bytes = image.use { it.planes[0].buffer.toByteArray() }
                val file = saveImageFile()
                file.writeBytes(bytes)
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStoreHelper.saveToMediaStore(context, file, MediaStoreHelper.MediaKind.Photo)
                } else file.toUri()
                onImage(uri)
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
            }
        }
        photoCapture.takePicture(cameraExecutor, photo)
        return photoCapture
    }

    fun bindVideoCapture(previewView: PreviewView): VideoCapture<MediaStoreOutput> {
        val videoCapture = VideoCapture.withOutput(
            MediaStoreOutputOptions.Builder(
                MediaStoreHelper.videoContentUri(context),
                MediaStore.RELATIVE_PATH,
                MediaStore.Video.Media.RELATIVE_PATH
            ).build()
        )
        currentVideoCapture = videoCapture
        return videoCapture
    }

    private fun saveImageFile(): File {
        val dir = File(context.getExternalFilesDir(null), "media").also { it.mkdirs() }
        val name = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date()) + "_capture.jpg"
        return File(dir, name)
    }

    fun bindToLifecycle(lifecycleProvider: androidx.lifecycle.LifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            provider.unbindAll()
        }, ContextCompat.getMainExecutor(context))
    }
}

object MediaStoreHelper {
    enum class MediaKind { Photo, Video }

    fun saveToMediaStore(context: Context, source: File, kind: MediaKind): Uri {
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            if (kind == MediaKind.Photo) {
                put(MediaStore.Images.Media.DISPLAY_NAME, "report_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CivicReport")
            } else {
                put(MediaStore.Video.Media.DISPLAY_NAME, "report_${System.currentTimeMillis()}.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CivicReport")
            }
        }
        val collection = if (kind == MediaKind.Photo) MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, contentValues) ?: source.toUri()
        resolver.openOutputStream(uri)?.use { out ->
            source.inputStream().use { `in` -> out.write(`in`.readAllBytes()) }
        }
        return uri
    }

    fun photoContentUri(context: Context): Uri {
        return MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    fun videoContentUri(context: Context): Uri {
        return MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }
}
