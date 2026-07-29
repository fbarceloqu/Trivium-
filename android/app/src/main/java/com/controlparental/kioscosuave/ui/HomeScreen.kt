package com.controlparental.kioscosuave.ui

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class LaunchableApp(
    val label: String,
    val packageName: String,
    val icon: Drawable
)

/** Paquetes preferidos para el dock inferior (se usan los que estén instalados). */
private val DOCK_PREFERRED = listOf(
    "com.android.chrome",
    "com.sec.android.app.sbrowser",   // Samsung Internet
    "com.google.android.youtube",
    "com.whatsapp",
    "com.sec.android.app.camera",     // Cámara Samsung
    "com.sec.android.gallery3d",      // Galería Samsung
    "com.duolingo"
)

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
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map {
                LaunchableApp(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    icon = it.loadIcon(pm)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    val dockApps = remember(apps) {
        val preferred = DOCK_PREFERRED.mapNotNull { pkg -> apps.find { it.packageName == pkg } }
        (preferred + apps.filter { it !in preferred }).take(5)
    }
    val gridApps = remember(apps) { apps.filter { it !in dockApps } }

    var query by remember { mutableStateOf("") }
    val visibleApps =
        if (query.isBlank()) gridApps
        else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }

    // Reloj estilo One UI (se refresca cada 20 segundos)
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(20_000)
        }
    }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEEE d 'de' MMMM", Locale("es")) }

    fun launch(app: LaunchableApp) {
        ctx.packageManager.getLaunchIntentForPackage(app.packageName)
            ?.let { ctx.startActivity(it) }
    }

    // Fondo tipo wallpaper One UI (degradado azul profundo)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0E1A33), Color(0xFF15264D), Color(0xFF0B1220))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {

            // Fila superior discreta: saludo + candado padres + bloquear
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "✅ $childName · tareas listas",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onLockAgain) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Bloquear tablet",
                        tint = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Reloj grande estilo One UI
            Text(
                timeFmt.format(now),
                fontSize = 64.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                dateFmt.format(now).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(18.dp))

            // Barra de búsqueda tipo "Finder" de Samsung
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                "Buscar aplicaciones",
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 14.sp
                            )
                        }
                        inner()
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            // Cuadrícula de apps (limpia, sin fondos: los íconos Samsung ya son squircles)
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                items(visibleApps, key = { it.packageName }) { app ->
                    AppIcon(app, iconSize = 58) { launch(app) }
                }
            }

            // Dock inferior translúcido estilo One UI
            if (query.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
                        .padding(vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        dockApps.forEach { app ->
                            AppIcon(app, iconSize = 54, showLabel = false) { launch(app) }
                        }
                    }
                }
            }
        }

        // Botón flotante discreto de acceso de padres (esquina inferior derecha)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 90.dp)
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.07f), CircleShape)
                .clickable(onClick = onParentAccess),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = "Acceso de padres",
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AppIcon(
    app: LaunchableApp,
    iconSize: Int,
    showLabel: Boolean = true,
    onClick: () -> Unit
) {
    val bitmap = remember(app.packageName) {
        app.icon.toBitmap(width = 144, height = 144).asImageBitmap()
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = app.label,
            modifier = Modifier.size(iconSize.dp)
        )
        if (showLabel) {
            Spacer(Modifier.height(5.dp))
            Text(
                app.label,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
