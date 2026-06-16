plugins {
    id("awtimesheet.android.compose.feature")
    id("awtimesheet.feature.dependencies")
}

android {
    namespace = "com.akiwiksten.awtimesheet.feature.location"
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(testFixtures(project(":domain")))
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.activity.compose)
}
