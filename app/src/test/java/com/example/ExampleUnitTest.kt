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
  }
}
