package com.civic.shared.model

import kotlinx.serialization.Serializable

/**
 * Optional "what made it feel unsafe?" tags on a safety report, modelled on Safetipin-style audit parameters.
 * They describe the place, never people. Stored by name, so new values must only be appended.
 */
@Serializable
enum class SafetyTag(val displayName: String) {
    POOR_LIGHTING("Poor lighting"),
    DESERTED("Deserted"),
    HARASSMENT("Harassment / staring"),
    NO_FOOTPATH("No footpath"),
    NO_TRANSPORT("No transport nearby"),
    DRINKING("Drinking / drugs"),
}
