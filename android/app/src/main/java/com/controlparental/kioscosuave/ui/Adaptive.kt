package com.controlparental.kioscosuave.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Puente entre el sistema de dimensiones ([AdaptiveMetrics], Kotlin puro) y
 * Compose. Aquí solo se convierten números a `dp`/`sp`; toda la lógica de
 * tamaños vive en AdaptiveMetrics.kt para poder probarla fuera del emulador.
 */

/**
 * Métricas de la pantalla actual. Las provee [KioskScreen] a partir del espacio
 * REAL disponible (no de la configuración global), así que también es correcto
 * dentro de un panel o en pantalla dividida.
 */
val LocalMetrics = staticCompositionLocalOf {
    // Valor de respaldo: tablet mediana. Solo se usa si alguien olvida proveer.
    Adaptive.metrics(600f, 900f, big = false)
}

@Composable
@ReadOnlyComposable
fun metrics(): AdaptiveMetrics = LocalMetrics.current

/** Azúcar para no repetir `.dp` / `.sp` en cada uso. */
val Float.adp: Dp get() = this.dp
val Float.asp: TextUnit get() = this.sp
