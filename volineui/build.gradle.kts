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

    // Activity-Compose for VolineCameraActivity's setContent (the library
    // previously had no Compose activity, so this was not on the classpath).
    implementation(libs.androidx.activity.compose)

    // Material icons for the in-app camera controls (close, flash, switch camera).
    implementation("androidx.compose.material:material-icons-extended")

    // CameraX - in-app camera used by PhotoCaptureManager.
    // Latest stable at time of writing is 1.6.2 (1.7.0 is release-candidate only).
    // No system camera app is launched for photo capture.
    implementation("androidx.camera:camera-core:1.6.2")
    implementation("androidx.camera:camera-camera2:1.6.2")
    implementation("androidx.camera:camera-lifecycle:1.6.2")
    implementation("androidx.camera:camera-view:1.6.2")

    // ML Kit on-device text recognition (bundled models) for the OCR step
    // of photo capture. Bundled, not Play-services-delivered, so OCR works
    // immediately after install with no download and no network: a document
    // photo never leaves the device. Latin covers English and other Latin
    // scripts; Devanagari covers Hindi and also reads Latin text, and both
    // recognizers run so a mixed-script document is covered either way.
    // Adding a script later (e.g. Bengali, Telugu) is one more artifact
    // here plus one entry in the recognizer set. Versions are the current
    // stable ML Kit releases.
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.1")

    // ML Kit selfie segmentation (bundled, ~4.5 MB) for the person
    // capture guide: a live person mask over the preview, used to
    // enable the shutter only while a person is framed inside the
    // outline. Bundled, so it works immediately after install with
    // no download and no network. Still beta (16.0.0-beta6).
    implementation("com.google.mlkit:segmentation-selfie:16.0.0-beta6")

    // ML Kit face mesh detection (bundled, ~6.4 MB) for the person
    // capture result: 468 3D points plus a bounding box for the face
    // in the finished photo. Bundled, no network. Still beta.
    // Beta3, not beta1: beta1 was compiled against
    // mediapipe-internal beta8, but Gradle resolves beta10 (pulled
    // in by segmentation-selfie beta6), whose MediaPipeGraphRunner
    // .create signature changed -- and face mesh crashed on device
    // with NoSuchMethodError. Beta3 is built against the newer
    // mediapipe, so the two guides share one compatible version.
    // It reports no head pose -- only points and box.
    implementation("com.google.mlkit:face-mesh-detection:16.0.0-beta3")

    // ML Kit document scanner (Play-services only) for the document
    // capture guide. This is a small stub (~300 KB); the scanner UI,
    // models, and cleaning logic are downloaded by Play services on
    // first use, so unlike the bundled APIs above it needs Play
    // services and cannot work offline on first use. It launches
    // Google's own scanner screens and returns cleaned page images;
    // it cannot clean a photo this camera already captured, which is
    // why DOCUMENT captures launch it instead of this camera screen.
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0")

    testImplementation(libs.junit)
}
