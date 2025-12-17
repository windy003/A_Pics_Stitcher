package com.example.imagestitcher.adapter

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
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.ivThumbnail)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val cbSelected: CheckBox = view.findViewById(R.id.cbSelected)
        val vSelectedOverlay: View = view.findViewById(R.id.vSelectedOverlay)

        init {
            // 点击整个项目切换选中状态
            view.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val image = images[position]
                    image.isSelected = !image.isSelected
                    notifyItemChanged(position)
                    onSelectionChanged()
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
     * 获取所有选中的图片
     */
    fun getSelectedImages(): List<GalleryImage> {
        return images.filter { it.isSelected }
    }
}
