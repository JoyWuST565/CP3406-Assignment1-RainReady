package au.edu.jcu.assessment.utilityapp.data

import au.edu.jcu.assessment.utilityapp.domain.City
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRepositoryTest {
    private val fetchedAt = Instant.parse("2026-10-29T02:05:00Z")
    private val firstEpoch = Instant.parse("2026-10-29T02:00:00Z").epochSecond

    private fun response() = ForecastResponse("Asia/Singapore", HourlyResponse(
        time = listOf(firstEpoch, firstEpoch + 3_600, firstEpoch + 7_200),
        temperatures = listOf(28.5, 29.0, 29.5),
        rainProbabilities = listOf(90, 10, 25),
        weatherCodes = listOf(2, 3, 3)
    ))

    @Test fun mappingConvertsZoneAndAssignsRainToItsActualHour() {
        val forecast = mapForecast(response(), City.SINGAPORE, fetchedAt)
        assertEquals(ZoneId.of("Asia/Singapore"), forecast.timeZone)
        assertEquals(LocalDateTime.of(2026, 10, 29, 10, 0), forecast.hours.first().time)
        assertEquals(listOf(10, 25), forecast.hours.map { it.rainProbability })
        assertEquals(28.5, forecast.hours.first().temperatureCelsius, 0.0001)
        assertEquals(fetchedAt, forecast.fetchedAt)
        assertEquals(City.SINGAPORE, forecast.city)
    }

    @Test(expected = InvalidForecastException::class)
    fun mismatchedArraysAreRejected() {
        val original = response()
        mapForecast(original.copy(hourly = original.hourly!!.copy(temperatures = listOf(1.0))),
            City.SINGAPORE, fetchedAt)
    }

    @Test(expected = InvalidForecastException::class)
    fun absentHourlyDataIsRejected() {
        mapForecast(ForecastResponse("Asia/Singapore"), City.SINGAPORE, fetchedAt)
    }

    @Test(expected = InvalidForecastException::class)
    fun invalidTimezoneIsRejected() {
        mapForecast(response().copy(timezone = "invalid-zone"), City.SINGAPORE, fetchedAt)
    }

    @Test fun missingProbabilityCreatesGapInsteadOfInventingZero() {
        val original = response()
        val mapped = mapForecast(original.copy(hourly = original.hourly!!.copy(
            rainProbabilities = listOf(90, null, 25))), City.SINGAPORE, fetchedAt)
        assertEquals(1, mapped.hours.size)
        assertEquals(LocalDateTime.of(2026, 10, 29, 11, 0), mapped.hours.first().time)
    }

    @Test(expected = InvalidForecastException::class)
    fun completelyInvalidDataIsRejected() {
        val original = response()
        mapForecast(original.copy(hourly = original.hourly!!.copy(
            rainProbabilities = listOf(90, 101, -1))), City.SINGAPORE, fetchedAt)
    }

    @Test fun repositoryRequestsChosenCityAndUsesInjectedClock() = runTest {
        var requestedLatitude = 0.0
        var requestedLongitude = 0.0
        val api = object : OpenMeteoApi {
            override suspend fun getForecast(latitude: Double, longitude: Double, hourly: String,
                timezone: String, forecastDays: Int, temperatureUnit: String,
                timeFormat: String): ForecastResponse {
                requestedLatitude = latitude
                requestedLongitude = longitude
                assertEquals("auto", timezone)
                assertEquals(2, forecastDays)
                assertEquals("celsius", temperatureUnit)
                assertEquals("unixtime", timeFormat)
                assertEquals("temperature_2m,precipitation_probability,weather_code", hourly)
                return response()
            }
        }
        val forecast = OpenMeteoWeatherRepository(api, Clock.fixed(fetchedAt, ZoneOffset.UTC))
            .fetchForecast(City.TOWNSVILLE)
        assertEquals(City.TOWNSVILLE.latitude, requestedLatitude, 0.000001)
        assertEquals(City.TOWNSVILLE.longitude, requestedLongitude, 0.000001)
        assertEquals(fetchedAt, forecast.fetchedAt)
    }

    @Test fun retrofitUsesDocumentedEndpointAndDecodesRealJsonShape() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody("""
                {"timezone":"Asia/Singapore","hourly":{
                  "time":[$firstEpoch,${firstEpoch + 3_600},${firstEpoch + 7_200}],
                  "temperature_2m":[28.5,29.0,29.5],
                  "precipitation_probability":[90,10,25],"weather_code":[2,3,3]}}
            """.trimIndent()))
            server.start()
            val api = Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(OpenMeteoApi::class.java)
            val result = OpenMeteoWeatherRepository(api).fetchForecast(City.SINGAPORE)
            val request = server.takeRequest(1, TimeUnit.SECONDS)
            assertNotNull(request)
            assertEquals("/v1/forecast", request!!.requestUrl!!.encodedPath)
            assertEquals("1.3521", request.requestUrl!!.queryParameter("latitude"))
            assertEquals("103.8198", request.requestUrl!!.queryParameter("longitude"))
            assertEquals("auto", request.requestUrl!!.queryParameter("timezone"))
            assertEquals("2", request.requestUrl!!.queryParameter("forecast_days"))
            assertEquals("unixtime", request.requestUrl!!.queryParameter("timeformat"))
            assertEquals("temperature_2m,precipitation_probability,weather_code",
                request.requestUrl!!.queryParameter("hourly"))
            assertEquals(listOf(10, 25), result.hours.map { it.rainProbability })
        }
    }
}
