package com.example.filecategorizer

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.filecategorizer.databinding.ActivityCategoryDetailBinding

class CategoryDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_FILE_PATHS = "extra_file_paths"
        const val EXTRA_FILE_NAMES = "extra_file_names"
        const val EXTRA_FILE_SIZES = "extra_file_sizes"
        const val EXTRA_FILE_MIMES = "extra_file_mimes"
    }

    private lateinit var binding: ActivityCategoryDetailBinding
    private lateinit var fileAdapter: FileAdapter
    private var allFiles: List<SimpleFileItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val categoryName = intent.getStringExtra(EXTRA_CATEGORY) ?: "Files"
        val paths = intent.getStringArrayListExtra(EXTRA_FILE_PATHS) ?: arrayListOf()
        val names = intent.getStringArrayListExtra(EXTRA_FILE_NAMES) ?: arrayListOf()
        val sizes = intent.getLongArrayExtra(EXTRA_FILE_SIZES) ?: LongArray(0)
        val mimes = intent.getStringArrayListExtra(EXTRA_FILE_MIMES) ?: arrayListOf()

        val category = try { FileCategory.valueOf(categoryName) } catch (e: Exception) { null }
        supportActionBar?.title = category?.displayName ?: categoryName

        allFiles = paths.indices.map { i ->
            SimpleFileItem(
                name = names.getOrElse(i) { "Unknown" },
                path = paths[i],
                size = sizes.getOrElse(i) { 0L },
                mimeType = mimes.getOrElse(i) { "" }
            )
        }

        binding.tvFileCount.text = "${allFiles.size} file${if (allFiles.size != 1) "s" else ""}"
        binding.tvTotalSize.text = FileCategorizer.formatSize(allFiles.sumOf { it.size })

        fileAdapter = FileAdapter(allFiles)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@CategoryDetailActivity)
            adapter = fileAdapter
        }

        // Search / filter
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.lowercase() ?: ""
                val filtered = if (query.isEmpty()) allFiles
                else allFiles.filter { it.name.lowercase().contains(query) || it.path.lowercase().contains(query) }
                fileAdapter = FileAdapter(filtered)
                binding.recyclerView.adapter = fileAdapter
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Sort toggle
        var sortBySize = false
        binding.btnSort.setOnClickListener {
            sortBySize = !sortBySize
            binding.btnSort.text = if (sortBySize) "Sort: Size ↓" else "Sort: Name ↑"
            val sorted = if (sortBySize) allFiles.sortedByDescending { it.size }
            else allFiles.sortedBy { it.name.lowercase() }
            fileAdapter = FileAdapter(sorted)
            binding.recyclerView.adapter = fileAdapter
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
