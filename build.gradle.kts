plugins {
    kotlin("multiplatform") version "2.1.20"
    id("com.android.application") version "8.10.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20"
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    androidTarget() // Android target

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("io.opentelemetry:opentelemetry-api:1.49.0")
                implementation("io.opentelemetry:opentelemetry-sdk:1.49.0")
                implementation("io.opentelemetry:opentelemetry-sdk-metrics:1.49.0")
                implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.49.0")
                implementation("com.github.oshi:oshi-core:6.8.1")
                implementation("net.java.dev.jna:jna-platform:5.17.0")
                implementation("org.slf4j:slf4j-simple:2.0.17")
                implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.appcompat:appcompat:1.6.1")
                implementation("io.opentelemetry:opentelemetry-api:1.49.0")
                implementation("io.opentelemetry:opentelemetry-sdk:1.49.0")
                implementation("io.opentelemetry:opentelemetry-sdk-metrics:1.49.0")
                implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.49.0")
                implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
            }
        }
    }
}

tasks.register<JavaExec>("runJvm") {
    group = "application"
    mainClass.set("MainKt") // or your.package.MainKt
    classpath = files(
        "build/libs/kotlin_monitor-jvm.jar",
        configurations["jvmRuntimeClasspath"].files
    )
}

android {
    namespace = "com.example.kotlinmonitor"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.monitor"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}