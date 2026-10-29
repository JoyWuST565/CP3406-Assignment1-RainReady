package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.RainReadyUiState
import au.edu.jcu.assessment.utilityapp.domain.UserSettings
import java.time.Instant
import kotlinx.coroutines.delay

/** The starter's two-tab structure, driven only by observable state and callbacks. */
@Composable
fun UtilityApp(
    state: RainReadyUiState,
    onRefresh: () -> Unit,
    onSettingsChange: (UserSettings) -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf("Utility") }
    val lifecycleOwner = LocalLifecycleOwner.current
    val now by produceState(initialValue = Instant.now(), key1 = lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = Instant.now()
                delay(30_000)
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = selectedTab == "Utility",
                    onClick = { selectedTab = "Utility" },
                    icon = { Icon(Icons.AutoMirrored.Outlined.DirectionsWalk, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_utility)) }
                )
                NavigationBarItem(
                    selected = selectedTab == "Settings",
                    onClick = { selectedTab = "Settings" },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_settings)) }
                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            if (selectedTab == "Utility") {
                UtilityScreen(state, onRefresh, { selectedTab = "Settings" }, now)
            } else {
                SettingsScreen(state.settings, onSettingsChange)
            }
        }
    }
}



