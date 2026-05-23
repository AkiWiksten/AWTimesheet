# Build Logic

This folder contains Gradle convention plugins used by the main project via `includeBuild("build-logic")` in `settings.gradle.kts`.

## Why this exists

Convention plugins keep module `build.gradle.kts` files small and consistent by moving repeated configuration into one place.

## Plugin catalog

### `awtimesheet.android.base`

Applies shared Android/Kotlin defaults for app and library modules:

- `compileSdk = 37`
- `minSdk = 29`
- Java compile options set to `11`
- Kotlin `jvmTarget = JVM_11`

Implementation class: `AwtimesheetAndroidBaseConventionPlugin`

---

### `awtimesheet.android.compose.app`

For Compose Android app modules. Applies:

- `awtimesheet.android.base`
- `com.android.application`
- `org.jetbrains.kotlin.plugin.compose`
- `com.google.dagger.hilt.android`
- `com.google.devtools.ksp`
- `com.android.compose.screenshot`

Also enables:

- `android.buildFeatures.compose = true`
- `android.experimental.enableScreenshotTest = true`

Implementation class: `AwtimesheetAndroidComposeAppConventionPlugin`

---

### `awtimesheet.android.compose.feature`

For Compose Android library/feature modules. Applies:

- `awtimesheet.android.base`
- `com.android.library`
- `org.jetbrains.kotlin.plugin.compose`
- `com.google.dagger.hilt.android`
- `com.google.devtools.ksp`
- `com.android.compose.screenshot`

Also enables:

- `android.buildFeatures.compose = true`
- `android.experimental.enableScreenshotTest = true`

Implementation class: `AwtimesheetAndroidComposeFeatureConventionPlugin`

---

### `awtimesheet.feature.dependencies`

Adds shared dependency set for feature modules:

- Base module dependency: `project(":core")`
- Common Compose and lifecycle dependencies
- Hilt + KSP compiler
- Screenshot test/tooling dependencies
- Test base: JUnit + Kotlin test

Use this on feature modules that follow the project default dependency shape.

Implementation class: `AwtimesheetFeatureDependenciesConventionPlugin`

## Example usage

```kotlin
plugins {
    id("awtimesheet.android.compose.feature")
    id("awtimesheet.feature.dependencies")
}

dependencies {
    // Keep only module-specific dependencies here.
    implementation(project(":domain"))
    testImplementation(testFixtures(project(":domain")))
}
```

## Add a new convention plugin

1. Add a class under `build-logic/src/main/kotlin/`.
2. Register plugin ID + implementation class in `build-logic/build.gradle.kts`.
3. Apply it in target modules.
4. Run project tests.

