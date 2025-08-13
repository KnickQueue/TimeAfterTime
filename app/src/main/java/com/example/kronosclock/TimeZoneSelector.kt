@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.kronosclock

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import java.util.TimeZone as JavaTimeZone

@Composable
fun TimeZoneSelector(
    zoneId: ZoneId,
    onZoneChange: (ZoneId) -> Unit,
    onDetect: () -> Unit
) {
    val zones = remember { JavaTimeZone.getAvailableIDs().sorted() }
    var expanded by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                readOnly = true,
                value = zoneId.id,
                onValueChange = {},
                label = { Text("Timezone") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                zones.forEach { id ->
                    DropdownMenuItem(
                        text = { Text(id) },
                        onClick = {
                            onZoneChange(ZoneId.of(id))
                            expanded = false
                        }
                    )
                }
            }
        }
        Button(onClick = onDetect) { Text("Detect via GPS") }
    }
}