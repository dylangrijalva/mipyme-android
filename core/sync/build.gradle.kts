plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "co.mipyme.sync"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(project(":core:contract"))
    api(project(":core:database"))
    api(libs.arrow.core)
    api(libs.kotlinx.coroutines.core)
    api(libs.koin.core)

    implementation(project(":core:auth"))
    implementation(project(":core:network"))
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.pusher.java.client)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
}
