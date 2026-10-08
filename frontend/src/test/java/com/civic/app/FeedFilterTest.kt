package com.civic.app

import com.civic.app.ui.categoryName
import com.civic.app.ui.screens.feed.FeedFilter
import com.civic.app.ui.statusOf
import com.civic.shared.model.IssueCategory
import com.civic.shared.model.IssueStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedFilterTest {
    @Test
    fun emptyFilterIsInactive() {
        assertFalse(FeedFilter().isActive)
        assertTrue(FeedFilter(status = IssueStatus.RESOLVED).isActive)
        assertTrue(FeedFilter(category = IssueCategory.FIRE).isActive)
    }

    @Test
    fun unknownStoredValuesFallBack() {
        assertEquals(IssueStatus.RESOLVED, statusOf("RESOLVED"))
        assertEquals(IssueStatus.REPORTED, statusOf("garbage"))
        assertEquals("Fallen tree", categoryName("FALLEN_TREE"))
        assertEquals("CUSTOM", categoryName("CUSTOM"))
    }
}
