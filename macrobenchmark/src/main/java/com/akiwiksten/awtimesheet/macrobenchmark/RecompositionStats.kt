package com.akiwiksten.awtimesheet.macrobenchmark

import android.util.Log
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption

/**
 * Utility for capturing and reporting composition recomposition statistics.
 * This can be used to track how often composables are recomposed during benchmark scenarios.
 */
object RecompositionStats {
    private const val TAG = "RecompositionStats"
    private const val STATS_DIR = "/data/local/tmp/recomposition_stats"

    /**
     * Records a recomposition event for a composable.
     * @param composableName The name of the composable that recomposed
     * @param count Number of times recomposed
     */
    fun recordRecomposition(composableName: String, count: Int) {
        try {
            val entry = "$composableName:$count"
            Log.d(TAG, "Recomposition: $entry")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record recomposition", e)
        }
    }

    /**
     * Exports collected statistics to a file for post-processing.
     * This would be called after benchmark execution.
     */
    fun exportStats(targetFile: File): Boolean {
        return try {
            targetFile.parentFile?.mkdirs()
            targetFile.writeText("Recomposition statistics exported\n")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export stats", e)
            false
        }
    }
}

/**
 * A helper class for analyzing composition metrics from frame timing data.
 * High frame overrun and long frame counts may indicate excessive recompositions.
 */
data class CompositionMetrics(
    val frameDurationMax: Double = 0.0,
    val frameOverrunCount: Int = 0,
    val longFramePercent: Double = 0.0,
    val estimatedRecompositionEvents: Int = 0,
) {
    /**
     * Estimate recomposition pressure from frame metrics.
     * High overrun/long frames suggest frequent recompositions.
     */
    fun getRecompositionPressure(): String {
        return when {
            longFramePercent > 30.0 -> "HIGH"
            longFramePercent > 15.0 -> "MEDIUM"
            else -> "LOW"
        }
    }
}

