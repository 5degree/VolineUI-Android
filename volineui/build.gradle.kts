import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "in.fivedegree.volineui"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

// ── Maven Central Publishing ────────────────────────────────────────────
mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(
        groupId = property("GROUP").toString(),
        artifactId = "volineui",
        version = property("LIBRARY_VERSION").toString(),
    )

    pom {
        name.set("VolineUI")
        description.set("Reusable Jetpack Compose UI components for the VolineUI Android library — design system, layouts, and UI utilities.")
        url.set("https://github.com/5degree/VolineUI-Android")
        inceptionYear.set("2025")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("5degree")
                name.set("5Degree")
                url.set("https://github.com/5degree")
            }
        }

        scm {
            url.set("https://github.com/5degree/VolineUI-Android")
            connection.set("scm:git:git://github.com/5degree/VolineUI-Android.git")
            developerConnection.set("scm:git:ssh://git@github.com/5degree/VolineUI-Android.git")
        }
    }
}

//noinspection UseTomlInstead
dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")

    implementation(project(":volinecore"))

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2026.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Google Play Services for Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // ExifInterface for image rotation
    implementation("androidx.exifinterface:exifinterface:1.4.2")

    // Glide for image loading, caching, and GIF support
    implementation("com.github.bumptech.glide:glide:5.0.5")

    // Coil for Compose image loading
    implementation("io.coil-kt.coil3:coil-compose:3.4.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.4.0")
    implementation("io.coil-kt.coil3:coil-gif:3.4.0")
}
