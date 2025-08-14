package com.example.kronosclock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kronosclock.data.Watch
import com.example.kronosclock.data.WatchDao
import kotlinx.coroutines.launch

@Composable
fun WatchListScreen(
    watchDao: WatchDao,
    onCapture: (Long) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val watches by watchDao.getAll().collectAsState(initial = emptyList())
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TopAppBar(title = { Text("My Watches") }, navigationIcon = {
            Button(onClick = onBack) { Text("Back") }
        })
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(watches) { watch ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${'$'}{watch.make} ${'$'}{watch.model}")
                    Button(onClick = { onCapture(watch.id) }) { Text("Capture") }
                }
            }
        }
        OutlinedTextField(
            value = make,
            onValueChange = { make = it },
            label = { Text("Make") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (make.isNotBlank() && model.isNotBlank()) {
                    scope.launch {
                        watchDao.insert(Watch(make = make, model = model))
                        make = ""
                        model = ""
                    }
                }
            }) { Text("Add") }
        }
    }
}

