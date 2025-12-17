package com.example.imagestitcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.imagestitcher.adapter.ImagePickerAdapter
import com.example.imagestitcher.model.GalleryImage
import kotlinx.coroutines.*

class ImagePickerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SELECTED_URIS = "selected_uris"
        private const val REQUEST_PERMISSION = 100
        private const val TAG = "ImagePickerActivity"
    }

    private lateinit var rvGallery: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyHint: TextView
    private lateinit var tvSelectedCount: TextView
    private lateinit var btnConfirm: Button

    private val images = mutableListOf<GalleryImage>()
    private lateinit var adapter: ImagePickerAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_picker)

        initViews()
        checkPermissionAndLoadImages()
    }

    private fun initViews() {
        rvGallery = findViewById(R.id.rvGallery)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyHint = findViewById(R.id.tvEmptyHint)
        tvSelectedCount = findViewById(R.id.tvSelectedCount)
        btnConfirm = findViewById(R.id.btnConfirm)

        // 设置网格布局（3列）
        rvGallery.layoutManager = GridLayoutManager(this, 3)

        // 初始化Adapter
        adapter = ImagePickerAdapter(images) {
            updateSelectedCount()
        }
        rvGallery.adapter = adapter

        // 确定按钮
        btnConfirm.setOnClickListener {
            val selectedImages = adapter.getSelectedImages()
            if (selectedImages.isEmpty()) {
                Toast.makeText(this, "请至少选择一张图片", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 返回选中的URI列表
            val uris = ArrayList(selectedImages.map { it.uri.toString() })
            val intent = Intent().apply {
                putStringArrayListExtra(EXTRA_SELECTED_URIS, uris)
            }
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    private fun checkPermissionAndLoadImages() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadImages()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadImages()
            } else {
                Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun loadImages() {
        progressBar.visibility = View.VISIBLE
        tvEmptyHint.visibility = View.GONE

        coroutineScope.launch {
            try {
                val loadedImages = withContext(Dispatchers.IO) {
                    queryImagesFromMediaStore()
                }

                images.clear()
                images.addAll(loadedImages)
                adapter.notifyDataSetChanged()

                progressBar.visibility = View.GONE
                if (images.isEmpty()) {
                    tvEmptyHint.visibility = View.VISIBLE
                }

                Log.d(TAG, "成功加载 ${images.size} 张图片")
            } catch (e: Exception) {
                Log.e(TAG, "加载图片失败", e)
                progressBar.visibility = View.GONE
                tvEmptyHint.visibility = View.VISIBLE
                Toast.makeText(this@ImagePickerActivity, "加载图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 从MediaStore查询所有图片，按创建日期降序排列（最新的在前）
     */
    private fun queryImagesFromMediaStore(): List<GalleryImage> {
        val images = mutableListOf<GalleryImage>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )

        // 按DATE_ADDED降序排序（最新的在前）
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val dateAdded = cursor.getLong(dateColumn)

                val contentUri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )

                images.add(
                    GalleryImage(
                        id = id,
                        uri = contentUri,
                        dateAdded = dateAdded,
                        displayName = name
                    )
                )
            }
        }

        Log.d(TAG, "从MediaStore查询到 ${images.size} 张图片")
        return images
    }

    private fun updateSelectedCount() {
        val count = adapter.getSelectedImages().size
        tvSelectedCount.text = "已选 $count 张"
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
