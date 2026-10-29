package au.edu.jcu.assessment.utilityapp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.assessment.utilityapp.data.InvalidForecastException
import au.edu.jcu.assessment.utilityapp.data.MemorySettingsStore
import au.edu.jcu.assessment.utilityapp.data.SettingsStore
import au.edu.jcu.assessment.utilityapp.data.WeatherRepository
import au.edu.jcu.assessment.utilityapp.domain.City
import au.edu.jcu.assessment.utilityapp.domain.TemperatureUnit
import au.edu.jcu.assessment.utilityapp.domain.UserSettings
import au.edu.jcu.assessment.utilityapp.domain.WeatherForecast
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class RainReadyUiState(
    val settings: UserSettings = UserSettings(),
    val forecast: WeatherForecast? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class RainReadyViewModel(
    private val repository: WeatherRepository,
    private val savedStateHandle: SavedStateHandle,
    private val settingsStore: SettingsStore = MemorySettingsStore()
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(RainReadyUiState(settings = restoreSettings()))
    val uiState: StateFlow<RainReadyUiState> = mutableUiState.asStateFlow()
    private var requestJob: Job? = null
    private var requestGeneration = 0

    init {
        refresh()
    }

    fun updateSettings(settings: UserSettings) {
        val validated = settings.copy(
            walkMinutes = settings.walkMinutes.coerceIn(1, 120),
            rainLimit = settings.rainLimit.coerceIn(0, 100)
        )
        val cityChanged = validated.city != mutableUiState.value.settings.city
        settingsStore.write(validated)
        savedStateHandle["city"] = validated.city.name
        savedStateHandle["walk_minutes"] = validated.walkMinutes
        savedStateHandle["rain_limit"] = validated.rainLimit
        savedStateHandle["temperature_unit"] = validated.temperatureUnit.name
        mutableUiState.update {
            if (cityChanged) RainReadyUiState(settings = validated)
            else it.copy(settings = validated)
        }
        if (cityChanged) refresh()
    }

    fun refresh() {
        val city = mutableUiState.value.settings.city
        val generation = ++requestGeneration
        requestJob?.cancel()
        mutableUiState.update {
            it.copy(
                forecast = it.forecast?.takeIf { forecast -> forecast.city == city },
                isLoading = true,
                errorMessage = null
            )
        }
        requestJob = viewModelScope.launch {
            try {
                val forecast = repository.fetchForecast(city)
                if (generation == requestGeneration && mutableUiState.value.settings.city == city) {
                    if (forecast.city != city) throw InvalidForecastException()
                    mutableUiState.update {
                        it.copy(forecast = forecast, isLoading = false, errorMessage = null)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (generation == requestGeneration && mutableUiState.value.settings.city == city) {
                    mutableUiState.update {
                        it.copy(isLoading = false, errorMessage = failure.userMessage())
                    }
                }
            }
        }
    }

    private fun restoreSettings(): UserSettings {
        val stored = settingsStore.read()
        return stored.copy(
            city = City.entries.firstOrNull { it.name == savedStateHandle.get<String>("city") }
                ?: stored.city,
            walkMinutes = (savedStateHandle.get<Int>("walk_minutes") ?: stored.walkMinutes)
                .coerceIn(1, 120),
            rainLimit = (savedStateHandle.get<Int>("rain_limit") ?: stored.rainLimit)
                .coerceIn(0, 100),
            temperatureUnit = TemperatureUnit.entries.firstOrNull {
                it.name == savedStateHandle.get<String>("temperature_unit")
            } ?: stored.temperatureUnit
        )
    }

    private fun Exception.userMessage(): String = when (this) {
        is HttpException -> "The weather service is unavailable. Please try again."
        is IOException -> "Unable to connect. Check your internet connection and try again."
        is InvalidForecastException -> "The weather service returned incomplete data. Please try again."
        else -> "The forecast could not be loaded. Please try again."
    }
}
