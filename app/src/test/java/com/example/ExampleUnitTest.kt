package com.example

import com.example.data.PlayerPreferencesEntity
import com.example.model.DecoderMode
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun decodingEngines_specificationsAreCorrect() {
    val modes = DecoderMode.entries
    assertEquals(3, modes.size)
    assertEquals(DecoderMode.SW, modes[0])
    assertEquals(DecoderMode.HW, modes[1])
    assertEquals(DecoderMode.HW_PLUS, modes[2])

    // SW and HW volume fixed to 0% - 100% (Standard device music volume)
    assertEquals(100, DecoderMode.SW.maxVolumePercent)
    assertEquals(100, DecoderMode.HW.maxVolumePercent)
    assertEquals(150, DecoderMode.HW_PLUS.maxVolumePercent)

    // Default decoder is SW
    val defaultPrefs = PlayerPreferencesEntity()
    assertEquals("SW", defaultPrefs.defaultDecoderMode)
    assertFalse(defaultPrefs.useHardwareDecoder)
    assertTrue("Call auto-pause feature should be enabled by default", defaultPrefs.pauseOnCall)
  }

  @Test
  fun callAutoPauseFeature_defaultPreferencesAndState() {
    val defaultPrefs = PlayerPreferencesEntity()
    assertTrue(defaultPrefs.pauseOnCall)

    val updatedPrefs = defaultPrefs.copy(pauseOnCall = false)
    assertFalse(updatedPrefs.pauseOnCall)

    val playerState = com.example.player.VTPlayerState()
    assertFalse(playerState.isPausedByCall)

    val pausedByCallState = playerState.copy(isPausedByCall = true)
    assertTrue(pausedByCallState.isPausedByCall)
  }

  @Test
  fun hwMovieQuality_propertiesAndPreferences() {
    val qualities = com.example.model.HwMovieQuality.values()
    assertEquals(4, qualities.size)
    assertEquals(com.example.model.HwMovieQuality.ULTRA, qualities[0])
    assertEquals(com.example.model.HwMovieQuality.HIGH, qualities[1])
    assertEquals(com.example.model.HwMovieQuality.BALANCED, qualities[2])
    assertEquals(com.example.model.HwMovieQuality.SAVER, qualities[3])

    // Verify Ultra allows maximum dimensions and unconstrained bitrate
    assertEquals(Int.MAX_VALUE, com.example.model.HwMovieQuality.ULTRA.maxWidth)
    assertEquals(Int.MAX_VALUE, com.example.model.HwMovieQuality.ULTRA.maxHeight)
    assertEquals(Int.MAX_VALUE, com.example.model.HwMovieQuality.ULTRA.maxBitrate)

    // Verify 1080p constraints
    assertEquals(1920, com.example.model.HwMovieQuality.HIGH.maxWidth)
    assertEquals(1080, com.example.model.HwMovieQuality.HIGH.maxHeight)

    // Verify 720p constraints
    assertEquals(1280, com.example.model.HwMovieQuality.BALANCED.maxWidth)
    assertEquals(720, com.example.model.HwMovieQuality.BALANCED.maxHeight)

    // Verify 480p constraints
    assertEquals(854, com.example.model.HwMovieQuality.SAVER.maxWidth)
    assertEquals(480, com.example.model.HwMovieQuality.SAVER.maxHeight)

    // Default preferences
    val defaultPrefs = PlayerPreferencesEntity()
    assertEquals("ULTRA", defaultPrefs.hwMovieQuality)
    assertEquals("STANDARD", defaultPrefs.movieColorFilter)

    val updatedPrefs = defaultPrefs.copy(
        hwMovieQuality = com.example.model.HwMovieQuality.BALANCED.name,
        movieColorFilter = com.example.model.MovieColorFilter.CINEMA.name
    )
    assertEquals("BALANCED", updatedPrefs.hwMovieQuality)
    assertEquals("CINEMA", updatedPrefs.movieColorFilter)
  }

  @Test
  fun movieColorFilter_properties() {
    val filters = com.example.model.MovieColorFilter.values()
    assertEquals(4, filters.size)
    assertTrue(filters.contains(com.example.model.MovieColorFilter.STANDARD))
    assertTrue(filters.contains(com.example.model.MovieColorFilter.CINEMA))
    assertTrue(filters.contains(com.example.model.MovieColorFilter.VIVID))
    assertTrue(filters.contains(com.example.model.MovieColorFilter.WARM))
  }
}
