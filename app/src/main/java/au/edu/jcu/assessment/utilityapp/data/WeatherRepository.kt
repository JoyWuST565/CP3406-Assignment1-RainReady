package au.edu.jcu.assessment.utilityapp.data

import au.edu.jcu.assessment.utilityapp.domain.City
import au.edu.jcu.assessment.utilityapp.domain.HourForecast
import au.edu.jcu.assessment.utilityapp.domain.WeatherForecast
import java.time.Clock
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

interface WeatherRepository {
    suspend fun fetchForecast(city: City): WeatherForecast
}

class InvalidForecastException : Exception("The forecast response is incomplete.")

class OpenMeteoWeatherRepository(
    private val api: OpenMeteoApi,
    private val clock: Clock = Clock.systemUTC()
) : WeatherRepository {
    override suspend fun fetchForecast(city: City): WeatherForecast =
        mapForecast(api.getForecast(city.latitude, city.longitude), city, clock.instant())
}

internal fun mapForecast(
    response: ForecastResponse,
    city: City,
    fetchedAt: Instant
): WeatherForecast {
    val zone = try {
        ZoneId.of(response.timezone ?: throw InvalidForecastException())
    } catch (_: DateTimeException) {
        throw InvalidForecastException()
    }
    val hourly = response.hourly ?: throw InvalidForecastException()
    val times = hourly.time ?: throw InvalidForecastException()
    val temperatures = hourly.temperatures ?: throw InvalidForecastException()
    val probabilities = hourly.rainProbabilities ?: throw InvalidForecastException()
    val codes = hourly.weatherCodes ?: throw InvalidForecastException()
    if (times.size < 2 || listOf(temperatures.size, probabilities.size, codes.size)
            .any { it != times.size }
    ) throw InvalidForecastException()

    val hours = (0 until times.lastIndex).mapNotNull { index ->
        val start = times[index] ?: return@mapNotNull null
        val next = times[index + 1] ?: return@mapNotNull null
        val temperature = temperatures[index] ?: return@mapNotNull null
        val code = codes[index] ?: return@mapNotNull null
        // The service labels precipitation probability by the END of the preceding hour.
        // Move it to the start of its bin so a 10:00 row covers rain risk from 10:00 to 11:00.
        val probability = probabilities[index + 1] ?: return@mapNotNull null
        if (next - start != 3_600L || !temperature.isFinite() || probability !in 0..100 ||
            code < 0
        ) return@mapNotNull null
        val time = try {
            LocalDateTime.ofInstant(Instant.ofEpochSecond(start), zone)
        } catch (_: DateTimeException) {
            return@mapNotNull null
        }
        HourForecast(time, temperature, probability, code)
    }
    if (hours.isEmpty()) throw InvalidForecastException()
    return WeatherForecast(city, hours, zone, fetchedAt)
}
