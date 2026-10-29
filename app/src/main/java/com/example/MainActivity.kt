package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.example.ui.theme.MyApplicationTheme
import com.example.voicenav.ui.VoiceNavScreen
import com.example.voicenav.ui.VoiceNavViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: VoiceNavViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                // Request permissions launcher for audio recording and location
                val permissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    val fineLocationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
                    val coarseLocationGranted = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true

                    viewModel.onLocationPermissionResult(fineLocationGranted || coarseLocationGranted)
                }

                // Prompt user for location permissions on launch for immediate GPS map lock
                LaunchedEffect(Unit) {
                    permissionsLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.RECORD_AUDIO
                        )
                    )
                }

                VoiceNavScreen(
                    viewModel = viewModel,
                    onRequestPermissions = {
                        permissionsLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.RECORD_AUDIO
                            )
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
