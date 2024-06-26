plugins {
    id("server.common-conventions")
    kotlin("plugin.serialization") version "1.6.21"
}

group = "com.bluedragonmc.server"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation(libs.minestom)
    implementation(libs.kmongo)
    implementation(libs.caffeine)
    implementation(libs.minimessage)
    implementation(libs.bundles.configurate)
    implementation(libs.bundles.messaging)
    implementation(libs.serialization.json)
    implementation(libs.bundles.tinylog)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}