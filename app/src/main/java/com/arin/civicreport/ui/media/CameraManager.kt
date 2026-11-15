package com.arin.civicreport.ui.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Small wrapper around CameraX: binds a preview, takes photos and records videos into app storage. */
class CameraManager(private val context: Context) {

    private var provider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private fun authority(): String = "${context.packageName}.fileprovider"

    fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onReady: () -> Unit,
        onError: (String) -> Unit
    ) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val cameraProvider = future.get()
                provider = cameraProvider

                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD))
                    .build()
                val video = VideoCapture.withOutput(recorder)

                imageCapture = capture
                videoCapture = video

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture,
                    video
                )
                onReady()
            } catch (e: Exception) {
                onError(e.message ?: "Camera could not be started")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun takePhoto(onSaved: (Uri) -> Unit, onError: (String) -> Unit) {
        val capture = imageCapture
        if (capture == null) {
            onError("Camera is not ready")
            return
        }
        val file = newFile("jpg")
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onSaved(FileProvider.getUriForFile(context, authority(), file))
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception.message ?: "Photo capture failed")
                }
            }
        )
    }

    fun startRecording(onFinished: (Uri) -> Unit, onError: (String) -> Unit) {
        val video = videoCapture
        if (video == null) {
            onError("Camera is not ready")
            return
        }
        val file = newFile("mp4")
        val options = FileOutputOptions.Builder(file).build()
        var pending: PendingRecording = video.output.prepareRecording(context, options)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            pending = pending.withAudioEnabled()
        }
        recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            if (event is VideoRecordEvent.Finalize) {
                recording = null
                if (event.hasError()) {
                    onError("Recording failed (code ${event.error})")
                } else {
                    onFinished(FileProvider.getUriForFile(context, authority(), file))
                }
            }
        }
    }

    fun stopRecording() {
        recording?.stop()
        recording = null
    }

    fun release() {
        recording?.stop()
        recording = null
        imageCapture = null
        videoCapture = null
        provider?.unbindAll()
        provider = null
    }

    private fun newFile(extension: String): File {
        val base = context.getExternalFilesDir("media") ?: File(context.filesDir, "media").also { it.mkdirs() }
        base.mkdirs()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        return File(base, "report_$stamp.$extension")
    }
}
