package com.example.imagestitcher.model

data class StitchState(
    val images: List<ImageItem>,
    val orientation: Orientation
) {
    fun clone(): StitchState {
        return StitchState(
            images = images.map { it.clone() },
            orientation = orientation
        )
    }
}

enum class Orientation {
    VERTICAL,    // 纵向拼接
    HORIZONTAL   // 横向拼接
}
