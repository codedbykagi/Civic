package com.civic.app

import com.civic.shared.model.IssueCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun categoryDisplayName() {
        assertEquals("Pothole", IssueCategory.POTHOLE.displayName)
    }
}
