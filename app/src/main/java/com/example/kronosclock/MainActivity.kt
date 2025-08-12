@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.kronosanalogclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kronosanalogclock.ui.theme.KronosClockTheme
import com.lyft.kronos.KronosClock

class MainActivity : ComponentActivity() {
    private val kronos: KronosClock by lazy { (application as KronosApp).kronos }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var darkTheme by remember { mutableStateOf(false) }
            var dynamicColor by remember { mutableStateOf(true) }
            var ambient by remember { mutableStateOf(false) }
            var showNumerals by remember { mutableStateOf(true) }
            var accentSlot by remember { mutableStateOf(0) }

            KronosClockTheme(darkTheme = darkTheme, useDynamicColor = dynamicColor) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Kronos Analog Clock") },
                            actions = {
                                Row(Modifier.padding(end = 8.dp)) {
                                    AssistChip(
                                        onClick = { darkTheme = !darkTheme },
                                        label = { Text(if (darkTheme) "Dark" else "Light") }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    AssistChip(
                                        onClick = { dynamicColor = !dynamicColor },
                                        label = { Text("Dynamic ${if (dynamicColor) "On" else "Off"}") }
                                    )
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            FilterChip(
                                selected = ambient,
                                onClick = { ambient = !ambient },
                                label = { Text(if (ambient) "Ambient On" else "Ambient Off") }
                            )
                            FilterChip(
                                selected = showNumerals,
                                onClick = { showNumerals = !showNumerals },
                                label = { Text(if (showNumerals) "Numerals On" else "Numerals Off") }
                            )
                            FilterChip(
                                selected = accentSlot == 0,
                                onClick = { accentSlot = 0 },
                                label = { Text("Accent A") }
                            )
                            FilterChip(
                                selected = accentSlot == 1,
                                onClick = { accentSlot = 1 },
                                label = { Text("Accent B") }
                            )
                            FilterChip(
                                selected = accentSlot == 2,
                                onClick = { accentSlot = 2 },
                                label = { Text("Accent C") }
                            )
                        }

                        val timeSource: () -> Long = remember {
                            { kronos.getCurrentTimeMs() ?: System.currentTimeMillis() }
                        }

                        val scheme = MaterialTheme.colorScheme
                        val accent = when (accentSlot) {
                            1 -> scheme.secondary
                            2 -> scheme.tertiary
                            else -> scheme.primary
                        }

                        AnalogClock(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            timeSourceMs = timeSource,
                            showNumerals = showNumerals,
                            ambientMode = ambient,
                            accentColor = accent
                        )
                    }
                }
            }
        }
    }
}
