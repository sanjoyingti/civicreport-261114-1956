package com.arin.civicreport.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arin.civicreport.ui.location.LocationManager
import com.arin.civicreport.ui.media.CameraManager
import com.arin.civicreport.ui.media.GalleryManager
import com.arin.civicreport.ui.screens.report.ReportFlowViewModel
import com.arin.civicreport.ui.util.PermissionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    reportViewModel: ReportFlowViewModel,
    permissionManager: PermissionManager,
    onBack: () -> Unit,
    onCaptured: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val cameraManager = remember { CameraManager(context) }
    val locationManager = remember { LocationManager(context) }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraReady by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var cameraGranted by remember { mutableStateOf(permissionManager.hasCamera(context)) }
    var error by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
        if (!granted) error = "Camera permission denied. You can still pick from the gallery."
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mime = context.contentResolver.getType(uri).orEmpty()
        val isVideo = mime.startsWith("video")
        reportViewModel.setMedia(uri.toString(), if (isVideo) "video" else "photo")
        val gps = if (!isVideo) GalleryManager.extractExifGPS(context, uri) else null
        if (gps != null) {
            scope.launch {
                val loc = locationManager.reverseGeocode(gps.first, gps.second)
                reportViewModel.setLocation(
                    loc.latitude, loc.longitude, loc.address,
                    loc.city, loc.district, loc.state, loc.pincode
                )
                onCaptured()
            }
        } else {
            onCaptured()
        }
    }

    DisposableEffect(Unit) {
        onDispose { cameraManager.release() }
    }

    LaunchedEffect(cameraGranted, previewView) {
        val view = previewView
        if (cameraGranted && view != null) {
            cameraManager.bind(
                lifecycleOwner = lifecycleOwner,
                previewView = view,
                onReady = { cameraReady = true },
                onError = { message -> error = message }
            )
        }
    }

    fun capturePhoto() {
        if (!cameraGranted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        if (!cameraReady) {
            error = "The camera is still starting, try again in a moment."
            return
        }
        error = null
        status = "Saving photo..."
        cameraManager.takePhoto(
            onSaved = { uri ->
                status = null
                reportViewModel.setMedia(uri.toString(), "photo")
                onCaptured()
            },
            onError = { message ->
                status = null
                error = message
            }
        )
    }

    fun toggleRecording() {
        if (!cameraGranted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        if (!cameraReady) {
            error = "The camera is still starting, try again in a moment."
            return
        }
        error = null
        if (recording) {
            cameraManager.stopRecording()
            recording = false
            status = "Saving video..."
        } else {
            if (!permissionManager.hasAudio(context)) {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            recording = true
            status = "Recording..."
            cameraManager.startRecording(
                onFinished = { uri ->
                    recording = false
                    status = null
                    reportViewModel.setMedia(uri.toString(), "video")
                    onCaptured()
                },
                onError = { message ->
                    recording = false
                    status = null
                    error = message
                }
            )
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Capture media", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        error ?: "",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            if (status != null) {
                Text(status ?: "", style = MaterialTheme.typography.bodyMedium)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx -> PreviewView(ctx).also { previewView = it } }
                )
                if (!cameraGranted) {
                    Text(
                        "Camera permission is needed for the live preview",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                } else if (!cameraReady) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            Button(onClick = { capturePhoto() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Take photo")
            }
            Button(
                onClick = { toggleRecording() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (recording) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (recording) "Stop recording" else "Record video")
            }
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Photo from gallery")
            }
            OutlinedButton(
                onClick = { galleryLauncher.launch("video/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Video from gallery")
            }
            Text(
                "Photos and videos are saved with the exact GPS position of the spot.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
