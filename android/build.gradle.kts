// Top-level build file. Versiones probadas y estables (early 2026).
plugins {
    id("com.android.application") version "8.5.2" apply false
    // Kotlin 2.x: requerido por los SDK de Firebase modernos (compilados con
    // metadata Kotlin 2.1). Con Kotlin 2.x, Compose usa su propio plugin.
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    // Plugin de Google Services: lee google-services.json para conectar Firebase
    id("com.google.gms.google-services") version "4.5.0" apply false
}
