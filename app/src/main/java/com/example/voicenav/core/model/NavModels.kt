package com.example.voicenav.core.model

/**
 * Deterministic navigation commands parsed from voice input.
 * No AI or LLMs used.
 */
sealed class NavCommand {
    data class NavigateTo(val destination: String) : NavCommand()
    data class FindNearest(val poiType: String) : NavCommand()
    object HowFar : NavCommand()
    object NextTurn : NavCommand()
    object Repeat : NavCommand()
    object Reroute : NavCommand()
    object StopNavigation : NavCommand()
    object Help : NavCommand()
    data class Unknown(val rawText: String) : NavCommand()
}

/**
 * A turn-by-turn maneuver instruction.
 */
data class ManeuverInstruction(
    val instruction: String,
    val distanceMeters: Int,
    val streetName: String = "",
    val turnType: String = "straight" // left, right, uturn, roundabout, arrive, straight
)

/**
 * Coordinates and route details.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val label: String = ""
)

/**
 * Overall active navigation session.
 */
data class NavigationSession(
    val isActive: Boolean = false,
    val destinationName: String = "",
    val destinationCoords: GeoPoint? = null,
    val maneuvers: List<ManeuverInstruction> = emptyList(),
    val currentStepIndex: Int = 0,
    val totalDistanceMeters: Int = 0,
    val totalDurationSeconds: Int = 0,
    val routePoints: List<GeoPoint> = emptyList()
) {
    val currentManeuver: ManeuverInstruction?
        get() = maneuvers.getOrNull(currentStepIndex)
}

/**
 * UI State for Voice Nav single-screen rider interface.
 */
data class VoiceNavUiState(
    val instructionText: String = "Where would you like to go?",
    val youSaidText: String = "",
    val isListening: Boolean = false,
    val isSimulatedListening: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val apiKey: String = "",
    val isApiKeyConfigured: Boolean = false,
    val isGpsEnabled: Boolean = false,
    val currentLocation: GeoPoint? = null,
    val isRouting: Boolean = false,
    val navigationSession: NavigationSession = NavigationSession(),
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val searchSuggestions: List<GeoPoint> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null
)
