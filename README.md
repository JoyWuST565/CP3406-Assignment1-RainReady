# RainReady

**Shuntong WU · 14772395**  
**CP3406 Mobile Computing · Assignment 1: Utility App**

Repository: https://github.com/JoyWuST565/CP3406-Assignment1-RainReady

RainReady helps someone planning a short walk find an earlier, lower-rain opportunity without interpreting a full weather dashboard. It checks the next six hours and suggests the earliest departure whose entire walking duration meets the selected hourly rain-probability limit. The result is a forecast-based suggestion, not a guarantee of dry conditions.

## Using the app

1. Open **Settings** and choose Singapore, Townsville, Brisbane or Melbourne.
2. Choose a **15-, 30- or 60-minute** walk and a maximum rain chance of **10%, 30% or 50%**. Choose Celsius or Fahrenheit. Changes save automatically.
3. Return to **Utility**. The main card shows a suggested departure and return time, the highest hourly rain probability encountered, and the time-weighted mean forecast temperature. If no complete walk qualifies, the app explains that result.
4. Swipe the hourly outlook horizontally to inspect the forecast periods. Each card shows a time, temperature, weather description, rain probability and whether it meets the current limit.
5. Use the refresh icon to request a new forecast. The retrieval time, location time zone and Open-Meteo source link appear below the outlook.

The app evaluates departure now and then each following whole hour. A departure qualifies only if every forecast hour touched by the walk is covered and its probability is **at or below** the chosen limit. Thunderstorm periods are excluded. A walk must finish within six hours of the evaluation time. Probabilities are not added together or represented as a whole-walk probability.

## Screens and behaviour

- **Utility:** latest forecast, walking suggestion, hourly outlook, refresh, loading state and useful retry messages.
- **Settings:** four city choices, walking duration, rain tolerance and temperature units. Preferences persist on the device; the selected tab survives activity recreation.
- A failed refresh retains the previous successful forecast for the same city and displays an error. A location change immediately clears the old city's forecast.
- Forecasts at least **60 minutes old** remain visible for reference but produce no departure suggestion until refreshed.
- The interface supports system light and dark themes, scrolling, wrapping text, descriptive accessibility labels and touch controls of at least 48 dp. No sample forecast substitutes for a failed live request. Illustrative data exists only in development previews and tests.

## Build and run

Requirements: **JDK 21**, Android SDK **36**, and an Android device or emulator running **Android 8.0 / API 26 or later**. The project uses Android Gradle Plugin **8.13.2**, Kotlin **2.0.21**, Gradle **8.13** and Jetpack Compose with Material 3. The application package is `au.edu.jcu.assessment.utilityapp`.

1. Open this repository's root folder in Android Studio; it contains `settings.gradle.kts` and the Gradle wrapper.
2. Set the Gradle JDK to JDK 21, install Android SDK 36 if prompted, and allow Gradle to synchronise dependencies.
3. Select the `app` run configuration and an API 26+ device or emulator, then run.
4. Allow internet access on the device. The forecast service requires no API key for this educational use.

From Windows PowerShell in the repository root:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

To run device tests with one connected emulator or device:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

On macOS or Linux, use `sh ./gradlew` in place of `.\gradlew.bat`. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. `local.properties` is machine-specific and is excluded from version control; Android Studio creates it for the local SDK installation.

## Implementation

| Location | Responsibility |
| --- | --- |
| `MainActivity.kt` | Creates the Compose content and collects ViewModel state with lifecycle awareness. |
| `RainReadyApplication.kt` | Builds Retrofit, the repository, preference storage and the ViewModel factory. |
| `RainReadyViewModel.kt` | Coordinates loading, refresh, errors, settings, cancellation and response ordering. |
| `domain/` | Immutable forecast/settings models, weather descriptions and the independent walking-window algorithm. |
| `data/` | Open-Meteo Retrofit service, validated response mapping and preference storage. |
| `ui/` | Two-tab screen composition, accessible forecast cards, settings and development previews. |
| `ui/theme/` and `res/` | Light/dark colours, typography, strings and umbrella launcher artwork. |
| `app/src/test/` | 34 focused local tests for planning, response mapping, HTTP integration and state behaviour. |
| `app/src/androidTest/` | Six Compose interaction tests and the retained starter application-context smoke test. |
| `docs/` | [Architecture](docs/ARCHITECTURE.md) and [testing details](docs/TESTING.md). |

Source files reside below `app/src/main/java/au/edu/jcu/assessment/utilityapp/` unless otherwise indicated. Gradle configuration and wrapper files make the project reproducible without committing local caches or build outputs.

## Data, attribution and privacy

Weather data are supplied by **[Open-Meteo](https://open-meteo.com/)** under **[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)**. The app uses the [Weather Forecast API documentation](https://open-meteo.com/en/docs) and the free service for this educational, non-commercial project, subject to [Open-Meteo's terms](https://open-meteo.com/en/terms).

Requests use HTTPS and the selected city coordinates. The only declared permission is `INTERNET`; the app requests no GPS or location permission and needs no account. Preferences are stored locally using SharedPreferences and saved state. The forecast is held in memory and fetched again after a fresh process launch. Open-Meteo receives the network request and handles it under its own terms and privacy policy.

Open-Meteo labels hourly precipitation probability for the preceding hour. The mapper associates the next response probability with the start of the corresponding domain interval. Temperatures and weather codes remain hourly samples. Epoch timestamps are converted using the returned location time zone; ambiguous or nonexistent local walking times are handled conservatively. See the architecture document for the exact interval rules.

## Limitations

The four selected city locations represent a forecast grid area rather than a walking route. Hourly probabilities cannot identify the exact minute rain will begin or provide a combined probability for an entire walk. The app does not evaluate heat, air quality, flooding or official severe-weather warnings. Check local conditions before leaving. There is no background alert service, historical forecast archive or persistent offline forecast database.

## Starter provenance

This project extends the supplied **MobileTech Utility App** starter for Assessment 1, retaining its package and the `UtilityApp`, `UtilityScreen` and `SettingsScreen` concepts. The starter states that it is provided for educational use in CP3406 and may be modified and extended for the assessment. This submission uses it for that educational purpose; it does not assert a broader licence over the supplied template.

