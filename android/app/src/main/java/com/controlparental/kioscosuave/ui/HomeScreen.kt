package com.controlparental.kioscosuave.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private data class LaunchableApp(val label: String, val packageName: String)

@Composable
fun HomeScreen(
    childName: String,
    onLockAgain: () -> Unit,
    onParentAccess: () -> Unit
) {
    val ctx = LocalContext.current
    val apps = remember {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .map { LaunchableApp(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .filter { it.packageName != ctx.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("¡Tablet desbloqueada!", style = MaterialTheme.typography.titleMedium)
                Text(
                    "$childName completó sus tareas de hoy 🎉",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = onParentAccess) {
                Icon(Icons.Filled.Lock, contentDescription = "Acceso de padres")
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Aplicaciones", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(apps) { app ->
                OutlinedButton(
                    onClick = {
                        ctx.packageManager.getLaunchIntentForPackage(app.packageName)
                            ?.let { ctx.startActivity(it) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(app.label) }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onLockAgain, modifier = Modifier.fillMaxWidth()) {
            Text("Bloquear tablet / fin de sesión")
        }
    }
}
