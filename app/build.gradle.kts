plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id ("kotlin-parcelize")
}

android {
    namespace = "com.pant.aegispass"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pant.aegispass"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Enable code obfuscation and shrinking for release builds
            this.isShrinkResources = true // Enable resource shrinking for release builds
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Disable obfuscation for debug builds to make debugging easier
            isMinifyEnabled = false
            this.isShrinkResources = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        dataBinding = true
        viewBinding = true // This is crucial for generating binding classes
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.biometric:biometric:1.2.0-alpha05") // Biometric for fingerprint
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation ("io.github.chrisbanes:PhotoView:2.3.0")
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    kapt ("com.github.bumptech.glide:compiler:4.15.1")

    // Room Database for local storage
    implementation("androidx.room:room-runtime:2.6.1")
    implementation(libs.androidx.monitor)
    implementation(libs.play.services.cast.tv)
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")

    // jBCrypt for password hashing (THIS WAS MISSING!)
    implementation("org.mindrot:jbcrypt:0.4")

    // Other dependencies you had
    implementation("androidx.security:security-crypto:1.1.0-alpha06") // For EncryptedSharedPreferences etc.
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Optional: OkHttp Logging Interceptor for debugging network requests
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}