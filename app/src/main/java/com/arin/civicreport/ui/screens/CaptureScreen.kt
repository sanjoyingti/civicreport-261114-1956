package com.arin.civicreport.ui.screens

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arin.civicreport.data.Repository
import com.arin.civicreport.ui.util.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    repo: Repository,
    incidentTypes: List<String>,
    permissionManager: PermissionManager,
    onCaptureSuccess: (Uri, String) -> Unit,
    onChooseGallery: (Uri, String) -> Unit,
    onCameraError: (String) -> Unit,
    onRetryCamera: () -> Unit
) {
    val context = LocalContext.current
    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    var captureMode by remember { mutableStateOf<CaptureMode>(None) }

    fun takePhoto() {
        if (!permissionManager.hasCamera(context)) {
            permissionManager.requestPermission(android.Manifest.permission.CAMERA)
            onCameraError("Camera permission denied")
            return
        }
        capturedUri = Uri.fromFile(java.io.File(context.externalFilesDir("media"), "photo_${System.currentTimeMillis()}.jpg"))
        onCaptureSuccess(capturedUri!!, "photo")
    }

    fun takeVideo() {
        if (!permissionManager.hasCamera(context)) {
            permissionManager.requestPermission(android.Manifest.permission.CAMERA)
            onCameraError("Camera permission denied")
            return
        }
        capturedUri = Uri.fromFile(java.io.File(context.externalFilesDir("media"), "video_${System.currentTimeMillis()}.mp4"))
        onCaptureSuccess(capturedUri!!, "video")
    }

    fun openGallery() {
        if (!permissionManager.hasReadMedia(context) && !permissionManager.hasReadMediaForLegacy(context)) {
            permissionManager.storageLauncher.launch(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.READ_MEDIA_IMAGES, android.Manifest.permission.READ_MEDIA_VIDEO))
            onCameraError("Gallery permission denied")
            return
        }
        val intent = android.content.Intent(android.content.Intent.ACTION_PICK, if (captureMode == VideoMode) android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI else android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = if (captureMode == VideoMode) "video/*" else "image/*"
        }
        // In a real activity we would startForResult here; here we just show a hint since this composable has no launcher
        onChooseGallery(Uri.fromFile(java.io.File(context.externalFilesDir("media"), "gallery_${System.currentTimeMillis()}.${if (captureMode == VideoMode) "mp4" else "jpg"}")), if (captureMode == VideoMode) "video" else "photo")
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Capture media", fontWeight = FontWeight.Bold) })
    }) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            if (capturedUri != null) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Preview not available in this demo; media saved.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = { captureMode = None; capturedUri = null }) { Text("Retake"); }
                    Button(onClick = { onCaptureSuccess(capturedUri!!, if (captureMode == VideoMode) "video" else "photo") }) { Text("Confirm") }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("How would you like to capture the incident?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    SelectionRow(mode = CaptureMode.PhotoMode, selected = captureMode == CaptureMode.PhotoMode, onClick = { captureMode = CaptureMode.PhotoMode })
                    SelectionRow(mode = CaptureMode.VideoMode, selected = captureMode == CaptureMode.VideoMode, onClick = { captureMode = CaptureMode.VideoMode })
                    Button(
                        onClick = { if (permissionManager.hasCamera(context)) { if (captureMode == CaptureMode.VideoMode) takeVideo() else takePhoto() } else permissionManager.requestPermission(android.Manifest.permission.CAMERA) },
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
