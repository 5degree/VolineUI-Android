import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.cropintellix.volineui"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_24
        targetCompatibility = JavaVersion.VERSION_24
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_24
        }
    }
}

//noinspection UseTomlInstead
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    
    // Google Play Services for Location
    implementation("com.google.android.gms:play-services-location:21.3.0")
    
    // ExifInterface for image rotation
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    
    // Glide for image loading, caching, and GIF support
    implementation("com.github.bumptech.glide:glide:5.0.5")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}