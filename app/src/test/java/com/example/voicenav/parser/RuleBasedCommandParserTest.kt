package com.example.voicenav.parser

import com.example.voicenav.core.model.NavCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleBasedCommandParserTest {

    private val parser = RuleBasedCommandParser()

    @Test
    fun testNavigateToShaniwarWada() {
        val result = parser.parse("Take me to Shaniwar Wada")
        assertTrue(result is NavCommand.NavigateTo)
        assertEquals("Shaniwar Wada", (result as NavCommand.NavigateTo).destination)
    }

    @Test
    fun testNavigateToWithPunctuation() {
        val result = parser.parse("take me to pune railway station?")
        assertTrue(result is NavCommand.NavigateTo)
        assertEquals("Pune Railway Station", (result as NavCommand.NavigateTo).destination)
    }

    @Test
    fun testNearestPetrolPump() {
        val result = parser.parse("Nearest petrol pump")
        assertTrue(result is NavCommand.FindNearest)
        assertEquals("petrol pump", (result as NavCommand.FindNearest).poiType)
    }

    @Test
    fun testHowFar() {
        val result = parser.parse("How far?")
        assertEquals(NavCommand.HowFar, result)
    }

    @Test
    fun testNextTurn() {
        val result = parser.parse("Next turn")
        assertEquals(NavCommand.NextTurn, result)
    }

    @Test
    fun testRepeat() {
        val result = parser.parse("Repeat")
        assertEquals(NavCommand.Repeat, result)
    }

    @Test
    fun testReroute() {
        val result = parser.parse("Reroute")
        assertEquals(NavCommand.Reroute, result)
    }

    @Test
    fun testStopNavigation() {
        val result = parser.parse("Stop navigation")
        assertEquals(NavCommand.StopNavigation, result)
    }
}
