package com.akiwiksten.awtimesheet.macrobenchmark

/**
 * Centralized configuration for macrobenchmark tests.
 * These constants control benchmark behavior and can be adjusted globally.
 */
object BenchmarkConfig {
    /**
     * Target package name for benchmarks (benchmark variant with .benchmark suffix).
     * The app automatically bypasses the intro screen when this package name is used.
     */
    const val TARGET_PACKAGE = "com.akiwiksten.awtimesheet.benchmark"

    /**
     * Number of iterations for scroll/recomposition benchmarks.
     * Kept low for fast local feedback.
     */
    const val ITERATIONS = 2

    /**
     * Default number of iterations for startup benchmarks when running locally.
     */
    const val STARTUP_ITERATIONS_LOCAL = 8

    /**
     * Default number of iterations for startup benchmarks in CI profile.
     */
    const val STARTUP_ITERATIONS_CI = 12
}
