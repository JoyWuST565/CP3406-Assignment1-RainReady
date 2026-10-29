# Testing

The test suite targets the decisions that can produce a misleading walking suggestion, the boundary between a remote response and usable forecast data, and the actions available when retrieval succeeds or fails. Local tests use controlled time and fake or local HTTP responses so their expected results do not depend on today's weather.

## Final verification: 29 October 2026

Verification used **JDK 21.0.12.1** and **Android SDK Platform 36** with the repository's pinned Gradle, Android Gradle Plugin and Kotlin versions. Device execution used the **CP3406_Test_API37** x86_64 emulator running **Android 17 / API 37** on Windows 11. After Windows was restarted, `emulator -accel-check` returned **status 0** and reported **WHPX 10.0.26100 installed and usable**; Windows also reported an active hypervisor. The virtual device completed boot in approximately 38 seconds.

| Check | Observed result |
| --- | --- |
| `testDebugUnitTest` | **34 tests passed; 0 failures, errors or skipped tests.** The XML results contain 17 planner, 8 repository and 9 ViewModel tests. |
| `assembleDebug` | **Passed.** The debug application APK was assembled. |
| `lintDebug` | **Passed with 0 errors and 29 warnings.** |
| `assembleDebugAndroidTest` | **Passed.** The device-test APK, including all seven instrumented tests, compiled and packaged. |
| `connectedDebugAndroidTest` | **7 tests passed; 0 failures and 0 skipped tests.** This includes all six Compose interaction tests and the retained package-context test. |
| Live API response check | A separate Singapore request returned **48 hourly response rows** and the expected **Asia/Singapore** time zone. This checked the real endpoint and response shape outside the Android interface. |
| Installed application | Live forecasts, saved preferences after process restart, offline failure and retry recovery, landscape Settings, and both screens in dark mode with 200 percent font size were checked on the final APK. |

The final combined Gradle run executed `testDebugUnitTest assembleDebug lintDebug connectedDebugAndroidTest` and completed with `BUILD SUCCESSFUL`. All 34 JVM tests and all seven device tests passed, with no failed or skipped tests. The 29 Lint warnings comprise 16 plugin/dependency update notifications, 9 version-catalog style suggestions, and one each for an obsolete API-level resource qualifier, a plural-resource candidate, an unused colour and a Kotlin-extension style suggestion. The plural-resource candidate is a percentage label. The verified version pins are retained rather than introducing major dependency upgrades solely to remove update notifications.

Device tests execute controlled forecast fixtures. The additional observations below concern the installed application and its real network requests. They provide separate evidence of runtime behaviour on this emulator; they do not establish behaviour on every physical Android device.

## Local tests

There are **34 JVM tests** in `app/src/test/`.

| Test class | Count | Coverage |
| --- | ---: | --- |
| `WalkPlannerTest` | 17 | Inclusive thresholds, departure now, partial final hours, exact boundaries, full coverage, storms, no qualifying result, six-hour deadline, weighted temperature, mismatched cities, duplicate times, invalid settings, unknown codes, ordering, a daylight-saving gap, an ambiguous repeated hour and an elapsed-time horizon across a clock change. |
| `WeatherRepositoryTest` | 8 | Time-zone conversion, preceding-hour probability normalisation, mismatched or absent arrays, invalid zones, missing probabilities, unusable data, selected-city request parameters, injectable retrieval time, and Retrofit decoding through MockWebServer. |
| `RainReadyViewModelTest` | 9 | Initial loading/success, failure and retry, retained data after a failed refresh, persistence without unnecessary requests, city changes, saved-state restoration, stale response ordering, repeated refresh ordering and mismatched response cities. |

Run all local tests:

```powershell
.\gradlew.bat testDebugUnitTest
```

The HTML report is written to `app/build/reports/tests/testDebugUnitTest/index.html`; machine-readable results are under `app/build/test-results/testDebugUnitTest/`.

## Android and Compose tests

`RainReadyUiTest` contains **six Compose tests**:

1. Changing city, duration, rain limit and units updates the selected preferences.
2. A fresh forecast displays a departure-now suggestion and exposes a working refresh action.
3. A forecast older than one hour hides the departure recommendation and asks for refresh.
4. A failed initial request exposes an understandable unavailable state and a working retry action.
5. The Settings tab opens the settings screen.
6. A qualifying departure at 11:00, when the current time is 10:59:30 and the preceding interval is unsuitable, is labelled as a later departure rather than departure now.

