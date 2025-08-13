@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.kronosanalogclock

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.kronosanalogclock.ui.theme.KronosClockTheme
import com.google.android.gms.location.LocationServices
import com.lyft.kronos.KronosClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone as JavaTimeZone
import android.icu.util.TimeZone as IcuTimeZone
import kotlin.coroutines.resume

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
            var zoneId by remember { mutableStateOf(ZoneId.systemDefault()) }

            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    scope.launch { detectTimeZone(context)?.let { zoneId = it } }
                }
            }

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

                        TimeZoneSelector(
                            zoneId = zoneId,
                            onZoneChange = { zoneId = it },
                            onDetect = {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    scope.launch {
                                        detectTimeZone(context)?.let { zoneId = it }
                                    }
                                } else {
                                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            }
                        )

                        AnalogClock(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            timeSourceMs = timeSource,
                            zoneId = zoneId,
                            showNumerals = showNumerals,
                            ambientMode = ambient,
                            accentColor = accent
                        )

                        Button(onClick = {
                            context.startActivity(Intent(context, WatchCaptureActivity::class.java))
                        }) {
                            Text("Track Watch")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeZoneSelector(
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

@SuppressLint("MissingPermission")
private suspend fun detectTimeZone(context: Context): ZoneId? = withContext(Dispatchers.IO) {
    val client = LocationServices.getFusedLocationProviderClient(context)
    val location = suspendCancellableCoroutine<android.location.Location?> { cont ->
        client.lastLocation
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resume(null) }
    }
    location ?: return@withContext null
    val geocoder = Geocoder(context, Locale.getDefault())
    val address = try {
        geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
    } catch (_: Exception) {
        null
    }
    val ids = address?.countryCode?.let { IcuTimeZone.getAvailableIDs(it) }
    return@withContext ids?.firstOrNull()?.let { ZoneId.of(it) }
}
