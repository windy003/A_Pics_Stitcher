package com.example.imagestitcher.model

import android.graphics.Bitmap
import android.net.Uri

data class ImageItem(
    val id: Long,
    val uri: Uri,
    var bitmap: Bitmap,
    var cropStart: Float = 0f,  // 裁剪开始位置 (0-1)
    var cropEnd: Float = 1f     // 裁剪结束位置 (0-1)
) {
    fun clone(): ImageItem {
        return ImageItem(
            id = id,
            uri = uri,
            bitmap = bitmap,
            cropStart = cropStart,
            cropEnd = cropEnd
        )
    }
}
