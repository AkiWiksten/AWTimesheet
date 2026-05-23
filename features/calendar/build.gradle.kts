plugins {
    id("awtimesheet.android.compose.feature")
    id("awtimesheet.feature.dependencies")
}

android {
    namespace = "com.akiwiksten.awtimesheet.feature.calendar"
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(project(":data"))
    testImplementation(testFixtures(project(":domain")))
    testImplementation(libs.kotlinx.coroutines.test)
}