The supplied starter's `ExampleInstrumentedTest` also checks the application package through the Android context. There are therefore seven device tests in total, six of which exercise the implemented interface. Test fixtures are deterministic and do not request real weather.

Compile the device-test APK:

```powershell
.\gradlew.bat assembleDebugAndroidTest
```

Execute it with one Android API 26+ emulator or device connected:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

The connected-device HTML report is written below `app/build/reports/androidTests/connected/`, with machine-readable results below `app/build/outputs/androidTest-results/connected/`. The recorded seven-test pass is based on execution on the emulator, rather than test-APK compilation alone.

## Build and static analysis

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

This runs local tests, Android Lint, debug application assembly and device-test compilation. Lint output is generated under `app/build/reports/`, including `lint-results-debug.html`. The application APK is `app/build/outputs/apk/debug/app-debug.apk`.

Use JDK 21 and Android SDK 36. On macOS or Linux, replace `.\gradlew.bat` with `sh ./gradlew`. The wrapper downloads the declared Gradle version, and the initial build also requires access to the configured dependency repositories.

## Device verification scenarios

### Observed installed-application behaviour

The actual Utility screen successfully retrieved the selected city's live forecast. Singapore displayed a no-qualifying-window result for forecast intervals with rain probabilities of at least 57 percent and thunderstorms. This was a real service response, rather than preview or test data.

Brisbane and Melbourne also loaded live forecasts with the correct city-local labels, completing checks of all four cities; Melbourne displayed the Australia/Melbourne time zone, Open-Meteo attribution and update timestamp, and its hourly outlook scrolled horizontally to later intervals.

In Settings, **Townsville**, **60 minutes**, a **10 percent** rain limit and **Fahrenheit** were selected. The Utility screen reflected these preferences. Inspection of the app's private preferences confirmed all four values. After stopping and relaunching the app process, the same settings remained selected and a fresh network request succeeded. Townsville displayed a departure-now recommendation covering the complete one-hour walk, with **0 percent** rain probability and **84 °F**. These forecast values document the observed response and can change on later requests.

With network access disabled after a successful request, refreshing retained the previous forecast and displayed a visible retrieval failure. Stopping and relaunching the app while still offline displayed **Forecast unavailable** and **Try again**, without inventing forecast data. Restoring connectivity and retrying recovered a live forecast.

The Settings screen remained usable in landscape orientation. In dark mode with the system font size set to **200 percent**, both Utility and Settings remained scrollable and usable, with no observed clipping. These checks used the installed final APK on the emulator. A screen-reader session and a physical-device review were not performed.

### Repeatable review scenarios

The following scenarios define the broader runtime review. Expected behaviour is listed separately from the completed observations and deterministic automated coverage above.

| Scenario | Expected behaviour |
| --- | --- |
| Fresh launch with internet access | Visible loading followed by the selected city's real forecast or a specific retrieval error. |
| No connection on first launch | No fabricated forecast; unavailable state and retry remain accessible. |
| Connection lost after a successful request | The previous same-city forecast remains visible with an error and its original retrieval time. |
| Switch city while a request is slow | The old city's data disappears immediately; a late old response cannot replace the latest selection. |
| Change units or walking preferences | The display or recommendation updates without another forecast request. |
| Rotate or recreate the activity | The selected tab and preferences survive recreation. |
| Restart the app process | Preferences remain saved; a new forecast request is made. |
| Leave the app open for at least 60 minutes without refreshing | The old forecast remains readable; no departure suggestion is offered. |
| Dark appearance and large system font | Both screens remain scrollable, meaningful labels wrap, and controls remain reachable. |
| Screen reader use | Tabs and controls have meaningful labels; each hourly card is read as a combined description. |

Development previews include a clearly labelled illustrative forecast and a settings preview at increased font size. Previews assist layout inspection; they do not replace runtime verification or live API checks.

## Interpreting results

A successful local suite verifies the tested mapping, algorithm and state cases; it cannot establish forecast accuracy. A successful app build establishes compilation and packaging, while device execution verifies the tested UI interactions on that device configuration. Neither automated suite claims that a future walk will remain dry. The app communicates that limitation in its normal interface.


