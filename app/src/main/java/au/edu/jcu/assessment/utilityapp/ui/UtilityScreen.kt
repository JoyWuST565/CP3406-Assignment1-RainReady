package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.RainReadyUiState
import au.edu.jcu.assessment.utilityapp.domain.HourForecast
import au.edu.jcu.assessment.utilityapp.domain.TemperatureUnit
import au.edu.jcu.assessment.utilityapp.domain.UserSettings
import au.edu.jcu.assessment.utilityapp.domain.WalkPlanner
import au.edu.jcu.assessment.utilityapp.domain.WalkWindow
import au.edu.jcu.assessment.utilityapp.domain.WeatherForecast
import au.edu.jcu.assessment.utilityapp.domain.weatherDescription
import au.edu.jcu.assessment.utilityapp.ui.theme.DeepTeal
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val HourFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.UK)
private val UpdatedFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.UK)

@Composable
fun UtilityScreen(
    state: RainReadyUiState,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    now: Instant
) {
    val refreshingLabel = stringResource(R.string.refreshing_forecast)
    val forecast = state.forecast?.takeIf { it.city == state.settings.city }
    val localNow = forecast?.let { LocalDateTime.ofInstant(now, it.timeZone) }
    val isStale = forecast != null && Duration.between(forecast.fetchedAt, now).toMinutes() >= 60
    val window = if (forecast != null && localNow != null && !isStale) {
        WalkPlanner.findWindow(forecast, state.settings, localNow)
    } else null

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { AppHeader(state.isLoading, onRefresh) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.plan_heading),
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(stringResource(R.string.plan_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.city_local_time, state.settings.city.label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (state.isLoading && forecast != null) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentDescription = refreshingLabel
                        }
                    )
                }
            }
            if (state.errorMessage != null) {
                item { ErrorNotice(state.errorMessage, forecast != null, state.isLoading, onRefresh) }
            }
            when {
                forecast == null && state.isLoading -> item { LoadingCard(state.settings.city.label) }
                forecast == null -> item { UnavailableCard(state.isLoading, onRefresh) }
                isStale -> item { StaleCard(state.isLoading, onRefresh) }
                window != null && localNow != null -> item { WalkSuggestion(window, state.settings, localNow) }
                else -> item { NoWindowCard(state.settings) }
            }
            item { PreferenceSummary(state.settings, onOpenSettings) }
            if (forecast != null && localNow != null) {
                item {
                    SectionHeading(
                        stringResource(R.string.hourly_heading),
                        stringResource(R.string.hourly_subtitle)
                    )
                }
                item {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WalkPlanner.nextHours(forecast, localNow, count = 7).forEach { hour ->
                            HourCard(hour, state.settings)
                        }
                    }
                }
                item { ForecastDetails(forecast) }
            }
            item { ForecastCaution() }
        }
    }
}

@Composable
private fun AppHeader(isLoading: Boolean, onRefresh: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(15.dp)) {
            Icon(
                Icons.Outlined.Umbrella, null,
                Modifier.padding(10.dp).size(25.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onRefresh, enabled = !isLoading, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Outlined.Refresh,
                stringResource(if (isLoading) R.string.refreshing_forecast else R.string.refresh_forecast)
            )
        }
    }
}

@Composable
private fun WalkSuggestion(window: WalkWindow, settings: UserSettings, now: LocalDateTime) {
    Surface(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = DeepTeal, contentColor = Color.White, shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Outlined.DirectionsWalk, null, Modifier.size(25.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.suggested_departure), style = MaterialTheme.typography.labelSmall)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (!window.start.isAfter(now)) stringResource(R.string.leave_now)
                    else stringResource(R.string.leave_at, HourFormat.format(window.start)),
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    stringResource(R.string.walk_time_range, HourFormat.format(window.start), HourFormat.format(window.end)),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFC0ECDD)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroDetail(Icons.Outlined.Schedule, stringResource(R.string.walk_duration, settings.walkMinutes))
                HeroDetail(Icons.Outlined.WaterDrop, stringResource(R.string.rain_up_to, window.maxRainProbability))
                HeroDetail(Icons.Outlined.CloudQueue, stringResource(R.string.temperature_label, temperature(window.temperatureCelsius, settings.temperatureUnit)))
            }
            Text(
                stringResource(R.string.window_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD3E6DE)
            )
        }
    }
}

