package com.arin.civicreport.ui.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore

object MediaStoreHelper {
    enum class MediaKind { Photo, Video }

    fun saveToMediaStore(context: Context, file: File, kind: MediaKind): Uri {
        val contentValues = ContentValues().apply {
            put(
                when (kind) {
                    MediaKind.Photo -> MediaStore.Images.Media.DISPLAY_NAME
                    MediaKind.Video -> MediaStore.Video.Media.DISPLAY_NAME
                },
                file.name
            )
            put(
                when (kind) {
                    MediaKind.Photo -> MediaStore.Images.Media.MIME_TYPE
                    MediaKind.Video -> MediaStore.Video.Media.MIME_TYPE
                },
                when (kind) {
                    MediaKind.Photo -> "image/jpeg"
                    MediaKind.Video -> "video/mp4"
                }
            )
            put(
                when (kind) {
                    MediaKind.Photo -> MediaStore.Images.Media.RELATIVE_PATH
                    MediaKind.Video -> MediaStore.Video.Media.RELATIVE_PATH
                },
                "CivicReport/" + when (kind) {
                    MediaKind.Photo -> "photos"
                    MediaKind.Video -> "recorded"
                }
            )
        }
        val collection = when (kind) {
            MediaKind.Photo -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaKind.Video -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        return context.contentResolver.insert(collection, contentValues)!!
    }

    fun getContentUriForVideo(name: String): Uri {
        return MediaStore.Video.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL_PRIMARY.name,
            null,
            null
        )
    }
}
