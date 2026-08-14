package com.strive.antiqum.museums.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.strive.antiqum.designsystem.AntiqumDimens
import com.strive.antiqum.designsystem.AntiqumSectionLabel
import com.strive.antiqum.designsystem.ThemeMode

@Composable
fun SettingsScreen(
    appState: AntiqumAppState,
    onThemeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AntiqumDimens.ScreenPadding, vertical = 24.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.displayMedium)
        Text(
            "Make Antiqum feel like yours.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(28.dp))
        SettingsGroup(title = "General") {
            SettingRow("Location", "Zagreb, Croatia")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            SettingRow("Units / Distance", "Kilometres")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            SettingRow("Language", "English")
        }

        Spacer(Modifier.height(26.dp))
        SettingsGroup(title = "Appearance") {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ThemeMode.entries.forEach { themeMode ->
                        val selected = appState.themeMode == themeMode
                        Text(
                            themeMode.name,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.outline else androidx.compose.ui.graphics.Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onThemeSelected(themeMode) }
                                .padding(vertical = 9.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(26.dp))
        SettingsGroup(title = "About Antiqum") {
            SettingRow("Privacy Policy")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            SettingRow("Terms & Conditions")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            SettingRow("Support")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            SettingRow("Contact")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            SettingRow("Report a problem")
        }

        Spacer(Modifier.height(30.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Antiqum", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Version 1.0.0",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Museum information from Wikidata",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    AntiqumSectionLabel(title, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingRow(label: String, value: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().height(57.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        value?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.width(5.dp))
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
