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
    // 1. Only SW and HW decoders (no HW+)
    val modes = DecoderMode.entries
    assertEquals(2, modes.size)
    assertEquals(DecoderMode.SW, modes[0])
    assertEquals(DecoderMode.HW, modes[1])

    // 2. SW and HW volume fixed to 0% - 100% (Standard device music volume)
    assertEquals(100, DecoderMode.SW.maxVolumePercent)
    assertEquals(100, DecoderMode.HW.maxVolumePercent)

    // 3. Default decoder is SW
    val defaultPrefs = PlayerPreferencesEntity()
    assertEquals("SW", defaultPrefs.defaultDecoderMode)
    assertFalse(defaultPrefs.useHardwareDecoder)
  }
}
