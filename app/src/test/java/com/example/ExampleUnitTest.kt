package com.example

import com.example.util.DateTimeUtils
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testDurationFormatting() {
    assertEquals("45m", DateTimeUtils.formatDurationMinutes(45))
    assertEquals("1h", DateTimeUtils.formatDurationMinutes(60))
    assertEquals("1h 30m", DateTimeUtils.formatDurationMinutes(90))
    assertEquals("00:45", DateTimeUtils.formatDurationSeconds(45))
    assertEquals("01:15", DateTimeUtils.formatDurationSeconds(75))
    assertEquals("01:00:00", DateTimeUtils.formatDurationSeconds(3600))
  }

  @Test
  fun testIsoDateString() {
    val today = DateTimeUtils.getTodayIsoString()
    assertTrue(today.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
  }
}

