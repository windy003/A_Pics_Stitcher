package com.example.imagestitcher

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.imagestitcher.adapter.ImageStitchAdapter
import com.example.imagestitcher.databinding.ActivityMainBinding
import com.example.imagestitcher.manager.UndoManager
import com.example.imagestitcher.model.ImageItem
import com.example.imagestitcher.model.Orientation
import com.example.imagestitcher.model.StitchState
import com.example.imagestitcher.utils.BitmapUtils
import kotlinx.coroutines.*
import java.util.Collections

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val images = mutableListOf<ImageItem>()
    private var orientation = Orientation.VERTICAL
    private val undoManager = UndoManager()
    private lateinit var adapter: ImageStitchAdapter
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val pickImages = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uriStrings = result.data?.getStringArrayListExtra(ImagePickerActivity.EXTRA_SELECTED_URIS)
            if (uriStrings != null && uriStrings.isNotEmpty()) {
                val uris = uriStrings.map { Uri.parse(it) }
                Log.d("MainActivity", "选择了 ${uris.size} 张图片")
                loadImages(uris)
            } else {
                Toast.makeText(this, "未选择任何图片", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            saveStitchedImage()
        } else {
            Toast.makeText(this, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show()
        }
    }

    private val deleteImagesLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d("MainActivity", "用户确认删除图片")
            Toast.makeText(this, "原图片已删除", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("MainActivity", "用户取消删除图片")
            Toast.makeText(this, "已取消删除原图片", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupButtons()
        updateUI()

        // 自动打开图片选择器
        val intent = Intent(this, ImagePickerActivity::class.java)
        pickImages.launch(intent)
    }

    private fun setupRecyclerView() {
        adapter = ImageStitchAdapter(orientation) { action, position ->
            handleImageAction(action, position)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupButtons() {
        // 导入图片
        binding.btnImport.setOnClickListener {
            val intent = Intent(this, ImagePickerActivity::class.java)
            pickImages.launch(intent)
        }

        // 切换方向
        binding.btnToggleOrientation.setOnClickListener {
            orientation = if (orientation == Orientation.VERTICAL) {
                Orientation.HORIZONTAL
            } else {
                Orientation.VERTICAL
            }
            updateOrientation()
            saveCurrentState()
        }

        // 撤销
        binding.btnUndo.setOnClickListener {
            performUndo()
        }

        // 预览
        binding.btnPreview.setOnClickListener {
            showPreview()
        }

        // 保存
        binding.btnSave.setOnClickListener {
            checkPermissionAndSave()
        }
    }

    private fun loadImages(uris: List<Uri>) {
        Log.d("MainActivity", "开始加载 ${uris.size} 张图片")
        binding.progressBar.visibility = android.view.View.VISIBLE
        scope.launch {
            val newImages = withContext(Dispatchers.IO) {
                // 反转顺序，使第一个选中的图片显示在最后面
                uris.reversed().mapNotNull { uri ->
                    Log.d("MainActivity", "加载图片: $uri")
                    val bitmap = BitmapUtils.loadBitmapFromUri(this@MainActivity, uri)
                    if (bitmap == null) {
                        Log.e("MainActivity", "图片加载失败: $uri")
                    } else {
                        Log.d("MainActivity", "图片加载成功: ${bitmap.width}x${bitmap.height}")
                    }
                    bitmap?.let {
                        ImageItem(
                            id = System.currentTimeMillis() + uri.hashCode(),
                            uri = uri,
                            bitmap = it
                        )
                    }
                }
            }

            Log.d("MainActivity", "成功加载 ${newImages.size} 张图片，当前总数: ${images.size + newImages.size}")
            images.addAll(newImages)
            Log.d("MainActivity", "调用 adapter.updateImages，图片数量: ${images.size}")
            adapter.updateImages(images)
            Log.d("MainActivity", "adapter.itemCount = ${adapter.itemCount}")
            updateUI()
            saveCurrentState()
            binding.progressBar.visibility = android.view.View.GONE

            if (newImages.isNotEmpty()) {
                Toast.makeText(this@MainActivity, "已导入 ${newImages.size} 张图片", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "图片加载失败，请检查权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleImageAction(action: ImageStitchAdapter.Action, position: Int) {
        Log.d("MainActivity", "handleImageAction: $action, position: $position")
        when (action) {
            is ImageStitchAdapter.Action.Delete -> {
                Log.d("MainActivity", "删除图片 at position: $position")
                if (position < images.size) {
                    images.removeAt(position)
                    adapter.updateImages(images)
                    updateUI()
                    saveCurrentState()
                }
            }
            is ImageStitchAdapter.Action.MoveUp -> {
                Log.d("MainActivity", "上移图片 from position: $position to ${position - 1}")
                if (position > 0) {
                    Collections.swap(images, position, position - 1)
                    adapter.updateImages(images)
                    saveCurrentState()
                }
            }
            is ImageStitchAdapter.Action.MoveDown -> {
                Log.d("MainActivity", "下移图片 from position: $position to ${position + 1}")
                if (position < images.size - 1) {
                    Collections.swap(images, position, position + 1)
                    adapter.updateImages(images)
                    saveCurrentState()
                }
            }
            is ImageStitchAdapter.Action.MoveLeft -> {
                Log.d("MainActivity", "左移图片 from position: $position to ${position - 1}")
                if (position > 0) {
                    Collections.swap(images, position, position - 1)
                    adapter.updateImages(images)
                    saveCurrentState()
                }
            }
            is ImageStitchAdapter.Action.MoveRight -> {
                Log.d("MainActivity", "右移图片 from position: $position to ${position + 1}")
                if (position < images.size - 1) {
                    Collections.swap(images, position, position + 1)
                    adapter.updateImages(images)
                    saveCurrentState()
                }
            }
            is ImageStitchAdapter.Action.CropChanged -> {
                if (position < images.size) {
                    images[position].cropStart = action.start
                    images[position].cropEnd = action.end
                    saveCurrentState()
                }
            }
        }
    }

    private fun updateOrientation() {
        val layoutManager = if (orientation == Orientation.VERTICAL) {
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        } else {
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        }

        binding.recyclerView.layoutManager = layoutManager
        adapter.updateOrientation(orientation)

        // 更新按钮图标
        binding.btnToggleOrientation.setImageResource(
            if (orientation == Orientation.VERTICAL) {
                R.drawable.ic_horizontal
            } else {
                R.drawable.ic_vertical
            }
        )
    }

    private fun saveCurrentState() {
        val state = StitchState(images.toList(), orientation)
        undoManager.saveState(state)
        updateUI()
    }

    private fun performUndo() {
        val state = undoManager.undo()
        state?.let {
            images.clear()
            images.addAll(it.images)
            orientation = it.orientation
            adapter.updateImages(images)
            updateOrientation()
            updateUI()
        }
    }

    private fun updateUI() {
        val hasImages = images.isNotEmpty()
        Log.d("MainActivity", "updateUI - hasImages: $hasImages, images.size: ${images.size}")
        binding.btnSave.isEnabled = hasImages
        binding.btnPreview.isEnabled = hasImages
        binding.btnUndo.isEnabled = undoManager.canUndo()
        binding.btnToggleOrientation.isEnabled = hasImages

        if (hasImages) {
            Log.d("MainActivity", "显示 RecyclerView，隐藏 EmptyView")
            binding.emptyView.visibility = android.view.View.GONE
            binding.recyclerView.visibility = android.view.View.VISIBLE
        } else {
            Log.d("MainActivity", "显示 EmptyView，隐藏 RecyclerView")
            binding.emptyView.visibility = android.view.View.VISIBLE
            binding.recyclerView.visibility = android.view.View.GONE
        }
    }

    private fun showPreview() {
        if (images.isEmpty()) {
            Toast.makeText(this, "请先导入图片", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogBinding = com.example.imagestitcher.databinding.DialogPreviewBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        // 关闭按钮
        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        // 生成预览图
        dialogBinding.previewProgressBar.visibility = android.view.View.VISIBLE
        scope.launch {
            val previewBitmap = withContext(Dispatchers.IO) {
                BitmapUtils.stitchImages(images, orientation)
            }

            dialogBinding.previewProgressBar.visibility = android.view.View.GONE
            if (previewBitmap != null) {
                dialogBinding.previewImageView.setImageBitmap(previewBitmap)
            } else {
                Toast.makeText(this@MainActivity, "预览生成失败", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        // 从预览直接保存
        dialogBinding.btnSaveFromPreview.setOnClickListener {
            dialog.dismiss()
            checkPermissionAndSave()
        }

        dialog.show()
    }

    private fun checkPermissionAndSave() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 不需要权限
            saveStitchedImage()
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                saveStitchedImage()
            } else {
                requestPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun saveStitchedImage() {
        if (images.isEmpty()) {
            Toast.makeText(this, "请先导入图片", Toast.LENGTH_SHORT).show()
            return
        }

        // 显示确认对话框
        android.app.AlertDialog.Builder(this)
            .setTitle("保存并删除原图")
            .setMessage("保存拼接图片后，将自动删除手机中的 ${images.size} 张原图片。\n\n是否继续？")
            .setPositiveButton("保存并删除") { _, _ ->
                performSaveAndDelete()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun performSaveAndDelete() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                val stitchedBitmap = BitmapUtils.stitchImages(images, orientation)
                stitchedBitmap?.let { bitmap ->
                    BitmapUtils.saveBitmapToGallery(this@MainActivity, bitmap)
                }
            }

            binding.progressBar.visibility = android.view.View.GONE

            if (result != null) {
                Toast.makeText(this@MainActivity, "图片已保存到相册", Toast.LENGTH_SHORT).show()

                // 保存成功后删除原图并清空列表
                clearAllImages()
            } else {
                Toast.makeText(this@MainActivity, "保存失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearAllImages() {
        Log.d("MainActivity", "清空所有图片并从手机中删除")

        scope.launch {
            // 删除手机中的原图片
            val deletedCount = withContext(Dispatchers.IO) {
                var count = 0

                Log.d("MainActivity", "准备删除 ${images.size} 张图片")

                // 将文档URI转换为媒体URI
                val mediaUris = images.mapNotNull { item ->
                    Log.d("MainActivity", "处理图片 URI: ${item.uri}")
                    val mediaUri = convertToMediaUri(item.uri)
                    if (mediaUri == null) {
                        Log.e("MainActivity", "无法转换 URI: ${item.uri}")
                    }
                    mediaUri
                }

                Log.d("MainActivity", "成功转换 ${mediaUris.size}/${images.size} 个 URI")

                if (mediaUris.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+ 需要请求用户确认删除
                    try {
                        Log.d("MainActivity", "Android 11+: 使用 MediaStore.createDeleteRequest")
                        val pendingIntent = MediaStore.createDeleteRequest(
                            contentResolver,
                            mediaUris
                        )
                        // 启动系统删除确认对话框
                        Log.d("MainActivity", "启动系统删除确认对话框")
                        withContext(Dispatchers.Main) {
                            deleteImagesLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                            )
                        }
                        // 注意：实际删除数量将在回调中处理
                        return@withContext -1 // 特殊标记表示使用了系统对话框
                    } catch (e: Exception) {
                        Log.e("MainActivity", "请求删除权限失败", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "删除请求失败: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                // Android 10及以下，或者Android 11+失败后，尝试直接删除
                Log.d("MainActivity", "Android 10及以下，或创建删除请求失败，尝试直接删除")
                images.forEach { item ->
                    try {
                        val mediaUri = convertToMediaUri(item.uri)
                        if (mediaUri != null) {
                            val deleted = contentResolver.delete(mediaUri, null, null)
                            if (deleted > 0) {
                                count++
                                Log.d("MainActivity", "已删除图片: $mediaUri")
                            } else {
                                Log.w("MainActivity", "无法删除图片 (返回0): $mediaUri")
                            }
                        } else {
                            Log.w("MainActivity", "无法转换URI: ${item.uri}")
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "删除图片失败: ${item.uri}", e)
                    }
                }
                Log.d("MainActivity", "直接删除完成，删除数量: $count")
                count
            }

            // 回收bitmap内存并清空
            images.forEach { item ->
                item.bitmap.recycle()
            }
            images.clear()
            adapter.updateImages(images)
            undoManager.clear()
            updateUI()

            // 显示结果
            if (deletedCount == -1) {
                // 使用了系统对话框，不显示提示
                Log.d("MainActivity", "等待用户在系统对话框中确认删除")
            } else if (deletedCount > 0) {
                Toast.makeText(this@MainActivity, "已删除 $deletedCount 张原图片", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@MainActivity, "原图片无法删除，已清空编辑内容", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun convertToMediaUri(uri: Uri): Uri? {
        return try {
            Log.d("MainActivity", "转换URI: $uri, authority: ${uri.authority}, scheme: ${uri.scheme}")
            val uriPath = uri.path ?: ""

            // 处理 Android 13+ Photo Picker URI
            // 格式: content://media/picker_get_content/0/com.android.providers.media.photopicker/media/1000023734
            if (uri.authority == "media" && uriPath.contains("picker_get_content")) {
                try {
                    // 提取最后的媒体ID
                    val mediaId = uriPath.substringAfterLast("/").toLongOrNull()
                    if (mediaId != null) {
                        val mediaUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            mediaId
                        )
                        Log.d("MainActivity", "Photo Picker URI 转换成功: $mediaUri")
                        return mediaUri
                    } else {
                        Log.w("MainActivity", "无法从 Photo Picker URI 提取媒体ID: $uriPath")
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Photo Picker URI 转换失败", e)
                }
            }

            // 如果是文档URI，转换为媒体URI
            if (uri.authority == "com.android.providers.media.documents") {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                if (split.size >= 2) {
                    val type = split[0]
                    val id = split[1]

                    Log.d("MainActivity", "文档URI - type: $type, id: $id")

                    when (type) {
                        "image" -> {
                            val mediaUri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id.toLong()
                            )
                            Log.d("MainActivity", "文档URI转换成功: $mediaUri")
                            mediaUri
                        }
                        else -> {
                            Log.w("MainActivity", "不支持的文档类型: $type")
                            null
                        }
                    }
                } else {
                    Log.w("MainActivity", "文档ID格式错误: $docId")
                    null
                }
            } else if (uri.authority?.contains("media") == true && uriPath.contains("/external/images/media/")) {
                // 已经是标准媒体URI
                Log.d("MainActivity", "已经是标准媒体URI: $uri")
                uri
            } else if (uri.scheme == "content") {
                // 尝试查询媒体库获取真实的媒体URI
                try {
                    val projection = arrayOf(MediaStore.Images.Media._ID)
                    contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                            val id = cursor.getLong(idColumn)
                            val mediaUri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            )
                            Log.d("MainActivity", "通过查询获取到媒体URI: $mediaUri")
                            mediaUri
                        } else {
                            Log.w("MainActivity", "查询结果为空")
                            null
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "通过查询获取媒体URI失败", e)
                    null
                }
            } else {
                Log.w("MainActivity", "不支持的URI类型: $uri")
                null
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "转换URI失败: $uri", e)
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
