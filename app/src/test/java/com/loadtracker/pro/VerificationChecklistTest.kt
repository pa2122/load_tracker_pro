package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VerificationChecklistTest {
    private val body = """
        ## In-App Verification
        ### v1.x (READY)
        - [ ] [v1.x] Confirm the feature works.
        - [x] [v1.x] Confirm the result persists.
        ### v2.0 (READY)
        - [ ] [v2.0] Confirm the v2 flow works.
        ### v1.x (N/A)
        Not applicable.
    """.trimIndent()

    @Test
    fun parsesVersionedStepsAndStatuses() {
        val sections = VerificationChecklist.sections(body)

        assertEquals(3, sections.size)
        assertEquals("v1.x", sections[0].version)
        assertEquals("READY", sections[0].status)
        assertFalse(sections[0].steps[0].isComplete)
        assertTrue(sections[0].steps[1].isComplete)
        assertEquals("N/A", sections[2].status)
    }

    @Test
    fun updatesOnlyRequestedVersionStep() {
        val updated = VerificationChecklist.updateStep(body, "v1.x", 0, true)

        assertNotNull(updated)
        assertTrue(updated!!.contains("- [x] [v1.x] Confirm the feature works."))
        assertTrue(updated.contains("- [ ] [v2.0] Confirm the v2 flow works."))
    }

    @Test
    fun refusesCompletionUntilAllApplicableVersionsPass() {
        assertFalse(VerificationChecklist.allApplicableVersionsComplete(body))

        val allChecked = body
            .replace("- [ ] [v1.x] Confirm the feature works.", "- [x] [v1.x] Confirm the feature works.")
            .replace("- [ ] [v2.0] Confirm the v2 flow works.", "- [x] [v2.0] Confirm the v2 flow works.")
        assertTrue(VerificationChecklist.allApplicableVersionsComplete(allChecked))

        val blocked = allChecked.replace("v2.0 (READY)", "v2.0 (BLOCKED)")
        assertFalse(VerificationChecklist.allApplicableVersionsComplete(blocked))
    }

    @Test
    fun returnsNullWhenStepNoLongerExists() {
        assertNull(VerificationChecklist.updateStep(body, "v1.x", 4, true))
    }

    @Test
    fun returnsNullWhenTheChecklistTextChangedRemotely() {
        assertNull(
            VerificationChecklist.updateStep(
                body = body,
                version = "v1.x",
                stepIndex = 0,
                isComplete = true,
                expectedStepText = "A different checklist step"
            )
        )
    }
}
