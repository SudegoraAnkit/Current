package com.sudegoratechglobal.current

import com.sudegoratechglobal.current.util.NlpParser
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class NlpParserTest {

    @Test
    fun testParseSimpleTask() {
        val parsed = NlpParser.parse("Buy milk")
        assertEquals("Buy milk", parsed.title)
        assertFalse(parsed.isLocked)
        assertEquals(2, parsed.priority)
    }

    @Test
    fun testParseTomorrowAndTime() {
        val parsed = NlpParser.parse("Call Mom tomorrow at 5pm")
        assertEquals("Call Mom", parsed.title)
        
        val calendar = Calendar.getInstance().apply {
            timeInMillis = parsed.scheduledTime
        }
        
        val expectedDay = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }.get(Calendar.DAY_OF_YEAR)
        
        assertEquals(expectedDay, calendar.get(Calendar.DAY_OF_YEAR))
        assertEquals(17, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun testParsePriorityAndLock() {
        val parsed = NlpParser.parse("Submit tax report !urgent lock 555-1234")
        assertEquals("Submit tax report", parsed.title)
        assertTrue(parsed.isLocked)
        assertEquals("555-1234", parsed.accountabilityContact)
        assertEquals(1, parsed.priority)
    }

    @Test
    fun testParseRelativeInHours() {
        val parsed = NlpParser.parse("Read books in 2 hours")
        assertEquals("Read books", parsed.title)
        
        val timeDiff = parsed.scheduledTime - System.currentTimeMillis()
        // Difference should be roughly 2 hours (7200 seconds)
        assertTrue(timeDiff in 7100000..7300000)
    }
}
