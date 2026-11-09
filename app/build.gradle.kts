import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.play.publisher)
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// The Play Developer API key lives with the signing key, in a folder git ignores.
val playCredentialsFile = rootProject.file("keystore/play-service-account.json")

android {
    namespace = "com.speedcontact.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.speedcontact.app"
        minSdk = 26
        // Google Play's current minimum. Targeting 37 would need a contacts declaration from
        // 27 January 2027, and Android 17 cannot yet be tested here.
        targetSdk = 36
        versionCode = 11
        versionName = "1.4.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Its signature differs from release, so it needs its own id to sit beside it.
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    androidResources {
        generateLocaleConfig = true
        localeFilters += listOf("en", "ar", "ur", "hi", "in", "tr", "es", "pt")
    }

    bundle {
        language {
            // People can choose a language their phone is not set to, which Play would not have sent.
            enableSplit = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Publishes to Google Play from the command line; see docs/PLAY-PUBLISHING-AR.md.
// Off without the key, so a normal build never needs it.
play {
    enabled.set(playCredentialsFile.isFile)
    if (playCredentialsFile.isFile) {
        serviceAccountCredentials.set(playCredentialsFile)
    }
    defaultToAppBundles.set(true)
    // Safe defaults: a draft on the internal track. Anything wider is asked for by name.
    track.set("internal")
    releaseStatus.set(ReleaseStatus.DRAFT)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)

    implementation(libs.datastore)
    implementation(libs.work.runtime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.reorderable)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
