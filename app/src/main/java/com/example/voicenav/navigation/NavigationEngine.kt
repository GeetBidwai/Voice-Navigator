package com.example.voicenav.navigation

import com.example.voicenav.core.model.GeoPoint
import com.example.voicenav.core.model.ManeuverInstruction
import com.example.voicenav.core.model.NavigationSession
import com.example.voicenav.routing.OpenRouteServiceRepository
import com.example.voicenav.routing.RoutingResult
import com.example.voicenav.voice.SpeechOutputManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class NavigationEvent {
    data class InstructionAnnounced(val text: String) : NavigationEvent()
    data class RouteCalculated(val session: NavigationSession) : NavigationEvent()
    object NavigationStopped : NavigationEvent()
    data class Error(val message: String) : NavigationEvent()
}

/**
 * Handles navigation lifecycle, maneuver step tracking, and audio announcements.
 */
class NavigationEngine(
    private val routingRepo: OpenRouteServiceRepository,
    private val speechOutput: SpeechOutputManager
) {
    private val _session = MutableStateFlow(NavigationSession())
    val session: StateFlow<NavigationSession> = _session.asStateFlow()

    private var lastAnnouncedInstruction: String = "Where would you like to go?"

    suspend fun routeTo(
        destinationName: String,
        currentLocation: GeoPoint?,
        onAnnouncement: (String) -> Unit
    ): RoutingResult {
        if (currentLocation == null) {
            val errorMsg = "GPS location unavailable. Please enable device location."
            speechOutput.speak(errorMsg)
            onAnnouncement(errorMsg)
            return RoutingResult.Error(errorMsg)
        }

        if (!routingRepo.hasValidApiKey()) {
            val errorMsg = "OpenRouteService API key not set. Tap settings to configure."
            speechOutput.speak("API key not configured. Tap settings to enter your key.")
            onAnnouncement(errorMsg)
            return RoutingResult.Error(errorMsg)
        }

        onAnnouncement("Finding route to $destinationName…")
        speechOutput.speak("Finding route to $destinationName")

        val geocodeResult = routingRepo.geocodeDestination(destinationName, currentLocation)
        val destinationPoint = geocodeResult.getOrElse {
            val err = it.message ?: "Destination not found"
            speechOutput.speak(err)
            onAnnouncement(err)
            return RoutingResult.Error(err)
        }

        val routeResult = routingRepo.getRoute(currentLocation, destinationPoint)
        when (routeResult) {
            is RoutingResult.Success -> {
                _session.value = routeResult.session
                val firstManeuver = routeResult.session.currentManeuver?.instruction
                    ?: "Route started. Head toward ${destinationPoint.label}."
                val announceText = "Navigating to ${destinationPoint.label}. $firstManeuver"
                lastAnnouncedInstruction = announceText
                speechOutput.speak(announceText)
                onAnnouncement(announceText)
            }
            is RoutingResult.Error -> {
                speechOutput.speak(routeResult.message)
                onAnnouncement(routeResult.message)
            }
        }
        return routeResult
    }

    suspend fun routeToPoint(
        destinationPoint: GeoPoint,
        currentLocation: GeoPoint?,
        onAnnouncement: (String) -> Unit
    ): RoutingResult {
        if (currentLocation == null) {
            val errorMsg = "GPS location unavailable. Please enable device location."
            speechOutput.speak(errorMsg)
            onAnnouncement(errorMsg)
            return RoutingResult.Error(errorMsg)
        }

        if (!routingRepo.hasValidApiKey()) {
            val errorMsg = "OpenRouteService API key not set. Tap ⚙ to configure."
            speechOutput.speak("API key not configured. Tap settings to enter your key.")
            onAnnouncement(errorMsg)
            return RoutingResult.Error(errorMsg)
        }

        val label = destinationPoint.label.ifEmpty { "Destination" }
        onAnnouncement("Calculating route to $label…")
        speechOutput.speak("Finding route to $label")

        val routeResult = routingRepo.getRoute(currentLocation, destinationPoint)
        when (routeResult) {
            is RoutingResult.Success -> {
                _session.value = routeResult.session
                val firstManeuver = routeResult.session.currentManeuver?.instruction
                    ?: "Route started. Head toward $label."
                val announceText = "Navigating to $label. $firstManeuver"
                lastAnnouncedInstruction = announceText
                speechOutput.speak(announceText)
                onAnnouncement(announceText)
            }
            is RoutingResult.Error -> {
                speechOutput.speak(routeResult.message)
                onAnnouncement(routeResult.message)
            }
        }
        return routeResult
    }

    fun handleHowFar(): String {
        val current = _session.value
        if (!current.isActive) {
            val msg = "No active navigation route"
            speechOutput.speak(msg)
            return msg
        }

        val km = current.totalDistanceMeters / 1000.0
        val mins = (current.totalDurationSeconds / 60)
        val response = if (km >= 1.0) {
            String.format(Locale.ROOT, "%.1f kilometers remaining, approximately %d minutes", km, mins)
        } else {
            "${current.totalDistanceMeters} meters remaining, approximately $mins minutes"
        }
        speechOutput.speak(response)
        return response
    }

    fun handleNextTurn(): String {
        val current = _session.value
        if (!current.isActive) {
            val msg = "No active navigation route"
            speechOutput.speak(msg)
            return msg
        }

        val maneuver = current.currentManeuver
        val response = if (maneuver != null) {
            "In ${maneuver.distanceMeters} meters, ${maneuver.instruction}"
        } else {
            "Continue on current road"
        }
        lastAnnouncedInstruction = response
        speechOutput.speak(response)
        return response
    }

    fun handleRepeat(): String {
        speechOutput.speak(lastAnnouncedInstruction)
        return lastAnnouncedInstruction
    }

    fun stopNavigation(): String {
        _session.value = NavigationSession(isActive = false)
        val msg = "Navigation stopped"
        lastAnnouncedInstruction = "Where would you like to go?"
        speechOutput.speak(msg)
        return msg
    }

    fun advanceToNextManeuver(): ManeuverInstruction? {
        val current = _session.value
        if (!current.isActive) return null

        val nextIndex = current.currentStepIndex + 1
        return if (nextIndex < current.maneuvers.size) {
            _session.value = current.copy(currentStepIndex = nextIndex)
            val next = current.maneuvers[nextIndex]
            val text = "Next: ${next.instruction}"
            lastAnnouncedInstruction = text
            speechOutput.speak(text)
            next
        } else {
            val arrived = "You have arrived at your destination"
            lastAnnouncedInstruction = arrived
            speechOutput.speak(arrived)
            _session.value = current.copy(isActive = false)
            null
        }
    }
}
