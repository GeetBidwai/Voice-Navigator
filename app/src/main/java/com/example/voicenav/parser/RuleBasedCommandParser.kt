package com.example.voicenav.parser

import com.example.voicenav.core.model.NavCommand
import java.util.Locale

/**
 * Deterministic, rule-based voice command parser.
 * Strictly avoids LLMs or AI inference to guarantee predictable, zero-latency parsing
 * for motorcycle riders in loud helmet environments.
 */
class RuleBasedCommandParser {

    fun parse(rawInput: String): NavCommand {
        val cleaned = rawInput.trim().lowercase(Locale.ROOT)
            .replace(Regex("[?!.,']"), "")
            .trim()

        if (cleaned.isEmpty()) {
            return NavCommand.Unknown("")
        }

        // 1. Stop Navigation rules
        if (matchesStopNavigation(cleaned)) {
            return NavCommand.StopNavigation
        }

        // 2. Repeat rules
        if (matchesRepeat(cleaned)) {
            return NavCommand.Repeat
        }

        // 3. How far / ETA rules
        if (matchesHowFar(cleaned)) {
            return NavCommand.HowFar
        }

        // 4. Next turn rules
        if (matchesNextTurn(cleaned)) {
            return NavCommand.NextTurn
        }

        // 5. Reroute rules
        if (matchesReroute(cleaned)) {
            return NavCommand.Reroute
        }

        // 6. Help rules
        if (cleaned == "help" || cleaned == "commands" || cleaned == "what can i say") {
            return NavCommand.Help
        }

        // 7. Nearest POI (e.g. Petrol pump, mechanic, hospital)
        val nearestPoi = extractNearestPoi(cleaned)
        if (nearestPoi != null) {
            return NavCommand.FindNearest(nearestPoi)
        }

        // 8. Navigate to destination
        val destination = extractDestination(cleaned)
        if (!destination.isNullOrBlank()) {
            return NavCommand.NavigateTo(destination)
        }

        return NavCommand.Unknown(rawInput)
    }

    private fun matchesStopNavigation(text: String): Boolean {
        val stopKeywords = listOf(
            "stop navigation", "stop navigating", "end navigation",
            "cancel navigation", "stop route", "cancel route",
            "end route", "stop", "exit navigation", "quit navigation"
        )
        return stopKeywords.any { text == it || text.startsWith(it) }
    }

    private fun matchesRepeat(text: String): Boolean {
        val repeatKeywords = listOf(
            "repeat", "repeat instruction", "say again", "repeat last",
            "repeat that", "what was that", "pardon", "again"
        )
        return repeatKeywords.any { text == it || text.startsWith(it) }
    }

    private fun matchesHowFar(text: String): Boolean {
        val distanceKeywords = listOf(
            "how far", "how far is it", "distance", "distance remaining",
            "remaining distance", "how much distance", "how long",
            "time remaining", "eta", "when will we arrive"
        )
        return distanceKeywords.any { text == it || text.startsWith(it) }
    }

    private fun matchesNextTurn(text: String): Boolean {
        val nextTurnKeywords = listOf(
            "next turn", "whats next turn", "what is next turn",
            "what's next turn", "what is the next turn", "which turn",
            "where to turn", "directions", "next direction", "next step"
        )
        return nextTurnKeywords.any { text == it || text.startsWith(it) }
    }

    private fun matchesReroute(text: String): Boolean {
        val rerouteKeywords = listOf(
            "reroute", "recalculate", "recalculate route",
            "find another way", "different route", "alternative route"
        )
        return rerouteKeywords.any { text == it || text.startsWith(it) }
    }

    private fun extractNearestPoi(text: String): String? {
        val poiPatterns = listOf(
            Regex("(?:nearest|find|locate|search for|closest)\\s+(?:a\\s+)?(petrol pump|fuel station|gas station|petrol bunk|puncture shop|mechanic|garage|hospital|pharmacy|chemist)"),
            Regex("^(petrol pump|fuel station|gas station|petrol bunk|puncture shop|mechanic|hospital)$")
        )

        for (pattern in poiPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                var poi = match.groupValues[1]
                if (poi in listOf("fuel station", "gas station", "petrol bunk")) {
                    poi = "petrol pump"
                }
                return poi
            }
        }
        return null
    }

    private fun extractDestination(text: String): String? {
        val navPatterns = listOf(
            Regex("^(?:take me to|navigate to|go to|directions to|route to|head to|drive to|ride to)\\s+(.+)$"),
            Regex("^(?:take me)\\s+(.+)$")
        )

        for (pattern in navPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val dest = match.groupValues[1].trim()
                if (dest.isNotEmpty()) {
                    return capitalizeWords(dest)
                }
            }
        }

        // If user simply spoke a place name (e.g. "Shaniwar Wada", "MG Road")
        // and it didn't trigger any system commands
        if (text.split(" ").size in 1..4 &&
            !text.contains("stop") &&
            !text.contains("how") &&
            !text.contains("repeat") &&
            !text.contains("turn")
        ) {
            return capitalizeWords(text)
        }

        return null
    }

    private fun capitalizeWords(input: String): String {
        return input.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
    }
}
