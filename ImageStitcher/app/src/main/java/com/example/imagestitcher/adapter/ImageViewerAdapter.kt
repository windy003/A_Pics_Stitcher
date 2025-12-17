package com.example.imagestitcher.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.imagestitcher.R
import com.example.imagestitcher.model.GalleryImage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ImageViewerAdapter(
    private val images: List<GalleryImage>
) : RecyclerView.Adapter<ImageViewerAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivFullImage: ImageView = view.findViewById(R.id.ivFullImage)
        val llDateTimeInfo: LinearLayout = view.findViewById(R.id.llDateTimeInfo)
        val tvFullDate: TextView = view.findViewById(R.id.tvFullDate)
        val tvFullTime: TextView = view.findViewById(R.id.tvFullTime)

        init {
            // 点击图片切换日期时间信息的显示/隐藏
            ivFullImage.setOnClickListener {
                llDateTimeInfo.visibility = if (llDateTimeInfo.visibility == View.VISIBLE) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_viewer, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val image = images[position]

        // 加载全尺寸图片
        holder.ivFullImage.setImageURI(image.uri)

        // 设置日期和时间
        val date = Date(image.dateAdded * 1000)
        holder.tvFullDate.text = formatDateString(date)
        holder.tvFullTime.text = timeFormat.format(date)

        // 默认隐藏日期时间信息
        holder.llDateTimeInfo.visibility = View.GONE
    }

    override fun getItemCount(): Int = images.size

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
}
