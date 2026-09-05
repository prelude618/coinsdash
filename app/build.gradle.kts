import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    id("com.google.gms.google-services")
}

val signingPropertiesPath = providers.gradleProperty("coinsdashSigningProperties").orNull
    ?: providers.environmentVariable("COINSDASH_SIGNING_PROPERTIES").orNull
val defaultSigningPropertiesFiles = listOf(
    file("/etc/coinsdash/signing/signing.properties"),
    file("${System.getProperty("user.home")}/.config/coinsdash/signing/signing.properties"),
)
val signingPropertiesFile = signingPropertiesPath
    ?.let(rootProject::file)
    ?.takeIf { it.isFile }
    ?: defaultSigningPropertiesFiles.firstOrNull { it.isFile }
val releaseSigningProperties = signingPropertiesFile?.let { file ->
    Properties().apply {
        file.inputStream().use(::load)
    }
}

if (gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }) {
    check(signingPropertiesFile != null) {
        "Release signing is required. Install signing.properties under /etc/coinsdash/signing or ~/.config/coinsdash/signing, or set COINSDASH_SIGNING_PROPERTIES."
    }
}

android {
    namespace = "com.holyware.coinsdash"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.holyware.coinsdash"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningProperties != null) {
            create("release") {
                storeFile = rootProject.file(releaseSigningProperties.getProperty("storeFile"))
                storePassword = releaseSigningProperties.getProperty("storePassword")
                keyAlias = releaseSigningProperties.getProperty("keyAlias")
                keyPassword = releaseSigningProperties.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            if (releaseSigningProperties != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
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
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(platform("com.google.firebase:firebase-bom:34.18.0"))
}
