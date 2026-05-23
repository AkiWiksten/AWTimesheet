plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:9.2.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
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


