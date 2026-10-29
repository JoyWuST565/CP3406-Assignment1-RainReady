package au.edu.jcu.assessment.utilityapp.domain

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

enum class City(val label: String, val latitude: Double, val longitude: Double) {
    SINGAPORE("Singapore", 1.3521, 103.8198),
    TOWNSVILLE("Townsville", -19.2589, 146.8169),
    BRISBANE("Brisbane", -27.4698, 153.0251),
    MELBOURNE("Melbourne", -37.8136, 144.9631)
}

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }

data class UserSettings(
    val city: City = City.SINGAPORE,
    val walkMinutes: Int = 30,
    val rainLimit: Int = 30,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
)

/** Rain probability describes the hour beginning at [time]. */
data class HourForecast(
    val time: LocalDateTime,
    val temperatureCelsius: Double,
    val rainProbability: Int,
    val weatherCode: Int
)

data class WeatherForecast(
    val city: City,
    val hours: List<HourForecast>,
    val timeZone: ZoneId,
    val fetchedAt: Instant
)

data class WalkWindow(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val maxRainProbability: Int,
    val temperatureCelsius: Double
)

fun weatherDescription(code: Int): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45 -> "Fog"
    48 -> "Rime fog"
    51 -> "Light drizzle"
    53 -> "Moderate drizzle"
    55 -> "Dense drizzle"
    56 -> "Light freezing drizzle"
    57 -> "Dense freezing drizzle"
    61 -> "Slight rain"
    63 -> "Moderate rain"
    65 -> "Heavy rain"
    66 -> "Light freezing rain"
    67 -> "Heavy freezing rain"
    71 -> "Slight snowfall"
    73 -> "Moderate snowfall"
    75 -> "Heavy snowfall"
    77 -> "Snow grains"
    80 -> "Slight rain showers"
    81 -> "Moderate rain showers"
    82 -> "Violent rain showers"
    85 -> "Slight snow showers"
    86 -> "Heavy snow showers"
    95 -> "Thunderstorm"
    96 -> "Thunderstorm with slight hail"
    97 -> "Heavy thunderstorm"
    99 -> "Thunderstorm with heavy hail"
    else -> "Conditions unavailable"
}
