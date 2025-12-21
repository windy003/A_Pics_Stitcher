package com.example.imagestitcher.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.imagestitcher.R
import com.example.imagestitcher.model.GalleryImage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ImagePickerAdapter(
    private val images: List<GalleryImage>,
    private val onSelectionChanged: () -> Unit,
    private val onZoomClick: (Int) -> Unit
) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

    companion object {
        private const val TAG = "ImagePickerAdapter"
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // 用于跟踪选择顺序的计数器
    private var selectionCounter = 1

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.ivThumbnail)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val cbSelected: CheckBox = view.findViewById(R.id.cbSelected)
        val vSelectedOverlay: View = view.findViewById(R.id.vSelectedOverlay)
        val btnZoom: ImageView = view.findViewById(R.id.btnZoom)

        init {
            // 点击整个项目自动选择从第0张到当前位置的所有图片
            view.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // 清除所有之前的选择
                    images.forEach {
                        it.isSelected = false
                        it.selectionOrder = 0
                    }

                    // 选择从第0张到当前点击位置的所有图片
                    for (i in 0..position) {
                        val img = images[i]
                        img.isSelected = true
                        img.selectionOrder = i + 1  // 按位置顺序分配order
                        Log.d(TAG, "自动选中: 位置$i, ${img.displayName}, selectionOrder=${img.selectionOrder}")
                    }

                    // 重置计数器
                    selectionCounter = position + 2

                    Log.d(TAG, "点击位置$position，自动选择了 ${position + 1} 张图片")

                    // 刷新所有项目的显示状态
                    notifyDataSetChanged()
                    onSelectionChanged()
                }
            }

            // 点击放大按钮打开全屏查看
            btnZoom.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onZoomClick(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val image = images[position]

        // 加载缩略图
        holder.ivThumbnail.setImageURI(image.uri)

        // 显示日期和时间
        val date = Date(image.dateAdded * 1000) // dateAdded是秒级时间戳
        holder.tvDate.text = formatDateString(date)
        holder.tvTime.text = timeFormat.format(date)

        // 更新选中状态
        holder.cbSelected.isChecked = image.isSelected
        holder.vSelectedOverlay.visibility = if (image.isSelected) View.VISIBLE else View.GONE
    }

    /**
     * 格式化日期字符串：今天、昨天、前天或具体日期
     */
    private fun formatDateString(date: Date): String {
        val calendar = Calendar.getInstance()
        val today = calendar.clone() as Calendar
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val imageCalendar = Calendar.getInstance()
        imageCalendar.time = date
        imageCalendar.set(Calendar.HOUR_OF_DAY, 0)
        imageCalendar.set(Calendar.MINUTE, 0)
        imageCalendar.set(Calendar.SECOND, 0)
        imageCalendar.set(Calendar.MILLISECOND, 0)

        val daysDiff = ((today.timeInMillis - imageCalendar.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

        return when (daysDiff) {
            0 -> "今天"
            1 -> "昨天"
            2 -> "前天"
            else -> dateFormat.format(date)
        }
    }

    override fun getItemCount(): Int = images.size

    /**
     * 重新调整选择顺序，确保顺序连续
     */
    private fun reorderSelection() {
        val selectedImages = images.filter { it.isSelected }
            .sortedBy { it.selectionOrder }

        selectedImages.forEachIndexed { index, image ->
            image.selectionOrder = index + 1
        }

        selectionCounter = selectedImages.size + 1
    }

    /**
     * 获取所有选中的图片，按选择顺序排序（第一个选中的在最前面）
     */
    fun getSelectedImages(): List<GalleryImage> {
        return images.filter { it.isSelected }
            .sortedBy { it.selectionOrder }
    }
}
