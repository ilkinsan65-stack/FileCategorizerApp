package com.example.filecategorizer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class SimpleFileItem(
    val name: String,
    val path: String,
    val size: Long,
    val mimeType: String
)

class FileAdapter(
    private var files: List<SimpleFileItem>
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    inner class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvFileName)
        val tvPath: TextView = view.findViewById(R.id.tvFilePath)
        val tvSize: TextView = view.findViewById(R.id.tvFileSize)
        val tvMime: TextView = view.findViewById(R.id.tvFileMime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]
        holder.tvName.text = file.name
        holder.tvPath.text = file.path
        holder.tvSize.text = FileCategorizer.formatSize(file.size)
        holder.tvMime.text = file.mimeType
    }

    override fun getItemCount() = files.size

    fun filter(query: String) {
        // Filtering is handled by the Activity
    }
}
