package com.example.imagestitcher.model

import android.net.Uri

/**
 * 相册图片数据模型
 * @param id 图片ID
 * @param uri 图片URI
 * @param dateAdded 创建时间（秒级时间戳）
 * @param displayName 图片文件名
 * @param isSelected 是否被选中
 */
data class GalleryImage(
    val id: Long,
    val uri: Uri,
    val dateAdded: Long,
    val displayName: String,
    var isSelected: Boolean = false
)
