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
    val coroutinesCore = libs.kotlinx.coroutines.core

    implementation(project(":core"))
    implementation(coroutinesCore)
    implementation(libs.javax.inject)

    testFixturesImplementation(project(":core"))
    testFixturesImplementation(coroutinesCore)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}

