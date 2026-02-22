package com.example.filecategorizer

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ScanState {
    object Idle : ScanState()
    data class Scanning(val count: Int, val currentFile: String) : ScanState()
    data class Done(val categories: List<CategorySummary>, val totalFiles: Int) : ScanState()
    data class Error(val message: String) : ScanState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _scanState = MutableLiveData<ScanState>(ScanState.Idle)
    val scanState: LiveData<ScanState> = _scanState

    fun startScan() {
        viewModelScope.launch {
            _scanState.value = ScanState.Scanning(0, "Starting...")
            try {
                val result = withContext(Dispatchers.IO) {
                    val root = Environment.getExternalStorageDirectory()
                    FileCategorizer.scanDirectory(root) { count, name ->
                        // Post progress updates (throttled)
                        if (count % 10 == 0) {
                            _scanState.postValue(ScanState.Scanning(count, name))
                        }
                    }
                }
                val categories = FileCategorizer.groupByCategory(result)
                _scanState.value = ScanState.Done(categories, result.size)
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
}
