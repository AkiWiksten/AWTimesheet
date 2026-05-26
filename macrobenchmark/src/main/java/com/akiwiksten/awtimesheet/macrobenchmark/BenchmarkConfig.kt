package com.akiwiksten.awtimesheet.macrobenchmark

/**
 * Centralized configuration for macrobenchmark tests.
 * These constants control benchmark behavior and can be adjusted globally.
 */
object BenchmarkConfig {
    /**
     * Target package name for benchmarks (benchmark variant with .benchmark suffix).
     */
    const val TARGET_PACKAGE = "com.akiwiksten.awtimesheet.benchmark"

    /**
     * Number of iterations for scroll/recomposition benchmarks.
     * Kept low (3) for fast local feedback; raise to 10+ for stable CI baselines.
     * At 3 iterations the frame pool is small (~57 frames per run), meaning a
     * single extra missed frame shifts jank% by ~1.5–2 pp — prefer higher
     * iteration counts when comparing before/after changes.
     */
    const val ITERATIONS = 3

    /**
     * Default number of iterations for startup benchmarks when running locally.
     */
    const val STARTUP_ITERATIONS_LOCAL = 3

    /**
     * Default number of iterations for startup benchmarks in CI profile.
     */
    const val STARTUP_ITERATIONS_CI = 1
}
