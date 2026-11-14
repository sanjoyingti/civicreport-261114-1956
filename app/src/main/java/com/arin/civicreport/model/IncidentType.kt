package com.arin.civicreport.model

/** All selectable incident types. Keep short slug keys plus human titles. */
sealed class IncidentType(val slug: String, val title: String) {
    object Pothole : IncidentType("pothole", "Pothole / Bad Road")
    object Deforestation : IncidentType("deforestation", "Deforestation / Illegal Tree Cutting")
    object SandMining : IncidentType("sand_mining", "Illegal Sand Mining")
    object IllegalConstruction : IncidentType("illegal_construction", "Illegal Construction")
    object GarbageDumping : IncidentType("garbage_dumping", "Garbage Dumping")
    object Waterlogging : IncidentType("waterlogging", "Waterlogging")
    object StreetLightMalfunction : IncidentType("street_light", "Street Light Malfunction")
    object Encroachment : IncidentType("encroachment", "Encroachment")
    object NoisePollution : IncidentType("noise_pollution", "Noise Pollution")

    companion object {
        fun all(): List<IncidentType> = listOf(
            Pothole, Deforestation, SandMining, IllegalConstruction,
            GarbageDumping, Waterlogging, StreetLightMalfunction,
            Encroachment, NoisePollution
        )

        // Reverse lookup by tag so the authority table's tags work
        fun bySlug(slug: String): IncidentType? = all().find { it.slug == slug }
    }
}
