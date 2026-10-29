package au.edu.jcu.assessment.utilityapp.domain

import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** Selects the earliest qualifying walk; hourly predictions cannot guarantee dry conditions. */
object WalkPlanner {
    private val knownWeatherCodes = setOf(
        0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67,
        71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 97, 99
    )

    fun nextHours(
        forecast: WeatherForecast,
        now: LocalDateTime,
        count: Int = 6
    ): List<HourForecast> = forecast.hours
        .filter { !it.time.isBefore(now.truncatedTo(ChronoUnit.HOURS)) }
        .sortedBy { it.time }
        .take(count.coerceAtLeast(0))

    fun findWindow(
        forecast: WeatherForecast,
        settings: UserSettings,
        now: LocalDateTime
    ): WalkWindow? {
        if (forecast.city != settings.city || settings.walkMinutes !in 1..120 ||
            settings.rainLimit !in 0..100
        ) return null

        val rules = forecast.timeZone.rules
        if (rules.getValidOffsets(now).size != 1) return null
        val deadline = now.atZone(forecast.timeZone).plusHours(6).toLocalDateTime()
        // Duplicate local times during an autumn clock change are ambiguous: do not guess.
        val hours = forecast.hours.groupBy { it.time }
        var candidate = now
        while (!candidate.plusMinutes(settings.walkMinutes.toLong()).isAfter(deadline)) {
            val end = candidate.plusMinutes(settings.walkMinutes.toLong())
            var cursor = candidate.truncatedTo(ChronoUnit.HOURS)
            val covered = mutableListOf<Pair<HourForecast, Long>>()
            var usable = rules.getValidOffsets(candidate).size == 1 &&
                rules.getValidOffsets(end).size == 1 &&
                Duration.between(candidate.atZone(forecast.timeZone), end.atZone(forecast.timeZone))
                    .toMinutes() == settings.walkMinutes.toLong()

            // Include every bin touched by the walk, including a partial final hour.
            while (usable && cursor.isBefore(end)) {
                val hour = hours[cursor]?.singleOrNull()
                if (hour == null || hour.rainProbability !in 0..settings.rainLimit ||
                    hour.weatherCode !in knownWeatherCodes || hour.weatherCode >= 95 ||
                    !hour.temperatureCelsius.isFinite()
                ) {
                    usable = false
                    break
                }
                val overlapStart = maxOf(candidate, cursor)
                val overlapEnd = minOf(end, cursor.plusHours(1))
                covered += hour to Duration.between(overlapStart, overlapEnd).seconds
                cursor = cursor.plusHours(1)
            }

            if (usable && covered.isNotEmpty()) {
                val seconds = covered.sumOf { it.second }
                return WalkWindow(
                    start = candidate,
                    end = end,
                    maxRainProbability = covered.maxOf { it.first.rainProbability },
                    temperatureCelsius = covered.sumOf {
                        it.first.temperatureCelsius * it.second
                    } / seconds
                )
            }
            candidate = candidate.truncatedTo(ChronoUnit.HOURS).plusHours(1)
        }
        return null
    }
}
