plugins {
    id("awtimesheet.android.base")
    alias(libs.plugins.android.library)
    kotlin("plugin.parcelize")
}

android {
    namespace = "com.akiwiksten.awtimesheet.domain"
    testFixtures {
        enable = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)

    testFixturesImplementation(project(":core"))
    testFixturesImplementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}

