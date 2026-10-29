package au.edu.jcu.assessment.utilityapp.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String =
            "temperature_2m,precipitation_probability,weather_code",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 2,
        @Query("temperature_unit") temperatureUnit: String = "celsius",
        @Query("timeformat") timeFormat: String = "unixtime"
    ): ForecastResponse
}

data class ForecastResponse(
    val timezone: String? = null,
    val hourly: HourlyResponse? = null
)

data class HourlyResponse(
    val time: List<Long?>? = null,
    @SerializedName("temperature_2m") val temperatures: List<Double?>? = null,
    @SerializedName("precipitation_probability") val rainProbabilities: List<Int?>? = null,
    @SerializedName("weather_code") val weatherCodes: List<Int?>? = null
)
