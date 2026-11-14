package com.arin.civicreport.ui.screens

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CircleOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arin.civicreport.ui.media.CameraManager
import com.arin.civicreport.ui.util.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    incidentTypes: List<String>,
    permissionManager: PermissionManager,
    onCaptureSuccess: (Uri, String) -> Unit,
    onChooseGallery: (Uri, String) -> Unit,
    onCameraError: (String) -> Unit,
    onRetryCamera: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    var captureMode by remember { mutableStateOf<CaptureMode>(None) }
    val cameraManager = remember { CameraManager(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraReady by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val type = if (it.toString().lowercase().endsWith(".mp4") || it.toString().lowercase().endsWith(".webm")) "video" else "photo"
            onChooseGallery(it, type)
        }
    }

    LaunchedEffect(Unit) {
        cameraManager.initializeCamera(lifecycleOwner) {
            cameraReady = true
        }
    }

    fun takePhoto() {
        if (!permissionManager.hasCamera(context)) {
            permissionManager.requestCameraPermission()
            onCameraError("Camera permission denied")
            return
        }
        if (!cameraReady) {
            onCameraError("Camera not ready")
            return
        }
        previewView?.let {
            cameraManager.takePhoto(it) { uri ->
                capturedUri = uri
                onCaptureSuccess(uri, "photo")
            } onError = { err ->
                onCameraError("Camera error: $err")
            }
        }
    }

    fun takeVideo() {
        if (!permissionManager.hasCamera(context)) {
            permissionManager.requestCameraPermission()
            onCameraError("Camera permission denied")
            return
        }
        if (!cameraReady) {
            onCameraError("Camera not ready")
            return
        }
        previewView?.let {
            cameraManager.startVideo(it) { uri ->
                capturedUri = uri
                onCaptureSuccess(uri, "video")
            } onError = { err ->
                onCameraError("Camera error: $err")
            }
        }
    }

    fun openGallery() {
        if (!permissionManager.hasReadMedia(context) && !permissionManager.hasReadMediaForLegacy(context)) {
            permissionManager.requestLocationPermissions()
            onCameraError("Gallery permission denied")
            return
        }
        launcher.launch(
            if (captureMode == VideoMode) "video/*" else "image/*"
        )
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Capture media", fontWeight = FontWeight.Bold) })
    }) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            Text("Viewfinder", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!cameraReady) {
                    CircularProgressIndicator()
                }
                PreviewView(
                    modifier = Modifier.fillMaxSize(),
                    onAttachedToWindow = { previewView = it },
                    onDetachedFromWindow = { previewView = null }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (capturedUri != null) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Preview: ${capturedUri?.lastPathSegment}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = { captureMode = None; capturedUri = null }) { Text("Retake") }
                    Button(onClick = { onCaptureSuccess(capturedUri!!, if (captureMode == VideoMode) "video" else "photo") }) { Text("Confirm") }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("How would you like to capture the incident?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    SelectionRow(mode = CaptureMode.PhotoMode, selected = captureMode == CaptureMode.PhotoMode, onClick = { captureMode = CaptureMode.PhotoMode })
                    SelectionRow(mode = CaptureMode.VideoMode, selected = captureMode == CaptureMode.VideoMode, onClick = { captureMode = CaptureMode.VideoMode })
                    Button(
                        onClick = {
                            if (permissionManager.hasCamera(context) && cameraReady) {
                                if (captureMode == VideoMode) takeVideo() else takePhoto()
                            } else permissionManager.requestCameraPermission()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text(if (captureMode == CaptureMode.VideoMode) "Record Video" else "Take Photo")
                    }
                    Button(
                        onClick = { openGallery() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Choose from Gallery")
                    }
                }
            }
        }
    }
}

sealed class CaptureMode { object PhotoMode : CaptureMode; object VideoMode : CaptureMode; object None : CaptureMode }

@Composable
private fun SelectionRow(mode: CaptureMode, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (mode == CaptureMode.PhotoMode) "Take Photo" else "Record Video", fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            Icon(if (selected) Icons.Default.CheckCircle else Icons.Default.CircleOutline, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        }
    }
}
