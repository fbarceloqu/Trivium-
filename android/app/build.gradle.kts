import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

// Gemini se llama exclusivamente mediante Firebase Cloud Functions. Ninguna
// API key viaja en el APK; las dos claves viven como secretos del servidor.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}
// Un OAuth client ID es público (no es un secreto ni una API key). Se permite
// sobrescribir para otro proyecto desde local.properties.
val googleWebClientId: String = localProperties.getProperty(
    "GOOGLE_WEB_CLIENT_ID",
    "469846995967-cd8pigjnhv9ptkuq6ei0jp9p2n4jgcin.apps.googleusercontent.com"
)

android {
    namespace = "com.controlparental.kioscosuave"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.controlparental.kioscosuave"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0-standalone"
        vectorDrawables { useSupportLibrary = true }
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Nota: con Kotlin 2.x ya no se usa composeOptions/kotlinCompilerExtensionVersion;
    // el plugin org.jetbrains.kotlin.plugin.compose fija el compilador de Compose.
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Firebase (el BoM fija las versiones de todos los módulos firebase-*).
    // OJO: BoM 34+ requiere Kotlin 2.x; el proyecto usa Kotlin 1.9.24, por eso
    // se fija la serie 33.x (última compatible). Si algún día se sube Kotlin a
    // 2.x (con el plugin compose de Kotlin), se puede subir el BoM a 34+.
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-functions")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
