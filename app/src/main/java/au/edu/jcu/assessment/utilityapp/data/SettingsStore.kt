package au.edu.jcu.assessment.utilityapp.data

import android.content.SharedPreferences
import au.edu.jcu.assessment.utilityapp.domain.City
import au.edu.jcu.assessment.utilityapp.domain.TemperatureUnit
import au.edu.jcu.assessment.utilityapp.domain.UserSettings

interface SettingsStore {
    fun read(): UserSettings
    fun write(settings: UserSettings)
}

class MemorySettingsStore(private var settings: UserSettings = UserSettings()) : SettingsStore {
    override fun read(): UserSettings = settings
    override fun write(settings: UserSettings) {
        this.settings = settings
    }
}

class PreferenceSettingsStore(private val preferences: SharedPreferences) : SettingsStore {
    override fun read(): UserSettings = UserSettings(
        city = City.entries.firstOrNull {
            it.name == preferences.getString("city", null)
        } ?: City.SINGAPORE,
        walkMinutes = preferences.getInt("walk_minutes", 30).coerceIn(1, 120),
        rainLimit = preferences.getInt("rain_limit", 30).coerceIn(0, 100),
        temperatureUnit = TemperatureUnit.entries.firstOrNull {
            it.name == preferences.getString("temperature_unit", null)
        } ?: TemperatureUnit.CELSIUS
    )

    override fun write(settings: UserSettings) {
        preferences.edit()
            .putString("city", settings.city.name)
            .putInt("walk_minutes", settings.walkMinutes)
            .putInt("rain_limit", settings.rainLimit)
            .putString("temperature_unit", settings.temperatureUnit.name)
            .apply()
    }
}
