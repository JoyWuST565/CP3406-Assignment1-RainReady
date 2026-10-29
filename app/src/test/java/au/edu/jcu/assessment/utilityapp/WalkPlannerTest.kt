package au.edu.jcu.assessment.utilityapp

import au.edu.jcu.assessment.utilityapp.domain.City
import au.edu.jcu.assessment.utilityapp.domain.HourForecast
import au.edu.jcu.assessment.utilityapp.domain.UserSettings
import au.edu.jcu.assessment.utilityapp.domain.WalkPlanner
import au.edu.jcu.assessment.utilityapp.domain.WeatherForecast
import au.edu.jcu.assessment.utilityapp.domain.weatherDescription
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WalkPlannerTest {
    private val ten = LocalDateTime.of(2026, 10, 29, 10, 0)
    private val settings = UserSettings()

    private fun forecast(
        rain: List<Int>,
        stormAt: Int? = null
    ) = WeatherForecast(
        City.SINGAPORE,
        rain.mapIndexed { index, risk ->
            HourForecast(ten.plusHours(index.toLong()), 26.0 + index, risk,
                if (index == stormAt) 95 else 2)
        },
        ZoneId.of("Asia/Singapore"),
        Instant.parse("2026-10-29T02:00:00Z")
    )

    @Test fun thresholdIsInclusiveAndStartsImmediately() {
        val result = WalkPlanner.findWindow(forecast(List(8) { 30 }), settings, ten)
        assertEquals(ten, result?.start)
        assertEquals(ten.plusMinutes(30), result?.end)
        assertEquals(30, result?.maxRainProbability)
    }

    @Test fun partialFinalHourMustAlsoMeetRainLimit() {
        val result = WalkPlanner.findWindow(
            forecast(listOf(10, 80, 10, 10, 10, 10, 10)),
            settings.copy(walkMinutes = 60), ten.plusMinutes(30)
        )
        assertEquals(ten.plusHours(2), result?.start)
    }

    @Test fun anHourTouchedOnlyAtItsStartIsNotIncludedInPreviousWalk() {
        val result = WalkPlanner.findWindow(
            forecast(listOf(10, 100, 100)), settings.copy(walkMinutes = 60), ten
        )
        assertEquals(ten.plusHours(1), result?.end)
    }

    @Test fun thunderstormsAreExcludedEvenWhenRainProbabilityIsLow() {
        val result = WalkPlanner.findWindow(forecast(List(8) { 0 }, stormAt = 0), settings, ten)
        assertEquals(ten.plusHours(1), result?.start)
    }

    @Test fun allRainyHoursProduceNoSuggestion() {
        assertNull(WalkPlanner.findWindow(forecast(List(8) { 31 }), settings, ten))
    }

    @Test fun incompleteCoverageDoesNotBecomeDryWeather() {
        val original = forecast(List(3) { 0 })
        val withGap = original.copy(hours = original.hours.filter { it.time != ten.plusHours(1) })
        assertNull(WalkPlanner.findWindow(withGap, settings.copy(walkMinutes = 120), ten))
    }

    @Test fun sixHourDeadlineIncludesWalkEndingExactlyAtDeadline() {
        val result = WalkPlanner.findWindow(
            forecast(listOf(100, 100, 100, 100, 100, 0, 100)),
            settings.copy(walkMinutes = 60), ten
        )
        assertEquals(ten.plusHours(5), result?.start)
        assertEquals(ten.plusHours(6), result?.end)
    }

    @Test fun suggestionNeverExtendsPastSixHourDeadline() {
        assertNull(WalkPlanner.findWindow(
            forecast(listOf(100, 100, 100, 100, 100, 0, 0, 0)),
            settings.copy(walkMinutes = 120), ten
        ))
    }

    @Test fun temperatureIsWeightedByTimeSpentInEachHour() {
        val result = WalkPlanner.findWindow(
            forecast(List(8) { 0 }), settings.copy(walkMinutes = 60), ten.plusMinutes(15)
        )
        assertEquals(26.25, result!!.temperatureCelsius, 0.0001)
    }

    @Test fun forecastForAnotherCityIsNotUsed() {
        assertNull(WalkPlanner.findWindow(
            forecast(List(8) { 0 }), settings.copy(city = City.BRISBANE), ten
        ))
    }

    @Test fun duplicateHourlyDataIsTreatedAsAmbiguous() {
        val original = forecast(listOf(0))
        assertNull(WalkPlanner.findWindow(
            original.copy(hours = original.hours + original.hours), settings, ten
        ))
    }

    @Test fun invalidSettingsAreRejected() {
        val weather = forecast(List(8) { 0 })
        assertNull(WalkPlanner.findWindow(weather, settings.copy(walkMinutes = 0), ten))
        assertNull(WalkPlanner.findWindow(weather, settings.copy(rainLimit = -1), ten))
        assertNull(WalkPlanner.findWindow(weather, settings.copy(walkMinutes = 121), ten))
    }

    @Test fun unknownWeatherCodeDoesNotProduceRecommendation() {
        val original = forecast(List(8) { 0 })
        val unknown = original.copy(hours = original.hours.map { it.copy(weatherCode = 123) })
        assertNull(WalkPlanner.findWindow(unknown, settings, ten))
        assertEquals("Conditions unavailable", weatherDescription(123))
    }

    @Test fun nextHoursIncludeCurrentPartialHourInChronologicalOrder() {
        val original = forecast(List(8) { 0 })
        val result = WalkPlanner.nextHours(original.copy(hours = original.hours.reversed()),
            ten.plusMinutes(15), 3)
        assertEquals(listOf(ten, ten.plusHours(1), ten.plusHours(2)), result.map { it.time })
        assertEquals(emptyList<HourForecast>(), WalkPlanner.nextHours(original, ten, -1))
    }

    @Test fun daylightSavingGapDoesNotProduceNonexistentWalkTime() {
        val start = LocalDateTime.of(2026, 10, 4, 1, 30)
        val weather = WeatherForecast(City.MELBOURNE, listOf(
            HourForecast(start.withMinute(0), 18.0, 0, 0),
            HourForecast(start.withHour(3).withMinute(0), 18.0, 0, 0),
            HourForecast(start.withHour(4).withMinute(0), 18.0, 0, 0)
        ), ZoneId.of("Australia/Melbourne"), Instant.EPOCH)
        val result = WalkPlanner.findWindow(weather,
            settings.copy(city = City.MELBOURNE, walkMinutes = 60), start)
        assertNotNull(result)
        assertEquals(start.withHour(3).withMinute(0), result?.start)
    }

    @Test fun sixHourHorizonUsesElapsedTimeAcrossAutumnClockChange() {
        val start = LocalDateTime.of(2026, 4, 5, 1, 0)
        val weather = WeatherForecast(City.MELBOURNE, (0..7).map { index ->
            HourForecast(start.plusHours(index.toLong()), 18.0,
                if (index >= 5) 0 else 100, 0)
        }, ZoneId.of("Australia/Melbourne"), Instant.EPOCH)
        assertNull(WalkPlanner.findWindow(weather,
            settings.copy(city = City.MELBOURNE, walkMinutes = 60), start))
    }

    @Test fun ambiguousCurrentLocalTimeDoesNotProduceAnUncertainDeparture() {
        val start = LocalDateTime.of(2026, 4, 5, 2, 30)
        val weather = WeatherForecast(City.MELBOURNE, (0..7).map { index ->
            HourForecast(start.withMinute(0).plusHours(index.toLong()), 18.0, 0, 0)
        }, ZoneId.of("Australia/Melbourne"), Instant.EPOCH)
        assertNull(WalkPlanner.findWindow(weather,
            settings.copy(city = City.MELBOURNE), start))
    }
}
