package com.example.voicenav.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenav.core.model.GeoPoint
import com.example.voicenav.core.model.NavCommand
import com.example.voicenav.core.model.VoiceNavUiState
import com.example.voicenav.location.LocationTracker
import com.example.voicenav.navigation.NavigationEngine
import com.example.voicenav.parser.RuleBasedCommandParser
import com.example.voicenav.routing.OpenRouteServiceRepository
import com.example.voicenav.voice.SpeechOutputManager
import com.example.voicenav.voice.VoiceInputListener
import com.example.voicenav.voice.VoiceInputManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VoiceNavViewModel(application: Application) : AndroidViewModel(application) {

    private val routingRepo = OpenRouteServiceRepository(application)
    private val speechOutput = SpeechOutputManager(application)
    private val commandParser = RuleBasedCommandParser()
    val locationTracker = LocationTracker(application)
    private val navigationEngine = NavigationEngine(routingRepo, speechOutput)

    private val _uiState = MutableStateFlow(
        VoiceNavUiState(
            instructionText = "Where would you like to go?",
            youSaidText = "",
            apiKey = routingRepo.getStoredApiKey(),
            isApiKeyConfigured = routingRepo.hasValidApiKey(),
            isGpsEnabled = locationTracker.hasLocationPermission() && locationTracker.isLocationServiceEnabled()
        )
    )
    val uiState: StateFlow<VoiceNavUiState> = _uiState.asStateFlow()

    private var simulatedListeningJob: Job? = null

    private val voiceListener = object : VoiceInputListener {
        override fun onReadyForSpeech() {
            _uiState.update { it.copy(isListening = true, instructionText = "Listening…") }
        }

        override fun onBeginningOfSpeech() {
            _uiState.update { it.copy(instructionText = "Listening to your voice…") }
        }

        override fun onEndOfSpeech() {
            _uiState.update { it.copy(isListening = false) }
        }

        override fun onSpeechResult(text: String, isSimulated: Boolean) {
            handleSpeechInput(text, isSimulated = isSimulated)
        }

        override fun onSpeechError(errorMessage: String, isSimulated: Boolean) {
            _uiState.update {
                it.copy(
                    isListening = false,
                    isSimulatedListening = isSimulated,
                    statusMessage = errorMessage
                )
            }
        }
    }

    private val voiceInputManager = VoiceInputManager(application, voiceListener)

    init {
        // Collect location updates
        viewModelScope.launch {
            locationTracker.currentLocation.collect { loc ->
                _uiState.update { it.copy(currentLocation = loc) }
            }
        }
        viewModelScope.launch {
            locationTracker.isGpsEnabled.collect { enabled ->
                _uiState.update { it.copy(isGpsEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            navigationEngine.session.collect { session ->
                _uiState.update { it.copy(navigationSession = session) }
            }
        }

        // Start location tracking if permitted
        if (locationTracker.hasLocationPermission()) {
            locationTracker.startTracking()
        }
    }

    fun onMicClicked() {
        if (_uiState.value.isListening) {
            stopListening()
            return
        }

        if (voiceInputManager.isNativeSpeechAvailable && !_uiState.value.isSimulatedListening) {
            _uiState.update {
                it.copy(
                    isListening = true,
                    instructionText = "Listening…"
                )
            }
            voiceInputManager.startListening()
        } else {
            // Clearly labelled simulated listening mode for testing or when native speech is unavailable
            startSimulatedListening()
        }
    }

    private fun startSimulatedListening() {
        simulatedListeningJob?.cancel()
        _uiState.update {
            it.copy(
                isListening = true,
                isSimulatedListening = true,
                instructionText = "Listening… [Simulated Mode]"
            )
        }

        // In simulated mode, simulate a rider speaking a navigation command
        simulatedListeningJob = viewModelScope.launch {
            delay(1800) // Brief pause simulating speech capture
            val testCommands = listOf(
                "Take me to Shaniwar Wada",
                "Nearest petrol pump",
                "How far?",
                "Next turn",
                "Repeat",
                "Reroute",
                "Stop navigation"
            )
            // Pick appropriate simulated command based on session state
            val simulatedText = if (_uiState.value.navigationSession.isActive) {
                listOf("How far?", "Next turn", "Repeat", "Stop navigation").random()
            } else {
                listOf("Take me to Shaniwar Wada", "Nearest petrol pump").random()
            }

            handleSpeechInput(simulatedText, isSimulated = true)
        }
    }

    fun stopListening() {
        simulatedListeningJob?.cancel()
        voiceInputManager.stopListening()
        _uiState.update { it.copy(isListening = false) }
    }

    fun handleSpeechInput(rawInput: String, isSimulated: Boolean) {
        _uiState.update {
            it.copy(
                isListening = false,
                isSimulatedListening = isSimulated,
                youSaidText = rawInput
            )
        }

        executeCommand(rawInput)
    }

    private fun executeCommand(text: String) {
        viewModelScope.launch {
            when (val command = commandParser.parse(text)) {
                is NavCommand.NavigateTo -> {
                    _uiState.update { it.copy(instructionText = "Calculating route to ${command.destination}…", isRouting = true) }
                    navigationEngine.routeTo(
                        destinationName = command.destination,
                        currentLocation = _uiState.value.currentLocation
                    ) { announcedText ->
                        _uiState.update { it.copy(instructionText = announcedText, isRouting = false) }
                    }
                }
                is NavCommand.FindNearest -> {
                    val query = "${command.poiType} near me"
                    _uiState.update { it.copy(instructionText = "Searching for nearest ${command.poiType}…", isRouting = true) }
                    navigationEngine.routeTo(
                        destinationName = query,
                        currentLocation = _uiState.value.currentLocation
                    ) { announcedText ->
                        _uiState.update { it.copy(instructionText = announcedText, isRouting = false) }
                    }
                }
                is NavCommand.HowFar -> {
                    val answer = navigationEngine.handleHowFar()
                    _uiState.update { it.copy(instructionText = answer) }
                }
                is NavCommand.NextTurn -> {
                    val answer = navigationEngine.handleNextTurn()
                    _uiState.update { it.copy(instructionText = answer) }
                }
                is NavCommand.Repeat -> {
                    val answer = navigationEngine.handleRepeat()
                    _uiState.update { it.copy(instructionText = answer) }
                }
                is NavCommand.Reroute -> {
                    val currentDest = _uiState.value.navigationSession.destinationName
                    if (currentDest.isNotEmpty()) {
                        _uiState.update { it.copy(instructionText = "Rerouting to $currentDest…", isRouting = true) }
                        navigationEngine.routeTo(currentDest, _uiState.value.currentLocation) { msg ->
                            _uiState.update { it.copy(instructionText = msg, isRouting = false) }
                        }
                    } else {
                        val msg = "No active route to recalculate"
                        speechOutput.speak(msg)
                        _uiState.update { it.copy(instructionText = msg) }
                    }
                }
                is NavCommand.StopNavigation -> {
                    val answer = navigationEngine.stopNavigation()
                    _uiState.update { it.copy(instructionText = answer) }
                }
                is NavCommand.Help -> {
                    val helpMsg = "Commands: Take me to [place], Nearest petrol pump, How far, Next turn, Repeat, Stop navigation."
                    speechOutput.speak(helpMsg)
                    _uiState.update { it.copy(instructionText = helpMsg) }
                }
                is NavCommand.Unknown -> {
                    val unknownMsg = "Command not recognized: \"${command.rawText}\". Say: 'Take me to [place]' or 'Nearest petrol pump'."
                    speechOutput.speak(unknownMsg)
                    _uiState.update { it.copy(instructionText = unknownMsg) }
                }
            }
        }
    }

    fun setSimulatedMode(simulated: Boolean) {
        _uiState.update { it.copy(isSimulatedListening = simulated) }
    }

    fun toggleSettings(open: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = open) }
    }

    fun updateApiKeyInput(newKey: String) {
        _uiState.update { it.copy(apiKey = newKey) }
    }

    fun saveApiKey() {
        val key = _uiState.value.apiKey.trim()
        routingRepo.saveApiKey(key)
        _uiState.update {
            it.copy(
                isApiKeyConfigured = key.isNotEmpty(),
                isSettingsOpen = false,
                statusMessage = if (key.isNotEmpty()) "OpenRouteService API key saved" else "API key cleared"
            )
        }
    }

    fun stopNavigation() {
        val answer = navigationEngine.stopNavigation()
        _uiState.update {
            it.copy(
                instructionText = answer,
                youSaidText = "",
                isRouting = false
            )
        }
    }

    private var searchJob: Job? = null

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.trim().isEmpty()) {
            _uiState.update {
                it.copy(
                    searchSuggestions = emptyList(),
                    isSearching = false,
                    searchError = null
                )
            }
            return
        }

        searchJob = viewModelScope.launch {
            delay(350) // Debounce typing
            _uiState.update { it.copy(isSearching = true, searchError = null) }

            val result = routingRepo.searchDestinations(query, _uiState.value.currentLocation)
            result.onSuccess { suggestions ->
                _uiState.update {
                    it.copy(
                        searchSuggestions = suggestions,
                        isSearching = false,
                        searchError = if (suggestions.isEmpty()) "No places found for \"$query\"" else null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        searchSuggestions = emptyList(),
                        isSearching = false,
                        searchError = error.message ?: "Failed to search destinations"
                    )
                }
            }
        }
    }

    fun onSuggestionSelected(suggestion: GeoPoint) {
        val label = suggestion.label.ifEmpty { "Destination" }
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchSuggestions = emptyList(),
                isSearching = false,
                searchError = null,
                isRouting = true,
                instructionText = "Calculating route to $label…"
            )
        }

        viewModelScope.launch {
            navigationEngine.routeToPoint(
                destinationPoint = suggestion,
                currentLocation = _uiState.value.currentLocation
            ) { announcedText ->
                _uiState.update {
                    it.copy(
                        instructionText = announcedText,
                        isRouting = false
                    )
                }
            }
        }
    }

    fun onSearchSubmitted(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    searchQuery = "",
                    searchSuggestions = emptyList(),
                    isSearching = false,
                    searchError = null
                )
            }
            executeCommand("Take me to $trimmed")
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchSuggestions = emptyList(),
                isSearching = false,
                searchError = null
            )
        }
    }

    fun onLocationPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            locationTracker.startTracking()
        }
        _uiState.update {
            it.copy(isGpsEnabled = isGranted && locationTracker.isLocationServiceEnabled())
        }
    }

    override fun onCleared() {
        super.onCleared()
        simulatedListeningJob?.cancel()
        voiceInputManager.destroy()
        speechOutput.shutdown()
        locationTracker.stopTracking()
    }
}
