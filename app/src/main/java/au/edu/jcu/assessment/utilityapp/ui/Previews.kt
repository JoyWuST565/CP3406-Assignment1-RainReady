package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.RainReadyUiState
import au.edu.jcu.assessment.utilityapp.domain.City
import au.edu.jcu.assessment.utilityapp.domain.HourForecast
import au.edu.jcu.assessment.utilityapp.domain.UserSettings
import au.edu.jcu.assessment.utilityapp.domain.WeatherForecast
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Preview(name = "Utility · light", showBackground = true, widthDp = 393, heightDp = 850)
@Preview(name = "Utility · dark", showBackground = true, widthDp = 393, heightDp = 850, uiMode = 0x20)
@Composable
private fun UtilityPreview() {
    val now = Instant.parse("2026-10-29T02:15:00Z")
    val zone = ZoneId.of("Asia/Singapore")
    val start = LocalDateTime.ofInstant(now, zone).withMinute(0)
    val forecast = WeatherForecast(
        city = City.SINGAPORE,
        hours = List(8) { index -> HourForecast(start.plusHours(index.toLong()), 29.0 + index * 0.3, listOf(20, 10, 10, 45, 65, 40, 20, 15)[index], 2) },
        timeZone = zone,
        fetchedAt = now.minusSeconds(120)
    )
    MobileTech_UtilityAppTheme {
        Column {
            Text(stringResource(R.string.preview_label))
            UtilityScreen(RainReadyUiState(UserSettings(), forecast, false), {}, {}, now)
        }
    }
}

@Preview(name = "Settings · large text", showBackground = true, widthDp = 393, heightDp = 850, fontScale = 1.5f)
@Composable
private fun SettingsPreview() {
    MobileTech_UtilityAppTheme { SettingsScreen(UserSettings(), {}) }
}
