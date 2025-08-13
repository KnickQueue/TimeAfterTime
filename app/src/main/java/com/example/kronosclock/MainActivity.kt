@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.kronosclock

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.kronosclock.ui.theme.KronosClockTheme
import com.google.android.gms.location.LocationServices
import com.lyft.kronos.KronosClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val kronosClock: KronosClock =
            (application as KronosApp).kronosClock

        setContent {
            KronosClockTheme {
                MainScreen(kronosClock = kronosClock)
            }
        }
    }
}

@Composable
private fun MainScreen(kronosClock: KronosClock) {
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }

    var hasLocationPerm by remember { mutableStateOf(isLocationGranted(context)) }
    var locality by remember { mutableStateOf<String?>(null) }
    var country by remember { mutableStateOf<String?>(null) }
    var zoneId by remember { mutableStateOf(ZoneId.systemDefault()) }
    var kronosNow by remember { mutableStateOf(kronosClock.now()) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasLocationPerm = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Update Kronos time periodically (Kronos syncs in background from Application)
    LaunchedEffect(Unit) {
        kronosNow = kronosClock.now()
    }

    // Fetch location → locality/country + keep current ZoneId (system)
    LaunchedEffect(hasLocationPerm) {
        if (!hasLocationPerm) return@LaunchedEffect
        val loc = try {
            withContext(Dispatchers.IO) { fused.lastLocation.awaitOrNull() }
        } catch (_: Exception) { null }

        if (loc != null) {
            // Best-effort reverse geocode for a friendly name
            val geo = Geocoder(context, Locale.getDefault())
            val addr = withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geo.getFromLocation(loc.latitude, loc.longitude, 1)
                    ?.firstOrNull()
            }
            locality = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
            country = addr?.countryName
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Kronos Clock") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnalogClock()

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Zone: ${zoneId.id}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Kronos: ${kronosNow?.toDate()?.toString() ?: "syncing…"}",
                style = MaterialTheme.typography.bodyMedium
            )

            locality?.let { city ->
                Text(
                    text = buildString {
                        append(city)
                        country?.let { append(", $it") }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (!hasLocationPerm) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    permLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }) {
                    Text("Enable Location")
                }
            }
        }
    }
}

/* ---------- Helpers ---------- */

private fun isLocationGranted(context: android.content.Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

// Lightweight suspend wrapper for FusedLocationProviderClient.lastLocation
private suspend fun com.google.android.gms.location.FusedLocationProviderClient.awaitOrNull()
        : android.location.Location? = withContext(Dispatchers.IO) {
    try {
        val task = lastLocation
        kotlinx.coroutines.suspendCancellableCoroutine<android.location.Location?> { cont ->
            task.addOnSuccessListener { cont.resume(it, onCancellation = null) }
            task.addOnFailureListener { cont.resume(null, onCancellation = null) }
        }
    } catch (_: Exception) { null }
}
