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
        var is: InputStream? = null
        try {
            is = context.contentResolver.openInputStream(uri)
            val exif = android.media.ExifInterface(is)
            val lat = exif.getAttributeDouble(android.media.ExifInterface.TAG_GPS_LATITUDE, null)
            val lon = exif.getAttributeDouble(android.media.ExifInterface.TAG_GPS_LONGITUDE, null)
            if (lat != null && lon != null) {
                val latRef = exif.getAttribute(android.media.ExifInterface.TAG_GPS_LATITUDE_REF)
                val lonRef = exif.getAttribute(android.media.ExifInterface.TAG_GPS_LONGITUDE_REF)
                val latD = toDegrees(lat, latRef)
                val lonD = toDegrees(lon, lonRef)
                return Pair(latD, lonD)
            }
        } catch (e: Exception) {
            Log.e("CivicReport", "Exif read error", e)
        } finally {
            is?.close()
        }
        return null
    }

    private fun toDegrees(value: Double, ref: String?): Double {
        var d = value
        val lowerRef = ref?.lowercase()
        if (lowerRef == "s" || lowerRef == "w") d = -d
        return d
    }

    fun bitmapFromUri(context: Context, uri: Uri): android.graphics.Bitmap? {
        return try {
            val is = context.contentResolver.openInputStream(uri)
            android.graphics.BitmapFactory.decodeStream(is)
        } catch (e: Exception) {
            null
        }
    }

    fun thumbnailFromUri(context: Context, uri: Uri, w: Int = 200, h: Int = 200): android.graphics.Bitmap? {
        return try {
            val is = context.contentResolver.openInputStream(uri)
            val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = computeSampleSize(is, w, h) }
            android.graphics.BitmapFactory.decodeStream(is, null, opts)
        } catch (e: Exception) {
            null
        }
    }

    private fun computeSampleSize(is: InputStream?, w: Int, h: Int): Int {
        val buf = ByteArray(8192)
        var size = 1
        var bytesRead: Int
        var imgW = 0
        var imgH = 0
        try {
            is?.mark(0)
            val d = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeStream(is, null, d)
            is?.reset()
            imgW = d.outWidth
            imgH = d.outHeight
        } catch (e: Exception) { return 1 }
        while (imgW / size >= w && imgH / size >= h && size < 16) size *= 2
        return size
    }
}
