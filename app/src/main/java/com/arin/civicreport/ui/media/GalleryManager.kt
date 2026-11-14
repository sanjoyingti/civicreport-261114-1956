package com.arin.civicreport.ui.media

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
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

    fun extractExifGPS(context: Context, uri: Uri): Pair<Double, Double>? {
        var is: InputStream? = null
        try {
            is = context.contentResolver.openInputStream(uri)
            val exif = ExifInterface(is)
            val lat = exif.getAttributeDouble(ExifInterface.TAG_GPS_LATITUDE, null)
            val lon = exif.getAttributeDouble(ExifInterface.TAG_GPS_LONGITUDE, null)
            if (lat != null && lon != null) {
                val latD = toDegrees(lat, exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF))
                val lonD = toDegrees(lon, exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF))
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
        val d = value
        return d // simplified: ExifInterface already returns decimal degrees when using getAttributeDouble
    }

    fun bitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val is = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(is)
        } catch (e: Exception) {
            null
        }
    }

    fun thumbnailFromUri(context: Context, uri: Uri, w: Int = 200, h: Int = 200): Bitmap? {
        return try {
            val is = context.contentResolver.openInputStream(uri)
            val opts = BitmapFactory.Options().apply { inSampleSize = computeSampleSize(is, w, h) }
            BitmapFactory.decodeStream(is, null, opts)
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
            val d = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(is, null, d)
            is?.reset()
            imgW = d.outWidth
            imgH = d.outHeight
        } catch (e: Exception) { return 1 }
        while (imgW / size >= w && imgH / size >= h && size < 16) size *= 2
        return size
    }
}
