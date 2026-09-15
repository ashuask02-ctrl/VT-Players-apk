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

    // 2. SW and HW volume fixed to 0% - 100%
    assertEquals(100, DecoderMode.SW.maxVolumePercent)
    assertEquals(100, DecoderMode.HW.maxVolumePercent)

    // 3. HW+ volume fixed to 101% - 200% (+100% Audio Boost)
    assertEquals(200, DecoderMode.HW_PLUS.maxVolumePercent)

    // 4. Default decoder is SW
    val defaultPrefs = PlayerPreferencesEntity()
    assertEquals("SW", defaultPrefs.defaultDecoderMode)
    assertFalse(defaultPrefs.useHardwareDecoder)
  }
}