@Composable
private fun HeroDetail(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(19.dp), tint = Color(0xFFA1E1CC))
        Spacer(Modifier.width(9.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun NoWindowCard(settings: UserSettings) {
    MessageCard(
        icon = Icons.Outlined.Umbrella,
        title = stringResource(R.string.no_window_heading),
        message = stringResource(R.string.no_window_body, settings.walkMinutes, settings.rainLimit)
    )
}

@Composable
private fun StaleCard(isLoading: Boolean, onRefresh: () -> Unit) {
    MessageCard(
        icon = Icons.Outlined.WarningAmber,
        title = stringResource(R.string.stale_heading),
        message = stringResource(R.string.stale_body),
        action = { Button(onRefresh, enabled = !isLoading) { Text(stringResource(R.string.refresh_forecast)) } }
    )
}

@Composable
private fun LoadingCard(city: String) {
    Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(Modifier.size(34.dp))
            Text(stringResource(R.string.forecast_loading), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.forecast_loading_body, city), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun UnavailableCard(isLoading: Boolean, onRefresh: () -> Unit) {
    MessageCard(
        Icons.Outlined.WifiOff,
        stringResource(R.string.forecast_unavailable),
        stringResource(R.string.forecast_unavailable_body),
        action = { Button(onRefresh, enabled = !isLoading) { Text(stringResource(R.string.try_again)) } }
    )
}

@Composable
private fun MessageCard(icon: ImageVector, title: String, message: String, action: (@Composable () -> Unit)? = null) {
    Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(message, style = MaterialTheme.typography.bodyLarge)
            action?.invoke()
        }
    }
}

@Composable
private fun ErrorNotice(message: String, hasForecast: Boolean, isLoading: Boolean, onRefresh: () -> Unit) {
    Surface(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.errorContainer) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.refresh_failed), style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (hasForecast) {
                Text(stringResource(R.string.cached_forecast_note), style = MaterialTheme.typography.bodySmall)
                TextButton(onRefresh, enabled = !isLoading) { Text(stringResource(R.string.try_again)) }
            }
        }
    }
}

@Composable
private fun PreferenceSummary(settings: UserSettings, onOpenSettings: () -> Unit) {
    OutlinedCard(shape = RoundedCornerShape(18.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.settings_summary, settings.walkMinutes, settings.rainLimit),
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onOpenSettings) { Text(stringResource(R.string.adjust_plan)) }
        }
    }
}

@Composable
private fun HourCard(hour: HourForecast, settings: UserSettings) {
    val storm = hour.weatherCode in 95..99
    val meetsLimit = hour.rainProbability <= settings.rainLimit && !storm
    val status = stringResource(when {
        storm -> R.string.storm_period
        meetsLimit -> R.string.within_limit
        else -> R.string.above_limit
    })
    val weather = weatherDescription(hour.weatherCode)
    val temp = temperature(hour.temperatureCelsius, settings.temperatureUnit)
    val description = stringResource(
        R.string.hour_accessibility, HourFormat.format(hour.time), weather, temp, hour.rainProbability, status
    )
    Surface(
        modifier = Modifier.width(152.dp).clearAndSetSemantics { contentDescription = description },
        color = if (meetsLimit) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.43f)
            else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(HourFormat.format(hour.time), style = MaterialTheme.typography.titleMedium)
            Icon(weatherIcon(hour.weatherCode), null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
            Text(temp, style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.hour_rain, hour.rainProbability), style = MaterialTheme.typography.labelLarge)
            Text(weather, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                status,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (meetsLimit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun SectionHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ForecastDetails(forecast: WeatherForecast) {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(stringResource(R.string.forecast_updated, UpdatedFormat.format(forecast.fetchedAt.atZone(forecast.timeZone))), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.forecast_timezone, forecast.timeZone.id), style = MaterialTheme.typography.bodySmall)
        TextButton(
            onClick = { uriHandler.openUri("https://open-meteo.com/") },
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) { Text(stringResource(R.string.forecast_source)) }
        Text(stringResource(R.string.forecast_hour_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ForecastCaution() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Outlined.Info, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.forecast_caution), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun temperature(celsius: Double, unit: TemperatureUnit): String =
    if (unit == TemperatureUnit.CELSIUS) "${celsius.roundToInt()}°C"
    else "${(celsius * 9 / 5 + 32).roundToInt()}°F"

private fun weatherIcon(code: Int): ImageVector = when (code) {
    0 -> Icons.Outlined.WbSunny
    1, 2 -> Icons.Outlined.CloudQueue
    3, 45, 48 -> Icons.Outlined.Cloud
    71, 73, 75, 77, 85, 86 -> Icons.Outlined.AcUnit
    95, 96, 99 -> Icons.Outlined.Thunderstorm
    else -> Icons.Outlined.WaterDrop
}



