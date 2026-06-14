plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(versionCatalogs.named("libs").findLibrary("android-gradle-plugin").get())
    implementation(versionCatalogs.named("libs").findLibrary("kotlin-gradle-plugin").get())
}

gradlePlugin {
    plugins {
        register("androidBaseConvention") {
            id = "awtimesheet.android.base"
            implementationClass = "AwtimesheetAndroidBaseConventionPlugin"
        }
        register("androidComposeFeatureConvention") {
            id = "awtimesheet.android.compose.feature"
            implementationClass = "AwtimesheetAndroidComposeFeatureConventionPlugin"
        }
        register("androidComposeAppConvention") {
            id = "awtimesheet.android.compose.app"
            implementationClass = "AwtimesheetAndroidComposeAppConventionPlugin"
        }
        register("featureDependenciesConvention") {
            id = "awtimesheet.feature.dependencies"
            implementationClass = "AwtimesheetFeatureDependenciesConventionPlugin"
        }
    }
}


