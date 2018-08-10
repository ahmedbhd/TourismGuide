package com.mobelite.tourismguide.tools

class Validators {
    companion object {
        fun isPhone(s: String): Boolean {
            return Regex("\\d{8}").matches(s) || Regex("\\+216\\d{8}").matches(s)
        }
    }
}