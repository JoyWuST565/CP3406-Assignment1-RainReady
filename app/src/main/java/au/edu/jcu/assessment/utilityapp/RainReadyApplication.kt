package au.edu.jcu.assessment.utilityapp

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import au.edu.jcu.assessment.utilityapp.data.OpenMeteoApi
import au.edu.jcu.assessment.utilityapp.data.OpenMeteoWeatherRepository
import au.edu.jcu.assessment.utilityapp.data.PreferenceSettingsStore
import au.edu.jcu.assessment.utilityapp.data.WeatherRepository
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RainReadyApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/** Keeps service construction outside the screens and allows a fake repository in tests. */
class AppContainer(context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()
    private val api = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenMeteoApi::class.java)
    val repository: WeatherRepository = OpenMeteoWeatherRepository(api)
    private val settingsStore = PreferenceSettingsStore(
        context.getSharedPreferences("rainready_preferences", Context.MODE_PRIVATE)
    )

    val viewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            RainReadyViewModel(repository, createSavedStateHandle(), settingsStore)
        }
    }
}
