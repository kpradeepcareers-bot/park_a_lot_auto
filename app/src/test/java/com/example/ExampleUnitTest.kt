package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testCoordinateRegexExtraction() {
    val coordRegex = Regex("""^(-?\d+(?:\.\d+)?)[,\s]+(-?\d+(?:\.\d+)?)$""")
    val input = "17.4375, 78.3752"
    val match = coordRegex.find(input)
    assertNotNull(match)
    assertEquals("17.4375", match?.groupValues?.get(1))
    assertEquals("78.3752", match?.groupValues?.get(2))
  }

  @Test
  fun testGoogleMapsUrlCoordinateExtraction() {
    val atPattern = Regex("""@(-?\d+\.\d+),(-?\d+\.\d+)""")
    val url = "https://www.google.com/maps/place/Cyber+Towers/@17.4504,78.3808,17z/data=!3m1!4b1"
    val match = atPattern.find(url)
    assertNotNull(match)
    assertEquals("17.4504", match?.groupValues?.get(1))
    assertEquals("78.3808", match?.groupValues?.get(2))

    val placeNameRegex = Regex("""/place/([^/@?]+)""")
    val placeMatch = placeNameRegex.find(url)
    assertNotNull(placeMatch)
    val decoded = java.net.URLDecoder.decode(placeMatch?.groupValues?.get(1)?.replace("+", " ") ?: "", "UTF-8")
    assertEquals("Cyber Towers", decoded)
  }
}

