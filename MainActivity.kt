package com.example.filecategorizer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.filecategorizer.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var categoryAdapter: CategoryAdapter

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            viewModel.startScan()
        } else {
            Snackbar.make(binding.root, "Storage permission required to scan files", Snackbar.LENGTH_LONG).show()
            showEmptyState("Permission denied.\nGrant storage permission in Settings to use this app.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        categoryAdapter = CategoryAdapter(emptyList())
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = categoryAdapter
        }

        binding.btnScan.setOnClickListener { checkPermissionsAndScan() }
        binding.btnRescan.setOnClickListener { checkPermissionsAndScan() }

        viewModel.scanState.observe(this) { state ->
            when (state) {
                is ScanState.Idle -> showIdleState()
                is ScanState.Scanning -> showScanningState(state.count, state.currentFile)
                is ScanState.Done -> showDoneState(state.categories, state.totalFiles)
                is ScanState.Error -> showErrorState(state.message)
            }
        }
    }

    private fun checkPermissionsAndScan() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            viewModel.startScan()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    private fun showIdleState() {
        binding.layoutWelcome.visibility = View.VISIBLE
        binding.layoutScanning.visibility = View.GONE
        binding.layoutResults.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
    }

    private fun showScanningState(count: Int, fileName: String) {
        binding.layoutWelcome.visibility = View.GONE
        binding.layoutScanning.visibility = View.VISIBLE
        binding.layoutResults.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
        binding.tvScanCount.text = "Scanned $count files..."
        binding.tvCurrentFile.text = fileName
    }

    private fun showDoneState(categories: List<CategorySummary>, totalFiles: Int) {
        binding.layoutWelcome.visibility = View.GONE
        binding.layoutScanning.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
        binding.layoutResults.visibility = View.VISIBLE

        binding.tvSummary.text = "Found $totalFiles files in ${categories.size} categories"
        categoryAdapter.updateData(categories)
    }

    private fun showErrorState(message: String) {
        showEmptyState("Error: $message")
    }

    private fun showEmptyState(message: String) {
        binding.layoutWelcome.visibility = View.GONE
        binding.layoutScanning.visibility = View.GONE
        binding.layoutResults.visibility = View.GONE
        binding.layoutEmpty.visibility = View.VISIBLE
        binding.tvEmptyMessage.text = message
    }
}
