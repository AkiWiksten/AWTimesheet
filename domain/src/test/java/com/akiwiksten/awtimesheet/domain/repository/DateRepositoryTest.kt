package com.akiwiksten.awtimesheet.domain.repository

import com.akiwiksten.awtimesheet.test.InMemoryDateRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DateRepositoryTest {
    @Test
    fun selectedDate_hasDefaultValue() {
        val repository = InMemoryDateRepository()

        val selectedDate = repository.selectedDate.value

        assertNotNull(selectedDate)
        assertEquals(10, selectedDate.length)
    }

    @Test
    fun updateDate_updatesSelectedDate_whenDateIsNotEmpty() {
        val repository = InMemoryDateRepository()

        repository.updateDate("2026-04-10")

        assertEquals("2026-04-10", repository.selectedDate.value)
    }

    @Test
    fun updateDate_ignoresEmptyDate() {
        val repository = InMemoryDateRepository()
        val initial = repository.selectedDate.value

        repository.updateDate("")

        assertEquals(initial, repository.selectedDate.value)
    }

    @Test
    fun notifyCalendarDataChanged_incrementsCalendarRefreshVersion() {
        val repository = InMemoryDateRepository()

        val initial = repository.calendarRefreshVersion.value
        repository.notifyCalendarDataChanged()

        assertEquals(initial + 1, repository.calendarRefreshVersion.value)
    }
}
