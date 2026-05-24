package com.akiwiksten.awtimesheet.macrobenchmark

/**
 * Centralized configuration for macrobenchmark tests.
 * These constants control benchmark behavior and can be adjusted globally.
 */
object BenchmarkConfig {
    /**
     * Number of iterations each benchmark runs.
     * Each iteration performs the benchmark measurement once.
     * Higher values (3+) provide more data but take longer.
     * Recommended: 2-3 for consistent results on most devices.
     */
    const val ITERATIONS = 2
}

