package au.edu.jcu.assessment.utilityapp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import au.edu.jcu.assessment.utilityapp.data.MemorySettingsStore
import au.edu.jcu.assessment.utilityapp.data.WeatherRepository
import au.edu.jcu.assessment.utilityapp.domain.City
import au.edu.jcu.assessment.utilityapp.domain.HourForecast
import au.edu.jcu.assessment.utilityapp.domain.TemperatureUnit
import au.edu.jcu.assessment.utilityapp.domain.UserSettings
import au.edu.jcu.assessment.utilityapp.domain.WeatherForecast
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RainReadyViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = mutableListOf<RainReadyViewModel>()

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun tearDown() {
        models.forEach { model -> ViewModelStore().apply { put("model", model); clear() } }
        Dispatchers.resetMain()
    }

    private fun weather(city: City = City.SINGAPORE) = WeatherForecast(
        city, listOf(HourForecast(LocalDateTime.of(2026, 10, 29, 10, 0), 28.0, 20, 2)),
        ZoneId.of("Asia/Singapore"), Instant.parse("2026-10-29T02:00:00Z")
    )

    private fun model(
        repository: WeatherRepository,
        state: SavedStateHandle = SavedStateHandle(),
        store: MemorySettingsStore = MemorySettingsStore()
    ) = RainReadyViewModel(repository, state, store).also { models += it }

    @Test fun initialLoadHasExplicitLoadingThenSuccessfulData() = runTest(dispatcher) {
        val repository = object : WeatherRepository {
            override suspend fun fetchForecast(city: City) = weather(city)
        }
        val model = model(repository)
        assertTrue(model.uiState.value.isLoading)
        assertNull(model.uiState.value.forecast)
        runCurrent()
        assertFalse(model.uiState.value.isLoading)
        assertEquals(weather(), model.uiState.value.forecast)
        assertNull(model.uiState.value.errorMessage)
    }

    @Test fun networkFailureHasMessageAndRetryCanRecover() = runTest(dispatcher) {
        var fail = true
        val repository = object : WeatherRepository {
            override suspend fun fetchForecast(city: City): WeatherForecast {
                if (fail) throw IOException("offline")
                return weather(city)
            }
        }
        val model = model(repository)
        runCurrent()
        assertFalse(model.uiState.value.isLoading)
        assertNull(model.uiState.value.forecast)
        assertTrue(model.uiState.value.errorMessage!!.contains("internet connection"))
        fail = false
        model.refresh()
        assertTrue(model.uiState.value.isLoading)
        runCurrent()
        assertNotNull(model.uiState.value.forecast)
        assertNull(model.uiState.value.errorMessage)
    }

    @Test fun refreshFailureRetainsPreviousForecastForSameCity() = runTest(dispatcher) {
        var fail = false
        val repository = object : WeatherRepository {
            override suspend fun fetchForecast(city: City): WeatherForecast {
                if (fail) throw IOException("offline")
                return weather(city)
            }
        }
        val model = model(repository)
        runCurrent()
        val previous = model.uiState.value.forecast
        fail = true
        model.refresh()
        assertEquals(previous, model.uiState.value.forecast)
        runCurrent()
        assertEquals(previous, model.uiState.value.forecast)
        assertNotNull(model.uiState.value.errorMessage)
    }

    @Test fun changingDisplayPreferencesDoesNotRefetchAndPersistsSettings() = runTest(dispatcher) {
        var requests = 0
        val repository = object : WeatherRepository {
            override suspend fun fetchForecast(city: City): WeatherForecast {
                requests++
                return weather(city)
            }
        }
        val state = SavedStateHandle()
        val store = MemorySettingsStore()
        val model = model(repository, state, store)
        runCurrent()
        val newSettings = UserSettings(walkMinutes = 60, rainLimit = 50,
            temperatureUnit = TemperatureUnit.FAHRENHEIT)
        model.updateSettings(newSettings)
        runCurrent()
        assertEquals(1, requests)
        assertEquals(newSettings, model.uiState.value.settings)
        assertEquals(newSettings, store.read())
        assertEquals("FAHRENHEIT", state.get<String>("temperature_unit"))
        assertEquals(60, state.get<Int>("walk_minutes"))
    }

    @Test fun cityChangeImmediatelyClearsDataFromPreviousCity() = runTest(dispatcher) {
        val repository = object : WeatherRepository {
            override suspend fun fetchForecast(city: City) = weather(city)
        }
        val model = model(repository)
        runCurrent()
        model.updateSettings(model.uiState.value.settings.copy(city = City.BRISBANE))
        assertNull(model.uiState.value.forecast)
        assertTrue(model.uiState.value.isLoading)
        runCurrent()
        assertEquals(City.BRISBANE, model.uiState.value.forecast?.city)
    }

    @Test fun savedStateOverridesStoredPreferencesAfterRecreation() = runTest(dispatcher) {
        val repository = object : WeatherRepository {
            override suspend fun fetchForecast(city: City) = weather(city)
        }
        val state = SavedStateHandle(mapOf("city" to "MELBOURNE", "walk_minutes" to 45,
            "rain_limit" to 40, "temperature_unit" to "FAHRENHEIT"))
        val model = model(repository, state)
        runCurrent()
        assertEquals(UserSettings(City.MELBOURNE, 45, 40, TemperatureUnit.FAHRENHEIT),
            model.uiState.value.settings)
        assertEquals(City.MELBOURNE, model.uiState.value.forecast?.city)
    }

    @Test fun oldCityResponseCannotOverwriteLatestCityEvenIfCancellationIsIgnored() = runTest(dispatcher) {
        val repository = ControlledRepository()
        val model = model(repository)
        runCurrent()
        model.updateSettings(UserSettings(city = City.BRISBANE))
        runCurrent()
        repository.requests[1].second.resume(weather(City.BRISBANE))
        runCurrent()
        repository.requests[0].second.resume(weather(City.SINGAPORE))
        runCurrent()
        assertEquals(City.BRISBANE, model.uiState.value.forecast?.city)
        assertEquals(City.BRISBANE, model.uiState.value.settings.city)
        assertNull(model.uiState.value.errorMessage)
    }

    @Test fun olderRefreshCannotReplaceMoreRecentRefresh() = runTest(dispatcher) {
        val repository = ControlledRepository()
        val model = model(repository)
        runCurrent()
        model.refresh()
        runCurrent()
        val newest = weather().copy(fetchedAt = Instant.parse("2026-10-29T03:00:00Z"))
        repository.requests[1].second.resume(newest)
        runCurrent()
        repository.requests[0].second.resume(weather())
        runCurrent()
        assertEquals(newest, model.uiState.value.forecast)
        assertFalse(model.uiState.value.isLoading)
    }

    @Test fun repositoryReturningWrongCityIsReportedAsIncompleteData() = runTest(dispatcher) {
        val repository = object : WeatherRepository {
            override suspend fun fetchForecast(city: City) = weather(City.MELBOURNE)
        }
        val model = model(repository)
        runCurrent()
        assertNull(model.uiState.value.forecast)
        assertTrue(model.uiState.value.errorMessage!!.contains("incomplete data"))
    }

    /** Deliberately ignores cancellation to exercise the ViewModel's generation guard. */
    private class ControlledRepository : WeatherRepository {
        val requests = mutableListOf<Pair<City, Continuation<WeatherForecast>>>()
        override suspend fun fetchForecast(city: City): WeatherForecast = suspendCoroutine {
            requests += city to it
        }
    }
}
