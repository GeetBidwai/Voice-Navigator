package com.example.voicenav.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.MainText
import com.example.ui.theme.MutedText
import com.example.voicenav.core.model.GeoPoint
import org.osmdroid.views.MapView

@Composable
fun VoiceNavScreen(
    viewModel: VoiceNavViewModel,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var osMapInstance by remember { mutableStateOf<MapView?>(null) }
    var recenterTrigger by remember { mutableIntStateOf(0) }

    // Pulse animation when listening
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val buttonBgColor by animateColorAsState(
        targetValue = if (uiState.isListening) AmberAccent else DarkSurface,
        animationSpec = tween(300),
        label = "button_color"
    )
    val buttonContentColor by animateColorAsState(
        targetValue = if (uiState.isListening) DarkBackground else AmberAccent,
        animationSpec = tween(300),
        label = "button_content_color"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("voice_nav_screen")
    ) {
        // 1. Full-screen Interactive Real MapView
        VoiceNavMapView(
            currentLocation = uiState.currentLocation,
            routePoints = uiState.navigationSession.routePoints,
            destinationPoint = uiState.navigationSession.destinationCoords,
            destinationName = uiState.navigationSession.destinationName,
            recenterTrigger = recenterTrigger,
            onMapReady = { osMapInstance = it },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Section: Header + Destination Search Bar + Suggestions Dropdown
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // Compact Header Bar
            Surface(
                color = Color(0xEE12161B),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33F2A900)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Brand Title
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "VOICE NAV",
                            color = AmberAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("app_header_title")
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // GPS Status Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (!uiState.isGpsEnabled) {
                                        onRequestPermissions()
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("gps_status_badge")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (uiState.currentLocation != null) Color(0xFF4CAF50)
                                        else if (uiState.isGpsEnabled) AmberAccent
                                        else Color(0xFFFF5252)
                                    )
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (uiState.currentLocation != null) "GPS ON"
                                else if (uiState.isGpsEnabled) "LOCATING…"
                                else "ENABLE GPS",
                                color = MainText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Settings Icon Button
                        IconButton(
                            onClick = { viewModel.toggleSettings(!uiState.isSettingsOpen) },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.action_settings),
                                tint = if (uiState.isSettingsOpen) AmberAccent else MutedText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Inline OpenRouteService API Key Configuration panel
            AnimatedVisibility(
                visible = uiState.isSettingsOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                InlineSettingsCard(
                    apiKey = uiState.apiKey,
                    isConfigured = uiState.isApiKeyConfigured,
                    onKeyChange = { viewModel.updateApiKeyInput(it) },
                    onSave = { viewModel.saveApiKey() },
                    onClose = { viewModel.toggleSettings(false) }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Wide Compact Destination Search Bar
            Surface(
                color = Color(0xF212161B),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x55F2A900)),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = AmberAccent,
                        modifier = Modifier.size(20.dp)
                    )

                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_placeholder),
                                color = MutedText,
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                viewModel.onSearchSubmitted(uiState.searchQuery)
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedTextColor = MainText,
                            unfocusedTextColor = MainText,
                            cursorColor = AmberAccent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("destination_search_input")
                    )

                    if (uiState.isSearching) {
                        CircularProgressIndicator(
                            color = AmberAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearSearch() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MutedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Search Results / Suggestions Dropdown
            AnimatedVisibility(
                visible = uiState.searchSuggestions.isNotEmpty() || (uiState.searchQuery.isNotEmpty() && uiState.searchError != null),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = Color(0xF81B2129),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (uiState.searchError != null && uiState.searchSuggestions.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.searchError ?: "",
                                    color = MutedText,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!uiState.isApiKeyConfigured) {
                                    Button(
                                        onClick = { viewModel.toggleSettings(true) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AmberAccent,
                                            contentColor = DarkBackground
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text("Set Key", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                            ) {
                                items(uiState.searchSuggestions) { suggestion ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                focusManager.clearFocus()
                                                viewModel.onSuggestionSelected(suggestion)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                            .testTag("suggestion_item_${suggestion.label}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Place,
                                            contentDescription = null,
                                            tint = AmberAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = suggestion.label,
                                            color = MainText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Subtle Status / Voice Feedback pill when active
            if (uiState.isRouting) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xEE12161B),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = AmberAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.instructionText,
                            color = MainText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 3. Right Floating Map Controls (Recenter, Zoom In, Zoom Out)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Recenter GPS Button
            Surface(
                color = Color(0xEE12161B),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (uiState.currentLocation != null) {
                            recenterTrigger++
                        } else {
                            onRequestPermissions()
                        }
                    }
                    .testTag("recenter_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Recenter on GPS",
                        tint = if (uiState.currentLocation != null) AmberAccent else MutedText,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Zoom In Button
            Surface(
                color = Color(0xEE12161B),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        osMapInstance?.controller?.zoomIn()
                    }
                    .testTag("zoom_in_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = MainText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Zoom Out Button
            Surface(
                color = Color(0xEE12161B),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        osMapInstance?.controller?.zoomOut()
                    }
                    .testTag("zoom_out_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = MainText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 4. Bottom Controls: Route Active Panel OR Compact Idle Controls + Floating Mic
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val session = uiState.navigationSession

            if (session.isActive) {
                // ACTIVE ROUTE NAVIGATION PANEL
                Surface(
                    color = Color(0xF612161B),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0x99F2A900)),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_navigation_panel")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Current maneuver instruction & turn icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33F2A900)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    tint = AmberAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.currentManeuver?.instruction ?: uiState.instructionText,
                                    color = MainText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                session.currentManeuver?.let {
                                    if (it.distanceMeters > 0) {
                                        Text(
                                            text = "In ${it.distanceMeters} m",
                                            color = AmberAccent,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Route Progress Summary (Distance, Time, Destination)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "To: ${session.destinationName}",
                                color = MainText,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            val kmRemaining = (session.totalDistanceMeters / 1000.0)
                            val minsRemaining = session.totalDurationSeconds / 60
                            Text(
                                text = String.format(java.util.Locale.ROOT, "%.1f km • %d min", kmRemaining, minsRemaining),
                                color = AmberAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Navigation Actions Row: Floating Mic + Repeat + Stop Navigation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Compact Floating Mic Button
                            CompactMicButton(
                                isListening = uiState.isListening,
                                pulseScale = pulseScale,
                                bgColor = buttonBgColor,
                                contentColor = buttonContentColor,
                                onClick = {
                                    onRequestPermissions()
                                    viewModel.onMicClicked()
                                }
                            )

                            // Voice instruction chips
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                QuickChip(label = stringResource(R.string.btn_repeat)) {
                                    viewModel.handleSpeechInput("Repeat", isSimulated = true)
                                }
                                QuickChip(label = stringResource(R.string.btn_how_far)) {
                                    viewModel.handleSpeechInput("How far?", isSimulated = true)
                                }
                            }

                            // Stop Navigation Action Button
                            Button(
                                onClick = { viewModel.stopNavigation() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0x33FF5252),
                                    contentColor = Color(0xFFFF5252)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252)),
                                modifier = Modifier.testTag("stop_navigation_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = stringResource(R.string.stop_navigation),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Stop",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // IDLE / EXPLORE MODE: Quick destination shortcuts + Floating Microphone Button
                if (uiState.youSaidText.isNotEmpty()) {
                    Surface(
                        color = Color(0xEE12161B),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "You said: \"${uiState.youSaidText}\"",
                            color = AmberAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Quick Destination Shortcuts row (translucent pill chips)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickChip(label = "Shaniwar Wada") {
                        viewModel.handleSpeechInput("Take me to Shaniwar Wada", isSimulated = true)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    QuickChip(label = "Petrol Pump") {
                        viewModel.handleSpeechInput("Nearest petrol pump", isSimulated = true)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    QuickChip(label = "Pune Station") {
                        viewModel.handleSpeechInput("Take me to Pune Railway Station", isSimulated = true)
                    }
                }

                // Floating Microphone Button (~64dp, circular, pulses when listening)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .scale(if (uiState.isListening) pulseScale else 1.0f)
                ) {
                    // Outer border ring for visual affordance
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.5.dp,
                                color = if (uiState.isListening) AmberAccent else Color(0x88F2A900),
                                shape = CircleShape
                            )
                    )

                    // Interactive circular button
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(buttonBgColor)
                            .clickable {
                                onRequestPermissions()
                                viewModel.onMicClicked()
                            }
                            .testTag("microphone_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = if (uiState.isListening) {
                                    stringResource(R.string.btn_listening)
                                } else {
                                    stringResource(R.string.btn_ask)
                                },
                                tint = buttonContentColor,
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                text = if (uiState.isListening) "Listening" else "Ask",
                                color = buttonContentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactMicButton(
    isListening: Boolean,
    pulseScale: Float,
    bgColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .scale(if (isListening) pulseScale else 1.0f)
            .clip(CircleShape)
            .border(2.dp, AmberAccent, CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .testTag("compact_microphone_button")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = if (isListening) "Listening" else "Ask",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = if (isListening) "Hear" else "Ask",
                color = contentColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QuickChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xEE1B2129),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44F2A900)),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("quick_chip_$label")
    ) {
        Text(
            text = label,
            color = MainText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun InlineSettingsCard(
    apiKey: String,
    isConfigured: Boolean,
    onKeyChange: (String) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("inline_settings_card"),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xF51B2129),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Key,
                        contentDescription = null,
                        tint = AmberAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.ors_api_key_label),
                        color = MainText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cancel),
                        tint = MutedText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = onKeyChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ors_api_key_input"),
                placeholder = {
                    Text(
                        text = "Enter 5b3ce3597851110001...",
                        color = MutedText,
                        fontSize = 12.sp
                    )
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AmberAccent,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = MainText,
                    unfocusedTextColor = MainText,
                    cursorColor = AmberAccent
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isConfigured) "Key saved locally" else "Key required for live search & routing",
                    color = if (isConfigured) AmberAccent else MutedText,
                    fontSize = 11.sp
                )

                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberAccent,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("save_api_key_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.save),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
