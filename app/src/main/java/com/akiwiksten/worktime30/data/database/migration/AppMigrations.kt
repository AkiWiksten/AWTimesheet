package com.akiwiksten.worktime30.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.akiwiksten.worktime30.core.DATE
import com.akiwiksten.worktime30.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.worktime30.core.INITIAL_FLEX_TIME_TOTAL
import com.akiwiksten.worktime30.core.WORK_STATS_TABLE
import com.akiwiksten.worktime30.core.WORKDAY_TABLE
import com.akiwiksten.worktime30.core.WORK_TIME_TODAY
import com.akiwiksten.worktime30.core.WORK_TIME_TODAY_ESTIMATE
import com.akiwiksten.worktime30.core.ZERO_TIME

object AppMigrations {
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $WORKDAY_TABLE (
                    $DATE TEXT NOT NULL,
                    $WORK_TIME_TODAY TEXT NOT NULL DEFAULT '$ZERO_TIME',
                    $WORK_TIME_TODAY_ESTIMATE TEXT NOT NULL DEFAULT '$DEFAULT_DAILY_WORK_TIME',
                    $INITIAL_FLEX_TIME_TOTAL TEXT NOT NULL DEFAULT '$ZERO_TIME',
                    PRIMARY KEY($DATE)
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val legacyFlexColumn = "flex_time_total"

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ${WORK_STATS_TABLE}_new (
                    id INTEGER NOT NULL,
                    daily_work_time TEXT NOT NULL,
                    lunch_time TEXT NOT NULL,
                    $INITIAL_FLEX_TIME_TOTAL TEXT NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO ${WORK_STATS_TABLE}_new (id, daily_work_time, lunch_time, $INITIAL_FLEX_TIME_TOTAL)
                SELECT id, daily_work_time, lunch_time, $legacyFlexColumn
                FROM $WORK_STATS_TABLE
                """.trimIndent()
            )

            db.execSQL("DROP TABLE $WORK_STATS_TABLE")
            db.execSQL("ALTER TABLE ${WORK_STATS_TABLE}_new RENAME TO $WORK_STATS_TABLE")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ${WORKDAY_TABLE}_new (
                    $DATE TEXT NOT NULL,
                    $WORK_TIME_TODAY TEXT NOT NULL DEFAULT '$ZERO_TIME',
                    $WORK_TIME_TODAY_ESTIMATE TEXT NOT NULL DEFAULT '$DEFAULT_DAILY_WORK_TIME',
                    PRIMARY KEY($DATE)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO ${WORKDAY_TABLE}_new ($DATE, $WORK_TIME_TODAY, $WORK_TIME_TODAY_ESTIMATE)
                SELECT $DATE, $WORK_TIME_TODAY, $WORK_TIME_TODAY_ESTIMATE
                FROM $WORKDAY_TABLE
                """.trimIndent()
            )

            db.execSQL("DROP TABLE $WORKDAY_TABLE")
            db.execSQL("ALTER TABLE ${WORKDAY_TABLE}_new RENAME TO $WORKDAY_TABLE")
        }
    }
}




