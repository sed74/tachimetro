import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Release signing credentials live outside version control (keystore.properties is
// gitignored; see keystore.properties.example for the expected fields). When absent,
// release builds stay unsigned instead of failing the build.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseSigning = keystorePropertiesFile.exists()

android {
    namespace = "com.sed.tachimetro"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.sed.tachimetro"
        minSdk = 30
        targetSdk = 36
        versionCode = 4
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    // D-01 (Fase 12): flavor TEMPORANEI dello spike Surface, rimossi a fine fase (D-02).
    // D-05: mai distribuiti, nessun caricamento Play Store nemmeno su test interno: solo APK
    // debug locali su DHU. Ciascun flavor ha il proprio manifest (src/spikePoi, src/spikeNav)
    // cosi' nessun APK dichiara insieme MAP_TEMPLATES e NAVIGATION_TEMPLATES (T-12-01).
    // Comandi: ./gradlew.bat :app:installSpikePoiDebug [-PpoiCard=message|list|pane|grid]
    //          ./gradlew.bat :app:installSpikeNavDebug
    flavorDimensions += "surfaceSpike"
    productFlavors {
        create("spikePoi") {
            dimension = "surfaceSpike"
            // D-06: variante della card obbligatoria di MapWithContentTemplate, scelta da riga
            // di comando. Allow-list: nessuna stringa arbitraria finisce in BuildConfig (T-12-03).
            val allowedPoiCards = setOf("message", "list", "pane", "grid")
            val poiCard = project.findProperty("poiCard")?.toString() ?: "message"
            if (poiCard !in allowedPoiCards) {
                throw GradleException(
                    "Valore -PpoiCard='$poiCard' non valido. Valori ammessi: " +
                        allowedPoiCards.joinToString(", ")
                )
            }
            buildConfigField("String", "SPIKE_POI_CARD", "\"$poiCard\"")
        }
        create("spikeNav") {
            dimension = "surfaceSpike"
            // Nessuna card nel percorso NAVIGATION: il campo esiste solo perche' il codice in
            // main compili per entrambe le varianti.
            buildConfigField("String", "SPIKE_POI_CARD", "\"none\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // AGP 8+ no longer generates BuildConfig by default. Enabled here so Piano 02 can gate
    // the car-screen refresh-count diagnostic log behind BuildConfig.DEBUG, keeping speed
    // values out of logcat in release builds (T-08-03).
    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.car.app)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.car.app.testing)
}