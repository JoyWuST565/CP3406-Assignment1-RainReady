package au.edu.jcu.assessment.utilityapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.assessment.utilityapp.ui.UtilityApp
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme

class MainActivity : ComponentActivity() {
    private val viewModel: RainReadyViewModel by viewModels {
        (application as RainReadyApplication).container.viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            MobileTech_UtilityAppTheme {
                UtilityApp(
                    state = state,
                    onRefresh = viewModel::refresh,
                    onSettingsChange = viewModel::updateSettings
                )
            }
        }
    }
}
