package com.example.filecategorizer

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class CategoryAdapter(
    private var categories: List<CategorySummary>
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.card)
        val tvEmoji: TextView = view.findViewById(R.id.tvEmoji)
        val tvName: TextView = view.findViewById(R.id.tvCategoryName)
        val tvCount: TextView = view.findViewById(R.id.tvFileCount)
        val tvSize: TextView = view.findViewById(R.id.tvTotalSize)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val summary = categories[position]
        holder.tvEmoji.text = summary.category.emoji
        holder.tvName.text = summary.category.displayName
        holder.tvCount.text = "${summary.files.size} file${if (summary.files.size != 1) "s" else ""}"
        holder.tvSize.text = FileCategorizer.formatSize(summary.totalSize)
        holder.card.strokeColor = summary.category.color
        holder.card.setOnClickListener {
            val context = it.context
            val intent = Intent(context, CategoryDetailActivity::class.java).apply {
                putExtra(CategoryDetailActivity.EXTRA_CATEGORY, summary.category.name)
                putStringArrayListExtra(
                    CategoryDetailActivity.EXTRA_FILE_PATHS,
                    ArrayList(summary.files.map { f -> f.path })
                )
                putStringArrayListExtra(
                    CategoryDetailActivity.EXTRA_FILE_NAMES,
                    ArrayList(summary.files.map { f -> f.name })
                )
                putLongArrayExtra(
                    CategoryDetailActivity.EXTRA_FILE_SIZES,
                    summary.files.map { f -> f.size }.toLongArray()
                )
                putStringArrayListExtra(
                    CategoryDetailActivity.EXTRA_FILE_MIMES,
                    ArrayList(summary.files.map { f -> f.detectedMimeType })
                )
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = categories.size

    fun updateData(newCategories: List<CategorySummary>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}
