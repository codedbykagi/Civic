// Android app: Jetpack Compose UI, CameraX capture, GPS tagging, offline record (Room).
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.civic.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.civic.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 10.0.2.2 = host machine's localhost from the Android emulator
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080\"")

        // Walking-route servers (free FOSSGIS instances: max 1 request/s, prototype use only, not for production).
        // Kept in build config rather than code, as FOSSGIS asks, so they can be swapped for a self-hosted server.
        buildConfigField("String", "VALHALLA_URL", "\"https://valhalla1.openstreetmap.de/route\"")
        buildConfigField("String", "OSRM_FOOT_URL", "\"https://routing.openstreetmap.de/routed-foot/route/v1/foot\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // TODO: point to production backend URL
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
}

// Room writes each DB version's schema here, so future migrations can be checked against the real history.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":shared"))

    // Core / lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Compose UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Camera + location
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.play.services.location)

    // In-app map (OpenStreetMap tiles, no API key)
    implementation(libs.osmdroid.android)

    // Local record of reports (offline drafts / history)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Networking + images
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.json)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
}
