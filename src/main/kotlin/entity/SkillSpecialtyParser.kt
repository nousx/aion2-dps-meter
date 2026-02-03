package com.tbread.entity

/**
 * Parses specialty slots from skill ID
 *
 * Skill ID format: XXXXXXYYYY
 * - XXXXXX = Base skill code (e.g., 133500)
 * - YYYY = Specialty slots [S1][S2][S3]0
 *
 * Examples:
 * - 13350000 → [] (no specialties)
 * - 13350020 → [2] (specialty slot 2)
 * - 13350120 → [1, 2] (specialty slots 1 and 2)
 * - 13351350 → [1, 3, 5] (specialty slots 1, 3, and 5)
 */
object SkillSpecialtyParser {

    /**
     * Parse specialty slot numbers from skill ID
     * Returns list of specialty slot numbers (1-5)
     */
    fun parseSpecialtySlots(skillId: Int): List<Int> {
        // Extract last 4 digits
        val last4Digits = skillId % 10000

        // Extract individual digits: [S1][S2][S3]0
        val slot1 = (last4Digits / 1000) % 10  // Thousands place
        val slot2 = (last4Digits / 100) % 10   // Hundreds place
        val slot3 = (last4Digits / 10) % 10    // Tens place

        val slots = mutableListOf<Int>()

        // Collect non-zero slots
        if (slot1 > 0) slots.add(slot1)
        if (slot2 > 0) slots.add(slot2)
        if (slot3 > 0) slots.add(slot3)

        // Debug logging
        // if (slots.isNotEmpty()) {
        //     println("[SkillSpecialtyParser] SkillID: $skillId, Last4: $last4Digits, Digits: [$slot1][$slot2][$slot3]0, Parsed: $slots")
        // }

        return slots.sorted() // Return sorted list
    }

    /**
     * Check if skill has any specialties
     */
    fun hasSpecialties(skillId: Int): Boolean {
        return parseSpecialtySlots(skillId).isNotEmpty()
    }

    /**
     * Get base skill ID (without specialty)
     */
    fun getBaseSkillId(skillId: Int): Int {
        return (skillId / 10000) * 10000
    }

    /**
     * Format specialty slots for display
     * Example: [1, 3, 5] → "Slots 1, 3, 5"
     */
    fun formatSpecialtySlots(skillId: Int): String {
        val slots = parseSpecialtySlots(skillId)
        return if (slots.isEmpty()) {
            "No specialties"
        } else {
            "Slots ${slots.joinToString(", ")}"
        }
    }
}
