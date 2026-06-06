import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "in.fivedegree.volinecore"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
        artifactId = "volinecore",
        version = property("LIBRARY_VERSION").toString(),
    )

    pom {
        name.set("VolineCore")
        description.set("Core utilities and data layer for VolineUI Android library — Firebase integration, offline queue, and shared infrastructure.")
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

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("com.google.android.material:material:1.14.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")

    // Firebase
    api(platform("com.google.firebase:firebase-bom:34.14.0"))
    api("com.google.firebase:firebase-database")

    // Room (offline log queue)
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // Gson (JSON serialization)
    implementation("com.google.code.gson:gson:2.14.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}
