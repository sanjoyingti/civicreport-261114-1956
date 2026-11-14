package com.arin.civicreport.ui.media

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GalleryManager {
    const val PICK_IMAGE_REQUEST = 1001
    const val PICK_VIDEO_REQUEST = 1002

    fun pickPhotoIntent(context: Context): Intent {
        return Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
    }

    fun pickVideoIntent(context: Context): Intent {
        return Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).apply {
            type = "video/*"
        }
    }

    fun hasReadMedia(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    fun extractExifGPS(context: Context, uri: Uri): Pair<Double, Double>? {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val exif = android.media.ExifInterface(inputStream)
            val latRaw = exif.getAttribute(android.media.ExifInterface.TAG_GPS_LATITUDE)
            val lonRaw = exif.getAttribute(android.media.ExifInterface.TAG_GPS_LONGITUDE)
            val latRef = exif.getAttribute(android.media.ExifInterface.TAG_GPS_LATITUDE_REF)
            val lonRef = exif.getAttribute(android.media.ExifInterface.TAG_GPS_LONGITUDE_REF)
            if (latRaw != null && lonRaw != null && latRef != null && lonRef != null) {
                val latD = toDegrees(parseDegrees(latRaw), latRef)
                val lonD = toDegrees(parseDegrees(lonRaw), lonRef)
                return Pair(latD, lonD)
            }
        } catch (e: Exception) {
            Log.e("CivicReport", "Exif read error", e)
        } finally {
            inputStream?.close()
        }
        return null
    }

    private fun parseDegrees(raw: String): Double {
        val parts = raw.split(",").map { it.trim().toFloat() }
        if (parts.size < 3) return raw.toDoubleOrNull() ?: 0.0
        return parts[0] + parts[1] / 60.0f + parts[2] / 3600.0f
    }

    private fun toDegrees(value: Double, ref: String?): Double {
        var d = value
        val lowerRef = ref?.lowercase()
        if (lowerRef == "s" || lowerRef == "w") d = -d
        return d
    }

    fun bitmapFromUri(context: Context, uri: Uri): android.graphics.Bitmap? {
        return try {
            val `is` = context.contentResolver.openInputStream(uri)
            android.graphics.BitmapFactory.decodeStream(`is`)
        } catch (e: Exception) {
            null
        }
    }

    fun thumbnailFromUri(context: Context, uri: Uri, w: Int = 200, h: Int = 200): android.graphics.Bitmap? {
        return try {
            val `is` = context.contentResolver.openInputStream(uri)
            val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = computeSampleSize(`is`, w, h) }
            android.graphics.BitmapFactory.decodeStream(`is`, null, opts)
        } catch (e: Exception) {
            null
        }
    }

    private fun computeSampleSize(`is`: InputStream?, w: Int, h: Int): Int {
        var stream = `is`
        val buf = ByteArray(8192)
        var size = 1
        var bytesRead: Int
        var imgW = 0
        var imgH = 0
        try {
            stream?.mark(0)
            val d = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeStream(stream, null, d)
            stream?.reset()
            imgW = d.outWidth
            imgH = d.outHeight
        } catch (e: Exception) {
            return 1
        }
        while (imgW / size >= w && imgH / size >= h && size < 16) size *= 2
        return size
    }
}
