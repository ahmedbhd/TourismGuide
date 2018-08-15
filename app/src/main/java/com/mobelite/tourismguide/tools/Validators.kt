package com.mobelite.tourismguide.tools

import com.mobelite.tourismguide.data.webservice.Model

class Validators {
    companion object {
        fun isPhone(s: String): Boolean {
            return Regex("\\d{8}").matches(s) || Regex("\\+216\\d{8}").matches(s)
        }

        fun calculateRating(rates : List<Model.Rating>): Float {
            var s  =0f
            rates.forEach{ r ->
                s += r.rate
            }
            if (rates.isEmpty())
                return 0f
            return s/rates.size
        }
    }
}