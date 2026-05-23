# AWTimesheet Quality Scorecard

Last updated: 2026-05-23
Review cadence: Monthly (light) and Quarterly (full)

## How to use
- Score each quality from 0 to 5.
- Weighted points = Weight * (Score / 5).
- Keep evidence links updated every review.
- Track trends, not just point-in-time score.

## Current baseline (v1)

| Quality | Weight | Score (0-5) | Weighted points | Evidence (current) | Primary risk |
|---|---:|---:|---:|---|---|
| Maintainable | 12 | 4.4 | 10.6 | `README.md`, `build-logic/src/main/kotlin/AwtimesheetAndroidBaseConventionPlugin.kt` | Architecture drift over time |
| Testable | 12 | 4.5 | 10.8 | `features/singleproject/src/test/java/com/akiwiksten/awtimesheet/feature/singleproject/SingleProjectViewModelTest.kt`, `core/build.gradle.kts` | Coverage gaps in new features |
| Reliable | 11 | 4.1 | 9.0 | `domain/src/main/java/com/akiwiksten/awtimesheet/domain/usecase/SaveWorkdayUseCase.kt` | Uncovered error and edge paths |
| Stable | 9 | 4.0 | 7.2 | `README.md` quality pipeline (`test`, screenshot, `detekt`, `lint`) | Flaky tests and regression slips |
| Modular | 10 | 4.6 | 9.2 | `features/`, `core/`, `data/`, `domain/` structure | Cross-module coupling creep |
| Consistent | 8 | 4.3 | 6.9 | `build-logic/src/main/kotlin/`, `gradle/libs.versions.toml` | Version/config drift |
| Readable | 7 | 4.2 | 5.9 | Test naming and package clarity in feature tests | Readability decline in rapid changes |
| Reusable | 8 | 4.1 | 6.6 | `core` testFixtures, fake repositories in tests | Duplicate helper logic |
| Flexible | 7 | 4.0 | 5.6 | Repository and use-case layering | Missing interfaces in some areas |
| Scalable | 8 | 4.0 | 6.4 | Feature-based packaging and convention plugins | Build speed and dependency growth |
| High-performing | 8 | 3.2 | 5.1 | Architecture supports optimization work | Limited direct runtime evidence |

**Total weighted score: 83.3 / 100**

## Improvement priorities
1. High-performing (collect hard runtime evidence).
2. Reliable and Stable (expand error-path and regression tests).
3. Scalable governance (enforce boundaries and dependency rules).
4. Maintainability and Testability upkeep (preserve current strengths).

## Quarterly targets (Q3 2026)

| Quality | Target score | KPI | Owner | Status |
|---|---:|---|---|---|
| High-performing | 3.8 | Startup p95 and key screen jank baseline captured | TBA | Not started |
| Reliable | 4.4 | Add critical failure-path unit tests for each feature | TBA | Not started |
| Stable | 4.3 | Flaky unit/instrumented test rerun rate below 1% | TBA | Not started |
| Scalable | 4.3 | Zero module boundary violations in CI | TBA | Not started |
| Testable | 4.7 | Coverage trend positive in all feature modules | TBA | Not started |

## KPI dashboard template

| KPI | Current | Target | Trend | Notes |
|---|---:|---:|---|---|
| `testDebugUnitTest` pass rate | TBA | >= 99% | TBA | |
| Unit test duration (minutes) | TBA | <= current baseline + 10% | TBA | |
| Flaky rerun rate | TBA | < 1% | TBA | |
| `detekt` issue count | TBA | Downward trend | TBA | |
| `lint` issue count | TBA | Downward trend | TBA | |
| `verifyModuleBoundaries` violations | TBA | 0 | TBA | |
| Cold startup p95 (ms) | TBA | Set after baseline | TBA | |
| Key Compose recomposition hotspot count | TBA | Downward trend | TBA | |

## Monthly review checklist
- Update all KPI values.
- Re-score all qualities (0-5) with short rationale.
- Recalculate weighted points and total score.
- Add new evidence links for major architecture/test changes.
- Confirm top 3 priorities for next month.

## Optional CI gate ideas
- Fail CI on any module boundary violation.
- Fail CI if flaky rerun rate exceeds threshold.
- Warn if unit test duration regresses above agreed threshold.
- Fail CI on new `detekt`/`lint` critical issues.

