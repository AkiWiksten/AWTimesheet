package com.akiwiksten.awtimesheet.benchmark

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.akiwiksten.awtimesheet.core.DATE_FORMAT
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.usecase.GenerateWorkdaysUseCase
import com.akiwiksten.awtimesheet.domain.usecase.WorkdayGenerationMode
import com.akiwiksten.awtimesheet.domain.usecase.WorkdayGenerationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class BenchmarkDataSeedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var projectRepository: ProjectRepository

    @Inject
    lateinit var generateWorkdaysUseCase: GenerateWorkdaysUseCase

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != ACTION_SEED_REALISTIC_STARTUP_DATA) {
            return
        }

        val result = runCatching {
            runBlocking(Dispatchers.IO) {
                val formatter = DateTimeFormatter.ofPattern(DATE_FORMAT)
                val todayDate = LocalDate.now()
                val today = todayDate.format(formatter)
                val monthStart = todayDate.withDayOfMonth(1).format(formatter)
                val monthEnd = todayDate
                    .withDayOfMonth(todayDate.month.length(todayDate.isLeapYear))
                    .format(formatter)

                val hasAnyRecords = projectRepository.anyRecords()
                if (!hasAnyRecords) {
                    generateWorkdaysUseCase(
                        selectedDate = today,
                        scope = WorkdayGenerationScope.YEAR,
                        mode = WorkdayGenerationMode.UPSERT_ALL_WEEKDAYS
                    )
                    "seeded_year"
                } else {
                    val monthProjects = projectRepository.getProjectsByDateRange(monthStart, monthEnd)
                    if (monthProjects.isEmpty()) {
                        generateWorkdaysUseCase(
                            selectedDate = today,
                            scope = WorkdayGenerationScope.MONTH,
                            mode = WorkdayGenerationMode.UPSERT_ALL_WEEKDAYS
                        )
                        "seeded_month"
                    } else {
                        "skipped_existing_month_has_data"
                    }
                }
            }
        }

        result.onSuccess { status ->
            Log.i(TAG, "Benchmark startup dataset status: $status")
            setResultCode(Activity.RESULT_OK)
            setResultData(status)
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to prepare benchmark startup dataset", throwable)
            setResultCode(Activity.RESULT_CANCELED)
            setResultData("error:${throwable.message.orEmpty()}")
        }
    }

    companion object {
        const val ACTION_SEED_REALISTIC_STARTUP_DATA =
            "com.akiwiksten.awtimesheet.benchmark.ACTION_SEED_REALISTIC_STARTUP_DATA"

        private const val TAG = "BenchmarkDataSeedReceiver"
    }
}

