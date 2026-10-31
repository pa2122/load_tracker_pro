package com.loadtracker.pro

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "facility_notes")
data class FacilityNoteEntity(
    @PrimaryKey val facilityKey: String,
    val displayName: String,
    val instructions: String,
    val directions: String,
    val parking: String,
    val updatedAt: Long
) {
    fun fields() = FacilityNoteFields(instructions, directions, parking)
}

data class FacilityNoteFields(
    val instructions: String = "",
    val directions: String = "",
    val parking: String = ""
) {
    val isEmpty: Boolean
        get() = instructions.isBlank() && directions.isBlank() && parking.isBlank()
}

/** Scan result tied to the facility it was read for, so it is never applied to a different facility. */
data class ScannedFacilityNotes(val facilityKey: String, val fields: FacilityNoteFields) {
    fun encode(): String =
        listOf(facilityKey, fields.instructions, fields.directions, fields.parking).joinToString(SEPARATOR)

    companion object {
        private const val SEPARATOR = "\u001F"

        fun decode(encoded: String): ScannedFacilityNotes? {
            val parts = encoded.split(SEPARATOR)
            if (parts.size != 4 || parts[0].isBlank()) return null
            return ScannedFacilityNotes(parts[0], FacilityNoteFields(parts[1], parts[2], parts[3]))
        }
    }
}

sealed class FacilityNotePlan {
    object NoChange : FacilityNotePlan()
    data class Save(val fields: FacilityNoteFields) : FacilityNotePlan()

    /** Saved and scanned notes disagree: [keepSaved] only fills blanks, [useScanned] replaces differing fields. */
    data class Ask(val keepSaved: FacilityNoteFields, val useScanned: FacilityNoteFields) : FacilityNotePlan()
}

data class FacilityPrompt(
    val key: String,
    val displayName: String,
    val saved: FacilityNoteFields,
    val plan: FacilityNotePlan.Ask
)

data class FacilityPromptState(val load: CurrentLoad, val queue: List<FacilityPrompt>)

object FacilityNotes {
    private val sectionHeader = Regex("""^-+\s*(SHIPPER|CONSIGNEE)\s+FACILITY\s+NOTES\s*-+""", RegexOption.IGNORE_CASE)
    private val fieldLabel = Regex("""(Instructions|Directions|Parking)\s*:\s*(.*)""", RegexOption.IGNORE_CASE)
    private val noiseEmoji = Regex("""[\u26A0\uFE0F\x{1F5FA}\x{1F17F}]""")

    // Must stay stable: stored keys are compared against keys computed from new scans.
    fun key(name: String): String =
        name.lowercase().replace(Regex("""[^a-z0-9]+"""), " ").trim()

    /** Splits a combined trip-notes blob into (shipper, consignee) facility notes. */
    fun parseTripNotes(tripNotes: String): Pair<FacilityNoteFields, FacilityNoteFields> {
        val sections = mutableMapOf("SHIPPER" to mutableMapOf<String, StringBuilder>(), "CONSIGNEE" to mutableMapOf<String, StringBuilder>())
        var section: String? = null
        var field: String? = null

        for (raw in tripNotes.lines()) {
            val line = raw.replace(noiseEmoji, "").trim()
            if (line.isEmpty()) continue
            val header = sectionHeader.find(line)
            if (header != null) {
                section = header.groupValues[1].uppercase()
                field = null
                continue
            }
            val current = section ?: continue
            val label = fieldLabel.find(line)
            if (label != null && label.range.first == 0) {
                field = label.groupValues[1].lowercase()
                val builder = sections.getValue(current).getOrPut(field) { StringBuilder() }
                builder.append(label.groupValues[2].trim())
            } else if (field != null) {
                sections.getValue(current).getValue(field).append(' ').append(line)
            }
        }

        fun toFields(values: Map<String, StringBuilder>) = FacilityNoteFields(
            instructions = values["instructions"]?.toString()?.trim().orEmpty(),
            directions = values["directions"]?.toString()?.trim().orEmpty(),
            parking = values["parking"]?.toString()?.trim().orEmpty()
        )
        return toFields(sections.getValue("SHIPPER")) to toFields(sections.getValue("CONSIGNEE"))
    }

    /** Newer non-blank values win; blank values never erase existing text. */
    fun merge(base: FacilityNoteFields, newer: FacilityNoteFields) = FacilityNoteFields(
        instructions = newer.instructions.ifBlank { base.instructions },
        directions = newer.directions.ifBlank { base.directions },
        parking = newer.parking.ifBlank { base.parking }
    )

    fun plan(existing: FacilityNoteFields?, scanned: FacilityNoteFields): FacilityNotePlan {
        if (scanned.isEmpty) return FacilityNotePlan.NoChange
        if (existing == null || existing.isEmpty) return FacilityNotePlan.Save(scanned)

        val keepSaved = merge(scanned, existing)
        val conflict = differs(existing.instructions, scanned.instructions) ||
            differs(existing.directions, scanned.directions) ||
            differs(existing.parking, scanned.parking)

        return when {
            conflict -> FacilityNotePlan.Ask(keepSaved, merge(existing, scanned))
            keepSaved != existing -> FacilityNotePlan.Save(keepSaved)
            else -> FacilityNotePlan.NoChange
        }
    }

    // OCR varies slightly between scans of identical text, so compare letters and digits only.
    private fun differs(saved: String, scanned: String): Boolean {
        if (saved.isBlank() || scanned.isBlank()) return false
        return squash(saved) != squash(scanned)
    }

    private fun squash(text: String) = text.lowercase().filter { it.isLetterOrDigit() }
}
