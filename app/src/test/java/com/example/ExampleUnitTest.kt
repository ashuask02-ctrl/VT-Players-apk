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
  fun threeDecodingEngines_specificationsAreCorrect() {
    // 1. Sequence SW, HW, HW+
    val modes = DecoderMode.entries
    assertEquals(DecoderMode.SW, modes[0])
    assertEquals(DecoderMode.HW, modes[1])
    assertEquals(DecoderMode.HW_PLUS, modes[2])

    // 2. SW, HW, and HW+ have standard volume 0% - 100% and Audio Boost up to 120% (+20 dB)
    assertEquals(120, DecoderMode.SW.maxVolumePercent)
    assertEquals(120, DecoderMode.HW.maxVolumePercent)
    assertEquals(120, DecoderMode.HW_PLUS.maxVolumePercent)

    // 4. Default decoder is SW
    val defaultPrefs = PlayerPreferencesEntity()
    assertEquals("SW", defaultPrefs.defaultDecoderMode)
    assertFalse(defaultPrefs.useHardwareDecoder)
  }
}
