package com.example.model

data class VideoItem(
    val id: Long,
    val title: String,
    val uriString: String,
    val duration: Long = 0L,
    val size: Long = 0L,
    val resolution: String = "1080p",
    val folderName: String = "Internal",
    val dateModified: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val lastPosition: Long = 0L
) {
    val formattedDuration: String
        get() {
            val totalSecs = duration / 1000
            val hours = totalSecs / 3600
            val minutes = (totalSecs % 3600) / 60
            val seconds = totalSecs % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            if (size <= 0) return "--"
            val mb = size.toDouble() / (1024 * 1024)
            return if (mb >= 1024) {
                String.format("%.2f GB", mb / 1024)
            } else {
                String.format("%.1f MB", mb)
            }
        }
}

data class FolderItem(
    val name: String,
    val videoCount: Int,
    val totalDuration: Long,
    val sampleVideo: VideoItem?
)

enum class AspectRatioMode(val label: String) {
    FIT("Fit to Screen"),
    STRETCH("Stretch"),
    CROP_ZOOM("Crop / Zoom"),
    RATIO_16_9("16:9 Cinema"),
    RATIO_4_3("4:3 Standard"),
    RATIO_1_1("1:1 Square")
}

enum class DecoderMode(val code: String, val label: String, val maxVolumePercent: Int) {
    HW("HW", "Hardware Decoder", 100),
    HW_PLUS("HW+", "Hardware+ (150% Volume)", 150),
    SW("SW", "Software Decoder (150% Volume)", 150)
}

data class TrackItem(
    val id: String,
    val label: String,
    val language: String,
    val isSelected: Boolean
)

data class StreamPreset(
    val title: String,
    val url: String,
    val description: String,
    val tag: String
)

enum class SortOrder(val displayName: String) {
    DATE_DESC("Date (Newest first)"),
    DATE_ASC("Date (Oldest first)"),
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    SIZE_DESC("Size (Largest first)"),
    DURATION_DESC("Duration (Longest first)")
}
