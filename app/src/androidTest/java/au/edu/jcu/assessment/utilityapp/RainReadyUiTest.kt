package au.edu.jcu.assessment.utilityapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import au.edu.jcu.assessment.utilityapp.domain.City
import au.edu.jcu.assessment.utilityapp.domain.HourForecast
import au.edu.jcu.assessment.utilityapp.domain.TemperatureUnit
import au.edu.jcu.assessment.utilityapp.domain.UserSettings
import au.edu.jcu.assessment.utilityapp.domain.WeatherForecast
import au.edu.jcu.assessment.utilityapp.ui.SettingsScreen
import au.edu.jcu.assessment.utilityapp.ui.UtilityApp
import au.edu.jcu.assessment.utilityapp.ui.UtilityScreen
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RainReadyUiTest {
    @get:Rule val compose = createComposeRule()
    private val now = Instant.parse("2026-10-29T02:15:00Z")

    @Test
    fun settingsChangeCityDurationRainLimitAndUnit() {
        var settings by mutableStateOf(UserSettings())
        compose.setContent { MobileTech_UtilityAppTheme { SettingsScreen(settings) { settings = it } } }
        compose.onNodeWithText("Townsville").performClick().assertIsSelected()
        compose.onNodeWithText("60 min").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("10%").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Fahrenheit (°F)").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle {
            assertEquals(City.TOWNSVILLE, settings.city)
            assertEquals(60, settings.walkMinutes)
            assertEquals(10, settings.rainLimit)
            assertEquals(TemperatureUnit.FAHRENHEIT, settings.temperatureUnit)
        }
    }

    @Test
    fun freshForecastShowsRecommendationAndRefreshAction() {
        var refreshed = false
        compose.setContent {
            MobileTech_UtilityAppTheme {
                UtilityScreen(RainReadyUiState(UserSettings(), forecast(now.minusSeconds(120)), false), { refreshed = true }, {}, now)
            }
        }
        compose.onNodeWithText("Head out now").assertIsDisplayed()
        compose.onNodeWithContentDescription("Refresh forecast").performClick()
        compose.runOnIdle { assertTrue(refreshed) }
    }

    @Test
    fun staleForecastSuppressesDepartureSuggestion() {
        compose.setContent {
            MobileTech_UtilityAppTheme {
                UtilityScreen(RainReadyUiState(UserSettings(), forecast(now.minusSeconds(7200)), false), {}, {}, now)
            }
        }
        compose.onNodeWithText("Refresh before you go").assertIsDisplayed()
        compose.onNodeWithText("Head out now").assertDoesNotExist()
    }

    @Test
    fun failedInitialFetchProvidesRetry() {
        var retried = false
        compose.setContent {
            MobileTech_UtilityAppTheme {
                UtilityScreen(
                    RainReadyUiState(UserSettings(), isLoading = false, errorMessage = "No internet connection."),
                    { retried = true }, {}, now
                )
            }
        }
        compose.onNodeWithText("Forecast unavailable").assertIsDisplayed()
        compose.onNodeWithText("Try again").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(retried) }
    }

    @Test
    fun navigationReachesSettings() {
        compose.setContent {
            MobileTech_UtilityAppTheme {
                UtilityApp(RainReadyUiState(UserSettings(), isLoading = true), {}, {})
            }
        }
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Make it your walk").assertIsDisplayed()
    }

    @Test
    fun imminentFutureWindowIsNotPresentedAsNow() {
        val boundary = now.plusSeconds(44L * 60 + 30)
        val forecast = forecast(boundary.minusSeconds(120)).let { original ->
            original.copy(hours = original.hours.mapIndexed { index, hour ->
                hour.copy(rainProbability = if (index == 0) 80 else 10)
            })
        }
        compose.setContent {
            MobileTech_UtilityAppTheme {
                UtilityScreen(RainReadyUiState(UserSettings(), forecast, false), {}, {}, boundary)
            }
        }
        compose.onNodeWithText("Leave at 11:00").assertIsDisplayed()
        compose.onNodeWithText("Head out now").assertDoesNotExist()
    }
    private fun forecast(fetched: Instant): WeatherForecast {
        val zone = ZoneId.of("Asia/Singapore")
        val first = LocalDateTime.ofInstant(now, zone).withMinute(0)
        return WeatherForecast(
            City.SINGAPORE,
            List(8) { HourForecast(first.plusHours(it.toLong()), 29.0, 10, 2) },
            zone,
            fetched
        )
    }
}



