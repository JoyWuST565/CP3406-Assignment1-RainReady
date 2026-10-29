package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.domain.City
import au.edu.jcu.assessment.utilityapp.domain.TemperatureUnit
import au.edu.jcu.assessment.utilityapp.domain.UserSettings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(settings: UserSettings, onSettingsChange: (UserSettings) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.settings_heading),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() }
                )
                Text(stringResource(R.string.settings_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading(stringResource(R.string.city_heading), stringResource(R.string.city_description))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Column(Modifier.selectableGroup().padding(vertical = 5.dp)) {
                        City.entries.forEach { city ->
                            Row(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                    .selectable(
                                        selected = settings.city == city,
                                        onClick = { onSettingsChange(settings.copy(city = city)) },
                                        role = Role.RadioButton
                                    ).padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(selected = settings.city == city, onClick = null)
                                Text(city.label, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeading(stringResource(R.string.duration_heading), stringResource(R.string.duration_description))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(15, 30, 60).forEach { minutes ->
                        FilterChip(
                            selected = settings.walkMinutes == minutes,
                            onClick = { onSettingsChange(settings.copy(walkMinutes = minutes)) },
                            label = { Text(stringResource(R.string.duration_option, minutes)) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeading(stringResource(R.string.rain_heading), stringResource(R.string.rain_description))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(10, 30, 50).forEach { probability ->
                        FilterChip(
                            selected = settings.rainLimit == probability,
                            onClick = { onSettingsChange(settings.copy(rainLimit = probability)) },
                            label = { Text(stringResource(R.string.rain_option, probability)) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeading(stringResource(R.string.units_heading), stringResource(R.string.units_description))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TemperatureUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = settings.temperatureUnit == unit,
                            onClick = { onSettingsChange(settings.copy(temperatureUnit = unit)) },
                            label = {
                                Text(stringResource(if (unit == TemperatureUnit.CELSIUS) R.string.celsius else R.string.fahrenheit))
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.settings_saved), style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SectionHeading(stringResource(R.string.how_it_works_heading), stringResource(R.string.how_it_works_body))
            SectionHeading(stringResource(R.string.privacy_heading), stringResource(R.string.privacy_body))
        }
    }
}

