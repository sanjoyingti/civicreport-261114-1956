package com.arin.civicreport.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Seed data for authorities. Edit this file or use the Settings editor to extend. */
object SeededAuthorities {

    private val MaharashtraCities = listOf("mumbai", "pune", "nagpur", "nashik", "aurangabad", "thane", "navi mumbai")
    private val KarnatakaCities = listOf("bangalore", "mysore", "hubli", "dharwad", "mangalore")
    private val TnCities = listOf("chennai", "coimbatore", "madurai", "tiruchirappalli", "salem")
    private val DelhiCities = listOf("delhi", "new delhi")
    private val UpCities = listOf("lucknow", "kanpur", "varanasi", "agra", "prayagraj", "meerut")
    private val GujaratCities = listOf("ahmedabad", "surat", "vadodara", "rajkot")
    private val WbCities = listOf("kolkata", "siliguri", "asansol")
    private val BiharCities = listOf("patna", "gaya", "bhagalpur")
    private val RajCities = listOf("jaipur", "jodhpur", "udaipur", "kota")
    private val KeralaCities = listOf("thiruvananthapuram", "kochi", "kozhikode", "kollam")

    fun all(): List<Authority> = listOf(
        // ---------------- Pothole / Bad Road ----------------
        Authority(
            id = "maharashtra_mumbai_pwd",
            name = "Mumbai PWD (Roads)",
            phone = "+91 22 2202 7161",
            email = "pwdmumbai@gmail.com",
            level = "Local",
            activity_tags = "pothole,bad_road,road_defect",
            applicable_states = json("Maharashtra"),
            applicable_cities = json("mumbai"),
            order = 1
        ),
        Authority(
            id = "maharashtra_mumbai_municipal",
            name = "BMC Engineering Dept",
            phone = "15331",
            email = "complaints@bmcgov.com",
            level = "Local",
            activity_tags = "pothole,bad_road,garbage_dumping,waterlogging,illegal_construction,encroachment",
            applicable_states = json("Maharashtra"),
            applicable_cities = json("mumbai","thane","navi mumbai"),
            order = 2
        ),
        Authority(
            id = "traffic_police_mumbai",
            name = "Mumbai Traffic Police",
            phone = "+91 22 2202 8755",
            email = "traffic@maha.gov.in",
            level = "Local",
            activity_tags = "pothole,bad_road,traffic",
            applicable_states = json("Maharashtra"),
            applicable_cities = json("mumbai"),
            order = 3
        ),
        Authority(
            id = "mh_pmda",
            name = "PWD Maharashtra (State)",
            phone = "1553",
            email = "pmda-maharashtra@gov.in",
            level = "State",
            activity_tags = "pothole,bad_road,road_defect",
            applicable_states = json("Maharashtra"),
            applicable_cities = json(""),
            order = 4
        ),
        Authority(
            id = "mh_cm_office",
            name = "Chief Minister Office, Maharashtra",
            phone = "020 2612 8060",
            email = "cmo@maharashtra.gov.in",
            level = "State",
            activity_tags = json("all"),
            applicable_states = json("Maharashtra"),
            applicable_cities = json(""),
            order = 5
        ),
        Authority(
            id = "pmo_india",
            name = "Prime Minister's Office (PMO)",
            phone = "1553",
            email = "pmo@pmindia.gov.in",
            level = "Central",
            activity_tags = json("all"),
            applicable_states = json(""),
            applicable_cities = json(""),
            order = 10
        ),

        // ---------------- Deforestation ----------------
        Authority(
            id = "maharashtra_forest",
            name = "Maharashtra Forest Department",
            phone = "+91 20 2612 5003",
            email = "pfc.maharashtra@nic.in",
            level = "State",
            activity_tags = "deforestation,tree_cutting,illegal_tree_cutting",
            applicable_states = json("Maharashtra"),
            applicable_cities = json(""),
            order = 1
        ),
        Authority(
            id = "maharashtra_wildlife",
            name = "Wildlife Crime Control Bureau (Maharashtra Unit)",
            phone = "1926",
            email = "wccb.gov.in",
            level = "Central",
            activity_tags = "deforestation,tree_cutting,wildlife",
            applicable_states = json(""),
            applicable_cities = json(""),
            order = 2
        ),
        Authority(
            id = "moefcc_india",
            name = "Ministry of Environment, Forest and Climate Change (MoEFCC)",
            phone = "+91 11 2430 6203",
            email = "moef@gov.in",
            level = "Central",
            activity_tags = "deforestation,tree_cutting,environment",
            applicable_states = json(""),
            applicable_cities = json(""),
            order = 3
        ),

        // ---------------- Illegal Sand Mining ----------------
        Authority(
            id = "mh_mines_geology",
            name = "Mines & Geology Department, Maharashtra",
            phone = "1800 233 4721",
            email = "dgm.maharashtra@nic.in",
            level = "State",
            activity_tags = "sand_mining,illegal_sand_mining",
            applicable_states = json("Maharashtra"),
            applicable_cities = json(""),
            order = 1
        ),
        Authority(
            id = "maharashtra_pollution",
            name = "Maharashtra Pollution Control Board",
            phone = "+91 22 2433 4456",
            email = "mpcb@maharashtra.gov.in",
            level = "State",
            activity_tags = "sand_mining,pollution,garbage_dumping",
            applicable_states = json("Maharashtra"),
            applicable_cities = json(""),
            order = 2
        ),

        // ---------------- Garbage Dumping / Pollution ----------------
        Authority(
            id = "cpcb_india",
            name = "Central Pollution Control Board (CPCB)",
            phone = "+91 11 2436 3983",
            email = "cpcb@nic.in",
            level = "Central",
            activity_tags = "garbage_dumping,pollution",
            applicable_states = json(""),
            applicable_cities = json(""),
            order = 1
        ),

        // ---------------- Illegal Construction ----------------
        Authority(
            id = "maharashtra_tp",
            name = "Town & Regional Planning (Maharashtra)",
            phone = "1553",
            email = "trp.maharashtra@nic.in",
            level = "State",
            activity_tags = "illegal_construction",
            applicable_states = json("Maharashtra"),
            applicable_cities = json(""),
            order = 1
        ),
        Authority(
            id = "maharashtra_rera",
            name = "RERA Maharashtra",
            phone = "+91 20 2612 8574",
            email = "info@rera.maharashtra.gov.in",
            level = "State",
            activity_tags = "illegal_construction",
            applicable_states = json("Maharashtra"),
            applicable_cities = json(""),
            order = 2
        ),

        // ---------------- Waterlogging ----------------
        Authority(
            id = "maharashtra_drainage",
            name = "BMC Drainage Dept",
            phone = "15331",
            email = "drainage@maha.gov.in",
            level = "Local",
            activity_tags = "waterlogging",
            applicable_states = json("Maharashtra"),
            applicable_cities = json("mumbai"),
            order = 1
        ),
        Authority(
            id = "mh_cm_relief",
            name = "CM Relief Fund, Maharashtra",
            phone = "1553",
            email = "cmrelief@maharashtra.gov.in",
            level = "State",
            activity_tags = "waterlogging,flood",
            applicable_states = json("Maharashtra"),
            applicable_cities = json(""),
            order = 2
        ),

        // ---------------- Encroachment ----------------
        Authority(
            id = "maharashtra_revenue",
            name = "Maharashtra Revenue Department",
            phone = "1553",
            email = "revenue.maharashtra@nic.in",
            level = "State",
            activity_tags = "encroachment",
            applicable_states = json("Maharashtra"),
            applicable_cities = json(""),
            order = 1
        ),

        // ---------------- Street Light ----------------
        Authority(
            id = "maharashtra_electric",
            name = "Mahavitaran (Electricity)",
            phone = "1912",
            email = "mahavitaran@maha.gov.in",
            level = "State",
            activity_tags = "street_light",
            applicable_states = json("Maharashtra"),
            applicable_cities = json(""),
            order = 1
        ),

        // ---------------- Noise Pollution ----------------
        Authority(
            id = "maharashtra_noise",
            name = "Maharashtra Pollution Control Board (Noise)",
            phone = "+91 22 2433 4456",
            email = "mpcb@maharashtra.gov.in",
            level = "State",
            activity_tags = "noise_pollution",
            applicable_states = json("Maharashtra"),
            applicable_cities = json(""),
            order = 1
        )
    )

    private fun json(vararg items: String): String {
        if (items.isEmpty()) return "[]"
        return items.joinToString(",", "[", "]") { "\"$it\"" }
    }
}
