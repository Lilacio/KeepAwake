import java.util.Properties

plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.lilacio.keepawake"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.lilacio.keepawake"
        minSdk = 36
        targetSdk = 36
        versionCode = 7
        versionName = "2.5"
    }

    // Generates the locale list for the per-app language setting (Android 13+).
    // The default (unqualified) resources are English, see res/resources.properties.
    androidResources {
        generateLocaleConfig = true
    }

    // Fixed release signing so rebuilds on any machine can update the installed app.
    // Key and passwords live in keystore.properties (not in git); falls back to the debug key if missing.
    val ksProps = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    val hasPersonalKey = ksProps.getProperty("storeFile")?.let { rootProject.file(it).exists() } == true

    signingConfigs {
        if (hasPersonalKey) {
            create("personal") {
                storeFile = rootProject.file(ksProps.getProperty("storeFile"))
                storePassword = ksProps.getProperty("storePassword")
                keyAlias = ksProps.getProperty("keyAlias")
                keyPassword = ksProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            if (hasPersonalKey) signingConfig = signingConfigs.getByName("personal")
        }
        release {
            isMinifyEnabled = false
            signingConfig = if (hasPersonalKey) signingConfigs.getByName("personal")
            else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
