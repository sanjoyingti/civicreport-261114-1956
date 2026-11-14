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
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture? = null
    private var videoRecording: Recording? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val fileProviderAuthority = "${context.packageName}.fileprovider"

    fun initializeCamera(lifecycleOwner: LifecycleOwner, onReady: () -> Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                cameraProvider = future.get()
                val selector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            ResolutionStrategy.FROM_HIGHEST_SUPPORTED_CLASS,
                            ResolutionStrategy.FROM_BACKWARD_COMPATIBLE
                        )
                    )
                    .build()

                preview = Preview.Builder().setResolutionSelector(selector).build()
                imageCapture = ImageCapture.Builder().build()
                videoCapture = VideoCapture.withOutput(
                    FileOutputOptions.Builder(context.cacheDir).setFileName("civic_report.mp4").build()
                )

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, preview!!, imageCapture!!, videoCapture)
                onReady()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun takePhoto(previewView: PreviewView, onImage: (Uri) -> Unit, onError: (String) -> Unit) {
        val file = getPhotoFile()
        val output = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture?.takePicture(
            output, cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onImageCaptured(output: ImageCapture.OutputFileResults) {
                    val fileToUse = output.savedFiles.firstOrNull()?.outputFile ?: file
                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStoreHelper.saveToMediaStore(context, fileToUse, MediaStoreHelper.MediaKind.Photo)
                    } else {
                        Uri.fromFile(fileToUse)
                    }
                    onImage(uri)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception.message ?: "Capture failed")
                }
            }
        )
    }

    fun startVideo(previewView: PreviewView, onVideoReady: (Uri) -> Unit, onError: (String) -> Unit) {
        val name = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date()) + "_record.mp4"
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "CivicReport/recorded")
        }
        val contentUri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (contentUri == null) {
            onError("Could not create video output")
            return
        }
        val output = FileOutputOptions.Builder(contentUri).setFileName(name).build()
        videoCapture = VideoCapture.withOutput(output)

        val surfaceProvider = previewView.surfaceProvider
        preview?.setSurfaceProvider(surfaceProvider)

        videoRecording = videoCapture?.prepareRecording(context, output)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setQualitySelector(QualitySelector.from(Quality.HD))
                }
            }
            .execute(ContextCompat.getMainExecutor(context))
        onVideoReady(contentUri)
    }

    fun stopVideo(onVideoStop: (Uri) -> Unit, onError: (String) -> Unit) {
        videoRecording?.stop()
        videoRecording = null
        onVideoStop(contentUriFromRecording(context, it.output))
    }

    private fun contentUriFromRecording(context: Context, output: Recording.Output?): Uri? {
        return if (output != null) {
            val path = output.savedFile?.absolutePath
            if (path != null) {
                val file = File(path)
                Uri.fromFile(file)
            } else {
                output.savedUri
            }
        } else {
            null
        }
    }

    fun releaseCamera() {
        preview?.let {
            it.setSurfaceProvider(null)
        }
        videoRecording?.stop()
        videoCapture = null
        videoRecording = null
        cameraProvider?.unbindAll()
        cameraProvider = null
    }

    private fun getPhotoFile(): File {
        val dir = File(context.externalFilesDir("media") ?: context.filesDir, "media").also { it.mkdirs() }
        val name = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date()) + "_capture.jpg"
        return File(dir, name)
    }
}
