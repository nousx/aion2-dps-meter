package data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Data class representing the skill code configuration loaded from JSON.
 *
 * @property possibleOffsets Array of offsets used for skill code inference
 * @property skillCodes Array of valid skill codes (sorted)
 * @property skillNames Map of skill codes to their display names
 */
@Serializable
data class SkillCodeData(
    val possibleOffsets: List<Int>,
    val skillCodes: List<Int>,
    val skillNames: Map<Int, String>
)

/**
 * Loader for skill code configuration data.
 *
 * Loads skill codes, offsets, and skill names from a JSON resource file.
 * This externalizes hardcoded skill data to allow for easier maintenance and updates.
 */
object SkillCodeLoader {
    private val logger = LoggerFactory.getLogger(SkillCodeLoader::class.java)

    /**
     * Loads skill code data from the specified JSON resource file.
     *
     * @param resourcePath Path to the JSON resource file (default: "/data/skill_codes.json")
     * @return SkillCodeData object containing all skill configuration
     * @throws IllegalStateException if the resource file cannot be found or parsed
     */
    fun loadSkillCodeData(resourcePath: String = "/data/skill_codes.json"): SkillCodeData {
        logger.info("Loading skill code data from resource: {}", resourcePath)

        val resourceStream = SkillCodeLoader::class.java.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Could not find resource: $resourcePath")

        return try {
            val jsonContent = resourceStream.bufferedReader().use { it.readText() }
            val json = Json { ignoreUnknownKeys = true }
            val data = json.decodeFromString<SkillCodeData>(jsonContent)

            logger.info(
                "Successfully loaded skill data: {} offsets, {} skill codes, {} named skills",
                data.possibleOffsets.size,
                data.skillCodes.size,
                data.skillNames.size
            )

            data
        } catch (e: Exception) {
            logger.error("Failed to load skill code data from {}", resourcePath, e)
            throw IllegalStateException("Failed to load skill code data: ${e.message}", e)
        }
    }

    /**
     * Validates the loaded skill code data for consistency.
     *
     * @param data The skill code data to validate
     * @return true if data is valid, false otherwise
     */
    fun validateData(data: SkillCodeData): Boolean {
        if (data.possibleOffsets.isEmpty()) {
            logger.warn("No possible offsets defined")
            return false
        }

        if (data.skillCodes.isEmpty()) {
            logger.warn("No skill codes defined")
            return false
        }

        // Check if skillCodes is sorted (required for binary search)
        val sortedCodes = data.skillCodes.sorted()
        if (data.skillCodes != sortedCodes) {
            logger.warn("Skill codes are not sorted - binary search may fail")
            return false
        }

        logger.info("Skill code data validation passed")
        return true
    }
}
