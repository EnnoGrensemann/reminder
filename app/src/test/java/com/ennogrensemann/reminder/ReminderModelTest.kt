package com.ennogrensemann.reminder

import com.ennogrensemann.reminder.data.FavoriteLocationEntity
import com.ennogrensemann.reminder.data.ReminderEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderModelTest {

    @Test
    fun reminderDefaultValues_areCorrect() {
        val reminder = ReminderEntity(
            title = "Milch kaufen",
            locationName = "Rewe",
            latitude = 52.5200,
            longitude = 13.4050,
            radiusMeters = 150f
        )
        assertEquals("Milch kaufen", reminder.title)
        assertEquals("Rewe", reminder.locationName)
        assertEquals(150f, reminder.radiusMeters, 0.01f)
        assertFalse(reminder.isCompleted)
        assertFalse(reminder.isRecurring)
        assertFalse(reminder.hasTriggered)
    }

    @Test
    fun favoriteLocationEntity_holdsRadiusAndCoordinates() {
        val fav = FavoriteLocationEntity(
            name = "Zuhause",
            iconName = "Home",
            latitude = 53.5511,
            longitude = 9.9937,
            defaultRadiusMeters = 200f
        )
        assertEquals("Zuhause", fav.name)
        assertEquals("Home", fav.iconName)
        assertTrue(fav.defaultRadiusMeters >= 50f)
    }
}
