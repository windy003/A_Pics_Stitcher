package com.example.imagestitcher.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.imagestitcher.R
import com.example.imagestitcher.model.ImageItem
import com.example.imagestitcher.model.Orientation
import com.example.imagestitcher.view.CropImageView

class ImageStitchAdapter(
    private var orientation: Orientation,
    private val onImageAction: (Action, Int) -> Unit
) : RecyclerView.Adapter<ImageStitchAdapter.ImageViewHolder>() {

    private val images = mutableListOf<ImageItem>()

    sealed class Action {
        object Delete : Action()
        object MoveUp : Action()
        object MoveDown : Action()
        object MoveLeft : Action()
        object MoveRight : Action()
        data class CropChanged(val start: Float, val end: Float) : Action()
    }

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: LinearLayout = view.findViewById(R.id.imageContainer)
        val imageView: CropImageView = view.findViewById(R.id.cropImageView)
        val deleteButton: ImageButton = view.findViewById(R.id.btnDelete)
        val moveButton1: ImageButton = view.findViewById(R.id.btnMove1)
        val moveButton2: ImageButton = view.findViewById(R.id.btnMove2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        Log.d("ImageStitchAdapter", "onCreateViewHolder")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stitch_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        Log.d("ImageStitchAdapter", "onBindViewHolder - position: $position")
        val item = images[position]

        Log.d("ImageStitchAdapter", "设置图片: ${item.bitmap.width}x${item.bitmap.height}")
        holder.imageView.setImageBitmap(item.bitmap)
        holder.imageView.cropStart = item.cropStart
        holder.imageView.cropEnd = item.cropEnd
        holder.imageView.orientation = orientation
        holder.imageView.showCropSliders = true

        // 设置裁剪回调
        holder.imageView.onCropChanged = { start, end ->
            onImageAction(Action.CropChanged(start, end), position)
        }

        // 删除按钮
        holder.deleteButton.setOnClickListener {
            onImageAction(Action.Delete, position)
        }

        // 根据方向设置移动按钮
        when (orientation) {
            Orientation.VERTICAL -> {
                holder.moveButton1.setImageResource(R.drawable.ic_arrow_up)
                holder.moveButton2.setImageResource(R.drawable.ic_arrow_down)
                holder.moveButton1.visibility = if (position > 0) View.VISIBLE else View.INVISIBLE
                holder.moveButton2.visibility = if (position < images.size - 1) View.VISIBLE else View.INVISIBLE

                holder.moveButton1.setOnClickListener {
                    onImageAction(Action.MoveUp, position)
                }
                holder.moveButton2.setOnClickListener {
                    onImageAction(Action.MoveDown, position)
                }
            }
            Orientation.HORIZONTAL -> {
                holder.moveButton1.setImageResource(R.drawable.ic_arrow_left)
                holder.moveButton2.setImageResource(R.drawable.ic_arrow_right)
                holder.moveButton1.visibility = if (position > 0) View.VISIBLE else View.INVISIBLE
                holder.moveButton2.visibility = if (position < images.size - 1) View.VISIBLE else View.INVISIBLE

                holder.moveButton1.setOnClickListener {
                    onImageAction(Action.MoveLeft, position)
                }
                holder.moveButton2.setOnClickListener {
                    onImageAction(Action.MoveRight, position)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        Log.d("ImageStitchAdapter", "getItemCount: ${images.size}")
        return images.size
    }

    fun updateImages(newImages: List<ImageItem>) {
        Log.d("ImageStitchAdapter", "updateImages - 新图片数量: ${newImages.size}")
        images.clear()
        images.addAll(newImages)
        Log.d("ImageStitchAdapter", "updateImages - 更新后数量: ${images.size}")
        notifyDataSetChanged()
    }

    fun updateOrientation(newOrientation: Orientation) {
        orientation = newOrientation
        notifyDataSetChanged()
    }
}
