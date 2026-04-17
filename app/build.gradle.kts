import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.akiwiksten.worktime30"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.akiwiksten.worktime30"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(file("$rootDir/config/detekt/detekt.yml"))
}

tasks.register<Exec>("pullScreenshots") {
    group = "verification"
    description = "Pulls screenshots from the device"

    val packageName = "com.akiwiksten.worktime30"
    val remoteDir = "/sdcard/Android/data/$packageName/files/screenshots"
    val localDir = layout.projectDirectory.asFile
    val deviceIdProvider = providers.gradleProperty("deviceId")

    doFirst {
        val id = deviceIdProvider.getOrNull()
        val adbBase = if (id != null) listOf("adb", "-s", id) else listOf("adb")
        commandLine(adbBase + listOf("pull", remoteDir, localDir.absolutePath))
    }
}

tasks.register<Exec>("recordScreenshots") {
    group = "verification"
    description = "Installs, runs screenshot tests, and pulls them. Use -Pfeature=<feature_name> to run specific tests."
    dependsOn("installDebug", "installDebugAndroidTest")
    
    val deviceIdProvider = providers.gradleProperty("deviceId")
    val featureProvider = providers.gradleProperty("feature")

    doFirst {
        val id = deviceIdProvider.getOrNull()
        val adbBase = if (id != null) listOf("adb", "-s", id) else listOf("adb")
        
        val feature = featureProvider.getOrNull()
        val testArgs = if (feature != null) {
            // Run all tests in the feature package to include multiple test files
            // (e.g. SingleProjectScreenScreenshotTest)
            listOf("-e", "package", "com.akiwiksten.worktime30.feature.$feature")
        } else {
            // If no feature provided, we run all screenshot tests in the features package
            listOf("-e", "package", "com.akiwiksten.worktime30.feature")
        }

        commandLine(adbBase + listOf(
            "shell", "am", "instrument", "-w"
        ) + testArgs + listOf(
            "com.akiwiksten.worktime30.test/androidx.test.runner.AndroidJUnitRunner"
        ))
    }
    finalizedBy("pullScreenshots")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Navigation 3
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    detektPlugins(libs.detekt.formatting)
}
