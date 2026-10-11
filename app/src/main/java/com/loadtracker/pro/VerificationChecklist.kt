package com.loadtracker.pro

data class VerificationStep(
    val version: String,
    val text: String,
    val isComplete: Boolean
)

data class VerificationSection(
    val version: String,
    val status: String,
    val blockedReason: String?,
    val steps: List<VerificationStep>
)

object VerificationChecklist {
    private val sectionHeader = Regex("""^###\s+(v1\.x|v2\.0)\s+\((READY|BLOCKED|N/A)\)\s*$""")
    private val stepLine = Regex("""^\s*-\s+\[([ xX])]\s+\[(v1\.x|v2\.0)]\s+(.+?)\s*$""")

    fun sections(body: String): List<VerificationSection> {
        val lines = body.lines()
        val sections = mutableListOf<VerificationSection>()
        var currentVersion: String? = null
        var currentStatus: String? = null
        var blockedReason: String? = null
        var steps = mutableListOf<VerificationStep>()

        fun finishSection() {
            val version = currentVersion ?: return
            sections += VerificationSection(
                version = version,
                status = currentStatus.orEmpty(),
                blockedReason = blockedReason,
                steps = steps.toList()
            )
        }

        for (line in lines) {
            val header = sectionHeader.matchEntire(line)
            if (header != null) {
                finishSection()
                currentVersion = header.groupValues[1]
                currentStatus = header.groupValues[2]
                blockedReason = null
                steps = mutableListOf()
                continue
            }

            if (currentVersion != null && line.startsWith("**Blocked:**")) {
                blockedReason = line.removePrefix("**Blocked:**").trim()
            }

            val step = stepLine.matchEntire(line)
            if (step != null && currentVersion == step.groupValues[2]) {
                steps += VerificationStep(
                    version = step.groupValues[2],
                    text = step.groupValues[3],
                    isComplete = step.groupValues[1].isNotBlank()
                )
            }
        }
        finishSection()
        return sections
    }

    fun updateStep(
        body: String,
        version: String,
        stepIndex: Int,
        isComplete: Boolean,
        expectedStepText: String? = null
    ): String? {
        if (stepIndex < 0) return null
        var inSection = false
        var index = 0
        val lines = body.lines().toMutableList()
        for (lineIndex in lines.indices) {
            val header = sectionHeader.matchEntire(lines[lineIndex])
            if (header != null) {
                inSection = header.groupValues[1] == version
                index = 0
                continue
            }
            if (!inSection) continue

            val step = stepLine.matchEntire(lines[lineIndex]) ?: continue
            if (step.groupValues[2] != version) continue
            if (index == stepIndex) {
                if (expectedStepText != null && step.groupValues[3] != expectedStepText) return null
                val mark = if (isComplete) "x" else " "
                lines[lineIndex] = "- [$mark] [$version] ${step.groupValues[3]}"
                return lines.joinToString("\n")
            }
            index++
        }
        return null
    }

    fun allApplicableVersionsComplete(body: String): Boolean {
        val sections = sections(body)
        if (sections.isEmpty()) return false
        val applicableSections = sections.filter { it.status != "N/A" }
        if (applicableSections.isEmpty()) return false
        return applicableSections.all { section ->
            section.status == "READY" &&
                    section.steps.isNotEmpty() &&
                    section.steps.all { it.isComplete }
        }
    }
}
