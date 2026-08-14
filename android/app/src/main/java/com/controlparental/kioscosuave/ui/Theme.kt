package com.controlparental.kioscosuave.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * TEMA POR NIVEL EDUCATIVO
 * ========================
 *
 * Los dos lenguajes visuales comparten IDENTIDAD (tema oscuro, la misma base
 * de azul pizarra, el mismo índigo de acento) pero difieren en TEMPERATURA y
 * FORMA, que es lo que hace que uno se sienta de juego y el otro de estudio:
 *
 *   PRIMARY    acentos cálidos y saturados, esquinas muy redondeadas,
 *              superficies con más contraste. "Estoy jugando y aprendiendo."
 *
 *   SECONDARY  paleta contenida, acento solo en lo interactivo, esquinas
 *              moderadas, superficies discretas. "Es una herramienta para
 *              estudiar."
 *
 * Ambas siguen siendo inequívocamente Trivium: no cambia la marca, cambia el
 * registro.
 */

/** Nivel visual activo. Lo provee [KioscoTheme]. */
val LocalLevel = staticCompositionLocalOf { Level.SECONDARY }

// --- Base común: es lo que mantiene la identidad entre los dos niveles ---
private val Fondo = Color(0xFF020617)
private val Texto = Color(0xFFE2E8F0)
private val Indigo = Color(0xFF6366F1)

private val PrimaryColors = darkColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    // Verde más vivo: en primaria el acierto tiene que celebrarse.
    secondary = Color(0xFF22C55E),
    onSecondary = Color.White,
    // Ámbar de apoyo para lo lúdico (lectura, logros).
    tertiary = Color(0xFFF59E0B),
    background = Fondo,
    // Superficie algo más clara: las tarjetas se separan más del fondo, que
    // ayuda a un niño a distinguir "esto es una zona".
    surface = Color(0xFF16213E),
    onBackground = Texto,
    onSurface = Texto,
    error = Color(0xFFEF4444)
)

private val SecondaryColors = darkColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    // Verde más sobrio: informa que acertó, no lo festeja.
    secondary = Color(0xFF10B981),
    onSecondary = Color.White,
    tertiary = Color(0xFF38BDF8),
    background = Fondo,
    // Superficie apenas por encima del fondo: menos ruido, más foco en el texto.
    surface = Color(0xFF0F172A),
    onBackground = Texto,
    onSurface = Texto,
    error = Color(0xFFEF4444)
)

// Las formas también hablan: muy redondeado se lee como juguete; moderado, como
// herramienta. Es de los cambios que más nota un adolescente sin saber por qué.
private val PrimaryShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val SecondaryShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

/**
 * Tema de la app. [level] decide el lenguaje visual; por omisión SECONDARY,
 * que es el registro neutro (pantallas de padres, configuración).
 */
@Composable
fun KioscoTheme(
    level: Level = Level.SECONDARY,
    content: @Composable () -> Unit
) {
    val esPrimaria = level.isPrimary
    // Siempre tema oscuro: es parte de la identidad del proyecto.
    MaterialTheme(
        colorScheme = if (esPrimaria) PrimaryColors else SecondaryColors,
        shapes = if (esPrimaria) PrimaryShapes else SecondaryShapes
    ) {
        CompositionLocalProvider(LocalLevel provides level, content = content)
    }
}
