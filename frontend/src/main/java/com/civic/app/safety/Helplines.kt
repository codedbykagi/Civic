package com.civic.app.safety

/**
 * India helplines shown next to the quick report, because someone in danger may tap "felt unsafe" expecting help.
 * Verified 2026-10 against Government of India sources (Mission Shakti / PIB). 1091 is deliberately left out:
 * it is a legacy state-run line whose national status could not be confirmed. Re-verify before every release.
 */
enum class Helpline(val number: String, val label: String) {
    EMERGENCY("112", "Emergency"),
    WOMEN("181", "Women"),
    CHILDREN("1098", "Children"),
}
