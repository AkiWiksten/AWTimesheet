package com.akiwiksten.worktime30.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DateRepositoryTest {
    @Test
    fun selectedDate_hasDefaultValue() {
        val repository = DateRepository()

        val selectedDate = repository.selectedDate.value

        assertNotNull(selectedDate)
        assertEquals(10, selectedDate.length)
    }

    @Test
    fun updateDate_updatesSelectedDate_whenDateIsNotEmpty() {
        val repository = DateRepository()

        repository.updateDate("2026-04-10")

        assertEquals("2026-04-10", repository.selectedDate.value)
    }

    @Test
    fun updateDate_ignoresEmptyDate() {
        val repository = DateRepository()
        val initial = repository.selectedDate.value

        repository.updateDate("")

        assertEquals(initial, repository.selectedDate.value)
    }
}

