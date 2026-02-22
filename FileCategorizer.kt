package com.example.filecategorizer

import android.content.Context
import android.os.Build
import android.os.Environment
import android.webkit.MimeTypeMap
import java.io.File
import java.io.InputStream

object FileCategorizer {

    // Magic bytes signatures for content-based detection
    private val MAGIC_SIGNATURES: List<Triple<ByteArray, String, FileCategory>> = listOf(
        // Images
        Triple(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()), "image/jpeg", FileCategory.IMAGE),
        Triple(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A), "image/png", FileCategory.IMAGE),
        Triple(byteArrayOf(0x47, 0x49, 0x46, 0x38), "image/gif", FileCategory.IMAGE),
        Triple(byteArrayOf(0x42, 0x4D), "image/bmp", FileCategory.IMAGE),
        Triple(byteArrayOf(0x49, 0x49, 0x2A, 0x00), "image/tiff", FileCategory.IMAGE),
        Triple(byteArrayOf(0x4D, 0x4D, 0x00, 0x2A), "image/tiff", FileCategory.IMAGE),
        Triple(byteArrayOf(0x52, 0x49, 0x46, 0x46), "image/webp", FileCategory.IMAGE), // RIFF (WebP)
        Triple(byteArrayOf(0x00, 0x00, 0x00, 0x0C, 0x6A, 0x50, 0x20, 0x20), "image/jp2", FileCategory.IMAGE),

        // Video
        Triple(byteArrayOf(0x00, 0x00, 0x00, 0x14, 0x66, 0x74, 0x79, 0x70), "video/mp4", FileCategory.VIDEO),
        Triple(byteArrayOf(0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70), "video/mp4", FileCategory.VIDEO),
        Triple(byteArrayOf(0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70), "video/mp4", FileCategory.VIDEO),
        Triple(byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte()), "video/webm", FileCategory.VIDEO), // MKV/WebM
        Triple(byteArrayOf(0x52, 0x49, 0x46, 0x46), "video/avi", FileCategory.VIDEO), // RIFF (AVI) - needs ext check
        Triple(byteArrayOf(0x30, 0x26, 0xB2.toByte(), 0x75, 0x8E.toByte(), 0x66, 0xCF.toByte(), 0x11), "video/x-ms-wmv", FileCategory.VIDEO),

        // Audio
        Triple(byteArrayOf(0x49, 0x44, 0x33), "audio/mpeg", FileCategory.AUDIO), // MP3 ID3
        Triple(byteArrayOf(0xFF.toByte(), 0xFB.toByte()), "audio/mpeg", FileCategory.AUDIO), // MP3
        Triple(byteArrayOf(0xFF.toByte(), 0xF3.toByte()), "audio/mpeg", FileCategory.AUDIO),
        Triple(byteArrayOf(0xFF.toByte(), 0xF2.toByte()), "audio/mpeg", FileCategory.AUDIO),
        Triple(byteArrayOf(0x66, 0x4C, 0x61, 0x43), "audio/flac", FileCategory.AUDIO),
        Triple(byteArrayOf(0x4F, 0x67, 0x67, 0x53), "audio/ogg", FileCategory.AUDIO),
        Triple(byteArrayOf(0x52, 0x49, 0x46, 0x46), "audio/wav", FileCategory.AUDIO), // RIFF (WAV) - needs ext
        Triple(byteArrayOf(0x4D, 0x34, 0x41, 0x20), "audio/m4a", FileCategory.AUDIO),

        // Documents
        Triple(byteArrayOf(0x25, 0x50, 0x44, 0x46), "application/pdf", FileCategory.DOCUMENT), // %PDF
        Triple(byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte()), "application/msword", FileCategory.DOCUMENT), // OLE (DOC, XLS, PPT)
        Triple(byteArrayOf(0x7B, 0x5C, 0x72, 0x74, 0x66), "application/rtf", FileCategory.DOCUMENT), // RTF {\rtf

        // Archives
        Triple(byteArrayOf(0x50, 0x4B, 0x03, 0x04), "application/zip", FileCategory.ARCHIVE), // PK (ZIP, DOCX, XLSX, APK, JAR)
        Triple(byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07), "application/x-rar", FileCategory.ARCHIVE), // RAR
        Triple(byteArrayOf(0x1F.toByte(), 0x8B.toByte()), "application/gzip", FileCategory.ARCHIVE), // GZ
        Triple(byteArrayOf(0x42, 0x5A, 0x68), "application/x-bzip2", FileCategory.ARCHIVE), // BZ2
        Triple(byteArrayOf(0x37, 0x7A, 0xBC.toByte(), 0xAF.toByte(), 0x27, 0x1C), "application/x-7z-compressed", FileCategory.ARCHIVE), // 7Z
        Triple(byteArrayOf(0x75, 0x73, 0x74, 0x61, 0x72), "application/x-tar", FileCategory.ARCHIVE), // TAR

        // Fonts
        Triple(byteArrayOf(0x00, 0x01, 0x00, 0x00), "font/ttf", FileCategory.FONT),
        Triple(byteArrayOf(0x4F, 0x54, 0x54, 0x4F), "font/otf", FileCategory.FONT),
        Triple(byteArrayOf(0x77, 0x4F, 0x46, 0x46), "font/woff", FileCategory.FONT),
        Triple(byteArrayOf(0x77, 0x4F, 0x46, 0x32), "font/woff2", FileCategory.FONT),

        // Databases
        Triple(byteArrayOf(0x53, 0x51, 0x4C, 0x69, 0x74, 0x65, 0x20, 0x66), "application/x-sqlite3", FileCategory.DATABASE),
    )

    // Extension-based fallback map
    private val EXTENSION_MAP: Map<String, FileCategory> = mapOf(
        // Images
        "jpg" to FileCategory.IMAGE, "jpeg" to FileCategory.IMAGE, "png" to FileCategory.IMAGE,
        "gif" to FileCategory.IMAGE, "bmp" to FileCategory.IMAGE, "webp" to FileCategory.IMAGE,
        "svg" to FileCategory.IMAGE, "heic" to FileCategory.IMAGE, "heif" to FileCategory.IMAGE,
        "ico" to FileCategory.IMAGE, "tiff" to FileCategory.IMAGE, "tif" to FileCategory.IMAGE,
        "raw" to FileCategory.IMAGE, "cr2" to FileCategory.IMAGE, "dng" to FileCategory.IMAGE,

        // Video
        "mp4" to FileCategory.VIDEO, "mkv" to FileCategory.VIDEO, "avi" to FileCategory.VIDEO,
        "mov" to FileCategory.VIDEO, "wmv" to FileCategory.VIDEO, "flv" to FileCategory.VIDEO,
        "webm" to FileCategory.VIDEO, "3gp" to FileCategory.VIDEO, "m4v" to FileCategory.VIDEO,
        "ts" to FileCategory.VIDEO, "vob" to FileCategory.VIDEO,

        // Audio
        "mp3" to FileCategory.AUDIO, "flac" to FileCategory.AUDIO, "wav" to FileCategory.AUDIO,
        "aac" to FileCategory.AUDIO, "ogg" to FileCategory.AUDIO, "m4a" to FileCategory.AUDIO,
        "opus" to FileCategory.AUDIO, "wma" to FileCategory.AUDIO, "aiff" to FileCategory.AUDIO,
        "mid" to FileCategory.AUDIO, "midi" to FileCategory.AUDIO,

        // Documents
        "pdf" to FileCategory.DOCUMENT, "doc" to FileCategory.DOCUMENT, "docx" to FileCategory.DOCUMENT,
        "txt" to FileCategory.DOCUMENT, "rtf" to FileCategory.DOCUMENT, "odt" to FileCategory.DOCUMENT,
        "pages" to FileCategory.DOCUMENT, "md" to FileCategory.DOCUMENT,

        // Spreadsheets
        "xls" to FileCategory.SPREADSHEET, "xlsx" to FileCategory.SPREADSHEET,
        "csv" to FileCategory.SPREADSHEET, "ods" to FileCategory.SPREADSHEET,
        "numbers" to FileCategory.SPREADSHEET, "tsv" to FileCategory.SPREADSHEET,

        // Presentations
        "ppt" to FileCategory.PRESENTATION, "pptx" to FileCategory.PRESENTATION,
        "odp" to FileCategory.PRESENTATION, "key" to FileCategory.PRESENTATION,

        // Archives
        "zip" to FileCategory.ARCHIVE, "rar" to FileCategory.ARCHIVE, "7z" to FileCategory.ARCHIVE,
        "tar" to FileCategory.ARCHIVE, "gz" to FileCategory.ARCHIVE, "bz2" to FileCategory.ARCHIVE,
        "xz" to FileCategory.ARCHIVE, "apk" to FileCategory.ARCHIVE, "jar" to FileCategory.ARCHIVE,

        // Code / Scripts
        "kt" to FileCategory.CODE, "java" to FileCategory.CODE, "py" to FileCategory.CODE,
        "js" to FileCategory.CODE, "ts" to FileCategory.CODE, "html" to FileCategory.CODE,
        "css" to FileCategory.CODE, "xml" to FileCategory.CODE, "json" to FileCategory.CODE,
        "c" to FileCategory.CODE, "cpp" to FileCategory.CODE, "h" to FileCategory.CODE,
        "sh" to FileCategory.CODE, "bat" to FileCategory.CODE, "php" to FileCategory.CODE,
        "rb" to FileCategory.CODE, "go" to FileCategory.CODE, "rs" to FileCategory.CODE,
        "swift" to FileCategory.CODE, "dart" to FileCategory.CODE, "sql" to FileCategory.CODE,
        "yaml" to FileCategory.CODE, "yml" to FileCategory.CODE, "toml" to FileCategory.CODE,

        // Fonts
        "ttf" to FileCategory.FONT, "otf" to FileCategory.FONT,
        "woff" to FileCategory.FONT, "woff2" to FileCategory.FONT,

        // Databases
        "db" to FileCategory.DATABASE, "sqlite" to FileCategory.DATABASE,
        "sqlite3" to FileCategory.DATABASE, "realm" to FileCategory.DATABASE,

        // eBooks
        "epub" to FileCategory.EBOOK, "mobi" to FileCategory.EBOOK,
        "azw" to FileCategory.EBOOK, "azw3" to FileCategory.EBOOK, "fb2" to FileCategory.EBOOK
    )

    /**
     * Read the first N bytes of a file for magic byte detection
     */
    private fun readMagicBytes(file: File, count: Int = 12): ByteArray? {
        return try {
            file.inputStream().use { stream ->
                val bytes = ByteArray(count)
                val read = stream.read(bytes)
                if (read > 0) bytes.copyOf(read) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if byte array starts with the given signature
     */
    private fun ByteArray.startsWith(signature: ByteArray): Boolean {
        if (this.size < signature.size) return false
        for (i in signature.indices) {
            if (this[i] != signature[i]) return false
        }
        return true
    }

    /**
     * Detect category from raw file bytes (magic bytes)
     */
    private fun detectFromMagicBytes(file: File): Pair<String, FileCategory>? {
        val bytes = readMagicBytes(file) ?: return null
        val ext = file.extension.lowercase()

        for ((signature, mimeType, category) in MAGIC_SIGNATURES) {
            if (bytes.startsWith(signature)) {
                // Special case: RIFF header is shared by WAV, AVI, WEBP
                if (signature.contentEquals(byteArrayOf(0x52, 0x49, 0x46, 0x46))) {
                    return when (ext) {
                        "avi" -> Pair("video/avi", FileCategory.VIDEO)
                        "wav" -> Pair("audio/wav", FileCategory.AUDIO)
                        "webp" -> Pair("image/webp", FileCategory.IMAGE)
                        else -> Pair(mimeType, category)
                    }
                }
                // Special case: PK header is shared by ZIP, DOCX, XLSX, PPTX, APK, JAR
                if (signature.contentEquals(byteArrayOf(0x50, 0x4B, 0x03, 0x04))) {
                    return when (ext) {
                        "docx" -> Pair("application/vnd.openxmlformats-officedocument.wordprocessingml.document", FileCategory.DOCUMENT)
                        "xlsx" -> Pair("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", FileCategory.SPREADSHEET)
                        "pptx" -> Pair("application/vnd.openxmlformats-officedocument.presentationml.presentation", FileCategory.PRESENTATION)
                        "apk" -> Pair("application/vnd.android.package-archive", FileCategory.ARCHIVE)
                        "epub" -> Pair("application/epub+zip", FileCategory.EBOOK)
                        else -> Pair("application/zip", FileCategory.ARCHIVE)
                    }
                }
                // Special case: OLE compound (DOC, XLS, PPT)
                if (signature.contentEquals(byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte()))) {
                    return when (ext) {
                        "xls" -> Pair("application/vnd.ms-excel", FileCategory.SPREADSHEET)
                        "ppt" -> Pair("application/vnd.ms-powerpoint", FileCategory.PRESENTATION)
                        else -> Pair("application/msword", FileCategory.DOCUMENT)
                    }
                }
                return Pair(mimeType, category)
            }
        }
        return null
    }

    /**
     * Categorize a single file using content-first, then extension-based approach
     */
    fun categorizeFile(file: File): FileItem {
        // 1. Try magic bytes first (content-based)
        val magicResult = detectFromMagicBytes(file)
        if (magicResult != null) {
            return FileItem(
                name = file.name,
                path = file.absolutePath,
                size = file.length(),
                category = magicResult.second,
                detectedMimeType = magicResult.first,
                lastModified = file.lastModified()
            )
        }

        // 2. Extension fallback
        val ext = file.extension.lowercase()
        val extCategory = EXTENSION_MAP[ext]
        if (extCategory != null) {
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
            return FileItem(
                name = file.name,
                path = file.absolutePath,
                size = file.length(),
                category = extCategory,
                detectedMimeType = mime,
                lastModified = file.lastModified()
            )
        }

        // 3. Unknown
        return FileItem(
            name = file.name,
            path = file.absolutePath,
            size = file.length(),
            category = FileCategory.UNKNOWN,
            detectedMimeType = "application/octet-stream",
            lastModified = file.lastModified()
        )
    }

    /**
     * Recursively scan a directory and return categorized files
     */
    fun scanDirectory(
        directory: File,
        onProgress: (scanned: Int, current: String) -> Unit = { _, _ -> }
    ): List<FileItem> {
        val results = mutableListOf<FileItem>()
        var count = 0

        fun scan(dir: File) {
            val files = try { dir.listFiles() } catch (e: SecurityException) { null } ?: return
            for (file in files) {
                if (file.isDirectory) {
                    // Skip hidden/system directories
                    if (!file.name.startsWith(".") && !SKIP_DIRS.contains(file.name.lowercase())) {
                        scan(file)
                    }
                } else if (file.isFile && file.length() > 0) {
                    count++
                    onProgress(count, file.name)
                    results.add(categorizeFile(file))
                }
            }
        }

        scan(directory)
        return results
    }

    /**
     * Group file items into category summaries
     */
    fun groupByCategory(files: List<FileItem>): List<CategorySummary> {
        return files.groupBy { it.category }
            .map { (category, items) ->
                CategorySummary(
                    category = category,
                    files = items.sortedByDescending { it.size },
                    totalSize = items.sumOf { it.size }
                )
            }
            .sortedByDescending { it.files.size }
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824L -> "%.2f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576L -> "%.2f MB".format(bytes / 1_048_576.0)
            bytes >= 1_024L -> "%.2f KB".format(bytes / 1_024.0)
            else -> "$bytes B"
        }
    }

    private val SKIP_DIRS = setOf("proc", "sys", "dev", "acct", "config", "d", "vendor")
}
