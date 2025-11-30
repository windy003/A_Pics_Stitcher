package com.example.imagestitcher.manager

import com.example.imagestitcher.model.StitchState

class UndoManager(private val maxHistorySize: Int = 20) {

    private val history = mutableListOf<StitchState>()
    private var currentIndex = -1

    fun saveState(state: StitchState) {
        // 移除当前位置之后的所有状态
        if (currentIndex < history.size - 1) {
            history.subList(currentIndex + 1, history.size).clear()
        }

        // 添加新状态
        history.add(state.clone())
        currentIndex++

        // 限制历史记录大小
        if (history.size > maxHistorySize) {
            history.removeAt(0)
            currentIndex--
        }
    }

    fun canUndo(): Boolean = currentIndex > 0

    fun undo(): StitchState? {
        if (!canUndo()) return null

        currentIndex--
        return history[currentIndex].clone()
    }

    fun getCurrentState(): StitchState? {
        return if (currentIndex >= 0 && currentIndex < history.size) {
            history[currentIndex].clone()
        } else null
    }

    fun clear() {
        history.clear()
        currentIndex = -1
    }
}
