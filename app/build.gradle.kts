plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.room)
}

// The release keystore, from the environment the host script sets (android.ps1 / android.sh build --release): the file
// is mounted into the container, the password never touches a file or a command line.
val keystoreFile: String? = System.getenv("PHOSHARE_KEYSTORE_FILE")

android {
    namespace = "es.ouss.phoshare"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "es.ouss.phoshare"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (keystoreFile != null) {
                storeFile = file(keystoreFile)
                storeType = "pkcs12"
                storePassword = System.getenv("PHOSHARE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("PHOSHARE_KEY_ALIAS") ?: "phoshare"
                // PKCS12 keeps one password for the store and its key.
                keyPassword = System.getenv("PHOSHARE_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }

    testOptions {
        animationsDisabled = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.glide)
    implementation(libs.material)
    annotationProcessor(libs.androidx.room.compiler)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.espresso.contrib)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.intents)
    androidTestImplementation(libs.androidx.room.testing)
}
