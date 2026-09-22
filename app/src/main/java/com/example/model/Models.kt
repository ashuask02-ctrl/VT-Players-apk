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

    val formattedLastPosition: String
        get() {
            val totalSecs = lastPosition / 1000
            val hours = totalSecs / 3600
            val minutes = (totalSecs % 3600) / 60
            val seconds = totalSecs % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val hasResumeProgress: Boolean
        get() = lastPosition > 1000L && (duration <= 0L || lastPosition < (duration - 3000L))
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

enum class DecoderMode(
    val code: String,
    val label: String,
    val description: String,
    val minVolumePercent: Int = 0,
    val maxVolumePercent: Int = 100
) {
    SW("SW", "SW Decoder", "Software decoding", 0, 100),
    HW("HW", "HW Decoder", "Hardware decoding", 0, 100),
    HW_PLUS("HW+", "HW+ Decoder", "Hardware decoder + MediaCodec pipeline & 150% Audio Boost", 0, 150)
}

enum class AudioDecoderMode(
    val code: String,
    val label: String,
    val description: String
) {
    AUTO("Auto", "System MediaCodec (Auto)", "Hardware audio decoder with platform DSP acceleration"),
    SW("SW", "Software (FFmpeg Mode)", "Software audio decoder for AC3/DTS/AAC/FLAC maximum compatibility")
}

enum class EqualizerPreset(
    val displayName: String,
    val bassBoostDb: Int,
    val midDb: Int,
    val trebleDb: Int
) {
    FLAT("Flat", 0, 0, 0),
    BASS_BOOST("Bass Boost", 8, 2, 0),
    VOCAL("Vocal", -2, 6, 2),
    ROCK("Rock", 6, -1, 5),
    POP("Pop", 4, 3, 2),
    CLASSICAL("Classical", 5, 2, 4),
    CUSTOM("Custom", 0, 0, 0)
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

enum class HwMovieQuality(
    val code: String,
    val title: String,
    val description: String,
    val badgeLabel: String,
    val maxWidth: Int,
    val maxHeight: Int,
    val maxBitrate: Int
) {
    ULTRA(
        code = "ULTRA",
        title = "Ultra (Original / 4K Direct HW)",
        description = "Direct hardware surface buffer, native bitstream, uncompressed up to 4K 60fps.",
        badgeLabel = "HQ: 4K",
        maxWidth = Int.MAX_VALUE,
        maxHeight = Int.MAX_VALUE,
        maxBitrate = Int.MAX_VALUE
    ),
    HIGH(
        code = "1080P",
        title = "Full HD (1080p)",
        description = "Hardware accelerated 1080p decoding optimized for clarity and fluid motion.",
        badgeLabel = "1080p",
        maxWidth = 1920,
        maxHeight = 1080,
        maxBitrate = 15_000_000
    ),
    BALANCED(
        code = "720P",
        title = "HD Balanced (720p)",
        description = "Balanced 720p hardware decoding. Smooth playback with lower power and heat.",
        badgeLabel = "720p",
        maxWidth = 1280,
        maxHeight = 720,
        maxBitrate = 6_000_000
    ),
    SAVER(
        code = "480P",
        title = "Battery Saver (480p SD)",
        description = "Power-saving hardware decoding capped at 480p SD for maximum battery life.",
        badgeLabel = "480p",
        maxWidth = 854,
        maxHeight = 480,
        maxBitrate = 2_500_000
    )
}

enum class MovieColorFilter(
    val code: String,
    val title: String,
    val description: String
) {
    STANDARD("STANDARD", "Natural / Standard", "True original colors without post-processing"),
    CINEMA("CINEMA", "Cinema Movie", "Warm tones and boosted contrast for cinematic immersion"),
    VIVID("VIVID", "Vivid Pop", "High saturation and vibrant contrast for animated/HDR content"),
    WARM("WARM", "Warm Night", "Gentle amber tone with reduced blue light for eye comfort")
}
