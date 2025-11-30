package com.example.imagestitcher.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.imagestitcher.model.ImageItem
import com.example.imagestitcher.model.Orientation
import java.io.OutputStream

object BitmapUtils {

    fun loadBitmapFromUri(context: Context, uri: Uri, maxSize: Int = 4096): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                android.graphics.BitmapFactory.decodeStream(inputStream, null, options)

                // 只有当图片非常大时才缩小，保持更高的清晰度
                var scale = 1
                while (options.outWidth / scale > maxSize || options.outHeight / scale > maxSize) {
                    scale *= 2
                }

                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                        inSampleSize = scale
                        // 使用更高质量的解码
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    android.graphics.BitmapFactory.decodeStream(stream, null, decodeOptions)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun stitchImages(images: List<ImageItem>, orientation: Orientation): Bitmap? {
        if (images.isEmpty()) return null

        // 计算裁剪后的图片尺寸
        val croppedImages = images.map { item ->
            val bitmap = item.bitmap
            when (orientation) {
                Orientation.VERTICAL -> {
                    val height = bitmap.height
                    val startY = (height * item.cropStart).toInt()
                    val endY = (height * item.cropEnd).toInt()
                    val cropHeight = endY - startY
                    if (cropHeight > 0) {
                        Bitmap.createBitmap(bitmap, 0, startY, bitmap.width, cropHeight)
                    } else null
                }
                Orientation.HORIZONTAL -> {
                    val width = bitmap.width
                    val startX = (width * item.cropStart).toInt()
                    val endX = (width * item.cropEnd).toInt()
                    val cropWidth = endX - startX
                    if (cropWidth > 0) {
                        Bitmap.createBitmap(bitmap, startX, 0, cropWidth, bitmap.height)
                    } else null
                }
            }
        }.filterNotNull()

        if (croppedImages.isEmpty()) return null

        // 计算最终画布尺寸
        val (finalWidth, finalHeight) = when (orientation) {
            Orientation.VERTICAL -> {
                val maxWidth = croppedImages.maxOf { it.width }
                val totalHeight = croppedImages.sumOf { it.height }
                maxWidth to totalHeight
            }
            Orientation.HORIZONTAL -> {
                val totalWidth = croppedImages.sumOf { it.width }
                val maxHeight = croppedImages.maxOf { it.height }
                totalWidth to maxHeight
            }
        }

        // 创建最终画布
        val result = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 绘制图片
        var offset = 0f
        croppedImages.forEach { bitmap ->
            when (orientation) {
                Orientation.VERTICAL -> {
                    val x = (finalWidth - bitmap.width) / 2f
                    canvas.drawBitmap(bitmap, x, offset, null)
                    offset += bitmap.height
                }
                Orientation.HORIZONTAL -> {
                    val y = (finalHeight - bitmap.height) / 2f
                    canvas.drawBitmap(bitmap, offset, y, null)
                    offset += bitmap.width
                }
            }
        }

        return result
    }

    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "stitched_${System.currentTimeMillis()}.png",
        usePNG: Boolean = true
    ): Uri? {
        val format = if (usePNG) "png" else "jpg"
        val mimeType = if (usePNG) "image/png" else "image/jpeg"
        val finalFileName = if (fileName.endsWith(".jpg") || fileName.endsWith(".png")) {
            fileName.substringBeforeLast(".") + ".$format"
        } else {
            "$fileName.$format"
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, finalFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ImageStitcher")
            }
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            var outputStream: OutputStream? = null
            try {
                outputStream = context.contentResolver.openOutputStream(it)
                outputStream?.let { stream ->
                    if (usePNG) {
                        // PNG无损压缩，质量100
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    } else {
                        // JPEG高质量压缩
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    }
                }
                return it
            } catch (e: Exception) {
                e.printStackTrace()
                context.contentResolver.delete(it, null, null)
            } finally {
                outputStream?.close()
            }
        }

        return null
    }
}
