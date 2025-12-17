package com.example.imagestitcher

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.imagestitcher.adapter.ImageViewerAdapter
import com.example.imagestitcher.model.GalleryImage

class ImageViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGES = "extra_images"
        const val EXTRA_POSITION = "extra_position"
        const val EXTRA_SELECT_FROM_POSITION = "extra_select_from_position"
        const val EXTRA_INCLUDE_CURRENT = "extra_include_current"
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var btnClose: ImageView
    private lateinit var tvImageCount: TextView
    private lateinit var btnSelectBefore: Button
    private lateinit var btnSelectIncludingCurrent: Button
    private lateinit var adapter: ImageViewerAdapter
    private var images: ArrayList<GalleryImage> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 隐藏 ActionBar
        supportActionBar?.hide()

        setContentView(R.layout.activity_image_viewer)

        // 隐藏状态栏和导航栏，实现全屏效果
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        // 获取传递的数据
        @Suppress("DEPRECATION")
        images = intent.getParcelableArrayListExtra(EXTRA_IMAGES) ?: arrayListOf()
        val initialPosition = intent.getIntExtra(EXTRA_POSITION, 0)

        if (images.isEmpty()) {
            finish()
            return
        }

        initViews()
        setupViewPager(initialPosition)
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        btnClose = findViewById(R.id.btnClose)
        tvImageCount = findViewById(R.id.tvImageCount)
        btnSelectBefore = findViewById(R.id.btnSelectBefore)
        btnSelectIncludingCurrent = findViewById(R.id.btnSelectIncludingCurrent)

        // 关闭按钮
        btnClose.setOnClickListener {
            finish()
        }

        // "前前"按钮：选择当前图片之前的（不包括当前）
        btnSelectBefore.setOnClickListener {
            val currentPosition = viewPager.currentItem
            if (currentPosition > 0) {
                // 返回选择当前图片之前的，不包括当前图片
                returnSelection(currentPosition, includeCurrent = false)
            } else {
                // 如果是第一张图片，没有之前的图片
                android.widget.Toast.makeText(this, "当前是第一张图片，没有之前的图片", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        // "这前"按钮：选择包括当前图片及之前的
        btnSelectIncludingCurrent.setOnClickListener {
            val currentPosition = viewPager.currentItem
            // 返回选择包括当前图片及之前的所有图片
            returnSelection(currentPosition, includeCurrent = true)
        }
    }

    /**
     * 返回选择结果
     * @param position 当前图片位置
     * @param includeCurrent 是否包括当前图片
     */
    private fun returnSelection(position: Int, includeCurrent: Boolean) {
        val intent = Intent().apply {
            putExtra(EXTRA_SELECT_FROM_POSITION, position)
            putExtra(EXTRA_INCLUDE_CURRENT, includeCurrent)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun setupViewPager(initialPosition: Int) {
        adapter = ImageViewerAdapter(images)
        viewPager.adapter = adapter

        // 设置初始位置
        viewPager.setCurrentItem(initialPosition, false)

        // 更新计数
        updateImageCount(initialPosition)

        // 监听页面切换
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateImageCount(position)
            }
        })
    }

    private fun updateImageCount(position: Int) {
        tvImageCount.text = "${position + 1} / ${images.size}"
    }

    override fun onBackPressed() {
        @Suppress("DEPRECATION")
        super.onBackPressed()
        finish()
    }
}
