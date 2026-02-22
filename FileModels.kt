package com.example.filecategorizer

data class FileItem(
    val name: String,
    val path: String,
    val size: Long,
    val category: FileCategory,
    val detectedMimeType: String,
    val lastModified: Long
)

enum class FileCategory(val displayName: String, val emoji: String, val color: Int) {
    IMAGE("Images", "🖼️", 0xFF4CAF50.toInt()),
    VIDEO("Videos", "🎬", 0xFF2196F3.toInt()),
    AUDIO("Audio", "🎵", 0xFF9C27B0.toInt()),
    DOCUMENT("Documents", "📄", 0xFFFF9800.toInt()),
    SPREADSHEET("Spreadsheets", "📊", 0xFF009688.toInt()),
    PRESENTATION("Presentations", "📋", 0xFFE91E63.toInt()),
    ARCHIVE("Archives", "🗜️", 0xFF795548.toInt()),
    CODE("Code / Scripts", "💻", 0xFF607D8B.toInt()),
    FONT("Fonts", "🔤", 0xFF00BCD4.toInt()),
    DATABASE("Databases", "🗄️", 0xFFFF5722.toInt()),
    EBOOK("eBooks", "📚", 0xFF8BC34A.toInt()),
    UNKNOWN("Other Files", "📁", 0xFF9E9E9E.toInt())
}

data class CategorySummary(
    val category: FileCategory,
    val files: List<FileItem>,
    val totalSize: Long
)
