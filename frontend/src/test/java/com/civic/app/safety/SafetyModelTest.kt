package com.civic.app.safety

import com.civic.app.data.repository.decodeTags
import com.civic.app.data.repository.encodeTags
import com.civic.shared.model.IssueCategory
import com.civic.shared.model.SafetyTag
import com.civic.shared.model.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyModelTest {

    @Test
    fun fallbackTimeOfDayBoundaries() {
        assertEquals(TimeOfDay.LATE_NIGHT, TimeOfDay.fallbackFor(0))
        assertEquals(TimeOfDay.LATE_NIGHT, TimeOfDay.fallbackFor(3 * 60 + 59))
        assertEquals(TimeOfDay.DAWN, TimeOfDay.fallbackFor(4 * 60))
        assertEquals(TimeOfDay.DAWN, TimeOfDay.fallbackFor(6 * 60 + 59))
        assertEquals(TimeOfDay.MORNING, TimeOfDay.fallbackFor(7 * 60))
        assertEquals(TimeOfDay.MIDDAY, TimeOfDay.fallbackFor(12 * 60))
        assertEquals(TimeOfDay.EVENING, TimeOfDay.fallbackFor(17 * 60))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fallbackFor(18 * 60 + 30))
        assertEquals(TimeOfDay.LATE_NIGHT, TimeOfDay.fallbackFor(23 * 60 + 59))
    }

    @Test(expected = IllegalArgumentException::class)
    fun fallbackRejectsOutOfRangeMinutes() {
        TimeOfDay.fallbackFor(24 * 60)
    }

    @Test
    fun safetyCategoriesAreAppendedAndGrouped() {
        // Stored by name, but keep the original civic values first so nothing that relies on order shifts.
        assertEquals(IssueCategory.POTHOLE, IssueCategory.entries.first())
        assertTrue(IssueCategory.SAFETY.all { it.isSafety })
        assertFalse(IssueCategory.CIVIC.any { it.isSafety })
        assertEquals(IssueCategory.entries.size, IssueCategory.SAFETY.size + IssueCategory.CIVIC.size)
        assertEquals(IssueCategory.SAFETY, IssueCategory.DISPLAY_ORDER.take(3))
    }

    @Test
    fun tagsRoundTripInStableOrder() {
        val tags = setOf(SafetyTag.DRINKING, SafetyTag.POOR_LIGHTING)
        assertEquals("POOR_LIGHTING,DRINKING", encodeTags(tags))
        assertEquals(tags, decodeTags(encodeTags(tags)))
        assertNull(encodeTags(emptySet()))
        assertEquals(emptySet<SafetyTag>(), decodeTags(null))
        assertEquals(setOf(SafetyTag.DESERTED), decodeTags("DESERTED,NOT_A_TAG"))
    }

    @Test
    fun timeMatchWrapsAroundMidnight() {
        assertEquals(1.0, ZoneRelevance.timeMatch(TimeOfDay.DAWN, TimeOfDay.DAWN), 0.0)
        assertEquals(0.5, ZoneRelevance.timeMatch(TimeOfDay.LATE_NIGHT, TimeOfDay.DAWN), 0.0)
        assertEquals(0.5, ZoneRelevance.timeMatch(TimeOfDay.MORNING, TimeOfDay.DAWN), 0.0)
        assertEquals(0.2, ZoneRelevance.timeMatch(TimeOfDay.MIDDAY, TimeOfDay.DAWN), 0.0)
    }

    @Test
    fun personalSafetyOutweighsCivicProblems() {
        val dawn = TimeOfDay.DAWN
        assertEquals(1.0, ZoneRelevance.categoryWeight(IssueCategory.UNSAFE_WOMEN, dawn), 0.0)
        assertTrue(ZoneRelevance.categoryWeight(IssueCategory.POTHOLE, dawn) < 0.5)
        // A broken streetlight matters more in the dark.
        assertTrue(
            ZoneRelevance.categoryWeight(IssueCategory.STREETLIGHT, dawn) >
                ZoneRelevance.categoryWeight(IssueCategory.STREETLIGHT, TimeOfDay.MIDDAY),
        )
    }
}
