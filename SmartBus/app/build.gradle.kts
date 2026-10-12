import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// La clave de Google Maps se lee de local.properties para no dejarla
// escrita en el repositorio.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun requireLocalProperty(name: String): String =
    localProperties.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }
        ?: error(
            "Falta $name en local.properties. " +
                "No se escribe en el codigo: cada clave va en tu maquina."
        )

val mapsApiKey: String = requireLocalProperty("MAPS_API_KEY")

// Client ID de OAuth 2.0 tipo Web de Google. Sin esto el boton de
// iniciar sesion con Google no puede funcionar, pero la app sigue
// arrancando en modo invitado.
val googleWebClientId: String =
    localProperties.getProperty("GOOGLE_WEB_CLIENT_ID") ?: ""

android {
    namespace = "com.ejemplo.smartbus"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ejemplo.smartbus"
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        manifestPlaceholders["GOOGLE_WEB_CLIENT_ID"] = googleWebClientId
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        // Necesario para separar larevision documental simulada, que
        // solo debe existir en debug, de una revision real de backend.
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Google Maps para Jetpack Compose
    // Google Maps para Jetpack Compose
// Google Maps para Jetpack Compose
    implementation("com.google.maps.android:maps-compose:4.3.3")

    // Servicios de Ubicación GPS
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.datastore:datastore-preferences:1.1.0")
    implementation("com.google.zxing:core:3.5.3")

    // Inicio de sesion con Google
    implementation(libs.play.services.auth)

    // Base de datos local Room: paradas, rutas y posicion de los buses
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}