plugins {
    kotlin("jvm") version "1.9.22"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.opentelemetry:opentelemetry-api:1.49.0")
    implementation("io.opentelemetry:opentelemetry-sdk:1.49.0")
    implementation("io.opentelemetry:opentelemetry-sdk-metrics:1.49.0")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.49.0")
    implementation("com.github.oshi:oshi-core:6.8.1")
    implementation("net.java.dev.jna:jna-platform:5.17.0")
    implementation("org.slf4j:slf4j-simple:2.0.17") // Logging implementation
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
}

application {
    mainClass.set("MainKt")
}