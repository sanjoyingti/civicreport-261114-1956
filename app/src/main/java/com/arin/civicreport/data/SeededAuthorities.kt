package com.arin.civicreport.data

/** Seed data for authorities. Edit this file or use the Settings editor to extend. */
object SeededAuthorities {

    fun all(): List<Authority> = listOf(
        // ---------------- Pothole / Bad Road ----------------
        Authority(
            id = "maharashtra_mumbai_pwd",
            name = "Mumbai PWD (Roads)",
            phone = "+91 22 2202 7161",
            email = "pwdmumbai@gmail.com",
            level = "Local",
            activity_tags = list("pothole", "bad_road", "road_defect"),
            applicable_states = list("Maharashtra"),
            applicable_cities = list("mumbai"),
            order = 1
        ),
        Authority(
            id = "maharashtra_mumbai_municipal",
            name = "BMC Engineering Dept",
            phone = "15331",
            email = "complaints@bmcgov.com",
            level = "Local",
            activity_tags = list("pothole", "bad_road", "garbage_dumping", "waterlogging", "illegal_construction", "encroachment"),
            applicable_states = list("Maharashtra"),
            applicable_cities = list("mumbai", "thane", "navi mumbai"),
            order = 2
        ),
        Authority(
            id = "traffic_police_mumbai",
            name = "Mumbai Traffic Police",
            phone = "+91 22 2202 8755",
            email = "traffic@maha.gov.in",
            level = "Local",
            activity_tags = list("pothole", "bad_road"),
            applicable_states = list("Maharashtra"),
            applicable_cities = list("mumbai"),
            order = 3
        ),
        Authority(
            id = "mh_pwd_state",
            name = "PWD Maharashtra (State)",
            phone = "1553",
            email = "pmda-maharashtra@gov.in",
            level = "State",
            activity_tags = list("pothole", "bad_road", "road_defect"),
            applicable_states = list("Maharashtra"),
            applicable_cities = list(),
            order = 4
        ),
        Authority(
            id = "mh_cm_office",
            name = "Chief Minister Office, Maharashtra",
            phone = "020 2612 8060",
            email = "cmo@maharashtra.gov.in",
            level = "State",
            activity_tags = list("all"),
            applicable_states = list("Maharashtra"),
            applicable_cities = list(),
            order = 5
        ),
        Authority(
            id = "pmo_india",
            name = "Prime Minister's Office (PMO)",
            phone = "1553",
            email = "pmo@pmindia.gov.in",
            level = "Central",
            activity_tags = list("all"),
            applicable_states = list(),
            applicable_cities = list(),
            order = 10
        ),

        // ---------------- Deforestation ----------------
        Authority(
            id = "maharashtra_forest",
            name = "Maharashtra Forest Department",
            phone = "+91 20 2612 5003",
            email = "pfc.maharashtra@nic.in",
            level = "State",
            activity_tags = list("deforestation", "tree_cutting"),
            applicable_states = list("Maharashtra"),
            applicable_cities = list(),
            order = 1
        ),
        Authority(
            id = "wccb_india",
            name = "Wildlife Crime Control Bureau",
            phone = "1926",
            email = "wccb-mowef@nic.in",
            level = "Central",
            activity_tags = list("deforestation", "tree_cutting", "wildlife"),
            applicable_states = list(),
            applicable_cities = list(),
            order = 2
        ),
        Authority(
            id = "moefcc_india",
            name = "Ministry of Environment, Forest and Climate Change",
            phone = "+91 11 2430 6203",
            email = "moef@gov.in",
            level = "Central",
            activity_tags = list("deforestation", "tree_cutting", "sand_mining"),
            applicable_states = list(),
            applicable_cities = list(),
            order = 3
        ),

        // ---------------- Illegal Sand Mining ----------------
        Authority(
            id = "mh_mines_geology",
            name = "Mines & Geology Department, Maharashtra",
            phone = "1800 233 4721",
            email = "dgm.maharashtra@nic.in",
            level = "State",
            activity_tags = list("sand_mining"),
            applicable_states = list("Maharashtra"),
            applicable_cities = list(),
            order = 1
        ),
        Authority(
            id = "maharashtra_pollution",
            name = "Maharashtra Pollution Control Board",
            phone = "+91 22 2433 4456",
            email = "mpcb@maharashtra.gov.in",
            level = "State",
            activity_tags = list("sand_mining", "garbage_dumping", "noise_pollution"),
            applicable_states = list("Maharashtra"),
            applicable_cities = list(),
            order = 2
        ),

        // ---------------- Garbage Dumping / Pollution ----------------
        Authority(
            id = "cpcb_india",
            name = "Central Pollution Control Board (CPCB)",
            phone = "+91 11 2436 3983",
            email = "cpcb@nic.in",
            level = "Central",
            activity_tags = list("garbage_dumping", "noise_pollution", "sand_mining"),
            applicable_states = list(),
            applicable_cities = list(),
            order = 1
        ),

        // ---------------- Illegal Construction ----------------
        Authority(
            id = "maharashtra_tp",
            name = "Town & Regional Planning (Maharashtra)",
            phone = "1553",
            email = "trp.maharashtra@nic.in",
            level = "State",
            activity_tags = list("illegal_construction", "encroachment"),
            applicable_states = list("Maharashtra"),
            applicable_cities = list(),
            order = 1
        ),
        Authority(
            id = "maharashtra_rera",
            name = "RERA Maharashtra",
            phone = "+91 20 2612 8574",
            email = "info@rera.maharashtra.gov.in",
            level = "State",
            activity_tags = list("illegal_construction"),
            applicable_states = list("Maharashtra"),
            applicable_cities = list(),
            order = 2
        ),

        // ---------------- Waterlogging ----------------
        Authority(
            id = "maharashtra_drainage",
            name = "BMC Drainage Dept",
            phone = "15331",
            email = "drainage@mcgm.gov.in",
            level = "Local",
            activity_tags = list("waterlogging"),
            applicable_states = list("Maharashtra"),
            applicable_cities = list("mumbai"),
            order = 1
        ),
        Authority(
            id = "mh_cm_relief",
            name = "CM Relief Fund, Maharashtra",
            phone = "1553",
            email = "cmrelief@maharashtra.gov.in",
            level = "State",
            activity_tags = list("waterlogging"),
            applicable_states = list("Maharashtra"),
            applicable_cities = list(),
            order = 2
        ),

        // ---------------- Encroachment ----------------
        Authority(
            id = "maharashtra_revenue",
            name = "Maharashtra Revenue Department",
            phone = "1553",
            email = "revenue.maharashtra@nic.in",
            level = "State",
            activity_tags = list("encroachment"),
            applicable_states = list("Maharashtra"),
            applicable_cities = list(),
            order = 1
        ),

        // ---------------- Street Light ----------------
        Authority(
            id = "maharashtra_electric",
            name = "Mahavitaran (Electricity)",
            phone = "1912",
            email = "mahavitaran@maha.gov.in",
            level = "State",
            activity_tags = list("street_light"),
            applicable_states = list("Maharashtra"),
            applicable_cities = list(),
            order = 1
        )
    )

    private fun list(vararg items: String): String = items.joinToString(",")
}
