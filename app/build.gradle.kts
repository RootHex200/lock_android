plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    // NOTE (U12): apply the google-services plugin once google-services.json is
    // provisioned for the pilot Firebase project:
    //   alias(libs.plugins.google.services)
    // FCM is a wake-hint only (KTD-6); the app still works via polling without it.
}

android {
    namespace = "com.example.lock"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.lock"
        // KTD-7: raise minSdk to 26 for reliable Device Owner LockTask behaviour.
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Backend base URL + certificate public-key pin (U10 cert pinning).
        // Override the URL at build time: ./gradlew installDebug -PserverUrl=https://xxxx.ngrok-free.app
        val serverUrl = (project.findProperty("serverUrl") as String?) ?: "https://dpc.example.com"
        val certPin = (project.findProperty("certPin") as String?)
            ?: "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        buildConfigField("String", "SERVER_URL", "\"$serverUrl\"")
        buildConfigField("String", "CERT_PIN_SHA256", "\"$certPin\"")
    }

    buildTypes {
        debug {
            // U25: dev escape hatches are ONLY compiled into debug builds.
            buildConfigField("Boolean", "DEV_ESCAPE_ENABLED", "true")
            // Debug builds skip certificate pinning so you can point at a local
            // tunnel (ngrok) or dev host without a real pin. Release keeps the pin.
            buildConfigField("String", "CERT_PIN_SHA256", "\"\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // U25: release builds strip every dev escape hatch.
            buildConfigField("Boolean", "DEV_ESCAPE_ENABLED", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.kotlinx.coroutines.android)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Room + DataStore
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // FCM (wake hint only)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // libsodium (Ed25519 verify) + JNA. lazysodium-android needs the JNA *aar*
    // (bundles the native libs); exclude its transitive jna *jar* so we don't get
    // duplicate classes at packaging time.
    implementation(libs.lazysodium.android) {
        exclude(group = "net.java.dev.jna", module = "jna")
    }
    implementation(libs.jna) { artifact { type = "aar" } }

    // Jetpack Security
    implementation(libs.security.crypto)
    implementation(libs.lifecycle.service)

    // Unit tests (JVM / Robolectric)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.room.testing)

    // Instrumented tests (require a device/emulator — HITL for hardware gates)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.work.testing)
}
