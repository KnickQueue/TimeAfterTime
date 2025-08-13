@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.kronosclock

import android.Manifest
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.kronosclock.ui.theme.KronosClockTheme
import com.example.kronosclock.data.WatchDatabase
import com.google.android.gms.location.LocationServices
import com.lyft.kronos.KronosClock
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KronosClockTheme { KronosClockApp() } }
    }
}

@Composable
private fun KronosClockApp() {
    val context = LocalContext.current
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val kronos: KronosClock = remember { KronosApp.kronosClock }
    val watchDao = remember { WatchDatabase.getInstance(context).watchDao() }
    var showWatches by remember { mutableStateOf(false) }

    var city by remember { mutableStateOf<String?>(null) }
    var zoneId by remember { mutableStateOf(ZoneId.systemDefault()) }
    var ntpNow by remember { mutableStateOf(Instant.now()) }
    var isSynced by remember { mutableStateOf(false) }
    var lastSyncStatus by remember { mutableStateOf<String?>(null) }

    val locationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchCityAndZone(context, fusedClient) { c, z ->
                city = c
                zoneId = z ?: ZoneId.systemDefault()
            }
        }
    }

    LaunchedEffect(Unit) {
        kronos.sync()
        isSynced = true
        lastSyncStatus = "NTP sync started"
    }

    LaunchedEffect(Unit) {
        while (true) {
            val ms = kronos.getCurrentTimeMs() ?: System.currentTimeMillis()
            ntpNow = Instant.ofEpochMilli(ms)
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            fetchCityAndZone(context, fusedClient) { c, z ->
                city = c
                zoneId = z ?: ZoneId.systemDefault()
            }
        } else {
            locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d uuuu").withLocale(Locale.getDefault()) }
    val timeFmt = remember { DateTimeFormatter.ofPattern("hh:mm:ss a").withLocale(Locale.getDefault()) }

    if (showWatches) {
        WatchListScreen(
            watchDao = watchDao,
            onCapture = {
                context.startActivity(Intent(context, WatchCaptureActivity::class.java))
            },
            onBack = { showWatches = false }
        )
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Kronos Clock", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            ElevatedCard(Modifier.padding(end = 8.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Date", fontWeight = FontWeight.SemiBold)
                    Text(dateFmt.withZone(zoneId).format(ntpNow))
                    Spacer(Modifier.height(12.dp))
                    Text("Time", fontWeight = FontWeight.SemiBold)
                    Text(timeFmt.withZone(zoneId).format(ntpNow), style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(12.dp))
                    Row { Text("Zone: ", fontWeight = FontWeight.SemiBold); Text(zoneId.id) }
                    city?.let { Row { Text("City: ", fontWeight = FontWeight.SemiBold); Text(it) } }
                }
            }

            Spacer(Modifier.height(16.dp))

            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("NTP Status", fontWeight = FontWeight.SemiBold)
                    Text(if (isSynced) "Sync in progress / using Kronos time when available" else "Not synced yet")
                    lastSyncStatus?.let { Text(it) }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Button(onClick = {
                            kronos.sync(); isSynced = true; lastSyncStatus = "Manual sync triggered"
                        }) { Text("Sync Now") }

                        Spacer(Modifier.width(12.dp))

                        Button(onClick = {
                            fetchCityAndZone(context, fusedClient) { c, z ->
                                city = c
                                zoneId = z ?: ZoneId.systemDefault()
                            }
                        }) { Text("Refresh Location") }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(onClick = { showWatches = true }) { Text("Manage Watches") }
        }
    }
}

@Suppress("DEPRECATION")
private fun fetchCityAndZone(
    context: Context,
    fused: com.google.android.gms.location.FusedLocationProviderClient,
    onResult: (city: String?, zone: ZoneId?) -> Unit
) {
    fused.lastLocation
        .addOnSuccessListener { loc ->
            if (loc == null) return@addOnSuccessListener onResult(null, ZoneId.systemDefault())
            val city = runCatching {
                val geocoder = Geocoder(context, Locale.getDefault())
                geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                    ?.firstOrNull()?.locality
            }.getOrNull()
            onResult(city, ZoneId.systemDefault())
        }
        .addOnFailureListener { onResult(null, ZoneId.systemDefault()) }
}
