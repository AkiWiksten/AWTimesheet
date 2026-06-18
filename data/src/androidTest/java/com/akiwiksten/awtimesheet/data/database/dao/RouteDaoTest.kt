package com.akiwiksten.awtimesheet.data.database.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.akiwiksten.awtimesheet.data.database.AppDatabase
import com.akiwiksten.awtimesheet.data.database.entity.RouteEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RouteDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var routeDao: RouteDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        routeDao = db.routeDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertRoute_sameStartAndDestination_replacesExistingRow() = runBlocking {
        routeDao.insertRoute(
            RouteEntity(
                timestamp = "1000",
                startPoint = "Start A",
                destinationPoint = "Dest A",
                distance = "10 km",
            )
        )
        routeDao.insertRoute(
            RouteEntity(
                timestamp = "2000",
                startPoint = "Start A",
                destinationPoint = "Dest A",
                distance = "11 km",
            )
        )

        val all = routeDao.getAll()

        assertEquals(1, all.size)
        assertEquals("2000", all.single().timestamp)
        assertEquals("11 km", all.single().distance)
    }

    @Test
    fun getAll_returnsNewestTimestampFirst() = runBlocking {
        routeDao.insertRoute(
            RouteEntity(
                timestamp = "1000",
                startPoint = "Start A",
                destinationPoint = "Dest A",
                distance = "10 km",
            )
        )
        routeDao.insertRoute(
            RouteEntity(
                timestamp = "3000",
                startPoint = "Start B",
                destinationPoint = "Dest B",
                distance = "20 km",
            )
        )
        routeDao.insertRoute(
            RouteEntity(
                timestamp = "2000",
                startPoint = "Start C",
                destinationPoint = "Dest C",
                distance = "30 km",
            )
        )

        val all = routeDao.getAll()

        assertEquals(listOf("3000", "2000", "1000"), all.map { it.timestamp })
    }

    @Test
    fun clearAll_removesAllRoutes() = runBlocking {
        routeDao.insertRoute(
            RouteEntity(
                timestamp = "1000",
                startPoint = "Start A",
                destinationPoint = "Dest A",
                distance = "10 km",
            )
        )
        routeDao.insertRoute(
            RouteEntity(
                timestamp = "2000",
                startPoint = "Start B",
                destinationPoint = "Dest B",
                distance = "20 km",
            )
        )

        routeDao.clearAll()

        assertTrue(routeDao.getAll().isEmpty())
    }
}

