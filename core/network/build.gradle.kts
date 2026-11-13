plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    api(project(":core:contract"))
    api(libs.arrow.core)
    api(libs.koin.core)
    api(libs.ktor.client.core)
    api(libs.ktor.client.logging)

    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
}
