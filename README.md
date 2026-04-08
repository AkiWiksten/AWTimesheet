# WorkTime 3.0
## Modern App Architecture
### Navigation3
### Hilt
### Structure (Compose + MVVM)

A clean, scalable example approach is:
+ app/
    + core/                 # shared utilities, theme, base classes
    + data/                 # repositories, API, database
    + domain/               # use cases, business logic (optional but good)
    + feature/
        + home/
            + HomeScreen.kt
            + HomeViewModel.kt
            + HomeUiState.kt
            + components/
        + profile/
            + ProfileScreen.kt
            + ProfileViewModel.kt
            + components/
        + login/
    + navigation/
        + NavGraph.kt

This is called feature-based packaging.
